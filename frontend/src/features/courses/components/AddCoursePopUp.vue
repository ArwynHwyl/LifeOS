<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { COURSE_COVER_PRESETS, DEFAULT_COVER_ID } from '@/features/courses/constants/courseCoverPresets'

const props = defineProps<{
  open: boolean
}>()

const emit = defineEmits<{
  close: []
}>()

const title = ref('')
const description = ref('')
const selectedCoverId = ref(DEFAULT_COVER_ID)
const aiGenerated = ref(false)
const prompt = ref('')
const pdfFile = ref<File | null>(null)
const pdfInputRef = ref<HTMLInputElement | null>(null)

const pdfFileName = computed(() => pdfFile.value?.name ?? '')

function resetForm() {
  title.value = ''
  description.value = ''
  selectedCoverId.value = DEFAULT_COVER_ID
  aiGenerated.value = false
  prompt.value = ''
  pdfFile.value = null
  if (pdfInputRef.value) pdfInputRef.value.value = ''
}

function close() {
  emit('close')
}

function onBackdropClick() {
  close()
}

function onPdfChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0] ?? null
  if (file && file.type !== 'application/pdf') {
    input.value = ''
    pdfFile.value = null
    return
  }
  pdfFile.value = file
}

function removePdf() {
  pdfFile.value = null
  if (pdfInputRef.value) pdfInputRef.value.value = ''
}

function onSubmit() {
  // UI only — backend wiring comes later
  close()
}

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') close()
}

watch(
  () => props.open,
  (isOpen) => {
    if (!isOpen) resetForm()
  },
)
</script>

<template>
  <Teleport to="body">
    <Transition
      enter-active-class="transition duration-200 ease-out"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="transition duration-150 ease-in"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div
        v-if="open"
        class="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6"
        @keydown="onKeydown"
      >
        <div
          class="absolute inset-0 bg-slate-900/40 backdrop-blur-sm"
          aria-hidden="true"
          @click="onBackdropClick"
        />

        <Transition
          enter-active-class="transition duration-200 ease-out"
          enter-from-class="opacity-0 scale-95 translate-y-2"
          enter-to-class="opacity-100 scale-100 translate-y-0"
          leave-active-class="transition duration-150 ease-in"
          leave-from-class="opacity-100 scale-100 translate-y-0"
          leave-to-class="opacity-0 scale-95 translate-y-2"
        >
          <div
            v-if="open"
            role="dialog"
            aria-modal="true"
            aria-labelledby="add-course-title"
            class="relative z-10 flex max-h-[min(90vh,720px)] w-full max-w-lg flex-col overflow-hidden rounded-2xl border border-slate-200/80 bg-white shadow-2xl shadow-slate-900/10"
            @click.stop
          >
            <header class="shrink-0 border-b border-slate-100 px-6 py-5">
              <div class="flex items-start justify-between gap-4">
                <div>
                  <h2
                    id="add-course-title"
                    class="font-[family-name:var(--font-serif)] text-xl font-bold tracking-tight text-slate-900"
                  >
                    New Course
                  </h2>
                  <p class="mt-1 text-sm text-slate-500">
                    Add course details. AI generation options are optional.
                  </p>
                </div>
                <button
                  type="button"
                  class="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl text-slate-400 transition hover:bg-slate-100 hover:text-slate-600"
                  aria-label="Close"
                  @click="close"
                >
                  <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M18 6 6 18M6 6l12 12" />
                  </svg>
                </button>
              </div>
            </header>

            <form class="flex min-h-0 flex-1 flex-col" @submit.prevent="onSubmit">
              <div class="min-h-0 flex-1 space-y-5 overflow-y-auto px-6 py-5">
                <div>
                  <label for="course-title" class="mb-1.5 block text-sm font-medium text-slate-700">
                    Course title
                  </label>
                  <input
                    id="course-title"
                    v-model="title"
                    type="text"
                    required
                    placeholder="e.g. Quadratic Functions"
                    class="w-full rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm text-slate-800 outline-none ring-[#5b4cfa]/20 placeholder:text-slate-400 focus:border-[#5b4cfa]/40 focus:ring-2"
                  />
                </div>

                <div>
                  <label for="course-description" class="mb-1.5 block text-sm font-medium text-slate-700">
                    Course description
                  </label>
                  <textarea
                    id="course-description"
                    v-model="description"
                    rows="3"
                    placeholder="Brief overview of what students will learn..."
                    class="w-full resize-none rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm text-slate-800 outline-none ring-[#5b4cfa]/20 placeholder:text-slate-400 focus:border-[#5b4cfa]/40 focus:ring-2"
                  />
                </div>

                <fieldset>
                  <legend class="mb-2 text-sm font-medium text-slate-700">Course cover</legend>
                  <p class="mb-3 text-xs text-slate-500">Choose a preset icon for the course card.</p>
                  <div class="grid grid-cols-6 gap-2">
                    <button
                      v-for="preset in COURSE_COVER_PRESETS"
                      :key="preset.id"
                      type="button"
                      class="flex aspect-square items-center justify-center rounded-xl border-2 font-serif text-lg font-semibold transition"
                      :class="[
                        preset.bgClass,
                        preset.textClass,
                        selectedCoverId === preset.id
                          ? 'border-[#5b4cfa] ring-2 ring-[#5b4cfa]/25'
                          : 'border-transparent hover:border-slate-200',
                      ]"
                      :aria-pressed="selectedCoverId === preset.id"
                      :aria-label="`Cover icon ${preset.symbol}`"
                      @click="selectedCoverId = preset.id"
                    >
                      {{ preset.symbol }}
                    </button>
                  </div>
                </fieldset>

                <div class="rounded-xl border border-slate-100 bg-slate-50/80 px-4 py-3">
                  <label class="flex cursor-pointer items-start gap-3">
                    <input
                      v-model="aiGenerated"
                      type="checkbox"
                      class="mt-0.5 h-4 w-4 shrink-0 rounded border-slate-300 text-[#5b4cfa] focus:ring-[#5b4cfa]/30"
                    />
                    <span>
                      <span class="block text-sm font-medium text-slate-800">Activate AI generated</span>
                      <span class="mt-0.5 block text-xs text-slate-500">
                        Use a prompt and PDF as source material for AI-generated content.
                      </span>
                    </span>
                  </label>
                </div>

                <Transition
                  enter-active-class="transition duration-200 ease-out"
                  enter-from-class="opacity-0 -translate-y-1"
                  enter-to-class="opacity-100 translate-y-0"
                  leave-active-class="transition duration-150 ease-in"
                  leave-from-class="opacity-100 translate-y-0"
                  leave-to-class="opacity-0 -translate-y-1"
                >
                  <div v-if="aiGenerated" class="space-y-5">
                    <div>
                      <label for="ai-prompt" class="mb-1.5 block text-sm font-medium text-slate-700">
                        Prompt
                      </label>
                      <textarea
                        id="ai-prompt"
                        v-model="prompt"
                        rows="4"
                        placeholder="Describe how the AI should structure modules, tone, difficulty..."
                        class="w-full resize-none rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm text-slate-800 outline-none ring-[#5b4cfa]/20 placeholder:text-slate-400 focus:border-[#5b4cfa]/40 focus:ring-2"
                      />
                    </div>

                    <div>
                      <span class="mb-1.5 block text-sm font-medium text-slate-700">PDF upload</span>
                      <p class="mb-2 text-xs text-slate-500">
                        Upload a PDF to use as the source for AI generation.
                      </p>

                      <input
                        ref="pdfInputRef"
                        type="file"
                        accept="application/pdf,.pdf"
                        class="sr-only"
                        @change="onPdfChange"
                      />

                      <button
                        v-if="!pdfFile"
                        type="button"
                        class="flex w-full flex-col items-center justify-center gap-2 rounded-xl border-2 border-dashed border-slate-200 bg-white px-4 py-8 text-center transition hover:border-[#5b4cfa]/40 hover:bg-[#5b4cfa]/[0.03]"
                        @click="pdfInputRef?.click()"
                      >
                        <svg
                          class="h-8 w-8 text-slate-300"
                          viewBox="0 0 24 24"
                          fill="none"
                          stroke="currentColor"
                          stroke-width="1.5"
                          aria-hidden="true"
                        >
                          <path
                            d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"
                          />
                          <polyline points="14 2 14 8 20 8" />
                          <line x1="12" y1="18" x2="12" y2="12" />
                          <line x1="9" y1="15" x2="15" y2="15" />
                        </svg>
                        <span class="text-sm font-medium text-slate-600">Click to upload PDF</span>
                        <span class="text-xs text-slate-400">PDF only · max size set on backend later</span>
                      </button>

                      <div
                        v-else
                        class="flex items-center gap-3 rounded-xl border border-slate-200 bg-white px-4 py-3"
                      >
                        <div
                          class="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-red-50 text-red-600"
                        >
                          <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path
                              d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"
                            />
                            <polyline points="14 2 14 8 20 8" />
                          </svg>
                        </div>
                        <div class="min-w-0 flex-1">
                          <p class="truncate text-sm font-medium text-slate-800">{{ pdfFileName }}</p>
                          <p class="text-xs text-slate-400">Ready for upload when backend is connected</p>
                        </div>
                        <button
                          type="button"
                          class="shrink-0 text-sm font-medium text-red-600 hover:text-red-700"
                          @click="removePdf"
                        >
                          Remove
                        </button>
                      </div>
                    </div>
                  </div>
                </Transition>
              </div>

              <footer class="flex shrink-0 gap-3 border-t border-slate-100 bg-slate-50/50 px-6 py-4">
                <button
                  type="button"
                  class="flex-1 rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm font-medium text-slate-700 transition hover:bg-slate-50"
                  @click="close"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  class="flex-1 rounded-xl bg-[#5b4cfa] px-4 py-2.5 text-sm font-semibold text-white shadow-md shadow-[#5b4cfa]/25 transition hover:bg-[#4d3ee0]"
                >
                  Create course
                </button>
              </footer>
            </form>
          </div>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>
