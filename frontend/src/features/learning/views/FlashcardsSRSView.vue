<script setup lang="ts">
import { useRouter } from 'vue-router'
import LmIcon from '../components/LmIcon.vue'
import FlashcardFace from '../components/FlashcardFace.vue'

const router = useRouter()

const recallButtons = [
  { label: 'Again', sub: 'forgot',  next: '< 1 min', key: '1', bgClass: 'bg-lm-red-soft' },
  { label: 'Hard',  sub: 'barely',  next: '6 min',   key: '2', bgClass: 'bg-lm-yellow-soft' },
  { label: 'Good',  sub: 'solid',   next: '1 day',   key: '3', bgClass: 'bg-lm-green-soft' },
  { label: 'Easy',  sub: 'instant', next: '4 days',  key: '4', bgClass: 'bg-lm-blue-soft' },
]
</script>

<template>
  <main class="flex-1 flex flex-col bg-lm-yellow-soft overflow-hidden relative">
    <div class="absolute inset-0 bg-dot-grid opacity-40 pointer-events-none" />

    <!-- Top bar -->
    <div class="relative flex items-center gap-3.5 px-6 py-3.5 bg-lm-surface border-b-2 border-lm-line shrink-0">
      <button
        @click="router.back()"
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
        7 day streak
      </span>
      <span class="px-3 py-1 text-sm font-semibold border border-lm-line rounded-full bg-lm-bg-soft text-lm-ink shrink-0">3 / 8</span>
    </div>

    <!-- Progress strip -->
    <div class="shrink-0 px-6 py-2.5 bg-lm-surface border-b border-lm-line-soft">
      <div class="h-3.5 bg-lm-bg-soft rounded-full overflow-hidden border border-lm-line">
        <div class="h-full bg-lm-rust transition-all duration-200" style="width: 37%" />
      </div>
    </div>

    <!-- Card pair -->
    <div class="flex-1 flex flex-col px-6 py-6 min-h-0 relative">
      <FlashcardFace
        front="Quadratic formula"
        formula="x = ( −b ± √(b² − 4ac) ) / 2a"
        label="Quadratic formula"
        note="Use for any quadratic ax² + bx + c = 0. The discriminant b² − 4ac tells you the number of real roots."
        example="x² − 5x + 6 = 0  →  x = 2 or x = 3"
        :tags="['algebra', 'quadratic', 'must-know']"
      />
    </div>

    <!-- SRS recall bar -->
    <div class="shrink-0 px-6 pb-5 pt-3.5 bg-lm-surface border-t-2 border-lm-line">
      <p class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3 text-center mb-2.5">HOW WELL DID YOU RECALL?</p>
      <div class="grid grid-cols-4 gap-3">
        <button
          v-for="b in recallButtons"
          :key="b.label"
          :class="['flex flex-col items-center px-3.5 py-3 border-2 border-lm-line rounded-[18px] shadow-stamp-sm cursor-pointer hover:-translate-y-px hover:shadow-stamp-md transition-all duration-200', b.bgClass]"
        >
          <span class="font-display font-bold text-[22px] tracking-tight text-lm-ink">{{ b.label }}</span>
          <span class="text-[13px] text-lm-ink-2 mt-0.5">{{ b.sub }}</span>
          <span class="font-mono text-[10px] text-lm-ink-3 mt-1.5">NEXT: {{ b.next }}</span>
          <span class="font-mono text-[9px] text-lm-ink-3 mt-0.5">PRESS [{{ b.key }}]</span>
        </button>
      </div>
    </div>
  </main>
</template>
