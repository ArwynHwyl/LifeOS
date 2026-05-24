<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import LmIcon from './LmIcon.vue'

interface Course {
  id: string
  title: string
  desc: string
  topics: number
  done: number
  level: 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED'
  color: string
  glyph: string
}

const props = defineProps<{ course: Course }>()
const router = useRouter()

const pct = computed(() => Math.round((props.course.done / props.course.topics) * 100))
const isDone = computed(() => props.course.done === props.course.topics)
const isLocked = computed(() => props.course.done === 0 && props.course.level === 'ADVANCED')

const coverColorClass: Record<string, string> = {
  'var(--course-alg)': 'bg-lm-alg',
  'var(--course-geo)': 'bg-lm-geo',
  'var(--course-tri)': 'bg-lm-tri',
  'var(--course-sta)': 'bg-lm-sta',
  'var(--course-cal)': 'bg-lm-cal',
  'var(--course-lin)': 'bg-lm-lin',
}

function open() {
  if (!isLocked.value) router.push(`/learn/lesson/${props.course.id}`)
}
</script>

<template>
  <div
    @click="open"
    :class="[
      'flex flex-col border-2 border-lm-line rounded-[18px] shadow-stamp-md overflow-hidden transition-all duration-200',
      isLocked ? 'opacity-65 cursor-default' : 'cursor-pointer hover:-translate-y-0.5 hover:shadow-stamp-lg',
      'bg-lm-surface'
    ]"
  >
    <!-- Cover -->
    <div
      :class="['h-[130px] border-b-2 border-lm-line relative flex items-center justify-center overflow-hidden', coverColorClass[course.color] ?? 'bg-lm-yellow']"
    >
      <!-- Dotted grid overlay -->
      <div class="absolute inset-0 bg-dot-grid opacity-30 pointer-events-none" />

      <!-- Math glyph -->
      <span class="relative z-10 font-math italic font-bold text-[64px] text-lm-ink leading-none">{{ course.glyph }}</span>

      <!-- Sine wave ornament -->
      <svg class="absolute bottom-2 right-2 opacity-25 pointer-events-none" width="60" height="60" viewBox="0 0 80 80">
        <path d="M2 30 Q 15 5, 28 30 T 54 30 T 78 30" stroke="#1a1814" stroke-width="2" fill="none" stroke-linecap="round"/>
      </svg>

      <!-- Level chip -->
      <div class="absolute top-3 left-3 flex items-center gap-1 px-2.5 py-1 bg-lm-surface border border-lm-line rounded-full text-[11px] font-semibold font-mono tracking-wide text-lm-ink">
        {{ course.level }}
      </div>

      <!-- Done badge -->
      <div v-if="isDone" class="absolute -top-0.5 -right-0.5 w-9 h-9 rounded-full bg-lm-ink border-2 border-lm-line flex items-center justify-center text-lm-bg">
        <LmIcon name="check" :size="18" />
      </div>

      <!-- Lock icon -->
      <div v-if="isLocked" class="absolute top-3 right-3 text-lm-ink-2">
        <LmIcon name="lock" :size="18" />
      </div>
    </div>

    <!-- Body -->
    <div class="flex flex-col gap-1.5 p-4 flex-1">
      <h3 class="font-display text-[20px] font-bold tracking-tight text-lm-ink leading-tight m-0">{{ course.title }}</h3>
      <p class="text-[13px] text-lm-ink-2 leading-snug">{{ course.desc }}</p>

      <!-- Progress -->
      <div class="flex items-center gap-2 mt-1.5">
        <div class="flex-1 h-[9px] bg-lm-bg-soft rounded-full overflow-hidden border border-lm-line">
          <div
            class="h-full transition-all duration-200"
            :class="isDone ? 'bg-lm-green' : 'bg-lm-yellow'"
            :style="{ width: `${pct}%` }"
          />
        </div>
        <span class="font-mono text-[11px] font-semibold text-lm-ink-2 shrink-0">{{ course.done }}/{{ course.topics }}</span>
      </div>

      <!-- Status line -->
      <div class="flex items-center gap-1.5 mt-1 text-[13px] font-semibold text-lm-ink">
        <template v-if="isLocked">
          <LmIcon name="lock" :size="14" /><span>Unlocks at Lv 15</span>
        </template>
        <template v-else-if="isDone">
          <span class="text-lm-green"><LmIcon name="check" :size="14" /></span>
          <span>Mastered</span>
          <span class="ml-auto">review →</span>
        </template>
        <template v-else-if="course.done > 0">
          <span class="text-lm-rust"><LmIcon name="bolt" :size="14" :filled="true" /></span>
          <span>In progress</span>
          <span class="ml-auto">continue →</span>
        </template>
        <template v-else>
          <span>Start course</span>
          <span class="ml-auto">→</span>
        </template>
      </div>
    </div>
  </div>
</template>
