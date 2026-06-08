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
        class="h-8 min-w-[32px] px-2 rounded-[6px] border-none bg-transparent text-lm-ink-2 font-display text-[12px] font-bold cursor-pointer transition-colors duration-120 hover:bg-lm-line-soft"
        title="Insert image"
      >
        Img
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
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:modelValue'])

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
  window.alert('Image upload connects to backend S3')
}

function onInput() {
  if (editorRef.value) {
    emit('update:modelValue', editorRef.value.innerHTML)
  }
  updateFormats()
}
</script>

