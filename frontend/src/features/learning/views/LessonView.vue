<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import InteractiveChallengeShell from '@/features/courses/components/interactive/InteractiveChallengeShell.vue'
import InteractivePreview from '@/features/courses/components/interactive/InteractivePreview.vue'
import { parseInteractiveConfig } from '@/features/courses/types/interactive'
import {
  getPublishedCourse,
  submitInteractiveAttempt,
  updateInteractiveProgress,
  type InteractiveAttemptRequest,
  type InteractiveProgressDto,
  type InteractiveProgressStatus,
  type PublishedCourseDetailDto,
  type PublishedSubTopicDto,
} from '@/features/learning/services/learnerCourses'
import toraMascotUrl from '@/assets/tora-mascot.svg'
import LmIcon from '../components/LmIcon.vue'

const route = useRoute()
const router = useRouter()

const course = ref<PublishedCourseDetailDto | null>(null)
const selectedSubTopicId = ref<number | null>(null)
const loading = ref(true)
const error = ref('')
const interactiveServerFeedback = ref('')
const canvasEl = ref<HTMLElement | null>(null)
const showMasteryFlash = ref(false)
const contentTransitionDir = ref<'next' | 'prev'>('next')

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
const mascotPrompt = computed(() => {
  const current = selectedSubTopic.value
  const prompt = current?.mascotPrompt?.trim()
  if (prompt) return prompt
  return current ? `Think about how "${current.title}" connects to this lesson.` : ''
})
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

/* ── Clever #1: Estimated reading time ── */
const estimatedReadTime = computed(() => {
  const html = selectedLessonHtml.value
  if (!html) return 0
  const text = html.replace(/<[^>]*>/g, ' ').replace(/\s+/g, ' ').trim()
  const words = text.split(' ').filter(Boolean).length
  return Math.max(1, Math.ceil(words / 200))
})

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
  window.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
})

/* ── Clever #2: Keyboard navigation ── */
function handleKeydown(e: KeyboardEvent) {
  if ((e.target as HTMLElement)?.closest('input, textarea, [contenteditable]')) return
  if (e.key === 'ArrowRight' || e.key === 'ArrowDown') {
    e.preventDefault()
    goToOffset(1)
  } else if (e.key === 'ArrowLeft' || e.key === 'ArrowUp') {
    e.preventDefault()
    goToOffset(-1)
  }
}

watch(selectedSubTopicId, () => {
  nextTick(() => {
    if (canvasEl.value) canvasEl.value.scrollTop = 0
  })
})

function selectSubTopic(subTopic: PublishedSubTopicDto) {
  const idx = allSubTopics.value.findIndex((s) => s.id === subTopic.id)
  contentTransitionDir.value = idx > selectedIndex.value ? 'next' : 'prev'
  selectedSubTopicId.value = subTopic.id
  interactiveServerFeedback.value = ''
}

function goToOffset(offset: number) {
  contentTransitionDir.value = offset > 0 ? 'next' : 'prev'
  const next = allSubTopics.value[selectedIndex.value + offset]
  if (next) {
    selectedSubTopicId.value = next.id
  }
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
  /* ── Clever #4: Mastery celebration flash ── */
  if (progress.status === 'MASTERED') {
    const prev = course.value.modules
      .flatMap((m) => m.subTopics)
      .find((s) => s.id === subTopicId)
      ?.interactiveProgress?.status
    if (prev !== 'MASTERED') {
      showMasteryFlash.value = true
      setTimeout(() => { showMasteryFlash.value = false }, 1600)
    }
  }
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
  if (selectedSubTopic.value?.interactionType === 'LOGIC_FLOW') {
    if (currentProgressStatus.value === 'NOT_STARTED') {
      void persistInteractiveProgress('TRIED')
    }
    return
  }
  if (currentInteractiveMode.value !== 'PRACTICE') {
    if (currentProgressStatus.value === 'NOT_STARTED') {
      void persistInteractiveProgress('TRIED')
    }
    return
  }
  if (currentProgressStatus.value === 'NOT_STARTED') {
    void persistInteractiveProgress('TRIED')
  }
}

async function persistServerGradedAttempt(payload: InteractiveAttemptRequest) {
  const current = selectedSubTopic.value
  if (!current || current.interactionType === 'NONE') return
  try {
    const saved = await submitInteractiveAttempt(String(route.params.courseId), current.id, payload)
    interactiveServerFeedback.value = saved.feedback
    setSubTopicProgress(current.id, {
      subTopicId: saved.subTopicId,
      status: saved.status,
      attemptCount: saved.attemptCount,
      masteredAt: saved.masteredAt,
      updatedAt: saved.updatedAt,
    })
  } catch (err) {
    interactiveServerFeedback.value = ''
    error.value = err instanceof Error ? err.message : 'Unable to submit logic attempt.'
  }
}

function handleInteractiveChecked(payload: { passed: boolean; attempt?: InteractiveAttemptRequest }) {
  if (payload.attempt) {
    void persistServerGradedAttempt(payload.attempt)
    return
  }
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
            <strong>{{ module.subTopics.length }}</strong>
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

    <section ref="canvasEl" class="lesson-canvas">
      <div class="lesson-canvas__bg" />

      <!-- Mastery celebration overlay -->
      <Transition name="mastery-flash">
        <div v-if="showMasteryFlash" class="mastery-overlay">
          <div class="mastery-burst" />
          <svg class="mastery-check" viewBox="0 0 64 64" fill="none">
            <circle cx="32" cy="32" r="28" stroke="#245e3e" stroke-width="3" fill="#dff4df" />
            <polyline points="20 33 28 41 44 25" stroke="#245e3e" stroke-width="4" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </div>
      </Transition>

      <div v-if="loading" class="lesson-state">Loading lesson...</div>
      <div v-else-if="error" class="lesson-state lesson-state--error">{{ error }}</div>
      <div v-else-if="!selectedSubTopic" class="lesson-state">No lesson content is available.</div>

      <Transition :name="contentTransitionDir === 'next' ? 'slide-next' : 'slide-prev'" mode="out-in">
        <article v-if="selectedSubTopic" :key="selectedSubTopic.id" class="lesson-content" :class="{ 'lesson-content--quiz': isQuizActivity }">
          <button type="button" class="lesson-exit-button" @click="router.back()">
            <LmIcon name="close" :size="14" />
            Exit
          </button>

          <template v-if="isQuizActivity">
            <header class="lesson-content__header">
              <div class="lesson-hero-copy">
                <span>{{ selectedModule?.title }}</span>
                <h1>{{ selectedSubTopic.title }}</h1>
                <div class="lesson-meta">
                  <span class="lesson-meta__badge">Quiz</span>
                </div>
              </div>
              <div class="lesson-mascot">
                <p>{{ mascotPrompt }}</p>
                <div class="lesson-mascot__avatar">
                  <img :src="toraMascotUrl" alt="" />
                </div>
              </div>
            </header>

            <section v-if="selectedLessonHtml" class="lesson-body lesson-body--quiz" v-html="selectedLessonHtml" />

            <div class="quiz-preview-frame">
              <InteractivePreview
                :config="interactiveConfig"
                :server-feedback="interactiveServerFeedback"
                @started="handleInteractiveStarted"
                @checked="handleInteractiveChecked"
              />
            </div>
          </template>

          <template v-else>
            <header class="lesson-content__header">
              <div class="lesson-hero-copy">
                <span>{{ selectedModule?.title }}</span>
                <h1>{{ selectedSubTopic.title }}</h1>
                <div class="lesson-meta">
                  <span v-if="estimatedReadTime" class="lesson-meta__item">
                    <svg viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="8" cy="8" r="6.5"/><path d="M8 4.5V8l2.5 1.5"/></svg>
                    {{ estimatedReadTime }} min read
                  </span>
                  <span v-if="selectedSubTopic.interactionType !== 'NONE'" class="lesson-meta__item lesson-meta__item--activity">
                    <svg viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M8 1L1 4.5 8 8l7-3.5L8 1zM1 11.5l7 3.5 7-3.5M1 8l7 3.5L15 8"/></svg>
                    Has activity
                  </span>
                </div>
              </div>
              <div class="lesson-mascot">
                <p>{{ mascotPrompt }}</p>
                <div class="lesson-mascot__avatar">
                  <img :src="toraMascotUrl" alt="" />
                </div>
              </div>
            </header>

            <!-- Lesson body — open flow, no box wrapper -->
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
                :server-feedback="interactiveServerFeedback"
                @started="handleInteractiveStarted"
                @checked="handleInteractiveChecked"
              />
            </InteractiveChallengeShell>
          </template>
        </article>
      </Transition>
    </section>

    <footer class="lesson-footer">
      <button class="nav-button" :disabled="selectedIndex <= 0" @click="goToOffset(-1)">
        <LmIcon name="arrow" :size="16" style="transform: rotate(180deg)" />
        Previous
      </button>
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
  background: #f6f0e7;
  color: #1a1814;
}
.lesson-sidebar {
  grid-row: 1 / span 2;
  display: flex;
  min-height: 0;
  flex-direction: column;
  border-right: 2px solid #1a1814;
  background: #f6f0e7;
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
  transition: width 400ms cubic-bezier(0.25, 0.8, 0.25, 1);
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
  transition: background 150ms ease, border-color 200ms ease;
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
  gap: 1.9rem;
  width: min(calc(100% - 6rem), 900px);
  margin: 6.2rem auto 7rem;
  border: 2px solid #1a1814;
  border-radius: 22px;
  background: #fffdf8;
  padding: 4rem 3.1rem 3rem;
  box-shadow: 6px 6px 0 #1a1814;
}
.lesson-content--quiz {
  width: min(calc(100% - 6rem), 940px);
  align-content: start;
}
.lesson-content__header {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(270px, 420px);
  align-items: center;
  gap: 2rem;
}
.lesson-hero-copy span {
  color: #8f887e;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}
.lesson-hero-copy h1 {
  margin: 0.25rem 0 0;
  color: #1a1814;
  font-size: clamp(2.35rem, 5vw, 4rem);
  font-weight: 950;
  line-height: 0.96;
}
.lesson-mascot {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 78px;
  align-items: center;
  justify-self: end;
  width: min(100%, 390px);
}
.lesson-mascot p {
  position: relative;
  min-width: 0;
  margin: 0;
  border: 2px solid #1a1814;
  border-radius: 14px;
  background: #fffdf8;
  padding: 1rem 1.2rem;
  color: #1a1814;
  font-size: 0.9rem;
  font-style: italic;
  font-weight: 650;
  line-height: 1.45;
  box-shadow: 4px 4px 0 #1a1814;
}
.lesson-mascot p::after {
  content: "";
  position: absolute;
  top: 50%;
  right: -12px;
  width: 18px;
  height: 18px;
  border-top: 2px solid #1a1814;
  border-right: 2px solid #1a1814;
  background: #fffdf8;
  transform: translateY(-50%) rotate(45deg);
}
.lesson-mascot__avatar {
  display: grid;
  width: 78px;
  height: 78px;
  place-items: center;
  overflow: hidden;
  border: 2px solid #1a1814;
  border-radius: 50%;
  background: #ffd333;
}
.lesson-mascot__avatar img {
  width: 76px;
  height: 88px;
  object-fit: contain;
  object-position: center 15px;
}

/* ── Lesson meta (reading time + activity badge) ── */
.lesson-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.75rem;
  margin-top: 0.5rem;
}
.lesson-meta__item {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  color: #8f887e;
  font-size: 12px;
  font-weight: 700;
}
.lesson-meta__item svg {
  width: 13px;
  height: 13px;
}
.lesson-meta__item--activity {
  color: #a37c2b;
}
.lesson-meta__badge {
  display: inline-flex;
  align-items: center;
  border: 2px solid #1a1814;
  border-radius: 6px;
  background: #ffd333;
  padding: 0.15rem 0.5rem;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 10px;
  font-weight: 900;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: #1a1814;
}

.lesson-exit-button {
  position: absolute;
  top: -4.55rem;
  right: 0;
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
  box-shadow: 3px 3px 0 #1a1814;
}

/* ── Lesson body — open, no outer box ── */
.lesson-body {
  border: none;
  border-radius: 0;
  background: transparent;
  padding: 0;
  box-shadow: none;
}
.lesson-body--quiz {
  width: min(100%, 860px);
  margin: 0 auto;
  border: 1.5px solid #d4cec6;
  border-radius: 12px;
  background: #fffdf8;
  padding: 1.25rem 1.5rem;
  box-shadow: none;
}
.lesson-footer {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  border-top: 2px solid #1a1814;
  background: rgba(255, 240, 168, 0.78);
  padding: 1.35rem 2rem;
  backdrop-filter: blur(10px);
}
.lesson-footer__spacer {
  flex: 1;
}

.nav-button {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  border: 2px solid #1a1814;
  border-radius: 10px;
  background: #fbf7ef;
  min-width: 140px;
  justify-content: center;
  padding: 0.85rem 1.35rem;
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

/* ── Mastery celebration overlay ── */
.mastery-overlay {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: grid;
  place-items: center;
  pointer-events: none;
}
.mastery-burst {
  position: absolute;
  width: 200px;
  height: 200px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(223, 244, 223, 0.7) 0%, transparent 70%);
  animation: burst-expand 1.2s cubic-bezier(0.22, 1, 0.36, 1) forwards;
}
.mastery-check {
  width: 64px;
  height: 64px;
  animation: check-pop 0.5s cubic-bezier(0.34, 1.56, 0.64, 1) forwards;
  filter: drop-shadow(0 4px 12px rgba(36, 94, 62, 0.25));
}
@keyframes burst-expand {
  0% { transform: scale(0.3); opacity: 1; }
  60% { transform: scale(3); opacity: 0.6; }
  100% { transform: scale(4.5); opacity: 0; }
}
@keyframes check-pop {
  0% { transform: scale(0); opacity: 0; }
  50% { transform: scale(1.15); opacity: 1; }
  100% { transform: scale(1); opacity: 1; }
}
.mastery-flash-enter-active { transition: opacity 200ms ease; }
.mastery-flash-leave-active { transition: opacity 600ms ease; }
.mastery-flash-enter-from { opacity: 0; }
.mastery-flash-leave-to { opacity: 0; }

/* ── Content transition: slide + fade ── */
.slide-next-enter-active,
.slide-next-leave-active,
.slide-prev-enter-active,
.slide-prev-leave-active {
  transition: opacity 220ms ease, transform 220ms ease;
}
.slide-next-enter-from {
  opacity: 0;
  transform: translateX(24px);
}
.slide-next-leave-to {
  opacity: 0;
  transform: translateX(-24px);
}
.slide-prev-enter-from {
  opacity: 0;
  transform: translateX(-24px);
}
.slide-prev-leave-to {
  opacity: 0;
  transform: translateX(24px);
}

/* ── Typography for lesson body content ── */
:deep(.lesson-body h2) {
  margin: 1.5rem 0 0.5rem;
  font-size: 1.35rem;
  font-weight: 900;
}
:deep(.lesson-body h3) {
  margin: 1.1rem 0 0.35rem;
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
    width: min(calc(100% - 2rem), 640px);
    margin: 5.2rem auto 6rem;
    padding: 2rem 1.2rem 1.5rem;
    border-radius: 18px;
  }
  .lesson-content__header {
    grid-template-columns: 1fr;
    gap: 1.25rem;
  }
  .lesson-mascot {
    justify-self: stretch;
    width: 100%;
    grid-template-columns: minmax(0, 1fr) 64px;
  }
  .lesson-mascot__avatar {
    width: 64px;
    height: 64px;
  }
  .lesson-mascot__avatar img {
    width: 64px;
    height: 76px;
  }
  .lesson-footer {
    padding: 0.8rem 1rem;
  }
  .nav-button {
    min-width: 0;
    flex: 1;
  }
}
</style>
