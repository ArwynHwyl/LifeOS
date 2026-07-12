<script setup lang="ts">
import LmIcon from '../../learning/components/LmIcon.vue'

withDefaults(defineProps<{
  streakDaysLost?: number
  longestStreak?: number
}>(), {
  streakDaysLost: 0,
  longestStreak: 0,
})

defineEmits<{ close: [] }>()
</script>

<template>
  <div class="absolute inset-0 flex items-center justify-center z-50">
    <div class="absolute inset-0 bg-[rgba(14,13,11,0.62)]" @click="$emit('close')" />

    <!-- Modal -->
    <div class="relative z-10 w-[min(560px,76%)] bg-lm-surface border-2 border-lm-line rounded-[24px] shadow-stamp-lg p-8 flex flex-col items-center gap-4 text-center">
      <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-rust">STREAK ENDED</span>

      <!-- Extinguished flame -->
      <div class="relative">
        <div class="w-[110px] h-[110px] rounded-full bg-lm-bg-soft border-[3px] border-lm-line shadow-stamp-md flex items-center justify-center text-lm-ink-3">
          <LmIcon name="flame" :size="56" :filled="true" />
        </div>
        <svg class="absolute inset-0" width="110" height="110" viewBox="0 0 110 110">
          <line x1="14" y1="14" x2="96" y2="96" stroke="#d64545" stroke-width="5" stroke-linecap="round"/>
        </svg>
      </div>

      <h2 class="font-display text-[28px] font-bold tracking-tight text-lm-ink m-0">Your {{ streakDaysLost }}-day streak ended.</h2>
      <p class="text-[14px] text-lm-ink-2 max-w-[420px] m-0">It happens. You missed a study day and your shields were already used up.</p>

      <!-- Comeback nudge -->
      <div class="w-full bg-lm-yellow-soft border-2 border-lm-line rounded-[12px] p-3.5 text-[14px] text-lm-ink text-left">
        <strong>Start a new streak today.</strong> Your longest was
        <span class="bg-lm-yellow px-1.5 rounded border border-lm-line font-bold">{{ longestStreak }} days</span>.
        You can beat it.
      </div>

      <div class="flex gap-3 w-full">
        <button @click="$emit('close')" class="flex-1 px-[18px] py-[9px] text-[15px] font-semibold border-2 border-lm-line rounded-full bg-lm-surface shadow-stamp-sm hover:-translate-y-px hover:shadow-stamp-md transition-all duration-200 text-lm-ink">
          Maybe later
        </button>
        <button @click="$emit('close')" class="flex items-center justify-center gap-2 px-[18px] py-[9px] text-[15px] font-semibold border-2 border-lm-line rounded-full bg-lm-ink text-lm-bg shadow-stamp-sm hover:-translate-y-px hover:shadow-stamp-md transition-all duration-200" style="flex: 1.5">
          Do a lesson now
          <LmIcon name="arrow" :size="16" />
        </button>
      </div>
    </div>
  </div>
</template>
