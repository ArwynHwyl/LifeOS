<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import InteractiveChallengeShell from '@/features/courses/components/interactive/InteractiveChallengeShell.vue'
import InteractivePreview from '@/features/courses/components/interactive/InteractivePreview.vue'
import LearningAssistantPanel from '@/features/learning/components/LearningAssistantPanel.vue'
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
import { useGamificationStore } from '@/features/gamified/stores/gamification'
import ConfettiBurst from '@/components/motion/ConfettiBurst.vue'

const gamificationStore = useGamificationStore()

const route = useRoute()
const router = useRouter()

const course = ref<PublishedCourseDetailDto | null>(null)
const selectedSubTopicId = ref<number | null>(null)
const loading = ref(true)
const error = ref('')
const interactiveServerFeedback = ref('')
const canvasEl = ref<HTMLElement | null>(null)
const showMasteryFlash = ref(false)
const completeFire = ref(0)
const masteryExpAwarded = ref(0)
const masteryLeveledUp = ref(false)
const masteryNewLevel = ref(0)
const contentTransitionDir = ref<'next' | 'prev'>('next')
const assistantOpen = ref(false)
const assistantSelectedText = ref('')
const selectionButton = ref<{ x: number; y: number } | null>(null)
const selectionNotice = ref<{ x: number; y: number; selectedLength: number } | null>(null)

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
const selectedLessonHtml = computed(() => {
  let html = selectedSubTopic.value?.contentHtml || selectedSubTopic.value?.content || ''
  // The page header already shows the title, so drop a leading <h2> that repeats it.
  const leading = /^\s*<h[12][^>]*>([\s\S]*?)<\/h[12]>/i.exec(html)
  const normalize = (text: string) => text.replace(/<[^>]*>/g, '').replace(/\s+/g, ' ').trim().toLowerCase()
  if (leading && normalize(leading[1]) === normalize(selectedSubTopic.value?.title ?? '')) {
    html = html.slice(leading[0].length)
  }
  // Safe replacement of discrete/continuous words outside HTML tags
  html = html.replace(/(?<!<[^>]*)\bdiscrete\b(?![^<>]*>)/gi, '<span class="math-word-discrete">discrete</span>')
  html = html.replace(/(?<!<[^>]*)\bcontinuous\b(?![^<>]*>)/gi, '<span class="math-word-continuous">continuous</span>')
  return html
})

const isSidebarOpen = ref(false)
function toggleSidebar() {
  isSidebarOpen.value = !isSidebarOpen.value
}

const currentModuleSubTopics = computed(() => {
  return selectedModule.value?.subTopics ?? []
})
const currentSubTopicIndexInModule = computed(() => {
  const current = selectedSubTopic.value
  if (!current) return 0
  return Math.max(0, currentModuleSubTopics.value.findIndex((subTopic) => subTopic.id === current.id))
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
  clearAssistantSelection()
  nextTick(() => {
    if (canvasEl.value) canvasEl.value.scrollTop = 0
  })
})

function captureLessonSelection() {
  const selection = window.getSelection()
  const text = selection?.toString().replace(/\s+/g, ' ').trim() ?? ''
  if (!selection || selection.rangeCount === 0 || !text) {
    assistantSelectedText.value = ''
    selectionButton.value = null
    selectionNotice.value = null
    return
  }
  const range = selection.getRangeAt(0)
  const container = range.commonAncestorContainer.nodeType === Node.TEXT_NODE
    ? range.commonAncestorContainer.parentElement
    : range.commonAncestorContainer as HTMLElement
  if (!container?.closest('.lesson-body')) {
    assistantSelectedText.value = ''
    selectionButton.value = null
    selectionNotice.value = null
    return
  }
  const rect = range.getBoundingClientRect()
  if (text.length > 2000) {
    assistantSelectedText.value = ''
    selectionButton.value = null
    selectionNotice.value = {
      x: Math.min(window.innerWidth - 280, Math.max(10, rect.left + rect.width / 2 - 130)),
      y: Math.max(10, rect.top - 64),
      selectedLength: text.length,
    }
    return
  }
  selectionNotice.value = null
  assistantSelectedText.value = text
  selectionButton.value = {
    x: Math.min(window.innerWidth - 110, Math.max(10, rect.left + rect.width / 2 - 48)),
    y: Math.max(10, rect.top - 46),
  }
}

function askAboutSelection() {
  assistantOpen.value = true
  selectionButton.value = null
  selectionNotice.value = null
  window.getSelection()?.removeAllRanges()
}

function clearAssistantSelection() {
  assistantSelectedText.value = ''
  selectionButton.value = null
  selectionNotice.value = null
  window.getSelection()?.removeAllRanges()
}

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
  const status = subTopic.interactiveProgress?.status ?? 'NOT_STARTED'
  if (status === 'MASTERED') return 'progress-marker progress-marker--mastered'
  if (status === 'TRIED') return 'progress-marker progress-marker--tried'
  return 'progress-marker progress-marker--not-started'
}

function setSubTopicProgress(
  subTopicId: number,
  progress: InteractiveProgressDto,
  options: { skipCelebration?: boolean; justMastered?: boolean } = {},
) {
  if (!course.value) return
  /* ── Clever #4: Mastery celebration flash ── */
  if (!options.skipCelebration && progress.status === 'MASTERED') {
    const prevStatus = course.value.modules
      .flatMap((m) => m.subTopics)
      .find((s) => s.id === subTopicId)
      ?.interactiveProgress?.status
    const justMastered = options.justMastered ?? prevStatus !== 'MASTERED'
    if (justMastered) {
      masteryExpAwarded.value = progress.reward?.expAwarded ?? 0
      masteryLeveledUp.value = progress.reward?.leveledUp ?? false
      masteryNewLevel.value = progress.reward?.newLevel ?? 0
      showMasteryFlash.value = true
      setTimeout(() => { showMasteryFlash.value = false }, 1600)
    }
  }
  gamificationStore.handleReward(progress.reward)
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
  setSubTopicProgress(current.id, next, { skipCelebration: true })
  return next
}

async function persistInteractiveProgress(status: Exclude<InteractiveProgressStatus, 'NOT_STARTED'>) {
  const current = selectedSubTopic.value
  if (!current) return
  if (current.interactiveProgress?.status === 'MASTERED' && status === 'TRIED') return
  const wasAlreadyMastered = current.interactiveProgress?.status === 'MASTERED'
  optimisticProgress(status)
  try {
    const saved = await updateInteractiveProgress(String(route.params.courseId), current.id, status)
    setSubTopicProgress(current.id, saved, { justMastered: !wasAlreadyMastered })
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
  interactiveServerFeedback.value = ''

  let finalPayload = payload
  if (
    current.interactionType === 'LOGIC_FLOW' &&
    'kind' in payload &&
    payload.kind === 'CIRCUIT'
  ) {
    const values: Record<string, number> = {}
    if (payload.inputs) {
      for (const [key, val] of Object.entries(payload.inputs)) {
        values[key] = val ? 1 : 0
      }
    }
    finalPayload = {
      ...payload,
      values,
    } as any
  }

  try {
    const saved = await submitInteractiveAttempt(String(route.params.courseId), current.id, finalPayload)
    interactiveServerFeedback.value = saved.feedback
    setSubTopicProgress(current.id, {
      subTopicId: saved.subTopicId,
      status: saved.status,
      attemptCount: saved.attemptCount,
      masteredAt: saved.masteredAt,
      updatedAt: saved.updatedAt,
      reward: saved.reward,
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
    <!-- Collapsible Sidebar -->
    <aside class="lesson-sidebar" :class="{ 'lesson-sidebar--open': isSidebarOpen }">
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
            @click="selectSubTopic(subTopic); isSidebarOpen = false"
          >
            <span :class="progressMarkerClass(subTopic)">
              <span v-if="subTopic.interactiveProgress?.status === 'MASTERED'">✓</span>
            </span>
            <span>{{ subTopic.title }}</span>
          </button>
        </section>
      </div>
    </aside>

    <!-- Main Canvas Area -->
    <section ref="canvasEl" class="lesson-canvas" @click="isSidebarOpen = false">
      <div class="lesson-canvas__bg" />

      <!-- Top level headers outside the card -->
      <div class="lesson-top-bar" @click.stop>
        <div class="lesson-top-left">
          <button type="button" class="sidebar-toggle-btn" @click="toggleSidebar" aria-label="Toggle Outline">
            <svg v-if="isSidebarOpen" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" class="toggle-icon"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
            <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" class="toggle-icon"><line x1="3" y1="12" x2="21" y2="12"/><line x1="3" y1="6" x2="21" y2="6"/><line x1="3" y1="18" x2="21" y2="18"/></svg>
          </button>
          <span class="reading-time">
            <svg viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="2.2" class="clock-icon"><circle cx="8" cy="8" r="6.5"/><path d="M8 4.5V8l2.5 1.5"/></svg>
            {{ estimatedReadTime }} MIN READ
          </span>
        </div>
        
        <button type="button" class="lesson-exit-button" @click="router.back()">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" class="exit-icon"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          EXIT
        </button>
      </div>

      

      

      <div v-if="loading" class="lesson-state">Loading lesson...</div>
      <div v-else-if="error" class="lesson-state lesson-state--error">{{ error }}</div>
      <div v-else-if="!selectedSubTopic" class="lesson-state">No lesson content is available.</div>

      <Transition :name="contentTransitionDir === 'next' ? 'slide-next' : 'slide-prev'" mode="out-in">
        <article v-if="selectedSubTopic" :key="selectedSubTopic.id" class="lesson-content">
          <header class="lesson-content__header">
            <h1>{{ selectedSubTopic.title }}</h1>
          </header>

          <!-- Lesson body — open flow, no box wrapper -->
          <section class="lesson-body" @mouseup="captureLessonSelection" v-html="selectedLessonHtml" />

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

          <!-- Mark as Completed button for reading-only lessons or visualization-only interactives -->
          <div
            v-if="selectedSubTopic.interactionType === 'NONE' || currentInteractiveMode !== 'PRACTICE'"
            class="reading-complete-section relative"
          >
            <ConfettiBurst :fire="completeFire" :count="34" :spread="170" />
            <button
              v-if="currentProgressStatus !== 'MASTERED'"
              type="button"
              class="complete-btn complete-btn--uncompleted"
              @click="completeFire += 1; persistInteractiveProgress('MASTERED')"
            >
              <svg class="complete-btn-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
                <polyline points="20 6 9 17 4 12" />
              </svg>
              Mark as Completed
            </button>
            <div
              v-else
              class="complete-btn complete-btn--completed"
            >
              <svg class="complete-btn-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
                <polyline points="20 6 9 17 4 12" />
              </svg>
              Completed
            </div>
          </div>

          <!-- Pagination Dots inside the card -->
          <div class="lesson-pagination">
            <button
              v-for="(subTopic, idx) in currentModuleSubTopics"
              :key="subTopic.id"
              type="button"
              class="pagination-dot"
              :class="{ 'pagination-dot--active': currentSubTopicIndexInModule === idx }"
              @click.stop="selectSubTopic(subTopic)"
              :aria-label="`Go to step ${idx + 1}`"
            />
          </div>
        </article>
      </Transition>

      <button
        v-if="selectionButton"
        type="button"
        class="selection-ask-button"
        :style="{ left: `${selectionButton.x}px`, top: `${selectionButton.y}px` }"
        @click.stop="askAboutSelection"
      >
        Ask Tora
      </button>

      <div
        v-if="selectionNotice"
        class="selection-limit-notice"
        role="status"
        aria-live="polite"
        :style="{ left: `${selectionNotice.x}px`, top: `${selectionNotice.y}px` }"
      >
        <strong>Select 2,000 characters or fewer.</strong>
        <span>{{ selectionNotice.selectedLength.toLocaleString() }} selected</span>
      </div>

      <LearningAssistantPanel
        v-if="selectedSubTopic"
        v-model:open="assistantOpen"
        :course-id="String(route.params.courseId)"
        :sub-topic-id="selectedSubTopic.id"
        :selected-text="assistantSelectedText"
        @clear-selection="clearAssistantSelection"
      />
    </section>
    <!-- Mastery celebration overlay -->
        <Transition name="mastery-flash">
          <div v-if="showMasteryFlash" class="mastery-overlay">
            <ConfettiBurst :fire="1" :count="48" :spread="260" />
            <div class="mastery-burst" />
            <svg class="mastery-check" viewBox="0 0 64 64" fill="none">
              <circle cx="32" cy="32" r="28" stroke="#58cc02" stroke-width="3.5" fill="#ffffff" />
              <polyline points="20 33 28 41 44 25" stroke="#58cc02" stroke-width="4.5" stroke-linecap="round" stroke-linejoin="round" />
            </svg>
            <div v-if="masteryExpAwarded > 0" class="mastery-exp">
              <span class="mastery-exp__amount">+{{ masteryExpAwarded }} XP</span>
              <span v-if="masteryLeveledUp" class="mastery-exp__levelup">LEVEL UP! → {{ masteryNewLevel }}</span>
            </div>
          </div>
        </Transition>
    <!-- Floating Prev/Next Buttons outside the card -->
        <button
          type="button"
          class="floating-nav-btn floating-nav-btn--prev"
          :disabled="selectedIndex <= 0"
          @click.stop="goToOffset(-1)"
          aria-label="Previous Page"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" class="nav-arrow-icon"><polyline points="15 18 9 12 15 6"/></svg>
        </button>
        <button
          type="button"
          class="floating-nav-btn floating-nav-btn--next"
          :disabled="selectedIndex >= allSubTopics.length - 1"
          @click.stop="goToOffset(1)"
          aria-label="Next Page"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" class="nav-arrow-icon"><polyline points="9 18 15 12 9 6"/></svg>
        </button>
  </main>
</template>

<style scoped>
.lesson-player {
  position: relative;
  display: grid;
  grid-template-columns: 1fr;
  grid-template-rows: 1fr;
  flex: 1;
  min-height: 0;
  overflow: hidden;
  background: var(--color-lx-surface-soft);
  color: var(--color-lx-ink);
  font-family: var(--font-body);
}
.lesson-sidebar {
  position: absolute;
  top: 0;
  left: 0;
  bottom: 0;
  width: 275px;
  z-index: 100;
  display: flex;
  min-height: 0;
  flex-direction: column;
  border-right: 1px solid var(--color-lx-line);
  background: #ffffff;
  transform: translateX(-100%);
  transition: transform 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  box-shadow: 4px 0 24px rgba(0, 0, 0, 0.08);
}
.lesson-sidebar--open {
  transform: translateX(0);
}
.lesson-sidebar__header {
  display: grid;
  gap: 0.75rem;
  border-bottom: 1px solid var(--color-lx-line);
  padding: 2rem 1.15rem 1.5rem;
}
.lesson-sidebar__header span {
  color: var(--color-lx-ink-faint);
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}
.lesson-sidebar__header h1 {
  margin: 0;
  font-family: var(--font-display);
  font-size: 1rem;
  font-weight: 800;
  line-height: 1.25;
}
.lesson-progress {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 0.6rem;
}
.lesson-progress div {
  height: 0.4rem;
  overflow: hidden;
  border: none;
  border-radius: 999px;
  background: var(--color-lx-surface-soft);
}
.lesson-progress span {
  display: block;
  height: 100%;
  background: var(--color-lx-macaw);
  transition: width 400ms cubic-bezier(0.25, 0.8, 0.25, 1);
}
.lesson-progress strong {
  color: var(--color-lx-ink-faint);
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 700;
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
  font-family: var(--font-display);
  font-size: 0.84rem;
  font-weight: 800;
  line-height: 1.25;
}
.lesson-module__header strong {
  color: var(--color-lx-ink-faint);
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 700;
}
.lesson-topic {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: 0.7rem;
  width: 100%;
  min-height: 2.2rem;
  border: 0;
  border-left: 3px solid transparent;
  background: transparent;
  padding: 0.45rem 1rem 0.45rem 2.4rem;
  color: var(--color-lx-ink-faint);
  text-align: left;
  font-size: 0.8rem;
  font-weight: 700;
  transition: background 150ms ease, border-color 200ms ease, color 150ms ease;
}
.lesson-topic span:last-child {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.lesson-topic:hover {
  background: var(--color-lx-surface-soft);
  color: var(--color-lx-ink);
}
.lesson-topic--active {
  border-left-color: var(--color-lx-macaw);
  background: rgba(28, 176, 246, 0.1);
  color: var(--color-lx-macaw-dark);
}
.lesson-sidebar__empty,
.lesson-state {
  padding: 2rem;
  color: var(--color-lx-ink);
  font-weight: 700;
}
.lesson-state {
  position: relative;
  z-index: 1;
}
.lesson-state--error {
  color: #dc2626;
}
.selection-ask-button {
  position: fixed;
  z-index: 120;
  border: none;
  border-radius: 999px;
  background: var(--color-lx-macaw);
  padding: 0.45rem 0.9rem;
  color: #fff;
  font-size: 0.72rem;
  font-weight: 800;
  box-shadow: 0 4px 12px rgba(28, 176, 246, 0.4);
  cursor: pointer;
}
.selection-limit-notice {
  position: fixed;
  z-index: 120;
  display: grid;
  width: 260px;
  gap: 0.15rem;
  border: none;
  border-radius: 12px;
  background: #fef2f2;
  padding: 0.55rem 0.7rem;
  color: #dc2626;
  font-size: 0.68rem;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.1);
  pointer-events: none;
}
.selection-limit-notice strong {
  font-weight: 800;
}
.selection-limit-notice span {
  color: var(--color-lx-ink-soft);
  font-size: 0.6rem;
  font-weight: 700;
}

.lesson-canvas {
  position: relative;
  min-height: 0;
  overflow: auto;
  background: var(--color-lx-surface-soft);
}
.lesson-canvas__bg {
  position: absolute;
  inset: 0;
  background: var(--color-lx-surface-soft);
  pointer-events: none;
}
.lesson-top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: absolute;
  top: 1.5rem;
  left: 2rem;
  right: 2rem;
  z-index: 50;
}
.lesson-top-left {
  display: flex;
  align-items: center;
  gap: 1rem;
}
.sidebar-toggle-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2.25rem;
  height: 2.25rem;
  border: none;
  border-radius: 50%;
  background: #ffffff;
  color: var(--color-lx-ink);
  cursor: pointer;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  transition: transform 0.1s ease, box-shadow 0.1s ease;
}
.sidebar-toggle-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.14);
}
.toggle-icon {
  width: 1.1rem;
  height: 1.1rem;
}
.reading-time {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  color: var(--color-lx-ink-soft);
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
}
.clock-icon {
  width: 14px;
  height: 14px;
}
.lesson-exit-button {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  min-height: 2.25rem;
  border: none;
  border-radius: 999px;
  background: #ffffff;
  padding: 0 1.25rem;
  color: var(--color-lx-ink);
  font-size: 0.8rem;
  font-weight: 800;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  transition: transform 0.1s ease, box-shadow 0.1s ease;
  text-transform: uppercase;
}
.lesson-exit-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.14);
}
.exit-icon {
  width: 12px;
  height: 12px;
}
.floating-nav-btn {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  z-index: 40;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 3.5rem;
  height: 3.5rem;
  border: none;
  border-radius: 50%;
  background: #ffffff;
  color: var(--color-lx-ink);
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  transition: transform 0.15s ease, box-shadow 0.15s ease, opacity 0.2s ease;
  opacity: 0.8;
}
.floating-nav-btn:hover:not(:disabled) {
  transform: translateY(-50%) translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.16);
  opacity: 1;
}
.floating-nav-btn:disabled {
  cursor: not-allowed;
  opacity: 0.15;
  box-shadow: none;
}
.floating-nav-btn--prev {
  left: 2rem;
}
.floating-nav-btn--next {
  right: 2rem;
}
.nav-arrow-icon {
  width: 1.4rem;
  height: 1.4rem;
}
.lesson-content {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 1.5rem;
  width: min(calc(100% - 12rem), 1200px);
  margin: 6.5rem auto 4rem;
  border: none;
  border-radius: 28px;
  background: #ffffff;
  padding: 3.5rem;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04), 0 24px 48px -24px rgba(0, 0, 0, 0.18);
}

.lesson-content__header {
  margin-bottom: 1rem;
}
.lesson-content__header h1 {
  margin: 0;
  font-family: var(--font-display);
  color: var(--color-lx-ink);
  font-size: clamp(2rem, 4vw, 3rem);
  font-weight: 800;
  line-height: 1.1;
  text-wrap: balance;
}

.lesson-body {
  border: none;
  border-radius: 0;
  background: transparent;
  padding: 0;
  box-shadow: none;
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
  color: var(--color-lx-ink-faint);
  background: transparent;
}
.progress-marker--tried {
  border-color: var(--color-lx-macaw);
  background: rgba(28, 176, 246, 0.15);
  color: var(--color-lx-macaw-dark);
}
.progress-marker--mastered {
  border-color: var(--color-lx-feather-dark);
  background: rgba(88, 204, 2, 0.15);
  color: var(--color-lx-feather-dark);
  font-weight: 800;
}

/* ── Pagination Dots ── */
.lesson-pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 0.6rem;
  margin-top: 2.5rem;
  padding-top: 1rem;
}
.pagination-dot {
  display: block;
  padding: 0;
  border: 0;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--color-lx-line);
  cursor: pointer;
  transition: width 0.2s ease, border-radius 0.2s ease, background-color 0.2s ease;
}
.pagination-dot:hover {
  background: var(--color-lx-ink-faint);
}
.pagination-dot--active {
  width: 24px;
  border-radius: 4px;
  background: var(--color-lx-macaw);
}

/* ── Mastery celebration overlay ── */
.mastery-overlay {
  position: absolute;
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
  background: radial-gradient(circle, rgba(88, 204, 2, 0.22) 0%, transparent 70%);
  animation: burst-expand 1.2s cubic-bezier(0.22, 1, 0.36, 1) forwards;
}
.mastery-check {
  width: 64px;
  height: 64px;
  animation: check-pop 0.5s cubic-bezier(0.34, 1.56, 0.64, 1) forwards;
  filter: drop-shadow(0 4px 12px rgba(70, 163, 2, 0.3));
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
.mastery-exp {
  position: absolute;
  margin-top: 92px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  animation: exp-rise 1.4s cubic-bezier(0.22, 1, 0.36, 1) forwards;
}
.mastery-exp__amount {
  font-family: var(--font-display);
  font-weight: 700;
  font-size: 20px;
  color: var(--color-lx-feather-dark);
  text-shadow: 0 2px 8px rgba(70, 163, 2, 0.2);
}
.mastery-exp__levelup {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--color-lx-fox-dark);
}
@keyframes exp-rise {
  0% { transform: translateY(8px); opacity: 0; }
  25% { transform: translateY(0); opacity: 1; }
  80% { transform: translateY(-4px); opacity: 1; }
  100% { transform: translateY(-14px); opacity: 0; }
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

/* ── Content entrance ── */
:deep(.lesson-body > *) { animation: rise-in 0.5s cubic-bezier(0.22, 1, 0.36, 1) both; }
:deep(.lesson-body > *:nth-child(2)) { animation-delay: 0.06s; }
:deep(.lesson-body > *:nth-child(3)) { animation-delay: 0.12s; }
:deep(.lesson-body > *:nth-child(4)) { animation-delay: 0.18s; }
:deep(.lesson-body > *:nth-child(5)) { animation-delay: 0.24s; }
:deep(.lesson-body > *:nth-child(n + 6)) { animation-delay: 0.3s; }
.complete-btn--uncompleted { transition: transform 0.08s ease, box-shadow 0.08s ease, filter 0.15s ease; }
.complete-btn--uncompleted:hover { filter: brightness(1.06); }
.complete-btn--completed { animation: pop-in 0.45s cubic-bezier(0.34, 1.56, 0.64, 1) both; }
@media (prefers-reduced-motion: reduce) { :deep(.lesson-body > *), .complete-btn--completed { animation: none; } }

/* ── Typography for lesson body content ── */
:deep(.lesson-body h2) {
  margin-top: 2.5rem;
  margin-bottom: 1.5rem;
  font-family: var(--font-display);
  font-size: 1.85rem;
  font-weight: 700;
  line-height: 1.2;
  border-bottom: 1px solid var(--color-lx-line);
  padding-bottom: 0.5rem;
  color: var(--color-lx-ink);
}
:deep(.lesson-body h3) {
  margin-top: 2rem;
  margin-bottom: 1rem;
  font-family: var(--font-display);
  font-size: 1.4rem;
  font-weight: 700;
  line-height: 1.3;
  color: var(--color-lx-ink);
}
:deep(.lesson-body p),
:deep(.lesson-body ul),
:deep(.lesson-body ol),
:deep(.lesson-body figure) {
  margin: 1.25rem 0;
  color: var(--color-lx-ink-soft);
  font-family: var(--font-body);
  font-size: 1.05rem;
  line-height: 1.65;
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
  color: var(--color-lx-macaw-dark);
  font-weight: 700;
  text-decoration: underline;
  text-decoration-thickness: 2px;
  text-underline-offset: 0.18em;
}
:deep(.lesson-body a:hover) {
  color: var(--color-lx-macaw);
}
:deep(.lesson-body figcaption) {
  margin-top: -0.35rem;
  color: var(--color-lx-ink-faint);
  font-size: 0.86rem;
  font-weight: 700;
}

/* ── Inline math word highlights ── */
:deep(.lesson-body .math-word-discrete) {
  color: var(--color-lx-fox-dark);
  font-weight: 800;
  text-decoration: underline;
  text-decoration-color: var(--color-lx-fox-dark);
  text-decoration-thickness: 2.5px;
  text-underline-offset: 4px;
}
:deep(.lesson-body .math-word-continuous) {
  color: var(--color-lx-macaw-dark);
  font-weight: 800;
  text-decoration: underline;
  text-decoration-color: var(--color-lx-macaw-dark);
  text-decoration-thickness: 2.5px;
  text-underline-offset: 4px;
}

@media (max-width: 900px) {
  .lesson-content {
    width: min(calc(100% - 2rem), 640px);
    margin: 5.2rem auto 6rem;
    padding: 2rem;
    border-radius: 32px;
  }
  .floating-nav-btn {
    width: 3rem;
    height: 3rem;
  }
  .floating-nav-btn--prev {
    left: 0.5rem;
  }
  .floating-nav-btn--next {
    right: 0.5rem;
  }
}

:deep(.material-symbols-outlined) {
  font-family: 'Material Symbols Outlined', sans-serif;
  font-weight: normal;
  font-style: normal;
  font-size: 24px;
  line-height: 1;
  letter-spacing: normal;
  text-transform: none;
  display: inline-block;
  white-space: nowrap;
  word-wrap: normal;
  direction: ltr;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-rendering: optimizeLegibility;
  font-feature-settings: "liga";
}

:deep(.math-var) {
  font-family: 'Literata', serif;
  font-style: italic;
}
/* ── Mark as Completed Button ── */
.reading-complete-section {
  display: flex;
  justify-content: center;
  margin-top: 2.5rem;
  padding-top: 1.5rem;
  border-top: 1px solid var(--color-lx-line);
}
.complete-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  min-height: 2.75rem;
  border: none;
  border-radius: 999px;
  padding: 0 1.75rem;
  font-family: var(--font-body);
  font-size: 0.95rem;
  font-weight: 800;
  cursor: pointer;
  transition: transform 0.08s ease, box-shadow 0.08s ease, background-color 0.15s ease;
}
.complete-btn--uncompleted {
  background: var(--color-lx-feather);
  color: #fff;
  box-shadow: 0 4px 0 var(--color-lx-feather-dark);
}
.complete-btn--uncompleted:active {
  transform: translateY(4px);
  box-shadow: 0 0 0 transparent;
}
.complete-btn--completed {
  background: rgba(88, 204, 2, 0.12);
  color: var(--color-lx-feather-dark);
  cursor: default;
}
.complete-btn-icon {
  width: 1rem;
  height: 1rem;
}
</style>
