<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import CourseRow, { type LearnerCourseCard } from '../components/CourseRow.vue'
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

// ── Filtering, search, sort, paging ─────────────────────────────────────────
type Filter = 'all' | 'progress' | 'new' | 'done'
type Sort = 'recommended' | 'az' | 'progress'

const PAGE_SIZE = 12

const activeFilter = ref<Filter>('all')
const query = ref('')
const sort = ref<Sort>('recommended')
const visibleCount = ref(PAGE_SIZE)

const isDone = (c: LearnerCourseCard) => c.topics > 0 && c.done === c.topics
const isInProgress = (c: LearnerCourseCard) => c.done > 0 && !isDone(c)
const pctOf = (c: LearnerCourseCard) => (c.topics > 0 ? c.done / c.topics : 0)

const counts = computed(() => ({
  all: courses.value.length,
  progress: courses.value.filter(isInProgress).length,
  new: courses.value.filter((c) => c.done === 0).length,
  done: courses.value.filter(isDone).length,
}))

const filters = computed<{ id: Filter; label: string; count: number }[]>(() => [
  { id: 'all', label: 'All courses', count: counts.value.all },
  { id: 'progress', label: 'In progress', count: counts.value.progress },
  { id: 'new', label: 'Not started', count: counts.value.new },
  { id: 'done', label: 'Mastered', count: counts.value.done },
])

const results = computed(() => {
  const q = query.value.trim().toLowerCase()
  let list = courses.value.filter((c) => {
    if (activeFilter.value === 'progress' && !isInProgress(c)) return false
    if (activeFilter.value === 'new' && c.done !== 0) return false
    if (activeFilter.value === 'done' && !isDone(c)) return false
    return !q || c.title.toLowerCase().includes(q) || c.desc.toLowerCase().includes(q)
  })
  if (sort.value === 'az') list = list.slice().sort((a, b) => a.title.localeCompare(b.title))
  else if (sort.value === 'progress') list = list.slice().sort((a, b) => pctOf(b) - pctOf(a))
  // "Recommended": courses you have started come first, then untouched ones, mastered last.
  else list = list.slice().sort((a, b) => rank(a) - rank(b))
  return list
})

function rank(c: LearnerCourseCard): number {
  if (isInProgress(c)) return 0
  if (c.done === 0) return 1
  return 2
}

const paged = computed(() => results.value.slice(0, visibleCount.value))
const remaining = computed(() => Math.max(0, results.value.length - visibleCount.value))
const filtering = computed(() => activeFilter.value !== 'all' || query.value.trim().length > 0)

watch([activeFilter, query, sort], () => {
  visibleCount.value = PAGE_SIZE
})

function reset() {
  activeFilter.value = 'all'
  query.value = ''
}

// ── Gamification rail ───────────────────────────────────────────────────────
const streak = computed(() => gamificationStore.profile?.currentStreak ?? 0)
const level = computed(() => gamificationStore.profile?.level ?? 1)
const currentExp = computed(() => gamificationStore.profile?.currentExp ?? 0)
const expRequired = computed(() => gamificationStore.profile?.expRequiredForNextLevel ?? 100)
const xpPct = computed(() => (expRequired.value <= 0 ? 100 : Math.min(100, Math.round((currentExp.value / expRequired.value) * 100))))
</script>

<template>
  <main class="flex-1 bg-white">
    <div class="mx-auto max-w-[1180px] px-8 py-8">
      <!-- Header -->
      <div class="mb-7">
        <span class="font-mono text-[11px] font-bold uppercase tracking-[0.08em] text-lx-ink-faint">{{ greeting }}, {{ greetingName }}</span>
        <h1 class="m-0 mt-1 font-display text-[34px] font-extrabold leading-tight tracking-tight text-lx-ink">Ready to level up?</h1>
      </div>

      <div class="grid grid-cols-[236px_minmax(0,1fr)] items-start gap-7">
        <!-- Left rail -->
        <aside class="sticky top-6 flex flex-col gap-4">
          <div class="anim-rise rounded-[20px] border-2 border-lx-line p-4" style="--i: 0">
            <div class="flex items-center gap-3">
              <span class="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-lx-fox">
                <svg class="anim-flame h-6 w-6" viewBox="0 0 24 24" fill="#fff" stroke="#fff" stroke-width="1">
                  <path d="M12 3c1 3 4 4 4 8a4 4 0 0 1-8 0c0-2 1-3 2-4 0 1 1 2 2 2 0-2-1-4 0-6z" />
                </svg>
              </span>
              <div>
                <div class="font-display text-[22px] font-extrabold leading-none text-lx-ink"><CountUp :value="streak" /> {{ streak === 1 ? 'day' : 'days' }}</div>
                <div class="mt-1 text-[11px] font-bold text-lx-ink-faint">Current streak</div>
              </div>
            </div>
            <div class="mt-4">
              <div class="mb-1.5 flex items-center justify-between">
                <span class="text-[12px] font-extrabold text-lx-ink">Level {{ level }}</span>
                <span class="font-mono text-[11px] font-bold text-lx-ink-faint"><CountUp :value="currentExp" />/{{ expRequired }} XP</span>
              </div>
              <div class="h-2 w-full overflow-hidden rounded-full bg-lx-surface-soft">
                <div class="bar-grow h-full rounded-full bg-lx-beetle" :style="{ width: `${xpPct}%`, '--i': 3 }" />
              </div>
            </div>
          </div>

          <div class="anim-rise rounded-[20px] border-2 border-lx-line p-2" style="--i: 1">
            <div class="px-3 pb-1.5 pt-2 font-mono text-[10px] font-bold uppercase tracking-[0.08em] text-lx-ink-faint">Show</div>
            <button
              v-for="f in filters"
              :key="f.id"
              type="button"
              :class="[
                'flex w-full items-center justify-between rounded-xl px-3 py-2.5 text-left text-[13px] font-bold transition-colors duration-150',
                activeFilter === f.id ? 'bg-lx-macaw text-white' : 'text-lx-ink-soft hover:bg-lx-surface-soft',
              ]"
              @click="activeFilter = f.id"
            >
              {{ f.label }}
              <span :class="['font-mono text-[11.5px]', activeFilter === f.id ? 'text-white/70' : 'text-lx-ink-faint']">{{ f.count }}</span>
            </button>
          </div>
        </aside>

        <!-- Course list -->
        <section class="min-w-0">
          <div
            v-if="loadError"
            class="mb-4 flex items-center justify-between rounded-2xl border-2 border-red-200 bg-red-50 px-4 py-3 text-[13px] font-semibold text-red-600"
          >
            <span>{{ loadError }}</span>
            <button type="button" class="font-extrabold hover:opacity-70" @click="loadCourses">Retry</button>
          </div>

          <div class="mb-3 flex items-center gap-3">
            <label class="relative min-w-0 flex-1">
              <span class="sr-only">Search courses</span>
              <svg class="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-lx-ink-faint" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"><circle cx="11" cy="11" r="7" /><path d="m20 20-3.5-3.5" /></svg>
              <input
                v-model="query"
                type="search"
                placeholder="Search courses"
                class="h-11 w-full rounded-2xl border-2 border-lx-line bg-lx-surface-soft pl-10 pr-3 text-[13.5px] font-semibold text-lx-ink outline-none transition-colors placeholder:text-lx-ink-faint focus:border-lx-macaw focus:bg-white"
              />
            </label>
            <label class="flex items-center gap-2 text-[12.5px] font-bold text-lx-ink-soft">
              Sort
              <select
                v-model="sort"
                class="h-11 rounded-2xl border-2 border-lx-line bg-white px-3 text-[13px] font-bold text-lx-ink outline-none transition-colors focus:border-lx-macaw"
              >
                <option value="recommended">Recommended</option>
                <option value="az">A – Z</option>
                <option value="progress">Most progress</option>
              </select>
            </label>
          </div>

          <!-- Loading skeleton -->
          <div v-if="loading" class="overflow-hidden rounded-[20px] border-2 border-lx-line">
            <div v-for="i in 6" :key="i" class="flex items-center gap-4 border-t border-lx-line px-5 py-3.5 first:border-t-0">
              <div class="h-11 w-11 animate-pulse rounded-[14px] bg-lx-surface-soft" />
              <div class="flex-1 space-y-2">
                <div class="h-3.5 w-40 animate-pulse rounded bg-lx-surface-soft" />
                <div class="h-3 w-72 animate-pulse rounded bg-lx-surface-soft" />
              </div>
              <div class="h-8 w-24 animate-pulse rounded-xl bg-lx-surface-soft" />
            </div>
          </div>

          <!-- Rows -->
          <template v-else-if="paged.length">
            <div class="overflow-hidden rounded-[20px] border-2 border-lx-line">
              <CourseRow
                v-for="(course, index) in paged"
                :key="course.id"
                :course="course"
                class="anim-rise"
                :style="{ '--i': index % PAGE_SIZE }"
              />
            </div>
            <p class="mt-3 px-1 text-[12px] font-semibold text-lx-ink-faint">
              Showing {{ paged.length }} of {{ results.length }} {{ results.length === 1 ? 'course' : 'courses' }}
            </p>
            <div v-if="remaining > 0" class="mt-3 flex justify-center">
              <button
                type="button"
                class="press rounded-2xl border-2 border-lx-line bg-white px-6 py-2.5 text-[13px] font-extrabold text-lx-ink shadow-[0_4px_0_var(--color-lx-line)] transition-[transform,border-color] duration-100 hover:border-lx-ink-faint active:translate-y-1 active:shadow-none"
                @click="visibleCount += PAGE_SIZE"
              >
                Show {{ Math.min(PAGE_SIZE, remaining) }} more <span class="text-lx-ink-faint">· {{ remaining }} left</span>
              </button>
            </div>
          </template>

          <!-- Empty states -->
          <div v-else class="rounded-[20px] border-2 border-dashed border-lx-line py-16 text-center">
            <p class="font-display text-[16px] font-extrabold text-lx-ink-soft">
              {{ filtering ? 'No courses match' : 'No courses here yet' }}
            </p>
            <p class="mt-1 text-[13px] text-lx-ink-faint">
              {{ filtering ? 'Try a different search or filter.' : 'Published courses will show up here.' }}
            </p>
            <button v-if="filtering" type="button" class="mt-3 text-[13px] font-extrabold text-lx-macaw-dark hover:underline" @click="reset">Clear filters</button>
          </div>
        </section>
      </div>
    </div>
  </main>
</template>
