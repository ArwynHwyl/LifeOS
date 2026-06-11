<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
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

const showDeleteConfirm = ref(false)
const deleteTargetId = ref<string | null>(null)
const deleteTargetTitle = ref('')
const deletingCourseFlag = ref(false)

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

function confirmDeleteCourse(course: AdminCourseCardModel) {
  deleteTargetId.value = course.id
  deleteTargetTitle.value = course.title
  showDeleteConfirm.value = true
}

async function proceedDeleteCourse() {
  if (!deleteTargetId.value || deletingCourseFlag.value) return
  deletingCourseFlag.value = true
  try {
    await deleteAdminCourse(deleteTargetId.value)
    courses.value = courses.value.filter((c) => c.id !== deleteTargetId.value)
    showDeleteConfirm.value = false
  } catch (error) {
    loadError.value = getErrorMessage(error, 'Unable to delete course.')
  } finally {
    deletingCourseFlag.value = false
  }
}

watch(showDeleteConfirm, (open) => {
  if (open) {
    window.addEventListener('keydown', handleGlobalKeydown)
  } else {
    window.removeEventListener('keydown', handleGlobalKeydown)
  }
})

function handleGlobalKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape') {
    showDeleteConfirm.value = false
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
              @delete="confirmDeleteCourse(course)"
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

    <!-- Confirm delete course modal -->
    <Teleport to="body">
      <Transition
        enter-active-class="transition duration-200 ease-out"
        enter-from-class="opacity-0"
        enter-to-class="opacity-100"
        leave-active-class="transition duration-150 ease-in"
        leave-from-class="opacity-100"
        leave-to-class="opacity-0"
      >
        <div v-if="showDeleteConfirm" class="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div class="absolute inset-0 bg-lm-ink/60 backdrop-blur-sm" aria-hidden="true" @click="showDeleteConfirm = false" />

          <Transition
            enter-active-class="transition duration-200 ease-out"
            enter-from-class="opacity-0 scale-95 translate-y-2"
            enter-to-class="opacity-100 scale-100 translate-y-0"
            leave-active-class="transition duration-150 ease-in"
            leave-from-class="opacity-100 scale-100 translate-y-0"
            leave-to-class="opacity-0 scale-95 translate-y-2"
          >
            <div
              v-if="showDeleteConfirm"
              role="dialog"
              aria-modal="true"
              aria-labelledby="confirm-delete-course-title"
              class="relative z-10 flex w-full max-w-md flex-col overflow-hidden rounded-[18px] border-2 border-lm-line bg-lm-surface shadow-stamp-md"
              @click.stop
            >
              <!-- Header -->
              <header class="shrink-0 border-b-2 border-lm-line px-6 py-5">
                <div class="flex items-start justify-between gap-4">
                  <div class="flex items-center gap-3">
                    <div class="flex h-9 w-9 items-center justify-center rounded-[10px] border-2 border-lm-line bg-lm-red-soft text-lm-red shadow-stamp-sm">
                      <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z" />
                        <line x1="12" y1="9" x2="12" y2="13" />
                        <line x1="12" y1="17" x2="12.01" y2="17" />
                      </svg>
                    </div>
                    <div>
                      <h2 id="confirm-delete-course-title" class="font-display text-[15px] font-bold text-lm-ink">Delete Course?</h2>
                      <p class="mt-1 text-[11px] text-lm-ink-3">This action cannot be undone</p>
                    </div>
                  </div>
                  <button
                    type="button"
                    class="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg text-lm-ink-3 transition hover:bg-lm-bg hover:text-lm-ink"
                    aria-label="Close"
                    @click="showDeleteConfirm = false"
                  >
                    <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M18 6 6 18M6 6l12 12" />
                    </svg>
                  </button>
                </div>
              </header>

              <!-- Body -->
              <div class="px-6 py-5">
                <p class="text-[13px] leading-relaxed text-lm-ink-2">
                  Are you sure you want to delete the course <strong class="text-lm-red">"{{ deleteTargetTitle }}"</strong>?
                  All modules, topics, and configurations associated with this course will be permanently removed.
                </p>
              </div>

              <!-- Footer -->
              <footer class="flex shrink-0 gap-2.5 border-t-2 border-lm-line px-6 py-4">
                <button
                  type="button"
                  class="h-10 flex-1 rounded-full border-2 border-lm-line bg-lm-surface px-4 text-[13px] font-bold text-lm-ink shadow-stamp-sm transition hover:-translate-y-px hover:shadow-stamp-md"
                  @click="showDeleteConfirm = false"
                >
                  Cancel
                </button>
                <button
                  type="button"
                  class="h-10 flex-1 rounded-full border-2 border-lm-line bg-lm-red text-white px-4 text-[13px] font-bold shadow-stamp-sm transition hover:-translate-y-px hover:shadow-stamp-md disabled:cursor-not-allowed disabled:opacity-50"
                  :disabled="deletingCourseFlag"
                  @click="proceedDeleteCourse"
                >
                  {{ deletingCourseFlag ? 'Deleting...' : 'Delete' }}
                </button>
              </footer>
            </div>
          </Transition>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>
