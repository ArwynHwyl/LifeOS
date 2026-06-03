<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import InteractiveChallengeShell from '@/features/courses/components/interactive/InteractiveChallengeShell.vue'
import InteractivePreview from '@/features/courses/components/interactive/InteractivePreview.vue'
import { parseInteractiveConfig } from '@/features/courses/types/interactive'
import {
  getPublishedCourse,
  updateInteractiveProgress,
  type InteractiveProgressDto,
  type InteractiveProgressStatus,
  type PublishedCourseDetailDto,
  type PublishedSubTopicDto,
} from '@/features/learning/services/learnerCourses'
import LmIcon from '../components/LmIcon.vue'

const route = useRoute()
const router = useRouter()

const course = ref<PublishedCourseDetailDto | null>(null)
const selectedSubTopicId = ref<number | null>(null)
const loading = ref(true)
const error = ref('')

const allSubTopics = computed(() => course.value?.modules.flatMap((module) => module.subTopics) ?? [])
const selectedSubTopic = computed(() => {
  return allSubTopics.value.find((subTopic) => subTopic.id === selectedSubTopicId.value) ?? allSubTopics.value[0] ?? null
})
const selectedModule = computed(() => {
  const current = selectedSubTopic.value
  return course.value?.modules.find((module) => module.subTopics.some((subTopic) => subTopic.id === current?.id)) ?? null
})
const selectedIndex = computed(() => {
  const current = selectedSubTopic.value
  if (!current) return 0
  return Math.max(0, allSubTopics.value.findIndex((subTopic) => subTopic.id === current.id))
})
const progressPercent = computed(() => {
  if (!allSubTopics.value.length) return 0
  return ((selectedIndex.value + 1) / allSubTopics.value.length) * 100
})
const interactiveConfig = computed(() => {
  const current = selectedSubTopic.value
  if (!current) return null
  return parseInteractiveConfig(current.interactionType, current.interactionConfig)
})
const currentInteractiveMode = computed(() => {
  if (selectedSubTopic.value?.interactionType === 'QUIZ') return 'PRACTICE'
  return interactiveConfig.value?.mode ?? 'VISUALIZATION'
})
const selectedLessonHtml = computed(() => selectedSubTopic.value?.contentHtml || selectedSubTopic.value?.content || '')
const currentProgressStatus = computed<InteractiveProgressStatus>(() => selectedSubTopic.value?.interactiveProgress?.status ?? 'NOT_STARTED')
const currentChallengeObjective = computed(() => {
  const current = selectedSubTopic.value
  const config = interactiveConfig.value
  if (!current) return 'Complete this activity to master the concept.'
  if (current.interactionPrompt?.trim()) return current.interactionPrompt
  if (config && 'prompt' in config && config.prompt?.trim()) return config.prompt
  if (current.interactionType === 'QUIZ') return 'Answer the quiz to check your understanding.'
  if (current.interactionType === 'GRAPH_2D') return 'Use the graph to match the target behavior.'
  if (current.interactionType === 'FORMULA_EXPLORER') return 'Adjust the formula inputs to reach the target.'
  if (current.interactionType === 'VISUAL_LAYER') return 'Build or inspect the set diagram to master the concept.'
  return 'Complete this activity to master the concept.'
})
const currentInteractiveMastered = computed(() => selectedSubTopic.value?.interactionType !== 'NONE' && currentProgressStatus.value === 'MASTERED')

onMounted(async () => {
  loading.value = true
  error.value = ''
  try {
    course.value = await getPublishedCourse(String(route.params.courseId))
    selectedSubTopicId.value = allSubTopics.value[0]?.id ?? null
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Unable to load lesson.'
  } finally {
    loading.value = false
  }
})

function selectSubTopic(subTopic: PublishedSubTopicDto) {
  selectedSubTopicId.value = subTopic.id
}

function goToOffset(offset: number) {
  const next = allSubTopics.value[selectedIndex.value + offset]
  if (next) selectedSubTopicId.value = next.id
}

function progressMarkerClass(subTopic: PublishedSubTopicDto) {
  if (subTopic.interactionType === 'NONE') return ''
  const status = subTopic.interactiveProgress?.status ?? 'NOT_STARTED'
  if (status === 'MASTERED') return 'progress-marker progress-marker--mastered'
  if (status === 'TRIED') return 'progress-marker progress-marker--tried'
  return 'progress-marker progress-marker--not-started'
}

function setSubTopicProgress(subTopicId: number, progress: InteractiveProgressDto) {
  if (!course.value) return
  course.value = {
    ...course.value,
    modules: course.value.modules.map((module) => ({
      ...module,
      subTopics: module.subTopics.map((subTopic) => (
        subTopic.id === subTopicId ? { ...subTopic, interactiveProgress: progress } : subTopic
      )),
    })),
  }
}

function optimisticProgress(status: Exclude<InteractiveProgressStatus, 'NOT_STARTED'>) {
  const current = selectedSubTopic.value
  if (!current) return null
  const existing = current.interactiveProgress
  if (existing?.status === 'MASTERED' && status === 'TRIED') return existing
  const next: InteractiveProgressDto = {
    subTopicId: current.id,
    status,
    attemptCount: (existing?.attemptCount ?? 0) + 1,
    masteredAt: status === 'MASTERED' ? (existing?.masteredAt ?? new Date().toISOString()) : (existing?.masteredAt ?? null),
    updatedAt: new Date().toISOString(),
  }
  setSubTopicProgress(current.id, next)
  return next
}

async function persistInteractiveProgress(status: Exclude<InteractiveProgressStatus, 'NOT_STARTED'>) {
  const current = selectedSubTopic.value
  if (!current || current.interactionType === 'NONE') return
  if (current.interactiveProgress?.status === 'MASTERED' && status === 'TRIED') return
  optimisticProgress(status)
  try {
    const saved = await updateInteractiveProgress(String(route.params.courseId), current.id, status)
    setSubTopicProgress(current.id, saved)
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Unable to save challenge progress.'
  }
}

function handleInteractiveStarted() {
  if (currentInteractiveMode.value !== 'PRACTICE') {
    if (currentProgressStatus.value !== 'MASTERED') {
      void persistInteractiveProgress('MASTERED')
    }
    return
  }
  if (currentProgressStatus.value === 'NOT_STARTED') {
    void persistInteractiveProgress('TRIED')
  }
}

function handleInteractiveChecked(payload: { passed: boolean }) {
  void persistInteractiveProgress(payload.passed ? 'MASTERED' : 'TRIED')
}
</script>

<template>
  <main class="flex-1 flex flex-col overflow-hidden bg-lm-bg">
    <div class="flex items-center gap-4 px-6 py-3 bg-lm-surface border-b-2 border-lm-line shrink-0">
      <button
        class="flex items-center gap-1.5 px-3 py-1.5 text-sm font-semibold border-2 border-lm-line rounded-full bg-lm-surface shadow-stamp-sm hover:-translate-y-px transition-all duration-200 text-lm-ink shrink-0"
        @click="router.back()"
      >
        <LmIcon name="close" :size="14" />
        Exit
      </button>
      <div class="flex-1 h-4 bg-lm-bg-soft rounded-full overflow-hidden border border-lm-line">
        <div class="h-full bg-lm-yellow transition-all duration-200" :style="{ width: `${progressPercent}%` }" />
      </div>
      <span class="flex items-center gap-1.5 px-3 py-1 text-sm font-semibold border border-lm-line rounded-full bg-lm-yellow-soft shrink-0">
        {{ selectedIndex + 1 }} / {{ Math.max(allSubTopics.length, 1) }}
      </span>
    </div>

    <div class="flex items-center gap-2.5 px-6 py-2 bg-lm-yellow-soft border-b border-lm-line-soft text-[13px] shrink-0">
      <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3">{{ course?.title ?? 'LESSON' }}</span>
      <span class="text-lm-ink-3">›</span>
      <span class="font-semibold text-lm-ink">{{ selectedModule?.title ?? 'Loading' }}</span>
      <div class="flex-1" />
      <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3">{{ selectedSubTopic?.title ?? '' }}</span>
    </div>

    <div class="flex-1 overflow-auto relative">
      <div class="absolute inset-0 bg-dot-grid opacity-50 pointer-events-none" />

      <div v-if="loading" class="relative max-w-[840px] mx-auto px-6 py-9 text-lm-ink font-bold">Loading lesson...</div>
      <div v-else-if="error" class="relative max-w-[840px] mx-auto px-6 py-9 text-lm-rust font-bold">{{ error }}</div>
      <div v-else-if="!selectedSubTopic" class="relative max-w-[840px] mx-auto px-6 py-9 text-lm-ink font-bold">No lesson content is available.</div>

      <div v-else class="relative grid max-w-[1180px] mx-auto px-6 py-8 gap-6 lg:grid-cols-[240px_minmax(0,1fr)]">
        <aside class="space-y-3">
          <div v-for="module in course?.modules" :key="module.id" class="space-y-1">
            <h2 class="m-0 px-2 text-[11px] font-mono font-bold uppercase tracking-[0.06em] text-lm-ink-3">{{ module.title }}</h2>
            <button
              v-for="subTopic in module.subTopics"
              :key="subTopic.id"
              type="button"
              class="w-full rounded-[8px] border-2 px-3 py-2 text-left text-[12px] font-bold transition"
              :class="selectedSubTopic.id === subTopic.id ? 'border-lm-ink bg-lm-yellow text-lm-ink' : 'border-lm-line-soft bg-lm-surface text-lm-ink-2 hover:border-lm-line'"
              @click="selectSubTopic(subTopic)"
            >
              <span class="flex min-w-0 items-center gap-2">
                <span v-if="subTopic.interactionType !== 'NONE'" :class="progressMarkerClass(subTopic)">
                  <span v-if="subTopic.interactiveProgress?.status === 'MASTERED'">✓</span>
                </span>
                <span class="min-w-0 flex-1">{{ subTopic.title }}</span>
              </span>
            </button>
          </div>
        </aside>

        <article class="min-w-0 space-y-6">
          <header>
            <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3">{{ selectedModule?.title }}</span>
            <h1 class="font-display text-[34px] font-bold tracking-tight text-lm-ink leading-tight mt-1.5 m-0">{{ selectedSubTopic.title }}</h1>
          </header>

          <section class="lesson-body rounded-[12px] border-2 border-lm-line bg-lm-surface px-5 py-4 shadow-stamp-sm" v-html="selectedLessonHtml" />

          <InteractiveChallengeShell
            v-if="selectedSubTopic.interactionType !== 'NONE'"
            :interaction-type="selectedSubTopic.interactionType"
            :objective="currentChallengeObjective"
            :status="currentProgressStatus"
            :mode="currentInteractiveMode"
          >
            <InteractivePreview
              :config="interactiveConfig"
              @started="handleInteractiveStarted"
              @checked="handleInteractiveChecked"
            />
          </InteractiveChallengeShell>
        </article>
      </div>
    </div>

    <div class="flex items-center gap-2.5 px-6 py-3.5 bg-lm-yellow-soft border-t-2 border-lm-line shrink-0">
      <button class="nav-button" :disabled="selectedIndex <= 0" @click="goToOffset(-1)">Previous</button>
      <div class="flex-1" />
      <button class="nav-button nav-button--primary" :disabled="selectedIndex >= allSubTopics.length - 1" @click="goToOffset(1)">
        {{ currentInteractiveMastered ? 'Mastered · Next' : 'Next' }}
        <LmIcon name="arrow" :size="16" />
      </button>
    </div>
  </main>
</template>

<style scoped>
.nav-button {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  border: 2px solid #1a1814;
  border-radius: 999px;
  background: #fbf7ef;
  padding: 0.55rem 1rem;
  color: #1a1814;
  font-size: 15px;
  font-weight: 800;
  box-shadow: 2px 2px 0 #1a1814;
  transition: transform 150ms ease, box-shadow 150ms ease;
}
.nav-button:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 3px 3px 0 #1a1814;
}
.nav-button--primary {
  background: #1a1814;
  color: #fbf7ef;
}
.nav-button:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}
.progress-marker {
  display: inline-grid;
  width: 0.85rem;
  height: 0.85rem;
  flex: 0 0 auto;
  place-items: center;
  border: 2px solid currentColor;
  border-radius: 999px;
  font-size: 0.62rem;
  line-height: 1;
}
.progress-marker--not-started {
  color: #8f887e;
  background: transparent;
}
.progress-marker--tried {
  border-color: #1a1814;
  background: #ffd333;
  color: #1a1814;
}
.progress-marker--mastered {
  border-color: #245e3e;
  background: #dff4df;
  color: #245e3e;
  font-weight: 900;
}
:deep(.lesson-body h2) {
  margin: 1rem 0 0.35rem;
  font-size: 1.35rem;
  font-weight: 900;
}
:deep(.lesson-body h3) {
  margin: 0.85rem 0 0.25rem;
  font-size: 1.05rem;
  font-weight: 900;
}
:deep(.lesson-body p),
:deep(.lesson-body ul),
:deep(.lesson-body ol),
:deep(.lesson-body figure) {
  margin: 0.7rem 0;
  color: #3b3630;
  line-height: 1.7;
}
:deep(.lesson-body ul),
:deep(.lesson-body ol) {
  padding-left: 1.3rem;
}
:deep(.lesson-body img) {
  display: block;
  max-width: 100%;
  height: auto;
  margin: 0.85rem 0;
  border-radius: 8px;
}
:deep(.lesson-body a) {
  color: #1f63d4;
  font-weight: 800;
  text-decoration: underline;
  text-decoration-thickness: 2px;
  text-underline-offset: 0.18em;
}
:deep(.lesson-body a:hover) {
  color: #174a9b;
}
:deep(.lesson-body figcaption) {
  margin-top: -0.35rem;
  color: #6b6660;
  font-size: 0.86rem;
  font-weight: 700;
}
</style>
