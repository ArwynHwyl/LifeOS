<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { AxiosError } from 'axios'
import { useRoute, useRouter } from 'vue-router'
import TeacherNavbar from '@/features/courses/components/Teacher/TeacherNavbar.vue'
import TeacherTopicCard, { type Comment } from '@/features/courses/components/Teacher/TeacherTopicCard.vue'
import { DEFAULT_COVER_ID, getCoverPreset } from '@/features/courses/constants/courseCoverPresets'
import {
  getTeacherCourse,
  approveTeacherCourse,
  rejectTeacherCourse,
} from '@/features/courses/services/teacherCourses'
import type { AdminCourseDetailDto } from '@/features/courses/services/adminCourses'
import type { CourseStatus } from '@/types/types'

const COURSE_COVER_STORAGE_KEY = 'lifeosCourseCovers'

const route = useRoute()
const router = useRouter()

const courseId = computed(() => String(route.params.id ?? ''))
const course = ref<AdminCourseDetailDto | null>(null)
const loading = ref(false)
const loadError = ref('')
const approving = ref(false)
const rejecting = ref(false)

const currentUser = computed(() => {
  const raw = localStorage.getItem('authUser')
  if (!raw) return { userId: 'teacher', username: 'Teacher' }
  try { return JSON.parse(raw) as { userId: string; username: string } } catch { return { userId: 'teacher', username: 'Teacher' } }
})

const coverPreset = computed(() => {
  if (!course.value) return getCoverPreset(DEFAULT_COVER_ID)
  const raw = localStorage.getItem(COURSE_COVER_STORAGE_KEY)
  let covers: Record<string, string> = {}
  if (raw) { try { covers = JSON.parse(raw) as Record<string, string> } catch { /* empty */ } }
  const coverId = covers[String(course.value.id)] ?? DEFAULT_COVER_ID
  return getCoverPreset(coverId)
})

const modules = computed(() => course.value?.modules ?? [])

const commentsMap = ref<Record<string, Comment[]>>({})

const courseStatus = computed<CourseStatus>(() => {
  if (!course.value) return 'draft'
  const s = course.value.status
  if (s === 'PUBLISHED' || s === 'APPROVED') return 'published'
  if (s === 'PENDING_REVIEW') return 'pending'
  if (s === 'NEED_REVISION') return 'revision'
  return 'draft'
})

onMounted(loadCourse)

async function loadCourse() {
  loading.value = true
  loadError.value = ''
  try {
    course.value = await getTeacherCourse(courseId.value)
  } catch (error) {
    loadError.value = getErrorMessage(error, 'Unable to load course.')
  } finally {
    loading.value = false
  }
}

async function handleApprove() {
  if (!course.value || approving.value) return
  approving.value = true
  try {
    await approveTeacherCourse(course.value.id)
    router.push('/teacher/courses')
  } catch {
    approving.value = false
  }
}

async function handleReject() {
  if (!course.value || rejecting.value) return
  rejecting.value = true
  try {
    await rejectTeacherCourse(course.value.id)
    router.push('/teacher/courses')
  } catch {
    rejecting.value = false
  }
}

function addComment(modId: string, text: string) {
  if (!commentsMap.value[modId]) commentsMap.value[modId] = []
  commentsMap.value[modId].push({
    id: `c-${Date.now()}`,
    authorId: currentUser.value.userId,
    authorName: currentUser.value.username,
    text,
    createdAt: 'just now',
  })
}

function editComment(modId: string, payload: { id: string; text: string }) {
  const list = commentsMap.value[modId]
  if (!list) return
  const i = list.findIndex((c) => c.id === payload.id)
  if (i !== -1) list[i] = { ...list[i], text: payload.text }
}

function deleteComment(modId: string, commentId: string) {
  if (!commentsMap.value[modId]) return
  commentsMap.value[modId] = commentsMap.value[modId].filter((c) => c.id !== commentId)
}

function endDiscussion(modId: string) {
  commentsMap.value[modId] = []
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

      <!-- Breadcrumb header -->
      <div class="flex shrink-0 items-center justify-between border-b-2 border-lm-line bg-lm-surface px-7 py-3">
        <div class="flex items-center gap-1.5 text-[12px]">
          <button
            type="button"
            class="font-semibold text-lm-ink-3 transition hover:text-lm-ink"
            @click="router.push('/teacher/courses')"
          >
            Courses
          </button>
          <svg class="h-3.5 w-3.5 text-lm-line-soft" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="9 18 15 12 9 6" />
          </svg>
          <span class="max-w-[260px] truncate font-bold text-lm-ink">
            {{ course?.title ?? 'Course' }}
          </span>
        </div>

        <!-- Approve / Reject -->
        <div v-if="course" class="flex items-center gap-2">
          <button
            type="button"
            class="inline-flex items-center gap-1.5 rounded-lg border-2 border-lm-green bg-lm-green-soft px-3 py-1.5 text-[12px] font-semibold text-lm-green shadow-stamp-sm transition-all duration-200 hover:-translate-y-px hover:shadow-stamp-md disabled:cursor-not-allowed disabled:opacity-50"
            :disabled="approving || rejecting"
            @click="handleApprove"
          >
            <svg class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
              <polyline points="20 6 9 17 4 12" />
            </svg>
            {{ approving ? 'Approving…' : 'Approve' }}
          </button>
          <button
            type="button"
            class="inline-flex items-center gap-1.5 rounded-lg border-2 border-lm-red bg-lm-red-soft px-3 py-1.5 text-[12px] font-semibold text-lm-red shadow-stamp-sm transition-all duration-200 hover:-translate-y-px hover:shadow-stamp-md disabled:cursor-not-allowed disabled:opacity-50"
            :disabled="approving || rejecting"
            @click="handleReject"
          >
            <svg class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
              <path d="M18 6 6 18M6 6l12 12" />
            </svg>
            {{ rejecting ? 'Sending…' : 'Request Revision' }}
          </button>
        </div>
      </div>

      <!-- Scrollable content -->
      <main class="flex-1 overflow-y-auto px-7 py-6 relative">
        <div class="absolute inset-0 bg-dot-grid opacity-40 pointer-events-none" />

        <div class="relative">
          <!-- Error banner -->
          <div
            v-if="loadError"
            class="mb-4 flex items-center justify-between rounded-xl border-2 border-lm-red bg-lm-red-soft px-4 py-3 text-[13px] font-medium text-lm-red"
          >
            <span>{{ loadError }}</span>
            <button type="button" class="font-bold hover:opacity-70" @click="loadCourse">Retry</button>
          </div>

          <!-- Loading -->
          <div v-if="loading" class="rounded-[18px] border-2 border-lm-line bg-lm-surface px-5 py-4 text-[13px] text-lm-ink-3 shadow-stamp-sm">
            Loading course...
          </div>

          <template v-else-if="course">
            <!-- Course summary card -->
            <div class="mb-6 rounded-[18px] bg-lm-surface border-2 border-lm-line px-6 py-5 shadow-stamp-sm">
              <div class="flex items-center gap-5">
                <div
                  class="flex h-[72px] w-[72px] shrink-0 items-center justify-center rounded-[18px] border-2 border-lm-line font-display text-[32px] font-bold shadow-stamp-md"
                  :class="[coverPreset.bgClass, coverPreset.textClass]"
                >
                  {{ coverPreset.symbol }}
                </div>
                <h1 class="font-display text-[17px] font-bold tracking-tight text-lm-ink">{{ course.title }}</h1>
              </div>
              <div class="mt-2 pl-[92px]">
                <p class="text-[12px] leading-relaxed text-lm-ink-2 line-clamp-2">{{ course.description }}</p>
                <div class="mt-2 flex items-center gap-3">
                  <span class="inline-flex items-center gap-1 font-mono text-[11px] text-lm-ink-3">
                    <svg class="h-3 w-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                      <polyline points="14 2 14 8 20 8" />
                    </svg>
                    {{ modules.length }} module{{ modules.length === 1 ? '' : 's' }}
                  </span>
                  <span
                    v-if="courseStatus === 'published'"
                    class="inline-flex items-center gap-1 rounded-full border-2 border-lm-line bg-lm-green-soft px-2 py-0.5 font-mono text-[10px] font-bold text-lm-green shadow-stamp-sm"
                  >
                    <span class="h-1.5 w-1.5 rounded-full bg-lm-green" />
                    Published
                  </span>
                  <span
                    v-else-if="courseStatus === 'pending'"
                    class="inline-flex items-center gap-1 rounded-full border-2 border-lm-line bg-lm-yellow px-2 py-0.5 font-mono text-[10px] font-bold text-lm-ink shadow-stamp-sm"
                  >
                    <span class="h-1.5 w-1.5 rounded-full bg-lm-ink" />
                    Pending Review
                  </span>
                  <span
                    v-else-if="courseStatus === 'revision'"
                    class="inline-flex items-center gap-1 rounded-full border-2 border-lm-line bg-lm-red-soft px-2 py-0.5 font-mono text-[10px] font-bold text-lm-red shadow-stamp-sm"
                  >
                    <span class="h-1.5 w-1.5 rounded-full bg-lm-red" />
                    Needs Revision
                  </span>
                  <span
                    v-else
                    class="inline-flex items-center gap-1 rounded-full border-2 border-lm-line-soft bg-lm-bg-soft px-2 py-0.5 font-mono text-[10px] font-bold text-lm-ink-3"
                  >
                    <span class="h-1.5 w-1.5 rounded-full bg-lm-ink-3" />
                    Draft
                  </span>
                </div>
              </div>
            </div>

            <!-- Modules section label -->
            <div class="mb-3 flex items-center justify-between px-0.5">
              <p class="font-mono text-[10px] font-bold uppercase tracking-[0.13em] text-lm-ink-3">Modules</p>
              <span class="font-mono text-[11px] text-lm-ink-3">{{ modules.length }}</span>
            </div>

            <!-- Modules list -->
            <div v-if="modules.length > 0" class="flex flex-col gap-3">
              <TeacherTopicCard
                v-for="(mod, index) in modules"
                :key="mod.id"
                :index="index + 1"
                :name="mod.title"
                :content="''"
                :sub-topics="mod.subTopics"
                :comments="commentsMap[String(mod.id)] ?? []"
                :current-user-id="currentUser.userId"
                @add-comment="addComment(String(mod.id), $event)"
                @edit-comment="editComment(String(mod.id), $event)"
                @delete-comment="deleteComment(String(mod.id), $event)"
                @end-discussion="endDiscussion(String(mod.id))"
              />
            </div>

            <!-- Empty modules -->
            <div
              v-else
              class="flex flex-col items-center justify-center rounded-[18px] border-2 border-dashed border-lm-line-soft bg-lm-surface py-20 text-center"
            >
              <div class="flex h-14 w-14 items-center justify-center rounded-[18px] bg-lm-yellow border-2 border-lm-line shadow-stamp-sm text-lm-ink">
                <svg class="h-7 w-7" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                  <polyline points="14 2 14 8 20 8" />
                </svg>
              </div>
              <p class="mt-4 font-display text-[15px] font-semibold text-lm-ink">No modules yet</p>
              <p class="mt-1 text-[12px] text-lm-ink-3">This course has no modules to review.</p>
            </div>
          </template>

          <!-- Course not found -->
          <div
            v-else-if="!loading && !loadError"
            class="flex flex-col items-center justify-center rounded-[18px] border-2 border-dashed border-lm-line-soft bg-lm-surface py-20 text-center"
          >
            <p class="text-[13px] text-lm-ink-3">This course does not exist or has been removed.</p>
            <button
              type="button"
              class="mt-4 rounded-full bg-lm-yellow border-2 border-lm-line px-5 py-2.5 text-[13px] font-bold text-lm-ink shadow-stamp-sm transition-all duration-200 hover:-translate-y-px hover:shadow-stamp-md"
              @click="router.push('/teacher/courses')"
            >
              Go to Courses
            </button>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>
