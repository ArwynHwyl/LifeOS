<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { AxiosError } from 'axios'
import { useRoute, useRouter } from 'vue-router'
import AdminNavbar from '@/features/courses/components/Admin/AdminNavbar.vue'
import AdminCourseOutline from '@/features/courses/components/Admin/AdminCourseOutline.vue'
import SubTopicLessonEditor from '@/features/courses/components/Admin/SubTopicLessonEditor.vue'
import { DEFAULT_COVER_ID, getCoverPreset } from '@/features/courses/constants/courseCoverPresets'
import {
  getAdminCourse,
  previewCourseDocument,
  requestModuleAiGeneration,
  getAiGenerationLog,
  updateModuleSubTopic,
  type AdminCourseDetailDto,
  type AdminDocumentSourceDto,
  type AdminModuleDto,
  type AdminSubTopicDto,
  type DocumentPreviewDto,
  type InteractionType,
} from '@/features/courses/services/adminCourses'

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

// ── Computed ──────────────────────────────────────────────────────────────
const cover = computed(() => getCoverPreset(course.value?.coverId ?? DEFAULT_COVER_ID))

const statusDisplay = computed(() => {
  const s = course.value?.status
  if (s === 'PUBLISHED')      return { label: 'Published',     classes: 'bg-lm-green-soft text-lm-green border-2 border-lm-line shadow-stamp-sm', dot: 'bg-lm-green'  }
  if (s === 'PENDING_REVIEW') return { label: 'Pending Review', classes: 'bg-lm-yellow text-lm-ink border-2 border-lm-line shadow-stamp-sm', dot: 'bg-lm-ink'    }
  if (s === 'NEEDS_REVISION') return { label: 'Needs Revision', classes: 'bg-lm-red-soft text-lm-red border-2 border-lm-line shadow-stamp-sm', dot: 'bg-lm-red'    }
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

// ── Auto-select first subtopic on load ───────────────────────────────────
watch(modules, (mods) => {
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

// ── Centre editor save ────────────────────────────────────────────────────
async function saveFromCentre(subTopic: AdminSubTopicDto, payload: {
  title: string
  content: string
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

function onModulesUpdated(newModules: AdminModuleDto[]) {
  if (course.value) course.value.modules = newModules
}

function wait(ms: number) { return new Promise(resolve => window.setTimeout(resolve, ms)) }

function formatFileSize(bytes: number) {
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

function formatInteractionType(value: string | null | undefined) {
  if (!value || value === 'NONE') return ''
  return value.toLowerCase().split('_').map(p => p.charAt(0).toUpperCase() + p.slice(1)).join(' ')
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

      <!-- ── Rich page header ─────────────────────────────────────── -->
      <header class="admin-page-header">
        <div class="admin-page-header__inner">
          <div class="flex min-w-0 items-center gap-4">
            <button
              type="button"
              class="admin-page-header__control inline-flex shrink-0 items-center gap-2 rounded-full border-2 border-lm-line bg-lm-bg-soft px-5 text-[14px] font-extrabold text-lm-ink shadow-stamp-sm transition-all duration-200 hover:-translate-y-px hover:shadow-md"
              @click="router.push('/admin/courses')"
            >
              <svg class="h-3 w-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M19 12H5M11 18l-6-6 6-6" />
              </svg>
              All Courses
            </button>

            <div
              v-if="course"
              class="admin-page-header__control flex w-10 shrink-0 items-center justify-center rounded-xl border-2 border-lm-line font-math text-[20px] font-bold italic shadow-stamp-sm"
              :class="[cover.bgClass, cover.textClass]"
            >
              {{ cover.symbol }}
            </div>

            <div class="min-w-0">
              <div class="flex min-w-0 items-center gap-2">
                <h1 class="admin-page-header__title truncate">{{ course?.title ?? 'Course' }}</h1>
                <span
                  v-if="course"
                  class="inline-flex h-7 shrink-0 items-center gap-1.5 rounded-full px-2.5 font-mono text-[10px] font-bold uppercase tracking-[0.04em]"
                  :class="statusDisplay.classes"
                >
                  <span class="h-1.5 w-1.5 rounded-full" :class="statusDisplay.dot" />
                  {{ statusDisplay.label }}
                </span>
              </div>
              <p v-if="course?.description" class="admin-page-header__subtitle">
                {{ course.description }}
              </p>
            </div>
          </div>

          <span
            v-if="course"
            class="admin-page-header__control inline-flex shrink-0 items-center gap-2 rounded-full border-2 border-lm-green bg-lm-green-soft px-5 font-display text-[14px] font-bold text-lm-green"
          >
            <svg class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
              <path d="M20 6 9 17l-5-5" />
            </svg>
            {{ statusDisplay.label }}
          </span>
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
          class="relative flex shrink-0 flex-col border-r-2 border-lm-line bg-lm-bg overflow-hidden transition-[width] duration-200 ease-out"
            :style="{ width: outlineOpen ? '292px' : '44px' }"
        >
            <!-- Panel header -->
          <div
              class="flex h-11 shrink-0 items-center border-b-2 border-lm-line-soft"
            :class="outlineOpen ? 'justify-between px-4' : 'justify-center'"
          >
            <p v-if="outlineOpen" class="font-mono text-[11px] font-bold uppercase tracking-[0.24em] text-lm-ink-3">Outline</p>
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
            <div v-for="module in modules" :key="module.id" class="mb-1">
              <div class="flex items-center justify-between px-5 py-2">
                <p class="truncate font-display text-[13px] font-extrabold text-lm-ink">{{ module.title }}</p>
                <span class="ml-2 shrink-0 font-mono text-[10px] font-bold text-lm-ink-3">{{ module.subTopics.length }}</span>
              </div>
              <button
                v-for="subTopic in module.subTopics"
                :key="subTopic.id"
                type="button"
                class="flex min-h-[38px] w-full items-center gap-2 border-l-[3px] py-2 pl-6 pr-4 text-left font-display text-[13px] transition-all duration-150"
                :class="selectedSubTopicId === subTopic.id
                  ? 'border-lm-line bg-lm-yellow font-bold text-lm-ink'
                  : 'border-transparent font-semibold text-lm-ink-2 hover:bg-lm-bg-soft'"
                @click="selectSubTopic(subTopic.id)"
              >
                <span class="flex-1 min-w-0 truncate">{{ subTopic.title }}</span>
                <span
                  v-if="subTopic.interactionType && subTopic.interactionType !== 'NONE'"
                  class="shrink-0 rounded-full px-1.5 py-0.5 font-mono text-[10px] font-bold uppercase tracking-[0.04em]"
                  :class="{
                    'bg-lm-blue-soft text-lm-blue':     subTopic.interactionType === 'QUIZ',
                    'bg-lm-purple-soft text-lm-purple':  subTopic.interactionType === 'GRAPH_2D' || subTopic.interactionType === 'FORMULA_EXPLORER',
                    'bg-lm-green-soft text-lm-green':    subTopic.interactionType === 'VISUAL_LAYER',
                  }"
                >
                  {{ subTopic.interactionType === 'GRAPH_2D' ? 'Graph' : subTopic.interactionType === 'FORMULA_EXPLORER' ? 'Formula' : subTopic.interactionType === 'VISUAL_LAYER' ? 'Visual' : subTopic.interactionType }}
                </span>
              </button>
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
                  <p class="font-mono text-[11px] font-bold uppercase tracking-[0.22em] text-lm-ink-3">
                    {{ selectedSubTopic.moduleTitle }}
                  </p>
                  <h2 class="mt-2 font-display text-[30px] font-extrabold leading-tight text-lm-ink">
                    {{ selectedSubTopic.title }}
                  </h2>
                  <span
                    v-if="selectedSubTopic.interactionType && selectedSubTopic.interactionType !== 'NONE'"
                    class="mt-2 inline-flex rounded-full px-2 py-0.5 font-mono text-[9px] font-bold uppercase tracking-normal"
                    :class="{
                      'bg-lm-blue-soft text-lm-blue':     selectedSubTopic.interactionType === 'QUIZ',
                      'bg-lm-purple-soft text-lm-purple':  selectedSubTopic.interactionType === 'GRAPH_2D' || selectedSubTopic.interactionType === 'FORMULA_EXPLORER',
                      'bg-lm-green-soft text-lm-green':    selectedSubTopic.interactionType === 'VISUAL_LAYER',
                    }"
                  >
                    {{ formatInteractionType(selectedSubTopic.interactionType) }}
                  </span>
                </div>
                <button
                  v-if="course.status !== 'PENDING_REVIEW'"
                  type="button"
                  class="inline-flex h-10 shrink-0 items-center gap-2 rounded-full border-2 border-lm-line bg-lm-surface px-5 py-2.5 text-[14px] font-bold text-lm-ink shadow-stamp-sm transition-all duration-200 hover:-translate-y-px hover:shadow-md"
                  @click="editingInCentre = true; centreMessage = ''"
                >
                  <svg class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M12 20h9" /><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4Z" />
                  </svg>
                  Edit
                </button>
              </div>

              <!-- Content HTML -->
              <div
                v-if="lessonHtml(selectedSubTopic)"
                class="lesson-preview rounded-[12px] border-2 border-lm-line bg-white px-7 py-6 text-[15px] leading-[1.7] text-[#3b3630] shadow-stamp-sm"
                v-html="lessonHtml(selectedSubTopic)"
              />
              <div v-else class="rounded-[14px] border-2 border-dashed border-lm-line-soft py-14 text-center">
                <p class="font-mono text-[11px] uppercase tracking-[0.06em] text-lm-ink-3">
                  No content yet — click Edit to add
                </p>
              </div>

              <!-- Interaction badge -->
              <div v-if="selectedSubTopic.interactionType && selectedSubTopic.interactionType !== 'NONE'" class="mt-4 overflow-hidden rounded-[12px] border-2 border-lm-line-soft">
                <div class="flex items-center gap-2 border-b border-lm-line-soft bg-lm-bg-soft px-4 py-2.5">
                  <div class="flex h-6 w-6 shrink-0 items-center justify-center rounded-[6px] border-2 border-lm-line bg-lm-yellow">
                    <svg class="h-3 w-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="m12 3-1.912 5.813a2 2 0 0 1-1.275 1.275L3 12l5.813 1.912a2 2 0 0 1 1.275 1.275L12 21l1.912-5.813a2 2 0 0 1 1.275-1.275L21 12l-5.813-1.912a2 2 0 0 1-1.275-1.275L12 3z" />
                    </svg>
                  </div>
                  <p class="font-mono text-[9px] font-bold uppercase tracking-[0.12em] text-lm-ink-3">
                    Interactive · {{ selectedSubTopic.interactionType }}
                  </p>
                </div>
                <div class="px-4 py-3">
                  <p v-if="selectedSubTopic.interactionPrompt" class="mb-1 text-[13px] font-semibold text-lm-ink">
                    {{ selectedSubTopic.interactionPrompt }}
                  </p>
                  <p class="text-[11px] italic text-lm-ink-3">Renders in the learner app. Click Edit to configure.</p>
                </div>
              </div>

              <p v-if="centreMessage" class="mt-3 text-[12px] font-medium" :class="centreMessage.includes('Unable') ? 'text-lm-red' : 'text-lm-green'">
                {{ centreMessage }}
              </p>
            </template>

            <!-- Edit mode -->
            <template v-else>
              <div class="mb-4 flex items-center gap-2">
                <p class="font-mono text-[9px] font-bold uppercase tracking-[0.14em] text-lm-ink-3">{{ selectedSubTopic.moduleTitle }}</p>
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

          <!-- ── Manage Course Outline (collapsible, always accessible) ── -->
          <div class="relative mx-auto w-full max-w-[740px] shrink-0 px-7 pb-10">
            <button
              type="button"
              class="flex w-full items-center gap-3 py-3"
              @click="manageOpen = !manageOpen"
            >
              <div class="flex-1 border-t-2 border-lm-line-soft" />
              <span class="font-mono text-[9px] font-bold uppercase tracking-[0.14em] text-lm-ink-3">Manage Course Outline</span>
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
                :selected-document="selectedDocument"
                :page-start="pageStart"
                :page-end="pageEnd"
                @module-selected="selectedModuleId = $event"
                @modules-updated="onModulesUpdated"
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
                Add a module in "Manage Course Outline" first.
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
.lesson-preview :deep(code) { border-radius: 4px; background: #f0ece4; padding: 0.1rem 0.3rem; font-size: 0.85em; }
.lesson-preview :deep(img) { max-width: 100%; border-radius: 0.5rem; }
</style>
