<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'

const props = defineProps<{
  name: string
  content: string
  courseTitle: string
}>()

const emit = defineEmits<{
  save: [{ name: string; content: string }]
  cancel: []
}>()

const titleRef = ref<HTMLElement | null>(null)
const editorRef = ref<HTMLElement | null>(null)
const imageInputRef = ref<HTMLInputElement | null>(null)
const activeFormats = ref(new Set<string>())
const currentBlock = ref('div')

const showImagePicker = ref(false)
const imageTab = ref<'upload' | 'url'>('upload')
const imageUrlInput = ref('')

onMounted(() => {
  if (titleRef.value) titleRef.value.textContent = props.name
  if (editorRef.value) {
    const isHtml = /<[a-z][\s\S]*?>/i.test(props.content)
    editorRef.value.innerHTML = isHtml
      ? props.content
      : props.content
          .replace(/&/g, '&amp;')
          .replace(/</g, '&lt;')
          .replace(/>/g, '&gt;')
          .replace(/\n/g, '<br>')
    editorRef.value.focus()
  }
  document.addEventListener('selectionchange', refreshFormats)
})

onBeforeUnmount(() => {
  document.removeEventListener('selectionchange', refreshFormats)
})

function refreshFormats() {
  const cmds = [
    'bold', 'italic', 'underline', 'strikeThrough',
    'justifyLeft', 'justifyCenter', 'justifyRight',
    'insertUnorderedList', 'insertOrderedList',
  ]
  const active = new Set<string>()
  cmds.forEach((cmd) => {
    try { if (document.queryCommandState(cmd)) active.add(cmd) } catch {}
  })
  activeFormats.value = active
  try {
    currentBlock.value = document.queryCommandValue('formatBlock').toLowerCase() || 'div'
  } catch {}
}

function execCmd(cmd: string, value?: string) {
  document.execCommand(cmd, false, value)
}

function setBlock(e: Event) {
  const val = (e.target as HTMLSelectElement).value
  document.execCommand('formatBlock', false, val)
  editorRef.value?.focus()
}

function onTitleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter') {
    e.preventDefault()
    editorRef.value?.focus()
  }
}

function insertImage(src: string) {
  editorRef.value?.focus()
  document.execCommand(
    'insertHTML',
    false,
    `<img src="${src}" style="max-width:100%;height:auto;border-radius:8px;margin:12px 0;display:block;" />`,
  )
}

function onImageFileChange(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = (ev) => {
    insertImage(ev.target?.result as string)
    showImagePicker.value = false
    if (imageInputRef.value) imageInputRef.value.value = ''
  }
  reader.readAsDataURL(file)
}

function insertImageFromUrl() {
  const url = imageUrlInput.value.trim()
  if (!url) return
  insertImage(url)
  imageUrlInput.value = ''
  showImagePicker.value = false
}

function onSave() {
  emit('save', {
    name: titleRef.value?.textContent?.trim() ?? props.name,
    content: editorRef.value?.innerHTML ?? props.content,
  })
}
</script>

<template>
  <div class="flex min-w-0 flex-1 flex-col overflow-hidden">

    <!-- Top bar: breadcrumb + Save/Cancel -->
    <div class="flex shrink-0 items-center justify-between border-b border-slate-200/70 bg-white px-7 py-3">
      <div class="flex items-center gap-1.5 text-[12px]">
        <span class="font-medium text-slate-400">{{ courseTitle }}</span>
        <svg class="h-3.5 w-3.5 text-slate-300" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="9 18 15 12 9 6" />
        </svg>
        <span class="font-semibold text-slate-700">Editing topic</span>
      </div>
      <div class="flex items-center gap-2">
        <button
          type="button"
          class="rounded-lg border border-slate-200 bg-white px-3.5 py-1.5 text-[12px] font-semibold text-slate-600 transition hover:bg-slate-50"
          @click="emit('cancel')"
        >
          Cancel
        </button>
        <button
          type="button"
          class="rounded-lg bg-[#5b4cfa] px-3.5 py-1.5 text-[12px] font-semibold text-white shadow-sm shadow-[#5b4cfa]/30 transition hover:bg-[#4d3ee0] active:scale-[0.98]"
          @click="onSave"
        >
          Save
        </button>
      </div>
    </div>

    <!-- Formatting toolbar -->
    <div class="flex shrink-0 flex-wrap items-center gap-0.5 border-b border-slate-200/70 bg-white px-4 py-1.5">

      <!-- Block format -->
      <select
        :value="currentBlock"
        class="mr-1 cursor-pointer rounded-md border border-slate-200 bg-white py-1 pl-2 pr-5 text-[11.5px] text-slate-700 outline-none transition focus:border-[#5b4cfa]/40 focus:ring-1 focus:ring-[#5b4cfa]/20"
        @change="setBlock"
        @mousedown.stop
      >
        <option value="div">Paragraph</option>
        <option value="h1">Heading 1</option>
        <option value="h2">Heading 2</option>
        <option value="h3">Heading 3</option>
      </select>

      <div class="mx-1 h-5 w-px bg-slate-200" />

      <!-- Bold -->
      <button
        type="button"
        class="flex h-7 w-7 items-center justify-center rounded-md text-[13px] font-bold transition"
        :class="activeFormats.has('bold') ? 'bg-slate-100 text-[#5b4cfa]' : 'text-slate-500 hover:bg-slate-100 hover:text-slate-700'"
        title="Bold (Ctrl+B)"
        @mousedown.prevent="execCmd('bold')"
      >B</button>

      <!-- Italic -->
      <button
        type="button"
        class="flex h-7 w-7 items-center justify-center rounded-md text-[13px] font-bold italic transition"
        :class="activeFormats.has('italic') ? 'bg-slate-100 text-[#5b4cfa]' : 'text-slate-500 hover:bg-slate-100 hover:text-slate-700'"
        title="Italic (Ctrl+I)"
        @mousedown.prevent="execCmd('italic')"
      >I</button>

      <!-- Underline -->
      <button
        type="button"
        class="flex h-7 w-7 items-center justify-center rounded-md text-[13px] font-bold underline transition"
        :class="activeFormats.has('underline') ? 'bg-slate-100 text-[#5b4cfa]' : 'text-slate-500 hover:bg-slate-100 hover:text-slate-700'"
        title="Underline (Ctrl+U)"
        @mousedown.prevent="execCmd('underline')"
      >U</button>

      <!-- Strikethrough -->
      <button
        type="button"
        class="flex h-7 w-7 items-center justify-center rounded-md text-[13px] font-bold line-through transition"
        :class="activeFormats.has('strikeThrough') ? 'bg-slate-100 text-[#5b4cfa]' : 'text-slate-500 hover:bg-slate-100 hover:text-slate-700'"
        title="Strikethrough"
        @mousedown.prevent="execCmd('strikeThrough')"
      >S</button>

      <div class="mx-1 h-5 w-px bg-slate-200" />

      <!-- Align Left -->
      <button
        type="button"
        class="flex h-7 w-7 items-center justify-center rounded-md transition"
        :class="activeFormats.has('justifyLeft') ? 'bg-slate-100 text-[#5b4cfa]' : 'text-slate-500 hover:bg-slate-100 hover:text-slate-700'"
        title="Align left"
        @mousedown.prevent="execCmd('justifyLeft')"
      >
        <svg class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="3" y1="6" x2="21" y2="6" /><line x1="3" y1="12" x2="15" y2="12" /><line x1="3" y1="18" x2="18" y2="18" />
        </svg>
      </button>

      <!-- Align Center -->
      <button
        type="button"
        class="flex h-7 w-7 items-center justify-center rounded-md transition"
        :class="activeFormats.has('justifyCenter') ? 'bg-slate-100 text-[#5b4cfa]' : 'text-slate-500 hover:bg-slate-100 hover:text-slate-700'"
        title="Align center"
        @mousedown.prevent="execCmd('justifyCenter')"
      >
        <svg class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="3" y1="6" x2="21" y2="6" /><line x1="6" y1="12" x2="18" y2="12" /><line x1="4" y1="18" x2="20" y2="18" />
        </svg>
      </button>

      <!-- Align Right -->
      <button
        type="button"
        class="flex h-7 w-7 items-center justify-center rounded-md transition"
        :class="activeFormats.has('justifyRight') ? 'bg-slate-100 text-[#5b4cfa]' : 'text-slate-500 hover:bg-slate-100 hover:text-slate-700'"
        title="Align right"
        @mousedown.prevent="execCmd('justifyRight')"
      >
        <svg class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="3" y1="6" x2="21" y2="6" /><line x1="9" y1="12" x2="21" y2="12" /><line x1="6" y1="18" x2="21" y2="18" />
        </svg>
      </button>

      <div class="mx-1 h-5 w-px bg-slate-200" />

      <!-- Bullet list -->
      <button
        type="button"
        class="flex h-7 w-7 items-center justify-center rounded-md transition"
        :class="activeFormats.has('insertUnorderedList') ? 'bg-slate-100 text-[#5b4cfa]' : 'text-slate-500 hover:bg-slate-100 hover:text-slate-700'"
        title="Bullet list"
        @mousedown.prevent="execCmd('insertUnorderedList')"
      >
        <svg class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="9" y1="6" x2="20" y2="6" /><line x1="9" y1="12" x2="20" y2="12" /><line x1="9" y1="18" x2="20" y2="18" />
          <circle cx="4" cy="6" r="1.5" fill="currentColor" stroke="none" />
          <circle cx="4" cy="12" r="1.5" fill="currentColor" stroke="none" />
          <circle cx="4" cy="18" r="1.5" fill="currentColor" stroke="none" />
        </svg>
      </button>

      <!-- Numbered list -->
      <button
        type="button"
        class="flex h-7 w-7 items-center justify-center rounded-md transition"
        :class="activeFormats.has('insertOrderedList') ? 'bg-slate-100 text-[#5b4cfa]' : 'text-slate-500 hover:bg-slate-100 hover:text-slate-700'"
        title="Numbered list"
        @mousedown.prevent="execCmd('insertOrderedList')"
      >
        <svg class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="10" y1="6" x2="21" y2="6" /><line x1="10" y1="12" x2="21" y2="12" /><line x1="10" y1="18" x2="21" y2="18" />
          <text x="2" y="8" font-size="7" fill="currentColor" stroke="none" font-weight="bold">1.</text>
          <text x="2" y="14" font-size="7" fill="currentColor" stroke="none" font-weight="bold">2.</text>
          <text x="2" y="20" font-size="7" fill="currentColor" stroke="none" font-weight="bold">3.</text>
        </svg>
      </button>

      <div class="mx-1 h-5 w-px bg-slate-200" />

      <!-- Undo -->
      <button
        type="button"
        class="flex h-7 w-7 items-center justify-center rounded-md text-slate-500 transition hover:bg-slate-100 hover:text-slate-700"
        title="Undo (Ctrl+Z)"
        @mousedown.prevent="execCmd('undo')"
      >
        <svg class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M3 7v6h6" /><path d="M3 13C5 8 9.5 5 15 5c4.5 0 8 2.5 9 7" />
        </svg>
      </button>

      <!-- Redo -->
      <button
        type="button"
        class="flex h-7 w-7 items-center justify-center rounded-md text-slate-500 transition hover:bg-slate-100 hover:text-slate-700"
        title="Redo (Ctrl+Y)"
        @mousedown.prevent="execCmd('redo')"
      >
        <svg class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M21 7v6h-6" /><path d="M21 13C19 8 14.5 5 9 5c-4.5 0-8 2.5-9 7" />
        </svg>
      </button>

      <div class="mx-1 h-5 w-px bg-slate-200" />

      <!-- Image insert -->
      <div class="relative">
        <button
          type="button"
          class="flex h-7 w-7 items-center justify-center rounded-md transition"
          :class="showImagePicker ? 'bg-slate-100 text-[#5b4cfa]' : 'text-slate-500 hover:bg-slate-100 hover:text-slate-700'"
          title="Insert image"
          @mousedown.prevent="showImagePicker = !showImagePicker"
        >
          <svg class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
            <circle cx="8.5" cy="8.5" r="1.5" />
            <polyline points="21 15 16 10 5 21" />
          </svg>
        </button>

        <!-- Image picker dropdown -->
        <div
          v-if="showImagePicker"
          class="absolute left-0 top-full z-30 mt-1.5 w-72 overflow-hidden rounded-xl border border-slate-200 bg-white shadow-xl shadow-slate-900/10"
          @mousedown.stop
        >
          <!-- Tabs -->
          <div class="flex border-b border-slate-100">
            <button
              type="button"
              class="flex-1 py-2.5 text-[11.5px] font-semibold transition"
              :class="imageTab === 'upload' ? 'text-[#5b4cfa] border-b-2 border-[#5b4cfa] -mb-px' : 'text-slate-500 hover:text-slate-700'"
              @click="imageTab = 'upload'"
            >Upload</button>
            <button
              type="button"
              class="flex-1 py-2.5 text-[11.5px] font-semibold transition"
              :class="imageTab === 'url' ? 'text-[#5b4cfa] border-b-2 border-[#5b4cfa] -mb-px' : 'text-slate-500 hover:text-slate-700'"
              @click="imageTab = 'url'"
            >From URL</button>
          </div>

          <!-- Upload tab -->
          <div v-if="imageTab === 'upload'" class="p-3">
            <input ref="imageInputRef" type="file" accept="image/*" class="sr-only" @change="onImageFileChange" />
            <button
              type="button"
              class="flex w-full flex-col items-center gap-2 rounded-xl border-2 border-dashed border-slate-200 bg-slate-50/50 py-6 text-center transition hover:border-[#5b4cfa]/40 hover:bg-[#5b4cfa]/[0.02]"
              @click="imageInputRef?.click()"
            >
              <svg class="h-7 w-7 text-slate-300" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
                <circle cx="8.5" cy="8.5" r="1.5" />
                <polyline points="21 15 16 10 5 21" />
              </svg>
              <span class="text-[12px] font-medium text-slate-500">Click to upload image</span>
              <span class="text-[11px] text-slate-400">PNG, JPG, GIF, WebP</span>
            </button>
          </div>

          <!-- URL tab -->
          <div v-else class="p-3">
            <p class="mb-2 text-[11px] font-semibold text-slate-500">Image URL</p>
            <input
              v-model="imageUrlInput"
              type="url"
              placeholder="https://example.com/image.png"
              class="w-full rounded-lg border border-slate-200 bg-slate-50/50 px-3 py-2 text-[12px] text-slate-700 outline-none transition placeholder:text-slate-400 focus:border-[#5b4cfa]/50 focus:bg-white focus:ring-2 focus:ring-[#5b4cfa]/10"
              @keydown.enter.prevent="insertImageFromUrl"
            />
            <button
              type="button"
              class="mt-2 w-full rounded-lg bg-[#5b4cfa] py-2 text-[12px] font-semibold text-white transition hover:bg-[#4d3ee0] disabled:opacity-40"
              :disabled="!imageUrlInput.trim()"
              @click="insertImageFromUrl"
            >
              Insert
            </button>
          </div>
        </div>
      </div>

      <!-- Hidden backdrop to close picker -->
      <div v-if="showImagePicker" class="fixed inset-0 z-20" @mousedown="showImagePicker = false" />
    </div>

    <!-- Document area -->
    <div class="flex-1 overflow-y-auto bg-[#f0f0f0] px-8 py-10">
      <div class="mx-auto max-w-[740px]">
        <div class="min-h-[680px] rounded-sm bg-white px-[72px] py-16 shadow-[0_1px_3px_rgba(0,0,0,0.10),0_4px_20px_rgba(0,0,0,0.07)]">

          <!-- Editable topic title -->
          <div
            ref="titleRef"
            contenteditable="true"
            role="heading"
            aria-level="1"
            data-placeholder="Untitled"
            class="topic-title mb-7 border-b border-slate-200 pb-5 text-[26px] font-bold leading-snug text-slate-900 outline-none"
            @keydown="onTitleKeydown"
          />

          <!-- Editable content -->
          <div
            ref="editorRef"
            contenteditable="true"
            data-placeholder="Start writing…"
            class="editor-content min-h-[400px] text-[14px] leading-[1.85] text-slate-700 outline-none"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Placeholder via data attribute + :empty */
[contenteditable][data-placeholder]:empty::before {
  content: attr(data-placeholder);
  color: #94a3b8;
  pointer-events: none;
}

/* Editor content heading/list styles */
.editor-content :deep(h1) {
  font-size: 1.75rem;
  font-weight: 700;
  color: #1e293b;
  margin: 1.25rem 0 0.5rem;
  line-height: 1.25;
}
.editor-content :deep(h2) {
  font-size: 1.375rem;
  font-weight: 700;
  color: #1e293b;
  margin: 1rem 0 0.375rem;
  line-height: 1.3;
}
.editor-content :deep(h3) {
  font-size: 1.125rem;
  font-weight: 600;
  color: #334155;
  margin: 0.75rem 0 0.25rem;
  line-height: 1.35;
}
.editor-content :deep(ul) {
  list-style-type: disc;
  padding-left: 1.5rem;
  margin: 0.5rem 0;
}
.editor-content :deep(ol) {
  list-style-type: decimal;
  padding-left: 1.5rem;
  margin: 0.5rem 0;
}
.editor-content :deep(li) {
  margin-bottom: 0.2rem;
}
.editor-content :deep(p) {
  margin: 0.2rem 0;
}
.editor-content :deep(blockquote) {
  border-left: 3px solid #e2e8f0;
  padding-left: 1rem;
  color: #64748b;
  margin: 0.75rem 0;
}
.editor-content :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 8px;
  margin: 12px 0;
  display: block;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}
</style>
