<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { AxiosError } from 'axios'
import { useRoute, useRouter } from 'vue-router'
import AdminNavbar from '@/features/courses/components/Admin/AdminNavbar.vue'
import AdminIcon from '@/features/courses/components/Admin/AdminIcon.vue'
import AdminCourseOutline from '@/features/courses/components/Admin/AdminCourseOutline.vue'
import ITypeBadge from '@/features/courses/components/Admin/ITypeBadge.vue'
import MonoLabel from '@/features/courses/components/Admin/MonoLabel.vue'
import StatusBadge from '@/features/courses/components/Admin/StatusBadge.vue'
import SubTopicLessonEditor from '@/features/courses/components/Admin/SubTopicLessonEditor.vue'
import { parseInteractiveConfig } from '@/features/courses/types/interactive'
import InteractiveChallengeShell from '@/features/courses/components/interactive/InteractiveChallengeShell.vue'
import InteractivePreview from '@/features/courses/components/interactive/InteractivePreview.vue'
import { DEFAULT_COVER_ID, getCoverPreset } from '@/features/courses/constants/courseCoverPresets'
import {
  getAdminCourse,
  previewCourseDocument,
  requestModuleAiGeneration,
  getAiGenerationLog,
  updateModuleSubTopic,
  updateAdminModule,
  createCourseModule,
  createModuleSubTopic,
  deleteAdminSubTopic,
  listAiGenerationLogs,
  requestCourseOutlineGeneration,
  deleteAdminModule,
  type AiGenerationLogDto,
  type AdminCourseDetailDto,
  type AdminDocumentSourceDto,
  type AdminModuleDto,
  type AdminSubTopicDto,
  type DocumentPreviewDto,
  type InteractionType,
} from '@/features/courses/services/adminCourses'
import type { CourseStatus } from '@/types/types'

const COURSE_COVER_STORAGE_KEY = 'lifeosCourseCovers'

const route  = useRoute()
const router = useRouter()

const courseId   = computed(() => String(route.params.id ?? ''))
const course     = ref<AdminCourseDetailDto | null>(null)
const loading    = ref(false)
const loadError  = ref('')

// ── PDF / AI (logic unchanged) ────────────────────────────────────────────
const selectedDocumentId = ref<number | null>(null)
const pageStart          = ref(1)
const pageEnd            = ref(10)
const preview            = ref<DocumentPreviewDto | null>(null)
const previewLoading     = ref(false)
const previewError       = ref('')
const selectedModuleId   = ref<number | null>(null)
const aiRequirements     = ref('')
const aiSubmitting       = ref(false)
const aiPolling          = ref(false)
const aiMessage          = ref('')
const outlineGenerationLog = ref<AiGenerationLogDto | null>(null)
const pollingOutline       = ref(false)

// ── Layout state ──────────────────────────────────────────────────────────
const outlineOpen  = ref(true)
const aiPanelOpen  = ref(true)
const manageOpen   = ref(false)
const pdfOpen      = ref(false)

// ── Centre state ──────────────────────────────────────────────────────────
const selectedSubTopicId  = ref<number | null>(null)
const editingInCentre     = ref(false)
const centreSaving        = ref(false)
const centreMessage       = ref('')

// ── Sidebar structure management state ────────────────────────────────────
const newModuleTitle = ref('')
const moduleCreating = ref(false)
const moduleError = ref('')

const addingSubTopicModuleId = ref<number | null>(null)
const newSubTopicTitle = ref('')
const subTopicCreating = ref(false)

const showDeleteConfirm = ref(false)
const deleteType = ref<'module' | 'subtopic' | null>(null)
const deleteTargetId = ref<number | null>(null)
const deleteTargetTitle = ref('')
const deletingInProgress = ref(false)

// ── Module inline edit state ───────────────────────────────────────────────
const editingModuleId = ref<number | null>(null)
const editingModuleTitle = ref('')
const moduleUpdating = ref(false)
const moduleUpdateError = ref('')

// ── Computed ──────────────────────────────────────────────────────────────
const cover = computed(() => {
  if (!course.value) return getCoverPreset(DEFAULT_COVER_ID)
  return getCoverPreset(readCourseCover(course.value.id))
})

const uiStatus = computed<CourseStatus>(() => {
  const status = course.value?.status
  if (status === 'PUBLISHED') return 'published'
  if (status === 'PENDING_REVIEW') return 'pending'
  if (status === 'NEED_REVISION') return 'revision'
  return 'draft'
})

const documents       = computed(() => course.value?.documentSources ?? [])
const modules         = computed(() => course.value?.modules ?? [])
const selectedDocument = computed(() => documents.value.find(d => d.id === selectedDocumentId.value) ?? null)

const allSubTopicsFlat = computed(() =>
  modules.value.flatMap(m => m.subTopics.map(t => ({ ...t, moduleTitle: m.title, moduleId: m.id })))
)
const selectedSubTopic = computed(() =>
  allSubTopicsFlat.value.find(t => t.id === selectedSubTopicId.value) ?? null
)

const interactiveServerFeedback = ref('')

const interactiveConfig = computed(() => {
  const current = selectedSubTopic.value
  if (!current) return null
  return parseInteractiveConfig(current.interactionType, current.interactionConfig)
})

const currentInteractiveMode = computed(() => {
  if (selectedSubTopic.value?.interactionType === 'QUIZ') return 'PRACTICE'
  return interactiveConfig.value?.mode ?? 'VISUALIZATION'
})

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

function handleInteractiveStarted() {
  console.log('Interactive started in admin preview mode')
}

function handleInteractiveChecked(payload: { passed: boolean }) {
  const config = interactiveConfig.value
  if (config?.type === 'LOGIC_FLOW') {
    interactiveServerFeedback.value = payload.passed
      ? config.feedback?.success || 'Correct.'
      : config.feedback?.failure || 'Not quite. Try again.'
  }
}

// ── Auto-select first subtopic on load ───────────────────────────────────
watch(modules, (mods) => {
  if (!selectedModuleId.value && mods.length) {
    selectedModuleId.value = mods[0].id
  }
  if (!selectedSubTopicId.value) {
    const first = mods[0]?.subTopics[0]
    if (first) selectedSubTopicId.value = first.id
  }
}, { immediate: true })

onMounted(async () => {
  await loadCourse()
  await checkActiveOutlineGeneration()
})

// ── Data loading (unchanged) ──────────────────────────────────────────────
async function loadCourse() {
  loading.value  = true
  loadError.value = ''
  try {
    const data = await getAdminCourse(courseId.value)
    course.value = data
    selectedDocumentId.value = data.documentSources[0]?.id ?? null
    if (data.status === 'NEED_REVISION') {
      manageOpen.value = true
    }
    if (!selectedSubTopicId.value) {
      const first = data.modules[0]?.subTopics[0]
      if (first) selectedSubTopicId.value = first.id
    }
  } catch (error) {
    loadError.value = getErrorMessage(error, 'Unable to load course.')
  } finally {
    loading.value = false
  }
}

// ── Outline nav ───────────────────────────────────────────────────────────
function selectSubTopic(id: number) {
  selectedSubTopicId.value = id
  editingInCentre.value    = false
  centreMessage.value      = ''
  interactiveServerFeedback.value = ''
}

// ── Inline outline structure management methods ───────────────────────────
async function addModuleInline() {
  const title = newModuleTitle.value.trim()
  if (!title) return
  moduleCreating.value = true
  moduleError.value = ''
  try {
    const newMod = await createCourseModule(courseId.value, {
      title,
      sortOrder: modules.value.length,
      contentDepth: 'MEDIUM',
    })
    await loadCourse()
    selectedModuleId.value = newMod.id
    newModuleTitle.value = ''
  } catch (error) {
    moduleError.value = getErrorMessage(error, 'Unable to create module.')
  } finally {
    moduleCreating.value = false
  }
}

async function addSubTopicInline(moduleId: number) {
  const title = newSubTopicTitle.value.trim()
  if (!title) return
  subTopicCreating.value = true
  try {
    const module = modules.value.find(m => m.id === moduleId)
    const sortOrder = module ? module.subTopics.length : 0
    const subTopic = await createModuleSubTopic(moduleId, {
      title,
      content: '',
      sortOrder,
      pageStart: selectedDocument.value ? pageStart.value : null,
      pageEnd: selectedDocument.value ? pageEnd.value : null,
    })
    await loadCourse()
    addingSubTopicModuleId.value = null
    newSubTopicTitle.value = ''
    selectSubTopic(subTopic.id)
    editingInCentre.value = true
  } catch (error) {
    console.error(error)
  } finally {
    subTopicCreating.value = false
  }
}

function triggerDeleteConfirm(type: 'module' | 'subtopic', id: number, title: string) {
  deleteType.value = type
  deleteTargetId.value = id
  deleteTargetTitle.value = title
  showDeleteConfirm.value = true
}

async function proceedDelete() {
  if (!deleteTargetId.value || deletingInProgress.value) return
  deletingInProgress.value = true
  try {
    if (deleteType.value === 'module') {
      await deleteAdminModule(deleteTargetId.value)
      if (selectedSubTopic.value && selectedSubTopic.value.moduleId === deleteTargetId.value) {
        selectedSubTopicId.value = null
      }
      if (selectedModuleId.value === deleteTargetId.value) {
        selectedModuleId.value = null
      }
    } else if (deleteType.value === 'subtopic') {
      await deleteAdminSubTopic(deleteTargetId.value)
      if (selectedSubTopicId.value === deleteTargetId.value) {
        selectedSubTopicId.value = null
      }
    }
    await loadCourse()
    showDeleteConfirm.value = false
  } catch (error) {
    console.error(error)
  } finally {
    deletingInProgress.value = false
  }
}

function startEditModule(module: AdminModuleDto) {
  editingModuleId.value = module.id
  editingModuleTitle.value = module.title
  moduleUpdateError.value = ''
}

function cancelEditModule() {
  editingModuleId.value = null
  moduleUpdateError.value = ''
}

async function saveEditModule(module: AdminModuleDto) {
  const title = editingModuleTitle.value.trim()
  if (!title) return
  moduleUpdating.value = true
  moduleUpdateError.value = ''
  try {
    await updateAdminModule(module.id, {
      title,
      description: module.description,
      sortOrder: module.sortOrder,
      contentDepth: module.contentDepth as 'LOW' | 'MEDIUM' | 'HIGH',
    })
    await loadCourse()
    editingModuleId.value = null
  } catch (error) {
    moduleUpdateError.value = getErrorMessage(error, 'Unable to update module.')
  } finally {
    moduleUpdating.value = false
  }
}

watch(showDeleteConfirm, (open) => {
  if (open) {
    window.addEventListener('keydown', handleGlobalKeydown)
  } else {
    window.removeEventListener('keydown', handleGlobalKeydown)
  }
})

function handleGlobalKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape') {
    showDeleteConfirm.value = false
  }
}

// ── Centre editor save ────────────────────────────────────────────────────
async function saveFromCentre(subTopic: AdminSubTopicDto, payload: {
  title: string
  content: string
  mascotPrompt: string | null
  interactionType: InteractionType
  interactionPrompt: string | null
  interactionConfig: string | null
}) {
  centreSaving.value  = true
  centreMessage.value = ''
  try {
    const updated = await updateModuleSubTopic(subTopic.id, {
      title:             payload.title,
      content:           payload.content,
      mascotPrompt:      payload.mascotPrompt,
      sortOrder:         subTopic.sortOrder,
      pageStart:         subTopic.pageStart,
      pageEnd:           subTopic.pageEnd,
      interactionType:   payload.interactionType,
      interactionPrompt: payload.interactionPrompt,
      interactionConfig: payload.interactionConfig,
    })
    if (course.value) {
      course.value = {
        ...course.value,
        modules: course.value.modules.map(m => ({
          ...m,
          subTopics: m.subTopics.map(t => t.id === updated.id ? updated : t),
        })),
      }
    }
    selectedSubTopicId.value = updated.id
    editingInCentre.value    = false
    centreMessage.value      = 'Saved.'
  } catch (error) {
    centreMessage.value = getErrorMessage(error, 'Unable to save subtopic.')
  } finally {
    centreSaving.value = false
  }
}

// ── PDF / AI functions (unchanged) ───────────────────────────────────────
async function loadPreview() {
  if (!selectedDocumentId.value) return
  previewLoading.value = true
  previewError.value   = ''
  preview.value        = null
  try {
    preview.value = await previewCourseDocument(selectedDocumentId.value, pageStart.value, pageEnd.value)
  } catch (error) {
    previewError.value = getErrorMessage(error, 'Unable to preview PDF.')
  } finally {
    previewLoading.value = false
  }
}

async function generateFromSelection() {
  if (!selectedModuleId.value || !selectedDocument.value) return
  aiSubmitting.value = true
  aiMessage.value    = ''
  try {
    const result = await requestModuleAiGeneration({
      moduleId:          selectedModuleId.value,
      documentSourceId:  selectedDocument.value.id,
      pageStart:         pageStart.value,
      pageEnd:           pageEnd.value,
      requirements:      aiRequirements.value.trim() || 'Generate concise learner-facing subtopics from the selected source pages.',
    })
    aiMessage.value = `AI generation queued. Log #${result.log.id} is ${result.log.status.toLowerCase()}.`
    await pollAiGeneration(result.log.id)
  } catch (error) {
    aiMessage.value = getErrorMessage(error, 'Unable to queue AI generation.')
  } finally {
    aiSubmitting.value = false
  }
}

async function pollAiGeneration(logId: number) {
  aiPolling.value = true
  try {
    for (let attempt = 0; attempt < 30; attempt += 1) {
      await wait(1500)
      const log = await getAiGenerationLog(logId)
      if (log.status === 'SUCCESS') {
        await loadCourse()
        aiMessage.value = `AI generation completed. Log #${log.id} created draft subtopics.`
        return
      }
      if (log.status === 'FAILED') {
        aiMessage.value = log.errorMessage || `AI generation failed. Log #${log.id}.`
        return
      }
      aiMessage.value = `AI generation ${log.status.toLowerCase()}... Log #${log.id}.`
    }
    aiMessage.value = 'AI generation is still running. Refresh this page in a moment.'
  } finally {
    aiPolling.value = false
  }
}

async function checkActiveOutlineGeneration() {
  try {
    const logs = await listAiGenerationLogs(courseId.value)
    const activeLog = logs.find(
      (log) =>
        log.type === 'COURSE_OUTLINE' &&
        (log.status === 'PENDING' || log.status === 'RUNNING'),
    )
    if (activeLog) {
      outlineGenerationLog.value = activeLog
      pollOutlineGeneration(activeLog.id)
    } else {
      outlineGenerationLog.value = null
    }
  } catch (error) {
    console.error('Error checking outline generation logs:', error)
  }
}

async function pollOutlineGeneration(logId: number) {
  pollingOutline.value = true
  try {
    for (let attempt = 0; attempt < 60; attempt += 1) {
      await wait(1500)
      const log = await getAiGenerationLog(logId)
      outlineGenerationLog.value = log
      if (log.status === 'SUCCESS') {
        await loadCourse()
        outlineGenerationLog.value = null
        return
      }
      if (log.status === 'FAILED') {
        return
      }
    }
    if (
      outlineGenerationLog.value &&
      (outlineGenerationLog.value.status === 'PENDING' ||
        outlineGenerationLog.value.status === 'RUNNING')
    ) {
      outlineGenerationLog.value.status = 'FAILED'
      outlineGenerationLog.value.errorMessage =
        'AI outline generation timed out. Please refresh or try again.'
    }
  } catch (error) {
    console.error('Error polling outline generation:', error)
  } finally {
    pollingOutline.value = false
  }
}

const retryingOutlineGen = ref(false)

const isAiBusyError = computed(() => {
  const msg = outlineGenerationLog.value?.errorMessage?.toLowerCase() || ''
  return msg.includes('429') ||
         msg.includes('503') ||
         msg.includes('resource_exhausted') ||
         msg.includes('quota') ||
         msg.includes('rate limit') ||
         msg.includes('too many requests') ||
         msg.includes('service unavailable') ||
         msg.includes('overloaded') ||
         msg.includes('busy')
})

async function retryOutlineGeneration() {
  if (!outlineGenerationLog.value) return
  retryingOutlineGen.value = true
  try {
    const prev = outlineGenerationLog.value
    const result = await requestCourseOutlineGeneration({
      courseId: courseId.value,
      documentSourceId: prev.documentSourceId,
      prompt: prev.requirements || prev.prompt,
      pageStart: prev.pageStart,
      pageEnd: prev.pageEnd,
    })
    outlineGenerationLog.value = result
    pollOutlineGeneration(result.id)
  } catch (error) {
    outlineGenerationLog.value = {
      ...(outlineGenerationLog.value || {}),
      status: 'FAILED',
      errorMessage: getErrorMessage(error, 'Unable to restart AI generation.'),
    } as AiGenerationLogDto
  } finally {
    retryingOutlineGen.value = false
  }
}

function selectDocument(document: AdminDocumentSourceDto) {
  selectedDocumentId.value = document.id
  preview.value            = null
  previewError.value       = ''
  pageStart.value          = 1
  pageEnd.value            = Math.min(10, document.pageCount ?? 1)
}

function clampPageRange() {
  const max = selectedDocument.value?.pageCount ?? 1
  pageStart.value = Math.min(Math.max(1, pageStart.value), max)
  pageEnd.value   = Math.min(Math.max(pageStart.value, pageEnd.value), max)
}



function wait(ms: number) { return new Promise(resolve => window.setTimeout(resolve, ms)) }

function formatFileSize(bytes: number) {
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

function lessonHtml(subTopic: AdminSubTopicDto) {
  if (subTopic.contentHtml) return subTopic.contentHtml
  return textToHtml(subTopic.content || '')
}

function textToHtml(value: string) {
  return value.split(/\n{2,}/).map(p => p.trim()).filter(Boolean).map(p => `<p>${escapeHtml(p).replace(/\n/g, '<br>')}</p>`).join('')
}

function escapeHtml(value: string) {
  return value.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
}

function readCourseCover(courseId: number) {
  const raw = localStorage.getItem(COURSE_COVER_STORAGE_KEY)
  if (!raw) return DEFAULT_COVER_ID
  try {
    const covers = JSON.parse(raw) as Record<string, string>
    return covers[String(courseId)] ?? DEFAULT_COVER_ID
  } catch {
    return DEFAULT_COVER_ID
  }
}

function getErrorMessage(error: unknown, fallback: string) {
  if (error instanceof AxiosError) {
    const data = error.response?.data as { error?: string; message?: string } | undefined
    return data?.error ?? data?.message ?? fallback
  }
  if (error instanceof Error) return error.message
  return fallback
}
</script>

<template>
  <div class="course-app flex h-screen w-full overflow-hidden bg-lm-bg">
    <AdminNavbar active-item="course" />

    <div class="flex min-w-0 flex-1 flex-col overflow-hidden">

      <header class="flex shrink-0 items-center gap-3.5 border-b-2 border-lm-line bg-lm-surface px-5 py-3">
        <div class="flex min-w-0 flex-1 items-center gap-3.5">
          <div class="flex min-w-0 items-center gap-4">
            <button
              type="button"
              class="inline-flex shrink-0 cursor-pointer items-center gap-2 rounded-full border-2 border-lm-line bg-lm-bg-soft px-3.5 py-1.5 font-display text-[12px] font-semibold text-lm-ink shadow-stamp-sm"
              @click="router.push('/admin/courses')"
            >
              <AdminIcon name="back" :size="13" />
              All Courses
            </button>

            <div
              v-if="course"
              class="grid h-[38px] w-[38px] shrink-0 place-items-center rounded-[10px] border-2 border-lm-line font-math text-[18px] font-bold italic shadow-stamp-sm"
              :class="[cover.bgClass, cover.textClass]"
            >
              {{ cover.symbol }}
            </div>

            <div class="min-w-0">
              <div class="flex min-w-0 items-center gap-2">
                <h1 class="m-0 truncate font-display text-[18px] font-bold text-lm-ink">{{ course?.title ?? 'Course' }}</h1>
                <StatusBadge v-if="course" :status="uiStatus" />
              </div>
              <MonoLabel v-if="course?.description" class="mt-[1px] truncate">{{ course.description }}</MonoLabel>
            </div>
          </div>
        </div>
      </header>

      <!-- Error banner -->
      <div v-if="loadError" class="shrink-0 flex items-center justify-between border-b-2 border-lm-red bg-lm-red-soft px-6 py-2.5 text-[12px] font-medium text-lm-red">
        {{ loadError }}
        <button type="button" class="font-bold hover:opacity-70" @click="loadCourse">Retry</button>
      </div>

      <!-- Loading -->
      <div v-if="loading" class="flex flex-1 items-center justify-center">
        <p class="font-mono text-[12px] text-lm-ink-3">Loading course...</p>
      </div>

      <!-- ── 3-column body ─────────────────────────────────────────── -->
      <div v-else-if="course" class="flex flex-1 min-h-0 overflow-hidden">

        <!-- LEFT: outline nav ─────────────────────────────────────── -->
        <aside
          class="relative flex shrink-0 flex-col border-r-2 border-lm-line bg-lm-surface overflow-hidden transition-[width] duration-200 ease-out"
          :style="{ width: outlineOpen ? '292px' : '44px' }"
        >
          <!-- Panel header -->
          <div
            class="flex h-11 shrink-0 items-center border-b-2 border-lm-line"
            :class="outlineOpen ? 'justify-between px-4' : 'justify-center'"
          >
            <div v-if="outlineOpen" class="flex items-center gap-2">
              <svg class="h-3.5 w-3.5 text-lm-ink-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="8" y1="6" x2="21" y2="6" /><line x1="8" y1="12" x2="21" y2="12" /><line x1="8" y1="18" x2="21" y2="18" />
                <line x1="3" y1="6" x2="3.01" y2="6" /><line x1="3" y1="12" x2="3.01" y2="12" /><line x1="3" y1="18" x2="3.01" y2="18" />
              </svg>
              <span class="font-mono text-[11px] font-bold uppercase tracking-[0.22em] text-lm-ink-3">Outline</span>
            </div>
            <button
              type="button"
              class="flex h-7 w-7 shrink-0 items-center justify-center rounded-[8px] border-2 border-lm-line bg-lm-bg text-lm-ink shadow-stamp-xs transition-all duration-200 hover:-translate-y-px hover:shadow-stamp-sm"
              :title="outlineOpen ? 'Collapse outline' : 'Expand outline'"
              @click="outlineOpen = !outlineOpen"
            >
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                <path v-if="outlineOpen" d="M11 19l-7-7 7-7M18 19l-7-7 7-7" />
                <path v-else d="M13 5l7 7-7 7M6 5l7 7-7 7" />
              </svg>
            </button>
          </div>

          <!-- Expanded content -->
          <div v-if="outlineOpen" class="flex flex-1 flex-col overflow-hidden">

            <!-- Scrollable module list -->
            <div class="flex-1 overflow-y-auto">

              <!-- Empty state -->
              <div v-if="!modules.length" class="flex flex-col items-center justify-center px-5 py-14 text-center">
                <div class="mb-3 flex h-10 w-10 items-center justify-center rounded-[12px] border-2 border-dashed border-lm-line-soft bg-lm-bg">
                  <svg class="h-4 w-4 text-lm-ink-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M3 6h18M3 12h18M3 18h18" />
                  </svg>
                </div>
                <p class="font-mono text-[10px] uppercase tracking-[0.06em] text-lm-ink-3">No modules yet</p>
              </div>

              <div v-for="(module, moduleIndex) in modules" :key="module.id">

                <!-- ── MODULE SECTION HEADER — full-bleed, clearly a parent ── -->
                <div
                  class="group relative flex items-center gap-2.5 border-b border-lm-line-soft bg-lm-bg px-3 py-2.5"
                  :class="moduleIndex > 0 ? 'border-t border-lm-line-soft' : ''"
                >
                  <!-- Left accent bar -->
                  <div class="absolute left-0 top-0 h-full w-[3px] bg-lm-ink" />

                  <!-- Filled number badge — heavier than any subtopic element -->
                  <span class="ml-1 flex h-[18px] w-[18px] shrink-0 items-center justify-center rounded-[4px] bg-lm-ink font-mono text-[9px] font-bold text-lm-bg">
                    {{ String(moduleIndex + 1).padStart(2, '0') }}
                  </span>

                  <!-- Inline edit form OR static title -->
                  <template v-if="editingModuleId === module.id">
                    <input
                      v-model="editingModuleTitle"
                      type="text"
                      class="h-7 min-w-0 flex-1 rounded-[6px] border-2 border-lm-line bg-white px-2 font-display text-[12px] font-bold text-lm-ink outline-none focus:border-lm-ink"
                      @keydown.enter="saveEditModule(module)"
                      @keydown.escape="cancelEditModule"
                    />
                    <div class="flex shrink-0 items-center gap-1">
                      <button
                        type="button"
                        class="flex h-6 w-6 items-center justify-center rounded-[6px] border-2 border-lm-ink bg-lm-ink text-lm-bg transition hover:opacity-80 disabled:opacity-50"
                        :disabled="moduleUpdating || !editingModuleTitle.trim()"
                        title="Save"
                        @click="saveEditModule(module)"
                      >
                        <svg class="h-3 w-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                          <polyline points="20 6 9 17 4 12" />
                        </svg>
                      </button>
                      <button
                        type="button"
                        class="flex h-6 w-6 items-center justify-center rounded-[6px] border-2 border-lm-line-soft bg-lm-surface text-lm-ink-3 transition hover:border-lm-line hover:text-lm-ink"
                        title="Cancel"
                        @click="cancelEditModule"
                      >
                        <svg class="h-3 w-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                          <path d="M18 6 6 18M6 6l12 12" />
                        </svg>
                      </button>
                    </div>
                  </template>

                  <template v-else>
                    <span class="min-w-0 flex-1 truncate font-display text-[12px] font-extrabold tracking-tight text-lm-ink">
                      {{ module.title }}
                    </span>
                    <!-- Module action buttons -->
                    <div
                      v-if="course.status !== 'PENDING_REVIEW'"
                      class="flex shrink-0 items-center gap-1"
                    >
                      <button
                        type="button"
                        class="flex h-6 w-6 items-center justify-center rounded-[6px] border-2 border-lm-line-soft bg-lm-surface text-lm-ink-3 shadow-stamp-xs transition-all duration-150 hover:-translate-y-px hover:border-lm-line hover:text-lm-ink hover:shadow-stamp-sm"
                        title="Edit module title"
                        @click.stop="startEditModule(module)"
                      >
                        <svg class="h-3 w-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                          <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
                          <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
                        </svg>
                      </button>
                      <button
                        type="button"
                        class="flex h-6 w-6 items-center justify-center rounded-[6px] border-2 border-lm-line-soft bg-lm-surface text-lm-ink-3 shadow-stamp-xs transition-all duration-150 hover:-translate-y-px hover:border-lm-red hover:text-lm-red hover:shadow-stamp-sm"
                        title="Delete module"
                        @click.stop="triggerDeleteConfirm('module', module.id, module.title)"
                      >
                        <svg class="h-3 w-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                          <path d="M3 6h18" /><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
                        </svg>
                      </button>
                    </div>
                  </template>
                </div>

                <!-- Module update error -->
                <p v-if="editingModuleId === module.id && moduleUpdateError" class="bg-lm-bg px-4 pb-1.5 pt-1 font-mono text-[10px] text-lm-red">{{ moduleUpdateError }}</p>

                <!-- ── SUBTOPIC TREE — indented child zone ── -->
                <div class="relative bg-lm-surface py-1.5 pl-5 pr-2">
                  <!-- Vertical tree trunk -->
                  <div class="pointer-events-none absolute bottom-2 left-[22px] top-0 w-px bg-lm-line-soft" />

                  <!-- Subtopic rows -->
                  <div class="space-y-px">
                    <div
                      v-for="subTopic in module.subTopics"
                      :key="subTopic.id"
                      class="group/st relative flex min-h-[34px] items-center gap-2 rounded-[7px] py-1.5 pl-3 pr-2 transition-all duration-150"
                      :class="selectedSubTopicId === subTopic.id
                        ? 'bg-lm-yellow shadow-stamp-xs ring-[1.5px] ring-lm-line'
                        : 'hover:bg-lm-bg'"
                    >
                      <!-- Horizontal tree branch -->
                      <div class="pointer-events-none absolute -left-[3px] top-1/2 h-px w-3 -translate-y-1/2 bg-lm-line-soft" />

                      <button
                        type="button"
                        class="flex min-w-0 flex-1 items-center gap-2 text-left"
                        @click="selectSubTopic(subTopic.id)"
                      >
                        <span
                          class="min-w-0 flex-1 truncate font-display text-[12px]"
                          :class="selectedSubTopicId === subTopic.id ? 'font-bold text-lm-ink' : 'font-medium text-lm-ink-2'"
                        >
                          {{ subTopic.title }}
                        </span>
                        <ITypeBadge :type="subTopic.interactionType" class="shrink-0" />
                      </button>

                      <!-- Delete subtopic — hover-reveal -->
                      <button
                        v-if="course.status !== 'PENDING_REVIEW'"
                        type="button"
                        class="hidden h-5 w-5 shrink-0 items-center justify-center rounded-[5px] border-[1.5px] border-lm-line-soft bg-lm-surface text-lm-ink-3 transition-all duration-150 hover:border-lm-red hover:text-lm-red group-hover/st:flex"
                        title="Delete subtopic"
                        @click.stop="triggerDeleteConfirm('subtopic', subTopic.id, subTopic.title)"
                      >
                        <svg class="h-3 w-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                          <path d="M3 6h18" /><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
                        </svg>
                      </button>
                    </div>
                  </div>

                  <!-- Add subtopic row -->
                  <div v-if="course.status !== 'PENDING_REVIEW'" class="relative mt-px">
                    <div class="pointer-events-none absolute -left-[3px] top-1/2 h-px w-3 -translate-y-1/2 bg-lm-line-soft" />
                    <button
                      v-if="addingSubTopicModuleId !== module.id"
                      type="button"
                      class="flex w-full items-center gap-1.5 rounded-[6px] py-1.5 pl-3 pr-2 font-mono text-[10px] font-bold text-lm-ink-3 transition-all duration-150 hover:bg-lm-bg hover:text-lm-ink"
                      @click="addingSubTopicModuleId = module.id; newSubTopicTitle = ''"
                    >
                      <svg class="h-3 w-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                        <line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" />
                      </svg>
                      Add subtopic
                    </button>
                    <div v-else class="flex items-center gap-1.5 rounded-[8px] border-2 border-lm-line bg-white p-1.5">
                      <input
                        v-model="newSubTopicTitle"
                        type="text"
                        placeholder="Subtopic title"
                        class="h-7 min-w-0 flex-1 bg-transparent px-1 font-display text-[12px] text-lm-ink outline-none placeholder:text-lm-ink-3"
                        @keydown.enter="addSubTopicInline(module.id)"
                        @keydown.escape="addingSubTopicModuleId = null"
                      />
                      <div class="flex shrink-0 items-center gap-1">
                        <button
                          type="button"
                          class="h-7 rounded-[5px] border-2 border-lm-ink bg-lm-ink px-2 font-mono text-[10px] font-bold text-lm-bg transition hover:opacity-80 disabled:opacity-50"
                          :disabled="subTopicCreating || !newSubTopicTitle.trim()"
                          @click="addSubTopicInline(module.id)"
                        >Save</button>
                        <button
                          type="button"
                          class="h-7 rounded-[5px] border-2 border-lm-line-soft px-2 font-mono text-[10px] font-bold text-lm-ink-2 transition hover:bg-lm-bg"
                          @click="addingSubTopicModuleId = null"
                        >×</button>
                      </div>
                    </div>
                  </div>
                </div>

              </div>
            </div>

            <!-- Add module — pinned to the bottom -->
            <div v-if="course.status !== 'PENDING_REVIEW'" class="shrink-0 border-t-2 border-lm-line bg-lm-surface px-3 py-3">
              <p class="mb-2 font-mono text-[9px] font-bold uppercase tracking-[0.14em] text-lm-ink-3">New Module</p>
              <div class="flex items-center gap-1.5">
                <input
                  v-model="newModuleTitle"
                  type="text"
                  placeholder="Module title…"
                  class="h-8 min-w-0 flex-1 rounded-[7px] border-2 border-lm-line-soft bg-lm-bg px-2.5 font-display text-[12px] text-lm-ink outline-none placeholder:text-lm-ink-3 focus:border-lm-line"
                  @keydown.enter="addModuleInline"
                />
                <button
                  type="button"
                  class="flex h-8 shrink-0 items-center gap-1 rounded-[7px] border-2 border-lm-ink bg-lm-ink px-2.5 font-mono text-[10px] font-bold text-lm-bg shadow-stamp-xs transition hover:opacity-80 disabled:opacity-50"
                  :disabled="moduleCreating || !newModuleTitle.trim()"
                  @click="addModuleInline"
                >
                  <svg class="h-3 w-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                    <line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" />
                  </svg>
                  {{ moduleCreating ? '…' : 'Add' }}
                </button>
              </div>
              <p v-if="moduleError" class="mt-1.5 font-mono text-[10px] text-lm-red">{{ moduleError }}</p>
            </div>
          </div>

          <!-- Collapsed: dot indicators -->
          <div v-else class="flex flex-1 flex-col items-center gap-1.5 overflow-y-auto pt-3">
            <button
              v-for="subTopic in allSubTopicsFlat"
              :key="subTopic.id"
              type="button"
              :title="subTopic.title"
              class="h-4 w-4 shrink-0 rounded-full border-2 transition-all duration-150"
              :class="selectedSubTopicId === subTopic.id
                ? 'border-lm-line bg-lm-yellow'
                : 'border-lm-line-soft bg-transparent hover:border-lm-line'"
              @click="selectSubTopic(subTopic.id)"
            />
          </div>
        </aside>

        <!-- CENTRE: subtopic viewer + manage section ──────────────── -->
        <section class="relative flex flex-1 min-w-0 flex-col overflow-y-auto" style="background:#fffdf8">
          <div class="pointer-events-none absolute inset-0 bg-dot-grid opacity-40" />

          <!-- AI Outline Generation Status Container -->
          <div v-if="outlineGenerationLog" class="relative flex flex-1 items-center justify-center p-7 z-10">
            <div class="w-full max-w-lg rounded-[18px] border-2 border-lm-line bg-white p-8 shadow-stamp-md text-center">
              
              <!-- State: PENDING or RUNNING -->
              <div v-if="outlineGenerationLog.status === 'PENDING' || outlineGenerationLog.status === 'RUNNING'">
                <!-- Pulsing AI Brain/Spark Icon -->
                <div class="mx-auto mb-6 flex h-16 w-16 items-center justify-center rounded-[20px] border-2 border-lm-line bg-lm-yellow shadow-stamp-sm animate-bounce">
                  <svg class="h-8 w-8 text-lm-ink animate-pulse" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="m12 3-1.912 5.813a2 2 0 0 1-1.275 1.275L3 12l5.813 1.912a2 2 0 0 1 1.275 1.275L12 21l1.912-5.813a2 2 0 0 1 1.275-1.275L21 12l-5.813-1.912a2 2 0 0 1-1.275-1.275L12 3z" />
                  </svg>
                </div>
                
                <h3 class="font-display text-[20px] font-extrabold text-lm-ink">AI Course Generation in Progress</h3>
                <p class="mt-2 font-mono text-[11px] uppercase tracking-[0.1em] text-lm-ink-3">
                  Status: <span class="rounded bg-lm-yellow-soft px-1.5 py-0.5 font-bold text-lm-ink">{{ outlineGenerationLog.status }}</span>
                </p>

                <!-- Animated loading bar -->
                <div class="my-6 h-3 w-full overflow-hidden rounded-full border border-lm-line bg-lm-bg-soft relative">
                  <div class="h-full bg-lm-yellow w-1/2 rounded-full absolute top-0 left-0 animate-loading-bar"></div>
                </div>

                <div class="space-y-3.5 text-left border-t border-lm-line-soft pt-5">
                  <div>
                    <span class="block font-mono text-[10px] font-bold uppercase tracking-[0.08em] text-lm-ink-3">Prompt / Requirements</span>
                    <p class="mt-1 text-[12px] text-lm-ink-2 line-clamp-3 italic">"{{ outlineGenerationLog.prompt || outlineGenerationLog.requirements }}"</p>
                  </div>
                  <div class="flex justify-between text-[11px]">
                    <div>
                      <span class="block font-mono text-[9px] font-bold uppercase tracking-[0.08em] text-lm-ink-3">Pages analyzed</span>
                      <span class="font-semibold text-lm-ink">{{ outlineGenerationLog.pageStart }} - {{ outlineGenerationLog.pageEnd }}</span>
                    </div>
                    <div>
                      <span class="block font-mono text-[9px] font-bold uppercase tracking-[0.08em] text-lm-ink-3">Log Reference</span>
                      <span class="font-mono text-lm-ink">#{{ outlineGenerationLog.id }}</span>
                    </div>
                  </div>
                </div>
              </div>

              <!-- State: FAILED -->
              <div v-else-if="outlineGenerationLog.status === 'FAILED'">
                <div class="mx-auto mb-6 flex h-16 w-16 items-center justify-center rounded-[20px] border-2 border-lm-line bg-lm-red-soft text-lm-red shadow-stamp-sm">
                  <svg class="h-8 w-8" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
                  </svg>
                </div>
                
                <h3 class="font-display text-[20px] font-extrabold text-lm-ink">AI Course Outline Generation Failed</h3>
                <p class="mt-1.5 text-[13px] text-lm-red font-medium leading-relaxed mb-4">{{ outlineGenerationLog.errorMessage || 'An unknown error occurred during generation.' }}</p>
                
                <!-- Busy/Rate-limited special prompt -->
                <div v-if="isAiBusyError" class="mb-4 rounded-xl border-2 border-lm-rust bg-lm-rust-soft/50 p-4 text-left text-[12.5px] leading-relaxed text-lm-rust">
                  <span class="font-bold flex items-center gap-1">⚠️ บริการ AI กำลังหนาแน่น (Gemini is overloaded)</span>
                  <p class="mt-1 text-lm-ink-2 font-sans">ระบบ Google Gemini มีผู้ใช้งานจำนวนมากในขณะนี้ ทำให้เกินอัตราที่กำหนด (Rate Limit) กรุณารอสักครู่ (ประมาณ 1 นาที) แล้วกดปุ่ม <b>"Retry Gen Outline"</b> ด้านล่างเพื่อเริ่มสร้างอีกครั้งครับ</p>
                </div>

                <div class="mt-6 flex flex-wrap gap-2.5">
                  <button 
                    type="button" 
                    class="h-10 flex-1 min-w-[80px] rounded-full border-2 border-lm-line bg-lm-surface px-4 text-[13px] font-bold text-lm-ink shadow-stamp-sm transition hover:-translate-y-px hover:shadow-stamp-md"
                    @click="outlineGenerationLog = null"
                  >
                    Close
                  </button>
                  <button 
                    type="button" 
                    class="h-10 flex-1 min-w-[100px] rounded-full border-2 border-lm-line bg-lm-bg-soft px-4 text-[13px] font-bold text-lm-ink shadow-stamp-sm transition hover:-translate-y-px hover:shadow-stamp-md"
                    @click="checkActiveOutlineGeneration"
                  >
                    Retry Check
                  </button>
                  <button 
                    v-if="!retryingOutlineGen"
                    type="button" 
                    class="h-10 flex-1 min-w-[140px] rounded-full border-2 border-lm-line bg-lm-yellow px-4 text-[13px] font-bold text-lm-ink shadow-stamp-sm transition hover:-translate-y-px hover:shadow-stamp-md"
                    @click="retryOutlineGeneration"
                  >
                    Retry Gen Outline
                  </button>
                  <button 
                    v-else
                    disabled
                    type="button" 
                    class="h-10 flex-1 min-w-[140px] rounded-full border-2 border-lm-line bg-lm-yellow px-4 text-[13px] font-bold text-lm-ink opacity-50 cursor-not-allowed"
                  >
                    Retrying...
                  </button>
                </div>
              </div>

            </div>
          </div>

          <!-- Subtopic viewer / editor -->
          <div v-else-if="selectedSubTopic" class="relative mx-auto w-full max-w-[840px] px-7 py-8">

            <!-- View mode -->
            <template v-if="!editingInCentre">
              <div class="mb-5 flex items-start justify-between gap-4">
                <div>
                  <MonoLabel>{{ selectedSubTopic.moduleTitle }}</MonoLabel>
                  <h2 class="mt-2 font-display text-[30px] font-extrabold leading-tight text-lm-ink">
                    {{ selectedSubTopic.title }}
                  </h2>
                  <div class="mt-2 flex gap-1.5">
                    <ITypeBadge :type="selectedSubTopic.interactionType" />
                    <span v-if="selectedSubTopic.interactionType !== 'NONE'" class="font-mono text-[10px] text-lm-ink-3">Topic #{{ selectedSubTopic.id }}</span>
                  </div>
                </div>
                <button
                  v-if="course.status !== 'PENDING_REVIEW'"
                  type="button"
                  class="inline-flex h-10 shrink-0 items-center gap-2 rounded-full border-2 border-lm-line bg-lm-surface px-5 py-2.5 text-[14px] font-bold text-lm-ink shadow-stamp-sm transition-all duration-200 hover:-translate-y-px hover:shadow-md"
                  @click="editingInCentre = true; centreMessage = ''"
                >
                  <AdminIcon name="edit" :size="14" />
                  Edit
                </button>
              </div>

              <!-- Content HTML & Interactive Previews -->
              <div v-if="selectedSubTopic.interactionType === 'QUIZ'">
                <!-- Quiz layout -->
                <div
                  v-if="lessonHtml(selectedSubTopic)"
                  class="lesson-preview mb-6 rounded-[12px] border-2 border-lm-line bg-white px-7 py-6 text-[15.5px] leading-[1.75] text-[#3b3630] shadow-stamp-sm"
                  v-html="lessonHtml(selectedSubTopic)"
                />
                <div class="rounded-xl border-2 border-lm-line bg-white p-6 shadow-stamp-sm">
                  <div class="mb-4 flex items-center gap-2">
                    <div class="flex h-6 w-6 shrink-0 items-center justify-center rounded-[6px] border-2 border-lm-line bg-lm-yellow">
                      <AdminIcon name="spark" :size="13" />
                    </div>
                    <p class="font-mono text-[10px] font-bold uppercase tracking-[0.12em] text-lm-ink-3">
                      Quiz Preview (Learner View)
                    </p>
                  </div>
                  <InteractivePreview
                    :config="interactiveConfig"
                    :server-feedback="interactiveServerFeedback"
                    @started="handleInteractiveStarted"
                    @checked="handleInteractiveChecked"
                  />
                </div>
              </div>

              <div v-else>
                <!-- Standard lesson layout -->
                <div
                  v-if="lessonHtml(selectedSubTopic)"
                  class="lesson-preview rounded-[12px] border-2 border-lm-line bg-white px-7 py-6 text-[15.5px] leading-[1.75] text-[#3b3630] shadow-stamp-sm"
                  v-html="lessonHtml(selectedSubTopic)"
                />
                <div v-else class="rounded-[14px] border-2 border-dashed border-lm-line-soft py-14 text-center">
                  <p class="font-mono text-[11px] uppercase tracking-[0.06em] text-lm-ink-3">
                    No content yet — click Edit to add
                  </p>
                </div>

                <div v-if="selectedSubTopic.interactionType !== 'NONE'" class="mt-6">
                  <InteractiveChallengeShell
                    :interaction-type="selectedSubTopic.interactionType"
                    :objective="currentChallengeObjective"
                    status="NOT_STARTED"
                    :mode="currentInteractiveMode"
                  >
                    <InteractivePreview
                      :config="interactiveConfig"
                      :server-feedback="interactiveServerFeedback"
                      @started="handleInteractiveStarted"
                      @checked="handleInteractiveChecked"
                    />
                  </InteractiveChallengeShell>
                </div>
              </div>

              <p v-if="centreMessage" class="mt-3 text-[12px] font-medium" :class="centreMessage.includes('Unable') ? 'text-lm-red' : 'text-lm-green'">
                {{ centreMessage }}
              </p>
            </template>

            <!-- Edit mode -->
            <template v-else>
              <div class="mb-4 flex items-center gap-2">
                <MonoLabel>{{ selectedSubTopic.moduleTitle }}</MonoLabel>
                <span class="text-lm-line-soft">/</span>
                <span class="font-display text-[13px] font-semibold text-lm-ink">{{ selectedSubTopic.title }}</span>
              </div>
              <SubTopicLessonEditor
                :sub-topic="selectedSubTopic"
                @cancel="editingInCentre = false; centreMessage = ''"
                @error="centreMessage = $event"
                @save="saveFromCentre(selectedSubTopic, $event)"
              />
            </template>
          </div>

          <!-- No subtopic selected -->
          <div v-else class="relative flex flex-1 items-center justify-center py-24">
            <p class="font-mono text-[11px] uppercase tracking-[0.06em] text-lm-ink-3">Select a topic from the outline</p>
          </div>

          <!-- ── Reviewer Feedback (collapsible, always accessible) ── -->
          <div class="relative mx-auto w-full max-w-[740px] shrink-0 px-7 pb-10">
            <button
              type="button"
              class="flex w-full items-center gap-3 py-3"
              @click="manageOpen = !manageOpen"
            >
              <div class="flex-1 border-t-2 border-lm-line-soft" />
              <span class="font-mono text-[9px] font-bold uppercase tracking-[0.14em] text-lm-ink-3">Reviewer Feedback</span>
              <svg
                class="h-3 w-3 text-lm-ink-3 transition-transform duration-200"
                :class="manageOpen ? 'rotate-180' : ''"
                viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
              >
                <polyline points="6 9 12 15 18 9" />
              </svg>
              <div class="flex-1 border-t-2 border-lm-line-soft" />
            </button>

            <div v-if="manageOpen">
              <AdminCourseOutline
                :course-id="courseId"
                :course-status="course.status"
                :modules="modules"
                @reload="loadCourse"
              />
            </div>
          </div>
        </section>

        <!-- RIGHT: AI generation panel ────────────────────────────── -->
        <aside
          class="flex shrink-0 flex-col border-l-2 border-lm-line bg-lm-surface overflow-hidden transition-[width] duration-200 ease-out"
          :style="{ width: aiPanelOpen ? '332px' : '44px' }"
        >
          <!-- Panel header -->
          <div
            class="flex h-11 shrink-0 items-center border-b-2 border-lm-line-soft gap-2"
            :class="aiPanelOpen ? 'justify-between px-4' : 'justify-center'"
          >
            <div v-if="aiPanelOpen" class="flex items-center gap-2">
              <div class="flex h-6 w-6 shrink-0 items-center justify-center rounded-[6px] border-2 border-lm-line bg-lm-yellow">
                <svg class="h-3 w-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="m12 3-1.912 5.813a2 2 0 0 1-1.275 1.275L3 12l5.813 1.912a2 2 0 0 1 1.275 1.275L12 21l1.912-5.813a2 2 0 0 1 1.275-1.275L21 12l-5.813-1.912a2 2 0 0 1-1.275-1.275L12 3z" />
                </svg>
              </div>
              <p class="font-mono text-[11px] font-bold uppercase tracking-[0.24em] text-lm-ink-3">AI Generation</p>
            </div>
            <button
              type="button"
              class="flex h-7 w-7 shrink-0 items-center justify-center rounded-[8px] border-2 border-lm-line bg-lm-bg-soft text-lm-ink shadow-stamp-xs transition-all duration-200 hover:-translate-y-px hover:shadow-stamp-sm"
              :title="aiPanelOpen ? 'Collapse AI panel' : 'Expand AI panel'"
              @click="aiPanelOpen = !aiPanelOpen"
            >
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                <path v-if="aiPanelOpen" d="M13 5l7 7-7 7M6 5l7 7-7 7" />
                <path v-else d="M11 19l-7-7 7-7M18 19l-7-7 7-7" />
              </svg>
            </button>
          </div>

          <!-- Expanded panel content -->
          <div v-if="aiPanelOpen" class="flex flex-1 flex-col gap-4 overflow-y-auto p-5">

            <!-- Target module -->
            <div>
              <p class="mb-2 font-mono text-[11px] font-bold uppercase tracking-[0.22em] text-lm-ink-3">Target Module</p>
              <select
                v-if="modules.length"
                v-model="selectedModuleId"
                class="h-11 w-full rounded-lg border-2 border-lm-line-soft bg-lm-bg-soft px-3 text-[13px] text-lm-ink outline-none focus:border-lm-line"
              >
                <option v-for="module in modules" :key="module.id" :value="module.id">{{ module.title }}</option>
              </select>
              <div v-else class="rounded-lg border-2 border-lm-yellow bg-lm-yellow/20 px-3 py-2 text-[12px] text-lm-ink">
                Add a module in the sidebar first.
              </div>
            </div>

            <!-- Requirements -->
            <div>
              <p class="mb-2 font-mono text-[11px] font-bold uppercase tracking-[0.22em] text-lm-ink-3">Requirements</p>
              <textarea
                v-model="aiRequirements"
                rows="5"
                placeholder="Describe what to generate…"
                class="w-full resize-none rounded-lg border-2 border-lm-line-soft bg-lm-bg-soft px-3 py-3 text-[13px] text-lm-ink outline-none placeholder:text-lm-ink-3 focus:border-lm-line"
              />
            </div>

            <!-- Generate button -->
            <button
              type="button"
              class="flex h-11 w-full items-center justify-center gap-2 rounded-full border-2 border-lm-line bg-lm-yellow px-4 text-[14px] font-bold text-lm-ink shadow-stamp-sm transition-all duration-200 hover:-translate-y-px hover:shadow-stamp-md disabled:cursor-not-allowed disabled:opacity-50"
              :disabled="aiSubmitting || aiPolling || !selectedModuleId || !selectedDocument"
              @click="generateFromSelection"
            >
              <svg class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="m12 3-1.912 5.813a2 2 0 0 1-1.275 1.275L3 12l5.813 1.912a2 2 0 0 1 1.275 1.275L12 21l1.912-5.813a2 2 0 0 1 1.275-1.275L21 12l-5.813-1.912a2 2 0 0 1-1.275-1.275L12 3z" />
              </svg>
              {{ aiSubmitting || aiPolling ? 'Generating…' : 'Generate draft' }}
            </button>

            <p v-if="aiMessage" class="text-[11px] font-medium leading-relaxed"
              :class="aiMessage.includes('Unable') || aiMessage.includes('failed') ? 'text-lm-red' : 'text-lm-green'">
              {{ aiMessage }}
            </p>

            <div class="border-t border-lm-line-soft" />

            <!-- PDF Sources -->
            <div>
              <div class="mb-2 flex items-center justify-between">
                <p class="font-mono text-[9px] font-bold uppercase tracking-[0.14em] text-lm-ink-3">PDF Sources</p>
                <span class="font-mono text-[10px] font-semibold text-lm-ink-3">{{ documents.length }}</span>
              </div>
              <div v-if="documents.length" class="space-y-1.5">
                <button
                  v-for="document in documents"
                  :key="document.id"
                  type="button"
                  class="w-full rounded-lg border-2 px-3 py-2 text-left transition-all duration-150"
                  :class="selectedDocumentId === document.id
                    ? 'border-lm-line bg-lm-yellow/20 shadow-stamp-sm'
                    : 'border-lm-line-soft bg-lm-bg-soft hover:border-lm-line'"
                  @click="selectDocument(document)"
                >
                  <p class="truncate font-display text-[11px] font-semibold text-lm-ink">{{ document.displayName }}</p>
                  <p class="mt-0.5 font-mono text-[10px] text-lm-ink-3">{{ document.pageCount ?? 0 }} pages · {{ formatFileSize(document.fileSizeBytes) }}</p>
                </button>
              </div>
              <p v-else class="rounded-lg border-2 border-dashed border-lm-line-soft py-6 text-center text-[11px] text-lm-ink-3">
                No PDFs registered.
              </p>
            </div>

            <!-- PDF Preview toggle -->
            <button
              type="button"
              class="flex items-center gap-1.5 font-mono text-[9px] font-bold uppercase tracking-[0.1em] text-lm-ink-3 transition hover:text-lm-ink"
              @click="pdfOpen = !pdfOpen"
            >
              <svg class="h-3 w-3 transition-transform duration-200" :class="pdfOpen ? 'rotate-180' : ''" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="6 9 12 15 18 9" />
              </svg>
              {{ pdfOpen ? 'Hide' : 'Show' }} PDF Preview
            </button>

            <!-- PDF Preview (expandable) -->
            <template v-if="pdfOpen">
              <div class="flex gap-2">
                <div class="flex-1">
                  <label class="mb-1 block font-mono text-[9px] font-bold uppercase tracking-[0.1em] text-lm-ink-3">Start</label>
                  <input v-model.number="pageStart" type="number" min="1"
                    class="w-full rounded-lg border-2 border-lm-line-soft bg-lm-bg-soft px-2 py-1.5 text-[12px] text-lm-ink outline-none focus:border-lm-line"
                    @change="clampPageRange" />
                </div>
                <div class="flex-1">
                  <label class="mb-1 block font-mono text-[9px] font-bold uppercase tracking-[0.1em] text-lm-ink-3">End</label>
                  <input v-model.number="pageEnd" type="number" min="1"
                    class="w-full rounded-lg border-2 border-lm-line-soft bg-lm-bg-soft px-2 py-1.5 text-[12px] text-lm-ink outline-none focus:border-lm-line"
                    @change="clampPageRange" />
                </div>
              </div>
              <button
                type="button"
                class="w-full rounded-lg border-2 border-lm-ink bg-lm-ink px-3 py-2 text-[12px] font-semibold text-lm-bg transition hover:opacity-80 disabled:cursor-not-allowed disabled:opacity-50"
                :disabled="previewLoading || !selectedDocument"
                @click="loadPreview"
              >
                {{ previewLoading ? 'Loading…' : 'Preview' }}
              </button>
              <p v-if="previewError" class="rounded-lg border-2 border-lm-red bg-lm-red-soft px-2 py-1.5 text-[11px] font-medium text-lm-red">
                {{ previewError }}
              </p>
              <div v-if="preview" class="space-y-2">
                <iframe :src="preview.fileUrl" class="h-64 w-full rounded-lg border-2 border-lm-line shadow-stamp-sm" title="PDF preview" />
                <div class="h-52 overflow-y-auto rounded-lg border-2 border-lm-line bg-lm-bg-soft p-3 shadow-stamp-sm">
                  <div v-for="page in preview.pages" :key="page.pageNumber" class="mb-3 last:mb-0">
                    <p class="mb-1 font-mono text-[9px] font-bold uppercase tracking-[0.1em] text-lm-ink-3">Page {{ page.pageNumber }}</p>
                    <p class="whitespace-pre-wrap text-[11px] leading-relaxed text-lm-ink-2">{{ page.text || 'No extractable text.' }}</p>
                  </div>
                </div>
              </div>
            </template>
          </div>

          <!-- Collapsed: spark icon -->
          <div v-else class="flex flex-1 flex-col items-center pt-3">
            <div class="flex h-6 w-6 items-center justify-center rounded-[6px] border-2 border-lm-line bg-lm-yellow">
              <svg class="h-3 w-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="m12 3-1.912 5.813a2 2 0 0 1-1.275 1.275L3 12l5.813 1.912a2 2 0 0 1 1.275 1.275L12 21l1.912-5.813a2 2 0 0 1 1.275-1.275L21 12l-5.813-1.912a2 2 0 0 1-1.275-1.275L12 3z" />
              </svg>
            </div>
          </div>
        </aside>
      </div>

    </div>
  </div>

  <!-- Confirm delete module/subtopic modal -->
  <Teleport to="body">
    <Transition
      enter-active-class="transition duration-200 ease-out"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="transition duration-150 ease-in"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div v-if="showDeleteConfirm" class="fixed inset-0 z-50 flex items-center justify-center p-4">
        <div class="absolute inset-0 bg-lm-ink/60 backdrop-blur-sm" aria-hidden="true" @click="showDeleteConfirm = false" />

        <Transition
          enter-active-class="transition duration-200 ease-out"
          enter-from-class="opacity-0 scale-95 translate-y-2"
          enter-to-class="opacity-100 scale-100 translate-y-0"
          leave-active-class="transition duration-150 ease-in"
          leave-from-class="opacity-100 scale-100 translate-y-0"
          leave-to-class="opacity-0 scale-95 translate-y-2"
        >
          <div
            v-if="showDeleteConfirm"
            role="dialog"
            aria-modal="true"
            aria-labelledby="confirm-delete-title"
            class="relative z-10 flex w-full max-w-md flex-col overflow-hidden rounded-[18px] border-2 border-lm-line bg-lm-surface shadow-stamp-md"
            @click.stop
          >
            <!-- Header -->
            <header class="shrink-0 border-b-2 border-lm-line px-6 py-5">
              <div class="flex items-start justify-between gap-4">
                <div class="flex items-center gap-3">
                  <div class="flex h-9 w-9 items-center justify-center rounded-[10px] border-2 border-lm-line bg-lm-red-soft text-lm-red shadow-stamp-sm">
                    <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z" />
                      <line x1="12" y1="9" x2="12" y2="13" />
                      <line x1="12" y1="17" x2="12.01" y2="17" />
                    </svg>
                  </div>
                  <div>
                    <h2 id="confirm-delete-title" class="font-display text-[15px] font-bold text-lm-ink">
                      Delete {{ deleteType === 'module' ? 'Module' : 'Subtopic' }}?
                    </h2>
                    <p class="mt-1 text-[11px] text-lm-ink-3">This action cannot be undone</p>
                  </div>
                </div>
                <button
                  type="button"
                  class="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg text-lm-ink-3 transition hover:bg-lm-bg hover:text-lm-ink"
                  aria-label="Close"
                  @click="showDeleteConfirm = false"
                >
                  <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M18 6 6 18M6 6l12 12" />
                  </svg>
                </button>
              </div>
            </header>

            <!-- Body -->
            <div class="px-6 py-5">
              <p class="text-[13px] leading-relaxed text-lm-ink-2">
                Are you sure you want to delete the {{ deleteType === 'module' ? 'module' : 'subtopic' }}
                <strong class="text-lm-red">"{{ deleteTargetTitle }}"</strong>?
                <span v-if="deleteType === 'module'">
                  All subtopics and configurations under this module will be permanently removed.
                </span>
                <span v-else>
                  This will permanently remove all lesson content and interactive configurations for this subtopic.
                </span>
              </p>
            </div>

            <!-- Footer -->
            <footer class="flex shrink-0 gap-2.5 border-t-2 border-lm-line px-6 py-4">
              <button
                type="button"
                class="h-10 flex-1 rounded-full border-2 border-lm-line bg-lm-surface px-4 text-[13px] font-bold text-lm-ink shadow-stamp-sm transition hover:-translate-y-px hover:shadow-stamp-md"
                @click="showDeleteConfirm = false"
              >
                Cancel
              </button>
              <button
                type="button"
                class="h-10 flex-1 rounded-full border-2 border-lm-line bg-lm-red text-white px-4 text-[13px] font-bold shadow-stamp-sm transition hover:-translate-y-px hover:shadow-stamp-md disabled:cursor-not-allowed disabled:opacity-50"
                :disabled="deletingInProgress"
                @click="proceedDelete"
              >
                {{ deletingInProgress ? 'Deleting...' : 'Delete' }}
              </button>
            </footer>
          </div>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.lesson-preview :deep(h2) { margin: 0.9rem 0 0.35rem; font-size: 1.05rem; font-weight: 800; color: #1a1814; }
.lesson-preview :deep(h3) { margin: 0.75rem 0 0.25rem; font-size: 0.95rem; font-weight: 800; color: #1a1814; }
.lesson-preview :deep(p),
.lesson-preview :deep(ul),
.lesson-preview :deep(ol),
.lesson-preview :deep(blockquote) { margin: 0.5rem 0; }
.lesson-preview :deep(ul) { list-style: disc; padding-left: 1.25rem; }
.lesson-preview :deep(ol) { list-style: decimal; padding-left: 1.25rem; }
.lesson-preview :deep(strong) { font-weight: 700; color: #1a1814; }
.lesson-preview :deep(em) { font-style: italic; }
.lesson-preview :deep(code) { border-radius: 4px; background: #f0ece4; color: #1a1814; padding: 0.1rem 0.3rem; font-size: 0.85em; }
.lesson-preview :deep(img) { max-width: 100%; border-radius: 0.5rem; }

@keyframes loading-bar {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(200%); }
}
.animate-loading-bar {
  animation: loading-bar 1.8s infinite linear;
}
</style>
