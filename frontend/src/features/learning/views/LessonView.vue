<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import LmIcon from '../components/LmIcon.vue'

const router = useRouter()
const selected = ref<number | null>(1)

const options = [
  { label: 'x = 3' },
  { label: 'x = 4' },
  { label: 'x = 6' },
  { label: 'x = 9' },
]
</script>

<template>
  <main class="flex-1 flex flex-col overflow-hidden bg-lm-bg">

    <!-- Quiz progress bar -->
    <div class="flex items-center gap-4 px-6 py-3 bg-lm-surface border-b-2 border-lm-line shrink-0">
      <button
        @click="router.back()"
        class="flex items-center gap-1.5 px-3 py-1.5 text-sm font-semibold border-2 border-lm-line rounded-full bg-lm-surface shadow-stamp-sm hover:-translate-y-px transition-all duration-200 text-lm-ink shrink-0"
      >
        <LmIcon name="close" :size="14" />
        Exit
      </button>
      <div class="flex-1 h-4 bg-lm-bg-soft rounded-full overflow-hidden border border-lm-line">
        <div class="h-full bg-lm-yellow transition-all duration-200" style="width: 40%" />
      </div>
      <span class="flex items-center gap-1.5 px-3 py-1 text-sm font-semibold border border-lm-line rounded-full bg-lm-yellow-soft shrink-0">
        <LmIcon name="bolt" :size="14" :filled="true" class="text-lm-rust" />
        +15 XP
      </span>
      <span class="flex items-center gap-1.5 px-3 py-1 text-sm font-semibold border border-lm-line rounded-full bg-lm-rust-soft shrink-0">
        <span class="text-lm-rust"><LmIcon name="flame" :size="14" :filled="true" /></span>
        7
      </span>
    </div>

    <!-- Breadcrumb strip -->
    <div class="flex items-center gap-2.5 px-6 py-2 bg-lm-yellow-soft border-b border-lm-line-soft text-[13px] shrink-0">
      <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3">ALGEBRA BASICS</span>
      <span class="text-lm-ink-3">›</span>
      <span class="font-semibold text-lm-ink">Topic 5 · Linear equations</span>
      <div class="flex-1" />
      <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3">QUESTION 2 / 5</span>
    </div>

    <!-- Scrollable content -->
    <div class="flex-1 overflow-auto relative">
      <div class="absolute inset-0 bg-dot-grid opacity-50 pointer-events-none" />
      <div class="relative max-w-[840px] mx-auto px-6 py-9 flex flex-col gap-7">

        <!-- Prompt -->
        <div>
          <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3">SOLVE FOR <em class="font-math italic not-italic">x</em></span>
          <h2 class="font-display text-[32px] font-bold tracking-tight text-lm-ink leading-tight mt-1.5 m-0">
            What value of <em class="font-math italic text-[32px]">x</em> makes this equation true?
          </h2>
        </div>

        <!-- Chalkboard equation -->
        <div class="relative bg-lm-ink rounded-[18px] border-2 border-lm-line shadow-stamp-md overflow-hidden px-6 py-10 text-center">
          <div class="absolute inset-0 bg-chalk-dots pointer-events-none" />
          <span class="relative font-math italic font-semibold text-[58px] text-lm-bg leading-none">2x + 5 = 13</span>
          <svg class="absolute bottom-2 right-3 opacity-15 pointer-events-none" width="60" height="60" viewBox="0 0 80 80">
            <path d="M2 30 Q 15 5, 28 30 T 54 30 T 78 30" stroke="#fbf7ef" stroke-width="2" fill="none" stroke-linecap="round"/>
          </svg>
        </div>

        <!-- Answer options (2×2 grid) -->
        <div class="grid grid-cols-2 gap-3.5">
          <button
            v-for="(opt, i) in options"
            :key="i"
            @click="selected = i"
            :class="[
              'flex items-center gap-3.5 px-[22px] py-[18px] border-2 border-lm-line rounded-[18px] transition-all duration-200 cursor-pointer',
              selected === i
                ? 'bg-lm-yellow shadow-stamp-md -translate-x-px -translate-y-px'
                : 'bg-lm-surface shadow-stamp-sm hover:bg-lm-bg-soft hover:shadow-stamp-md'
            ]"
          >
            <div :class="[
              'w-9 h-9 rounded-full border-2 border-lm-line flex items-center justify-center font-display font-bold text-[15px] shrink-0 transition-colors duration-200',
              selected === i ? 'bg-lm-ink text-lm-bg' : 'bg-lm-bg-soft text-lm-ink'
            ]">
              {{ String.fromCharCode(65 + i) }}
            </div>
            <em class="font-math italic font-semibold text-[22px] text-lm-ink not-italic">{{ opt.label }}</em>
          </button>
        </div>

        <!-- Hint callout -->
        <div class="flex items-center gap-3 p-4 bg-lm-surface border-2 border-lm-line rounded-[12px] shadow-stamp-sm -rotate-[0.4deg] self-start max-w-[460px]">
          <div class="w-7 h-7 rounded-full bg-lm-yellow border-2 border-lm-line flex items-center justify-center font-bold text-lm-ink shrink-0">?</div>
          <p class="text-[13.5px] text-lm-ink m-0">
            <strong>Hint:</strong> Subtract 5 from both sides first, then divide by 2.
          </p>
        </div>

      </div>
    </div>

    <!-- Footer action bar -->
    <div class="flex items-center gap-2.5 px-6 py-3.5 bg-lm-yellow-soft border-t-2 border-lm-line shrink-0">
      <button class="px-[18px] py-[9px] text-[15px] font-semibold border-2 border-lm-line rounded-full bg-lm-surface shadow-stamp-sm hover:-translate-y-px hover:shadow-stamp-md transition-all duration-200 text-lm-ink">
        Show hint
      </button>
      <button class="px-[18px] py-[9px] text-[15px] font-semibold border-2 border-lm-line rounded-full bg-lm-surface shadow-stamp-sm hover:-translate-y-px hover:shadow-stamp-md transition-all duration-200 text-lm-ink">
        Show steps
      </button>
      <div class="flex-1" />
      <button class="flex items-center gap-2 px-[18px] py-[9px] text-[15px] font-semibold border-2 border-lm-line rounded-full bg-lm-ink text-lm-bg shadow-stamp-sm hover:-translate-y-px hover:shadow-stamp-md transition-all duration-200">
        Check
        <LmIcon name="arrow" :size="16" />
      </button>
    </div>
  </main>
</template>
