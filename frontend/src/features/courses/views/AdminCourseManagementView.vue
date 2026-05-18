<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppSidebar from '@/features/courses/components/Admin/AdminNavbar.vue'
import CourseCard from '@/features/courses/components/Admin/AdminCourseCard.vue'
import AddCourseModal from '@/features/courses/components/Admin/AddCoursePopUp.vue'
import EditCourseModal from '@/features/courses/components/Admin/EditCoursePopUp.vue'
import type { EditableCourse } from '@/features/courses/components/Admin/EditCoursePopUp.vue'
import { DEFAULT_COVER_ID } from '@/features/courses/constants/courseCoverPresets'
import type { CourseStatus } from '@/types/types'

const router = useRouter()

const showAddModal = ref(false)
const showEditModal = ref(false)
const editingCourse = ref<EditableCourse | null>(null)
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
  createdBy: string
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
    createdBy: 'Admin',
  },
  {
    id: '2',
    title: 'Intro to Linear Algebra',
    description: 'Vectors, matrices, and systems of linear equations.',
    coverId: 'sigma',
    status: 'published',
    moduleCount: 6,
    lastEdited: '1 week ago',
    createdBy: 'Admin',
  },
  {
    id: '3',
    title: 'Probability Basics',
    description: 'Foundations of probability, events, and distributions.',
    coverId: 'pi',
    status: 'draft',
    moduleCount: 3,
    lastEdited: 'today',
    createdBy: 'Admin',
  },
  {
    id: '4',
    title: 'Calculus I: Limits',
    description: 'Limits, continuity, and introductory differential calculus.',
    coverId: 'fx',
    status: 'published',
    moduleCount: 8,
    lastEdited: '3 days ago',
    createdBy: 'Admin',
  },
])

const stats = computed(() => ({
  total: courses.value.length,
  published: courses.value.filter((c) => c.status === 'published').length,
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
  if (statusFilter.value === 'draft') return 'Draft'
  return 'All'
})

function openEdit(course: MockCourse) {
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
</script>

<template>
  <div class="course-app flex h-screen w-full overflow-hidden bg-[#f4f5f9]">
    <AppSidebar active-item="course" />

    <div class="flex min-w-0 flex-1 flex-col overflow-hidden">
      <!-- Toolbar header -->
      <div class="flex shrink-0 items-center justify-between border-b border-slate-200/70 bg-white px-7 py-3">
        <!-- Stats -->
        <div class="flex items-center gap-2">
          <span class="inline-flex items-center gap-1.5 rounded-full border border-slate-200 px-2.5 py-1 text-[11px] font-medium text-slate-500">
            <span class="font-bold text-slate-700">{{ stats.total }}</span> total
          </span>
          <span class="inline-flex items-center gap-1.5 rounded-full border border-emerald-200/70 bg-emerald-50 px-2.5 py-1 text-[11px] font-semibold text-emerald-700">
            <span class="h-1.5 w-1.5 rounded-full bg-emerald-500" />
            {{ stats.published }} published
          </span>
          <span class="inline-flex items-center gap-1.5 rounded-full border border-amber-200/70 bg-amber-50 px-2.5 py-1 text-[11px] font-semibold text-amber-700">
            <span class="h-1.5 w-1.5 rounded-full bg-amber-400" />
            {{ stats.draft }} draft
          </span>
        </div>

        <!-- Controls -->
        <div class="flex items-center gap-2">
          <!-- Search -->
          <div class="relative">
            <svg class="pointer-events-none absolute left-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-slate-400"
              viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8" /><path d="m21 21-4.3-4.3" />
            </svg>
            <input
              v-model="searchQuery"
              type="search"
              placeholder="Search…"
              class="w-48 rounded-lg border border-slate-200 bg-slate-50 py-1.5 pl-8 pr-3 text-[12px] text-slate-700 outline-none transition placeholder:text-slate-400 focus:border-[#5b4cfa]/40 focus:bg-white focus:ring-2 focus:ring-[#5b4cfa]/10"
            />
          </div>

          <!-- Status filter -->
          <div class="relative">
            <button
              type="button"
              class="inline-flex items-center gap-1.5 rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-[12px] font-medium text-slate-600 transition hover:bg-slate-50"
              @click="showStatusMenu = !showStatusMenu"
            >
              <span v-if="statusFilter !== 'all'" class="h-1.5 w-1.5 rounded-full"
                :class="statusFilter === 'published' ? 'bg-emerald-500' : 'bg-amber-400'" />
              {{ statusLabel }}
              <svg class="h-3 w-3 text-slate-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="6 9 12 15 18 9" />
              </svg>
            </button>
            <div v-if="showStatusMenu"
              class="absolute right-0 top-full z-20 mt-1.5 w-36 overflow-hidden rounded-xl border border-slate-200 bg-white shadow-lg shadow-slate-900/10">
              <button v-for="[val, label] in [['all','All'],['published','Published'],['draft','Draft']]" :key="val"
                type="button"
                class="flex w-full items-center gap-2.5 px-3.5 py-2.5 text-[12px] transition hover:bg-slate-50"
                :class="statusFilter === val ? 'font-semibold text-[#5b4cfa]' : 'text-slate-600'"
                @click="setStatus(val as 'all' | 'published' | 'draft')">
                <span class="h-1.5 w-1.5 rounded-full"
                  :class="val === 'published' ? 'bg-emerald-500' : val === 'draft' ? 'bg-amber-400' : 'bg-slate-300'" />
                {{ label }}
              </button>
            </div>
          </div>

          <!-- New course -->
          <button
            type="button"
            class="inline-flex items-center gap-1.5 rounded-lg bg-[#5b4cfa] px-3.5 py-1.5 text-[12px] font-semibold text-white shadow-sm shadow-[#5b4cfa]/30 transition hover:bg-[#4d3ee0] active:scale-[0.98]"
            @click="showAddModal = true"
          >
            <svg class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
              <line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" />
            </svg>
            New Course
          </button>
        </div>
      </div>

      <!-- Scrollable grid -->
      <main class="flex-1 overflow-y-auto px-7 py-6">
        <!-- Course grid -->
        <div
          v-if="filteredCourses.length > 0"
          class="grid gap-4 sm:grid-cols-3 xl:grid-cols-4"
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
            @click="showAddModal = true"
          >
            New Course
          </button>
        </div>
      </main>
    </div>

    <div v-if="showStatusMenu" class="fixed inset-0 z-10" aria-hidden="true" @click="showStatusMenu = false" />

    <AddCourseModal :open="showAddModal" @close="showAddModal = false" />
    <EditCourseModal :open="showEditModal" :course="editingCourse" @close="closeEdit" @save="onSave" />
  </div>
</template>
