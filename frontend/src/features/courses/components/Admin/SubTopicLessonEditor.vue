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
const localMessage = ref('')

const interactionChoices: Array<{ type: InteractionType; label: string; icon: string }> = [
  { type: 'NONE', label: 'None', icon: '' },
  { type: 'QUIZ', label: 'Quiz', icon: '?' },
  { type: 'GRAPH_2D', label: 'Graph', icon: '~' },
  { type: 'FORMULA_EXPLORER', label: 'Formula', icon: 'f(x)' },
  { type: 'VISUAL_LAYER', label: 'Set / Diagram', icon: '○○' },
]

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
      class: 'lesson-editor min-h-[260px] bg-lm-surface px-6 py-6 text-[15px] leading-8 text-lm-ink-2 outline-none',
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

function onSave() {
  let normalizedInteractionConfig = interactionConfig.value.trim() || null
  if (interactionType.value !== 'NONE' && interactionConfig.value.trim()) {
    try {
      const parsedConfig = JSON.parse(interactionConfig.value) as Record<string, unknown>
      delete parsedConfig.prompt
      normalizedInteractionConfig = JSON.stringify(parsedConfig)
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
    interactionConfig: interactionType.value === 'NONE' ? null : normalizedInteractionConfig,
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
  <div class="lesson-edit-shell">
    <div class="grid gap-4">
      <label class="block">
        <span class="form-label">Title</span>
        <input
          v-model="title"
          type="text"
          class="field-input h-12"
        />
      </label>

      <section>
        <span class="form-label">Content</span>
        <div class="content-editor-card">
          <div class="flex flex-wrap items-center gap-2 border-b-2 border-lm-line-soft bg-lm-bg-soft px-3 py-2">
            <select
              :value="currentBlock"
              class="h-8 rounded-[8px] border-2 border-lm-line-soft bg-lm-surface px-2 text-[11px] font-semibold text-lm-ink outline-none transition focus:border-lm-line"
              @change="setBlock"
            >
              <option value="paragraph">Paragraph</option>
              <option value="h2">Heading 2</option>
              <option value="h3">Heading 3</option>
            </select>
            <button type="button" class="tool-button font-bold" :class="{ 'tool-button--active': editor?.isActive('bold') }" title="Bold" @mousedown.prevent="editor?.chain().focus().toggleBold().run()">B</button>
            <button type="button" class="tool-button italic" :class="{ 'tool-button--active': editor?.isActive('italic') }" title="Italic" @mousedown.prevent="editor?.chain().focus().toggleItalic().run()">I</button>
            <button type="button" class="tool-button" :class="{ 'tool-button--active': editor?.isActive('code') }" title="Inline code" @mousedown.prevent="editor?.chain().focus().toggleCode().run()">&lt;/&gt;</button>
            <button type="button" class="tool-button" :class="{ 'tool-button--active': editor?.isActive('link') }" title="Link" @mousedown.prevent="setLink">Link</button>
            <button type="button" class="tool-button" :class="{ 'tool-button--active': editor?.isActive('bulletList') }" title="Bullet list" @mousedown.prevent="editor?.chain().focus().toggleBulletList().run()">List</button>
            <button type="button" class="tool-button" :class="{ 'tool-button--active': editor?.isActive('orderedList') }" title="Numbered list" @mousedown.prevent="editor?.chain().focus().toggleOrderedList().run()">1.</button>
            <input ref="imageInputRef" type="file" accept="image/png,image/jpeg,image/webp" class="sr-only" @change="onImageFileChange" />
            <button type="button" class="tool-button" title="Insert image" :disabled="imageUploading" @mousedown.prevent="imageInputRef?.click()">{{ imageUploading ? '...' : 'Img' }}</button>
            <span v-if="localMessage" class="text-[11px] font-medium text-lm-green">{{ localMessage }}</span>
          </div>
          <EditorContent :editor="editor" />
        </div>
      </section>

      <label class="block">
        <span class="form-label">Interaction Prompt</span>
        <input
          v-model="interactionPrompt"
          type="text"
          placeholder="What do learners need to do?"
          class="field-input h-14 text-[18px]"
        />
      </label>

      <section class="interactive-admin-panel">
        <div class="interactive-admin-panel__header">
          <div class="flex items-center gap-3">
            <h3>Interactive</h3>
            <span v-if="interactionType !== 'NONE'">{{ interactionType }}</span>
          </div>
        </div>
        <div class="interaction-type-grid">
          <button
            v-for="choice in interactionChoices"
            :key="choice.type"
            type="button"
            class="interaction-type-button"
            :class="{ active: interactionType === choice.type }"
            @click="interactionType = choice.type"
          >
            <span v-if="choice.icon" class="interaction-type-button__icon">{{ choice.icon }}</span>
            <strong>{{ choice.label }}</strong>
          </button>
        </div>
        <InteractiveConfigEditor
          v-model:interaction-type="interactionType"
          v-model:interaction-prompt="interactionPrompt"
          v-model:interaction-config="interactionConfig"
          @error="emit('error', $event)"
        />
      </section>

      <div class="flex gap-3">
        <button
          type="button"
          class="inline-flex h-14 items-center gap-3 rounded-full border-2 border-lm-ink bg-lm-ink px-8 text-[18px] font-extrabold text-lm-bg transition-all duration-200 hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-60"
          :disabled="!title.trim()"
          @click="onSave"
        >
          <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M20 6 9 17l-5-5" /></svg>
          Save
        </button>
        <button
          type="button"
          class="h-14 rounded-full border-2 border-lm-line bg-lm-surface px-8 text-[18px] font-extrabold text-lm-ink transition hover:bg-lm-bg"
          @click="emit('cancel')"
        >
          Cancel
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.lesson-edit-shell {
  display: grid;
  gap: 1.5rem;
}
.form-label {
  display: block;
  margin-bottom: 0.45rem;
  color: #8a8276;
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.22em;
  line-height: 1;
  text-transform: uppercase;
}
.field-input {
  width: 100%;
  border: 2px solid #d4cec6;
  border-radius: 10px;
  background: #f3eee2;
  padding: 0 1.2rem;
  color: #1a1814;
  font-weight: 700;
  outline: none;
  transition: border-color 160ms ease, background-color 160ms ease, box-shadow 160ms ease;
}
.field-input:focus {
  border-color: #1a1814;
  background: #fffdf8;
  box-shadow: 0 0 0 3px rgba(255, 211, 51, 0.25);
}
.content-editor-card {
  overflow: hidden;
  border: 2px solid #1a1814;
  border-radius: 10px;
  background: #fffdf8;
}
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
  overflow: hidden;
  border: 2px solid #d4cec6;
  border-radius: 12px;
  background: #fffdf8;
  padding: 0;
}
.interactive-admin-panel__header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  min-height: 52px;
  border-bottom: 2px solid #d4cec6;
  padding: 0.7rem 1rem;
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
.interaction-type-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  padding: 0.85rem 1rem 0;
}
.interaction-type-button {
  display: inline-flex;
  min-width: 72px;
  min-height: 46px;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 0.2rem;
  border: 2px solid #d4cec6;
  border-radius: 10px;
  background: #fffdf8;
  padding: 0.4rem 0.65rem;
  color: #1a1814;
  font-size: 11px;
  font-weight: 900;
  line-height: 1.1;
  transition: transform 160ms ease, border-color 160ms ease, box-shadow 160ms ease, background-color 160ms ease;
}
.interaction-type-button:hover,
.interaction-type-button.active {
  border-color: #1a1814;
  background: #ffd333;
  box-shadow: 2px 3px 0 #1a1814;
  transform: translateY(-1px);
}
.interaction-type-button__icon {
  font-family: var(--font-math);
  font-size: 16px;
  font-style: italic;
  line-height: 1;
}
:deep(.interactive-config-shell) {
  padding: 0 1rem 1rem;
}
:deep(.studio-tabs) {
  position: absolute;
  right: 1.1rem;
  top: 0.75rem;
  z-index: 2;
  border: 2px solid #d4cec6;
  border-radius: 12px;
  background: #f3eee2;
  padding: 0.25rem;
}
:deep(.studio-tab) {
  min-height: 1.8rem;
  border: 0;
  border-radius: 8px;
  background: transparent;
  padding: 0 0.9rem;
  color: #8a8276;
  font-size: 11px;
}
:deep(.studio-tab.active) {
  background: #1a1814;
  color: #fbf7ef;
}
:deep(.interactive-config-controls > .studio-section:nth-of-type(1)),
:deep(.interactive-config-controls > .studio-section:nth-of-type(2)),
:deep(.interactive-config-controls .mode-row),
:deep(.interactive-config-controls > .studio-section--fields > label.block) {
  display: none;
}
:deep(.studio-section) {
  border: 0;
  background: transparent;
  padding: 0.75rem 0 0;
}
:deep(.studio-section--fields) {
  display: block;
}
:deep(.field span),
:deep(.section-card__header h4),
:deep(.preview-label) {
  color: #8a8276;
  font-family: var(--font-mono);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}
:deep(.field input),
:deep(.field select),
:deep(.field textarea),
:deep(.json-box) {
  border-radius: 10px;
  background: #f3eee2;
  padding: 0.55rem 0.8rem;
  font-size: 13px;
  font-weight: 650;
}
:deep(.editor-grid) {
  gap: 0.75rem;
}
:deep(.quiz-answer-grid) {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
  position: relative;
  padding-top: 42px;
}
:deep(.quiz-answer-card) {
  border: 0;
  background: transparent;
  padding: 0;
  box-shadow: none;
}
:deep(.quiz-answer-card--correct) {
  background: transparent;
  box-shadow: none;
}
:deep(.quiz-answer-card__main) {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
}
:deep(.quiz-answer-card__main input[type='radio']) {
  width: 24px;
  height: 24px;
  accent-color: #3a7d44;
}
:deep(.quiz-answer-input) {
  min-height: 36px;
  border: 2px solid #d4cec6;
  border-radius: 12px;
  background: #f3eee2;
  padding: 0 12px;
  font-size: 12px;
  font-weight: 650;
}
:deep(.quiz-answer-card__meta) {
  display: contents;
  position: static;
}
:deep(.quiz-id-chip),
:deep(.quiz-answer-state) {
  display: none;
}
:deep(.quiz-add-card) {
  position: absolute;
  right: 0;
  top: 0;
  min-height: 32px;
  border: 2px solid #1a1814;
  border-radius: 12px;
  background: #fffdf8;
  padding: 0 12px;
  font-size: 12px;
  font-weight: 900;
}
:deep(.quiz-answer-card__main::after) {
  content: none;
}
:deep(.quiz-answer-card) {
  position: relative;
  min-height: 30px;
  gap: 0;
}
:deep(.quiz-answer-card .mini-button) {
  position: absolute;
  right: 0;
  top: 50%;
  width: 34px;
  min-width: 34px;
  min-height: 34px;
  transform: translateY(-50%);
}
:deep(.quiz-answer-card__main) {
  padding-right: 44px;
  min-height: auto;
}
:deep(.interactive-config-controls .space-y-3 > .field:nth-of-type(1) span),
:deep(.interactive-config-controls .space-y-3 > .field:nth-of-type(2) span),

:deep(.interactive-config-controls .space-y-3 > .field:nth-of-type(1)) {
  display: none;
}
:deep(.interactive-config-controls .space-y-3 > .field:nth-of-type(2) textarea) {
  min-height: 50px;
  border-radius: 12px;
  padding: 10px 12px;
  font-size: 12px;
}
:deep(.interactive-config-controls .space-y-3 > .field:nth-of-type(3)) {
  display: none;
}
:deep(.section-card) {
  border: 0;
  background: transparent;
  padding: 0;
}
:deep(.section-card__header) {
  align-items: center;
  margin-bottom: 0.9rem;
}
:deep(.mini-button) {
  min-height: 36px;
  border: 2px solid #1a1814;
  border-radius: 10px;
  background: #fffdf8;
  padding: 0 0.9rem;
  color: #1a1814;
  font-size: 13px;
  font-weight: 900;
}
:deep(.mini-button:hover),
:deep(.mini-button.active) {
  background: #ffd333;
}
:deep(.nested-row) {
  display: grid;
  grid-template-columns: repeat(6, minmax(64px, 1fr)) 34px;
  align-items: end;
  gap: 8px;
  max-width: 100%;
}
:deep(.visual-editor),
:deep(.formula-builder) {
  grid-template-columns: 1fr;
}
:deep(.visual-editor__layers),
:deep(.visual-editor__properties),
:deep(.formula-builder__list),
:deep(.formula-builder__properties) {
  max-height: none;
}
:deep(.visual-editor__canvas),
:deep(.formula-builder__canvas) {
  min-height: 140px;
}
:deep(.canvas-stage),
:deep(.visual-admin-stage-wrap) {
  min-height: 74px;
}
:deep(.visual-admin-stage) {
  min-height: 74px;
}
:deep(.preset-grid),
:deep(.tool-row) {
  gap: 0.45rem;
}
:deep(.preset-button),
:deep(.tool-row .mini-button) {
  min-height: 32px;
  border-radius: 9px;
  padding: 0 0.8rem;
  font-size: 11px;
}
:deep(.visual-editor) {
  gap: 0.7rem;
}
:deep(.visual-editor__layers),
:deep(.visual-editor__canvas),
:deep(.visual-editor__properties) {
  padding: 0;
}
:deep(.formula-builder) {
  display: none;
}
:deep(.interactive-config-controls .editor-grid:has(+ .section-card)) {
  display: block;
}
:deep(.interactive-config-controls .editor-grid:has(+ .section-card) .field:not(.field--wide)) {
  display: none;
}
:deep(.interactive-config-controls .editor-grid:has(+ .section-card) .field--wide:first-child) {
  display: none;
}
:deep(.interactive-config-controls .editor-grid:has(+ .section-card) .field--wide span) {
  font-size: 10px;
  letter-spacing: 0.22em;
}
:deep(.interactive-config-controls .editor-grid:has(+ .section-card) .field--wide input) {
  min-height: 42px;
  border-radius: 12px;
  padding: 0 12px;
  font-family: var(--font-math);
  font-size: 16px;
  font-style: italic;
  font-weight: 500;
}
:deep(.section-card) {
  padding-bottom: 8px;
}
:deep(.section-card__header) {
  min-height: 34px;
}
:deep(.section-card__header h4) {
  font-size: 10px;
  letter-spacing: 0.22em;
}
:deep(.section-card__header p) {
  display: none;
}
:deep(.section-card__header .mini-button) {
  margin-left: auto;
  min-height: 30px;
  border-radius: 9px;
  padding: 0 10px;
  background: #fffdf8;
  font-size: 11px;
}
:deep(.section-card .space-y-2) {
  display: grid;
  gap: 8px;
}
:deep(.section-card .nested-row .field span) {
  margin-bottom: 5px;
  font-size: 9px;
  letter-spacing: 0.18em;
}
:deep(.section-card .nested-row .field input) {
  min-height: 34px;
  border-radius: 8px;
  padding: 0 10px;
  font-size: 12px;
  font-weight: 650;
}
:deep(.section-card .nested-row .mini-button) {
  width: 32px;
  min-width: 32px;
  min-height: 32px;
  color: #8a8276;
  border-color: #d4cec6;
  background: #fffdf8;
  padding: 0;
}
@media (max-width: 1180px) {
  :deep(.nested-row) {
    grid-template-columns: repeat(3, minmax(90px, 1fr)) 42px;
  }
}
:deep(.empty-note) {
  color: #8a8276;
  font-family: var(--font-mono);
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.12em;
  text-transform: uppercase;
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

/* Compact interactive editor overrides */
.interactive-admin-panel__header {
  min-height: 46px;
  padding: 0.55rem 0.85rem;
}
.interaction-type-grid {
  padding: 0.65rem 0.85rem 0;
}
:deep(.interactive-config-shell) {
  padding: 0 0.85rem 0.85rem;
}
:deep(.studio-section) {
  padding-top: 0.55rem;
}
:deep(.field span),
:deep(.section-card__header h4),
:deep(.preview-label) {
  font-size: 10px;
}
:deep(.editor-grid) {
  gap: 0.5rem;
}
:deep(.quiz-answer-grid) {
  gap: 8px;
  padding-top: 36px;
}
:deep(.quiz-answer-card),
:deep(.quiz-answer-card--correct) {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr) 32px;
  align-items: center;
  gap: 8px;
  border: 0;
  background: transparent;
  box-shadow: none;
}
:deep(.quiz-answer-card__main) {
  display: contents;
}
:deep(.quiz-answer-card__main input[type='radio']) {
  width: 20px;
  height: 20px;
  accent-color: #3a7d44;
}
:deep(.quiz-answer-input) {
  min-height: 30px;
  border-radius: 9px;
  font-size: 12px;
}
:deep(.quiz-add-card) {
  min-height: 30px;
  border-radius: 10px;
  font-size: 12px;
}
:deep(.quiz-answer-card .mini-button) {
  position: static;
  width: 30px;
  min-width: 30px;
  min-height: 30px;
  border-color: #d4cec6;
  background: #fffdf8;
  padding: 0;
  color: #8a8276;
  transform: none;
}
:deep(.quiz-answer-card__meta) {
  display: contents;
}
:deep(.mini-button--icon) {
  display: inline-grid;
  place-items: center;
  aspect-ratio: 1;
  padding: 0;
}
:deep(.mini-button--icon svg) {
  width: 15px;
  height: 15px;
  fill: none;
  stroke: currentColor;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}
:deep(.interactive-config-controls .space-y-3 > .field:nth-of-type(2) textarea) {
  min-height: 40px;
  padding: 8px 10px;
}
:deep(.field input),
:deep(.field select),
:deep(.field textarea),
:deep(.json-box) {
  border-radius: 8px;
  padding: 0.45rem 0.65rem;
  font-size: 12px;
}
:deep(.visual-admin-stage-wrap),
:deep(.canvas-stage) {
  border-radius: 10px;
  padding: 0.65rem;
}
</style>
