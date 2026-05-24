<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { AxiosError } from 'axios'
import SubTopicLessonEditor from '@/features/courses/components/Admin/SubTopicLessonEditor.vue'
import {
  createCourseModule,
  createModuleSubTopic,
  getAiGenerationLog,
  listAiGenerationLogs,
  updateModuleSubTopic,
  type AdminDocumentSourceDto,
  type AdminModuleDto,
  type AdminSubTopicDto,
  type InteractionType,
} from '@/features/courses/services/adminCourses'

const props = defineProps<{
  courseId: string
  modules: AdminModuleDto[]
  selectedDocument: AdminDocumentSourceDto | null
  pageStart: number
  pageEnd: number
}>()

const emit = defineEmits<{
  'module-selected': [id: number | null]
  'modules-updated': [modules: AdminModuleDto[]]
  reload: []
}>()

const localModules = ref<AdminModuleDto[]>([...props.modules])

watch(
  () => props.modules,
  (newModules) => { localModules.value = [...newModules] },
)

const subTopicCount = computed(() =>
  localModules.value.reduce((total, m) => total + m.subTopics.length, 0),
)

const selectedModuleId = ref<number | null>(props.modules[0]?.id ?? null)
const newModuleTitle = ref('')
const moduleCreating = ref(false)
const moduleMessage = ref('')
const editingSubTopicModuleId = ref<number | null>(null)
const subTopicTitle = ref('')
const subTopicContent = ref('')
const subTopicSaving = ref(false)
const subTopicMessage = ref('')
const editingLessonSubTopicId = ref<number | null>(null)
const outlineMessage = ref('')
const outlinePolling = ref(false)

watch(selectedModuleId, (id) => emit('module-selected', id), { immediate: true })

onMounted(() => {
  void pollLatestCourseOutlineGeneration()
})

function syncModules() {
  emit('modules-updated', [...localModules.value])
}

async function addModuleForGeneration() {
  const title = newModuleTitle.value.trim()
  if (!title) return
  moduleCreating.value = true
  moduleMessage.value = ''
  try {
    const module = await createCourseModule(props.courseId, {
      title,
      sortOrder: localModules.value.length,
      contentDepth: 'MEDIUM',
    })
    localModules.value = [...localModules.value, module]
    selectedModuleId.value = module.id
    newModuleTitle.value = ''
    moduleMessage.value = 'Module created.'
    syncModules()
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
  if (!title) return
  const moduleIndex = localModules.value.findIndex((m) => m.id === moduleId)
  if (moduleIndex === -1) return

  subTopicSaving.value = true
  subTopicMessage.value = ''
  try {
    const module = localModules.value[moduleIndex]
    const subTopic = await createModuleSubTopic(moduleId, {
      title,
      content: subTopicContent.value,
      sortOrder: module.subTopics.length,
      pageStart: props.selectedDocument ? props.pageStart : null,
      pageEnd: props.selectedDocument ? props.pageEnd : null,
    })
    const updatedModules = [...localModules.value]
    updatedModules[moduleIndex] = {
      ...module,
      subTopics: [...module.subTopics, subTopic],
    }
    localModules.value = updatedModules
    closeSubTopicForm()
    subTopicMessage.value = 'Subtopic added.'
    syncModules()
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
    localModules.value = localModules.value.map((module) => {
      if (module.id !== updated.moduleId) return module
      return {
        ...module,
        subTopics: module.subTopics.map((item) => (item.id === updated.id ? updated : item)),
      }
    })
    editingLessonSubTopicId.value = null
    subTopicMessage.value = 'Subtopic updated.'
    syncModules()
  } catch (error) {
    subTopicMessage.value = getErrorMessage(error, 'Unable to update subtopic.')
  } finally {
    subTopicSaving.value = false
  }
}

async function pollLatestCourseOutlineGeneration() {
  if (outlinePolling.value) return
  try {
    const logs = await listAiGenerationLogs(props.courseId)
    const outlineLog = logs.find(
      (log) => log.type === 'COURSE_OUTLINE' && ['PENDING', 'RUNNING'].includes(log.status),
    )
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
        outlineMessage.value = 'AI course outline generation completed.'
        emit('reload')
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

function lessonHtml(subTopic: AdminSubTopicDto) {
  if (subTopic.contentHtml) return subTopic.contentHtml
  return textToHtml(subTopic.content || '')
}

function textToHtml(value: string) {
  return value
    .split(/\n{2,}/)
    .map((p) => p.trim())
    .filter(Boolean)
    .map((p) => `<p>${escapeHtml(p).replace(/\n/g, '<br>')}</p>`)
    .join('')
}

function escapeHtml(value: string) {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
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
  <section class="mb-5 rounded-[18px] bg-lm-surface border-2 border-lm-line p-5 shadow-stamp-sm">
    <!-- Header + add module -->
    <div class="mb-4 flex items-center justify-between gap-4">
      <div>
        <h2 class="font-display text-[14px] font-bold text-lm-ink">Course outline</h2>
        <p class="mt-1 font-mono text-[12px] font-medium text-lm-ink-3">
          {{ localModules.length }} module{{ localModules.length === 1 ? '' : 's' }} · {{ subTopicCount }} subtopic{{ subTopicCount === 1 ? '' : 's' }}
        </p>
      </div>
      <div class="flex min-w-0 items-center gap-2">
        <input
          v-model="newModuleTitle"
          type="text"
          placeholder="New module title"
          class="h-9 w-56 rounded-lg border-2 border-lm-line-soft bg-lm-bg-soft px-3 text-[12px] font-medium text-lm-ink outline-none transition placeholder:text-lm-ink-3 focus:border-lm-line focus:bg-lm-surface focus:ring-2 focus:ring-lm-yellow/40"
        />
        <button
          type="button"
          class="h-9 rounded-lg border-2 border-lm-ink bg-lm-ink px-3 text-[12px] font-bold text-lm-bg transition hover:opacity-80 disabled:cursor-not-allowed disabled:opacity-50"
          :disabled="moduleCreating || !newModuleTitle.trim()"
          @click="addModuleForGeneration"
        >
          {{ moduleCreating ? 'Adding...' : 'Add module' }}
        </button>
      </div>
    </div>

    <!-- Status messages -->
    <p v-if="moduleMessage" class="mb-3 text-[12px] font-medium" :class="moduleMessage.includes('Unable') ? 'text-lm-red' : 'text-lm-green'">
      {{ moduleMessage }}
    </p>
    <p v-if="subTopicMessage" class="mb-3 text-[12px] font-medium" :class="subTopicMessage.includes('Unable') ? 'text-lm-red' : 'text-lm-green'">
      {{ subTopicMessage }}
    </p>
    <p
      v-if="outlineMessage"
      class="mb-3 rounded-lg border-2 px-3 py-2 text-[12px] font-medium"
      :class="outlineMessage.includes('failed') || outlineMessage.includes('Unable') ? 'border-lm-red bg-lm-red-soft text-lm-red' : 'border-lm-green bg-lm-green-soft text-lm-green'"
    >
      {{ outlineMessage }}
    </p>

    <!-- Module list -->
    <div v-if="localModules.length" class="space-y-3">
      <article
        v-for="(module, moduleIndex) in localModules"
        :key="module.id"
        class="overflow-hidden rounded-xl border-2 transition-all duration-200"
        :class="selectedModuleId === module.id ? 'border-lm-line bg-lm-yellow/10' : 'border-lm-line-soft bg-lm-surface'"
      >
        <!-- Module row -->
        <button
          type="button"
          class="flex w-full items-center gap-3 px-4 py-3 text-left"
          @click="selectedModuleId = module.id"
        >
          <span class="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg border-2 border-lm-line bg-lm-yellow font-display text-[12px] font-bold text-lm-ink shadow-stamp-sm">
            {{ moduleIndex + 1 }}
          </span>
          <span class="min-w-0 flex-1">
            <span class="block truncate font-display text-[13px] font-bold text-lm-ink">{{ module.title }}</span>
            <span class="mt-0.5 block truncate font-mono text-[11px] font-medium text-lm-ink-3">
              {{ module.subTopics.length }} subtopic{{ module.subTopics.length === 1 ? '' : 's' }} · {{ module.contentDepth.toLowerCase() }} depth
            </span>
            <span v-if="module.interactionType !== 'NONE'" class="mt-1 inline-flex rounded-full border border-lm-line-soft bg-lm-purple-soft px-2 py-0.5 font-mono text-[10px] font-bold text-lm-purple">
              {{ formatInteractionType(module.interactionType) }}
            </span>
          </span>
          <span
            class="rounded-full border-2 border-lm-line px-2 py-1 font-mono text-[10px] font-bold shadow-stamp-sm"
            :class="module.subTopics.length ? 'bg-lm-green-soft text-lm-green' : 'bg-lm-bg-soft text-lm-ink-3'"
          >
            {{ module.subTopics.length ? 'Has content' : 'Empty' }}
          </span>
          <span
            class="rounded-lg border-2 border-lm-line bg-lm-surface px-3 py-1.5 font-mono text-[11px] font-bold text-lm-ink shadow-stamp-sm transition-all duration-200 hover:-translate-y-px hover:shadow-stamp-md"
            @click.stop="openSubTopicForm(module.id)"
          >
            Add subtopic
          </span>
        </button>

        <!-- Add subtopic inline form -->
        <div v-if="editingSubTopicModuleId === module.id" class="border-t-2 border-lm-line-soft bg-lm-bg-soft px-4 py-3">
          <div class="rounded-lg border-2 border-lm-line bg-lm-surface p-3 shadow-stamp-sm">
            <div class="grid gap-3 lg:grid-cols-[280px_minmax(0,1fr)_auto]">
              <input
                v-model="subTopicTitle"
                type="text"
                placeholder="Subtopic title"
                class="h-10 rounded-lg border-2 border-lm-line-soft bg-lm-bg-soft px-3 text-[12px] font-medium text-lm-ink outline-none focus:border-lm-line focus:bg-lm-surface"
              />
              <input
                v-model="subTopicContent"
                type="text"
                placeholder="Short content or notes"
                class="h-10 rounded-lg border-2 border-lm-line-soft bg-lm-bg-soft px-3 text-[12px] font-medium text-lm-ink outline-none focus:border-lm-line focus:bg-lm-surface"
              />
              <div class="flex gap-2">
                <button
                  type="button"
                  class="h-10 rounded-lg border-2 border-lm-ink bg-lm-ink px-3 text-[12px] font-bold text-lm-bg disabled:cursor-not-allowed disabled:opacity-50"
                  :disabled="subTopicSaving || !subTopicTitle.trim()"
                  @click="addManualSubTopic(module.id)"
                >
                  {{ subTopicSaving ? 'Saving...' : 'Save' }}
                </button>
                <button
                  type="button"
                  class="h-10 rounded-lg border-2 border-lm-line bg-lm-surface px-3 text-[12px] font-bold text-lm-ink shadow-stamp-sm"
                  @click="closeSubTopicForm"
                >
                  Cancel
                </button>
              </div>
            </div>
            <p class="mt-2 font-mono text-[11px] font-medium text-lm-ink-3">
              Page range will attach as {{ selectedDocument ? `${pageStart}–${pageEnd}` : 'empty' }}.
            </p>
          </div>
        </div>

        <!-- Subtopics list -->
        <div v-if="module.subTopics.length" class="border-t-2 border-lm-line-soft bg-lm-surface px-4 py-3">
          <p v-if="module.interactionPrompt" class="mb-3 rounded-lg border border-lm-line-soft bg-lm-purple-soft px-3 py-2 text-[11px] font-medium leading-relaxed text-lm-purple">
            {{ module.interactionPrompt }}
          </p>
          <div class="space-y-2">
            <div
              v-for="(subTopic, subTopicIndex) in module.subTopics"
              :key="subTopic.id"
              class="rounded-lg border border-lm-line-soft bg-lm-bg-soft px-3 py-2"
            >
              <div class="grid grid-cols-[auto_minmax(0,1fr)_auto] gap-3">
                <span class="mt-0.5 font-mono text-[11px] font-bold text-lm-ink-3">{{ moduleIndex + 1 }}.{{ subTopicIndex + 1 }}</span>
                <div class="min-w-0">
                  <p class="truncate font-display text-[12px] font-bold text-lm-ink">{{ subTopic.title }}</p>
                  <div class="lesson-preview mt-2 line-clamp-6 text-[12px] leading-relaxed text-lm-ink-2" v-html="lessonHtml(subTopic)" />
                  <div v-if="subTopic.interactionType !== 'NONE' || subTopic.interactionPrompt || subTopic.interactionConfig" class="mt-2 flex flex-wrap items-start gap-2">
                    <span v-if="subTopic.interactionType !== 'NONE'" class="rounded-full border border-lm-line-soft bg-lm-surface px-2 py-0.5 font-mono text-[10px] font-bold text-lm-purple">
                      {{ formatInteractionType(subTopic.interactionType) }}
                    </span>
                    <span v-if="subTopic.interactionPrompt" class="min-w-0 flex-1 text-[10px] font-medium leading-relaxed text-lm-purple">
                      {{ subTopic.interactionPrompt }}
                    </span>
                    <span v-if="subTopic.interactionConfig" class="rounded-full border border-lm-line-soft bg-lm-surface px-2 py-0.5 font-mono text-[10px] font-bold text-lm-ink-3">
                      Config
                    </span>
                  </div>
                  <p v-if="subTopic.pageStart && subTopic.pageEnd" class="mt-1 font-mono text-[10px] font-bold uppercase tracking-[0.1em] text-lm-ink-3">
                    Pages {{ subTopic.pageStart }}–{{ subTopic.pageEnd }}
                  </p>
                </div>
                <button
                  type="button"
                  class="h-8 rounded-lg border-2 border-lm-line bg-lm-surface px-3 font-mono text-[11px] font-bold text-lm-ink shadow-stamp-sm transition-all duration-200 hover:-translate-y-px hover:shadow-stamp-md"
                  @click="openLessonEditor(subTopic)"
                >
                  Edit
                </button>
              </div>

              <!-- Inline lesson editor -->
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

    <!-- Empty state -->
    <div v-else class="rounded-xl border-2 border-dashed border-lm-line-soft py-10 text-center">
      <p class="font-display text-[13px] font-semibold text-lm-ink-2">No modules yet</p>
      <p class="mt-1 text-[12px] text-lm-ink-3">Add a module, then generate subtopics from selected PDF pages.</p>
    </div>
  </section>
</template>
