<script setup lang="ts">
import { computed, ref } from 'vue'
import type { AdminSubTopicDto } from '@/features/courses/services/adminCourses'

export type Comment = {
  id: string
  authorId: string
  authorName: string
  text: string
  createdAt: string
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
  'add-comment': [text: string]
  'edit-comment': [payload: { id: string; text: string }]
  'delete-comment': [id: string]
  'end-discussion': []
}>()

const expanded = ref(false)
const discussionOpen = ref(false)
const confirmEnd = ref(false)
const newComment = ref('')
const editingId = ref<string | null>(null)
const editText = ref('')

const renderedContent = computed(() => {
  if (/<[a-z][\s\S]*?>/i.test(props.content)) return props.content
  return props.content
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\n/g, '<br>')
})

function openDiscussion() {
  discussionOpen.value = true
  confirmEnd.value = false
}

function onEndDiscussion() {
  emit('end-discussion')
  discussionOpen.value = false
  confirmEnd.value = false
  newComment.value = ''
  editingId.value = null
}

function submitComment() {
  const text = newComment.value.trim()
  if (!text) return
  emit('add-comment', text)
  newComment.value = ''
}

function startEdit(comment: Comment) {
  editingId.value = comment.id
  editText.value = comment.text
}

function saveEdit() {
  if (!editingId.value || !editText.value.trim()) return
  emit('edit-comment', { id: editingId.value, text: editText.value.trim() })
  editingId.value = null
  editText.value = ''
}

function cancelEdit() {
  editingId.value = null
  editText.value = ''
}

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

      <!-- Active discussion indicator -->
      <span
        v-if="discussionOpen"
        class="inline-flex items-center gap-1 rounded-full border border-lm-line-soft bg-lm-yellow/30 px-2 py-0.5 font-mono text-[10px] font-semibold text-lm-ink"
      >
        <span class="h-1.5 w-1.5 animate-pulse rounded-full bg-lm-ink" />
        Discussion open
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

        <!-- Topic content -->
        <div class="border-t-2 border-lm-line-soft bg-lm-bg px-5 py-4">
          <!-- Subtopics view when subTopics prop is provided -->
          <template v-if="subTopics !== undefined">
            <div v-if="subTopics.length > 0" class="space-y-2">
              <div
                v-for="(subTopic, subTopicIndex) in [...subTopics].sort((a, b) => a.sortOrder - b.sortOrder)"
                :key="subTopic.id"
                class="rounded-lg border border-lm-line-soft bg-lm-bg-soft px-3 py-2"
              >
                <div class="grid grid-cols-[auto_minmax(0,1fr)] gap-3">
                  <span class="mt-0.5 font-mono text-[11px] font-bold text-lm-ink-3">{{ index }}.{{ subTopicIndex + 1 }}</span>
                  <div class="min-w-0">
                    <p class="truncate font-display text-[12px] font-bold text-lm-ink">{{ subTopic.title }}</p>
                    <div class="lesson-preview mt-2 line-clamp-6 text-[12px] leading-relaxed text-lm-ink-2" v-html="lessonHtml(subTopic)" />
                    <div v-if="subTopic.interactionType !== 'NONE' || subTopic.interactionPrompt" class="mt-2 flex flex-wrap items-start gap-2">
                      <span v-if="subTopic.interactionType !== 'NONE'" class="rounded-full border border-lm-line-soft bg-lm-surface px-2 py-0.5 font-mono text-[10px] font-bold text-lm-purple">{{ formatInteractionType(subTopic.interactionType) }}</span>
                      <span v-if="subTopic.interactionPrompt" class="min-w-0 flex-1 text-[10px] font-medium leading-relaxed text-lm-purple">{{ subTopic.interactionPrompt }}</span>
                    </div>
                    <p v-if="subTopic.pageStart && subTopic.pageEnd" class="mt-1 font-mono text-[10px] font-bold uppercase tracking-[0.1em] text-lm-ink-3">
                      Pages {{ subTopic.pageStart }}–{{ subTopic.pageEnd }}
                    </p>
                  </div>
                </div>
              </div>
            </div>
            <p v-else class="font-mono text-[12px] text-lm-ink-3">No subtopics yet.</p>
          </template>

          <!-- Fallback: rendered HTML content when subTopics prop is not provided -->
          <div v-else class="topic-preview text-[12.5px] leading-relaxed text-lm-ink-2" v-html="renderedContent" />

          <!-- Open Discussion button (only when discussion is closed) -->
          <button
            v-if="!discussionOpen"
            type="button"
            class="mt-4 flex items-center gap-2 rounded-[12px] border-2 border-dashed border-lm-line-soft px-4 py-2.5 text-[12px] font-semibold text-lm-ink-3 transition-all duration-200 hover:border-lm-line hover:bg-lm-surface hover:text-lm-ink"
            @click.stop="openDiscussion"
          >
            <svg class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
            </svg>
            Open Discussion
          </button>
        </div>

        <!-- Discussion panel -->
        <Transition
          enter-active-class="transition-all duration-200 ease-out"
          enter-from-class="opacity-0 -translate-y-2"
          enter-to-class="opacity-100 translate-y-0"
          leave-active-class="transition-all duration-150 ease-in"
          leave-from-class="opacity-100 translate-y-0"
          leave-to-class="opacity-0 -translate-y-2"
        >
          <div v-if="discussionOpen" class="border-t-2 border-lm-line-soft bg-lm-bg-soft" @click.stop>

            <!-- Discussion header -->
            <div class="flex items-center justify-between px-5 py-3">
              <div class="flex items-center gap-2">
                <svg class="h-3.5 w-3.5 text-lm-ink" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
                </svg>
                <span class="font-mono text-[11px] font-bold uppercase tracking-[0.12em] text-lm-ink">Discussion</span>
                <span v-if="comments.length > 0"
                  class="rounded-full border border-lm-line-soft bg-lm-yellow/30 px-1.5 py-0.5 font-mono text-[10px] font-semibold text-lm-ink">
                  {{ comments.length }}
                </span>
              </div>

              <!-- Normal End Discussion button -->
              <button
                v-if="!confirmEnd"
                type="button"
                class="flex items-center gap-1.5 rounded-lg px-2.5 py-1.5 text-[11px] font-semibold text-lm-ink-3 transition hover:bg-lm-red-soft hover:text-lm-red"
                @click="confirmEnd = true"
              >
                <svg class="h-3 w-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M18 6 6 18M6 6l12 12" />
                </svg>
                End Discussion
              </button>

              <!-- Inline confirmation -->
              <div v-else class="flex items-center gap-2">
                <span class="text-[11px] text-lm-ink-2">Clear all comments?</span>
                <button
                  type="button"
                  class="rounded-lg border-2 border-lm-red bg-lm-red px-2.5 py-1 text-[11px] font-semibold text-lm-bg transition hover:opacity-90"
                  @click="onEndDiscussion"
                >
                  End it
                </button>
                <button
                  type="button"
                  class="rounded-lg border-2 border-lm-line bg-lm-surface px-2.5 py-1 text-[11px] font-semibold text-lm-ink transition hover:bg-lm-bg"
                  @click="confirmEnd = false"
                >
                  Cancel
                </button>
              </div>
            </div>

            <!-- Comments list -->
            <div class="px-5">
              <!-- Empty state -->
              <div v-if="comments.length === 0" class="mb-4 flex flex-col items-center gap-1 rounded-[12px] border-2 border-dashed border-lm-line-soft py-6 text-center">
                <svg class="h-6 w-6 text-lm-line-soft" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
                </svg>
                <p class="text-[12px] text-lm-ink-3">No comments yet. Start the discussion.</p>
              </div>

              <!-- Comment items -->
              <div v-else class="mb-4 flex flex-col gap-3">
                <div v-for="comment in comments" :key="comment.id" class="flex gap-3">
                  <div
                    class="flex h-7 w-7 shrink-0 items-center justify-center rounded-full text-[10px] font-bold text-white"
                    :class="avatarBg(comment.authorName)"
                  >
                    {{ initials(comment.authorName) }}
                  </div>

                  <div class="group/c flex-1 min-w-0">
                    <div class="mb-1 flex items-center gap-2">
                      <span class="text-[12px] font-semibold text-lm-ink">{{ comment.authorName }}</span>
                      <span class="text-[11px] text-lm-ink-3">· {{ comment.createdAt }}</span>
                      <div
                        v-if="comment.authorId === currentUserId && editingId !== comment.id"
                        class="ml-auto flex items-center gap-0.5 opacity-0 transition-opacity group-hover/c:opacity-100"
                      >
                        <button type="button"
                          class="flex h-6 w-6 items-center justify-center rounded-md text-lm-ink-3 transition hover:bg-lm-bg hover:text-lm-ink"
                          title="Edit"
                          @click="startEdit(comment)">
                          <svg class="h-3 w-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
                            <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
                          </svg>
                        </button>
                        <button type="button"
                          class="flex h-6 w-6 items-center justify-center rounded-md text-lm-ink-3 transition hover:bg-lm-red-soft hover:text-lm-red"
                          title="Delete"
                          @click="emit('delete-comment', comment.id)">
                          <svg class="h-3 w-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M3 6h18" /><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
                          </svg>
                        </button>
                      </div>
                    </div>

                    <!-- Text -->
                    <p v-if="editingId !== comment.id" class="text-[12.5px] leading-relaxed text-lm-ink-2">
                      {{ comment.text }}
                    </p>

                    <!-- Inline edit -->
                    <div v-else>
                      <textarea
                        v-model="editText"
                        rows="2"
                        class="w-full resize-none rounded-[10px] border-2 border-lm-line bg-lm-surface px-3 py-2 text-[12.5px] leading-relaxed text-lm-ink outline-none ring-2 ring-lm-yellow/20"
                        @keydown.enter.ctrl="saveEdit"
                        @keydown.escape="cancelEdit"
                      />
                      <div class="mt-1.5 flex gap-2">
                        <button type="button"
                          class="rounded-lg border-2 border-lm-ink bg-lm-ink px-3 py-1 text-[11px] font-semibold text-lm-bg transition hover:opacity-90"
                          @click="saveEdit">Save</button>
                        <button type="button"
                          class="rounded-lg border-2 border-lm-line bg-lm-surface px-3 py-1 text-[11px] font-semibold text-lm-ink transition hover:bg-lm-bg"
                          @click="cancelEdit">Cancel</button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- Add comment form -->
            <div class="flex items-start gap-3 border-t-2 border-lm-line-soft px-5 py-4">
              <div class="flex h-7 w-7 shrink-0 items-center justify-center rounded-full border-2 border-lm-line bg-lm-yellow font-display text-[10px] font-bold text-lm-ink shadow-stamp-sm">
                TC
              </div>
              <div class="flex flex-1 items-end gap-2">
                <textarea
                  v-model="newComment"
                  rows="1"
                  placeholder="Add a comment…"
                  class="flex-1 resize-none rounded-[10px] border-2 border-lm-line-soft bg-lm-surface px-3 py-2 text-[12.5px] leading-relaxed text-lm-ink outline-none transition placeholder:text-lm-ink-3 focus:border-lm-line focus:ring-2 focus:ring-lm-yellow/40"
                  @keydown.enter.prevent="submitComment"
                />
                <button
                  type="button"
                  class="flex h-8 w-8 shrink-0 items-center justify-center rounded-[10px] border-2 border-lm-ink bg-lm-ink text-lm-bg shadow-stamp-sm transition-all duration-200 hover:-translate-y-px hover:shadow-stamp-md disabled:opacity-40"
                  :disabled="!newComment.trim()"
                  @click="submitComment"
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
</style>
