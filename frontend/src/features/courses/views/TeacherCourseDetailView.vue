<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import TeacherNavbar from '@/features/courses/components/Teacher/TeacherNavbar.vue'
import TeacherTopicCard from '@/features/courses/components/Teacher/TeacherTopicCard.vue'
import type { Comment } from '@/features/courses/components/Teacher/TeacherTopicCard.vue'
import { getCoverPreset } from '@/features/courses/constants/courseCoverPresets'
import type { CourseStatus } from '@/types/types'

const CURRENT_TEACHER = { id: 'teacher-1', name: 'Ms. Johnson' }

type Topic = { id: string; name: string; content: string }

type MockCourse = {
  id: string
  title: string
  description: string
  coverId: string
  status: CourseStatus
  topics: Topic[]
}

const MOCK_COURSES: MockCourse[] = [
  {
    id: '1',
    title: 'Quadratic Functions',
    description: 'Explore parabolas, vertex form, factoring, and real-world quadratic models.',
    coverId: 'integral',
    status: 'published',
    topics: [
      {
        id: 't1',
        name: 'Introduction to Parabolas',
        content: 'A quadratic function graphs as a parabola. The coefficient of x² determines whether it opens upward or downward.\n\nKey vocabulary: vertex, axis of symmetry, roots, maximum/minimum value.',
      },
      {
        id: 't2',
        name: 'Vertex Form & Transformations',
        content: 'Vertex form: f(x) = a(x - h)² + k.\n\nThe point (h, k) is the vertex. Changing h shifts horizontally; changing k shifts vertically.',
      },
    ],
  },
  {
    id: '2',
    title: 'Intro to Linear Algebra',
    description: 'Vectors, matrices, and systems of linear equations.',
    coverId: 'sigma',
    status: 'published',
    topics: [
      { id: 't3', name: 'Vectors in R²', content: 'Vectors can be represented as arrows with magnitude and direction, or as ordered pairs (x, y).' },
    ],
  },
  {
    id: '3',
    title: 'Probability Basics',
    description: 'Foundations of probability, events, and distributions.',
    coverId: 'pi',
    status: 'draft',
    topics: [],
  },
  {
    id: '4',
    title: 'Calculus I: Limits',
    description: 'Limits, continuity, and introductory differential calculus.',
    coverId: 'fx',
    status: 'published',
    topics: [],
  },
]

const route = useRoute()
const router = useRouter()

const courseId = computed(() => String(route.params.id ?? ''))
const course = computed(() => MOCK_COURSES.find((c) => c.id === courseId.value) ?? null)
const cover = computed(() => course.value ? getCoverPreset(course.value.coverId) : null)
const topics = ref<Topic[]>([])

const commentsMap = ref<Record<string, Comment[]>>({
  t1: [
    {
      id: 'c1',
      authorId: 'teacher-1',
      authorName: 'Ms. Johnson',
      text: 'Great introduction! Students might benefit from a visual diagram of the parabola here.',
      createdAt: '2 hours ago',
    },
    {
      id: 'c2',
      authorId: 'teacher-2',
      authorName: 'Mr. Reyes',
      text: 'I agree. Also worth connecting this to projectile motion early on.',
      createdAt: '1 day ago',
    },
  ],
  t2: [],
  t3: [],
})

watch(course, (c) => { topics.value = c ? [...c.topics] : [] }, { immediate: true })

function getComments(topicId: string): Comment[] {
  return commentsMap.value[topicId] ?? []
}

function addComment(topicId: string, text: string) {
  if (!commentsMap.value[topicId]) commentsMap.value[topicId] = []
  commentsMap.value[topicId].push({
    id: `c-${Date.now()}`,
    authorId: CURRENT_TEACHER.id,
    authorName: CURRENT_TEACHER.name,
    text,
    createdAt: 'just now',
  })
}

function editComment(topicId: string, payload: { id: string; text: string }) {
  const list = commentsMap.value[topicId]
  if (!list) return
  const i = list.findIndex((c) => c.id === payload.id)
  if (i !== -1) list[i] = { ...list[i], text: payload.text }
}

function deleteComment(topicId: string, commentId: string) {
  if (!commentsMap.value[topicId]) return
  commentsMap.value[topicId] = commentsMap.value[topicId].filter((c) => c.id !== commentId)
}

function endDiscussion(topicId: string) {
  commentsMap.value[topicId] = []
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
            {{ course?.title ?? 'Not found' }}
          </span>
        </div>

        <!-- Approve / Reject -->
        <div v-if="course" class="flex items-center gap-2">
          <button
            type="button"
            class="inline-flex items-center gap-1.5 rounded-lg border-2 border-lm-green bg-lm-green-soft px-3 py-1.5 text-[12px] font-semibold text-lm-green shadow-stamp-sm transition-all duration-200 hover:-translate-y-px hover:shadow-stamp-md"
          >
            <svg class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
              <polyline points="20 6 9 17 4 12" />
            </svg>
            Approve
          </button>
          <button
            type="button"
            class="inline-flex items-center gap-1.5 rounded-lg border-2 border-lm-red bg-lm-red-soft px-3 py-1.5 text-[12px] font-semibold text-lm-red shadow-stamp-sm transition-all duration-200 hover:-translate-y-px hover:shadow-stamp-md"
          >
            <svg class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
              <path d="M18 6 6 18M6 6l12 12" />
            </svg>
            Reject
          </button>
        </div>
      </div>

      <!-- Scrollable content -->
      <main class="flex-1 overflow-y-auto px-7 py-6 relative">
        <div class="absolute inset-0 bg-dot-grid opacity-40 pointer-events-none" />

        <div class="relative">
          <!-- Course not found -->
          <div
            v-if="!course"
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

          <template v-else-if="cover">
            <!-- Course summary card -->
            <div class="mb-6 rounded-[18px] bg-lm-surface border-2 border-lm-line px-6 py-5 shadow-stamp-sm">
              <div class="flex items-center gap-5">
                <div
                  class="flex h-[72px] w-[72px] shrink-0 items-center justify-center rounded-[18px] border-2 border-lm-line font-display text-[32px] font-bold shadow-stamp-md"
                  :class="[cover.bgClass, cover.textClass]"
                >
                  {{ cover.symbol }}
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
                    {{ topics.length }} topic{{ topics.length === 1 ? '' : 's' }}
                  </span>
                  <span
                    v-if="course.status === 'published'"
                    class="inline-flex items-center gap-1 rounded-full border-2 border-lm-line bg-lm-green-soft px-2 py-0.5 font-mono text-[10px] font-bold text-lm-green shadow-stamp-sm"
                  >
                    <span class="h-1.5 w-1.5 rounded-full bg-lm-green" />
                    Published
                  </span>
                  <span
                    v-else
                    class="inline-flex items-center gap-1 rounded-full border-2 border-lm-line bg-lm-yellow px-2 py-0.5 font-mono text-[10px] font-bold text-lm-ink shadow-stamp-sm"
                  >
                    <span class="h-1.5 w-1.5 rounded-full bg-lm-ink" />
                    Pending Review
                  </span>
                </div>
              </div>
            </div>

            <!-- Topics section label -->
            <div class="mb-3 flex items-center justify-between px-0.5">
              <p class="font-mono text-[10px] font-bold uppercase tracking-[0.13em] text-lm-ink-3">Topics</p>
              <span class="font-mono text-[11px] text-lm-ink-3">{{ topics.length }}</span>
            </div>

            <!-- Topics list -->
            <div v-if="topics.length > 0" class="flex flex-col gap-3">
              <TeacherTopicCard
                v-for="(topic, index) in topics"
                :key="topic.id"
                :index="index + 1"
                :name="topic.name"
                :content="topic.content"
                :comments="getComments(topic.id)"
                :current-user-id="CURRENT_TEACHER.id"
                @add-comment="addComment(topic.id, $event)"
                @edit-comment="editComment(topic.id, $event)"
                @delete-comment="deleteComment(topic.id, $event)"
                @end-discussion="endDiscussion(topic.id)"
              />
            </div>

            <!-- Empty topics -->
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
              <p class="mt-4 font-display text-[15px] font-semibold text-lm-ink">No topics yet</p>
              <p class="mt-1 text-[12px] text-lm-ink-3">This course has no topics to review.</p>
            </div>
          </template>
        </div>
      </main>
    </div>
  </div>
</template>
