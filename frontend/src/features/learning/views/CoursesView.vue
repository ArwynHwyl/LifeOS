<script setup lang="ts">
import { computed, ref } from 'vue'
import CourseCard from '../components/CourseCard.vue'
import LmIcon from '../components/LmIcon.vue'
import MomentCourseComplete from '../components/MomentCourseComplete.vue'

const courses = [
  { id: 'alg', title: 'Algebra Basics',  topics: 24, done: 14, level: 'BEGINNER'     as const, color: 'var(--course-alg)', glyph: 'x²',  desc: 'Equations, variables & inequalities' },
  { id: 'geo', title: 'Geometry',         topics: 32, done: 8,  level: 'BEGINNER'     as const, color: 'var(--course-geo)', glyph: '△',   desc: 'Shapes, angles & coordinate plane' },
  { id: 'tri', title: 'Trigonometry',     topics: 18, done: 18, level: 'INTERMEDIATE' as const, color: 'var(--course-tri)', glyph: 'sin', desc: 'Triangles, sine, cosine, tangent' },
  { id: 'sta', title: 'Statistics 101',   topics: 20, done: 5,  level: 'BEGINNER'     as const, color: 'var(--course-sta)', glyph: 'σ',   desc: 'Mean, median, variance & graphs' },
  { id: 'cal', title: 'Calculus 1',       topics: 28, done: 0,  level: 'INTERMEDIATE' as const, color: 'var(--course-cal)', glyph: '∫',   desc: 'Limits, derivatives, integrals' },
  { id: 'lin', title: 'Linear Algebra',   topics: 36, done: 0,  level: 'ADVANCED'     as const, color: 'var(--course-lin)', glyph: '[]',  desc: 'Vectors, matrices & spaces' },
]

type Filter = 'all' | 'progress' | 'done' | 'new'
const activeFilter = ref<Filter>('all')

const filters: { id: Filter; label: string }[] = [
  { id: 'all',      label: 'All courses' },
  { id: 'progress', label: 'In progress' },
  { id: 'done',     label: 'Mastered' },
  { id: 'new',      label: 'Not started' },
]

const filtered = computed(() => {
  if (activeFilter.value === 'progress') return courses.filter(c => c.done > 0 && c.done < c.topics)
  if (activeFilter.value === 'done')     return courses.filter(c => c.done === c.topics)
  if (activeFilter.value === 'new')      return courses.filter(c => c.done === 0)
  return courses
})

const inProgress = computed(() => courses.filter(c => c.done > 0 && c.done < c.topics).length)
const completed = computed(() => courses.filter(c => c.done === c.topics).length)

const showComplete = ref(false)
</script>

<template>
  <main class="flex-1 overflow-auto bg-lm-bg relative">
    <div class="absolute inset-0 bg-dot-grid opacity-40 pointer-events-none" />

    <div class="relative max-w-[1280px] mx-auto px-9 py-7">

      <!-- Header -->
      <div class="flex items-end justify-between gap-6 mb-[22px]">
        <div>
          <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3">GOOD AFTERNOON, JAMIE</span>
          <h1 class="font-display text-[42px] font-bold tracking-tight text-lm-ink leading-tight mt-1 mb-1.5 m-0">
            Pick a course to
            <span class="inline-block bg-lm-yellow px-2 rounded-[6px] border-2 border-lm-line shadow-stamp-sm -rotate-1 whitespace-nowrap">
              level up
            </span>
          </h1>
          <p class="text-[15px] text-lm-ink-2 mt-2">
            {{ courses.length }} courses · {{ inProgress }} in progress · {{ completed }} mastered
          </p>
        </div>
        <button
          @click="showComplete = true"
          class="flex items-center gap-2 px-[18px] py-[9px] text-[15px] font-semibold border-2 border-lm-line rounded-full bg-lm-surface shadow-stamp-sm hover:-translate-y-px hover:shadow-stamp-md transition-all duration-200 text-lm-ink shrink-0"
        >
          <LmIcon name="plus" :size="16" />
          Browse catalog
        </button>
      </div>

      <!-- Filter chips -->
      <div class="flex gap-2 mb-[22px]">
        <button
          v-for="f in filters"
          :key="f.id"
          @click="activeFilter = f.id"
          :class="[
            'px-4 py-2 rounded-full border-2 border-lm-line font-semibold text-sm transition-all duration-200',
            activeFilter === f.id
              ? 'bg-lm-ink text-lm-bg shadow-stamp-sm'
              : 'bg-lm-surface text-lm-ink hover:bg-lm-bg-soft'
          ]"
        >
          {{ f.label }}
        </button>
      </div>

      <!-- Course grid -->
      <div class="grid grid-cols-3 gap-[22px]">
        <CourseCard v-for="c in filtered" :key="c.id" :course="c" />
      </div>
    </div>

    <!-- Course Complete moment overlay -->
    <MomentCourseComplete v-if="showComplete" @close="showComplete = false" />
  </main>
</template>
