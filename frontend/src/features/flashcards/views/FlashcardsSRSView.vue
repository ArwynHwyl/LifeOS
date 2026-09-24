<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import LmIcon from '@/features/learning/components/LmIcon.vue'
import FlashcardFace from '../components/FlashcardFace.vue'
import { watch } from 'vue'
import ToraMascot from '@/components/tora/ToraMascot.vue'
import ConfettiBurst from '@/components/motion/ConfettiBurst.vue'
import { getSrsDueSession, submitSrsReview, type SrsCardDto, type SrsOutcome } from '../services/flashcard'
import { useGamificationStore } from '@/features/gamified/stores/gamification'

const router = useRouter()
const gamificationStore = useGamificationStore()

const loading = ref(true)
const loadError = ref(false)
const submitting = ref(false)
const isFlipped = ref(false)
const isComplete = ref(false)
const confettiKey = ref(0)
watch(isComplete, (done) => { if (done && sessionCards.value.length > 0) confettiKey.value += 1 })

const sessionCards = ref<SrsCardDto[]>([])
const cardIndex = ref(0)

const currentCard = computed<SrsCardDto | null>(() => sessionCards.value[cardIndex.value] ?? null)
const totalCards = computed(() => sessionCards.value.length)
const progressPercent = computed(() => (totalCards.value === 0 ? 0 : (cardIndex.value / totalCards.value) * 100))
const streak = computed(() => gamificationStore.profile?.currentStreak ?? 0)

const recallButtons: { label: string; sub: string; next: string; key: string; bgClass: string; textClass: string; outcome: SrsOutcome }[] = [
  { label: 'Again', sub: 'forgot', next: '1 min', key: '1', bgClass: 'bg-red-50', textClass: 'text-red-600', outcome: 'AGAIN' },
  { label: 'Hard', sub: 'barely', next: '10 min', key: '2', bgClass: 'bg-lx-fox/10', textClass: 'text-lx-fox-dark', outcome: 'HARD' },
  { label: 'Good', sub: 'solid', next: '1 day', key: '3', bgClass: 'bg-lx-feather/10', textClass: 'text-lx-feather-dark', outcome: 'GOOD' },
  { label: 'Easy', sub: 'instant', next: '4 days', key: '4', bgClass: 'bg-lx-beetle/10', textClass: 'text-lx-beetle-dark', outcome: 'EASY' },
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
  <main class="flex-1 flex flex-col bg-lx-surface-soft overflow-hidden relative">

    <!-- Top bar -->
    <div class="relative flex items-center gap-3.5 px-6 py-3.5 bg-white border-b border-lx-line shrink-0">
      <button
        @click="router.push('/learn/flashcards')"
        class="flex items-center gap-1.5 px-3.5 py-2 text-sm font-extrabold rounded-2xl bg-lx-surface-soft hover:bg-lx-line transition-colors duration-150 text-lx-ink shrink-0"
      >
        <LmIcon name="back" :size="14" />
        Exit
      </button>
      <div class="flex-1 text-center">
        <span class="font-mono text-[11px] font-bold tracking-[0.06em] uppercase text-lx-ink-faint block">SRS queue</span>
        <span class="font-display font-extrabold text-[18px] text-lx-ink leading-tight block">Review queue</span>
      </div>
      <span class="flex items-center gap-1.5 px-3 py-1.5 text-sm font-extrabold rounded-full bg-lx-fox/10 text-lx-fox-dark shrink-0">
        <LmIcon name="flame" :size="14" :filled="true" />
        {{ streak }} day streak
      </span>
      <span class="px-3 py-1.5 text-sm font-extrabold rounded-full bg-lx-surface-soft text-lx-ink shrink-0">
        {{ Math.min(cardIndex + 1, totalCards) }} / {{ totalCards }}
      </span>
    </div>

    <div v-if="loading" class="flex-1 flex items-center justify-center relative">
      <p class="text-[14px] text-lx-ink-faint">Loading your queue…</p>
    </div>

    <div v-else-if="loadError" class="flex-1 flex items-center justify-center relative">
      <p class="text-[14px] text-lx-ink-faint">Couldn't load your queue. <button class="underline font-bold" @click="loadSession">Try again</button></p>
    </div>

    <div v-else-if="isComplete" class="flex-1 flex flex-col items-center justify-center gap-4 relative px-6 text-center">
      <div class="relative">
        <ConfettiBurst :fire="confettiKey" :count="60" :spread="300" />
        <ToraMascot mood="cheer" :size="190" :track="false" />
      </div>
      <h2 class="font-display text-[30px] font-extrabold text-lx-ink m-0">
        {{ totalCards === 0 ? 'Nothing due right now' : 'Queue cleared!' }}
      </h2>
      <p class="text-[15px] text-lx-ink-soft m-0">
        {{ totalCards === 0 ? 'Check back later as your cards become due.' : `You reviewed ${totalCards} card${totalCards === 1 ? '' : 's'}.` }}
      </p>
      <button
        @click="router.push('/learn/flashcards')"
        class="flex items-center gap-2 mt-2 px-6 py-3 text-[15px] font-extrabold rounded-2xl bg-lx-feather text-white shadow-[0_4px_0_var(--color-lx-feather-dark)] transition-transform duration-75 active:translate-y-1 active:shadow-none"
      >
        Back to flashcards
      </button>
    </div>

    <template v-else-if="currentCard">
      <!-- Progress strip -->
      <div class="shrink-0 px-6 py-2.5 bg-white border-b border-lx-line">
        <div class="h-2.5 bg-lx-surface-soft rounded-full overflow-hidden">
          <div class="h-full bg-lx-fox rounded-full transition-all duration-200" :style="{ width: `${progressPercent}%` }" />
        </div>
      </div>

      <!-- Card pair -->
      <div class="flex-1 flex flex-col px-6 py-6 min-h-0 relative">
        <Transition name="card-swap" mode="out-in">
          <FlashcardFace
            :key="currentCard.srsCardId"
            :card-key="currentCard.srsCardId"
          :front="currentCard.front"
          :formula="currentCard.backText"
          :label="currentCard.deckTitle"
          :note="currentCard.note ?? undefined"
          :example="currentCard.example ?? undefined"
          :tags="currentCard.tags"
          @flip="onFlip"
          />
        </Transition>
      </div>

      <!-- SRS recall bar -->
      <div class="shrink-0 px-6 pb-5 pt-3.5 bg-white border-t border-lx-line">
        <p class="font-mono text-[11px] font-bold tracking-[0.06em] uppercase text-lx-ink-faint text-center mb-2.5">
          {{ isFlipped ? 'How well did you recall?' : 'Click the card to reveal the answer first' }}
        </p>
        <div class="grid grid-cols-4 gap-3">
          <button
            v-for="b in recallButtons"
            :key="b.label"
            @click="submitOutcome(b.outcome)"
            :disabled="!isFlipped || submitting"
            :class="['flex flex-col items-center px-3.5 py-3 rounded-2xl cursor-pointer transition-all duration-150 hover:-translate-y-0.5 disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:translate-y-0', b.bgClass]"
          >
            <span :class="['font-display font-extrabold text-[20px] tracking-tight', b.textClass]">{{ b.label }}</span>
            <span class="text-[13px] font-semibold text-lx-ink-soft mt-0.5">{{ b.sub }}</span>
            <span class="font-mono text-[10px] font-bold text-lx-ink-faint mt-1.5">Recall Time: {{ b.next }}</span>
            <span class="font-mono text-[9px] font-bold text-lx-ink-faint mt-0.5">PRESS [{{ b.key }}]</span>
          </button>
        </div>
      </div>
    </template>
  </main>
</template>

<style scoped>
.card-swap-enter-active { transition: opacity 0.28s ease, transform 0.34s cubic-bezier(0.22, 1, 0.36, 1); }
.card-swap-leave-active { transition: opacity 0.14s ease, transform 0.16s ease-in; }
.card-swap-enter-from { opacity: 0; transform: translateX(48px) rotate(2deg); }
.card-swap-leave-to { opacity: 0; transform: translateX(-48px) rotate(-2deg); }
@media (prefers-reduced-motion: reduce) { .card-swap-enter-active, .card-swap-leave-active { transition: none; } }
</style>
