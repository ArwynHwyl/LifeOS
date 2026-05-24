<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import TeacherNavbar from '@/features/courses/components/Teacher/TeacherNavbar.vue'
import TeacherCourseCard from '@/features/courses/components/Teacher/TeacherCourseCard.vue'
import type { CourseStatus } from '@/types/types'

const router = useRouter()

const searchQuery = ref('')
const statusFilter = ref<'all' | CourseStatus>('all')
const showStatusMenu = ref(false)

type MockCourse = {
  id: string
  title: string
  description: string
  coverId: string
  status: CourseStatus
  moduleCount: number
  lastEdited: string
}

const courses = ref<MockCourse[]>([
  {
    id: '1',
    title: 'Quadratic Functions',
    description: 'Explore parabolas, vertex form, and real-world quadratic models.',
    coverId: 'integral',
    status: 'published',
    moduleCount: 4,
    lastEdited: '2 days ago',
  },
  {
    id: '2',
    title: 'Intro to Linear Algebra',
    description: 'Vectors, matrices, and systems of linear equations.',
    coverId: 'sigma',
    status: 'published',
    moduleCount: 6,
    lastEdited: '1 week ago',
  },
  {
    id: '3',
    title: 'Probability Basics',
    description: 'Foundations of probability, events, and distributions.',
    coverId: 'pi',
    status: 'draft',
    moduleCount: 3,
    lastEdited: 'today',
  },
  {
    id: '4',
    title: 'Calculus I: Limits',
    description: 'Limits, continuity, and introductory differential calculus.',
    coverId: 'fx',
    status: 'published',
    moduleCount: 8,
    lastEdited: '3 days ago',
  },
])

const stats = computed(() => ({
  total: courses.value.length,
  published: courses.value.filter((c) => c.status === 'published').length,
  pending: courses.value.filter((c) => c.status === 'draft').length,
}))

const filteredCourses = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  return courses.value.filter((c) => {
    const matchesSearch = !q || c.title.toLowerCase().includes(q) || c.description.toLowerCase().includes(q)
    const matchesStatus = statusFilter.value === 'all' || c.status === statusFilter.value
    return matchesSearch && matchesStatus
  })
})

const statusLabel = computed(() => {
  if (statusFilter.value === 'published') return 'Published'
  if (statusFilter.value === 'draft') return 'Pending'
  return 'All'
})

function approveCourse(id: string) {
  const c = courses.value.find((c) => c.id === id)
  if (c) c.status = 'published'
}

function rejectCourse(id: string) {
  const c = courses.value.find((c) => c.id === id)
  if (c) c.status = 'draft'
}

function setStatus(val: 'all' | CourseStatus) {
  statusFilter.value = val
  showStatusMenu.value = false
}
</script>

<template>
  <div class="course-app flex h-screen w-full overflow-hidden bg-lm-bg">
    <TeacherNavbar active-item="courses" />

    <div class="flex min-w-0 flex-1 flex-col overflow-hidden">
      <!-- Toolbar header -->
      <div class="flex shrink-0 items-center justify-between border-b-2 border-lm-line bg-lm-surface px-7 py-3">
        <!-- Stats chips -->
        <div class="flex items-center gap-2">
          <span class="inline-flex items-center gap-1.5 rounded-full border-2 border-lm-line bg-lm-surface px-2.5 py-1 font-mono text-[11px] font-medium text-lm-ink shadow-stamp-sm">
            <span class="font-bold">{{ stats.total }}</span> total
          </span>
          <span class="inline-flex items-center gap-1.5 rounded-full border-2 border-lm-line bg-lm-green-soft px-2.5 py-1 font-mono text-[11px] font-semibold text-lm-green shadow-stamp-sm">
            <span class="h-1.5 w-1.5 rounded-full bg-lm-green" />
            {{ stats.published }} published
          </span>
          <span class="inline-flex items-center gap-1.5 rounded-full border-2 border-lm-line bg-lm-yellow px-2.5 py-1 font-mono text-[11px] font-semibold text-lm-ink shadow-stamp-sm">
            <span class="h-1.5 w-1.5 rounded-full bg-lm-ink" />
            {{ stats.pending }} pending
          </span>
        </div>

        <!-- Controls -->
        <div class="flex items-center gap-2">
          <!-- Search -->
          <div class="relative">
            <svg class="pointer-events-none absolute left-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-lm-ink-3"
              viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8" /><path d="m21 21-4.3-4.3" />
            </svg>
            <input
              v-model="searchQuery"
              type="search"
              placeholder="Search…"
              class="w-48 rounded-lg border-2 border-lm-line-soft bg-lm-bg-soft py-1.5 pl-8 pr-3 text-[12px] text-lm-ink outline-none transition placeholder:text-lm-ink-3 focus:border-lm-line focus:bg-lm-surface focus:ring-2 focus:ring-lm-yellow/40"
            />
          </div>

          <!-- Status filter -->
          <div class="relative">
            <button
              type="button"
              class="inline-flex items-center gap-1.5 rounded-lg border-2 border-lm-line-soft bg-lm-surface px-3 py-1.5 text-[12px] font-semibold text-lm-ink transition hover:border-lm-line hover:bg-lm-bg-soft"
              @click="showStatusMenu = !showStatusMenu"
            >
              <span v-if="statusFilter !== 'all'" class="h-1.5 w-1.5 rounded-full"
                :class="statusFilter === 'published' ? 'bg-lm-green' : 'bg-lm-ink'" />
              {{ statusLabel }}
              <svg class="h-3 w-3 text-lm-ink-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="6 9 12 15 18 9" />
              </svg>
            </button>
            <div v-if="showStatusMenu"
              class="absolute right-0 top-full z-20 mt-1.5 w-36 overflow-hidden rounded-xl border-2 border-lm-line bg-lm-surface shadow-stamp-md">
              <button v-for="[val, label] in [['all','All'],['published','Published'],['draft','Pending']]" :key="val"
                type="button"
                class="flex w-full items-center gap-2.5 px-3.5 py-2.5 text-[12px] transition hover:bg-lm-bg-soft"
                :class="statusFilter === val ? 'font-bold text-lm-ink bg-lm-yellow/40' : 'text-lm-ink-2'"
                @click="setStatus(val as 'all' | 'published' | 'draft')">
                <span class="h-1.5 w-1.5 rounded-full"
                  :class="val === 'published' ? 'bg-lm-green' : val === 'draft' ? 'bg-lm-ink' : 'bg-lm-line-soft'" />
                {{ label }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Scrollable grid -->
      <main class="flex-1 overflow-y-auto px-7 py-6 relative">
        <div class="absolute inset-0 bg-dot-grid opacity-40 pointer-events-none" />

        <div class="relative">
          <div
            v-if="filteredCourses.length > 0"
            class="grid gap-[22px] sm:grid-cols-3 xl:grid-cols-4"
          >
            <TeacherCourseCard
              v-for="course in filteredCourses"
              :key="course.id"
              :title="course.title"
              :description="course.description"
              :cover-id="course.coverId"
              :status="course.status"
              :module-count="course.moduleCount"
              :last-edited="course.lastEdited"
              @open="router.push(`/teacher/courses/${course.id}`)"
              @approve="approveCourse(course.id)"
              @reject="rejectCourse(course.id)"
            />
          </div>

          <!-- Empty state -->
          <div
            v-else
            class="flex flex-col items-center justify-center rounded-[18px] border-2 border-dashed border-lm-line-soft bg-lm-surface py-24 text-center"
          >
            <div class="flex h-14 w-14 items-center justify-center rounded-[18px] bg-lm-yellow border-2 border-lm-line shadow-stamp-sm text-lm-ink">
              <svg class="h-7 w-7" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" />
                <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" />
              </svg>
            </div>
            <p class="mt-4 font-display text-[15px] font-semibold text-lm-ink">No courses found</p>
            <p class="mt-1 text-[12px] text-lm-ink-3">Try a different search term or filter.</p>
          </div>
        </div>
      </main>
    </div>

    <div v-if="showStatusMenu" class="fixed inset-0 z-10" aria-hidden="true" @click="showStatusMenu = false" />
  </div>
</template>
