<template>
  <div class="border-2 border-lm-line rounded-[12px] bg-lm-surface overflow-hidden">
    <!-- Toolbar -->
    <div class="flex flex-wrap items-center gap-[2px] p-[6px_8px] border-b-2 border-lm-line-soft bg-lm-bg-soft">
      <select @change="onBlockChange" class="h-7 rounded-[6px] border-2 border-lm-line-soft bg-lm-surface px-2 text-[11px] font-semibold text-lm-ink outline-none cursor-pointer">
        <option value="p">Paragraph</option>
        <option value="h2">Heading 2</option>
        <option value="h3">Heading 3</option>
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
        @mousedown.prevent="exec('italic')"
        class="h-8 min-w-[32px] px-2 rounded-[6px] border-none bg-transparent text-lm-ink-2 font-display text-[12px] font-bold cursor-pointer transition-colors duration-120 hover:bg-lm-line-soft"
        :class="{ 'bg-lm-yellow text-lm-ink hover:bg-lm-yellow': activeFormats.italic }"
        title="Italic"
      >
        I
      </button>
      <button
        type="button"
        @mousedown.prevent="exec('formatBlock', '<pre>')"
        class="h-8 min-w-[32px] px-2 rounded-[6px] border-none bg-transparent text-lm-ink-2 font-display text-[12px] font-bold cursor-pointer transition-colors duration-120 hover:bg-lm-line-soft"
        title="Code block"
      >
        &lt;/&gt;
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
        List
      </button>
      <button
        type="button"
        @mousedown.prevent="exec('insertOrderedList')"
        class="h-8 min-w-[32px] px-2 rounded-[6px] border-none bg-transparent text-lm-ink-2 font-display text-[12px] font-bold cursor-pointer transition-colors duration-120 hover:bg-lm-line-soft"
        :class="{ 'bg-lm-yellow text-lm-ink hover:bg-lm-yellow': activeFormats.orderedList }"
        title="Numbered list"
      >
        1.
      </button>
      <button
        type="button"
        @mousedown.prevent="insertImage"
        class="h-8 min-w-[32px] px-2 rounded-[6px] border-none bg-transparent text-lm-ink-2 font-display text-[12px] font-bold cursor-pointer transition-colors duration-120 hover:bg-lm-line-soft disabled:cursor-not-allowed disabled:opacity-50"
        :disabled="isUploading"
        title="Insert image"
      >
        {{ isUploading ? 'Uploading...' : 'Img' }}
      </button>
    </div>
    <!-- Editable area -->
    <div
      ref="editorRef"
      contenteditable="true"
      @input="onInput"
      @keyup="updateFormats"
      @mouseup="updateFormats"
      class="min-h-[200px] px-4 py-3.5 outline-none text-[13px] leading-[1.7] text-lm-ink-2 text-left empty:before:content-[attr(data-placeholder)] empty:before:text-lm-ink-3 empty:before:cursor-text"
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

<script setup>
import { ref, onMounted, watch } from 'vue'
import { uploadSubTopicImage } from '@/features/courses/services/adminCourses'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  subTopicId: {
    type: [Number, String],
    default: null
  }
})

const emit = defineEmits(['update:modelValue'])

const fileInputRef = ref(null)
const isUploading = ref(false)

const editorRef = ref(null)
const activeFormats = ref({
  bold: false,
  italic: false,
  bulletList: false,
  orderedList: false
})

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

function exec(cmd, val = null) {
  document.execCommand(cmd, false, val)
  editorRef.value?.focus()
  updateFormats()
}

function updateFormats() {
  activeFormats.value = {
    bold: document.queryCommandState('bold'),
    italic: document.queryCommandState('italic'),
    bulletList: document.queryCommandState('insertUnorderedList'),
    orderedList: document.queryCommandState('insertOrderedList')
  }
}

function onBlockChange(e) {
  const v = e.target.value
  if (v === 'h2') exec('formatBlock', '<h2>')
  else if (v === 'h3') exec('formatBlock', '<h3>')
  else exec('formatBlock', '<p>')
}

function insertLink() {
  const url = window.prompt('URL:', 'https://')
  if (url) exec('createLink', url)
}

function insertImage() {
  if (isUploading.value) return
  fileInputRef.value?.click()
}

async function handleImageFileChange(event) {
  const file = event.target?.files?.[0]
  if (!file) return

  // Reset file input value so same file can be selected again
  event.target.value = ''

  // Local client validation
  const allowedTypes = ['image/png', 'image/jpeg', 'image/webp']
  if (!allowedTypes.includes(file.type)) {
    window.alert('Only PNG, JPEG, and WebP images are supported')
    return
  }

  const maxSizeBytes = 10 * 1024 * 1024
  if (file.size > maxSizeBytes) {
    window.alert('Subtopic images must be 10MB or smaller')
    return
  }

  const subTopicId = props.subTopicId
  if (!subTopicId) {
    window.alert('Cannot upload image: Subtopic ID is missing. Please save the subtopic first.')
    return
  }

  isUploading.value = true
  try {
    const asset = await uploadSubTopicImage(subTopicId, file)
    if (asset && asset.id && asset.fileUrl) {
      // Focus editor back
      editorRef.value?.focus()
      // Insert image with data-asset-id to prevent sanitize check removal on backend
      const imgHtml = `<img data-asset-id="${asset.id}" src="${asset.fileUrl}" alt="${file.name.replace(/\.[^/.]+$/, '')}" style="max-width: 100%; height: auto; border-radius: 8px;" />`
      exec('insertHTML', imgHtml)
    } else {
      throw new Error('Upload succeeded but asset details are incomplete')
    }
  } catch (error) {
    console.error('Image upload failed:', error)
    window.alert(`Image upload failed: ${error instanceof Error ? error.message : 'Unknown error'}`)
  } finally {
    isUploading.value = false
  }
}

function onInput() {
  if (editorRef.value) {
    emit('update:modelValue', editorRef.value.innerHTML)
  }
  updateFormats()
}
</script>

