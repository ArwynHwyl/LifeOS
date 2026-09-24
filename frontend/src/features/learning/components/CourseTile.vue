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

const props = withDefaults(defineProps<{
  course: LearnerCourseCard
  variant?: 'hero' | 'normal' | 'wide'
  color?: 'feather' | 'macaw' | 'fox' | 'beetle' | 'eel'
}>(), {
  variant: 'normal',
  color: 'eel',
})

const router = useRouter()

const isDone = computed(() => props.course.topics > 0 && props.course.done === props.course.topics)
const isStarted = computed(() => props.course.done > 0)
const pct = computed(() => props.course.topics > 0 ? Math.round((props.course.done / props.course.topics) * 100) : 0)

const statusLabel = computed(() => {
  if (isDone.value) return 'Mastered'
  if (isStarted.value) return 'In progress'
  return 'Not started'
})

const ctaLabel = computed(() => (isDone.value ? 'Review' : isStarted.value ? 'Continue' : 'Start'))

const heroKicker = computed(() => (isDone.value ? 'Time to review' : isStarted.value ? 'Continue learning' : 'Start here'))

const BG_CLASSES: Record<string, string> = {
  feather: 'bg-lx-feather',
  macaw: 'bg-lx-macaw',
  fox: 'bg-lx-fox',
  beetle: 'bg-lx-beetle',
  eel: 'bg-lx-eel',
}

function open() {
  router.push(`/learn/lesson/${props.course.id}`)
}
</script>

<template>
  <!-- Hero tile: 2x2 feature spot -->
  <button
    v-if="variant === 'hero'"
    type="button"
    @click="open"
    :class="[
      'group press relative col-span-2 row-span-2 flex flex-col justify-end overflow-hidden rounded-[22px] p-5 text-left transition-[transform,box-shadow] duration-200 hover:-translate-y-1 hover:shadow-[0_20px_36px_-18px_rgba(0,0,0,0.45)]',
      BG_CLASSES[color],
    ]"
  >
    <span
      class="pointer-events-none absolute -top-5 -right-3 font-display text-[150px] font-extrabold leading-none text-white/15 select-none transition-transform duration-500 ease-out group-hover:-rotate-6 group-hover:scale-110"
      aria-hidden="true"
    >{{ course.glyph }}</span>

    <span class="relative font-mono text-[10.5px] font-bold uppercase tracking-[0.08em] text-white/85">{{ heroKicker }}</span>
    <h3 class="relative font-display text-[26px] font-extrabold leading-tight text-white mt-1 mb-1">{{ course.title }}</h3>
    <p class="relative text-[13px] text-white/80 mb-3 line-clamp-1">{{ course.desc }}</p>

    <div v-if="course.topics > 0" class="relative mb-3 h-2 max-w-[220px] overflow-hidden rounded-full bg-white/30">
      <div class="bar-grow h-full rounded-full bg-white" :style="{ width: `${pct}%`, '--i': 3 }" />
    </div>

    <span class="relative inline-flex w-fit items-center gap-1.5 rounded-2xl bg-white px-5 py-2.5 text-[13.5px] font-extrabold text-lx-macaw shadow-[0_4px_0_rgba(0,0,0,0.16)] transition-transform duration-75 group-active:translate-y-1 group-active:shadow-none">
      {{ ctaLabel }} →
    </span>
  </button>

  <!-- Normal / wide course tile -->
  <button
    v-else
    type="button"
    @click="open"
    :class="[
      'group press relative flex flex-col justify-between overflow-hidden rounded-[22px] p-4 text-left text-white transition-[transform,box-shadow,background-color] duration-200 hover:-translate-y-1 hover:shadow-[0_16px_28px_-16px_rgba(0,0,0,0.5)]',
      variant === 'wide' ? 'col-span-2' : '',
      color === 'eel' ? 'bg-lx-eel hover:bg-lx-eel-soft' : BG_CLASSES[color],
    ]"
  >
    <span class="inline-block origin-bottom-left font-display text-[26px] font-extrabold leading-none transition-transform duration-300 ease-out group-hover:-rotate-6 group-hover:scale-125">{{ course.glyph }}</span>
    <span>
      <span class="block text-[13px] font-extrabold leading-tight">{{ course.title }}</span>
      <span class="mt-1 flex items-center gap-2">
        <span class="font-mono text-[10px] font-bold uppercase tracking-[0.04em] text-white/70">{{ statusLabel }}</span>
        <span class="text-[11px] font-extrabold text-white opacity-0 -translate-x-1 transition-all duration-150 group-hover:opacity-100 group-hover:translate-x-0">
          {{ ctaLabel }} →
        </span>
      </span>
    </span>
  </button>
</template>
