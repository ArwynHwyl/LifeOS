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
    <div class="moment-backdrop absolute inset-0 bg-[rgba(14,13,11,0.62)]" @click="$emit('close')" />

    <!-- Modal -->
    <div class="streak-card relative z-10 w-[min(560px,76%)] bg-lm-surface border-2 border-lm-line rounded-[24px] shadow-stamp-lg p-8 flex flex-col items-center gap-4 text-center">
      <span class="streak-rise font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-rust">STREAK ENDED</span>

      <!-- Flame burns, flickers, then goes out -->
      <div class="relative">
        <div class="w-[110px] h-[110px] rounded-full bg-lm-bg-soft border-[3px] border-lm-line shadow-stamp-md flex items-center justify-center text-lm-ink-3">
          <span class="streak-flame flex text-lm-rust">
            <LmIcon name="flame" :size="56" :filled="true" />
          </span>
        </div>
        <span v-for="i in 3" :key="i" :class="['streak-smoke', `streak-smoke--${i}`]" />
        <svg class="absolute inset-0" width="110" height="110" viewBox="0 0 110 110">
          <line class="streak-slash" x1="14" y1="14" x2="96" y2="96" stroke="#d64545" stroke-width="5" stroke-linecap="round" pathLength="1"/>
        </svg>
      </div>

      <h2 class="streak-rise streak-rise--2 font-display text-[28px] font-bold tracking-tight text-lm-ink m-0">Your {{ streakDaysLost }}-day streak ended.</h2>
      <p class="streak-rise streak-rise--2 text-[14px] text-lm-ink-2 max-w-[420px] m-0">It happens. You missed a study day and your shields were already used up.</p>

      <!-- Comeback nudge -->
      <div class="streak-rise streak-rise--3 w-full bg-lm-yellow-soft border-2 border-lm-line rounded-[12px] p-3.5 text-[14px] text-lm-ink text-left">
        <strong>Start a new streak today.</strong> Your longest was
        <span class="bg-lm-yellow px-1.5 rounded border border-lm-line font-bold">{{ longestStreak }} days</span>.
        You can beat it.
      </div>

      <div class="streak-rise streak-rise--4 flex gap-3 w-full">
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

<style scoped>
.moment-backdrop { animation: backdrop-in .3s ease both; }
.streak-card { animation: card-sink .55s cubic-bezier(.2, 1.2, .4, 1) both; }
.streak-flame { transform-origin: 50% 90%; animation: flame-out 1.2s .3s ease-in forwards; }
.streak-slash { stroke-dasharray: 1; stroke-dashoffset: 1; animation: slash-draw .3s 1.3s ease-out forwards; }
.streak-smoke {
  position: absolute;
  left: 50%;
  top: 18px;
  width: 14px;
  height: 14px;
  margin-left: -7px;
  border-radius: 50%;
  background: rgba(26, 24, 20, .18);
  opacity: 0;
  pointer-events: none;
  animation: smoke-rise 1.6s 1.2s ease-out forwards;
}
.streak-smoke--2 { margin-left: -1px; animation-delay: 1.4s; }
.streak-smoke--3 { margin-left: -13px; animation-delay: 1.6s; }
.streak-rise { animation: rise-in .4s .2s ease-out both; }
.streak-rise--2 { animation-delay: 1.4s; }
.streak-rise--3 { animation-delay: 1.55s; }
.streak-rise--4 { animation-delay: 1.7s; }

@keyframes backdrop-in { from { opacity: 0; } to { opacity: 1; } }
@keyframes card-sink { from { opacity: 0; transform: translateY(-24px) scale(.95); } to { opacity: 1; transform: none; } }
@keyframes flame-out {
  0% { transform: scale(1); }
  15% { transform: scale(1.1, .92) rotate(-4deg); }
  30% { transform: scale(.94, 1.06) rotate(3deg); }
  45% { transform: scale(1.04, .96) rotate(-2deg); }
  100% { transform: scale(.82); filter: grayscale(1); opacity: .45; }
}
@keyframes slash-draw { to { stroke-dashoffset: 0; } }
@keyframes smoke-rise {
  0% { opacity: 0; transform: translateY(0) scale(.6); }
  25% { opacity: 1; }
  100% { opacity: 0; transform: translateY(-46px) scale(1.8); }
}
@keyframes rise-in { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: none; } }

@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after { animation: none !important; }
  .streak-flame { filter: grayscale(1); opacity: .45; }
  .streak-slash { stroke-dashoffset: 0; }
  .streak-smoke { display: none; }
}
</style>
