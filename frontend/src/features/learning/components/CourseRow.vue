<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'

export interface LearnerCourseCard {
  id: number
  title: string
  desc: string
  topics: number
  done: number
  glyph: string
}

const props = defineProps<{ course: LearnerCourseCard }>()

const router = useRouter()

const isDone = computed(() => props.course.topics > 0 && props.course.done === props.course.topics)
const isStarted = computed(() => props.course.done > 0)
const pct = computed(() => (props.course.topics > 0 ? Math.round((props.course.done / props.course.topics) * 100) : 0))

const chipClass = computed(() =>
  isDone.value ? 'bg-lx-feather text-white' : isStarted.value ? 'bg-lx-beetle text-white' : 'bg-lx-surface-soft text-lx-ink-soft',
)
const barClass = computed(() => (isDone.value ? 'bg-lx-feather' : isStarted.value ? 'bg-lx-beetle' : 'bg-lx-line'))
const ctaLabel = computed(() => (isDone.value ? 'Review' : isStarted.value ? 'Continue' : 'Start'))
const ctaClass = computed(() =>
  isStarted.value && !isDone.value
    ? 'bg-lx-macaw text-white shadow-[0_3px_0_var(--color-lx-macaw-dark)]'
    : 'border-2 border-lx-line bg-white text-lx-ink shadow-[0_3px_0_var(--color-lx-line)]',
)

function open() {
  router.push(`/learn/lesson/${props.course.id}`)
}
</script>

<template>
  <button
    type="button"
    class="group grid w-full grid-cols-[44px_minmax(0,1fr)_170px_96px] items-center gap-4 border-t border-lx-line bg-white px-5 py-3.5 text-left transition-colors duration-150 first:border-t-0 hover:bg-lx-surface-soft"
    @click="open"
  >
    <span :class="['flex h-11 w-11 items-center justify-center rounded-[14px] font-display text-[18px] font-extrabold leading-none transition-transform duration-300 group-hover:-rotate-6 group-hover:scale-105', chipClass]">{{ course.glyph }}</span>

    <span class="min-w-0">
      <span class="block truncate text-[14.5px] font-extrabold leading-tight text-lx-ink">{{ course.title }}</span>
      <span class="mt-0.5 block truncate text-[12px] font-medium text-lx-ink-faint">{{ course.desc || 'No description yet' }}</span>
    </span>

    <span class="flex flex-col gap-1.5">
      <span class="block h-1.5 overflow-hidden rounded-full bg-lx-surface-soft group-hover:bg-white">
        <span :class="['block h-full rounded-full transition-[width] duration-500', barClass]" :style="{ width: `${pct}%` }" />
      </span>
      <span class="text-[11px] font-bold text-lx-ink-soft">{{ course.done }} / {{ course.topics }} lessons</span>
    </span>

    <span class="flex justify-end">
      <span :class="['press inline-block rounded-[11px] px-4 py-2 text-[12px] font-extrabold transition-transform duration-100 group-active:translate-y-0.5', ctaClass]">{{ ctaLabel }}</span>
    </span>
  </button>
</template>
