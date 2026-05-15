<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import CourseManagementNavbar from '@/components/BackHourseNavbar.vue'
import TopicCard from '@/features/courses/components/TopicCard.vue'
import EditTopicModal from '@/features/courses/components/EditTopicPopUp.vue'
import type { EditableTopic } from '@/features/courses/components/EditTopicPopUp.vue'

type MockTopic = EditableTopic

type MockCourseDetail = {
  id: string
  title: string
  description: string
  topics: MockTopic[]
}

const MOCK_COURSES: MockCourseDetail[] = [
  {
    id: '1',
    title: 'Quadratic Functions',
    description:
      'Explore parabolas, vertex form, factoring, and real-world quadratic models. This course builds intuition before moving into deeper algebra.',
    topics: [
      {
        id: 't1',
        name: 'Introduction to Parabolas',
        content:
          'A quadratic function graphs as a parabola. The coefficient of x² determines whether the parabola opens upward or downward.\n\nKey vocabulary: vertex, axis of symmetry, roots, and maximum/minimum value.',
      },
      {
        id: 't2',
        name: 'Vertex Form & Transformations',
        content:
          'Vertex form: f(x) = a(x - h)² + k.\n\nThe point (h, k) is the vertex. Changing h shifts the graph horizontally; changing k shifts vertically. The value |a| controls how narrow or wide the parabola appears.\n\nExample: f(x) = 2(x - 3)² - 1 has vertex (3, -1) and opens upward because a > 0.\n\nPractice: rewrite y = x² - 6x + 11 in vertex form by completing the square. Step 1: group x terms. Step 2: add and subtract (b/2)². Step 3: simplify to identify h and k.\n\nApplications include projectile motion, area optimization, and revenue models where the relationship is naturally curved rather than linear.',
      },
    ],
  },
  {
    id: '2',
    title: 'Intro to Linear Algebra',
    description: 'Vectors, matrices, and systems of linear equations.',
    topics: [
      {
        id: 't3',
        name: 'Vectors in R²',
        content: 'Vectors can be represented as arrows with magnitude and direction, or as ordered pairs (x, y).',
      },
    ],
  },
]

const route = useRoute()
const router = useRouter()

const courseId = computed(() => String(route.params.id ?? ''))

const course = computed(() => MOCK_COURSES.find((c) => c.id === courseId.value) ?? null)

const topics = ref<MockTopic[]>([])

const showEditTopicModal = ref(false)
const editingTopic = ref<EditableTopic | null>(null)

function syncTopicsFromCourse() {
  topics.value = course.value ? [...course.value.topics] : []
}

watch(course, syncTopicsFromCourse, { immediate: true })

const showNotFound = computed(() => !course.value)

function openEditTopic(topic: MockTopic) {
  editingTopic.value = { ...topic }
  showEditTopicModal.value = true
}

function closeEditTopicModal() {
  showEditTopicModal.value = false
  editingTopic.value = null
}

function onSaveTopic(updated: EditableTopic) {
  const index = topics.value.findIndex((t) => t.id === updated.id)
  if (index === -1) return
  topics.value[index] = { ...updated }
}

function deleteTopic(topicId: string) {
  topics.value = topics.value.filter((t) => t.id !== topicId)
}

function goBack() {
  router.push('/')
}
</script>

<template>
  <div class="course-app flex min-h-screen w-full bg-[#f4f5f9] font-[family-name:var(--font-sans)]">
    <CourseManagementNavbar active-item="course" />

    <div class="flex min-w-0 flex-1 flex-col">
      <header class="border-b border-slate-200/60 bg-[#f4f5f9]/95 px-6 py-8 backdrop-blur sm:px-10">
        <button
          type="button"
          class="mb-6 inline-flex items-center gap-2 text-sm font-medium text-slate-500 transition hover:text-[#5b4cfa]"
          @click="goBack"
        >
          <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
            <polyline points="15 18 9 12 15 6" />
          </svg>
          Back to courses
        </button>

        <template v-if="course">
          <p class="text-xs font-semibold uppercase tracking-wider text-[#5b4cfa]">Course</p>
          <h1
            class="mt-2 font-[family-name:var(--font-serif)] text-3xl font-bold tracking-tight text-slate-900 sm:text-[2rem]"
          >
            {{ course.title }}
          </h1>
          <p class="mt-4 max-w-3xl text-sm leading-relaxed text-slate-600 sm:text-base">
            {{ course.description }}
          </p>
        </template>

        <template v-else>
          <h1
            class="font-[family-name:var(--font-serif)] text-3xl font-bold tracking-tight text-slate-900 sm:text-[2rem]"
          >
            Course not found
          </h1>
          <p class="mt-2 text-sm text-slate-500">This course does not exist or has been removed.</p>
        </template>
      </header>

      <main class="flex-1 px-6 py-8 sm:px-10">
        <div v-if="showNotFound" class="rounded-2xl border border-dashed border-slate-200 bg-white px-6 py-12 text-center">
          <p class="text-sm text-slate-500">Try opening a course from the management page.</p>
          <button
            type="button"
            class="mt-4 rounded-full bg-[#5b4cfa] px-5 py-2.5 text-sm font-semibold text-white shadow-md shadow-[#5b4cfa]/25 transition hover:bg-[#4d3ee0]"
            @click="goBack"
          >
            Go to Course Management
          </button>
        </div>

        <section v-else class="space-y-4">
          <div class="flex items-center justify-between gap-4">
            <h2 class="text-sm font-semibold uppercase tracking-wider text-slate-500">Topics</h2>
            <span class="text-xs text-slate-400">{{ topics.length }} topic{{ topics.length === 1 ? '' : 's' }}</span>
          </div>

          <TopicCard
            v-for="topic in topics"
            :key="topic.id"
            :name="topic.name"
            :content="topic.content"
            @delete="deleteTopic(topic.id)"
            @edit="openEditTopic(topic)"
          />

          <p
            v-if="topics.length === 0"
            class="rounded-2xl border border-dashed border-slate-200 bg-white px-6 py-10 text-center text-sm text-slate-500"
          >
            No topics yet. Backend integration will let you add topics here.
          </p>
        </section>
      </main>
    </div>

    <EditTopicModal
      :open="showEditTopicModal"
      :topic="editingTopic"
      @close="closeEditTopicModal"
      @save="onSaveTopic"
    />
  </div>
</template>
