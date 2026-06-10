<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'
import type { AdminSubTopicDto } from '@/features/courses/services/adminCourses'
import InteractivePreview from '@/features/courses/components/interactive/InteractivePreview.vue'
import { parseInteractiveConfig } from '@/features/courses/types/interactive'
import type { InteractionType, InteractiveConfig } from '@/features/courses/types/interactive'

export type Comment = {
  id: string | number
  authorId: string
  authorName: string
  text: string
  createdAt: string
  subTopicId?: number | null
}

const props = defineProps<{
  index: number
  name: string
  content: string
  comments: Comment[]
  currentUserId: string
  subTopics?: AdminSubTopicDto[]
}>()

const emit = defineEmits<{
  'add-comment': [payload: { text: string; subTopicId: number | null }]
}>()

const expanded = ref(false)
const moduleComposerOpen = ref(false)
const newComment = ref('')

// Per-subtopic UI state
const expandedSubTopics = ref<Record<number, boolean>>({})
const activeComposerSubTopicId = ref<number | null>(null)
const activePreviewSubTopicId = ref<number | null>(null)
const subTopicComment = ref('')

const sortedSubTopics = computed(() =>
  [...(props.subTopics ?? [])].sort((a, b) => a.sortOrder - b.sortOrder),
)

const moduleComments = computed(() => props.comments.filter((c) => !c.subTopicId))

const commentsBySubTopic = computed(() => {
  const map = new Map<number, Comment[]>()
  for (const comment of props.comments) {
    if (!comment.subTopicId) continue
    if (!map.has(comment.subTopicId)) map.set(comment.subTopicId, [])
    map.get(comment.subTopicId)!.push(comment)
  }
  return map
})

function subTopicComments(subTopicId: number): Comment[] {
  return commentsBySubTopic.value.get(subTopicId) ?? []
}

function isDraft(comment: Comment): boolean {
  return String(comment.id).startsWith('temp-')
}

function toggleSubTopic(subTopicId: number) {
  expandedSubTopics.value[subTopicId] = !expandedSubTopics.value[subTopicId]
  if (!expandedSubTopics.value[subTopicId]) {
    if (activeComposerSubTopicId.value === subTopicId) activeComposerSubTopicId.value = null
    if (activePreviewSubTopicId.value === subTopicId) activePreviewSubTopicId.value = null
  }
}

async function openComposer(subTopicId: number) {
  expandedSubTopics.value[subTopicId] = true
  if (activeComposerSubTopicId.value === subTopicId) {
    activeComposerSubTopicId.value = null
    return
  }
  activeComposerSubTopicId.value = subTopicId
  subTopicComment.value = ''
  await nextTick()
  document.getElementById(`subtopic-composer-${subTopicId}`)?.focus()
}

function submitSubTopicComment(subTopicId: number) {
  const text = subTopicComment.value.trim()
  if (!text) return
  emit('add-comment', { text, subTopicId })
  subTopicComment.value = ''
}

function submitModuleComment() {
  const text = newComment.value.trim()
  if (!text) return
  emit('add-comment', { text, subTopicId: null })
  newComment.value = ''
}

// Interactive activity preview
const previewConfigCache = new Map<number, InteractiveConfig | null>()

function previewConfig(subTopic: AdminSubTopicDto): InteractiveConfig | null {
  if (!previewConfigCache.has(subTopic.id)) {
    previewConfigCache.set(
      subTopic.id,
      parseInteractiveConfig(subTopic.interactionType as InteractionType, subTopic.interactionConfig),
    )
  }
  return previewConfigCache.get(subTopic.id) ?? null
}

function togglePreview(subTopicId: number) {
  activePreviewSubTopicId.value = activePreviewSubTopicId.value === subTopicId ? null : subTopicId
}

const renderedContent = computed(() => {
  if (/<[a-z][\s\S]*?>/i.test(props.content)) return props.content
  return props.content
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\n/g, '<br>')
})

const AVATAR_PALETTE = ['bg-violet-500', 'bg-blue-500', 'bg-teal-500', 'bg-amber-500', 'bg-rose-500', 'bg-indigo-500']

function avatarBg(name: string): string {
  let h = 0
  for (const c of name) h = (h * 31 + c.charCodeAt(0)) & 0xffff
  return AVATAR_PALETTE[h % AVATAR_PALETTE.length]
}

function initials(name: string): string {
  return name.split(' ').map((w) => w[0] ?? '').slice(0, 2).join('').toUpperCase()
}

function lessonHtml(subTopic: AdminSubTopicDto): string {
  if (subTopic.contentHtml) return subTopic.contentHtml
  return subTopic.content ? subTopic.content.split(/\n{2,}/).map(p => p.trim()).filter(Boolean).map(p => `<p>${p.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/\n/g,'<br>')}</p>`).join('') : ''
}

function formatInteractionType(value: string | null | undefined): string {
  if (!value || value === 'NONE') return ''
  return value.toLowerCase().split('_').map(part => part.charAt(0).toUpperCase() + part.slice(1)).join(' ')
}
</script>

<template>
  <div class="overflow-hidden rounded-[18px] border-2 border-lm-line bg-lm-surface shadow-stamp-sm transition-all duration-200 hover:shadow-stamp-md">

    <!-- Accordion header -->
    <div
      class="group flex cursor-pointer select-none items-center gap-4 px-5 py-4"
      @click="expanded = !expanded"
    >
      <div class="flex h-8 w-8 shrink-0 items-center justify-center rounded-[10px] border-2 border-lm-line bg-lm-yellow font-display text-[12px] font-bold text-lm-ink shadow-stamp-sm">
        {{ index }}
      </div>
      <span class="flex-1 text-[13.5px] font-semibold leading-snug text-lm-ink">{{ name }}</span>

      <!-- Comment count indicator -->
      <span
        v-if="comments.length > 0"
        class="inline-flex items-center gap-1.5 rounded-full border-2 border-lm-line bg-lm-yellow/40 px-2.5 py-0.5 font-mono text-[10px] font-bold text-lm-ink"
      >
        <svg class="h-3 w-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
        </svg>
        {{ comments.length }}
      </span>

      <svg
        class="h-4 w-4 shrink-0 text-lm-line-soft transition-transform duration-200"
        :class="expanded ? 'rotate-180 text-lm-ink' : ''"
        viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
      >
        <polyline points="6 9 12 15 18 9" />
      </svg>
    </div>

    <!-- Expanded body -->
    <Transition
      enter-active-class="transition-all duration-200 ease-out"
      enter-from-class="opacity-0 -translate-y-1"
      enter-to-class="opacity-100 translate-y-0"
      leave-active-class="transition-all duration-150 ease-in"
      leave-from-class="opacity-100 translate-y-0"
      leave-to-class="opacity-0 -translate-y-1"
    >
      <div v-if="expanded">

        <!-- Subtopic list -->
        <div class="border-t-2 border-lm-line-soft bg-lm-bg px-4 py-3.5">
          <template v-if="subTopics !== undefined">
            <div v-if="sortedSubTopics.length > 0" class="space-y-1.5">
              <div
                v-for="(subTopic, subTopicIndex) in sortedSubTopics"
                :key="subTopic.id"
                class="overflow-hidden rounded-[12px] border-2 bg-lm-bg-soft transition-all duration-200"
                :class="expandedSubTopics[subTopic.id] ? 'border-lm-line shadow-stamp-xs' : 'border-lm-line-soft'"
              >
                <!-- Compact row (always visible) -->
                <div
                  class="group/row flex cursor-pointer select-none items-center gap-3 px-3.5 py-2.5"
                  @click="toggleSubTopic(subTopic.id)"
                >
                  <span class="w-7 shrink-0 font-mono text-[11px] font-bold text-lm-ink-3">{{ index }}.{{ subTopicIndex + 1 }}</span>
                  <span class="min-w-0 flex-1 truncate font-display text-[12.5px] font-bold text-lm-ink">{{ subTopic.title }}</span>

                  <span
                    v-if="subTopic.interactionType !== 'NONE'"
                    class="hidden shrink-0 rounded-full border border-lm-line-soft bg-lm-surface px-2 py-0.5 font-mono text-[9px] font-bold text-lm-purple sm:inline"
                  >
                    {{ formatInteractionType(subTopic.interactionType) }}
                  </span>
                  <span
                    v-if="subTopic.pageStart && subTopic.pageEnd"
                    class="hidden shrink-0 font-mono text-[9px] font-bold uppercase tracking-[0.08em] text-lm-ink-3 md:inline"
                  >
                    p.{{ subTopic.pageStart }}–{{ subTopic.pageEnd }}
                  </span>

                  <!-- Comment button / count -->
                  <button
                    type="button"
                    class="flex shrink-0 cursor-pointer items-center gap-1 rounded-full border-2 px-2 py-0.5 font-mono text-[10px] font-bold transition-all duration-200"
                    :class="subTopicComments(subTopic.id).length
                      ? 'border-lm-line bg-lm-yellow text-lm-ink shadow-stamp-xs'
                      : 'border-transparent text-lm-ink-3 opacity-0 hover:border-lm-line hover:bg-lm-surface hover:text-lm-ink group-hover/row:opacity-100'"
                    :title="'Comment on this subtopic'"
                    @click.stop="openComposer(subTopic.id)"
                  >
                    <svg class="h-3 w-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                      <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
                    </svg>
                    <span>{{ subTopicComments(subTopic.id).length || '' }}</span>
                  </button>

                  <svg
                    class="h-3.5 w-3.5 shrink-0 text-lm-line-soft transition-transform duration-200"
                    :class="expandedSubTopics[subTopic.id] ? 'rotate-180 text-lm-ink' : ''"
                    viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"
                  >
                    <polyline points="6 9 12 15 18 9" />
                  </svg>
                </div>

                <!-- Expanded detail -->
                <Transition
                  enter-active-class="transition-all duration-200 ease-out"
                  enter-from-class="opacity-0 -translate-y-1"
                  enter-to-class="opacity-100 translate-y-0"
                  leave-active-class="transition-all duration-150 ease-in"
                  leave-from-class="opacity-100 translate-y-0"
                  leave-to-class="opacity-0 -translate-y-1"
                >
                  <div v-if="expandedSubTopics[subTopic.id]" @click.stop>
                    <!-- Lesson content -->
                    <div class="border-t-2 border-lm-line-soft bg-lm-surface px-4 py-3.5">
                      <div class="lesson-preview text-[12.5px] leading-relaxed text-lm-ink-2" v-html="lessonHtml(subTopic)" />

                      <!-- Activity row -->
                      <div v-if="subTopic.interactionType !== 'NONE' || subTopic.interactionPrompt" class="mt-3 flex flex-wrap items-center gap-2">
                        <button
                          v-if="subTopic.interactionType !== 'NONE' && previewConfig(subTopic)"
                          type="button"
                          class="flex cursor-pointer items-center gap-1.5 rounded-full border-2 px-2.5 py-1 font-mono text-[10px] font-bold transition-all duration-200"
                          :class="activePreviewSubTopicId === subTopic.id
                            ? 'border-lm-line bg-lm-purple/15 text-lm-purple shadow-stamp-xs'
                            : 'border-lm-line-soft bg-lm-surface text-lm-purple hover:-translate-y-px hover:border-lm-line hover:shadow-stamp-xs'"
                          :title="activePreviewSubTopicId === subTopic.id ? 'Hide the activity' : 'Try the activity like a learner'"
                          @click="togglePreview(subTopic.id)"
                        >
                          <svg v-if="activePreviewSubTopicId !== subTopic.id" class="h-3 w-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                            <polygon points="6 3 20 12 6 21 6 3" />
                          </svg>
                          <svg v-else class="h-3 w-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                            <polyline points="18 15 12 9 6 15" />
                          </svg>
                          {{ formatInteractionType(subTopic.interactionType) }}
                          <span class="font-normal opacity-70">· {{ activePreviewSubTopicId === subTopic.id ? 'Hide' : 'Try it' }}</span>
                        </button>
                        <span v-else-if="subTopic.interactionType !== 'NONE'" class="rounded-full border border-lm-line-soft bg-lm-surface px-2 py-0.5 font-mono text-[10px] font-bold text-lm-purple">{{ formatInteractionType(subTopic.interactionType) }}</span>
                        <span v-if="subTopic.interactionPrompt" class="min-w-0 flex-1 text-[10px] font-medium leading-relaxed text-lm-purple">{{ subTopic.interactionPrompt }}</span>
                      </div>

                      <!-- Interactive activity preview (compact) -->
                      <Transition
                        enter-active-class="transition-all duration-200 ease-out"
                        enter-from-class="opacity-0 -translate-y-1"
                        enter-to-class="opacity-100 translate-y-0"
                        leave-active-class="transition-all duration-150 ease-in"
                        leave-from-class="opacity-100 translate-y-0"
                        leave-to-class="opacity-0 -translate-y-1"
                      >
                        <div v-if="activePreviewSubTopicId === subTopic.id" class="mt-3">
                          <div class="mb-1.5 flex items-center gap-2">
                            <span class="font-mono text-[9px] font-bold uppercase tracking-[0.14em] text-lm-ink-3">Learner Preview</span>
                            <div class="flex-1 border-t border-dashed border-lm-line-soft" />
                          </div>
                          <div class="compact-preview max-w-[640px] mx-auto">
                            <InteractivePreview :config="previewConfig(subTopic)" />
                          </div>
                        </div>
                      </Transition>
                    </div>

                    <!-- Comments + composer -->
                    <div
                      v-if="subTopicComments(subTopic.id).length || activeComposerSubTopicId === subTopic.id"
                      class="border-t-2 border-dashed border-lm-line-soft bg-lm-bg-soft px-4 py-3"
                    >
                      <div v-if="subTopicComments(subTopic.id).length" class="flex flex-col gap-2.5" :class="{ 'mb-3': activeComposerSubTopicId === subTopic.id }">
                        <div v-for="comment in subTopicComments(subTopic.id)" :key="comment.id" class="flex gap-2.5">
                          <div
                            class="flex h-6 w-6 shrink-0 items-center justify-center rounded-full text-[9px] font-bold text-white"
                            :class="avatarBg(comment.authorName)"
                          >
                            {{ initials(comment.authorName) }}
                          </div>
                          <div class="min-w-0 flex-1">
                            <div class="flex items-center gap-2">
                              <span class="text-[11.5px] font-semibold text-lm-ink">{{ comment.authorName }}</span>
                              <span class="text-[10.5px] text-lm-ink-3">· {{ comment.createdAt }}</span>
                              <span
                                v-if="isDraft(comment)"
                                class="rounded-full border border-dashed border-lm-rust px-1.5 py-px font-mono text-[8.5px] font-bold uppercase tracking-[0.08em] text-lm-rust"
                                title="Sent with your revision request"
                              >
                                Draft
                              </span>
                            </div>
                            <p class="mt-0.5 text-[12px] leading-relaxed text-lm-ink-2">{{ comment.text }}</p>
                          </div>
                        </div>
                      </div>

                      <div v-if="activeComposerSubTopicId === subTopic.id" class="flex items-end gap-2">
                        <textarea
                          :id="`subtopic-composer-${subTopic.id}`"
                          v-model="subTopicComment"
                          rows="1"
                          :placeholder="`Comment on “${subTopic.title}”…`"
                          class="flex-1 resize-none rounded-[10px] border-2 border-lm-line-soft bg-lm-surface px-3 py-2 text-[12px] leading-relaxed text-lm-ink outline-none transition placeholder:text-lm-ink-3 focus:border-lm-line focus:ring-2 focus:ring-lm-yellow/40"
                          @keydown.enter.prevent="submitSubTopicComment(subTopic.id)"
                          @keydown.escape="activeComposerSubTopicId = null"
                        />
                        <button
                          type="button"
                          class="flex h-8 w-8 shrink-0 cursor-pointer items-center justify-center rounded-[10px] border-2 border-lm-ink bg-lm-ink text-lm-bg shadow-stamp-sm transition-all duration-200 hover:-translate-y-px hover:shadow-stamp-md disabled:cursor-not-allowed disabled:opacity-40"
                          :disabled="!subTopicComment.trim()"
                          @click="submitSubTopicComment(subTopic.id)"
                        >
                          <svg class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                            <line x1="22" y1="2" x2="11" y2="13" />
                            <polygon points="22 2 15 22 11 13 2 9 22 2" />
                          </svg>
                        </button>
                      </div>
                    </div>
                  </div>
                </Transition>
              </div>
            </div>
            <p v-else class="font-mono text-[12px] text-lm-ink-3">No subtopics yet.</p>
          </template>

          <!-- Fallback: rendered HTML content when subTopics prop is not provided -->
          <div v-else class="topic-preview text-[12.5px] leading-relaxed text-lm-ink-2" v-html="renderedContent" />
        </div>

        <!-- Module-level discussion -->
        <div class="border-t-2 border-lm-line-soft bg-lm-bg-soft" @click.stop>
          <div class="flex items-center justify-between px-5 py-3">
            <div class="flex items-center gap-2">
              <svg class="h-3.5 w-3.5 text-lm-ink" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
              </svg>
              <span class="font-mono text-[11px] font-bold uppercase tracking-[0.12em] text-lm-ink">Module Discussion</span>
              <span v-if="moduleComments.length > 0"
                class="rounded-full border border-lm-line-soft bg-lm-yellow/30 px-1.5 py-0.5 font-mono text-[10px] font-semibold text-lm-ink">
                {{ moduleComments.length }}
              </span>
            </div>
            <button
              v-if="!moduleComposerOpen && moduleComments.length === 0"
              type="button"
              class="flex cursor-pointer items-center gap-1.5 rounded-full border-2 border-lm-line bg-lm-surface px-3 py-1 font-mono text-[10px] font-bold text-lm-ink shadow-stamp-xs transition-all duration-200 hover:-translate-y-px hover:shadow-stamp-sm"
              @click="moduleComposerOpen = true"
            >
              <svg class="h-3 w-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                <line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" />
              </svg>
              Comment
            </button>
          </div>

          <div v-if="moduleComments.length" class="flex flex-col gap-3 px-5 pb-4">
            <div v-for="comment in moduleComments" :key="comment.id" class="flex gap-3">
              <div
                class="flex h-7 w-7 shrink-0 items-center justify-center rounded-full text-[10px] font-bold text-white"
                :class="avatarBg(comment.authorName)"
              >
                {{ initials(comment.authorName) }}
              </div>
              <div class="min-w-0 flex-1">
                <div class="mb-1 flex items-center gap-2">
                  <span class="text-[12px] font-semibold text-lm-ink">{{ comment.authorName }}</span>
                  <span class="text-[11px] text-lm-ink-3">· {{ comment.createdAt }}</span>
                  <span
                    v-if="isDraft(comment)"
                    class="rounded-full border border-dashed border-lm-rust px-1.5 py-px font-mono text-[8.5px] font-bold uppercase tracking-[0.08em] text-lm-rust"
                    title="Sent with your revision request"
                  >
                    Draft
                  </span>
                </div>
                <p class="text-[12.5px] leading-relaxed text-lm-ink-2">{{ comment.text }}</p>
              </div>
            </div>
          </div>

          <div
            v-if="moduleComposerOpen || moduleComments.length"
            class="flex items-start gap-3 border-t-2 border-dashed border-lm-line-soft px-5 py-4"
          >
            <div class="flex h-7 w-7 shrink-0 items-center justify-center rounded-full border-2 border-lm-line bg-lm-yellow font-display text-[10px] font-bold text-lm-ink shadow-stamp-sm">
              TC
            </div>
            <div class="flex flex-1 items-end gap-2">
              <textarea
                v-model="newComment"
                rows="1"
                placeholder="Comment on the whole module…"
                class="flex-1 resize-none rounded-[10px] border-2 border-lm-line-soft bg-lm-surface px-3 py-2 text-[12.5px] leading-relaxed text-lm-ink outline-none transition placeholder:text-lm-ink-3 focus:border-lm-line focus:ring-2 focus:ring-lm-yellow/40"
                @keydown.enter.prevent="submitModuleComment"
              />
              <button
                type="button"
                class="flex h-8 w-8 shrink-0 cursor-pointer items-center justify-center rounded-[10px] border-2 border-lm-ink bg-lm-ink text-lm-bg shadow-stamp-sm transition-all duration-200 hover:-translate-y-px hover:shadow-stamp-md disabled:cursor-not-allowed disabled:opacity-40"
                :disabled="!newComment.trim()"
                @click="submitModuleComment"
              >
                <svg class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                  <line x1="22" y1="2" x2="11" y2="13" />
                  <polygon points="22 2 15 22 11 13 2 9 22 2" />
                </svg>
              </button>
            </div>
          </div>
        </div>

      </div>
    </Transition>
  </div>
</template>

<style scoped>
.topic-preview :deep(h1) { font-size: 1.25rem; font-weight: 700; color: #1a1814; margin: 0.75rem 0 0.375rem; }
.topic-preview :deep(h2) { font-size: 1.1rem; font-weight: 700; color: #1a1814; margin: 0.5rem 0 0.25rem; }
.topic-preview :deep(h3) { font-size: 1rem; font-weight: 600; color: #1a1814; margin: 0.375rem 0 0.125rem; }
.topic-preview :deep(ul) { list-style-type: disc; padding-left: 1.25rem; margin: 0.375rem 0; }
.topic-preview :deep(ol) { list-style-type: decimal; padding-left: 1.25rem; margin: 0.375rem 0; }
.topic-preview :deep(li) { margin-bottom: 0.125rem; }
.topic-preview :deep(img) { max-width: 100%; height: auto; border-radius: 6px; margin: 8px 0; }

/* Slightly scale down the embedded learner activity so it reads as a preview */
.compact-preview :deep(.interactive-preview) {
  padding: 0.75rem;
  font-size: 13px;
}
.compact-preview :deep(.interactive-preview h3) {
  font-size: 0.85rem;
}
.compact-preview :deep(.graph-canvas) {
  min-height: 170px;
}
.compact-preview :deep(.formula-result) {
  font-size: 1.1rem;
}
</style>
