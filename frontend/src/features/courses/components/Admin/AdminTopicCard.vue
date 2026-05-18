<script setup lang="ts">
import { computed, ref } from 'vue'

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
}>()

const emit = defineEmits<{
  edit: []
  delete: []
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

function avatarBg(name: string): string {
  const palette = ['bg-violet-500', 'bg-blue-500', 'bg-teal-500', 'bg-amber-500', 'bg-rose-500', 'bg-indigo-500']
  let h = 0
  for (const c of name) h = (h * 31 + c.charCodeAt(0)) & 0xffff
  return palette[h % palette.length]
}

function initials(name: string): string {
  return name.split(' ').map((w) => w[0] ?? '').slice(0, 2).join('').toUpperCase()
}
</script>

<template>
  <div class="group overflow-hidden rounded-2xl bg-white ring-1 ring-slate-900/[0.06] shadow-sm transition-all duration-200 hover:shadow-md">

    <!-- Accordion header -->
    <div
      class="flex cursor-pointer select-none items-center gap-4 px-5 py-4"
      @click="expanded = !expanded"
    >
      <div class="flex h-8 w-8 shrink-0 items-center justify-center rounded-xl bg-[#5b4cfa]/10 text-[12px] font-bold text-[#5b4cfa]">
        {{ index }}
      </div>
      <span class="flex-1 text-[13.5px] font-semibold leading-snug text-slate-800">{{ name }}</span>

      <!-- Active discussion indicator -->
      <span
        v-if="discussionOpen"
        class="inline-flex items-center gap-1 rounded-full bg-[#5b4cfa]/10 px-2 py-0.5 text-[10px] font-semibold text-[#5b4cfa]"
      >
        <span class="h-1.5 w-1.5 animate-pulse rounded-full bg-[#5b4cfa]" />
        Discussion open
      </span>

      <!-- Topic hover actions (edit / delete) -->
      <div
        class="flex items-center gap-0.5 opacity-0 transition-opacity duration-150 group-hover:opacity-100"
        @click.stop
      >
        <button
          type="button"
          class="flex h-7 w-7 items-center justify-center rounded-lg text-slate-400 transition hover:bg-slate-100 hover:text-slate-600"
          title="Edit topic"
          @click.stop="emit('edit')"
        >
          <svg class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
            <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
          </svg>
        </button>
        <button
          type="button"
          class="flex h-7 w-7 items-center justify-center rounded-lg text-slate-400 transition hover:bg-red-50 hover:text-red-500"
          title="Delete topic"
          @click.stop="emit('delete')"
        >
          <svg class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M3 6h18" />
            <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
          </svg>
        </button>
      </div>

      <!-- Chevron -->
      <svg
        class="h-4 w-4 shrink-0 text-slate-300 transition-transform duration-200"
        :class="expanded ? 'rotate-180 text-[#5b4cfa]' : ''"
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
        <div class="border-t border-slate-100 bg-slate-50/50 px-5 py-4">
          <div class="topic-preview text-[12.5px] leading-relaxed text-slate-600" v-html="renderedContent" />

          <!-- Open Discussion button -->
          <button
            v-if="!discussionOpen"
            type="button"
            class="mt-4 flex items-center gap-2 rounded-xl border border-dashed border-slate-200 px-4 py-2.5 text-[12px] font-semibold text-slate-500 transition hover:border-[#5b4cfa]/50 hover:bg-[#5b4cfa]/[0.03] hover:text-[#5b4cfa]"
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
          <div v-if="discussionOpen" class="border-t border-[#5b4cfa]/15 bg-[#5b4cfa]/[0.02]" @click.stop>

            <!-- Discussion header -->
            <div class="flex items-center justify-between px-5 py-3">
              <div class="flex items-center gap-2">
                <svg class="h-3.5 w-3.5 text-[#5b4cfa]" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
                </svg>
                <span class="text-[11px] font-bold uppercase tracking-[0.12em] text-[#5b4cfa]">Discussion</span>
                <span v-if="comments.length > 0"
                  class="rounded-full bg-[#5b4cfa]/10 px-1.5 py-0.5 text-[10px] font-semibold text-[#5b4cfa]">
                  {{ comments.length }}
                </span>
              </div>

              <!-- End Discussion -->
              <button
                v-if="!confirmEnd"
                type="button"
                class="flex items-center gap-1.5 rounded-lg px-2.5 py-1.5 text-[11px] font-semibold text-slate-400 transition hover:bg-red-50 hover:text-red-500"
                @click="confirmEnd = true"
              >
                <svg class="h-3 w-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M18 6 6 18M6 6l12 12" />
                </svg>
                End Discussion
              </button>

              <!-- Inline confirmation -->
              <div v-else class="flex items-center gap-2">
                <span class="text-[11px] text-slate-500">Clear all comments?</span>
                <button type="button"
                  class="rounded-lg bg-red-500 px-2.5 py-1 text-[11px] font-semibold text-white transition hover:bg-red-600"
                  @click="onEndDiscussion">End it</button>
                <button type="button"
                  class="rounded-lg border border-slate-200 px-2.5 py-1 text-[11px] font-semibold text-slate-600 transition hover:bg-slate-50"
                  @click="confirmEnd = false">Cancel</button>
              </div>
            </div>

            <!-- Comments -->
            <div class="px-5">
              <!-- Empty state -->
              <div v-if="comments.length === 0"
                class="mb-4 flex flex-col items-center gap-1 rounded-xl border border-dashed border-slate-200 py-6 text-center">
                <svg class="h-6 w-6 text-slate-300" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
                </svg>
                <p class="text-[12px] text-slate-400">No comments yet. Start the discussion.</p>
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

                  <div class="group/c min-w-0 flex-1">
                    <div class="mb-1 flex items-center gap-2">
                      <span class="text-[12px] font-semibold text-slate-700">{{ comment.authorName }}</span>
                      <span class="text-[11px] text-slate-400">· {{ comment.createdAt }}</span>
                      <div
                        v-if="comment.authorId === currentUserId && editingId !== comment.id"
                        class="ml-auto flex items-center gap-0.5 opacity-0 transition-opacity group-hover/c:opacity-100"
                      >
                        <button type="button"
                          class="flex h-6 w-6 items-center justify-center rounded-md text-slate-400 transition hover:bg-slate-100 hover:text-slate-600"
                          title="Edit" @click="startEdit(comment)">
                          <svg class="h-3 w-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
                            <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
                          </svg>
                        </button>
                        <button type="button"
                          class="flex h-6 w-6 items-center justify-center rounded-md text-slate-400 transition hover:bg-red-50 hover:text-red-500"
                          title="Delete" @click="emit('delete-comment', comment.id)">
                          <svg class="h-3 w-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M3 6h18" /><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
                          </svg>
                        </button>
                      </div>
                    </div>

                    <p v-if="editingId !== comment.id" class="text-[12.5px] leading-relaxed text-slate-600">
                      {{ comment.text }}
                    </p>

                    <div v-else>
                      <textarea v-model="editText" rows="2"
                        class="w-full resize-none rounded-lg border border-[#5b4cfa]/40 bg-white px-3 py-2 text-[12.5px] leading-relaxed text-slate-700 outline-none ring-2 ring-[#5b4cfa]/10"
                        @keydown.enter.ctrl="saveEdit" @keydown.escape="cancelEdit" />
                      <div class="mt-1.5 flex gap-2">
                        <button type="button"
                          class="rounded-lg bg-[#5b4cfa] px-3 py-1 text-[11px] font-semibold text-white transition hover:bg-[#4d3ee0]"
                          @click="saveEdit">Save</button>
                        <button type="button"
                          class="rounded-lg border border-slate-200 px-3 py-1 text-[11px] font-semibold text-slate-600 transition hover:bg-slate-50"
                          @click="cancelEdit">Cancel</button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- Add comment form -->
            <div class="flex items-start gap-3 border-t border-[#5b4cfa]/10 px-5 py-4">
              <div class="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-[#5b7cff] to-[#5b4cfa] text-[10px] font-bold text-white">
                AD
              </div>
              <div class="flex flex-1 items-end gap-2">
                <textarea v-model="newComment" rows="1" placeholder="Add a comment…"
                  class="flex-1 resize-none rounded-xl border border-slate-200 bg-white px-3 py-2 text-[12.5px] leading-relaxed text-slate-700 outline-none transition placeholder:text-slate-400 focus:border-[#5b4cfa]/40 focus:ring-2 focus:ring-[#5b4cfa]/10"
                  @keydown.enter.prevent="submitComment" />
                <button type="button"
                  class="flex h-8 w-8 shrink-0 items-center justify-center rounded-xl bg-[#5b4cfa] text-white shadow-sm shadow-[#5b4cfa]/30 transition hover:bg-[#4d3ee0] disabled:opacity-40"
                  :disabled="!newComment.trim()" @click="submitComment">
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
.topic-preview :deep(h1) { font-size: 1.25rem; font-weight: 700; color: #1e293b; margin: 0.75rem 0 0.375rem; }
.topic-preview :deep(h2) { font-size: 1.1rem; font-weight: 700; color: #1e293b; margin: 0.5rem 0 0.25rem; }
.topic-preview :deep(h3) { font-size: 1rem; font-weight: 600; color: #334155; margin: 0.375rem 0 0.125rem; }
.topic-preview :deep(ul) { list-style-type: disc; padding-left: 1.25rem; margin: 0.375rem 0; }
.topic-preview :deep(ol) { list-style-type: decimal; padding-left: 1.25rem; margin: 0.375rem 0; }
.topic-preview :deep(li) { margin-bottom: 0.125rem; }
.topic-preview :deep(img) { max-width: 100%; height: auto; border-radius: 6px; margin: 8px 0; }
</style>
