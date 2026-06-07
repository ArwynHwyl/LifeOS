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
  if (!current) return 'Complete this activity to master the concept.'
  if (current.interactionPrompt?.trim()) return current.interactionPrompt
  if (current.interactionType === 'QUIZ') return 'Answer the quiz to check your understanding.'
  if (current.interactionType === 'GRAPH_2D') return 'Use the graph to match the target behavior.'
  if (current.interactionType === 'FORMULA_EXPLORER') return 'Adjust the formula inputs to reach the target.'
  if (current.interactionType === 'VISUAL_LAYER') return 'Build or inspect the set diagram to master the concept.'
  return 'Complete this activity to master the concept.'
})
const currentInteractiveMastered = computed(() => selectedSubTopic.value?.interactionType !== 'NONE' && currentProgressStatus.value === 'MASTERED')
const isQuizActivity = computed(() => selectedSubTopic.value?.interactionType === 'QUIZ')

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

function moduleProgressCount(module: PublishedCourseDetailDto['modules'][number]) {
  return module.subTopics.filter((subTopic) => subTopic.interactiveProgress?.status === 'MASTERED').length
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
  <main class="lesson-player">
    <aside class="lesson-sidebar">
      <div class="lesson-sidebar__header">
        <span>Course Outline</span>
        <h1>{{ course?.title ?? 'Lesson' }}</h1>
        <div class="lesson-progress">
          <div><span :style="{ width: `${progressPercent}%` }" /></div>
          <strong>{{ selectedIndex + 1 }}/{{ Math.max(allSubTopics.length, 1) }}</strong>
        </div>
      </div>

      <div v-if="loading" class="lesson-sidebar__empty">Loading outline...</div>
      <div v-else-if="course" class="lesson-module-list">
        <section v-for="module in course.modules" :key="module.id" class="lesson-module">
          <header class="lesson-module__header">
            <span>⌄</span>
            <h2>{{ module.title }}</h2>
            <strong>{{ moduleProgressCount(module) }}/{{ module.subTopics.length }}</strong>
          </header>
          <button
            v-for="subTopic in module.subTopics"
            :key="subTopic.id"
            type="button"
            class="lesson-topic"
            :class="{ 'lesson-topic--active': selectedSubTopic?.id === subTopic.id }"
            @click="selectSubTopic(subTopic)"
          >
            <span v-if="subTopic.interactionType !== 'NONE'" :class="progressMarkerClass(subTopic)">
              <span v-if="subTopic.interactiveProgress?.status === 'MASTERED'">✓</span>
            </span>
            <span v-else class="progress-marker progress-marker--not-started" />
            <span>{{ subTopic.title }}</span>
          </button>
        </section>
      </div>
    </aside>

    <section class="lesson-canvas">
      <div class="lesson-canvas__bg" />
      <div v-if="loading" class="lesson-state">Loading lesson...</div>
      <div v-else-if="error" class="lesson-state lesson-state--error">{{ error }}</div>
      <div v-else-if="!selectedSubTopic" class="lesson-state">No lesson content is available.</div>

      <article v-else class="lesson-content" :class="{ 'lesson-content--quiz': isQuizActivity }">
        <button type="button" class="lesson-exit-button" @click="router.back()">
          <LmIcon name="close" :size="14" />
          Exit
        </button>

        <template v-if="isQuizActivity">
          <section v-if="selectedLessonHtml" class="lesson-body lesson-body--quiz" v-html="selectedLessonHtml" />
          <InteractivePreview
            :config="interactiveConfig"
            @started="handleInteractiveStarted"
            @checked="handleInteractiveChecked"
          />
        </template>

        <template v-else>
          <header class="lesson-content__header">
            <span>{{ selectedModule?.title }}</span>
            <h1>{{ selectedSubTopic.title }}</h1>
          </header>

          <section class="lesson-body" v-html="selectedLessonHtml" />

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
        </template>
      </article>
    </section>

    <footer class="lesson-footer">
      <button class="nav-button" :disabled="selectedIndex <= 0" @click="goToOffset(-1)">Previous</button>
      <div class="lesson-footer__spacer" />
      <button class="nav-button nav-button--primary" :disabled="selectedIndex >= allSubTopics.length - 1" @click="goToOffset(1)">
        {{ currentInteractiveMastered ? 'Mastered · Next' : 'Next' }}
        <LmIcon name="arrow" :size="16" />
      </button>
    </footer>
  </main>
</template>

<style scoped>
.lesson-player {
  display: grid;
  grid-template-columns: 275px minmax(0, 1fr);
  grid-template-rows: minmax(0, 1fr) auto;
  flex: 1;
  min-height: 0;
  overflow: hidden;
  border-top: 2px solid #1a1814;
  background: #fbf7ef;
  color: #1a1814;
}
.lesson-sidebar {
  grid-row: 1 / span 2;
  display: flex;
  min-height: 0;
  flex-direction: column;
  border-right: 2px solid #1a1814;
  background: #fbf7ef;
}
.lesson-sidebar__header {
  display: grid;
  gap: 0.75rem;
  border-bottom: 1px solid #e4ded6;
  padding: 2rem 1.15rem 1.5rem;
}
.lesson-sidebar__header span {
  color: #8f887e;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 10px;
  font-weight: 900;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}
.lesson-sidebar__header h1 {
  margin: 0;
  font-size: 1rem;
  font-weight: 950;
  line-height: 1.25;
}
.lesson-progress {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 0.6rem;
}
.lesson-progress div {
  height: 0.45rem;
  overflow: hidden;
  border: 1px solid #1a1814;
  border-radius: 999px;
  background: #fffdf8;
}
.lesson-progress span {
  display: block;
  height: 100%;
  background: #1a1814;
}
.lesson-progress strong {
  color: #8f887e;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 10px;
  font-weight: 900;
}
.lesson-module-list {
  min-height: 0;
  overflow: auto;
  padding: 1rem 0 1.5rem;
}
.lesson-module {
  display: grid;
  gap: 0.25rem;
  margin-bottom: 1rem;
}
.lesson-module__header {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 0.55rem;
  padding: 0.3rem 1rem;
}
.lesson-module__header h2 {
  min-width: 0;
  margin: 0;
  font-size: 0.84rem;
  font-weight: 950;
  line-height: 1.25;
}
.lesson-module__header strong {
  color: #8f887e;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 10px;
  font-weight: 900;
}
.lesson-topic {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: 0.7rem;
  width: 100%;
  min-height: 2.2rem;
  border: 0;
  border-left: 4px solid transparent;
  background: transparent;
  padding: 0.45rem 1rem 0.45rem 2.4rem;
  color: #8f887e;
  text-align: left;
  font-size: 0.8rem;
  font-weight: 850;
}
.lesson-topic span:last-child {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.lesson-topic:hover {
  background: #fff4bf;
  color: #1a1814;
}
.lesson-topic--active {
  border-left-color: #1a1814;
  background: #ffd333;
  color: #1a1814;
}
.lesson-sidebar__empty,
.lesson-state {
  padding: 2rem;
  color: #1a1814;
  font-weight: 900;
}
.lesson-state {
  position: relative;
  z-index: 1;
}
.lesson-state--error {
  color: #8c3322;
}
.lesson-canvas {
  position: relative;
  min-height: 0;
  overflow: auto;
  background: #fffdf8;
}
.lesson-canvas__bg {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle, rgba(26, 24, 20, 0.08) 1px, transparent 1.5px),
    #fffdf8;
  background-size: 18px 18px;
  pointer-events: none;
}
.lesson-content {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 1.5rem;
  width: min(100%, 920px);
  margin: 0 auto;
  padding: 3rem 2rem;
}
.lesson-content--quiz {
  width: min(100%, 940px);
  min-height: 100%;
  align-content: start;
  padding-top: 3.2rem;
}
.lesson-content__header span {
  color: #8f887e;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}
.lesson-content__header h1 {
  margin: 0.25rem 0 0;
  color: #1a1814;
  font-size: 2rem;
  font-weight: 950;
  line-height: 1.12;
}
.lesson-exit-button {
  position: absolute;
  top: 1rem;
  right: 1rem;
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  min-height: 2.25rem;
  border: 2px solid #1a1814;
  border-radius: 999px;
  background: #fffdf8;
  padding: 0 1rem;
  color: #1a1814;
  font-size: 0.8rem;
  font-weight: 900;
  box-shadow: 2px 2px 0 #1a1814;
}
.lesson-body {
  border: 2px solid #1a1814;
  border-radius: 12px;
  background: #fffdf8;
  padding: 1.25rem;
  box-shadow: 3px 3px 0 #1a1814;
}
.lesson-body--quiz {
  width: min(100%, 860px);
  margin: 20px auto 0.25rem;
  border-color: #d4cec6;
  background: rgba(255, 253, 248, 0.92);
  box-shadow: none;
}
.lesson-footer {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  border-top: 2px solid #1a1814;
  background: #fff0a8;
  padding: 1rem 2rem;
}
.lesson-footer__spacer {
  flex: 1;
}
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
@media (max-width: 900px) {
  .lesson-player {
    grid-template-columns: 1fr;
  }
  .lesson-sidebar {
    grid-row: auto;
    max-height: 42vh;
    border-right: 0;
    border-bottom: 2px solid #1a1814;
  }
  .lesson-sidebar__header {
    padding: 1rem;
  }
  .lesson-topic {
    padding-left: 1rem;
  }
  .lesson-content,
  .lesson-content--quiz {
    padding: 3.5rem 1rem 1.5rem;
  }
  .lesson-footer {
    padding: 0.8rem 1rem;
  }
}
</style>
