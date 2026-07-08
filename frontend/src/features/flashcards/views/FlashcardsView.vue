<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import LmIcon from '@/features/learning/components/LmIcon.vue'
import {
  listDecks,
  getSrsQueuePage,
  type FlashcardDeckSummaryDto,
  type SrsQueuePageDto,
} from '../services/flashcard'

const router = useRouter()

const QUEUE_PAGE_SIZE = 5

const TAG_VISUALS: Record<string, { icon: string; bgClass: string }> = {
  ALGEBRA: { icon: 'x²', bgClass: 'bg-lm-alg' },
  TRIG: { icon: 'sin', bgClass: 'bg-lm-tri' },
  CALCULUS: { icon: '∫', bgClass: 'bg-lm-cal' },
  GEOMETRY: { icon: '△', bgClass: 'bg-lm-geo' },
  STATS: { icon: 'σ', bgClass: 'bg-lm-sta' },
}
const DEFAULT_VISUAL = { icon: '?', bgClass: 'bg-lm-bg-soft' }

function visualFor(tag: string) {
  return TAG_VISUALS[tag] ?? DEFAULT_VISUAL
}

const decks = ref<FlashcardDeckSummaryDto[]>([])
const queuePage = ref<SrsQueuePageDto | null>(null)
const queuePageIndex = ref(0)
const loadingDecks = ref(true)
const loadingQueue = ref(true)
const selectedTag = ref('All')

const uniqueTags = computed(() => {
  const tags = new Set(decks.value.map((d) => d.tag))
  return Array.from(tags)
})

const filteredDecks = computed(() => {
  if (selectedTag.value === 'All') return decks.value
  return decks.value.filter((d) => d.tag === selectedTag.value)
})

const totalDueNow = computed(() => queuePage.value?.totalDueNow ?? 0)
const totalTracked = computed(() => queuePage.value?.totalTracked ?? 0)
const totalDueLaterToday = computed(() => queuePage.value?.totalDueLaterToday ?? 0)
const totalPages = computed(() => Math.max(1, Math.ceil(totalDueNow.value / QUEUE_PAGE_SIZE)))
const hasNextPage = computed(() => queuePageIndex.value + 1 < totalPages.value)

async function loadDecks() {
  loadingDecks.value = true
  try {
    decks.value = await listDecks()
  } finally {
    loadingDecks.value = false
  }
}

async function loadQueuePage(page: number) {
  loadingQueue.value = true
  try {
    queuePage.value = await getSrsQueuePage(page, QUEUE_PAGE_SIZE)
    queuePageIndex.value = page
  } finally {
    loadingQueue.value = false
  }
}

function changeQueuePage(page: number) {
  if (page < 0 || page >= totalPages.value) return
  void loadQueuePage(page)
}

function tagLabel(tag: string) {
  return tag.charAt(0) + tag.slice(1).toLowerCase()
}

onMounted(() => {
  void loadDecks()
  void loadQueuePage(0)
})
</script>

<template>
  <main class="flex-1 overflow-auto bg-lm-bg relative">
    <div class="absolute inset-0 bg-dot-grid opacity-40 pointer-events-none" />

    <div class="relative max-w-[1280px] mx-auto px-9 py-7">

      <!-- Header -->
      <div class="mb-6">
        <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3">FLASHCARDS</span>
        <h1 class="font-display text-[42px] font-bold tracking-tight text-lm-ink leading-tight mt-1 mb-1.5 m-0">
          Lock formulas
          <span class="inline-block bg-lm-yellow px-2 rounded-[6px] border-2 border-lm-line shadow-stamp-sm -rotate-1 whitespace-nowrap">
            into memory
          </span>
        </h1>
        <p class="text-[15px] text-lm-ink-2 mt-2">Review your queue · practice pre-made math decks</p>
      </div>

      <!-- Queue section -->
      <section class="mb-9">
        <div class="flex items-baseline gap-3 mb-3.5">
          <h2 class="font-display text-[22px] font-bold text-lm-ink m-0">Today's review queue</h2>
          <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3">SPACED REPETITION</span>
          <span class="px-2.5 py-1 text-[11px] font-semibold border border-lm-line rounded-full bg-lm-rust-soft text-lm-ink">{{ totalDueNow }} due now</span>
        </div>

        <div class="grid gap-[18px]" style="grid-template-columns: 1.4fr 1fr">

          <!-- Hero card -->
          <div class="relative flex items-center gap-6 p-6 bg-lm-yellow border-2 border-lm-line rounded-[24px] shadow-stamp-lg overflow-hidden">
            <svg class="absolute top-[-30px] right-[-30px] opacity-15 pointer-events-none" width="140" height="140" viewBox="0 0 80 80">
              <circle cx="40" cy="40" r="30" stroke="#1a1814" stroke-width="2" fill="none"/>
              <path d="M10 40 H 70 M 40 10 V 70" stroke="#1a1814" stroke-width="1.5" fill="none" stroke-dasharray="3 5"/>
            </svg>
            <svg class="absolute bottom-2.5 left-2.5 opacity-15 pointer-events-none" width="80" height="80" viewBox="0 0 80 80">
              <path d="M2 30 Q 15 5, 28 30 T 54 30 T 78 30" stroke="#1a1814" stroke-width="2" fill="none" stroke-linecap="round"/>
            </svg>

            <!-- Stacked cards preview -->
            <div class="relative w-[130px] h-[160px] shrink-0">
              <div
                v-for="(c, i) in (queuePage?.items ?? []).slice(0, 4)"
                :key="c.srsCardId"
                class="absolute flex flex-col justify-between p-2.5 bg-lm-surface border-2 border-lm-line rounded-[12px] shadow-stamp-sm"
                :style="{ left: `${i * 8}px`, top: `${i * 4}px`, width: '100px', height: '140px', transform: `rotate(${(i - 1.5) * 4}deg)`, zIndex: i }"
              >
                <span class="font-mono text-[8px] font-semibold tracking-widest uppercase text-lm-ink-3">{{ c.deckTag }}</span>
                <p class="text-[11px] font-semibold leading-tight text-lm-ink m-0">{{ c.front }}</p>
                <span class="font-mono text-[9px] text-lm-ink-3">FRONT</span>
              </div>
            </div>

            <div class="flex-1 min-w-0 relative">
              <h3 class="font-display text-[36px] font-bold leading-none tracking-tight text-lm-ink m-0">{{ totalTracked }} cards waiting</h3>
              <p class="text-[14px] text-lm-ink mt-1.5 mb-0">
                <strong>{{ totalDueNow }} due right now</strong> · {{ totalDueLaterToday }} coming later today
              </p>
              <p class="font-mono text-[11px] text-lm-ink-2 mt-1 mb-0">AGAIN / HARD / GOOD / EASY</p>
              <button
                @click="router.push('/learn/flashcards/srs')"
                :disabled="totalDueNow === 0"
                class="flex items-center gap-2 mt-3.5 px-6 py-3 text-[17px] font-semibold border-2 border-lm-line rounded-full bg-lm-ink text-lm-bg shadow-stamp-sm hover:-translate-y-px hover:shadow-stamp-md transition-all duration-200 disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:translate-y-0 disabled:hover:shadow-stamp-sm"
              >
                Start review
                <LmIcon name="arrow" :size="18" />
              </button>
            </div>
          </div>

          <!-- Queue list -->
          <div class="flex flex-col bg-lm-surface border-2 border-lm-line rounded-[24px] shadow-stamp-md overflow-hidden">
            <div class="flex justify-between items-baseline px-[18px] py-3.5 border-b border-lm-line-soft">
              <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3">UP NEXT</span>
              <span class="font-mono text-[10px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3">{{ totalDueNow }} DUE</span>
            </div>
            <div class="flex-1 overflow-auto min-h-[140px]">
              <p v-if="!loadingQueue && (queuePage?.items.length ?? 0) === 0" class="px-[18px] py-6 text-[13px] text-lm-ink-2 text-center">
                Nothing due right now. Check back later.
              </p>
              <div
                v-for="(c, i) in queuePage?.items ?? []"
                :key="c.srsCardId"
                :class="['flex items-center gap-2.5 px-[18px] py-2.5 bg-lm-yellow-soft', i < (queuePage?.items.length ?? 0) - 1 ? 'border-b border-lm-line-soft' : '']"
              >
                <div class="w-2 h-2 rounded-full shrink-0 bg-lm-rust" />
                <div class="flex-1 min-w-0">
                  <p class="text-[13.5px] font-semibold truncate text-lm-ink m-0">{{ c.front }}</p>
                  <span class="font-mono text-[9px] font-semibold tracking-widest uppercase text-lm-ink-3">{{ c.deckTag }}</span>
                </div>
                <span class="px-2.5 py-0.5 text-[11px] font-semibold border border-lm-line rounded-full shrink-0 bg-lm-rust-soft">NOW</span>
              </div>
            </div>
            <div v-if="totalDueNow > QUEUE_PAGE_SIZE" class="flex items-center justify-between px-[18px] py-2.5 border-t border-lm-line-soft shrink-0">
              <button
                :disabled="queuePageIndex === 0"
                @click="changeQueuePage(queuePageIndex - 1)"
                class="flex items-center gap-1 px-2.5 py-1 text-xs font-semibold border-2 border-lm-line rounded-full bg-lm-surface disabled:opacity-30 disabled:cursor-not-allowed hover:bg-lm-bg-soft transition-all duration-200"
              >
                <LmIcon name="back" :size="12" />
                Prev
              </button>
              <span class="font-mono text-[10px] text-lm-ink-3">PAGE {{ queuePageIndex + 1 }} / {{ totalPages }}</span>
              <button
                :disabled="!hasNextPage"
                @click="changeQueuePage(queuePageIndex + 1)"
                class="flex items-center gap-1 px-2.5 py-1 text-xs font-semibold border-2 border-lm-line rounded-full bg-lm-surface disabled:opacity-30 disabled:cursor-not-allowed hover:bg-lm-bg-soft transition-all duration-200"
              >
                Next
                <LmIcon name="arrow" :size="12" />
              </button>
            </div>
          </div>
        </div>
      </section>

      <!-- Decks section -->
      <section>
        <div class="flex items-baseline gap-3 mb-3.5 flex-wrap">
          <h2 class="font-display text-[22px] font-bold text-lm-ink m-0">Pre-made decks</h2>
          <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3">FREE PRACTICE</span>
          <span class="text-[13px] text-lm-ink-2">· pass / not pass · repeat anytime</span>
          <div class="flex-1" />
          <div class="flex gap-1.5 flex-wrap">
            <button
              @click="selectedTag = 'All'"
              :class="['px-3 py-1 text-xs font-semibold border-2 border-lm-line rounded-full transition-all duration-200', selectedTag === 'All' ? 'bg-lm-ink text-lm-bg' : 'bg-lm-surface text-lm-ink hover:bg-lm-bg-soft']"
            >All</button>
            <button
              v-for="t in uniqueTags"
              :key="t"
              @click="selectedTag = t"
              :class="['px-3 py-1 text-xs font-semibold border-2 border-lm-line rounded-full transition-all duration-200', selectedTag === t ? 'bg-lm-ink text-lm-bg' : 'bg-lm-surface text-lm-ink hover:bg-lm-bg-soft']"
            >{{ tagLabel(t) }}</button>
          </div>
        </div>

        <p v-if="!loadingDecks && filteredDecks.length === 0" class="text-[14px] text-lm-ink-2 py-6 text-center">
          No decks in this category yet.
        </p>

        <div class="grid grid-cols-3 gap-[18px]">
          <div
            v-for="d in filteredDecks"
            :key="d.id"
            @click="router.push(`/learn/flashcards/set/${d.id}`)"
            class="flex gap-3.5 items-center p-4 bg-lm-surface border-2 border-lm-line rounded-[18px] shadow-stamp-md cursor-pointer hover:-translate-y-0.5 hover:shadow-stamp-lg transition-all duration-200"
          >
            <!-- Stacked deck visual -->
            <div class="relative w-[70px] h-[92px] shrink-0">
              <div :class="['absolute inset-[4px_-4px_-4px_4px] rounded-[8px] border-2 border-lm-line opacity-55', visualFor(d.tag).bgClass]" />
              <div :class="['absolute inset-0 rounded-[8px] border-2 border-lm-line flex items-center justify-center', visualFor(d.tag).bgClass]">
                <span class="font-math italic font-bold text-[28px] text-lm-ink">{{ visualFor(d.tag).icon }}</span>
              </div>
            </div>

            <div class="flex-1 min-w-0">
              <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3">{{ d.tag }}</span>
              <h3 class="font-display text-[17px] font-bold text-lm-ink leading-tight mt-0.5 mb-1 m-0">{{ d.title }}</h3>
              <p class="font-mono text-[11px] text-lm-ink-2 m-0">{{ d.cardCount }} CARDS</p>
              <p class="flex items-center gap-1.5 text-[13px] font-semibold text-lm-ink mt-1.5 m-0">
                Practice <LmIcon name="arrow" :size="14" />
              </p>
            </div>
          </div>
        </div>
      </section>
    </div>
  </main>
</template>
