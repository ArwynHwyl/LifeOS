<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import {
  COURSE_COVER_PRESETS,
  DEFAULT_COVER_ID,
  getCoverPreset,
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
const selectedCoverId = ref(DEFAULT_COVER_ID)

const selectedCover = computed(() => getCoverPreset(selectedCoverId.value))

function populateFromCourse(course: EditableCourse) {
  title.value = course.title
  description.value = course.description
  selectedCoverId.value = course.coverId
}

function close() {
  emit('close')
}

function onBackdropClick() {
  close()
}

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

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') close()
}

watch(
  () => [props.open, props.course] as const,
  ([isOpen, course]) => {
    if (isOpen && course) populateFromCourse(course)
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
        v-if="open && course"
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
            v-if="open && course"
            role="dialog"
            aria-modal="true"
            aria-labelledby="edit-course-title"
            class="relative z-10 flex max-h-[min(90vh,640px)] w-full max-w-lg flex-col overflow-hidden rounded-2xl border border-slate-200/80 bg-white shadow-2xl shadow-slate-900/10"
            @click.stop
          >
            <header class="shrink-0 border-b border-slate-100 px-6 py-5">
              <div class="flex items-start justify-between gap-4">
                <div>
                  <h2
                    id="edit-course-title"
                    class="font-[family-name:var(--font-serif)] text-xl font-bold tracking-tight text-slate-900"
                  >
                    Edit Course
                  </h2>
                  <p class="mt-1 text-sm text-slate-500">
                    Update the course title, description, or cover icon.
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
                  <label for="edit-course-title-input" class="mb-1.5 block text-sm font-medium text-slate-700">
                    Course title
                  </label>
                  <input
                    id="edit-course-title-input"
                    v-model="title"
                    type="text"
                    required
                    placeholder="e.g. Quadratic Functions"
                    class="w-full rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm text-slate-800 outline-none ring-[#5b4cfa]/20 placeholder:text-slate-400 focus:border-[#5b4cfa]/40 focus:ring-2"
                  />
                </div>

                <div>
                  <label for="edit-course-description" class="mb-1.5 block text-sm font-medium text-slate-700">
                    Course description
                  </label>
                  <textarea
                    id="edit-course-description"
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
