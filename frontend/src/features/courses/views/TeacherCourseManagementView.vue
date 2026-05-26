<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { AxiosError } from 'axios'
import { useRouter } from 'vue-router'
import TeacherNavbar from '@/features/courses/components/Teacher/TeacherNavbar.vue'
import TeacherCourseCard from '@/features/courses/components/Teacher/TeacherCourseCard.vue'
import {
  listTeacherCourses,
  toTeacherCourseCard,
  approveTeacherCourse,
  rejectTeacherCourse,
  type TeacherCourseCardModel,
} from '@/features/courses/services/teacherCourses'
const router = useRouter()

const searchQuery = ref('')
const loadingCourses = ref(false)
const loadError = ref('')

const courses = ref<TeacherCourseCardModel[]>([])

const stats = computed(() => ({
  total: courses.value.length,
  published: courses.value.filter((c) => c.status === 'published').length,
  pending: courses.value.filter((c) => c.status === 'pending').length,
  revision: courses.value.filter((c) => c.status === 'revision').length,
}))

const filteredCourses = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  return courses.value.filter((c) =>
    !q || c.title.toLowerCase().includes(q) || c.description.toLowerCase().includes(q)
  )
})

onMounted(loadCourses)

async function loadCourses() {
  loadingCourses.value = true
  loadError.value = ''
  try {
    const data = await listTeacherCourses()
    courses.value = data.map(toTeacherCourseCard)
  } catch (error) {
    loadError.value = getErrorMessage(error, 'Unable to load courses.')
  } finally {
    loadingCourses.value = false
  }
}

async function approveCourse(id: string) {
  try {
    await approveTeacherCourse(id)
    await loadCourses()
  } catch {
    // silently ignore — user can retry
  }
}

async function rejectCourse(id: string) {
  try {
    await rejectTeacherCourse(id)
    await loadCourses()
  } catch {
    // silently ignore — user can retry
  }
}

function getErrorMessage(error: unknown, fallback: string) {
  if (error instanceof AxiosError) {
    const data = error.response?.data as { error?: string; message?: string } | undefined
    return data?.error ?? data?.message ?? fallback
  }
  return fallback
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
          <span class="inline-flex items-center gap-1.5 rounded-full border-2 border-lm-line bg-lm-red-soft px-2.5 py-1 font-mono text-[11px] font-semibold text-lm-red shadow-stamp-sm">
            <span class="h-1.5 w-1.5 rounded-full bg-lm-red" />
            {{ stats.revision }} revision
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

        </div>
      </div>

      <!-- Scrollable grid -->
      <main class="flex-1 overflow-y-auto px-7 py-6 relative">
        <div class="absolute inset-0 bg-dot-grid opacity-40 pointer-events-none" />

        <div class="relative">
          <!-- Error banner -->
          <div
            v-if="loadError"
            class="mb-4 flex items-center justify-between rounded-xl border-2 border-lm-red bg-lm-red-soft px-4 py-3 text-[13px] font-medium text-lm-red"
          >
            <span>{{ loadError }}</span>
            <button type="button" class="font-bold hover:opacity-70" @click="loadCourses">Retry</button>
          </div>

          <!-- Loading skeletons -->
          <div v-if="loadingCourses" class="grid gap-[22px] sm:grid-cols-3 xl:grid-cols-4">
            <div v-for="index in 8" :key="index" class="h-[260px] animate-pulse rounded-[18px] bg-lm-surface border-2 border-lm-line-soft" />
          </div>

          <!-- Course grid -->
          <div
            v-else-if="filteredCourses.length > 0"
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
            <p class="mt-4 font-display text-[15px] font-semibold text-lm-ink">
              {{ searchQuery ? 'No courses found' : 'No courses yet' }}
            </p>
            <p class="mt-1 text-[12px] text-lm-ink-3">
              {{ searchQuery ? 'Try a different search term or filter.' : 'No courses have been assigned to you yet.' }}
            </p>
          </div>
        </div>
      </main>
    </div>

  </div>
</template>
