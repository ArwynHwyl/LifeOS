<script setup lang="ts">
import CourseManagementNavbar from '@/components/BackHourseNavbar.vue'
import CourseCard from '@/components/CourseCard.vue'
import type { CourseStatus } from '@/types/types'

type MockCourse = {
  id: string
  title: string
  status: CourseStatus
  moduleCount: number
  lastEdited: string
  createdBy: string
}

const mockCourses: MockCourse[] = [
  {
    id: '1',
    title: 'Quadratic Functions',
    status: 'published',
    moduleCount: 4,
    lastEdited: '2 days ago',
    createdBy: 'Admin',
  },
  {
    id: '2',
    title: 'Intro to Linear Algebra',
    status: 'published',
    moduleCount: 6,
    lastEdited: '1 week ago',
    createdBy: 'Admin',
  },
  {
    id: '3',
    title: 'Probability Basics',
    status: 'draft',
    moduleCount: 3,
    lastEdited: 'today',
    createdBy: 'Admin',
  },
  {
    id: '4',
    title: 'Calculus I: Limits',
    status: 'published',
    moduleCount: 8,
    lastEdited: '3 days ago',
    createdBy: 'Admin',
  },
]
</script>

<template>
  <div class="course-app flex min-h-screen w-full bg-[#f4f5f9] font-[family-name:var(--font-sans)]">
    <CourseManagementNavbar active-item="course" />

    <div class="flex min-w-0 flex-1 flex-col">
      <header class="border-b border-slate-200/60 bg-[#f4f5f9]/95 px-6 py-8 backdrop-blur sm:px-10">
        <div class="flex flex-col gap-8 lg:flex-row lg:items-start lg:justify-between">
          <div>
            <h1
              class="font-[family-name:var(--font-serif)] text-3xl font-bold tracking-tight text-slate-900 sm:text-[2rem]"
            >
              Course Management
            </h1>
            <p class="mt-2 max-w-xl text-sm text-slate-500">
              Manage, publish and track all content.
            </p>
          </div>

          <div class="flex w-full flex-col gap-3 sm:flex-row sm:flex-wrap sm:items-center lg:w-auto lg:justify-end">
            <div class="relative min-w-[200px] flex-1 sm:max-w-md lg:min-w-[280px] lg:flex-initial lg:w-[min(100%,380px)]">
              <svg
                class="pointer-events-none absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                aria-hidden="true"
              >
                <circle cx="11" cy="11" r="8" />
                <path d="m21 21-4.3-4.3" />
              </svg>
              <input
                type="search"
                placeholder="Search Courses..."
                class="w-full rounded-full border border-slate-200/80 bg-white py-3 pl-11 pr-4 text-sm text-slate-800 shadow-sm outline-none ring-[#5b4cfa]/20 placeholder:text-slate-400 focus:border-[#5b4cfa]/40 focus:ring-2"
              />
            </div>

            <button
              type="button"
              class="inline-flex items-center justify-center gap-2 rounded-full border border-slate-200 bg-white px-5 py-3 text-sm font-medium text-slate-700 shadow-sm transition hover:bg-slate-50"
            >
              All status
              <svg class="h-4 w-4 text-slate-500" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="6 9 12 15 18 9" />
              </svg>
            </button>

            <button
              type="button"
              class="inline-flex items-center justify-center gap-2 rounded-full bg-[#5b4cfa] px-6 py-3 text-sm font-semibold text-white shadow-md shadow-[#5b4cfa]/25 transition hover:bg-[#4d3ee0]"
            >
              <span class="text-lg leading-none">+</span>
              New Course
            </button>
          </div>
        </div>
      </header>

      <main class="flex-1 space-y-4 px-6 py-8 sm:px-10">
        <CourseCard
          v-for="course in mockCourses"
          :key="course.id"
          :title="course.title"
          :status="course.status"
          :module-count="course.moduleCount"
          :last-edited="course.lastEdited"
          :created-by="course.createdBy"
        />
      </main>
    </div>
  </div>
</template>
