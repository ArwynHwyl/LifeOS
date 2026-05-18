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

export type EditableCourse = {
  id: string
  title: string
  description: string
  coverId: string
}

const props = defineProps<{
  open: boolean
  course: EditableCourse | null
}>()

const emit = defineEmits<{
  close: []
  save: [course: EditableCourse]
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

function close() { emit('close') }

function onSubmit() {
  if (!props.course) return
  emit('save', {
    id: props.course.id,
    title: title.value.trim(),
    description: description.value.trim(),
    coverId: selectedCoverId.value,
  })
  close()
}

function onKeydown(e: KeyboardEvent) { if (e.key === 'Escape') close() }

watch(
  () => [props.open, props.course] as const,
  ([isOpen, course]) => {
    if (isOpen && course) {
      title.value = course.title
      description.value = course.description
      symbolSearch.value = ''
      const { symbolId, colorId } = parseCoverId(course.coverId)
      selectedSymbolId.value = symbolId
      selectedColorId.value = colorId
    }
  },
)
</script>

<template>
  <Teleport to="body">
    <Transition enter-active-class="transition duration-200 ease-out" enter-from-class="opacity-0" enter-to-class="opacity-100"
      leave-active-class="transition duration-150 ease-in" leave-from-class="opacity-100" leave-to-class="opacity-0">
      <div v-if="open && course" class="fixed inset-0 z-50 flex items-center justify-center p-4" @keydown="onKeydown">
        <div class="absolute inset-0 bg-slate-950/50 backdrop-blur-sm" aria-hidden="true" @click="close" />

        <Transition enter-active-class="transition duration-200 ease-out" enter-from-class="opacity-0 scale-95 translate-y-2"
          enter-to-class="opacity-100 scale-100 translate-y-0" leave-active-class="transition duration-150 ease-in"
          leave-from-class="opacity-100 scale-100 translate-y-0" leave-to-class="opacity-0 scale-95 translate-y-2">
          <div v-if="open && course" role="dialog" aria-modal="true" aria-labelledby="edit-course-title"
            class="relative z-10 flex max-h-[min(90vh,680px)] w-full max-w-lg flex-col overflow-hidden rounded-2xl bg-white shadow-2xl shadow-slate-900/20 ring-1 ring-slate-900/[0.06]"
            @click.stop>

            <!-- Header -->
            <header class="shrink-0 border-b border-slate-100 px-6 py-5">
              <div class="flex items-start justify-between gap-4">
                <div class="flex items-center gap-3">
                  <div class="flex h-9 w-9 items-center justify-center rounded-xl bg-[#5b4cfa]/10">
                    <svg class="h-4 w-4 text-[#5b4cfa]" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
                      <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
                    </svg>
                  </div>
                  <div>
                    <h2 id="edit-course-title" class="text-[15px] font-bold text-slate-900">Edit Course</h2>
                    <p class="text-[11px] text-slate-400">Update title, description, or cover.</p>
                  </div>
                </div>
                <button type="button"
                  class="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg text-slate-400 transition hover:bg-slate-100 hover:text-slate-600"
                  aria-label="Close" @click="close">
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
                  <label for="edit-course-title-input" class="mb-1.5 block text-[12px] font-semibold text-slate-600">Course title</label>
                  <input id="edit-course-title-input" v-model="title" type="text" required placeholder="e.g. Quadratic Functions"
                    class="w-full rounded-xl border border-slate-200 bg-slate-50/50 px-3.5 py-2.5 text-[13px] text-slate-800 outline-none transition placeholder:text-slate-400 focus:border-[#5b4cfa]/50 focus:bg-white focus:ring-2 focus:ring-[#5b4cfa]/10" />
                </div>

                <!-- Description -->
                <div>
                  <label for="edit-course-description" class="mb-1.5 block text-[12px] font-semibold text-slate-600">Description</label>
                  <textarea id="edit-course-description" v-model="description" rows="3" placeholder="Brief overview of what students will learn…"
                    class="w-full resize-none rounded-xl border border-slate-200 bg-slate-50/50 px-3.5 py-2.5 text-[13px] text-slate-800 outline-none transition placeholder:text-slate-400 focus:border-[#5b4cfa]/50 focus:bg-white focus:ring-2 focus:ring-[#5b4cfa]/10" />
                </div>

                <!-- Cover picker -->
                <fieldset>
                  <legend class="mb-3 text-[12px] font-semibold text-slate-600">Cover</legend>

                  <!-- Preview + color row -->
                  <div class="mb-3 flex items-start gap-4">
                    <!-- Live preview -->
                    <div
                      class="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl font-serif text-2xl font-bold shadow-sm transition-all duration-150"
                      :class="[selectedPreview.bgClass, selectedPreview.textClass]"
                    >
                      {{ selectedPreview.symbol }}
                    </div>

                    <!-- Color swatches -->
                    <div class="flex-1">
                      <p class="mb-2 text-[11px] font-semibold text-slate-500">Color</p>
                      <div class="flex flex-wrap gap-1.5">
                        <button
                          v-for="color in COVER_COLOR_THEMES"
                          :key="color.id"
                          type="button"
                          class="h-5 w-5 rounded-full transition-transform duration-100 hover:scale-110"
                          :class="selectedColorId === color.id ? 'ring-2 ring-[#5b4cfa] ring-offset-2' : ''"
                          :style="{ backgroundColor: color.swatch }"
                          :title="color.name"
                          @click="selectedColorId = color.id"
                        />
                      </div>
                    </div>
                  </div>

                  <!-- Symbol search -->
                  <div class="relative mb-2">
                    <svg class="pointer-events-none absolute left-2.5 top-1/2 h-3 w-3 -translate-y-1/2 text-slate-400"
                      viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <circle cx="11" cy="11" r="8" /><path d="m21 21-4.3-4.3" />
                    </svg>
                    <input
                      v-model="symbolSearch"
                      type="search"
                      placeholder="Search symbols…"
                      class="w-full rounded-lg border border-slate-200 bg-slate-50/50 py-1.5 pl-7 pr-3 text-[12px] text-slate-700 outline-none transition placeholder:text-slate-400 focus:border-[#5b4cfa]/40 focus:bg-white focus:ring-2 focus:ring-[#5b4cfa]/10"
                    />
                  </div>

                  <!-- Symbol grid -->
                  <div class="max-h-[152px] overflow-y-auto rounded-xl border border-slate-200 bg-slate-50/40 p-2">
                    <div v-if="filteredSymbols.length > 0" class="grid grid-cols-8 gap-1">
                      <button
                        v-for="sym in filteredSymbols"
                        :key="sym.id"
                        type="button"
                        class="flex aspect-square items-center justify-center rounded-lg text-[13px] font-bold leading-none transition-all duration-100"
                        :class="
                          selectedSymbolId === sym.id
                            ? 'bg-[#5b4cfa] text-white shadow-md shadow-[#5b4cfa]/30'
                            : 'text-slate-600 hover:bg-white hover:shadow-sm hover:text-slate-900'
                        "
                        :title="`${sym.label} · ${sym.category}`"
                        @click="selectedSymbolId = sym.id"
                      >
                        {{ sym.symbol }}
                      </button>
                    </div>
                    <p v-else class="py-4 text-center text-[11px] text-slate-400">No symbols found</p>
                  </div>
                </fieldset>
              </div>

              <footer class="flex shrink-0 gap-2.5 border-t border-slate-100 px-6 py-4">
                <button type="button"
                  class="flex-1 rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-[13px] font-semibold text-slate-600 transition hover:bg-slate-50"
                  @click="close">Cancel</button>
                <button type="submit"
                  class="flex-1 rounded-xl bg-[#5b4cfa] px-4 py-2.5 text-[13px] font-semibold text-white shadow-md shadow-[#5b4cfa]/25 transition hover:bg-[#4d3ee0]">
                  Save changes
                </button>
              </footer>
            </form>
          </div>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>
