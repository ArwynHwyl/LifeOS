<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'

type Comment = {
  id: string
  authorId: string
  authorName: string
  text: string
  createdAt: string
}
import { AxiosError } from 'axios'
import SubTopicLessonEditor from '@/features/courses/components/Admin/SubTopicLessonEditor.vue'
import {
  createCourseModule,
  createModuleSubTopic,
  deleteAdminSubTopic,
  getAiGenerationLog,
  listAiGenerationLogs,
  updateModuleSubTopic,
  type AdminDocumentSourceDto,
  type AdminModuleDto,
  type AdminSubTopicDto,
  type BackendCourseStatus,
  type InteractionType,
} from '@/features/courses/services/adminCourses'

const props = defineProps<{
  courseId: string
  courseStatus: BackendCourseStatus
  modules: AdminModuleDto[]
  selectedDocument: AdminDocumentSourceDto | null
  pageStart: number
  pageEnd: number
}>()

const emit = defineEmits<{
  'module-selected': [id: number | null]
  'module-edit': [id: number]
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

async function removeSubTopic(moduleId: number, subTopicId: number) {
  subTopicSaving.value = true
  subTopicMessage.value = ''
  try {
    await deleteAdminSubTopic(subTopicId)
    const moduleIndex = localModules.value.findIndex((m) => m.id === moduleId)
    if (moduleIndex !== -1) {
      localModules.value[moduleIndex] = {
        ...localModules.value[moduleIndex],
        subTopics: localModules.value[moduleIndex].subTopics.filter((s) => s.id !== subTopicId),
      }
      syncModules()
    }
  } catch (error) {
    subTopicMessage.value = getErrorMessage(error, 'Unable to delete subtopic.')
  } finally {
    subTopicSaving.value = false
  }
}

function closeLessonEditor() {
  editingLessonSubTopicId.value = null
}

async function saveLessonSubTopic(subTopic: AdminSubTopicDto, payload: {
  title: string
  content: string
  mascotPrompt: string | null
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
      mascotPrompt: payload.mascotPrompt,
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

const commentsMap = ref<Record<string, Comment[]>>({})
const discussionOpenMap = ref<Record<number, boolean>>({})
const confirmEndMap = ref<Record<number, boolean>>({})
const newCommentMap = ref<Record<number, string>>({})
const editingCommentId = ref<string | null>(null)
const editCommentText = ref('')

const currentUserId = computed(() => {
  const raw = localStorage.getItem('authUser')
  if (!raw) return 'admin'
  try { return (JSON.parse(raw) as { userId?: string }).userId ?? 'admin' } catch { return 'admin' }
})

const currentUserName = computed(() => {
  const raw = localStorage.getItem('authUser')
  if (!raw) return 'Admin'
  try { return (JSON.parse(raw) as { username?: string }).username ?? 'Admin' } catch { return 'Admin' }
})

const AVATAR_PALETTE = ['bg-violet-500', 'bg-blue-500', 'bg-teal-500', 'bg-amber-500', 'bg-rose-500', 'bg-indigo-500']
function avatarBg(name: string): string {
  let h = 0
  for (const c of name) h = (h * 31 + c.charCodeAt(0)) & 0xffff
  return AVATAR_PALETTE[h % AVATAR_PALETTE.length]
}
function initials(name: string): string {
  return name.split(' ').map(w => w[0] ?? '').slice(0, 2).join('').toUpperCase()
}

function openDiscussion(modId: number) {
  discussionOpenMap.value[modId] = true
  confirmEndMap.value[modId] = false
}
function addComment(modId: number) {
  const text = (newCommentMap.value[modId] ?? '').trim()
  if (!text) return
  const key = String(modId)
  if (!commentsMap.value[key]) commentsMap.value[key] = []
  commentsMap.value[key].push({ id: `c-${Date.now()}`, authorId: currentUserId.value, authorName: currentUserName.value, text, createdAt: 'just now' })
  newCommentMap.value[modId] = ''
}
function endDiscussion(modId: number) {
  commentsMap.value[String(modId)] = []
  discussionOpenMap.value[modId] = false
  confirmEndMap.value[modId] = false
}
function startEditComment(comment: Comment) {
  editingCommentId.value = comment.id
  editCommentText.value = comment.text
}
function saveEditComment(modId: number) {
  const key = String(modId)
  if (!editingCommentId.value || !editCommentText.value.trim()) return
  const list = commentsMap.value[key]
  if (!list) return
  const i = list.findIndex(c => c.id === editingCommentId.value)
  if (i !== -1) list[i] = { ...list[i], text: editCommentText.value.trim() }
  editingCommentId.value = null
  editCommentText.value = ''
}
function cancelEditComment() {
  editingCommentId.value = null
  editCommentText.value = ''
}
function deleteComment(modId: number, commentId: string) {
  const key = String(modId)
  if (!commentsMap.value[key]) return
  commentsMap.value[key] = commentsMap.value[key].filter(c => c.id !== commentId)
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
  <section class="mb-5 rounded-[18px] border-2 border-lm-line bg-lm-surface p-5 shadow-stamp-sm">
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
          class="h-9 w-56 rounded-lg border-2 border-lm-line-soft bg-lm-bg-soft px-3 text-[12px] text-lm-ink outline-none placeholder:text-lm-ink-3 focus:border-lm-line"
        />
        <button
          type="button"
          class="h-9 rounded-lg border-2 border-lm-ink bg-lm-ink px-4 text-[12px] font-bold text-lm-bg shadow-stamp-sm transition hover:-translate-y-px hover:shadow-stamp-md disabled:cursor-not-allowed disabled:opacity-50"
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
            <span v-if="module.interactionType !== 'NONE'" class="mt-1 inline-flex rounded-full border border-lm-line-soft bg-lm-purple-soft px-2 py-0.5 font-mono text-[9px] font-bold text-lm-purple">
              {{ formatInteractionType(module.interactionType) }}
            </span>
          </span>
          <button
            v-if="courseStatus !== 'PENDING_REVIEW' && module.subTopics.length"
            type="button"
            class="inline-flex h-8 cursor-pointer items-center justify-center gap-1.5 rounded-lg border-2 border-lm-line bg-lm-surface px-3 font-mono text-[11px] font-bold text-lm-ink shadow-stamp-sm transition-all duration-200 hover:-translate-y-px hover:shadow-stamp-md"
            @click.stop="emit('module-edit', module.id)"
          >
            <svg class="h-3 w-3 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 20h9" /><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4Z" />
            </svg>
            Edit
          </button>
          <span
            v-else-if="!module.subTopics.length"
            class="rounded-full border-2 border-lm-line bg-lm-bg-soft px-2 py-1 font-mono text-[10px] font-bold text-lm-ink-3 shadow-stamp-sm"
          >
            Empty
          </span>
          <span
            class="cursor-pointer rounded-lg border-2 border-lm-line bg-lm-surface px-3 py-1.5 font-mono text-[11px] font-bold text-lm-ink shadow-stamp-sm transition-all duration-200 hover:-translate-y-px hover:shadow-stamp-md"
            @click.stop="openSubTopicForm(module.id)"
          >
            Add subtopic
          </span>
        </button>

        <!-- Add subtopic inline form -->
        <div v-if="editingSubTopicModuleId === module.id" class="border-t-2 border-lm-line-soft bg-lm-bg-soft px-4 py-3">
          <div class="rounded-[10px] border-2 border-lm-line bg-lm-surface p-3 shadow-stamp-sm">
            <div class="grid gap-3 lg:grid-cols-[280px_minmax(0,1fr)_auto]">
              <input
                v-model="subTopicTitle"
                type="text"
                placeholder="Subtopic title"
                class="h-10 rounded-lg border-2 border-lm-line-soft bg-lm-bg-soft px-3 text-[12px] text-lm-ink outline-none placeholder:text-lm-ink-3 focus:border-lm-line"
              />
              <input
                v-model="subTopicContent"
                type="text"
                placeholder="Short content or notes"
                class="h-10 rounded-lg border-2 border-lm-line-soft bg-lm-bg-soft px-3 text-[12px] text-lm-ink outline-none placeholder:text-lm-ink-3 focus:border-lm-line"
              />
              <div class="flex gap-2">
                <button
                  type="button"
                  class="h-10 rounded-lg border-2 border-lm-ink bg-lm-ink px-4 text-[12px] font-bold text-lm-bg shadow-stamp-sm transition hover:-translate-y-px hover:shadow-stamp-md disabled:cursor-not-allowed disabled:opacity-50"
                  :disabled="subTopicSaving || !subTopicTitle.trim()"
                  @click="addManualSubTopic(module.id)"
                >
                  {{ subTopicSaving ? 'Saving...' : 'Save' }}
                </button>
                <button
                  type="button"
                  class="h-10 rounded-lg border-2 border-lm-line bg-lm-surface px-4 text-[12px] font-bold text-lm-ink shadow-stamp-sm transition hover:-translate-y-px hover:shadow-stamp-md"
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
                <div class="flex items-center gap-1.5">
                  <button
                    v-if="courseStatus !== 'PENDING_REVIEW'"
                    type="button"
                  class="h-8 cursor-pointer rounded-lg border-2 border-lm-line bg-lm-surface px-3 font-mono text-[11px] font-bold text-lm-ink shadow-stamp-sm transition-all duration-200 hover:-translate-y-px hover:shadow-stamp-md"
                    @click="openLessonEditor(subTopic)"
                  >
                    Edit
                  </button>
                  <button
                    type="button"
                    class="flex h-8 w-8 cursor-pointer items-center justify-center rounded-lg border-2 border-lm-line-soft bg-lm-surface text-lm-ink-3 transition-all duration-200 hover:border-lm-red hover:bg-lm-red-soft hover:text-lm-red"
                    title="Delete subtopic"
                    @click="removeSubTopic(module.id, subTopic.id)"
                  >
                    <svg class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M3 6h18" /><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
                    </svg>
                  </button>
                </div>
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

        <!-- Discussion panel (always shown at bottom of each module card) -->
        <div class="border-t-2 border-lm-line-soft" @click.stop>
          <!-- "Open Discussion" button when closed -->
          <div v-if="!discussionOpenMap[module.id]" class="px-4 py-3">
            <button
              type="button"
              class="flex cursor-pointer items-center gap-2 rounded-[12px] border-2 border-dashed border-lm-line-soft px-4 py-2.5 text-[12px] font-semibold text-lm-ink-3 transition-all duration-200 hover:border-lm-line hover:bg-lm-surface hover:text-lm-ink"
              @click.stop="openDiscussion(module.id)"
            >
              <svg class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
              </svg>
              Open Discussion
            </button>
          </div>

          <!-- Full discussion panel when open -->
          <div v-else class="bg-lm-bg-soft">
            <!-- Header -->
            <div class="flex items-center justify-between px-4 py-3">
              <div class="flex items-center gap-2">
                <svg class="h-3.5 w-3.5 text-lm-ink" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
                </svg>
                <span class="font-mono text-[11px] font-bold uppercase tracking-[0.12em] text-lm-ink">Discussion</span>
                <span v-if="(commentsMap[String(module.id)] ?? []).length > 0"
                  class="rounded-full border border-lm-line-soft bg-lm-yellow/30 px-1.5 py-0.5 font-mono text-[10px] font-semibold text-lm-ink">
                  {{ (commentsMap[String(module.id)] ?? []).length }}
                </span>
              </div>
              <button v-if="!confirmEndMap[module.id]" type="button"
                class="flex cursor-pointer items-center gap-1.5 rounded-lg px-2.5 py-1.5 text-[11px] font-semibold text-lm-ink-3 transition hover:bg-lm-red-soft hover:text-lm-red"
                @click.stop="confirmEndMap[module.id] = true">
                <svg class="h-3 w-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M18 6 6 18M6 6l12 12" />
                </svg>
                End Discussion
              </button>
              <div v-else class="flex items-center gap-2">
                <span class="text-[11px] text-lm-ink-2">Clear all comments?</span>
                <button type="button" class="cursor-pointer rounded-lg border-2 border-lm-red bg-lm-red px-2.5 py-1 text-[11px] font-semibold text-lm-bg transition hover:opacity-90" @click.stop="endDiscussion(module.id)">End it</button>
                <button type="button" class="cursor-pointer rounded-lg border-2 border-lm-line bg-lm-surface px-2.5 py-1 text-[11px] font-semibold text-lm-ink transition hover:bg-lm-bg" @click.stop="confirmEndMap[module.id] = false">Cancel</button>
              </div>
            </div>

            <!-- Comments list -->
            <div class="px-4">
              <div v-if="!(commentsMap[String(module.id)] ?? []).length" class="mb-4 flex flex-col items-center gap-1 rounded-[12px] border-2 border-dashed border-lm-line-soft py-6 text-center">
                <svg class="h-6 w-6 text-lm-line-soft" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
                </svg>
                <p class="text-[12px] text-lm-ink-3">No comments yet. Start the discussion.</p>
              </div>
              <div v-else class="mb-4 flex flex-col gap-3">
                <div v-for="comment in (commentsMap[String(module.id)] ?? [])" :key="comment.id" class="flex gap-3">
                  <div class="flex h-7 w-7 shrink-0 items-center justify-center rounded-full text-[10px] font-bold text-white" :class="avatarBg(comment.authorName)">
                    {{ initials(comment.authorName) }}
                  </div>
                  <div class="group/c flex-1 min-w-0">
                    <div class="mb-1 flex items-center gap-2">
                      <span class="text-[12px] font-semibold text-lm-ink">{{ comment.authorName }}</span>
                      <span class="text-[11px] text-lm-ink-3">· {{ comment.createdAt }}</span>
                      <div v-if="comment.authorId === currentUserId && editingCommentId !== comment.id" class="ml-auto flex items-center gap-0.5 opacity-0 transition-opacity group-hover/c:opacity-100">
                        <button type="button" class="flex h-6 w-6 cursor-pointer items-center justify-center rounded-md text-lm-ink-3 transition hover:bg-lm-bg hover:text-lm-ink" @click.stop="startEditComment(comment)">
                          <svg class="h-3 w-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" /><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" /></svg>
                        </button>
                        <button type="button" class="flex h-6 w-6 cursor-pointer items-center justify-center rounded-md text-lm-ink-3 transition hover:bg-lm-red-soft hover:text-lm-red" @click.stop="deleteComment(module.id, comment.id)">
                          <svg class="h-3 w-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 6h18" /><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" /></svg>
                        </button>
                      </div>
                    </div>
                    <p v-if="editingCommentId !== comment.id" class="text-[12.5px] leading-relaxed text-lm-ink-2">{{ comment.text }}</p>
                    <div v-else>
                      <textarea v-model="editCommentText" rows="2" class="w-full resize-none rounded-[10px] border-2 border-lm-line bg-lm-surface px-3 py-2 text-[12.5px] leading-relaxed text-lm-ink outline-none ring-2 ring-lm-yellow/20" @keydown.enter.ctrl="saveEditComment(module.id)" @keydown.escape="cancelEditComment" />
                      <div class="mt-1.5 flex gap-2">
                        <button type="button" class="cursor-pointer rounded-lg border-2 border-lm-ink bg-lm-ink px-3 py-1 text-[11px] font-semibold text-lm-bg transition hover:opacity-90" @click.stop="saveEditComment(module.id)">Save</button>
                        <button type="button" class="cursor-pointer rounded-lg border-2 border-lm-line bg-lm-surface px-3 py-1 text-[11px] font-semibold text-lm-ink transition hover:bg-lm-bg" @click.stop="cancelEditComment">Cancel</button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- Add comment input -->
            <div class="flex items-start gap-3 border-t-2 border-lm-line-soft px-4 py-4">
              <div class="flex h-7 w-7 shrink-0 items-center justify-center rounded-full border-2 border-lm-line bg-lm-yellow font-display text-[10px] font-bold text-lm-ink">
                {{ initials(currentUserName) }}
              </div>
              <div class="flex flex-1 items-end gap-2">
                <textarea
                  :value="newCommentMap[module.id] ?? ''"
                  rows="1"
                  placeholder="Add a comment…"
                  class="flex-1 resize-none rounded-[10px] border-2 border-lm-line-soft bg-lm-surface px-3 py-2 text-[12.5px] leading-relaxed text-lm-ink outline-none transition placeholder:text-lm-ink-3 focus:border-lm-line focus:ring-2 focus:ring-lm-yellow/40"
                  @input="newCommentMap[module.id] = ($event.target as HTMLTextAreaElement).value"
                  @keydown.enter.prevent="addComment(module.id)"
                />
                <button type="button"
                  class="flex h-8 w-8 shrink-0 cursor-pointer items-center justify-center rounded-[10px] border-2 border-lm-ink bg-lm-ink text-lm-bg shadow-stamp-sm transition-all duration-200 hover:-translate-y-px hover:shadow-stamp-md disabled:cursor-not-allowed disabled:opacity-40"
                  :disabled="!(newCommentMap[module.id] ?? '').trim()"
                  @click.stop="addComment(module.id)">
                  <svg class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                    <line x1="22" y1="2" x2="11" y2="13" /><polygon points="22 2 15 22 11 13 2 9 22 2" />
                  </svg>
                </button>
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
