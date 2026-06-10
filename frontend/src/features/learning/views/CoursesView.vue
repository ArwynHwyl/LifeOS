<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import CourseCard, { type LearnerCourseCard } from '../components/CourseCard.vue'
import { getCoverPreset } from '@/features/courses/constants/courseCoverPresets'
import { getPublishedCourse, listPublishedCourses } from '../services/learnerCourses'

const courses = ref<LearnerCourseCard[]>([])
const loading = ref(true)
const loadError = ref('')

const COVER_COLORS = ['bg-lm-alg', 'bg-lm-geo', 'bg-lm-tri', 'bg-lm-sta', 'bg-lm-cal', 'bg-lm-lin']

const GLYPHS: Array<[RegExp, string]> = [
  [/logic/i, '∧'],
  [/matri/i, '[]'],
  [/vector/i, '→'],
  [/prob|stat/i, 'σ'],
  [/set/i, '∪'],
  [/calc/i, '∫'],
  [/trig/i, 'sin'],
  [/geo/i, '△'],
  [/alge/i, 'x²'],
]

function glyphFor(title: string): string {
  for (const [pattern, glyph] of GLYPHS) {
    if (pattern.test(title)) return glyph
  }
  return 'ƒ'
}

const greetingName = computed(() => {
  const raw = localStorage.getItem('authUser')
  if (!raw) return 'learner'
  try {
    const user = JSON.parse(raw) as { username?: string; firstName?: string }
    return user.firstName || user.username || 'learner'
  } catch {
    return 'learner'
  }
})

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 12) return 'GOOD MORNING'
  if (hour < 18) return 'GOOD AFTERNOON'
  return 'GOOD EVENING'
})

onMounted(loadCourses)

async function loadCourses() {
  loading.value = true
  loadError.value = ''
  try {
    const summaries = await listPublishedCourses()
    const details = await Promise.all(
      summaries.map(async (summary) => {
        try {
          return await getPublishedCourse(summary.id)
        } catch {
          return null
        }
      }),
    )
    courses.value = summaries.map((summary, index) => {
      const detail = details[index]
      const subTopics = detail?.modules.flatMap((module) => module.subTopics) ?? []
      const trackable = subTopics
      const done = trackable.filter((subTopic) => subTopic.interactiveProgress?.status === 'MASTERED').length
      const cover = summary.coverId ? getCoverPreset(summary.coverId) : null
      return {
        id: summary.id,
        title: summary.title,
        desc: summary.description ?? '',
        topics: trackable.length,
        done,
        color: cover?.bgClass ?? COVER_COLORS[index % COVER_COLORS.length],
        glyph: cover?.symbol ?? glyphFor(summary.title),
      }
    })
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : 'Unable to load courses.'
  } finally {
    loading.value = false
  }
}

type Filter = 'all' | 'progress' | 'done' | 'new'
const activeFilter = ref<Filter>('all')

const filters: { id: Filter; label: string }[] = [
  { id: 'all',      label: 'All courses' },
  { id: 'progress', label: 'In progress' },
  { id: 'done',     label: 'Mastered' },
  { id: 'new',      label: 'Not started' },
]

const filtered = computed(() => {
  if (activeFilter.value === 'progress') return courses.value.filter(c => c.done > 0 && c.done < c.topics)
  if (activeFilter.value === 'done')     return courses.value.filter(c => c.topics > 0 && c.done === c.topics)
  if (activeFilter.value === 'new')      return courses.value.filter(c => c.done === 0)
  return courses.value
})

const inProgress = computed(() => courses.value.filter(c => c.done > 0 && c.done < c.topics).length)
const completed = computed(() => courses.value.filter(c => c.topics > 0 && c.done === c.topics).length)
</script>

<template>
  <main class="flex-1 overflow-auto bg-lm-bg relative">
    <div class="absolute inset-0 bg-dot-grid opacity-40 pointer-events-none" />

    <div class="relative max-w-[1280px] mx-auto px-9 py-7">

      <!-- Header -->
      <div class="flex items-end justify-between gap-6 mb-[22px]">
        <div>
          <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3">{{ greeting }}, {{ greetingName.toUpperCase() }}</span>
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

      <!-- Error banner -->
      <div
        v-if="loadError"
        class="mb-4 flex items-center justify-between rounded-xl border-2 border-lm-red bg-lm-red-soft px-4 py-3 text-[13px] font-medium text-lm-red"
      >
        <span>{{ loadError }}</span>
        <button type="button" class="font-bold hover:opacity-70" @click="loadCourses">Retry</button>
      </div>

      <!-- Loading skeleton -->
      <div v-if="loading" class="grid grid-cols-3 gap-[22px]">
        <div
          v-for="i in 3"
          :key="i"
          class="h-[290px] animate-pulse rounded-[18px] border-2 border-lm-line-soft bg-lm-surface"
        />
      </div>

      <!-- Course grid -->
      <div v-else-if="filtered.length" class="grid grid-cols-3 gap-[22px]">
        <CourseCard v-for="c in filtered" :key="c.id" :course="c" />
      </div>

      <!-- Empty state -->
      <div v-else class="rounded-[18px] border-2 border-dashed border-lm-line-soft py-16 text-center">
        <p class="font-display text-[16px] font-bold text-lm-ink-2">No courses here yet</p>
        <p class="mt-1 text-[13px] text-lm-ink-3">Published courses will show up on this shelf.</p>
      </div>
    </div>
  </main>
</template>
