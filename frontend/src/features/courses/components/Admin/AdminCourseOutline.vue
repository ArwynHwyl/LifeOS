<script setup lang="ts">
import { computed, ref, watch } from 'vue'

type Comment = {
  id: string
  authorId: string
  authorName: string
  text: string
  createdAt: string
}
import {
  type AdminModuleDto,
  type BackendCourseStatus,
} from '@/features/courses/services/adminCourses'

const props = defineProps<{
  courseId: string
  courseStatus: BackendCourseStatus
  modules: AdminModuleDto[]
}>()

const emit = defineEmits<{
  reload: []
}>()

const localModules = ref<AdminModuleDto[]>([...props.modules])

watch(
  () => props.modules,
  (newModules) => { localModules.value = [...newModules] },
)

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
</script>

<template>
  <section class="mb-5 rounded-[18px] border-2 border-lm-line bg-lm-surface p-5 shadow-stamp-sm">
    <!-- Header -->
    <div class="mb-4">
      <h2 class="font-display text-[14px] font-bold text-lm-ink">Module Discussions</h2>
      <p class="mt-1 font-mono text-[12px] font-medium text-lm-ink-3">
        Discuss and add comments for each module
      </p>
    </div>

    <!-- Module list -->
    <div v-if="localModules.length" class="space-y-3">
      <article
        v-for="(module, moduleIndex) in localModules"
        :key="module.id"
        class="overflow-hidden rounded-xl border-2 border-lm-line-soft bg-lm-surface"
      >
        <!-- Module row -->
        <div
          class="flex w-full items-center gap-3 px-4 py-3 text-left border-b-2 border-lm-line-soft"
        >
          <span class="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg border-2 border-lm-line bg-lm-yellow font-display text-[12px] font-bold text-lm-ink shadow-stamp-sm">
            {{ moduleIndex + 1 }}
          </span>
          <span class="min-w-0 flex-1">
            <span class="block truncate font-display text-[13px] font-bold text-lm-ink">{{ module.title }}</span>
          </span>
        </div>

        <!-- Discussion panel (always shown at bottom of each module card) -->
        <div class="bg-lm-bg-soft" @click.stop>
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
    </div>
  </section>
</template>
