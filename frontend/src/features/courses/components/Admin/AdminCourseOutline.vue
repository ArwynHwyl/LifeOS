<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'

type Comment = {
  id: string | number
  authorId: string
  authorName: string
  text: string
  createdAt: string
  subTopicId?: number | null
}
import {
  type AdminModuleDto,
  type BackendCourseStatus,
  getReviewComments,
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
  (newModules) => {
    localModules.value = [...newModules]
    void loadComments()
  },
)

const commentsMap = ref<Record<string, Comment[]>>({})

onMounted(loadComments)

function formatDate(value: string) {
  const date = new Date(value)
  const time = date.getTime()
  if (Number.isNaN(time)) return 'recently'

  const diffMs = Date.now() - time
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour

  if (diffMs < minute) return 'just now'
  if (diffMs < hour) return `${Math.floor(diffMs / minute)} min ago`
  if (diffMs < day) return `${Math.floor(diffMs / hour)} hours ago`
  if (diffMs < 2 * day) return 'yesterday'
  if (diffMs < 7 * day) return `${Math.floor(diffMs / day)} days ago`

  return date.toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })
}

async function loadComments() {
  try {
    const reviews = await getReviewComments(props.courseId)
    const moduleIdBySubTopicId = new Map<number, number>()
    for (const mod of localModules.value) {
      for (const subTopic of mod.subTopics ?? []) {
        moduleIdBySubTopicId.set(subTopic.id, mod.id)
      }
    }
    const tempMap: Record<string, Comment[]> = {}
    if (reviews) {
      for (const review of reviews) {
        for (const comment of review.comments ?? []) {
          const moduleId = comment.moduleId
            ?? (comment.subTopicId !== null ? moduleIdBySubTopicId.get(comment.subTopicId) : undefined)
          if (moduleId === undefined || moduleId === null) continue
          const modIdStr = String(moduleId)
          if (!tempMap[modIdStr]) {
            tempMap[modIdStr] = []
          }
          tempMap[modIdStr].push({
            id: comment.id,
            authorId: review.reviewerId,
            authorName: review.reviewerName || 'Reviewer',
            text: comment.feedback,
            createdAt: formatDate(comment.createdAt),
            subTopicId: comment.subTopicId,
          })
        }
      }
    }
    commentsMap.value = tempMap
  } catch (err) {
    console.error('Failed to load review comments', err)
  }
}

const AVATAR_PALETTE = ['bg-violet-500', 'bg-blue-500', 'bg-teal-500', 'bg-amber-500', 'bg-rose-500', 'bg-indigo-500']
function avatarBg(name: string): string {
  let h = 0
  for (const c of name) h = (h * 31 + c.charCodeAt(0)) & 0xffff
  return AVATAR_PALETTE[h % AVATAR_PALETTE.length]
}
function initials(name: string): string {
  return name.split(' ').map(w => w[0] ?? '').slice(0, 2).join('').toUpperCase()
}

function moduleLevelComments(moduleId: number): Comment[] {
  return (commentsMap.value[String(moduleId)] ?? []).filter((comment) => !comment.subTopicId)
}

function subTopicGroups(module: AdminModuleDto): Array<{ id: number; title: string; index: number; comments: Comment[] }> {
  const comments = commentsMap.value[String(module.id)] ?? []
  const sorted = [...(module.subTopics ?? [])].sort((a, b) => a.sortOrder - b.sortOrder)
  const groups: Array<{ id: number; title: string; index: number; comments: Comment[] }> = []
  sorted.forEach((subTopic, index) => {
    const subTopicComments = comments.filter((comment) => comment.subTopicId === subTopic.id)
    if (subTopicComments.length) {
      groups.push({ id: subTopic.id, title: subTopic.title, index: index + 1, comments: subTopicComments })
    }
  })
  return groups
}

function moduleCommentCount(moduleId: number): number {
  return (commentsMap.value[String(moduleId)] ?? []).length
}
</script>

<template>
  <section class="mb-5 rounded-[18px] border-2 border-lm-line bg-lm-surface p-5 shadow-stamp-sm">
    <!-- Header -->
    <div class="mb-4">
      <h2 class="font-display text-[14px] font-bold text-lm-ink">Reviewer Feedback</h2>
      <p class="mt-1 font-mono text-[12px] font-medium text-lm-ink-3">
        Comments from teacher reviews, grouped by module and subtopic
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
          <span
            v-if="moduleCommentCount(module.id) > 0"
            class="inline-flex items-center gap-1.5 rounded-full border-2 border-lm-line bg-lm-yellow/40 px-2.5 py-0.5 font-mono text-[10px] font-bold text-lm-ink"
          >
            <svg class="h-3 w-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
              <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
            </svg>
            {{ moduleCommentCount(module.id) }}
          </span>
        </div>

        <!-- Reviewer feedback (read-only) -->
        <div class="bg-lm-bg-soft px-4 py-3" @click.stop>
          <div v-if="moduleCommentCount(module.id) === 0" class="flex items-center gap-2 rounded-[12px] border-2 border-dashed border-lm-line-soft px-4 py-3">
            <svg class="h-3.5 w-3.5 text-lm-line-soft" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
            </svg>
            <p class="text-[12px] text-lm-ink-3">No reviewer feedback yet.</p>
          </div>

          <div v-else class="flex flex-col gap-3">
            <!-- Module-level comments -->
            <div v-if="moduleLevelComments(module.id).length" class="rounded-[12px] border-2 border-lm-line-soft bg-lm-surface px-4 py-3">
              <p class="mb-2 font-mono text-[10px] font-bold uppercase tracking-[0.12em] text-lm-ink-3">Whole module</p>
              <div class="flex flex-col gap-3">
                <div v-for="comment in moduleLevelComments(module.id)" :key="comment.id" class="flex gap-3">
                  <div class="flex h-7 w-7 shrink-0 items-center justify-center rounded-full text-[10px] font-bold text-white" :class="avatarBg(comment.authorName)">
                    {{ initials(comment.authorName) }}
                  </div>
                  <div class="min-w-0 flex-1">
                    <div class="mb-0.5 flex items-center gap-2">
                      <span class="text-[12px] font-semibold text-lm-ink">{{ comment.authorName }}</span>
                      <span class="text-[11px] text-lm-ink-3">· {{ comment.createdAt }}</span>
                    </div>
                    <p class="text-[12.5px] leading-relaxed text-lm-ink-2">{{ comment.text }}</p>
                  </div>
                </div>
              </div>
            </div>

            <!-- Per-subtopic comment groups -->
            <div
              v-for="group in subTopicGroups(module)"
              :key="group.id"
              class="rounded-[12px] border-2 border-lm-line-soft bg-lm-surface px-4 py-3"
            >
              <p class="mb-2 flex items-center gap-2 font-mono text-[10px] font-bold uppercase tracking-[0.12em] text-lm-purple">
                <span class="rounded-md border border-lm-line-soft bg-lm-bg-soft px-1.5 py-px">{{ moduleIndex + 1 }}.{{ group.index }}</span>
                <span class="truncate normal-case tracking-normal">{{ group.title }}</span>
              </p>
              <div class="flex flex-col gap-3">
                <div v-for="comment in group.comments" :key="comment.id" class="flex gap-3">
                  <div class="flex h-7 w-7 shrink-0 items-center justify-center rounded-full text-[10px] font-bold text-white" :class="avatarBg(comment.authorName)">
                    {{ initials(comment.authorName) }}
                  </div>
                  <div class="min-w-0 flex-1">
                    <div class="mb-0.5 flex items-center gap-2">
                      <span class="text-[12px] font-semibold text-lm-ink">{{ comment.authorName }}</span>
                      <span class="text-[11px] text-lm-ink-3">· {{ comment.createdAt }}</span>
                    </div>
                    <p class="text-[12.5px] leading-relaxed text-lm-ink-2">{{ comment.text }}</p>
                  </div>
                </div>
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
