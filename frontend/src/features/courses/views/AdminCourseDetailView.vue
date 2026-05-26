<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { AxiosError } from 'axios'
import { useRoute, useRouter } from 'vue-router'
import AdminNavbar from '@/features/courses/components/Admin/AdminNavbar.vue'
import AdminCourseOutline from '@/features/courses/components/Admin/AdminCourseOutline.vue'
import { DEFAULT_COVER_ID, getCoverPreset } from '@/features/courses/constants/courseCoverPresets'
import {
  getAdminCourse,
  previewCourseDocument,
  requestModuleAiGeneration,
  getAiGenerationLog,
  type AdminCourseDetailDto,
  type AdminDocumentSourceDto,
  type AdminModuleDto,
  type DocumentPreviewDto,
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
const aiRequirements = ref('')
const aiSubmitting = ref(false)
const aiPolling = ref(false)
const aiMessage = ref('')

const cover = computed(() => getCoverPreset(DEFAULT_COVER_ID))
const documents = computed(() => course.value?.documentSources ?? [])
const modules = computed(() => course.value?.modules ?? [])
const selectedDocument = computed(() =>
  documents.value.find((d) => d.id === selectedDocumentId.value) ?? null,
)

onMounted(loadCourse)

async function loadCourse() {
  loading.value = true
  loadError.value = ''
  try {
    const data = await getAdminCourse(courseId.value)
    course.value = data
    selectedDocumentId.value = data.documentSources[0]?.id ?? null
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
    aiMessage.value = 'AI generation is still running. Refresh this page in a moment.'
  } finally {
    aiPolling.value = false
  }
}

function selectDocument(document: AdminDocumentSourceDto) {
  selectedDocumentId.value = document.id
  preview.value = null
  previewError.value = ''
  pageStart.value = 1
  pageEnd.value = Math.min(10, document.pageCount ?? 1)
}

function clampPageRange() {
  const max = selectedDocument.value?.pageCount ?? 1
  pageStart.value = Math.min(Math.max(1, pageStart.value), max)
  pageEnd.value = Math.min(Math.max(pageStart.value, pageEnd.value), max)
}

function onModulesUpdated(newModules: AdminModuleDto[]) {
  if (course.value) course.value.modules = newModules
}

function wait(ms: number) {
  return new Promise((resolve) => window.setTimeout(resolve, ms))
}

function formatFileSize(bytes: number) {
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
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
      <!-- Breadcrumb header -->
      <div class="flex shrink-0 items-center justify-between border-b-2 border-lm-line bg-lm-surface px-7 py-3">
        <div class="flex items-center gap-1.5 text-[12px]">
          <button class="font-semibold text-lm-ink-3 transition hover:text-lm-ink" type="button" @click="router.push('/admin/courses')">
            Courses
          </button>
          <span class="text-lm-line-soft">/</span>
          <span class="truncate font-bold text-lm-ink">{{ course?.title ?? 'Course' }}</span>
        </div>
        <button
          class="rounded-lg border-2 border-lm-line bg-lm-surface px-3 py-1.5 text-[12px] font-semibold text-lm-ink shadow-stamp-sm transition-all duration-200 hover:-translate-y-px hover:shadow-stamp-md"
          type="button"
          @click="loadCourse"
        >
          Refresh
        </button>
      </div>

      <main class="flex-1 overflow-y-auto px-7 py-6 relative">
        <div class="absolute inset-0 bg-dot-grid opacity-40 pointer-events-none" />

        <div class="relative">
          <div v-if="loadError" class="mb-4 rounded-xl border-2 border-lm-red bg-lm-red-soft px-4 py-3 text-[13px] font-medium text-lm-red">
            {{ loadError }}
          </div>

          <div v-if="loading" class="rounded-[18px] border-2 border-lm-line bg-lm-surface px-5 py-4 text-[13px] text-lm-ink-3 shadow-stamp-sm">
            Loading course...
          </div>

          <template v-else-if="course">
            <!-- Course summary card -->
            <section class="mb-5 rounded-[18px] border-2 border-lm-line bg-lm-surface px-5 py-4 shadow-stamp-sm">
              <div class="flex items-center gap-4">
                <div
                  class="flex h-14 w-14 shrink-0 items-center justify-center rounded-xl border-2 border-lm-line font-display text-2xl font-bold shadow-stamp-sm"
                  :class="[cover.bgClass, cover.textClass]"
                >
                  {{ cover.symbol }}
                </div>
                <div class="min-w-0">
                  <h1 class="font-display text-[18px] font-bold text-lm-ink">{{ course.title }}</h1>
                  <p class="mt-1 line-clamp-2 text-[12px] leading-relaxed text-lm-ink-2">{{ course.description }}</p>
                </div>
              </div>
            </section>

            <!-- Course outline (extracted component) -->
            <AdminCourseOutline
              :course-id="courseId"
              :modules="modules"
              :selected-document="selectedDocument"
              :page-start="pageStart"
              :page-end="pageEnd"
              @module-selected="selectedModuleId = $event"
              @modules-updated="onModulesUpdated"
              @reload="loadCourse"
            />

            <!-- PDF + AI section -->
            <section class="grid gap-5 xl:grid-cols-[340px_minmax(0,1fr)]">
              <aside class="space-y-5">
                <!-- PDF sources -->
                <div class="rounded-[18px] border-2 border-lm-line bg-lm-surface p-4 shadow-stamp-sm">
                  <div class="mb-3 flex items-center justify-between">
                    <h2 class="font-display text-[13px] font-bold text-lm-ink">PDF sources</h2>
                    <span class="font-mono text-[11px] font-semibold text-lm-ink-3">{{ documents.length }}</span>
                  </div>
                  <div v-if="documents.length" class="space-y-2">
                    <button
                      v-for="document in documents"
                      :key="document.id"
                      type="button"
                      class="w-full rounded-lg border-2 px-3 py-2 text-left transition-all duration-200"
                      :class="selectedDocumentId === document.id ? 'border-lm-line bg-lm-yellow/20 shadow-stamp-sm' : 'border-lm-line-soft bg-lm-bg-soft hover:border-lm-line hover:bg-lm-surface'"
                      @click="selectDocument(document)"
                    >
                      <p class="truncate font-display text-[12px] font-semibold text-lm-ink">{{ document.displayName }}</p>
                      <p class="mt-1 font-mono text-[11px] text-lm-ink-3">{{ document.pageCount ?? 0 }} pages · {{ formatFileSize(document.fileSizeBytes) }}</p>
                    </button>
                  </div>
                  <p v-else class="rounded-lg border-2 border-dashed border-lm-line-soft py-8 text-center text-[12px] text-lm-ink-3">
                    No PDFs registered.
                  </p>
                </div>

                <!-- AI generation -->
                <div class="rounded-[18px] border-2 border-lm-line bg-lm-surface p-4 shadow-stamp-sm">
                  <h2 class="mb-3 font-display text-[13px] font-bold text-lm-ink">AI generation</h2>
                  <label class="mb-1 block font-mono text-[11px] font-semibold text-lm-ink-3">Module</label>
                  <select
                    v-if="modules.length"
                    v-model="selectedModuleId"
                    class="mb-3 w-full rounded-lg border-2 border-lm-line-soft bg-lm-bg-soft px-3 py-2 text-[12px] text-lm-ink outline-none focus:border-lm-line"
                  >
                    <option v-for="module in modules" :key="module.id" :value="module.id">{{ module.title }}</option>
                  </select>
                  <div v-else class="mb-3 rounded-lg border-2 border-lm-yellow bg-lm-yellow/20 px-3 py-2 text-[12px] text-lm-ink">
                    Add a module in the Course outline section above before generating AI subtopics.
                  </div>
                  <label class="mb-1 block font-mono text-[11px] font-semibold text-lm-ink-3">Requirements</label>
                  <textarea
                    v-model="aiRequirements"
                    rows="4"
                    class="w-full resize-none rounded-lg border-2 border-lm-line-soft bg-lm-bg-soft px-3 py-2 text-[12px] text-lm-ink outline-none focus:border-lm-line focus:bg-lm-surface"
                  />
                  <button
                    type="button"
                    class="mt-3 w-full rounded-lg border-2 border-lm-ink bg-lm-ink px-3 py-2 text-[12px] font-semibold text-lm-bg transition hover:opacity-80 disabled:cursor-not-allowed disabled:opacity-50"
                    :disabled="aiSubmitting || aiPolling || !selectedModuleId || !selectedDocument"
                    @click="generateFromSelection"
                  >
                    {{ aiSubmitting || aiPolling ? 'Generating...' : 'Generate from selection' }}
                  </button>
                  <p v-if="aiMessage" class="mt-2 text-[12px] font-medium" :class="aiMessage.includes('Unable') ? 'text-lm-red' : 'text-lm-green'">
                    {{ aiMessage }}
                  </p>
                </div>
              </aside>

              <!-- PDF preview -->
              <section class="min-w-0 rounded-[18px] border-2 border-lm-line bg-lm-surface p-4 shadow-stamp-sm">
                <div class="mb-4 flex flex-wrap items-end gap-3">
                  <div>
                    <label class="mb-1 block font-mono text-[11px] font-semibold text-lm-ink-3">Start page</label>
                    <input v-model.number="pageStart" type="number" min="1" class="w-24 rounded-lg border-2 border-lm-line-soft bg-lm-bg-soft px-3 py-2 text-[12px] text-lm-ink outline-none focus:border-lm-line" @change="clampPageRange" />
                  </div>
                  <div>
                    <label class="mb-1 block font-mono text-[11px] font-semibold text-lm-ink-3">End page</label>
                    <input v-model.number="pageEnd" type="number" min="1" class="w-24 rounded-lg border-2 border-lm-line-soft bg-lm-bg-soft px-3 py-2 text-[12px] text-lm-ink outline-none focus:border-lm-line" @change="clampPageRange" />
                  </div>
                  <button
                    type="button"
                    class="rounded-lg border-2 border-lm-ink bg-lm-ink px-4 py-2 text-[12px] font-semibold text-lm-bg transition hover:opacity-80 disabled:cursor-not-allowed disabled:opacity-50"
                    :disabled="previewLoading || !selectedDocument"
                    @click="loadPreview"
                  >
                    {{ previewLoading ? 'Loading...' : 'Preview' }}
                  </button>
                  <p v-if="selectedDocument" class="font-mono text-[11px] text-lm-ink-3">Max 100 pages per preview.</p>
                </div>

                <p v-if="previewError" class="mb-4 rounded-lg border-2 border-lm-red bg-lm-red-soft px-3 py-2 text-[12px] font-medium text-lm-red">
                  {{ previewError }}
                </p>

                <div v-if="preview" class="grid gap-4 lg:grid-cols-2">
                  <iframe :src="preview.fileUrl" class="h-[620px] w-full rounded-lg border-2 border-lm-line shadow-stamp-sm" title="PDF preview" />
                  <div class="h-[620px] overflow-y-auto rounded-lg border-2 border-lm-line bg-lm-bg-soft p-4 shadow-stamp-sm">
                    <div v-for="page in preview.pages" :key="page.pageNumber" class="mb-4 last:mb-0">
                      <p class="mb-2 font-mono text-[11px] font-bold uppercase tracking-[0.12em] text-lm-ink-3">Page {{ page.pageNumber }}</p>
                      <p class="whitespace-pre-wrap text-[12px] leading-relaxed text-lm-ink-2">{{ page.text || 'No extractable text on this page.' }}</p>
                    </div>
                  </div>
                </div>

                <div v-else class="flex min-h-[360px] items-center justify-center rounded-lg border-2 border-dashed border-lm-line-soft font-mono text-[12px] text-lm-ink-3">
                  Select a PDF and preview a page range.
                </div>
              </section>
            </section>
          </template>
        </div>
      </main>
    </div>
  </div>
</template>
