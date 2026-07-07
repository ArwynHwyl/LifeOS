<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import {
  COVER_SYMBOLS,
  COVER_COLOR_THEMES,
  buildCoverId,
  getCoverPreset,
  DEFAULT_COVER_ID,
  parseCoverId,
} from '@/features/courses/constants/courseCoverPresets'
import type { AdminCourseCardModel } from '@/features/courses/services/adminCourses'

const props = defineProps<{
  open: boolean
  course: AdminCourseCardModel | null
  submitting?: boolean
  errorMessage?: string
}>()

const emit = defineEmits<{
  close: []
  save: [payload: { title: string; description: string; coverId: string }]
}>()

const title = ref('')
const description = ref('')
const symbolSearch = ref('')

const { symbolId: defaultSymbolId, colorId: defaultColorId } = parseCoverId(DEFAULT_COVER_ID)
const selectedSymbolId = ref(defaultSymbolId)
const selectedColorId = ref(defaultColorId)

const selectedCoverId = computed(() => buildCoverId(selectedSymbolId.value, selectedColorId.value))
const selectedPreview = computed(() => getCoverPreset(selectedCoverId.value))

const filteredSymbols = computed(() => {
  const q = symbolSearch.value.trim().toLowerCase()
  if (!q) return COVER_SYMBOLS
  return COVER_SYMBOLS.filter(
    (s) =>
      s.label.toLowerCase().includes(q) ||
      s.category.toLowerCase().includes(q) ||
      s.symbol.toLowerCase().includes(q),
  )
})

function populateFromCourse(c: AdminCourseCardModel) {
  title.value = c.title
  description.value = c.description
  const { symbolId, colorId } = parseCoverId(c.coverId)
  selectedSymbolId.value = symbolId
  selectedColorId.value = colorId
  symbolSearch.value = ''
}

function close() { emit('close') }

function onSubmit() {
  if (props.submitting) return
  emit('save', {
    title: title.value.trim(),
    description: description.value.trim(),
    coverId: selectedCoverId.value,
  })
}

function onKeydown(e: KeyboardEvent) { if (e.key === 'Escape') close() }

watch(
  () => props.open,
  (open) => {
    if (open && props.course) populateFromCourse(props.course)
  },
)

watch(
  () => props.course,
  (c) => {
    if (props.open && c) populateFromCourse(c)
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
      <div v-if="open" class="fixed inset-0 z-50 flex items-center justify-center p-4" @keydown="onKeydown">
        <div class="absolute inset-0 bg-lm-ink/60 backdrop-blur-sm" aria-hidden="true" @click="close" />

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
            aria-labelledby="edit-course-title"
            class="relative z-10 flex max-h-[min(90vh,680px)] w-full max-w-lg flex-col overflow-hidden rounded-[18px] border-2 border-lm-line bg-lm-surface shadow-stamp-md"
            @click.stop
          >

            <!-- Header -->
            <header class="shrink-0 border-b-2 border-lm-line px-6 py-5">
              <div class="flex items-start justify-between gap-4">
                <div class="flex items-center gap-3">
                  <div class="flex h-9 w-9 items-center justify-center rounded-[10px] border-2 border-lm-line bg-lm-yellow shadow-stamp-sm">
                    <svg class="h-4 w-4 text-lm-ink" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M12 20h9" />
                      <path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4Z" />
                    </svg>
                  </div>
                  <div>
                    <h2 id="edit-course-title" class="font-display text-[15px] font-bold text-lm-ink">Edit Course</h2>
                    <p class="mt-1 text-[11px] text-lm-ink-3">Update the course details below</p>
                  </div>
                </div>
                <button
                  type="button"
                  class="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg text-lm-ink-3 transition hover:bg-lm-bg hover:text-lm-ink"
                  aria-label="Close"
                  @click="close"
                >
                  <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M18 6 6 18M6 6l12 12" />
                  </svg>
                </button>
              </div>
            </header>

            <form class="flex min-h-0 flex-1 flex-col" @submit.prevent="onSubmit">
              <div class="min-h-0 flex-1 space-y-4 overflow-y-auto px-6 py-5">

                <!-- Title -->
                <div>
                  <label for="edit-course-title-input" class="mb-1.5 block text-[12px] font-semibold text-lm-ink-2">Course title</label>
                  <input
                    id="edit-course-title-input"
                    v-model="title"
                    type="text"
                    required
                    placeholder="e.g. Quadratic Functions"
                    class="h-10 w-full rounded-[10px] border-2 border-lm-line-soft bg-lm-bg-soft px-3.5 text-[13px] text-lm-ink outline-none transition placeholder:text-lm-ink-3 focus:border-lm-line focus:ring-2 focus:ring-lm-yellow/40"
                  />
                </div>

                <!-- Description -->
                <div>
                  <label for="edit-course-desc" class="mb-1.5 block text-[12px] font-semibold text-lm-ink-2">Description</label>
                  <textarea
                    id="edit-course-desc"
                    v-model="description"
                    rows="3"
                    placeholder="Brief overview of what students will learn…"
                    class="w-full resize-none rounded-[10px] border-2 border-lm-line-soft bg-lm-bg-soft px-3.5 py-2.5 text-[13px] text-lm-ink outline-none transition placeholder:text-lm-ink-3 focus:border-lm-line focus:ring-2 focus:ring-lm-yellow/40"
                  />
                </div>

                <!-- Cover picker -->
                <fieldset>
                  <legend class="mb-3 text-[12px] font-semibold text-lm-ink-2">Cover</legend>

                  <!-- Preview + color row -->
                  <div class="mb-3 flex items-start gap-4">
                    <!-- Live preview -->
                    <div
                      class="flex h-14 w-14 shrink-0 items-center justify-center rounded-[14px] border-2 border-lm-line font-math text-2xl font-bold italic shadow-stamp-sm transition-all duration-150"
                      :class="[selectedPreview.bgClass, selectedPreview.textClass]"
                    >
                      {{ selectedPreview.symbol }}
                    </div>

                    <!-- Color swatches -->
                    <div class="flex-1">
                      <p class="mb-2 text-[11px] font-semibold text-lm-ink-3">Color</p>
                      <div class="flex flex-wrap gap-1.5">
                        <button
                          v-for="color in COVER_COLOR_THEMES"
                          :key="color.id"
                          type="button"
                          class="h-5 w-5 rounded-full transition-transform duration-100 hover:scale-110"
                          :class="selectedColorId === color.id ? 'ring-2 ring-lm-ink ring-offset-2' : ''"
                          :style="{ backgroundColor: color.swatch }"
                          :title="color.name"
                          @click="selectedColorId = color.id"
                        />
                      </div>
                    </div>
                  </div>

                  <!-- Symbol search -->
                  <div class="relative mb-2">
                    <svg
                      class="pointer-events-none absolute left-2.5 top-1/2 h-3 w-3 -translate-y-1/2 text-lm-ink-3"
                      viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
                    >
                      <circle cx="11" cy="11" r="8" /><path d="m21 21-4.3-4.3" />
                    </svg>
                    <input
                      v-model="symbolSearch"
                      type="search"
                      placeholder="Search symbols…"
                      class="h-9 w-full rounded-[10px] border-2 border-lm-line-soft bg-lm-bg-soft py-1.5 pl-7 pr-3 text-[12px] text-lm-ink outline-none placeholder:text-lm-ink-3 focus:border-lm-line"
                    />
                  </div>

                  <!-- Symbol grid -->
                  <div class="max-h-[152px] overflow-y-auto rounded-[10px] border-2 border-lm-line-soft bg-lm-bg-soft/40 p-2">
                    <div v-if="filteredSymbols.length > 0" class="grid grid-cols-8 gap-1">
                      <button
                        v-for="sym in filteredSymbols"
                        :key="sym.id"
                        type="button"
                        class="flex aspect-square items-center justify-center rounded-[8px] font-math text-[13px] font-bold leading-none transition-all duration-100"
                        :class="
                          selectedSymbolId === sym.id
                            ? 'border-2 border-lm-line bg-lm-yellow text-lm-ink shadow-stamp-sm'
                            : 'text-lm-ink-2 hover:bg-lm-surface hover:text-lm-ink'
                        "
                        :title="`${sym.label} · ${sym.category}`"
                        @click="selectedSymbolId = sym.id"
                      >
                        {{ sym.symbol }}
                      </button>
                    </div>
                    <p v-else class="py-4 text-center text-[11px] text-lm-ink-3">No symbols found</p>
                  </div>
                </fieldset>
              </div>

              <footer class="relative flex shrink-0 gap-2.5 border-t-2 border-lm-line px-6 py-4">
                <p
                  v-if="errorMessage"
                  class="absolute bottom-[70px] left-6 right-6 rounded-[8px] border-2 border-lm-red bg-lm-red-soft px-3 py-2 text-[12px] font-medium text-lm-red"
                >
                  {{ errorMessage }}
                </p>
                <button
                  type="button"
                  class="h-10 flex-1 rounded-full border-2 border-lm-line bg-lm-surface px-4 text-[13px] font-bold text-lm-ink shadow-stamp-sm transition hover:-translate-y-px hover:shadow-stamp-md"
                  :disabled="submitting"
                  @click="close"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  class="h-10 flex-1 rounded-full border-2 border-lm-line bg-lm-yellow px-4 text-[13px] font-bold text-lm-ink shadow-stamp-sm transition hover:-translate-y-px hover:shadow-stamp-md disabled:cursor-not-allowed disabled:opacity-50"
                  :disabled="submitting"
                >
                  {{ submitting ? 'Saving…' : 'Save changes' }}
                </button>
              </footer>
            </form>
          </div>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>
