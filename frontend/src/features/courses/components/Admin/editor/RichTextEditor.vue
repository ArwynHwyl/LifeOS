<template>
  <div class="border-2 border-lm-line rounded-[12px] bg-lm-surface overflow-hidden">
    <!-- Toolbar -->
    <div class="flex flex-wrap items-center gap-[2px] p-[6px_8px] border-b-2 border-lm-line-soft bg-lm-bg-soft">
      <select
        :value="activeBlock"
        @mousedown="saveSelection"
        @change="onBlockChange"
        class="h-7 rounded-[6px] border-2 border-lm-line-soft bg-lm-surface px-2 text-[11px] font-semibold text-lm-ink outline-none cursor-pointer"
      >
        <option value="p">Paragraph</option>
        <option value="h2">Heading 2</option>
        <option value="h3">Heading 3</option>
        <option value="pre">Code Block</option>
      </select>
      <div class="w-[1px] h-5 bg-lm-line-soft mx-1" />
      <button
        type="button"
        @mousedown.prevent="exec('bold')"
        class="h-8 min-w-[32px] px-2 rounded-[6px] border-none bg-transparent text-lm-ink-2 font-display text-[12px] font-bold cursor-pointer transition-colors duration-120 hover:bg-lm-line-soft"
        :class="{ 'bg-lm-yellow text-lm-ink hover:bg-lm-yellow': activeFormats.bold }"
        title="Bold"
      >
        B
      </button>
      <button
        type="button"
        @mousedown.prevent="exec('underline')"
        class="h-8 min-w-[32px] px-2 rounded-[6px] border-none bg-transparent text-lm-ink-2 font-display text-[12px] underline font-bold cursor-pointer transition-colors duration-120 hover:bg-lm-line-soft"
        :class="{ 'bg-lm-yellow text-lm-ink hover:bg-lm-yellow': activeFormats.underline }"
        title="Underline"
      >
        U
      </button>
      <button
        type="button"
        @mousedown.prevent="insertLink"
        class="h-8 min-w-[32px] px-2 rounded-[6px] border-none bg-transparent text-lm-ink-2 font-display text-[12px] font-bold cursor-pointer transition-colors duration-120 hover:bg-lm-line-soft"
        title="Insert link"
      >
        Link
      </button>
      <div class="w-[1px] h-5 bg-lm-line-soft mx-1" />
      <button
        type="button"
        @mousedown.prevent="exec('insertUnorderedList')"
        class="h-8 min-w-[32px] px-2 rounded-[6px] border-none bg-transparent text-lm-ink-2 font-display text-[12px] font-bold cursor-pointer transition-colors duration-120 hover:bg-lm-line-soft"
        :class="{ 'bg-lm-yellow text-lm-ink hover:bg-lm-yellow': activeFormats.bulletList }"
        title="Bullet list"
      >
        • List
      </button>
      <button
        type="button"
        @mousedown.prevent="exec('insertOrderedList')"
        class="h-8 min-w-[32px] px-2 rounded-[6px] border-none bg-transparent text-lm-ink-2 font-display text-[12px] font-bold cursor-pointer transition-colors duration-120 hover:bg-lm-line-soft"
        :class="{ 'bg-lm-yellow text-lm-ink hover:bg-lm-yellow': activeFormats.orderedList }"
        title="Numbered list"
      >
        1. List
      </button>
      <div class="w-[1px] h-5 bg-lm-line-soft mx-1" />
      <button
        type="button"
        @mousedown.prevent="insertImage"
        class="h-8 min-w-[32px] px-2 rounded-[6px] border-none bg-transparent text-lm-ink-2 font-display text-[12px] font-bold cursor-pointer transition-colors duration-120 hover:bg-lm-line-soft disabled:cursor-not-allowed disabled:opacity-50"
        :disabled="isUploading"
        title="Insert image"
      >
        {{ isUploading ? 'Uploading…' : 'Img' }}
      </button>
    </div>
    <!-- Editable area -->
    <div
      ref="editorRef"
      contenteditable="true"
      @input="onInput"
      @keyup="updateState"
      @mouseup="updateState"
      @focus="updateState"
      class="lm-rich-editor-body min-h-[200px] px-4 py-3.5 outline-none text-[13px] leading-[1.7] text-lm-ink-2 text-left"
      data-placeholder="Write lesson content here…"
    />
    <input
      type="file"
      ref="fileInputRef"
      accept="image/png, image/jpeg, image/webp"
      class="hidden"
      @change="handleImageFileChange"
    />
  </div>
</template>

<style>
/* Restore browser defaults that Tailwind preflight strips inside the rich editor */
.lm-rich-editor-body:empty::before {
  content: attr(data-placeholder);
  color: var(--color-lm-ink-3, #aaa);
  cursor: text;
  pointer-events: none;
}
.lm-rich-editor-body b,
.lm-rich-editor-body strong {
  font-weight: 700;
}
.lm-rich-editor-body i,
.lm-rich-editor-body em {
  font-style: italic;
}
.lm-rich-editor-body u {
  text-decoration: underline;
}
.lm-rich-editor-body h2 {
  font-size: 1.2rem;
  font-weight: 700;
  line-height: 1.4;
  margin: 0.6rem 0 0.3rem;
}
.lm-rich-editor-body h3 {
  font-size: 1.05rem;
  font-weight: 700;
  line-height: 1.4;
  margin: 0.5rem 0 0.25rem;
}
.lm-rich-editor-body ul {
  list-style-type: disc;
  padding-left: 1.4rem;
  margin: 0.35rem 0;
}
.lm-rich-editor-body ol {
  list-style-type: decimal;
  padding-left: 1.4rem;
  margin: 0.35rem 0;
}
.lm-rich-editor-body li {
  margin: 0.15rem 0;
}
.lm-rich-editor-body pre {
  white-space: pre-wrap;
  word-break: break-word;
  overflow-x: auto;
  background: var(--color-lm-bg-soft, #f5f5f5);
  padding: 0.6rem 0.875rem;
  border-radius: 6px;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12px;
  line-height: 1.6;
  margin: 0.4rem 0;
}
.lm-rich-editor-body a {
  color: #6366f1;
  text-decoration: underline;
}
.lm-rich-editor-body img {
  max-width: 100%;
  height: auto;
  border-radius: 8px;
  display: block;
  margin: 0.5rem 0;
}
.lm-rich-editor-body p {
  margin: 0.25rem 0;
}
</style>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { uploadSubTopicImage } from '@/features/courses/services/adminCourses'

const props = defineProps({
  modelValue: { type: String, default: '' },
  subTopicId: { type: [Number, String], default: null }
})

const emit = defineEmits(['update:modelValue'])

const editorRef = ref(null)
const fileInputRef = ref(null)
const isUploading = ref(false)

const activeBlock = ref('p')
const activeFormats = ref({
  bold: false,
  underline: false,
  bulletList: false,
  orderedList: false
})

// Saved Selection Range for restoring after interacting with toolbar controls that steal focus
let _savedRange = null

onMounted(() => {
  if (editorRef.value) {
    editorRef.value.innerHTML = props.modelValue || ''
  }
})

watch(() => props.modelValue, (newVal) => {
  if (editorRef.value && editorRef.value.innerHTML !== newVal) {
    editorRef.value.innerHTML = newVal || ''
  }
})

// ── Selection helpers ──────────────────────────────────────────────────────

function saveSelection() {
  const sel = window.getSelection()
  if (sel && sel.rangeCount > 0) {
    _savedRange = sel.getRangeAt(0).cloneRange()
  }
}

function restoreSelection() {
  if (!_savedRange) return
  editorRef.value?.focus()
  const sel = window.getSelection()
  if (sel) {
    sel.removeAllRanges()
    sel.addRange(_savedRange)
  }
}

// ── Core exec ─────────────────────────────────────────────────────────────

function exec(cmd, val = null) {
  // execCommand must run BEFORE focus() — calling focus() resets the selection
  document.execCommand(cmd, false, val)
  editorRef.value?.focus()
  updateState()
  syncModel()
}

function syncModel() {
  if (editorRef.value) {
    emit('update:modelValue', editorRef.value.innerHTML)
  }
}

function updateState() {
  try {
    activeFormats.value = {
      bold: document.queryCommandState('bold'),
      underline: document.queryCommandState('underline'),
      bulletList: document.queryCommandState('insertUnorderedList'),
      orderedList: document.queryCommandState('insertOrderedList')
    }
    const raw = document.queryCommandValue('formatBlock').toLowerCase()
    // Chrome uses 'div' as default block; map to 'p' for the select
    activeBlock.value = (raw === '' || raw === 'div' || raw === 'normal') ? 'p' : raw
  } catch {
    // queryCommandState/Value can throw in some edge cases
  }
}

function onInput() {
  syncModel()
  updateState()
}

// ── Block type ─────────────────────────────────────────────────────────────

function onBlockChange(e) {
  const v = e.target.value
  restoreSelection()
  if (v === 'h2') exec('formatBlock', '<h2>')
  else if (v === 'h3') exec('formatBlock', '<h3>')
  else if (v === 'pre') exec('formatBlock', '<pre>')
  else exec('formatBlock', '<p>')
}

// ── Inline helpers ─────────────────────────────────────────────────────────

function insertLink() {
  const url = window.prompt('URL:', 'https://')
  if (url) exec('createLink', url)
}

// ── Image upload ───────────────────────────────────────────────────────────

function insertImage() {
  if (isUploading.value) return
  fileInputRef.value?.click()
}

async function handleImageFileChange(event) {
  const file = event.target?.files?.[0]
  if (!file) return
  event.target.value = ''

  const allowedTypes = ['image/png', 'image/jpeg', 'image/webp']
  if (!allowedTypes.includes(file.type)) {
    window.alert('Only PNG, JPEG, and WebP images are supported.')
    return
  }
  if (file.size > 10 * 1024 * 1024) {
    window.alert('Subtopic images must be 10 MB or smaller.')
    return
  }
  if (!props.subTopicId) {
    window.alert('Cannot upload image: save the subtopic first to get an ID.')
    return
  }

  isUploading.value = true
  try {
    const asset = await uploadSubTopicImage(props.subTopicId, file)
    if (asset?.id && asset?.fileUrl) {
      const imgHtml = `<img data-asset-id="${asset.id}" src="${asset.fileUrl}" alt="${file.name.replace(/\.[^/.]+$/, '')}" />`
      exec('insertHTML', imgHtml)
    } else {
      throw new Error('Upload succeeded but asset details are incomplete.')
    }
  } catch (error) {
    window.alert(`Image upload failed: ${error instanceof Error ? error.message : 'Unknown error'}`)
  } finally {
    isUploading.value = false
  }
}
</script>
