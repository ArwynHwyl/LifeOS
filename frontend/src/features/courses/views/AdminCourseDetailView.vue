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
  createCourseModule,
  createModuleSubTopic,
  deleteAdminSubTopic,
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

const deletingSubTopicId = ref<number | null>(null)
const subTopicDeleting = ref(false)

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

const statusDisplay = computed(() => {
  const s = course.value?.status
  if (s === 'PUBLISHED')      return { label: 'Published',     classes: 'bg-lm-green-soft text-lm-green border-2 border-lm-line shadow-stamp-sm', dot: 'bg-lm-green'  }
  if (s === 'PENDING_REVIEW') return { label: 'Pending Review', classes: 'bg-lm-yellow text-lm-ink border-2 border-lm-line shadow-stamp-sm', dot: 'bg-lm-ink'    }
  if (s === 'NEED_REVISION') return { label: 'Needs Revision', classes: 'bg-lm-red-soft text-lm-red border-2 border-lm-line shadow-stamp-sm', dot: 'bg-lm-red'    }
  return                             { label: 'Draft',           classes: 'bg-lm-bg-soft text-lm-ink-3 border-2 border-lm-line-soft', dot: 'bg-lm-ink-3' }
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

function handleInteractiveChecked(payload: any) {
  console.log('Interactive checked in admin preview mode:', payload)
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

onMounted(loadCourse)

// ── Data loading (unchanged) ──────────────────────────────────────────────
async function loadCourse() {
  loading.value  = true
  loadError.value = ''
  try {
    const data = await getAdminCourse(courseId.value)
    course.value = data
    selectedDocumentId.value = data.documentSources[0]?.id ?? null
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

async function confirmDeleteSubTopic(moduleId: number, subTopicId: number) {
  subTopicDeleting.value = true
  try {
    await deleteAdminSubTopic(subTopicId)
    if (selectedSubTopicId.value === subTopicId) {
      selectedSubTopicId.value = null
    }
    await loadCourse()
    deletingSubTopicId.value = null
  } catch (error) {
    console.error(error)
  } finally {
    subTopicDeleting.value = false
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
        <span
          v-if="course"
          class="inline-flex shrink-0 items-center gap-2 rounded-full border-2 border-lm-green bg-lm-green-soft px-4 py-2 font-display text-[13px] font-bold text-lm-green"
        >
          <AdminIcon name="check" :size="15" />
          {{ statusDisplay.label }}
        </span>
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
          class="relative flex shrink-0 flex-col border-r-2 border-lm-line bg-lm-bg overflow-hidden transition-[width] duration-200 ease-out"
            :style="{ width: outlineOpen ? '292px' : '44px' }"
        >
            <!-- Panel header -->
          <div
              class="flex h-11 shrink-0 items-center border-b-2 border-lm-line-soft"
            :class="outlineOpen ? 'justify-between px-4' : 'justify-center'"
          >
            <MonoLabel v-if="outlineOpen">Outline</MonoLabel>
            <button
              type="button"
              class="flex h-7 w-7 shrink-0 items-center justify-center rounded-[8px] border-2 border-lm-line bg-lm-surface text-lm-ink shadow-stamp-xs transition-all duration-200 hover:-translate-y-px hover:shadow-stamp-sm"
              :title="outlineOpen ? 'Collapse outline' : 'Expand outline'"
              @click="outlineOpen = !outlineOpen"
            >
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                <path v-if="outlineOpen" d="M11 19l-7-7 7-7M18 19l-7-7 7-7" />
                <path v-else d="M13 5l7 7-7 7M6 5l7 7-7 7" />
              </svg>
            </button>
          </div>

          <!-- Module/subtopic list (expanded) -->
          <div v-if="outlineOpen" class="flex-1 overflow-y-auto py-2">
            <div v-for="module in modules" :key="module.id" class="mb-3">
              <!-- Module Header -->
              <div class="flex items-center justify-between px-5 py-2">
                <p class="truncate font-display text-[13px] font-extrabold text-lm-ink">{{ module.title }}</p>
                <span class="ml-2 shrink-0 font-mono text-[10px] font-bold text-lm-ink-3">{{ module.subTopics.length }}</span>
              </div>

              <!-- Subtopics -->
              <div class="space-y-[1px]">
                <div
                  v-for="subTopic in module.subTopics"
                  :key="subTopic.id"
                  class="group relative flex min-h-[38px] w-full items-center justify-between border-l-[3px] py-2 pl-6 pr-4 transition-all duration-150"
                  :class="selectedSubTopicId === subTopic.id
                    ? 'border-lm-line bg-lm-yellow font-bold text-lm-ink'
                    : 'border-transparent font-semibold text-lm-ink-2 hover:bg-lm-bg-soft'"
                >
                  <!-- Normal Navigation Mode -->
                  <button
                    v-if="deletingSubTopicId !== subTopic.id"
                    type="button"
                    class="flex flex-1 min-w-0 items-center justify-between text-left font-display text-[13px]"
                    @click="selectSubTopic(subTopic.id)"
                  >
                    <span class="flex-1 min-w-0 truncate pr-2">{{ subTopic.title }}</span>
                    <ITypeBadge :type="subTopic.interactionType" class="shrink-0 mr-2" />
                  </button>

                  <!-- Delete Trash Button (always visible) -->
                  <button
                    v-if="deletingSubTopicId !== subTopic.id && course.status !== 'PENDING_REVIEW'"
                    type="button"
                    class="flex h-6 w-6 shrink-0 items-center justify-center rounded bg-lm-surface border border-lm-line-soft text-lm-ink-3 hover:text-lm-red hover:border-lm-red transition-all duration-150 shadow-stamp-xs"
                    title="Delete subtopic"
                    @click.stop="deletingSubTopicId = subTopic.id"
                  >
                    <svg class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M3 6h18" /><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2 2v2" />
                    </svg>
                  </button>

                  <!-- Delete Confirmation Mode -->
                  <div v-if="deletingSubTopicId === subTopic.id" class="flex w-full items-center justify-between font-mono text-[11px] text-lm-red">
                    <span class="truncate">Delete?</span>
                    <div class="flex items-center gap-2 shrink-0">
                      <button
                        type="button"
                        class="px-1.5 py-0.5 font-bold text-lm-green hover:underline"
                        :disabled="subTopicDeleting"
                        @click.stop="confirmDeleteSubTopic(module.id, subTopic.id)"
                      >
                        ✓
                      </button>
                      <button
                        type="button"
                        class="px-1.5 py-0.5 font-bold text-lm-red hover:underline"
                        @click.stop="deletingSubTopicId = null"
                      >
                        ✕
                      </button>
                    </div>
                  </div>
                </div>
              </div>

              <!-- "+ Add subtopic" row at the end of each module's subtopic list -->
              <div v-if="course.status !== 'PENDING_REVIEW'" class="px-5 py-1.5">
                <!-- Click to open inline form -->
                <button
                  v-if="addingSubTopicModuleId !== module.id"
                  type="button"
                  class="flex w-full items-center gap-1.5 font-mono text-[10px] font-bold text-lm-ink-3 transition-colors duration-150 hover:text-lm-ink"
                  @click="addingSubTopicModuleId = module.id; newSubTopicTitle = ''"
                >
                  <svg class="h-3 w-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                    <line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" />
                  </svg>
                  Add subtopic
                </button>

                <!-- Inline form -->
                <div v-else class="mt-1 flex items-center gap-1.5">
                  <input
                    v-model="newSubTopicTitle"
                    type="text"
                    placeholder="New subtopic title"
                    class="h-8 flex-1 rounded border border-lm-line-soft bg-lm-bg-soft px-2 font-display text-[12px] text-lm-ink outline-none placeholder:text-lm-ink-3 focus:border-lm-line"
                    @keydown.enter="addSubTopicInline(module.id)"
                    @keydown.escape="addingSubTopicModuleId = null"
                  />
                  <div class="flex items-center gap-1 shrink-0">
                    <button
                      type="button"
                      class="h-8 rounded bg-lm-ink border border-lm-ink px-2 font-mono text-[10px] font-bold text-lm-bg hover:opacity-90 disabled:opacity-50"
                      :disabled="subTopicCreating || !newSubTopicTitle.trim()"
                      @click="addSubTopicInline(module.id)"
                    >
                      Save
                    </button>
                    <button
                      type="button"
                      class="h-8 rounded bg-lm-surface border border-lm-line-soft px-2 font-mono text-[10px] font-bold text-lm-ink-2 hover:bg-lm-bg-soft"
                      @click="addingSubTopicModuleId = null"
                    >
                      Cancel
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <!-- "Add module" input+button at the bottom of the module list -->
            <div v-if="course.status !== 'PENDING_REVIEW'" class="border-t border-lm-line-soft px-5 py-3 mt-4">
              <p class="mb-1.5 font-mono text-[10px] font-bold uppercase tracking-[0.08em] text-lm-ink-3">New Module</p>
              <div class="flex items-center gap-2">
                <input
                  v-model="newModuleTitle"
                  type="text"
                  placeholder="Module title"
                  class="h-8 flex-1 rounded border border-lm-line-soft bg-lm-bg-soft px-2 font-display text-[12px] text-lm-ink outline-none placeholder:text-lm-ink-3 focus:border-lm-line"
                  @keydown.enter="addModuleInline"
                />
                <button
                  type="button"
                  class="h-8 rounded bg-lm-ink border border-lm-ink px-2 font-mono text-[10px] font-bold text-lm-bg hover:opacity-90 disabled:opacity-50 shrink-0 shadow-stamp-xs"
                  :disabled="moduleCreating || !newModuleTitle.trim()"
                  @click="addModuleInline"
                >
                  {{ moduleCreating ? '...' : '+ Add' }}
                </button>
              </div>
              <p v-if="moduleError" class="mt-1.5 font-mono text-[10px] text-lm-red">{{ moduleError }}</p>
            </div>

            <div v-if="!modules.length" class="px-4 py-10 text-center">
              <p class="font-mono text-[10px] uppercase tracking-[0.06em] text-lm-ink-3">No modules yet</p>
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

          <!-- Subtopic viewer / editor -->
          <div v-if="selectedSubTopic" class="relative mx-auto w-full max-w-[840px] px-7 py-8">

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
</style>
