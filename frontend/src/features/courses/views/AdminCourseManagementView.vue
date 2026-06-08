<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { AxiosError } from 'axios'
import { useRouter } from 'vue-router'
import AppSidebar from '@/features/courses/components/Admin/AdminNavbar.vue'
import AdminIcon from '@/features/courses/components/Admin/AdminIcon.vue'
import MonoLabel from '@/features/courses/components/Admin/MonoLabel.vue'
import StatCard from '@/features/courses/components/Admin/StatCard.vue'
import StatusFilter from '@/features/courses/components/Admin/StatusFilter.vue'
import CourseCard from '@/features/courses/components/Admin/AdminCourseCard.vue'
import AddCourseModal from '@/features/courses/components/Admin/AddCoursePopUp.vue'
import CourseEditPopUp from '@/features/courses/components/Admin/CourseEditPopUp.vue'
import {
  createAdminCourse,
  deleteAdminCourse,
  listAdminCourses,
  submitAdminCourseForReview,
  toAdminCourseCard,
  updateAdminCourse,
  type AdminCourseCardModel,
  type CourseCreatePayload,
} from '@/features/courses/services/adminCourses'
import type { CourseStatus } from '@/types/types'

const router = useRouter()

const showAddModal = ref(false)
const searchQuery = ref('')
const statusFilter = ref<'all' | CourseStatus>('all')
const loadingCourses = ref(false)
const loadError = ref('')
const creatingCourse = ref(false)
const createError = ref('')

const showEditModal = ref(false)
const editingCourse = ref<AdminCourseCardModel | null>(null)
const updatingCourse = ref(false)
const updateError = ref('')

const courses = ref<AdminCourseCardModel[]>([])

const stats = computed(() => ({
  total: courses.value.length,
  published: courses.value.filter((c) => c.status === 'published').length,
  pending: courses.value.filter((c) => c.status === 'pending').length,
  revision: courses.value.filter((c) => c.status === 'revision').length,
  draft: courses.value.filter((c) => c.status === 'draft').length,
}))

const filteredCourses = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  return courses.value.filter((c) => {
    const matchesSearch = !q || c.title.toLowerCase().includes(q) || c.description.toLowerCase().includes(q)
    const matchesStatus = statusFilter.value === 'all' || c.status === statusFilter.value
    return matchesSearch && matchesStatus
  })
})

onMounted(loadCourses)

async function loadCourses() {
  loadingCourses.value = true
  loadError.value = ''
  try {
    const data = await listAdminCourses()
    courses.value = data.map(toAdminCourseCard)
  } catch (error) {
    loadError.value = getErrorMessage(error, 'Unable to load courses.')
  } finally {
    loadingCourses.value = false
  }
}

async function createCourse(payload: CourseCreatePayload) {
  creatingCourse.value = true
  createError.value = ''
  try {
    const created = await createAdminCourse(payload)
    courses.value = [toAdminCourseCard(created), ...courses.value]
    showAddModal.value = false
    await router.push(`/admin/courses/${created.id}`)
  } catch (error) {
    createError.value = getErrorMessage(error, 'Unable to create course.')
  } finally {
    creatingCourse.value = false
  }
}

function openAddModal() {
  createError.value = ''
  showAddModal.value = true
}

function closeAddModal() {
  if (creatingCourse.value) return
  showAddModal.value = false
  createError.value = ''
}

async function deleteCourse(id: string) {
  try {
    await deleteAdminCourse(id)
    courses.value = courses.value.filter((c) => c.id !== id)
  } catch (error) {
    loadError.value = getErrorMessage(error, 'Unable to delete course.')
  }
}

function openEditModal(course: AdminCourseCardModel) {
  editingCourse.value = course
  updateError.value = ''
  showEditModal.value = true
}

function closeEditModal() {
  if (updatingCourse.value) return
  showEditModal.value = false
  editingCourse.value = null
  updateError.value = ''
}

async function handleUpdateCourse(payload: { title: string; description: string; coverId: string }) {
  if (!editingCourse.value) return
  updatingCourse.value = true
  updateError.value = ''
  try {
    const updated = await updateAdminCourse(editingCourse.value.id, payload)
    const idx = courses.value.findIndex((c) => c.id === editingCourse.value!.id)
    if (idx !== -1) courses.value[idx] = toAdminCourseCard(updated)
    showEditModal.value = false
    editingCourse.value = null
  } catch (error) {
    updateError.value = getErrorMessage(error, 'Unable to update course.')
  } finally {
    updatingCourse.value = false
  }
}

async function submitCourseForReview(courseId: string) {
  try {
    const updated = await submitAdminCourseForReview(courseId)
    const idx = courses.value.findIndex((c) => c.id === courseId)
    if (idx !== -1) courses.value[idx] = toAdminCourseCard(updated)
  } catch (error) {
    loadError.value = getErrorMessage(error, 'Unable to submit course for review.')
  }
}

function getErrorMessage(error: unknown, fallback: string) {
  if (error instanceof AxiosError) {
    if (error.response?.status === 403) {
      const data = error.response.data as { error?: string; message?: string } | undefined
      return data?.error ?? data?.message ?? 'Session expired or permission denied. Please log in again.'
    }
    const data = error.response?.data as { error?: string; message?: string } | undefined
    return data?.error ?? data?.message ?? fallback
  }
  if (error instanceof Error) return error.message
  return fallback
}
</script>

<template>
  <div class="course-app flex h-screen w-full overflow-hidden bg-lm-bg">
    <AppSidebar active-item="course" />

    <div class="flex min-w-0 flex-1 flex-col overflow-hidden">
      <header class="shrink-0 border-b-2 border-lm-line bg-lm-surface px-7 py-4">
        <div class="flex items-center justify-between gap-4">
          <div>
            <h1 class="m-0 mb-[2px] font-display text-[23px] font-bold text-lm-ink">Course Management</h1>
            <MonoLabel>Manage, publish and track all learning content</MonoLabel>
          </div>

          <div class="flex items-center gap-3">
            <div class="relative">
              <span class="pointer-events-none absolute left-3 top-1/2 flex -translate-y-1/2 text-lm-ink-3">
                <AdminIcon name="search" :size="14" />
              </span>
              <input
                v-model="searchQuery"
                type="search"
                placeholder="Search courses…"
                class="h-11 w-[270px] rounded-[12px] border-2 border-lm-line-soft bg-lm-bg-soft pl-[38px] pr-3 font-display text-[12px] font-semibold text-lm-ink outline-none"
              />
            </div>

            <StatusFilter v-model="statusFilter" />

            <button
              type="button"
              class="inline-flex h-11 cursor-pointer items-center gap-2 rounded-full border-2 border-lm-line bg-lm-yellow px-5 font-display text-[13px] font-bold text-lm-ink shadow-stamp-sm"
              @click="openAddModal"
            >
              <AdminIcon name="plus" :size="14" />
              New Course
            </button>
          </div>
        </div>
      </header>

      <main class="relative flex-1 overflow-auto px-7 py-6">
        <div class="pointer-events-none absolute inset-0 bg-[radial-gradient(circle,#e3ddce_1px,transparent_1px)] bg-[length:18px_18px] opacity-40" />

        <div class="relative">
          <section class="mb-6 grid grid-cols-4 gap-4">
            <StatCard label="Total Courses" :value="stats.total" color="var(--lm-ink)" :sub="`+${stats.draft} draft`" :icon="{ name: 'courses', bg: 'var(--lm-purple-soft)', color: 'var(--lm-purple)' }" />
            <StatCard label="Published" :value="stats.published" color="var(--lm-green)" sub="Ready for learners" :icon="{ name: 'check', bg: 'var(--lm-green-soft)', color: 'var(--lm-green)' }" />
            <StatCard label="Pending Review" :value="stats.pending" color="var(--lm-purple)" sub="Awaiting teacher" :icon="{ name: 'publish', bg: 'var(--lm-purple-soft)', color: 'var(--lm-purple)' }" />
            <StatCard label="Needs Revision" :value="stats.revision" color="var(--lm-red)" sub="Teacher feedback" :icon="{ name: 'close', bg: 'var(--lm-red-soft)', color: 'var(--lm-red)' }" />
          </section>

          <div class="mb-4 flex items-center gap-3">
            <MonoLabel>Courses</MonoLabel>
            <div class="flex-1 border-t-2 border-lm-line-soft" />
            <span class="font-mono text-[11px] font-semibold text-lm-ink-3">{{ filteredCourses.length }}</span>
          </div>

          <!-- Error banner -->
          <div
            v-if="loadError"
            class="mb-4 flex items-center justify-between rounded-xl border-2 border-lm-red bg-lm-red-soft px-4 py-3 text-[13px] font-medium text-lm-red"
          >
            <span>{{ loadError }}</span>
            <button type="button" class="font-bold hover:opacity-70" @click="loadCourses">Retry</button>
          </div>

          <!-- Loading skeletons -->
          <div v-if="loadingCourses" class="space-y-3">
            <div v-for="index in 5" :key="index" class="h-[78px] animate-pulse rounded-[12px] border-2 border-lm-line-soft bg-lm-surface" />
          </div>

          <!-- Course list -->
          <div v-else-if="filteredCourses.length > 0" class="flex flex-col gap-3">
            <CourseCard
              v-for="course in filteredCourses"
              :key="course.id"
              :title="course.title"
              :description="course.description"
              :cover-id="course.coverId"
              :status="course.status"
              :module-count="course.moduleCount"
              :last-edited="course.lastEdited"
              :created-by="course.createdBy"
              @open="router.push(`/admin/courses/${course.id}`)"
              @edit="openEditModal(course)"
              @delete="deleteCourse(course.id)"
              @submit="submitCourseForReview(course.id)"
            />
          </div>

          <!-- Empty state -->
          <div
            v-else
            class="flex flex-col items-center justify-center rounded-[18px] border-2 border-dashed border-lm-line-soft bg-lm-surface py-24 text-center"
          >
            <div class="flex h-14 w-14 items-center justify-center rounded-[18px] border-2 border-lm-line bg-lm-yellow text-lm-ink shadow-stamp-sm">
              <AdminIcon name="courses" :size="28" />
            </div>
            <p class="mt-4 font-display text-[15px] font-semibold text-lm-ink">
              {{ searchQuery ? 'No courses found' : 'No courses yet' }}
            </p>
            <p class="mt-1 text-[12px] text-lm-ink-3">
              {{ searchQuery ? 'Try a different search term or filter.' : 'Create your first course to get started.' }}
            </p>
            <button
              v-if="!searchQuery"
              type="button"
              class="mt-5 inline-flex h-11 items-center rounded-full border-2 border-lm-line bg-lm-yellow px-5 text-[14px] font-bold text-lm-ink shadow-stamp-sm transition hover:-translate-y-px hover:shadow-stamp-md"
              @click="openAddModal"
            >
              New Course
            </button>
          </div>
        </div>
      </main>
    </div>

    <AddCourseModal
      :open="showAddModal"
      :submitting="creatingCourse"
      :error-message="createError"
      @close="closeAddModal"
      @create="createCourse"
    />

    <CourseEditPopUp
      :open="showEditModal"
      :course="editingCourse"
      :submitting="updatingCourse"
      :error-message="updateError"
      @close="closeEditModal"
      @save="handleUpdateCourse"
    />
  </div>
</template>
