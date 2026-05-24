<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { AxiosError } from 'axios'
import { useRouter } from 'vue-router'
import AppSidebar from '@/features/courses/components/Admin/AdminNavbar.vue'
import CourseCard from '@/features/courses/components/Admin/AdminCourseCard.vue'
import AddCourseModal from '@/features/courses/components/Admin/AddCoursePopUp.vue'
import EditCourseModal from '@/features/courses/components/Admin/EditCoursePopUp.vue'
import type { EditableCourse } from '@/features/courses/components/Admin/EditCoursePopUp.vue'
import { DEFAULT_COVER_ID } from '@/features/courses/constants/courseCoverPresets'
import {
  createAdminCourse,
  listAdminCourses,
  toAdminCourseCard,
  type AdminCourseCardModel,
  type CourseCreatePayload,
} from '@/features/courses/services/adminCourses'
import type { CourseStatus } from '@/types/types'

const router = useRouter()

const showAddModal = ref(false)
const showEditModal = ref(false)
const editingCourse = ref<EditableCourse | null>(null)
const searchQuery = ref('')
const statusFilter = ref<'all' | CourseStatus>('all')
const showStatusMenu = ref(false)
const loadingCourses = ref(false)
const loadError = ref('')
const creatingCourse = ref(false)
const createError = ref('')

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

const statusLabel = computed(() => {
  if (statusFilter.value === 'published') return 'Published'
  if (statusFilter.value === 'pending') return 'Pending Review'
  if (statusFilter.value === 'revision') return 'Needs Revision'
  if (statusFilter.value === 'draft') return 'Draft'
  return 'All'
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
    await router.push(`/courses/${created.id}`)
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

function openEdit(course: AdminCourseCardModel) {
  editingCourse.value = { id: course.id, title: course.title, description: course.description, coverId: course.coverId }
  showEditModal.value = true
}

function closeEdit() {
  showEditModal.value = false
  editingCourse.value = null
}

function onSave(updated: EditableCourse) {
  const i = courses.value.findIndex((c) => c.id === updated.id)
  if (i !== -1) {
    courses.value[i] = {
      ...courses.value[i],
      title: updated.title,
      description: updated.description,
      coverId: updated.coverId || DEFAULT_COVER_ID,
    }
  }
}

function deleteCourse(id: string) {
  courses.value = courses.value.filter((c) => c.id !== id)
}

function setStatus(val: 'all' | CourseStatus) {
  statusFilter.value = val
  showStatusMenu.value = false
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
  <div class="course-app flex h-screen w-full overflow-hidden bg-[#f4f5f9]">
    <AppSidebar active-item="course" />

    <div class="flex min-w-0 flex-1 flex-col overflow-hidden bg-[#f1f2f8]">
      <header class="shrink-0 border-b border-slate-200/70 bg-white px-7 py-4">
        <div class="flex items-center justify-between gap-4">
          <div>
            <h1 class="font-serif text-[23px] font-bold leading-tight text-[#25234d]">Course Management</h1>
            <p class="mt-0.5 text-[12px] font-medium text-slate-400">Manage, publish and track all learning content</p>
          </div>

          <div class="relative">
            <svg class="pointer-events-none absolute left-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-slate-400"
              viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8" /><path d="m21 21-4.3-4.3" />
            </svg>
            <input
              v-model="searchQuery"
              type="search"
              placeholder="Search courses..."
              class="h-11 w-72 rounded-xl border border-[#dedff0] bg-[#f7f7fd] py-2 pl-10 pr-3 text-[12px] font-semibold text-slate-700 outline-none transition placeholder:text-slate-400 focus:border-[#9d92ff] focus:bg-white focus:ring-2 focus:ring-[#5b4cfa]/10"
            />
          </div>

          <div class="relative">
            <button
              type="button"
              class="inline-flex h-11 min-w-32 items-center justify-between gap-2 rounded-xl border border-[#dedff0] bg-[#f7f7fd] px-4 text-[12px] font-bold text-slate-500 transition hover:bg-white"
              @click="showStatusMenu = !showStatusMenu"
            >
              <span v-if="statusFilter !== 'all'" class="h-1.5 w-1.5 rounded-full"
                :class="statusFilter === 'published' ? 'bg-emerald-500' : statusFilter === 'pending' ? 'bg-violet-500' : statusFilter === 'revision' ? 'bg-red-500' : 'bg-slate-400'" />
              {{ statusLabel }}
              <svg class="h-3 w-3 text-slate-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="6 9 12 15 18 9" />
              </svg>
            </button>
            <div v-if="showStatusMenu"
              class="absolute right-0 top-full z-20 mt-1.5 w-36 overflow-hidden rounded-xl border border-slate-200 bg-white shadow-lg shadow-slate-900/10">
              <button v-for="[val, label] in [['all','All status'],['published','Published'],['pending','Pending Review'],['revision','Needs Revision'],['draft','Draft']]" :key="val"
                type="button"
                class="flex w-full items-center gap-2.5 px-3.5 py-2.5 text-[12px] transition hover:bg-slate-50"
                :class="statusFilter === val ? 'font-semibold text-[#5b4cfa]' : 'text-slate-600'"
                @click="setStatus(val as 'all' | CourseStatus)">
                <span class="h-1.5 w-1.5 rounded-full"
                  :class="val === 'published' ? 'bg-emerald-500' : val === 'pending' ? 'bg-violet-500' : val === 'revision' ? 'bg-red-500' : val === 'draft' ? 'bg-slate-400' : 'bg-slate-300'" />
                {{ label }}
              </button>
            </div>
          </div>

          <button
            type="button"
            class="inline-flex h-11 items-center gap-2 rounded-full bg-[#5b4cfa] px-5 text-[12px] font-bold text-white shadow-lg shadow-[#5b4cfa]/25 transition hover:bg-[#493be0] active:scale-[0.98]"
            @click="openAddModal"
          >
            <svg class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
              <line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" />
            </svg>
            New Course
          </button>
        </div>
      </header>

      <main class="flex-1 overflow-y-auto px-7 py-6">
        <section class="mb-6 grid gap-4 xl:grid-cols-4">
          <div class="rounded-xl bg-white p-5 shadow-sm shadow-slate-200/60 ring-1 ring-slate-900/[0.04]">
            <div class="flex items-start justify-between">
              <p class="text-[10px] font-bold uppercase tracking-[0.14em] text-slate-400">Total Courses</p>
              <span class="flex h-8 w-8 items-center justify-center rounded-lg bg-[#f0edff] text-[#5b4cfa]">
                <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" /><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" /></svg>
              </span>
            </div>
            <p class="mt-6 font-serif text-[28px] font-bold text-[#25234d]">{{ stats.total }}</p>
            <p class="mt-1 text-[11px] font-semibold text-[#8d82ff]">+{{ stats.draft }} draft</p>
          </div>

          <div class="rounded-xl bg-white p-5 shadow-sm shadow-slate-200/60 ring-1 ring-slate-900/[0.04]">
            <div class="flex items-start justify-between">
              <p class="text-[10px] font-bold uppercase tracking-[0.14em] text-slate-400">Published</p>
              <span class="flex h-8 w-8 items-center justify-center rounded-lg bg-emerald-50 text-emerald-600">
                <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 6 9 17l-5-5" /></svg>
              </span>
            </div>
            <p class="mt-6 font-serif text-[28px] font-bold text-emerald-700">{{ stats.published }}</p>
            <p class="mt-1 text-[11px] font-semibold text-slate-400">Ready for learners</p>
          </div>

          <div class="rounded-xl bg-white p-5 shadow-sm shadow-slate-200/60 ring-1 ring-slate-900/[0.04]">
            <div class="flex items-start justify-between">
              <p class="text-[10px] font-bold uppercase tracking-[0.14em] text-slate-400">Pending Review</p>
              <span class="flex h-8 w-8 items-center justify-center rounded-lg bg-violet-50 text-violet-600">
                <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10" /><path d="M12 6v6l4 2" /></svg>
              </span>
            </div>
            <p class="mt-6 font-serif text-[28px] font-bold text-[#5b4cfa]">{{ stats.pending }}</p>
            <p class="mt-1 text-[11px] font-semibold text-slate-400">Awaiting teacher</p>
          </div>

          <div class="rounded-xl bg-white p-5 shadow-sm shadow-slate-200/60 ring-1 ring-slate-900/[0.04]">
            <div class="flex items-start justify-between">
              <p class="text-[10px] font-bold uppercase tracking-[0.14em] text-slate-400">Needs Revision</p>
              <span class="flex h-8 w-8 items-center justify-center rounded-lg bg-red-50 text-red-600">
                <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10" /><path d="M15 9 9 15M9 9l6 6" /></svg>
              </span>
            </div>
            <p class="mt-6 font-serif text-[28px] font-bold text-red-700">{{ stats.revision }}</p>
            <p class="mt-1 text-[11px] font-semibold text-slate-400">Teacher feedback</p>
          </div>
        </section>

        <div
          v-if="loadError"
          class="mb-4 flex items-center justify-between rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-[13px] font-medium text-red-700"
        >
          <span>{{ loadError }}</span>
          <button type="button" class="font-semibold text-red-800 hover:text-red-900" @click="loadCourses">Retry</button>
        </div>

        <div v-if="loadingCourses" class="space-y-4">
          <div v-for="index in 5" :key="index" class="h-[84px] animate-pulse rounded-xl bg-white ring-1 ring-slate-900/[0.04]">
          </div>
        </div>

        <div
          v-else-if="filteredCourses.length > 0"
          class="space-y-4"
        >
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
            @open="router.push(`/courses/${course.id}`)"
            @edit="openEdit(course)"
            @delete="deleteCourse(course.id)"
          />
        </div>

        <!-- Empty state -->
        <div
          v-else
          class="flex flex-col items-center justify-center rounded-2xl border border-dashed border-slate-200 bg-white py-24 text-center"
        >
          <div class="flex h-14 w-14 items-center justify-center rounded-2xl bg-[#5b4cfa]/8 text-[#5b4cfa]">
            <svg class="h-7 w-7" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" />
              <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" />
            </svg>
          </div>
          <p class="mt-4 text-[15px] font-semibold text-slate-700">
            {{ searchQuery ? 'No courses found' : 'No courses yet' }}
          </p>
          <p class="mt-1 text-[12px] text-slate-400">
            {{ searchQuery ? 'Try a different search term or filter.' : 'Create your first course to get started.' }}
          </p>
          <button
            v-if="!searchQuery"
            type="button"
            class="mt-5 inline-flex items-center gap-2 rounded-xl bg-[#5b4cfa] px-5 py-2.5 text-[13px] font-semibold text-white shadow-md shadow-[#5b4cfa]/25 transition hover:bg-[#4d3ee0]"
            @click="openAddModal"
          >
            New Course
          </button>
        </div>
      </main>
    </div>

    <div v-if="showStatusMenu" class="fixed inset-0 z-10" aria-hidden="true" @click="showStatusMenu = false" />

    <AddCourseModal
      :open="showAddModal"
      :submitting="creatingCourse"
      :error-message="createError"
      @close="closeAddModal"
      @create="createCourse"
    />
    <EditCourseModal :open="showEditModal" :course="editingCourse" @close="closeEdit" @save="onSave" />
  </div>
</template>
