<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppSidebar from '@/features/courses/components/Admin/AdminNavbar.vue'
import TopicCard from '@/features/courses/components/Admin/AdminTopicCard.vue'
import type { Comment } from '@/features/courses/components/Admin/AdminTopicCard.vue'
import TopicEditor from '@/features/courses/components/Admin/TopicEditor.vue'
import AddTopicModal from '@/features/courses/components/Admin/AddTopicPopUp.vue'
import { getCoverPreset } from '@/features/courses/constants/courseCoverPresets'

const CURRENT_ADMIN = { id: 'admin-1', name: 'Admin' }

type Topic = {
  id: string
  name: string
  content: string
}

type MockCourseDetail = {
  id: string
  title: string
  description: string
  coverId: string
  topics: Topic[]
}

const MOCK_COURSES: MockCourseDetail[] = [
  {
    id: '1',
    title: 'Quadratic Functions',
    description: 'Explore parabolas, vertex form, factoring, and real-world quadratic models. This course builds intuition before moving into deeper algebra.',
    coverId: 'integral',
    topics: [
      {
        id: 't1',
        name: 'Introduction to Parabolas',
        content: 'A quadratic function graphs as a parabola. The coefficient of x² determines whether it opens upward or downward.\n\nKey vocabulary: vertex, axis of symmetry, roots, maximum/minimum value.',
      },
      {
        id: 't2',
        name: 'Vertex Form & Transformations',
        content: 'Vertex form: f(x) = a(x - h)² + k.\n\nThe point (h, k) is the vertex. Changing h shifts horizontally; changing k shifts vertically. The value |a| controls how narrow or wide the parabola appears.\n\nExample: f(x) = 2(x - 3)² - 1 has vertex (3, -1) and opens upward because a > 0.',
      },
    ],
  },
  {
    id: '2',
    title: 'Intro to Linear Algebra',
    description: 'Vectors, matrices, and systems of linear equations.',
    coverId: 'sigma',
    topics: [
      { id: 't3', name: 'Vectors in R²', content: 'Vectors can be represented as arrows with magnitude and direction, or as ordered pairs (x, y).' },
    ],
  },
  {
    id: '3',
    title: 'Probability Basics',
    description: 'Foundations of probability, events, and distributions.',
    coverId: 'pi',
    topics: [],
  },
  {
    id: '4',
    title: 'Calculus I: Limits',
    description: 'Limits, continuity, and introductory differential calculus.',
    coverId: 'fx',
    topics: [],
  },
]

const route = useRoute()
const router = useRouter()

const courseId = computed(() => String(route.params.id ?? ''))
const course = computed(() => MOCK_COURSES.find((c) => c.id === courseId.value) ?? null)
const cover = computed(() => course.value ? getCoverPreset(course.value.coverId) : null)
const topics = ref<Topic[]>([])

const editingTopicId = ref<string | null>(null)
const showAddTopicModal = ref(false)

const commentsMap = ref<Record<string, Comment[]>>({
  t1: [
    {
      id: 'c1',
      authorId: 'admin-1',
      authorName: 'Admin',
      text: 'Solid introduction. Consider adding a note about the discriminant early on.',
      createdAt: '1 hour ago',
    },
  ],
  t2: [],
})

function getComments(topicId: string): Comment[] {
  return commentsMap.value[topicId] ?? []
}

function addComment(topicId: string, text: string) {
  if (!commentsMap.value[topicId]) commentsMap.value[topicId] = []
  commentsMap.value[topicId].push({
    id: `c-${Date.now()}`,
    authorId: CURRENT_ADMIN.id,
    authorName: CURRENT_ADMIN.name,
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

const editingTopic = computed(() =>
  editingTopicId.value ? topics.value.find((t) => t.id === editingTopicId.value) ?? null : null
)

watch(course, (c) => { topics.value = c ? [...c.topics] : [] }, { immediate: true })

function openEditor(topic: Topic) {
  editingTopicId.value = topic.id
}

function onEditorSave(updated: { name: string; content: string }) {
  const i = topics.value.findIndex((t) => t.id === editingTopicId.value)
  if (i !== -1) topics.value[i] = { ...topics.value[i], ...updated }
  editingTopicId.value = null
}

function onAddTopic(t: { name: string; content: string }) {
  topics.value.push({ id: `t-${Date.now()}`, name: t.name, content: t.content })
}

function deleteTopic(id: string) {
  topics.value = topics.value.filter((t) => t.id !== id)
}
</script>

<template>
  <div class="course-app flex h-screen w-full overflow-hidden bg-[#f4f5f9]">
    <AppSidebar active-item="course" />

    <!-- Normal course detail view -->
    <div v-if="editingTopicId === null" class="flex min-w-0 flex-1 flex-col overflow-hidden">

      <!-- Top navigation bar -->
      <div class="flex shrink-0 items-center justify-between border-b border-slate-200/70 bg-white px-7 py-3">
        <div class="flex items-center gap-1.5 text-[12px]">
          <button
            type="button"
            class="font-medium text-slate-400 transition hover:text-[#5b4cfa]"
            @click="router.push('/')"
          >
            Courses
          </button>
          <svg class="h-3.5 w-3.5 text-slate-300" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="9 18 15 12 9 6" />
          </svg>
          <span class="truncate max-w-[260px] font-semibold text-slate-700">
            {{ course?.title ?? 'Not found' }}
          </span>
        </div>
        <button
          v-if="course"
          type="button"
          class="inline-flex items-center gap-1.5 rounded-lg bg-[#5b4cfa] px-3.5 py-1.5 text-[12px] font-semibold text-white shadow-sm shadow-[#5b4cfa]/30 transition hover:bg-[#4d3ee0] active:scale-[0.98]"
          @click="showAddTopicModal = true"
        >
          <svg class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" />
          </svg>
          Add Topic
        </button>
      </div>

      <!-- Scrollable content -->
      <main class="flex-1 overflow-y-auto px-7 py-6">

        <!-- Course not found -->
        <div
          v-if="!course"
          class="flex flex-col items-center justify-center rounded-2xl border border-dashed border-slate-200 bg-white py-20 text-center"
        >
          <p class="text-[13px] text-slate-400">This course does not exist or has been removed.</p>
          <button
            type="button"
            class="mt-4 rounded-xl bg-[#5b4cfa] px-5 py-2.5 text-[13px] font-semibold text-white transition hover:bg-[#4d3ee0]"
            @click="router.push('/')"
          >
            Go to Courses
          </button>
        </div>

        <template v-else-if="cover">
          <!-- Course summary card -->
          <div class="mb-6 rounded-2xl bg-white px-6 py-5 shadow-sm ring-1 ring-slate-900/[0.05]">
            <div class="flex items-center gap-5">
              <div
                class="flex h-[72px] w-[72px] shrink-0 items-center justify-center rounded-2xl font-serif text-[32px] font-bold shadow-sm"
                :class="[cover.bgClass, cover.textClass]"
              >
                {{ cover.symbol }}
              </div>
              <h1 class="text-[17px] font-bold tracking-tight text-slate-900">{{ course.title }}</h1>
            </div>
            <div class="mt-2 pl-[92px]">
              <p class="text-[12px] leading-relaxed text-slate-500 line-clamp-2">{{ course.description }}</p>
              <span class="mt-2 inline-flex items-center gap-1 text-[11px] text-slate-400">
                <svg class="h-3 w-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                  <polyline points="14 2 14 8 20 8" />
                </svg>
                {{ topics.length }} topic{{ topics.length === 1 ? '' : 's' }}
              </span>
            </div>
          </div>

          <!-- Topics section label -->
          <div class="mb-3 flex items-center justify-between px-0.5">
            <p class="text-[10px] font-bold uppercase tracking-[0.13em] text-slate-400">Topics</p>
            <span class="text-[11px] text-slate-400">{{ topics.length }}</span>
          </div>

          <!-- Topics list -->
          <div v-if="topics.length > 0" class="flex flex-col gap-2">
            <TopicCard
              v-for="(topic, index) in topics"
              :key="topic.id"
              :index="index + 1"
              :name="topic.name"
              :content="topic.content"
              :comments="getComments(topic.id)"
              :current-user-id="CURRENT_ADMIN.id"
              @edit="openEditor(topic)"
              @delete="deleteTopic(topic.id)"
              @add-comment="addComment(topic.id, $event)"
              @edit-comment="editComment(topic.id, $event)"
              @delete-comment="deleteComment(topic.id, $event)"
              @end-discussion="endDiscussion(topic.id)"
            />
          </div>

          <!-- Empty topics state -->
          <div
            v-else
            class="flex flex-col items-center justify-center rounded-2xl border border-dashed border-slate-200 bg-white py-20 text-center"
          >
            <div class="flex h-14 w-14 items-center justify-center rounded-2xl bg-[#5b4cfa]/8 text-[#5b4cfa]">
              <svg class="h-7 w-7" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                <polyline points="14 2 14 8 20 8" />
              </svg>
            </div>
            <p class="mt-4 text-[15px] font-semibold text-slate-700">No topics yet</p>
            <p class="mt-1 text-[12px] text-slate-400">Add your first topic to this course.</p>
            <button
              type="button"
              class="mt-5 inline-flex items-center gap-2 rounded-xl bg-[#5b4cfa] px-5 py-2.5 text-[13px] font-semibold text-white shadow-md shadow-[#5b4cfa]/25 transition hover:bg-[#4d3ee0]"
              @click="showAddTopicModal = true"
            >
              Add Topic
            </button>
          </div>
        </template>
      </main>
    </div>

    <!-- Full-page topic editor -->
    <TopicEditor
      v-else-if="editingTopic"
      :key="editingTopicId!"
      :name="editingTopic.name"
      :content="editingTopic.content"
      :course-title="course?.title ?? ''"
      @save="onEditorSave"
      @cancel="editingTopicId = null"
    />

    <AddTopicModal :open="showAddTopicModal" @close="showAddTopicModal = false" @save="onAddTopic" />
  </div>
</template>
