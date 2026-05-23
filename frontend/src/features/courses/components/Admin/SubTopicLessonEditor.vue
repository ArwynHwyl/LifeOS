<script setup lang="ts">
import { computed, ref } from 'vue'
import { Node, mergeAttributes } from '@tiptap/core'
import Image from '@tiptap/extension-image'
import Link from '@tiptap/extension-link'
import Placeholder from '@tiptap/extension-placeholder'
import StarterKit from '@tiptap/starter-kit'
import { EditorContent, useEditor } from '@tiptap/vue-3'
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
      class: 'lesson-editor min-h-[280px] rounded-lg border border-slate-200 bg-slate-50 px-4 py-3 text-[13px] leading-7 text-slate-700 outline-none focus:border-[#5b4cfa]/50 focus:bg-white',
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
  if (interactionConfig.value.trim()) {
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
    interactionConfig: interactionConfig.value.trim() || null,
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
  <div class="rounded-lg border border-[#d9d4ff] bg-white">
    <div class="flex flex-wrap items-center gap-2 border-b border-slate-200 px-3 py-2">
      <select
        :value="currentBlock"
        class="h-8 rounded-md border border-slate-200 bg-white px-2 text-[11px] font-semibold text-slate-600"
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
      <span v-if="localMessage" class="text-[11px] font-medium text-emerald-700">{{ localMessage }}</span>
    </div>

    <div class="grid gap-3 p-3 lg:grid-cols-[260px_minmax(0,1fr)]">
      <div class="space-y-3">
        <label class="block">
          <span class="mb-1 block text-[11px] font-bold text-slate-500">Title</span>
          <input
            v-model="title"
            type="text"
            class="h-9 w-full rounded-lg border border-slate-200 px-3 text-[12px] font-semibold text-slate-800 outline-none focus:border-[#5b4cfa]/50"
          />
        </label>
        <label class="block">
          <span class="mb-1 block text-[11px] font-bold text-slate-500">Interaction</span>
          <select v-model="interactionType" class="h-9 w-full rounded-lg border border-slate-200 px-3 text-[12px] text-slate-700">
            <option value="NONE">None</option>
            <option value="THREE_JS">3D visual</option>
            <option value="GRAPH_2D">2D graph</option>
            <option value="FORMULA_EXPLORER">Formula explorer</option>
            <option value="QUIZ">Quiz</option>
            <option value="OTHER">Other</option>
          </select>
        </label>
        <label class="block">
          <span class="mb-1 block text-[11px] font-bold text-slate-500">Interaction prompt</span>
          <textarea
            v-model="interactionPrompt"
            rows="4"
            class="w-full resize-none rounded-lg border border-slate-200 px-3 py-2 text-[12px] text-slate-700 outline-none focus:border-[#5b4cfa]/50"
          />
        </label>
        <label class="block">
          <span class="mb-1 block text-[11px] font-bold text-slate-500">Interaction config JSON</span>
          <textarea
            v-model="interactionConfig"
            rows="6"
            spellcheck="false"
            placeholder="{&quot;kind&quot;:&quot;quiz&quot;,&quot;items&quot;:[]}"
            class="w-full resize-none rounded-lg border border-slate-200 px-3 py-2 font-mono text-[11px] leading-5 text-slate-700 outline-none focus:border-[#5b4cfa]/50"
          />
        </label>
        <div class="flex gap-2">
          <button
            type="button"
            class="h-9 rounded-lg bg-[#5b4cfa] px-3 text-[12px] font-bold text-white disabled:cursor-not-allowed disabled:opacity-60"
            :disabled="!title.trim()"
            @click="onSave"
          >
            Save
          </button>
          <button type="button" class="h-9 rounded-lg border border-slate-200 px-3 text-[12px] font-bold text-slate-500" @click="emit('cancel')">
            Cancel
          </button>
        </div>
      </div>

      <EditorContent :editor="editor" />
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
  color: #475569;
}
.tool-button:hover,
.tool-button--active {
  background: #f1f5f9;
  color: #5b4cfa;
}
.tool-button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
:deep(.tiptap p.is-editor-empty:first-child::before) {
  content: attr(data-placeholder);
  float: left;
  height: 0;
  color: #94a3b8;
  pointer-events: none;
}
:deep(.lesson-editor h2) {
  margin: 0.9rem 0 0.35rem;
  font-size: 1.05rem;
  font-weight: 800;
  color: #0f172a;
}
:deep(.lesson-editor h3) {
  margin: 0.75rem 0 0.25rem;
  font-size: 0.95rem;
  font-weight: 800;
  color: #1e293b;
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
  background: #e2e8f0;
  padding: 0.1rem 0.25rem;
  font-size: 0.85em;
}
:deep(.lesson-editor pre) {
  overflow: auto;
  border-radius: 0.5rem;
  background: #0f172a;
  padding: 0.75rem;
  color: #e2e8f0;
}
:deep(.lesson-editor pre code) {
  background: transparent;
  padding: 0;
  color: inherit;
}
:deep(.lesson-editor blockquote) {
  border-left: 3px solid #cbd5e1;
  padding-left: 0.8rem;
  color: #64748b;
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
  color: #64748b;
}
</style>
