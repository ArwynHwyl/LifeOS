<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import LmIcon from '@/features/learning/components/LmIcon.vue'
import FlashcardFace from '../components/FlashcardFace.vue'
import { getSrsDueSession, submitSrsReview, type SrsCardDto, type SrsOutcome } from '../services/flashcard'
import { useGamificationStore } from '@/features/gamified/stores/gamification'

const router = useRouter()
const gamificationStore = useGamificationStore()

const loading = ref(true)
const loadError = ref(false)
const submitting = ref(false)
const isFlipped = ref(false)
const isComplete = ref(false)

const sessionCards = ref<SrsCardDto[]>([])
const cardIndex = ref(0)

const currentCard = computed<SrsCardDto | null>(() => sessionCards.value[cardIndex.value] ?? null)
const totalCards = computed(() => sessionCards.value.length)
const progressPercent = computed(() => (totalCards.value === 0 ? 0 : (cardIndex.value / totalCards.value) * 100))
const streak = computed(() => gamificationStore.profile?.currentStreak ?? 0)

const recallButtons: { label: string; sub: string; next: string; key: string; bgClass: string; outcome: SrsOutcome }[] = [
  { label: 'Again', sub: 'forgot', next: '< 1 min', key: '1', bgClass: 'bg-lm-red-soft', outcome: 'AGAIN' },
  { label: 'Hard', sub: 'barely', next: '10 min', key: '2', bgClass: 'bg-lm-yellow-soft', outcome: 'HARD' },
  { label: 'Good', sub: 'solid', next: '1 day', key: '3', bgClass: 'bg-lm-green-soft', outcome: 'GOOD' },
  { label: 'Easy', sub: 'instant', next: '4 days', key: '4', bgClass: 'bg-lm-blue-soft', outcome: 'EASY' },
]

async function loadSession() {
  loading.value = true
  loadError.value = false
  try {
    sessionCards.value = await getSrsDueSession()
    cardIndex.value = 0
    isComplete.value = sessionCards.value.length === 0
  } catch {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

async function submitOutcome(outcome: SrsOutcome) {
  if (!currentCard.value || !isFlipped.value || submitting.value) return
  submitting.value = true
  try {
    const result = await submitSrsReview(currentCard.value.srsCardId, outcome)
    gamificationStore.handleReward(result.reward)
    if (cardIndex.value < sessionCards.value.length - 1) {
      cardIndex.value += 1
      isFlipped.value = false
    } else {
      isComplete.value = true
    }
  } finally {
    submitting.value = false
  }
}

function onFlip(flipped: boolean) {
  isFlipped.value = flipped
}

function onKeydown(event: KeyboardEvent) {
  if (isComplete.value || !isFlipped.value) return
  const match = recallButtons.find((b) => b.key === event.key)
  if (match) void submitOutcome(match.outcome)
}

onMounted(() => {
  void loadSession()
  document.addEventListener('keydown', onKeydown)
})
onBeforeUnmount(() => document.removeEventListener('keydown', onKeydown))
</script>

<template>
  <main class="flex-1 flex flex-col bg-lm-yellow-soft overflow-hidden relative">
    <div class="absolute inset-0 bg-dot-grid opacity-40 pointer-events-none" />

    <!-- Top bar -->
    <div class="relative flex items-center gap-3.5 px-6 py-3.5 bg-lm-surface border-b-2 border-lm-line shrink-0">
      <button
        @click="router.push('/learn/flashcards')"
        class="flex items-center gap-1.5 px-3 py-1.5 text-sm font-semibold border-2 border-lm-line rounded-full bg-lm-surface shadow-stamp-sm hover:-translate-y-px transition-all duration-200 text-lm-ink shrink-0"
      >
        <LmIcon name="back" :size="14" />
        Exit
      </button>
      <div class="flex-1 text-center">
        <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3 block">SRS QUEUE</span>
        <span class="font-display font-bold text-[18px] text-lm-ink leading-tight block">Review queue</span>
      </div>
      <span class="flex items-center gap-1.5 px-3 py-1 text-sm font-semibold border border-lm-line rounded-full bg-lm-rust-soft shrink-0">
        <span class="text-lm-rust"><LmIcon name="flame" :size="14" :filled="true" /></span>
        {{ streak }} day streak
      </span>
      <span class="px-3 py-1 text-sm font-semibold border border-lm-line rounded-full bg-lm-bg-soft text-lm-ink shrink-0">
        {{ Math.min(cardIndex + 1, totalCards) }} / {{ totalCards }}
      </span>
    </div>

    <div v-if="loading" class="flex-1 flex items-center justify-center relative">
      <p class="text-[14px] text-lm-ink-2">Loading your queue…</p>
    </div>

    <div v-else-if="loadError" class="flex-1 flex items-center justify-center relative">
      <p class="text-[14px] text-lm-ink-2">Couldn't load your queue. <button class="underline" @click="loadSession">Try again</button></p>
    </div>

    <div v-else-if="isComplete" class="flex-1 flex flex-col items-center justify-center gap-4 relative px-6 text-center">
      <h2 class="font-display text-[32px] font-bold text-lm-ink m-0">
        {{ totalCards === 0 ? 'Nothing due right now' : 'Queue cleared!' }}
      </h2>
      <p class="text-[15px] text-lm-ink-2 m-0">
        {{ totalCards === 0 ? 'Check back later as your cards become due.' : `You reviewed ${totalCards} card${totalCards === 1 ? '' : 's'}.` }}
      </p>
      <button
        @click="router.push('/learn/flashcards')"
        class="flex items-center gap-2 mt-2 px-6 py-3 text-[15px] font-semibold border-2 border-lm-line rounded-full bg-lm-ink text-lm-bg shadow-stamp-sm hover:-translate-y-px hover:shadow-stamp-md transition-all duration-200"
      >
        Back to flashcards
      </button>
    </div>

    <template v-else-if="currentCard">
      <!-- Progress strip -->
      <div class="shrink-0 px-6 py-2.5 bg-lm-surface border-b border-lm-line-soft">
        <div class="h-3.5 bg-lm-bg-soft rounded-full overflow-hidden border border-lm-line">
          <div class="h-full bg-lm-rust transition-all duration-200" :style="{ width: `${progressPercent}%` }" />
        </div>
      </div>

      <!-- Card pair -->
      <div class="flex-1 flex flex-col px-6 py-6 min-h-0 relative">
        <FlashcardFace
          :card-key="currentCard.srsCardId"
          :front="currentCard.front"
          :formula="currentCard.backText"
          :label="currentCard.deckTitle"
          :note="currentCard.note ?? undefined"
          :example="currentCard.example ?? undefined"
          :tags="currentCard.tags"
          @flip="onFlip"
        />
      </div>

      <!-- SRS recall bar -->
      <div class="shrink-0 px-6 pb-5 pt-3.5 bg-lm-surface border-t-2 border-lm-line">
        <p class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3 text-center mb-2.5">
          {{ isFlipped ? 'HOW WELL DID YOU RECALL?' : 'CLICK THE CARD TO REVEAL THE ANSWER FIRST' }}
        </p>
        <div class="grid grid-cols-4 gap-3">
          <button
            v-for="b in recallButtons"
            :key="b.label"
            @click="submitOutcome(b.outcome)"
            :disabled="!isFlipped || submitting"
            :class="['flex flex-col items-center px-3.5 py-3 border-2 border-lm-line rounded-[18px] shadow-stamp-sm cursor-pointer hover:-translate-y-px hover:shadow-stamp-md transition-all duration-200 disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:translate-y-0 disabled:hover:shadow-stamp-sm', b.bgClass]"
          >
            <span class="font-display font-bold text-[22px] tracking-tight text-lm-ink">{{ b.label }}</span>
            <span class="text-[13px] text-lm-ink-2 mt-0.5">{{ b.sub }}</span>
            <span class="font-mono text-[10px] text-lm-ink-3 mt-1.5">NEXT: {{ b.next }}</span>
            <span class="font-mono text-[9px] text-lm-ink-3 mt-0.5">PRESS [{{ b.key }}]</span>
          </button>
        </div>
      </div>
    </template>
  </main>
</template>
