<script setup lang="ts">
import { computed, ref } from 'vue'
import { Node, mergeAttributes } from '@tiptap/core'
import Image from '@tiptap/extension-image'
import Link from '@tiptap/extension-link'
import Placeholder from '@tiptap/extension-placeholder'
import StarterKit from '@tiptap/starter-kit'
import { EditorContent, useEditor } from '@tiptap/vue-3'
import InteractiveConfigEditor from '@/features/courses/components/interactive/InteractiveConfigEditor.vue'
import {
  refreshSubTopicAssets,
  uploadSubTopicImage,
  type AdminSubTopicDto,
  type InteractionType,
} from '@/features/courses/services/adminCourses'

const props = defineProps<{
  subTopic: AdminSubTopicDto
}>()

const emit = defineEmits<{
  save: [{
    title: string
    content: string
    interactionType: InteractionType
    interactionPrompt: string | null
    interactionConfig: string | null
  }]
  cancel: []
  error: [message: string]
}>()

const FigureImage = Node.create({
  name: 'figureImage',
  group: 'block',
  atom: true,
  selectable: true,
  draggable: true,

  addAttributes() {
    return {
      assetId: {
        default: null,
        parseHTML: (element) => element.querySelector('img')?.getAttribute('data-asset-id'),
      },
      src: {
        default: null,
        parseHTML: (element) => element.querySelector('img')?.getAttribute('src'),
      },
      alt: {
        default: '',
        parseHTML: (element) => element.querySelector('img')?.getAttribute('alt') ?? '',
      },
      caption: {
        default: '',
        parseHTML: (element) => element.querySelector('figcaption')?.textContent ?? '',
      },
    }
  },

  parseHTML() {
    return [{ tag: 'figure' }]
  },

  renderHTML({ HTMLAttributes }) {
    const { assetId, src, alt, caption } = HTMLAttributes
    return [
      'figure',
      {},
      ['img', mergeAttributes({
        src,
        alt,
        'data-asset-id': assetId,
      })],
      ['figcaption', {}, caption || alt || ''],
    ]
  },
})

const title = ref(props.subTopic.title)
const interactionType = ref<InteractionType>(props.subTopic.interactionType ?? 'NONE')
const interactionPrompt = ref(props.subTopic.interactionPrompt ?? '')
const interactionConfig = ref(props.subTopic.interactionConfig ?? '')
const imageInputRef = ref<HTMLInputElement | null>(null)
const imageUploading = ref(false)
const assetRefreshing = ref(false)
const localMessage = ref('')

const editor = useEditor({
  content: props.subTopic.contentHtml || textToHtml(props.subTopic.content || ''),
  extensions: [
    StarterKit.configure({
      heading: {
        levels: [2, 3],
      },
    }),
    Link.configure({
      openOnClick: false,
      HTMLAttributes: {
        rel: 'noopener noreferrer',
        target: '_blank',
      },
    }),
    Image.extend({
      addAttributes() {
        return {
          ...this.parent?.(),
          assetId: {
            default: null,
            parseHTML: (element) => element.getAttribute('data-asset-id'),
            renderHTML: (attributes) => {
              if (!attributes.assetId) return {}
              return { 'data-asset-id': attributes.assetId }
            },
          },
        }
      },
    }).configure({
      allowBase64: false,
    }),
    FigureImage,
    Placeholder.configure({
      placeholder: 'Write lesson content...',
    }),
  ],
  editorProps: {
    attributes: {
      class: 'lesson-editor min-h-[280px] rounded-[10px] border-2 border-lm-line-soft bg-lm-bg-soft px-4 py-3 text-[13px] leading-7 text-lm-ink-2 outline-none focus:border-lm-line focus:bg-lm-surface',
    },
  },
})

const currentBlock = computed(() => {
  if (!editor.value) return 'paragraph'
  if (editor.value.isActive('heading', { level: 2 })) return 'h2'
  if (editor.value.isActive('heading', { level: 3 })) return 'h3'
  return 'paragraph'
})

function setBlock(event: Event) {
  const value = (event.target as HTMLSelectElement).value
  if (!editor.value) return
  if (value === 'h2') {
    editor.value.chain().focus().setHeading({ level: 2 }).run()
  } else if (value === 'h3') {
    editor.value.chain().focus().setHeading({ level: 3 }).run()
  } else {
    editor.value.chain().focus().setParagraph().run()
  }
}

function setLink() {
  if (!editor.value) return
  const previousUrl = editor.value.getAttributes('link').href as string | undefined
  const url = window.prompt('Link URL', previousUrl ?? '')
  if (url === null) return
  if (!url.trim()) {
    editor.value.chain().focus().extendMarkRange('link').unsetLink().run()
    return
  }
  editor.value.chain().focus().extendMarkRange('link').setLink({ href: url.trim() }).run()
}

async function onImageFileChange(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file || !editor.value) return
  imageUploading.value = true
  localMessage.value = ''
  try {
    const defaultCaption = file.name.replace(/\.[^.]+$/, '')
    const asset = await uploadSubTopicImage(props.subTopic.id, file, defaultCaption)
    const caption = window.prompt('Caption', asset.altText || defaultCaption) ?? asset.altText ?? defaultCaption
    editor.value
      .chain()
      .focus()
      .insertContent({
        type: 'figureImage',
        attrs: {
          assetId: String(asset.id),
          src: asset.fileUrl,
          alt: asset.altText || asset.fileName,
          caption: caption.trim(),
        },
      })
      .run()
    localMessage.value = 'Image inserted.'
  } catch (error) {
    emit('error', error instanceof Error ? error.message : 'Unable to upload image.')
  } finally {
    imageUploading.value = false
    if (imageInputRef.value) imageInputRef.value.value = ''
  }
}

async function refreshAssetUrls() {
  if (!editor.value) return
  assetRefreshing.value = true
  localMessage.value = ''
  try {
    const assets = await refreshSubTopicAssets(props.subTopic.id)
    const urlsById = new Map(assets.map((asset) => [String(asset.id), asset.fileUrl]))
    const transaction = editor.value.state.tr
    editor.value.state.doc.descendants((node, pos) => {
      const assetId = node.attrs.assetId == null ? null : String(node.attrs.assetId)
      const freshUrl = assetId == null ? null : urlsById.get(assetId)
      if (!freshUrl || (node.type.name !== 'figureImage' && node.type.name !== 'image')) return
      transaction.setNodeMarkup(pos, undefined, {
        ...node.attrs,
        src: freshUrl,
      })
    })
    if (transaction.docChanged) {
      editor.value.view.dispatch(transaction)
    }
    localMessage.value = 'Image URLs refreshed.'
  } catch (error) {
    emit('error', error instanceof Error ? error.message : 'Unable to refresh image URLs.')
  } finally {
    assetRefreshing.value = false
  }
}

function onSave() {
  if (interactionType.value !== 'NONE' && interactionConfig.value.trim()) {
    try {
      JSON.parse(interactionConfig.value)
    } catch {
      emit('error', 'Interaction config must be valid JSON.')
      return
    }
  }
  emit('save', {
    title: title.value.trim(),
    content: editor.value?.getHTML() ?? '',
    interactionType: interactionType.value,
    interactionPrompt: interactionPrompt.value.trim() || null,
    interactionConfig: interactionType.value === 'NONE' ? null : interactionConfig.value.trim() || null,
  })
}

function textToHtml(value: string) {
  return value
    .split(/\n{2,}/)
    .map((paragraph) => paragraph.trim())
    .filter(Boolean)
    .map((paragraph) => `<p>${escapeHtml(paragraph).replace(/\n/g, '<br>')}</p>`)
    .join('')
}

function escapeHtml(value: string) {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}
</script>

<template>
  <div class="rounded-[18px] border-2 border-lm-line bg-lm-surface">
    <div class="flex flex-wrap items-center gap-2 border-b-2 border-lm-line-soft px-3 py-2">
      <select
        :value="currentBlock"
        class="h-8 rounded-[8px] border-2 border-lm-line-soft bg-lm-surface px-2 text-[11px] font-semibold text-lm-ink outline-none transition focus:border-lm-line"
        @change="setBlock"
      >
        <option value="paragraph">Paragraph</option>
        <option value="h2">Heading 2</option>
        <option value="h3">Heading 3</option>
      </select>
      <button
        type="button"
        class="tool-button font-bold"
        :class="{ 'tool-button--active': editor?.isActive('bold') }"
        title="Bold"
        @mousedown.prevent="editor?.chain().focus().toggleBold().run()"
      >
        B
      </button>
      <button
        type="button"
        class="tool-button italic"
        :class="{ 'tool-button--active': editor?.isActive('italic') }"
        title="Italic"
        @mousedown.prevent="editor?.chain().focus().toggleItalic().run()"
      >
        I
      </button>
      <button
        type="button"
        class="tool-button"
        :class="{ 'tool-button--active': editor?.isActive('code') }"
        title="Inline code"
        @mousedown.prevent="editor?.chain().focus().toggleCode().run()"
      >
        &lt;/&gt;
      </button>
      <button
        type="button"
        class="tool-button"
        :class="{ 'tool-button--active': editor?.isActive('codeBlock') }"
        title="Code block"
        @mousedown.prevent="editor?.chain().focus().toggleCodeBlock().run()"
      >
        Pre
      </button>
      <button
        type="button"
        class="tool-button"
        :class="{ 'tool-button--active': editor?.isActive('link') }"
        title="Link"
        @mousedown.prevent="setLink"
      >
        Link
      </button>
      <button
        type="button"
        class="tool-button"
        :class="{ 'tool-button--active': editor?.isActive('bulletList') }"
        title="Bullet list"
        @mousedown.prevent="editor?.chain().focus().toggleBulletList().run()"
      >
        List
      </button>
      <button
        type="button"
        class="tool-button"
        :class="{ 'tool-button--active': editor?.isActive('orderedList') }"
        title="Numbered list"
        @mousedown.prevent="editor?.chain().focus().toggleOrderedList().run()"
      >
        1.
      </button>
      <input ref="imageInputRef" type="file" accept="image/png,image/jpeg,image/webp" class="sr-only" @change="onImageFileChange" />
      <button
        type="button"
        class="tool-button"
        title="Insert image"
        :disabled="imageUploading"
        @mousedown.prevent="imageInputRef?.click()"
      >
        {{ imageUploading ? '...' : 'Img' }}
      </button>
      <button
        type="button"
        class="tool-button"
        title="Refresh image URLs"
        :disabled="assetRefreshing"
        @mousedown.prevent="refreshAssetUrls"
      >
        {{ assetRefreshing ? '...' : 'Refresh' }}
      </button>
      <span v-if="localMessage" class="text-[11px] font-medium text-lm-green">{{ localMessage }}</span>
    </div>

    <div class="grid gap-4 p-3">
      <label class="block">
        <span class="mb-1 block text-[11px] font-bold text-lm-ink-3">Title</span>
        <input
          v-model="title"
          type="text"
          class="h-9 w-full rounded-[8px] border-2 border-lm-line-soft bg-lm-bg-soft px-3 text-[12px] font-semibold text-lm-ink outline-none transition focus:border-lm-line focus:bg-lm-surface focus:ring-2 focus:ring-lm-yellow/40"
        />
      </label>
      <EditorContent :editor="editor" />

      <section class="interactive-admin-panel">
        <div class="interactive-admin-panel__header">
          <h3>Interactive</h3>
          <span>{{ interactionType }}</span>
        </div>
        <InteractiveConfigEditor
          v-model:interaction-type="interactionType"
          v-model:interaction-prompt="interactionPrompt"
          v-model:interaction-config="interactionConfig"
          @error="emit('error', $event)"
        />
      </section>

      <div class="flex gap-2">
        <button
          type="button"
          class="h-9 rounded-[8px] border-2 border-lm-ink bg-lm-ink px-3 text-[12px] font-bold text-lm-bg transition-all duration-200 hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-60"
          :disabled="!title.trim()"
          @click="onSave"
        >
          Save
        </button>
        <button
          type="button"
          class="h-9 rounded-[8px] border-2 border-lm-line bg-lm-surface px-3 text-[12px] font-bold text-lm-ink transition hover:bg-lm-bg"
          @click="emit('cancel')"
        >
          Cancel
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.tool-button {
  height: 2rem;
  min-width: 2rem;
  border-radius: 0.375rem;
  padding: 0 0.5rem;
  font-size: 11px;
  font-weight: 700;
  color: #6b6660;
}
.tool-button:hover,
.tool-button--active {
  background: #ffd333;
  color: #1a1814;
}
.tool-button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
.interactive-admin-panel {
  border: 2px solid #d4cec6;
  border-radius: 12px;
  background: #fffdf8;
  padding: 0.9rem;
}
.interactive-admin-panel__header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
}
.interactive-admin-panel__header h3 {
  margin: 0;
  color: #1a1814;
  font-size: 1rem;
  font-weight: 900;
}
.interactive-admin-panel__header span {
  border: 2px solid #d4cec6;
  border-radius: 999px;
  background: #f7f2ea;
  padding: 0.15rem 0.55rem;
  color: #6b6660;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
  font-weight: 800;
}
:deep(.tiptap p.is-editor-empty:first-child::before) {
  content: attr(data-placeholder);
  float: left;
  height: 0;
  color: #9e9892;
  pointer-events: none;
}
:deep(.lesson-editor h2) {
  margin: 0.9rem 0 0.35rem;
  font-size: 1.05rem;
  font-weight: 800;
  color: #1a1814;
}
:deep(.lesson-editor h3) {
  margin: 0.75rem 0 0.25rem;
  font-size: 0.95rem;
  font-weight: 800;
  color: #1a1814;
}
:deep(.lesson-editor p),
:deep(.lesson-editor ul),
:deep(.lesson-editor ol),
:deep(.lesson-editor blockquote),
:deep(.lesson-editor pre),
:deep(.lesson-editor figure) {
  margin: 0.55rem 0;
}
:deep(.lesson-editor ul),
:deep(.lesson-editor ol) {
  padding-left: 1.25rem;
}
:deep(.lesson-editor ul) {
  list-style: disc;
}
:deep(.lesson-editor ol) {
  list-style: decimal;
}
:deep(.lesson-editor code) {
  border-radius: 0.25rem;
  background: #f0ece4;
  padding: 0.1rem 0.25rem;
  font-size: 0.85em;
  color: #1a1814;
}
:deep(.lesson-editor pre) {
  overflow: auto;
  border-radius: 0.5rem;
  background: #1a1814;
  padding: 0.75rem;
  color: #fbf7ef;
}
:deep(.lesson-editor pre code) {
  background: transparent;
  padding: 0;
  color: inherit;
}
:deep(.lesson-editor blockquote) {
  border-left: 3px solid #d4cec6;
  padding-left: 0.8rem;
  color: #6b6660;
}
:deep(.lesson-editor figure) {
  display: block;
}
:deep(.lesson-editor img) {
  max-width: 100%;
  height: auto;
  border-radius: 0.5rem;
}
:deep(.lesson-editor figcaption) {
  margin-top: 0.25rem;
  font-size: 0.75rem;
  color: #9e9892;
}
</style>
