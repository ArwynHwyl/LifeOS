<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { AxiosError } from 'axios'
import { useRoute, useRouter } from 'vue-router'
import AppSidebar from '@/features/courses/components/Admin/AdminNavbar.vue'
import SubTopicLessonEditor from '@/features/courses/components/Admin/SubTopicLessonEditor.vue'
import { DEFAULT_COVER_ID, getCoverPreset } from '@/features/courses/constants/courseCoverPresets'
import {
  createCourseModule,
  createModuleSubTopic,
  getAiGenerationLog,
  getAdminCourse,
  listAiGenerationLogs,
  previewCourseDocument,
  requestModuleAiGeneration,
  type AdminCourseDetailDto,
  type AdminDocumentSourceDto,
  type AdminSubTopicDto,
  type DocumentPreviewDto,
  type InteractionType,
  updateModuleSubTopic,
} from '@/features/courses/services/adminCourses'

const route = useRoute()
const router = useRouter()

const courseId = computed(() => String(route.params.id ?? ''))
const course = ref<AdminCourseDetailDto | null>(null)
const loading = ref(false)
const loadError = ref('')

const selectedDocumentId = ref<number | null>(null)
const pageStart = ref(1)
const pageEnd = ref(10)
const preview = ref<DocumentPreviewDto | null>(null)
const previewLoading = ref(false)
const previewError = ref('')

const selectedModuleId = ref<number | null>(null)
const newModuleTitle = ref('')
const moduleCreating = ref(false)
const moduleMessage = ref('')
const editingSubTopicModuleId = ref<number | null>(null)
const subTopicTitle = ref('')
const subTopicContent = ref('')
const subTopicSaving = ref(false)
const subTopicMessage = ref('')
const editingLessonSubTopicId = ref<number | null>(null)
const aiRequirements = ref('')
const aiSubmitting = ref(false)
const aiMessage = ref('')
const aiPolling = ref(false)
const outlineMessage = ref('')
const outlinePolling = ref(false)

const cover = computed(() => getCoverPreset(DEFAULT_COVER_ID))
const documents = computed(() => course.value?.documentSources ?? [])
const modules = computed(() => course.value?.modules ?? [])
const selectedDocument = computed(() =>
  documents.value.find((document) => document.id === selectedDocumentId.value) ?? null,
)
const subTopicCount = computed(() =>
  modules.value.reduce((total, module) => total + module.subTopics.length, 0),
)

onMounted(async () => {
  await loadCourse()
  void pollLatestCourseOutlineGeneration()
})

watch(selectedDocument, (document) => {
  preview.value = null
  previewError.value = ''
  if (!document) return
  pageStart.value = 1
  pageEnd.value = Math.min(10, document.pageCount ?? 1)
}, { immediate: false })

async function loadCourse() {
  loading.value = true
  loadError.value = ''
  try {
    const data = await getAdminCourse(courseId.value)
    course.value = data
    selectedDocumentId.value = data.documentSources[0]?.id ?? null
    selectedModuleId.value = data.modules[0]?.id ?? null
  } catch (error) {
    loadError.value = getErrorMessage(error, 'Unable to load course.')
  } finally {
    loading.value = false
  }
}

async function loadPreview() {
  if (!selectedDocumentId.value) return
  previewLoading.value = true
  previewError.value = ''
  preview.value = null
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
  aiMessage.value = ''
  try {
    const result = await requestModuleAiGeneration({
      moduleId: selectedModuleId.value,
      documentSourceId: selectedDocument.value.id,
      pageStart: pageStart.value,
      pageEnd: pageEnd.value,
      requirements: aiRequirements.value.trim() || 'Generate concise learner-facing subtopics from the selected source pages.',
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
    aiMessage.value = `AI generation is still running. Refresh this page in a moment.`
  } finally {
    aiPolling.value = false
  }
}

async function pollLatestCourseOutlineGeneration() {
  if (outlinePolling.value) return
  try {
    const logs = await listAiGenerationLogs(courseId.value)
    const outlineLog = logs.find((log) => log.type === 'COURSE_OUTLINE' && ['PENDING', 'RUNNING'].includes(log.status))
    if (!outlineLog) return
    await pollCourseOutlineGeneration(outlineLog.id)
  } catch (error) {
    outlineMessage.value = getErrorMessage(error, 'Unable to check course outline generation.')
  }
}

async function pollCourseOutlineGeneration(logId: number) {
  outlinePolling.value = true
  outlineMessage.value = 'AI course outline generation is queued...'
  try {
    for (let attempt = 0; attempt < 40; attempt += 1) {
      await wait(1500)
      const log = await getAiGenerationLog(logId)
      if (log.status === 'SUCCESS') {
        await loadCourse()
        outlineMessage.value = 'AI course outline generation completed.'
        return
      }
      if (log.status === 'FAILED') {
        outlineMessage.value = log.errorMessage || 'AI course outline generation failed.'
        return
      }
      outlineMessage.value = `AI course outline generation ${log.status.toLowerCase()}...`
    }
    outlineMessage.value = 'AI course outline generation is still running. Refresh this page in a moment.'
  } finally {
    outlinePolling.value = false
  }
}

function wait(ms: number) {
  return new Promise((resolve) => window.setTimeout(resolve, ms))
}

async function addModuleForGeneration() {
  const title = newModuleTitle.value.trim()
  if (!course.value || !title) return
  moduleCreating.value = true
  moduleMessage.value = ''
  try {
    const module = await createCourseModule(course.value.id, {
      title,
      sortOrder: course.value.modules.length,
      contentDepth: 'MEDIUM',
    })
    course.value.modules = [...course.value.modules, module]
    selectedModuleId.value = module.id
    newModuleTitle.value = ''
    moduleMessage.value = 'Module created and selected.'
  } catch (error) {
    moduleMessage.value = getErrorMessage(error, 'Unable to create module.')
  } finally {
    moduleCreating.value = false
  }
}

function openSubTopicForm(moduleId: number) {
  editingSubTopicModuleId.value = moduleId
  selectedModuleId.value = moduleId
  subTopicTitle.value = ''
  subTopicContent.value = ''
  subTopicMessage.value = ''
}

function closeSubTopicForm() {
  editingSubTopicModuleId.value = null
  subTopicTitle.value = ''
  subTopicContent.value = ''
  subTopicMessage.value = ''
}

async function addManualSubTopic(moduleId: number) {
  const title = subTopicTitle.value.trim()
  if (!course.value || !title) return
  const moduleIndex = course.value.modules.findIndex((module) => module.id === moduleId)
  if (moduleIndex === -1) return

  subTopicSaving.value = true
  subTopicMessage.value = ''
  try {
    const module = course.value.modules[moduleIndex]
    const subTopic = await createModuleSubTopic(moduleId, {
      title,
      content: subTopicContent.value,
      sortOrder: module.subTopics.length,
      pageStart: selectedDocument.value ? pageStart.value : null,
      pageEnd: selectedDocument.value ? pageEnd.value : null,
    })
    const updatedModules = [...course.value.modules]
    updatedModules[moduleIndex] = {
      ...module,
      subTopics: [...module.subTopics, subTopic],
    }
    course.value.modules = updatedModules
    closeSubTopicForm()
    subTopicMessage.value = 'Subtopic added.'
  } catch (error) {
    subTopicMessage.value = getErrorMessage(error, 'Unable to add subtopic.')
  } finally {
    subTopicSaving.value = false
  }
}

function openLessonEditor(subTopic: AdminSubTopicDto) {
  editingLessonSubTopicId.value = subTopic.id
  subTopicMessage.value = ''
}

function closeLessonEditor() {
  editingLessonSubTopicId.value = null
}

async function saveLessonSubTopic(subTopic: AdminSubTopicDto, payload: {
  title: string
  content: string
  interactionType: InteractionType
  interactionPrompt: string | null
  interactionConfig: string | null
}) {
  if (!course.value) return
  subTopicSaving.value = true
  subTopicMessage.value = ''
  try {
    const updated = await updateModuleSubTopic(subTopic.id, {
      title: payload.title,
      content: payload.content,
      sortOrder: subTopic.sortOrder,
      pageStart: subTopic.pageStart,
      pageEnd: subTopic.pageEnd,
      interactionType: payload.interactionType,
      interactionPrompt: payload.interactionPrompt,
      interactionConfig: payload.interactionConfig,
    })
    course.value.modules = course.value.modules.map((module) => {
      if (module.id !== updated.moduleId) return module
      return {
        ...module,
        subTopics: module.subTopics.map((item) => (item.id === updated.id ? updated : item)),
      }
    })
    editingLessonSubTopicId.value = null
    subTopicMessage.value = 'Subtopic updated.'
  } catch (error) {
    subTopicMessage.value = getErrorMessage(error, 'Unable to update subtopic.')
  } finally {
    subTopicSaving.value = false
  }
}

function lessonHtml(subTopic: AdminSubTopicDto) {
  if (subTopic.contentHtml) return subTopic.contentHtml
  return textToHtml(subTopic.content || '')
}

function textToHtml(value: string) {
  return value
    .split(/\n{2,}/)
    .map((paragraph) => paragraph.trim())
    .filter(Boolean)
    .map((paragraph) => `<p>${escapeHtml(paragraph).replace(/\n/g, '<br>')}</p>`)
    .join('')
}

function escapeHtml(value: string) {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
}

function selectDocument(document: AdminDocumentSourceDto) {
  selectedDocumentId.value = document.id
}

function clampPageRange() {
  const max = selectedDocument.value?.pageCount ?? 1
  pageStart.value = Math.min(Math.max(1, pageStart.value), max)
  pageEnd.value = Math.min(Math.max(pageStart.value, pageEnd.value), max)
}

function formatFileSize(bytes: number) {
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

function formatInteractionType(value: string | null | undefined) {
  if (!value || value === 'NONE') return ''
  return value
    .toLowerCase()
    .split('_')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ')
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
  <div class="course-app flex h-screen w-full overflow-hidden bg-[#f4f5f9]">
    <AppSidebar active-item="course" />

    <div class="flex min-w-0 flex-1 flex-col overflow-hidden">
      <div class="flex shrink-0 items-center justify-between border-b border-slate-200/70 bg-white px-7 py-3">
        <div class="flex items-center gap-1.5 text-[12px]">
          <button class="font-medium text-slate-400 transition hover:text-[#5b4cfa]" type="button" @click="router.push('/courses')">
            Courses
          </button>
          <span class="text-slate-300">/</span>
          <span class="truncate font-semibold text-slate-700">{{ course?.title ?? 'Course' }}</span>
        </div>
        <button class="rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-[12px] font-semibold text-slate-600 transition hover:bg-slate-50" type="button" @click="loadCourse">
          Refresh
        </button>
      </div>

      <main class="flex-1 overflow-y-auto px-7 py-6">
        <div v-if="loadError" class="mb-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-[13px] font-medium text-red-700">
          {{ loadError }}
        </div>

        <div v-if="loading" class="rounded-xl bg-white px-5 py-4 text-[13px] text-slate-500 ring-1 ring-slate-900/[0.06]">
          Loading course...
        </div>

        <template v-else-if="course">
          <section class="mb-5 rounded-xl bg-white px-5 py-4 ring-1 ring-slate-900/[0.06]">
            <div class="flex items-center gap-4">
              <div class="flex h-14 w-14 shrink-0 items-center justify-center rounded-xl font-serif text-2xl font-bold" :class="[cover.bgClass, cover.textClass]">
                {{ cover.symbol }}
              </div>
              <div class="min-w-0">
                <h1 class="text-[18px] font-bold text-black">{{ course.title }}</h1>
                <p class="mt-1 line-clamp-2 text-[12px] leading-relaxed text-slate-500">{{ course.description }}</p>
              </div>
            </div>
          </section>

          <section class="mb-5 rounded-xl bg-white p-5 ring-1 ring-slate-900/[0.06]">
            <div class="mb-4 flex items-center justify-between gap-4">
              <div>
                <h2 class="text-[14px] font-bold !text-slate-900">Course outline</h2>
                <p class="mt-1 text-[12px] font-medium text-slate-400">
                  {{ modules.length }} module{{ modules.length === 1 ? '' : 's' }} · {{ subTopicCount }} subtopic{{ subTopicCount === 1 ? '' : 's' }}
                </p>
              </div>
              <div class="flex min-w-0 items-center gap-2">
                <input
                  v-model="newModuleTitle"
                  type="text"
                  placeholder="New module title"
                  class="h-9 w-56 rounded-lg border border-slate-200 px-3 text-[12px] font-medium text-slate-700 outline-none transition placeholder:text-slate-400 focus:border-[#5b4cfa]/50"
                />
                <button
                  type="button"
                  class="h-9 rounded-lg bg-[#5b4cfa] px-3 text-[12px] font-bold text-white transition hover:bg-[#493be0] disabled:cursor-not-allowed disabled:opacity-60"
                  :disabled="moduleCreating || !newModuleTitle.trim()"
                  @click="addModuleForGeneration"
                >
                  {{ moduleCreating ? 'Adding...' : 'Add module' }}
                </button>
              </div>
            </div>
            <p v-if="moduleMessage" class="mb-3 text-[12px] font-medium" :class="moduleMessage.includes('Unable') ? 'text-red-600' : 'text-emerald-700'">
              {{ moduleMessage }}
            </p>
            <p v-if="subTopicMessage" class="mb-3 text-[12px] font-medium" :class="subTopicMessage.includes('Unable') ? 'text-red-600' : 'text-emerald-700'">
              {{ subTopicMessage }}
            </p>
            <p
              v-if="outlineMessage"
              class="mb-3 rounded-lg border px-3 py-2 text-[12px] font-medium"
              :class="outlineMessage.includes('failed') || outlineMessage.includes('Unable') ? 'border-red-200 bg-red-50 text-red-700' : 'border-emerald-200 bg-emerald-50 text-emerald-700'"
            >
              {{ outlineMessage }}
            </p>

            <div v-if="modules.length" class="space-y-3">
              <article
                v-for="(module, moduleIndex) in modules"
                :key="module.id"
                class="overflow-hidden rounded-xl border transition"
                :class="selectedModuleId === module.id ? 'border-[#b9aeff] bg-[#f7f5ff]' : 'border-slate-200 bg-white'"
              >
                <button
                  type="button"
                  class="flex w-full items-center gap-3 px-4 py-3 text-left"
                  @click="selectedModuleId = module.id"
                >
                  <span class="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-white text-[12px] font-bold text-[#5b4cfa] ring-1 ring-slate-200">
                    {{ moduleIndex + 1 }}
                  </span>
                  <span class="min-w-0 flex-1">
                    <span class="block truncate text-[13px] font-bold text-slate-800">{{ module.title }}</span>
                    <span class="mt-0.5 block truncate text-[11px] font-medium text-slate-400">
                      {{ module.subTopics.length }} subtopic{{ module.subTopics.length === 1 ? '' : 's' }} · {{ module.contentDepth.toLowerCase() }} depth
                    </span>
                    <span v-if="module.interactionType !== 'NONE'" class="mt-1 inline-flex rounded-full bg-indigo-50 px-2 py-0.5 text-[10px] font-bold text-indigo-700">
                      {{ formatInteractionType(module.interactionType) }}
                    </span>
                  </span>
                  <span
                    class="rounded-full px-2 py-1 text-[10px] font-bold"
                    :class="module.subTopics.length ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-500'"
                  >
                    {{ module.subTopics.length ? 'Has content' : 'Empty' }}
                  </span>
                  <span
                    class="rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-[11px] font-bold text-slate-600 transition hover:border-[#b9aeff] hover:text-[#5b4cfa]"
                    @click.stop="openSubTopicForm(module.id)"
                  >
                    Add subtopic
                  </span>
                </button>

                <div v-if="editingSubTopicModuleId === module.id" class="border-t border-slate-200/70 bg-white px-4 py-3">
                  <div class="rounded-lg border border-[#d9d4ff] bg-[#faf9ff] p-3">
                    <div class="grid gap-3 lg:grid-cols-[280px_minmax(0,1fr)_auto]">
                      <input
                        v-model="subTopicTitle"
                        type="text"
                        placeholder="Subtopic title"
                        class="h-10 rounded-lg border border-slate-200 bg-white px-3 text-[12px] font-medium text-slate-700 outline-none focus:border-[#5b4cfa]/50"
                      />
                      <input
                        v-model="subTopicContent"
                        type="text"
                        placeholder="Short content or notes"
                        class="h-10 rounded-lg border border-slate-200 bg-white px-3 text-[12px] font-medium text-slate-700 outline-none focus:border-[#5b4cfa]/50"
                      />
                      <div class="flex gap-2">
                        <button
                          type="button"
                          class="h-10 rounded-lg bg-[#5b4cfa] px-3 text-[12px] font-bold text-white disabled:cursor-not-allowed disabled:opacity-60"
                          :disabled="subTopicSaving || !subTopicTitle.trim()"
                          @click="addManualSubTopic(module.id)"
                        >
                          {{ subTopicSaving ? 'Saving...' : 'Save' }}
                        </button>
                        <button
                          type="button"
                          class="h-10 rounded-lg border border-slate-200 bg-white px-3 text-[12px] font-bold text-slate-500"
                          @click="closeSubTopicForm"
                        >
                          Cancel
                        </button>
                      </div>
                    </div>
                    <p class="mt-2 text-[11px] font-medium text-slate-400">
                      Page range will attach as {{ selectedDocument ? `${pageStart}-${pageEnd}` : 'empty' }}.
                    </p>
                  </div>
                </div>

                <div v-if="module.subTopics.length" class="border-t border-slate-200/70 bg-white px-4 py-3">
                  <p v-if="module.interactionPrompt" class="mb-3 rounded-lg bg-indigo-50 px-3 py-2 text-[11px] font-medium leading-relaxed text-indigo-800">
                    {{ module.interactionPrompt }}
                  </p>
                  <div class="space-y-2">
                    <div
                      v-for="(subTopic, subTopicIndex) in module.subTopics"
                      :key="subTopic.id"
                      class="rounded-lg bg-slate-50 px-3 py-2"
                    >
                      <div class="grid grid-cols-[auto_minmax(0,1fr)_auto] gap-3">
                        <span class="mt-0.5 text-[11px] font-bold text-slate-400">{{ moduleIndex + 1 }}.{{ subTopicIndex + 1 }}</span>
                        <div class="min-w-0">
                          <p class="truncate text-[12px] font-bold text-slate-800">{{ subTopic.title }}</p>
                          <div class="lesson-preview mt-2 line-clamp-6 text-[12px] leading-relaxed text-slate-600" v-html="lessonHtml(subTopic)" />
                        <div v-if="subTopic.interactionType !== 'NONE' || subTopic.interactionPrompt || subTopic.interactionConfig" class="mt-2 flex flex-wrap items-start gap-2">
                            <span v-if="subTopic.interactionType !== 'NONE'" class="rounded-full bg-white px-2 py-0.5 text-[10px] font-bold text-indigo-700 ring-1 ring-indigo-100">
                              {{ formatInteractionType(subTopic.interactionType) }}
                            </span>
                          <span v-if="subTopic.interactionPrompt" class="min-w-0 flex-1 text-[10px] font-medium leading-relaxed text-indigo-700">
                            {{ subTopic.interactionPrompt }}
                          </span>
                          <span v-if="subTopic.interactionConfig" class="rounded-full bg-white px-2 py-0.5 text-[10px] font-bold text-slate-500 ring-1 ring-slate-200">
                            Config
                          </span>
                        </div>
                          <p v-if="subTopic.pageStart && subTopic.pageEnd" class="mt-1 text-[10px] font-bold uppercase tracking-[0.1em] text-slate-400">
                            Pages {{ subTopic.pageStart }}-{{ subTopic.pageEnd }}
                          </p>
                        </div>
                        <button
                          type="button"
                          class="h-8 rounded-lg border border-slate-200 bg-white px-3 text-[11px] font-bold text-slate-600 transition hover:border-[#b9aeff] hover:text-[#5b4cfa]"
                          @click="openLessonEditor(subTopic)"
                        >
                          Edit
                        </button>
                      </div>
                      <div v-if="editingLessonSubTopicId === subTopic.id" class="mt-3">
                        <SubTopicLessonEditor
                          :sub-topic="subTopic"
                          @cancel="closeLessonEditor"
                          @error="subTopicMessage = $event"
                          @save="saveLessonSubTopic(subTopic, $event)"
                        />
                      </div>
                    </div>
                  </div>
                </div>
              </article>
            </div>

            <div v-else class="rounded-xl border border-dashed border-slate-200 py-10 text-center">
              <p class="text-[13px] font-semibold text-slate-500">No modules yet</p>
              <p class="mt-1 text-[12px] text-slate-400">Add a module, then generate subtopics from selected PDF pages.</p>
            </div>
          </section>

          <section class="grid gap-5 xl:grid-cols-[340px_minmax(0,1fr)]">
            <aside class="space-y-5">
              <div class="rounded-xl bg-white p-4 ring-1 ring-slate-900/[0.06]">
                <div class="mb-3 flex items-center justify-between">
                  <h2 class="text-[13px] font-bold text-slate-800">PDF sources</h2>
                  <span class="text-[11px] font-semibold text-slate-400">{{ documents.length }}</span>
                </div>
                <div v-if="documents.length" class="space-y-2">
                  <button
                    v-for="document in documents"
                    :key="document.id"
                    type="button"
                    class="w-full rounded-lg border px-3 py-2 text-left transition"
                    :class="selectedDocumentId === document.id ? 'border-[#5b4cfa]/40 bg-[#5b4cfa]/5' : 'border-slate-200 bg-white hover:bg-slate-50'"
                    @click="selectDocument(document)"
                  >
                    <p class="truncate text-[12px] font-semibold text-slate-800">{{ document.displayName }}</p>
                    <p class="mt-1 text-[11px] text-slate-400">{{ document.pageCount ?? 0 }} pages · {{ formatFileSize(document.fileSizeBytes) }}</p>
                  </button>
                </div>
                <p v-else class="rounded-lg border border-dashed border-slate-200 py-8 text-center text-[12px] text-slate-400">
                  No PDFs registered.
                </p>
              </div>

              <div class="rounded-xl bg-white p-4 ring-1 ring-slate-900/[0.06]">
                <h2 class="mb-3 text-[13px] font-bold text-slate-800">AI generation</h2>
                <label class="mb-1 block text-[11px] font-semibold text-slate-500">Module</label>
                <select v-if="modules.length" v-model="selectedModuleId" class="mb-3 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-[12px] text-slate-700">
                  <option v-for="module in modules" :key="module.id" :value="module.id">{{ module.title }}</option>
                </select>
                <div v-else class="mb-3 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-[12px] text-amber-800">
                  Add a module in the Course outline section above before generating AI subtopics.
                </div>
                <label class="mb-1 block text-[11px] font-semibold text-slate-500">Requirements</label>
                <textarea v-model="aiRequirements" rows="4" class="w-full resize-none rounded-lg border border-slate-200 px-3 py-2 text-[12px] text-slate-700 outline-none focus:border-[#5b4cfa]/50" />
                <button
                  type="button"
                  class="mt-3 w-full rounded-lg bg-[#5b4cfa] px-3 py-2 text-[12px] font-semibold text-white disabled:cursor-not-allowed disabled:opacity-60"
                  :disabled="aiSubmitting || aiPolling || !selectedModuleId || !selectedDocument"
                  @click="generateFromSelection"
                >
                  {{ aiSubmitting || aiPolling ? 'Generating...' : 'Generate from selection' }}
                </button>
                <p v-if="aiMessage" class="mt-2 text-[12px] font-medium" :class="aiMessage.includes('Unable') ? 'text-red-600' : 'text-emerald-700'">
                  {{ aiMessage }}
                </p>
              </div>
            </aside>

            <section class="min-w-0 rounded-xl bg-white p-4 ring-1 ring-slate-900/[0.06]">
              <div class="mb-4 flex flex-wrap items-end gap-3">
                <div>
                  <label class="mb-1 block text-[11px] font-semibold text-slate-500">Start page</label>
                  <input v-model.number="pageStart" type="number" min="1" class="w-24 rounded-lg border border-slate-200 px-3 py-2 text-[12px]" @change="clampPageRange" />
                </div>
                <div>
                  <label class="mb-1 block text-[11px] font-semibold text-slate-500">End page</label>
                  <input v-model.number="pageEnd" type="number" min="1" class="w-24 rounded-lg border border-slate-200 px-3 py-2 text-[12px]" @change="clampPageRange" />
                </div>
                <button
                  type="button"
                  class="rounded-lg bg-slate-900 px-4 py-2 text-[12px] font-semibold text-white disabled:cursor-not-allowed disabled:opacity-60"
                  :disabled="previewLoading || !selectedDocument"
                  @click="loadPreview"
                >
                  {{ previewLoading ? 'Loading...' : 'Preview' }}
                </button>
                <p v-if="selectedDocument" class="text-[11px] text-slate-400">Max 100 pages per preview.</p>
              </div>

              <p v-if="previewError" class="mb-4 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-[12px] font-medium text-red-700">
                {{ previewError }}
              </p>

              <div v-if="preview" class="grid gap-4 lg:grid-cols-2">
                <iframe :src="preview.fileUrl" class="h-[620px] w-full rounded-lg border border-slate-200" title="PDF preview" />
                <div class="h-[620px] overflow-y-auto rounded-lg border border-slate-200 bg-slate-50 p-4">
                  <div v-for="page in preview.pages" :key="page.pageNumber" class="mb-4 last:mb-0">
                    <p class="mb-2 text-[11px] font-bold uppercase tracking-[0.12em] text-slate-400">Page {{ page.pageNumber }}</p>
                    <p class="whitespace-pre-wrap text-[12px] leading-relaxed text-slate-700">{{ page.text || 'No extractable text on this page.' }}</p>
                  </div>
                </div>
              </div>

              <div v-else class="flex min-h-[360px] items-center justify-center rounded-lg border border-dashed border-slate-200 text-[12px] text-slate-400">
                Select a PDF and preview a page range.
              </div>
            </section>
          </section>
        </template>
      </main>
    </div>
  </div>
</template>
