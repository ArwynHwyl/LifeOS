<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import CourseTile, { type LearnerCourseCard } from '../components/CourseTile.vue'
import { getCoverPreset } from '@/features/courses/constants/courseCoverPresets'
import { getPublishedCourse, listPublishedCourses } from '../services/learnerCourses'
import { useGamificationStore } from '@/features/gamified/stores/gamification'
import CountUp from '@/components/motion/CountUp.vue'

const gamificationStore = useGamificationStore()

const courses = ref<LearnerCourseCard[]>([])
const loading = ref(true)
const loadError = ref('')

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
  if (hour < 12) return 'Good morning'
  if (hour < 18) return 'Good afternoon'
  return 'Good evening'
})

onMounted(() => {
  void loadCourses()
  void gamificationStore.initialize()
})

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
      const done = subTopics.filter((subTopic) => subTopic.interactiveProgress?.status === 'MASTERED').length
      const cover = summary.coverId ? getCoverPreset(summary.coverId) : null
      return {
        id: summary.id,
        title: summary.title,
        desc: summary.description ?? '',
        topics: subTopics.length,
        done,
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

// The bento hero always leads with whatever's most worth doing next: an in-progress
// course first, otherwise something not started yet, otherwise a mastered one to revisit.
const heroCourse = computed(() => {
  const list = filtered.value
  return (
    list.find((c) => c.done > 0 && c.done < c.topics) ??
    list.find((c) => c.done === 0) ??
    list[0] ??
    null
  )
})

const restCourses = computed(() => filtered.value.filter((c) => c.id !== heroCourse.value?.id))

const heroColor = computed<'feather' | 'macaw'>(() => {
  const hero = heroCourse.value
  return hero && hero.topics > 0 && hero.done === hero.topics ? 'feather' : 'macaw'
})

function tileColor(course: LearnerCourseCard, index: number): 'feather' | 'macaw' | 'beetle' | 'eel' {
  const isDone = course.topics > 0 && course.done === course.topics
  if (isDone) return 'feather'
  if (course.done > 0) {
    // Start the cycle on whichever color the hero tile *isn't*, so the tile
    // sitting right next to the hero never repeats its color.
    const cycle: Array<'macaw' | 'beetle'> = heroColor.value === 'macaw' ? ['beetle', 'macaw'] : ['macaw', 'beetle']
    return cycle[index % cycle.length]
  }
  return 'eel'
}

const inProgress = computed(() => courses.value.filter(c => c.done > 0 && c.done < c.topics).length)
const completed = computed(() => courses.value.filter(c => c.topics > 0 && c.done === c.topics).length)

const streak = computed(() => gamificationStore.profile?.currentStreak ?? 0)
const level = computed(() => gamificationStore.profile?.level ?? 1)
const currentExp = computed(() => gamificationStore.profile?.currentExp ?? 0)
const expRequired = computed(() => gamificationStore.profile?.expRequiredForNextLevel ?? 100)
const xpPct = computed(() => expRequired.value <= 0 ? 100 : Math.min(100, Math.round((currentExp.value / expRequired.value) * 100)))
</script>

<template>
  <main class="flex-1 bg-white">
    <div class="max-w-[1180px] mx-auto px-8 py-8">

      <!-- Header -->
      <div class="mb-6">
        <span class="font-mono text-[11px] font-bold tracking-[0.08em] uppercase text-lx-ink-faint">{{ greeting }}, {{ greetingName }}</span>
        <h1 class="font-display text-[34px] font-extrabold tracking-tight text-lx-ink leading-tight mt-1 m-0">
          Ready to level up?
        </h1>
        <p class="text-[13.5px] font-semibold text-lx-ink-soft mt-1.5">
          {{ courses.length }} courses available / {{ inProgress }} in progress / {{ completed }} mastered
        </p>
      </div>

      <!-- Filter chips -->
      <div class="flex gap-2 mb-6">
        <button
          v-for="f in filters"
          :key="f.id"
          type="button"
          @click="activeFilter = f.id"
          :class="[
            'press px-4 py-2 rounded-2xl border-2 font-extrabold text-[12.5px] transition-[transform,border-color] duration-100 active:translate-y-0.5',
            activeFilter === f.id
              ? 'bg-lx-feather border-lx-feather text-white shadow-[0_4px_0_var(--color-lx-feather-dark)] active:shadow-none'
              : 'bg-white border-lx-line text-lx-ink-soft hover:border-lx-ink-faint'
          ]"
        >
          {{ f.label }}
        </button>
      </div>

      <!-- Error banner -->
      <div
        v-if="loadError"
        class="mb-5 flex items-center justify-between rounded-2xl border-2 border-red-200 bg-red-50 px-4 py-3 text-[13px] font-semibold text-red-600"
      >
        <span>{{ loadError }}</span>
        <button type="button" class="font-extrabold hover:opacity-70" @click="loadCourses">Retry</button>
      </div>

      <!-- Loading skeleton -->
      <div v-if="loading" class="grid grid-cols-4 gap-4" style="grid-auto-rows:124px">
        <div class="col-span-2 row-span-2 animate-pulse rounded-[22px] bg-lx-surface-soft" />
        <div class="col-span-2 row-span-2 animate-pulse rounded-[22px] bg-lx-surface-soft" />
        <div v-for="i in 4" :key="i" class="animate-pulse rounded-[22px] bg-lx-surface-soft" />
      </div>

      <!-- Bento grid -->
      <div v-else-if="heroCourse" class="grid grid-cols-4 gap-4" style="grid-auto-rows:124px">

        <CourseTile
          :course="heroCourse"
          variant="hero"
          :color="heroColor"
          class="anim-rise"
          :style="{ '--i': 0 }"
        />

        <!-- Streak + XP tile -->
        <div class="anim-rise col-span-2 row-span-2 flex flex-col justify-between rounded-[22px] bg-lx-fox p-5 text-white" style="--i: 1">
          <div class="flex items-center gap-3">
            <div class="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-white/20">
              <svg class="anim-flame h-6 w-6" viewBox="0 0 24 24" fill="#fff" stroke="#fff" stroke-width="1">
                <path d="M12 3c1 3 4 4 4 8a4 4 0 0 1-8 0c0-2 1-3 2-4 0 1 1 2 2 2 0-2-1-4 0-6z"/>
              </svg>
            </div>
            <div>
              <div class="font-display text-[24px] font-extrabold leading-none"><CountUp :value="streak" /> {{ streak === 1 ? 'day' : 'days' }}</div>
              <div class="mt-0.5 text-[11px] font-bold text-white/80">Current streak</div>
            </div>
          </div>

          <div>
            <div class="flex items-center justify-between mb-1.5">
              <span class="text-[11.5px] font-extrabold">Level {{ level }}</span>
              <span class="font-mono text-[11px] font-bold text-white/80"><CountUp :value="currentExp" />/{{ expRequired }} XP</span>
            </div>
            <div class="h-2 w-full overflow-hidden rounded-full bg-white/25">
              <div class="bar-grow h-full rounded-full bg-white" :style="{ width: `${xpPct}%`, '--i': 4 }" />
            </div>
          </div>
        </div>

        <CourseTile
          v-for="(course, index) in restCourses"
          :key="course.id"
          :course="course"
          :variant="index % 5 === 4 ? 'wide' : 'normal'"
          :color="tileColor(course, index)"
          class="anim-rise"
          :style="{ '--i': index + 2 }"
        />
      </div>

      <!-- Empty state -->
      <div v-else class="rounded-[22px] border-2 border-dashed border-lx-line py-16 text-center">
        <p class="font-display text-[16px] font-extrabold text-lx-ink-soft">No courses here yet</p>
        <p class="mt-1 text-[13px] text-lx-ink-faint">Published courses will show up here.</p>
      </div>
    </div>
  </main>
</template>

<style scoped>
@keyframes flicker {
  0%, 100% { transform: scale(1) rotate(-4deg); }
  50% { transform: scale(1.12) rotate(4deg); }
}
</style>
