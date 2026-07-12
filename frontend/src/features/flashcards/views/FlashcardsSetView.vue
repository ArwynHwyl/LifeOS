<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import LmIcon from '@/features/learning/components/LmIcon.vue'
import FlashcardFace from '../components/FlashcardFace.vue'
import { getDeckDetail, type FlashcardCardDto, type FlashcardDeckDetailDto } from '../services/flashcard'

const route = useRoute()
const router = useRouter()

const deckId = Number(route.params.deckId)

const deck = ref<FlashcardDeckDetailDto | null>(null)
const loading = ref(true)
const loadError = ref(false)

const round = ref<FlashcardCardDto[]>([])
const roundIndex = ref(0)
const notPassed = ref<FlashcardCardDto[]>([])
const passedCount = ref(0)
const missedCount = ref(0)
const roundPassed = ref(0)
const roundMissed = ref(0)
const isFlipped = ref(false)
const isComplete = ref(false)
const roundComplete = ref(false)

const currentCard = computed<FlashcardCardDto | null>(() => round.value[roundIndex.value] ?? null)
const totalCards = computed(() => deck.value?.cards.length ?? 0)
const cardsSeen = computed(() => passedCount.value + missedCount.value)
const progressPercent = computed(() => (totalCards.value === 0 ? 0 : Math.min(100, (cardsSeen.value / totalCards.value) * 100)))

function startSession() {
  if (!deck.value) return
  round.value = [...deck.value.cards]
  roundIndex.value = 0
  notPassed.value = []
  passedCount.value = 0
  missedCount.value = 0
  roundPassed.value = 0
  roundMissed.value = 0
  isFlipped.value = false
  isComplete.value = false
  roundComplete.value = false
}

async function loadDeck() {
  loading.value = true
  loadError.value = false
  try {
    deck.value = await getDeckDetail(deckId)
    startSession()
  } catch {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

function advance() {
  if (roundIndex.value < round.value.length - 1) {
    roundIndex.value += 1
    isFlipped.value = false
    return
  }
  if (notPassed.value.length > 0) {
    roundComplete.value = true
    return
  }
  isComplete.value = true
}

function markPass() {
  if (!currentCard.value) return
  if (!isFlipped.value) return
  passedCount.value += 1
  roundPassed.value += 1
  advance()
}

function markNotPass() {
  if (!currentCard.value || !deck.value) return
  if (!isFlipped.value) return
  missedCount.value += 1
  roundMissed.value += 1
  notPassed.value.push(currentCard.value)
  advance()
}

function reviewMissed() {
  round.value = notPassed.value
  notPassed.value = []
  roundIndex.value = 0
  roundPassed.value = 0
  roundMissed.value = 0
  isFlipped.value = false
  roundComplete.value = false
}

function exitToDecks() {
  router.push('/learn/flashcards')
}

function onFlip(flipped: boolean) {
  isFlipped.value = flipped
}

function restart() {
  startSession()
}

function onKeydown(event: KeyboardEvent) {
  if (isComplete.value || roundComplete.value) return
  if (event.key === '1' || event.key === 'ArrowLeft') markNotPass()
  else if (event.key === '2' || event.key === 'ArrowRight') markPass()
  else if (event.key === ' ') {
    event.preventDefault()
    isFlipped.value = !isFlipped.value
  }
}

onMounted(() => {
  void loadDeck()
  document.addEventListener('keydown', onKeydown)
})
onBeforeUnmount(() => document.removeEventListener('keydown', onKeydown))
</script>

<template>
  <main class="flex-1 flex flex-col bg-lm-bg overflow-hidden relative">
    <div class="absolute inset-0 bg-dot-grid opacity-40 pointer-events-none" />

    <!-- Top bar -->
    <div class="relative flex items-center gap-3.5 px-6 py-3.5 bg-lm-surface border-b-2 border-lm-line shrink-0">
      <button
        @click="router.back()"
        class="flex items-center gap-1.5 px-3 py-1.5 text-sm font-semibold border-2 border-lm-line rounded-full bg-lm-surface shadow-stamp-sm hover:-translate-y-px transition-all duration-200 text-lm-ink shrink-0"
      >
        <LmIcon name="back" :size="14" />
        Back to decks
      </button>
      <div class="flex-1 text-center">
        <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3 block">PRACTICE{{ deck ? ' · ' + deck.tag : '' }}</span>
        <span class="font-display font-bold text-[18px] text-lm-ink leading-tight block">{{ deck?.title ?? 'Loading…' }}</span>
      </div>
      <button
        @click="restart"
        :disabled="!deck"
        class="flex items-center gap-1.5 px-3 py-1.5 text-sm font-semibold border-2 border-lm-line rounded-full bg-lm-surface shadow-stamp-sm hover:-translate-y-px transition-all duration-200 text-lm-ink shrink-0 disabled:opacity-40 disabled:cursor-not-allowed"
      >
        <LmIcon name="refresh" :size="14" />
        Restart
      </button>
    </div>

    <div v-if="loading" class="flex-1 flex items-center justify-center relative">
      <p class="text-[14px] text-lm-ink-2">Loading deck…</p>
    </div>

    <div v-else-if="loadError" class="flex-1 flex items-center justify-center relative">
      <p class="text-[14px] text-lm-ink-2">Couldn't load this deck. <button class="underline" @click="loadDeck">Try again</button></p>
    </div>

    <template v-else-if="deck">
      <!-- Score strip -->
      <div class="relative flex items-center gap-3.5 px-6 py-2.5 bg-lm-surface border-b border-lm-line-soft shrink-0">
        <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3 shrink-0">CARD {{ Math.min(cardsSeen + 1, totalCards) }} / {{ totalCards }}</span>
        <span class="flex items-center gap-1.5 text-[13px]">
          <span class="text-lm-green"><LmIcon name="check" :size="14" /></span>
          <strong>{{ passedCount }}</strong> passed
        </span>
        <span class="flex items-center gap-1.5 text-[13px]">
          <span class="text-lm-red"><LmIcon name="close" :size="14" /></span>
          <strong>{{ missedCount }}</strong> missed
        </span>
        <div class="flex-1" />
        <div class="w-[180px] h-2.5 bg-lm-bg-soft rounded-full overflow-hidden border border-lm-line">
          <div class="h-full bg-lm-green transition-all duration-200" :style="{ width: `${progressPercent}%` }" />
        </div>
        <span class="font-mono text-[11px] text-lm-ink-2">{{ Math.round(progressPercent) }}%</span>
      </div>

      <!-- Complete state -->
      <div v-if="isComplete" class="flex-1 flex flex-col items-center justify-center gap-4 relative px-6 text-center">
        <h2 class="font-display text-[32px] font-bold text-lm-ink m-0">Deck complete!</h2>
        <p class="text-[15px] text-lm-ink-2 m-0">You passed all {{ totalCards }} cards in this deck.</p>
        <div class="flex gap-3 mt-2">
          <button
            @click="restart"
            class="flex items-center gap-2 px-6 py-3 text-[15px] font-semibold border-2 border-lm-line rounded-full bg-lm-ink text-lm-bg shadow-stamp-sm hover:-translate-y-px hover:shadow-stamp-md transition-all duration-200"
          >
            <LmIcon name="refresh" :size="16" />
            Practice again
          </button>
          <button
            @click="router.push('/learn/flashcards')"
            class="flex items-center gap-2 px-6 py-3 text-[15px] font-semibold border-2 border-lm-line rounded-full bg-lm-surface text-lm-ink shadow-stamp-sm hover:-translate-y-px hover:shadow-stamp-md transition-all duration-200"
          >
            Back to decks
          </button>
        </div>
      </div>

      <!-- Round complete interstitial -->
      <div v-else-if="roundComplete" class="flex-1 flex flex-col items-center justify-center gap-4 relative px-6 text-center">
        <h2 class="font-display text-[32px] font-bold text-lm-ink m-0">Round complete</h2>
        <p class="text-[15px] text-lm-ink-2 m-0">
          You got <strong class="text-lm-green">{{ roundPassed }}</strong> right and
          <strong class="text-lm-red">{{ roundMissed }}</strong> wrong this round.
        </p>
        <p class="text-[14px] text-lm-ink-2 m-0">
          {{ notPassed.length }} card{{ notPassed.length === 1 ? '' : 's' }} still need{{ notPassed.length === 1 ? 's' : '' }} review.
        </p>
        <div class="flex gap-3 mt-2">
          <button
            @click="reviewMissed"
            class="flex items-center gap-2 px-6 py-3 text-[15px] font-semibold border-2 border-lm-line rounded-full bg-lm-ink text-lm-bg shadow-stamp-sm hover:-translate-y-px hover:shadow-stamp-md transition-all duration-200"
          >
            <LmIcon name="refresh" :size="16" />
            Review missed cards
          </button>
          <button
            @click="exitToDecks"
            class="flex items-center gap-2 px-6 py-3 text-[15px] font-semibold border-2 border-lm-line rounded-full bg-lm-surface text-lm-ink shadow-stamp-sm hover:-translate-y-px hover:shadow-stamp-md transition-all duration-200"
          >
            Exit to decks
          </button>
        </div>
      </div>

      <template v-else-if="currentCard">
        <!-- Card pair -->
        <div class="flex-1 flex flex-col px-6 py-6 min-h-0 relative">
          <FlashcardFace
            :card-key="currentCard.id"
            :front="currentCard.front"
            :formula="currentCard.backText"
            :label="currentCard.front"
            :note="currentCard.note ?? undefined"
            :example="currentCard.example ?? undefined"
            :tags="currentCard.tags"
            @flip="onFlip"
          />
        </div>

        <!-- Pass / Not pass -->
        <div class="shrink-0 px-6 pb-5 pt-3.5 bg-lm-surface border-t-2 border-lm-line">
          <p class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3 text-center mb-2.5">
            {{ isFlipped ? 'DID YOU GET IT?' : 'CLICK THE CARD TO REVEAL THE ANSWER FIRST' }}
          </p>
          <div class="grid grid-cols-2 gap-3.5">
            <!-- Not pass -->
            <button
              @click="markNotPass"
              :disabled="!isFlipped"
              class="flex items-center justify-center gap-4 px-6 py-[18px] bg-lm-red-soft border-2 border-lm-line rounded-[24px] shadow-stamp-md cursor-pointer hover:-translate-y-0.5 hover:shadow-stamp-lg transition-all duration-200 disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:translate-y-0 disabled:hover:shadow-stamp-md"
            >
              <div class="w-[52px] h-[52px] rounded-full bg-lm-red text-lm-bg border-2 border-lm-line flex items-center justify-center shrink-0">
                <LmIcon name="close" :size="28" />
              </div>
              <div>
                <p class="font-display font-bold text-[24px] leading-none text-lm-ink m-0">Not pass</p>
                <p class="text-[12px] text-lm-ink-2 mt-1 m-0">will show again at end ·
                  <span class="font-mono">[1] or ←</span>
                </p>
              </div>
            </button>

            <!-- Pass -->
            <button
              @click="markPass"
              :disabled="!isFlipped"
              class="flex items-center justify-center gap-4 px-6 py-[18px] bg-lm-green-soft border-2 border-lm-line rounded-[24px] shadow-stamp-md cursor-pointer hover:-translate-y-0.5 hover:shadow-stamp-lg transition-all duration-200 disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:translate-y-0 disabled:hover:shadow-stamp-md"
            >
              <div class="w-[52px] h-[52px] rounded-full bg-lm-green text-lm-bg border-2 border-lm-line flex items-center justify-center shrink-0">
                <LmIcon name="check" :size="28" />
              </div>
              <div>
                <p class="font-display font-bold text-[24px] leading-none text-lm-ink m-0">Pass</p>
                <p class="text-[12px] text-lm-ink-2 mt-1 m-0">continue to next card ·
                  <span class="font-mono">[2] or →</span>
                </p>
              </div>
            </button>
          </div>
        </div>
      </template>
    </template>
  </main>
</template>
