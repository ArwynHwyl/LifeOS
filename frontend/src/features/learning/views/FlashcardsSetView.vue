<script setup lang="ts">
import { useRouter } from 'vue-router'
import LmIcon from '../components/LmIcon.vue'
import FlashcardFace from '../components/FlashcardFace.vue'

const router = useRouter()
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
        <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3 block">PRACTICE · GEOMETRY</span>
        <span class="font-display font-bold text-[18px] text-lm-ink leading-tight block">Geometry Formulas</span>
      </div>
      <button class="flex items-center gap-1.5 px-3 py-1.5 text-sm font-semibold border-2 border-lm-line rounded-full bg-lm-surface shadow-stamp-sm hover:-translate-y-px transition-all duration-200 text-lm-ink shrink-0">
        <LmIcon name="refresh" :size="14" />
        Restart
      </button>
    </div>

    <!-- Score strip -->
    <div class="relative flex items-center gap-3.5 px-6 py-2.5 bg-lm-surface border-b border-lm-line-soft shrink-0">
      <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3 shrink-0">CARD 7 / 22</span>
      <span class="flex items-center gap-1.5 text-[13px]">
        <span class="text-lm-green"><LmIcon name="check" :size="14" /></span>
        <strong>5</strong> passed
      </span>
      <span class="flex items-center gap-1.5 text-[13px]">
        <span class="text-lm-red"><LmIcon name="close" :size="14" /></span>
        <strong>1</strong> missed
      </span>
      <div class="flex-1" />
      <div class="w-[180px] h-2.5 bg-lm-bg-soft rounded-full overflow-hidden border border-lm-line">
        <div class="h-full bg-lm-green transition-all duration-200" style="width: 27%" />
      </div>
      <span class="font-mono text-[11px] text-lm-ink-2">27%</span>
    </div>

    <!-- Card pair -->
    <div class="flex-1 flex flex-col px-6 py-6 min-h-0 relative">
      <FlashcardFace
        front="Pythagorean theorem"
        formula="a² + b² = c²"
        label="Pythagorean theorem"
        note="Only for right triangles. c is the hypotenuse — the longest side, opposite the right angle."
        example="legs 3, 4  →  c = √(9+16) = 5"
        :tags="['geometry', 'triangle', 'classic']"
      />
    </div>

    <!-- Pass / Not pass -->
    <div class="shrink-0 px-6 pb-5 pt-3.5 bg-lm-surface border-t-2 border-lm-line">
      <p class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3 text-center mb-2.5">DID YOU GET IT?</p>
      <div class="grid grid-cols-2 gap-3.5">
        <!-- Not pass -->
        <button class="flex items-center justify-center gap-4 px-6 py-[18px] bg-lm-red-soft border-2 border-lm-line rounded-[24px] shadow-stamp-md cursor-pointer hover:-translate-y-0.5 hover:shadow-stamp-lg transition-all duration-200">
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
        <button class="flex items-center justify-center gap-4 px-6 py-[18px] bg-lm-green-soft border-2 border-lm-line rounded-[24px] shadow-stamp-md cursor-pointer hover:-translate-y-0.5 hover:shadow-stamp-lg transition-all duration-200">
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
  </main>
</template>
