<script setup lang="ts">
import LmIcon from '../../learning/components/LmIcon.vue'
import ToraMascot from '@/components/tora/ToraMascot.vue'

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
    <div class="anim-pop relative z-10 w-[min(560px,76%)] bg-white rounded-[28px] shadow-[0_32px_72px_-24px_rgba(0,0,0,0.4)] p-8 flex flex-col items-center gap-4 text-center">
      <div class="-mt-24 -mb-2"><ToraMascot mood="oops" :size="150" :track="false" /></div>

      <h2 class="font-display text-[26px] font-extrabold tracking-tight text-lx-ink m-0">Your {{ streakDaysLost }}-day streak ended.</h2>
      <p class="text-[14px] font-semibold text-lx-ink-soft max-w-[420px] m-0">It happens. You missed a study day and your shields were already used up.</p>

      <!-- Comeback nudge -->
      <div class="w-full bg-lx-fox/10 rounded-2xl p-3.5 text-[14px] font-semibold text-lx-ink text-left">
        <strong>Start a new streak today.</strong> Your longest was
        <span class="bg-lx-fox text-white px-1.5 py-0.5 rounded-md font-bold">{{ longestStreak }} days</span>.
        You can beat it.
      </div>

      <div class="flex gap-3 w-full">
        <button @click="$emit('close')" class="flex-1 px-[18px] py-2.5 text-[14px] font-extrabold rounded-2xl bg-lx-surface-soft hover:bg-lx-line transition-colors duration-150 text-lx-ink">
          Maybe later
        </button>
        <button @click="$emit('close')" class="flex items-center justify-center gap-2 px-[18px] py-2.5 text-[14px] font-extrabold rounded-2xl bg-lx-fox text-white shadow-[0_4px_0_var(--color-lx-fox-dark)] transition-transform duration-75 active:translate-y-1 active:shadow-none" style="flex: 1.5">
          Do a lesson now
          <LmIcon name="arrow" :size="16" />
        </button>
      </div>
    </div>
  </div>
</template>
