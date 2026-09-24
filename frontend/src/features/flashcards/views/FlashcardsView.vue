<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import LmIcon from '@/features/learning/components/LmIcon.vue'
import {
  listDecks,
  getSrsQueuePage,
  type FlashcardDeckSummaryDto,
  type SrsCardDto,
  type SrsQueuePageDto,
} from '../services/flashcard'

const router = useRouter()

const QUEUE_PAGE_SIZE = 5

const TAG_VISUALS: Record<string, { icon: string; bgClass: string }> = {
  ALGEBRA: { icon: 'x²', bgClass: 'bg-lx-macaw' },
  TRIG: { icon: 'sin', bgClass: 'bg-lx-fox' },
  CALCULUS: { icon: '∫', bgClass: 'bg-lx-beetle' },
  GEOMETRY: { icon: '△', bgClass: 'bg-lx-feather' },
  STATS: { icon: 'σ', bgClass: 'bg-lx-eel' },
}
const DEFAULT_VISUAL = { icon: '?', bgClass: 'bg-lx-eel' }

function visualFor(tag: string) {
  return TAG_VISUALS[tag] ?? DEFAULT_VISUAL
}

const decks = ref<FlashcardDeckSummaryDto[]>([])
const queuePage = ref<SrsQueuePageDto | null>(null)
const queuePageIndex = ref(0)
const heroPreviewItems = ref<SrsCardDto[]>([])
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

const heroPreviewCards = computed(() => heroPreviewItems.value.slice(0, 4))

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
    const data = await getSrsQueuePage(page, QUEUE_PAGE_SIZE)
    queuePage.value = data
    queuePageIndex.value = page
    if (page === 0) {
      heroPreviewItems.value = data.items
    }
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
  <main class="flex-1 bg-white">
    <div class="max-w-[1180px] mx-auto px-8 py-8">

      <!-- Header -->
      <div class="mb-6">
        <h1 class="font-display text-[34px] font-extrabold tracking-tight text-lx-ink leading-tight mt-1 m-0">
          Formulas into memory
        </h1>
      </div>

      <!-- Queue section -->
      <section class="mb-9">
        <div class="flex items-baseline gap-3 mb-3.5">
          <h2 class="font-display text-[19px] font-extrabold text-lx-ink m-0">Today's review queue</h2>
          <span class="px-2.5 py-1 text-[11px] font-extrabold rounded-full bg-lx-fox/10 text-lx-fox-dark">{{ totalDueNow }} due now</span>
        </div>

        <div class="grid gap-4" style="grid-template-columns: 1.4fr 1fr">

          <!-- Hero card -->
          <div class="anim-rise relative flex items-center gap-6 p-7 bg-lx-fox rounded-[24px] overflow-hidden">
            <!-- Stacked cards preview -->
            <div class="anim-float relative w-[130px] h-[160px] shrink-0">
              <div
                v-for="(c, i) in heroPreviewCards"
                :key="c.srsCardId"
                class="absolute flex flex-col justify-between p-2.5 bg-white rounded-2xl shadow-[0_8px_20px_-8px_rgba(0,0,0,0.35)]"
                :style="{ left: `${i * 8}px`, top: `${i * 4}px`, width: '100px', height: '140px', transform: `rotate(${(i - 1.5) * 4}deg)`, zIndex: heroPreviewCards.length - i }"
              >
                <span class="font-mono text-[8px] font-bold tracking-widest uppercase text-lx-ink-faint">{{ c.deckTag }}</span>
                <p class="text-[11px] font-bold leading-tight text-lx-ink m-0">{{ c.front }}</p>
                <span class="font-mono text-[9px] text-lx-ink-faint">FRONT</span>
              </div>
            </div>

            <div class="flex-1 min-w-0 text-white">
              <h3 class="font-display text-[32px] font-extrabold leading-none tracking-tight m-0">{{ totalTracked }} cards waiting</h3>
              <p class="text-[14px] font-semibold text-white/90 mt-2 mb-0">
                {{ totalDueNow }} due right now / {{ totalDueLaterToday }} cards coming later today
              </p>
              <button
                @click="router.push('/learn/flashcards/srs')"
                :disabled="totalDueNow === 0"
                class="flex items-center gap-2 mt-4 px-6 py-3 text-[15px] font-extrabold rounded-2xl bg-white text-lx-fox-dark shadow-[0_4px_0_rgba(0,0,0,0.16)] transition-transform duration-75 active:translate-y-1 active:shadow-none disabled:opacity-40 disabled:pointer-events-none"
              >
                Start review
                <LmIcon name="arrow" :size="18" />
              </button>
            </div>
          </div>

          <!-- Queue list -->
          <div class="anim-rise flex flex-col bg-white border border-lx-line rounded-[24px] overflow-hidden" style="--i: 1">
            <div class="flex justify-between items-baseline px-[18px] py-3.5 border-b border-lx-line">
              <span class="font-mono text-[11px] font-bold tracking-[0.06em] uppercase text-lx-ink-faint">Up next</span>
              <span class="font-mono text-[10px] font-bold tracking-[0.06em] uppercase text-lx-ink-faint">{{ totalDueNow }} due</span>
            </div>
            <div class="flex-1 overflow-auto min-h-[140px]">
              <p v-if="!loadingQueue && (queuePage?.items.length ?? 0) === 0" class="px-[18px] py-6 text-[13px] text-lx-ink-faint text-center">
                Nothing due right now. Check back later.
              </p>
              <div
                v-for="(c, i) in queuePage?.items ?? []"
                :key="c.srsCardId"
                :style="{ '--i': i + 2 }"
                :class="['anim-rise flex items-center gap-2.5 px-[18px] py-2.5', i < (queuePage?.items.length ?? 0) - 1 ? 'border-b border-lx-line' : '']"
              >
                <div class="w-2 h-2 rounded-full shrink-0 bg-lx-fox" />
                <div class="flex-1 min-w-0">
                  <p class="text-[13.5px] font-bold truncate text-lx-ink m-0">{{ c.front }}</p>
                  <span class="font-mono text-[9px] font-bold tracking-widest uppercase text-lx-ink-faint">{{ c.deckTag }}</span>
                </div>
                <span class="px-2.5 py-0.5 text-[11px] font-extrabold rounded-full shrink-0 bg-lx-fox/10 text-lx-fox-dark">NOW</span>
              </div>
            </div>
            <div v-if="totalDueNow > QUEUE_PAGE_SIZE" class="flex items-center justify-between px-[18px] py-2.5 border-t border-lx-line shrink-0">
              <button
                :disabled="queuePageIndex === 0"
                @click="changeQueuePage(queuePageIndex - 1)"
                class="flex items-center gap-1 px-2.5 py-1.5 text-xs font-bold rounded-full bg-lx-surface-soft disabled:opacity-30 disabled:cursor-not-allowed hover:bg-lx-line transition-colors duration-150"
              >
                <LmIcon name="back" :size="12" />
                Prev
              </button>
              <span class="font-mono text-[10px] text-lx-ink-faint">PAGE {{ queuePageIndex + 1 }} / {{ totalPages }}</span>
              <button
                :disabled="!hasNextPage"
                @click="changeQueuePage(queuePageIndex + 1)"
                class="flex items-center gap-1 px-2.5 py-1.5 text-xs font-bold rounded-full bg-lx-surface-soft disabled:opacity-30 disabled:cursor-not-allowed hover:bg-lx-line transition-colors duration-150"
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
          <h2 class="font-display text-[19px] font-extrabold text-lx-ink m-0">Pre-set decks</h2>
          <div class="flex-1" />
          <div class="flex gap-1.5 flex-wrap">
            <button
              @click="selectedTag = 'All'"
              :class="['px-3.5 py-1.5 text-xs font-extrabold rounded-2xl transition-transform duration-75 active:translate-y-0.5', selectedTag === 'All' ? 'bg-lx-feather text-white shadow-[0_4px_0_var(--color-lx-feather-dark)] active:shadow-none' : 'bg-white border border-lx-line text-lx-ink-soft hover:border-lx-ink-faint']"
            >All</button>
            <button
              v-for="t in uniqueTags"
              :key="t"
              @click="selectedTag = t"
              :class="['px-3.5 py-1.5 text-xs font-extrabold rounded-2xl transition-transform duration-75 active:translate-y-0.5', selectedTag === t ? 'bg-lx-feather text-white shadow-[0_4px_0_var(--color-lx-feather-dark)] active:shadow-none' : 'bg-white border border-lx-line text-lx-ink-soft hover:border-lx-ink-faint']"
            >{{ tagLabel(t) }}</button>
          </div>
        </div>

        <p v-if="!loadingDecks && filteredDecks.length === 0" class="text-[14px] text-lx-ink-faint py-6 text-center">
          No decks in this category yet.
        </p>

        <div class="grid grid-cols-3 gap-4">
          <button
            v-for="(d, i) in filteredDecks"
            :key="d.id"
            :style="{ '--i': i }"
            type="button"
            @click="router.push(`/learn/flashcards/set/${d.id}`)"
            class="anim-rise press group flex gap-4 items-center p-4 text-left bg-white border border-lx-line rounded-[20px] cursor-pointer transition-all duration-150 hover:-translate-y-1 hover:shadow-[0_16px_32px_-18px_rgba(0,0,0,0.25)]"
          >
            <div :class="['flex items-center justify-center w-14 h-14 shrink-0 rounded-2xl transition-transform duration-300 group-hover:-rotate-6 group-hover:scale-110', visualFor(d.tag).bgClass]">
              <span class="font-display font-extrabold text-[20px] text-white">{{ visualFor(d.tag).icon }}</span>
            </div>

            <div class="flex-1 min-w-0">
              <span class="font-mono text-[10.5px] font-bold tracking-[0.06em] uppercase text-lx-ink-faint">{{ d.tag }}</span>
              <h3 class="font-display text-[16px] font-extrabold text-lx-ink leading-tight mt-0.5 mb-1 m-0">{{ d.title }}</h3>
              <p class="font-mono text-[10.5px] font-bold text-lx-ink-faint m-0">{{ d.cardCount }} CARDS</p>
              <p class="flex items-center gap-1.5 text-[13px] font-extrabold text-lx-macaw mt-1.5 m-0">
                Practice <LmIcon name="arrow" :size="14" class="transition-transform duration-150 group-hover:translate-x-0.5" />
              </p>
            </div>
          </button>
        </div>
      </section>
    </div>
  </main>
</template>
