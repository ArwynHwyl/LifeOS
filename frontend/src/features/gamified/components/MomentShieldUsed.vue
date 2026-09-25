<script setup lang="ts">
import LmIcon from '../../learning/components/LmIcon.vue'

withDefaults(defineProps<{
  streakDays?: number
  shieldsRemaining?: number
  shieldMax?: number
}>(), {
  streakDays: 0,
  shieldsRemaining: 0,
  shieldMax: 0,
})

defineEmits<{ close: [] }>()
</script>

<template>
  <div class="absolute inset-0 flex flex-col items-center justify-center z-50" @click.self="$emit('close')">
    <div class="moment-backdrop absolute inset-0 bg-[rgba(14,13,11,0.55)]" @click="$emit('close')" />

    <!-- Notification card -->
    <div class="shield-card relative z-10 w-[min(620px,80%)] bg-lm-surface border-2 border-lm-line rounded-[24px] shadow-stamp-lg px-6 py-5 flex items-center gap-[18px]">
      <div class="shield-hero relative w-16 h-16 rounded-full bg-lm-blue-soft border-2 border-lm-line text-lm-blue shadow-stamp-sm flex items-center justify-center shrink-0">
        <span class="shield-ripple absolute inset-0 rounded-full border-2 border-lm-blue pointer-events-none" />
        <span class="shield-ripple shield-ripple--late absolute inset-0 rounded-full border-2 border-lm-blue pointer-events-none" />
        <LmIcon name="shield" :size="36" :filled="true" />
      </div>
      <div class="shield-copy flex-1">
        <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-blue block">SHIELD ACTIVATED</span>
        <h2 class="font-display text-[26px] font-bold tracking-tight text-lm-ink mt-0.5 mb-1 m-0">Streak protected!</h2>
        <p class="text-[14px] text-lm-ink-2 m-0">You missed yesterday — a shield kept your <strong>{{ streakDays }}-day streak</strong> alive.</p>
      </div>
      <div class="shield-copy shield-copy--late text-right shrink-0">
        <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3 block mb-1">SHIELDS LEFT</span>
        <div class="flex gap-1 justify-end">
          <div
            v-for="i in shieldMax"
            :key="i"
            :class="[
              'relative w-7 h-8 rounded-[4px] border-2 flex items-center justify-center',
              i <= shieldsRemaining
                ? 'bg-lm-blue-soft border-lm-line border-solid text-lm-blue'
                : 'bg-lm-bg-soft border-dashed border-lm-line text-lm-ink-3',
              { 'shield-slot--broken': i === shieldsRemaining + 1 },
            ]"
          >
            <LmIcon name="shield" :size="18" :filled="i <= shieldsRemaining" />
            <!-- Crack over the shield that was just spent -->
            <svg
              v-if="i === shieldsRemaining + 1"
              class="absolute inset-0 w-full h-full pointer-events-none"
              viewBox="0 0 28 32"
            >
              <path
                class="shield-crack"
                d="M15 3 L11 12 L17 16 L12 24 L15 30"
                fill="none"
                stroke="#d64545"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
                pathLength="1"
              />
            </svg>
          </div>
        </div>
      </div>
    </div>

    <button
      @click="$emit('close')"
      class="shield-copy shield-copy--button relative z-10 mt-5 px-[18px] py-[9px] text-[15px] font-semibold border-2 border-lm-line rounded-full bg-lm-ink text-lm-bg shadow-stamp-sm hover:-translate-y-px hover:shadow-stamp-md transition-all duration-200"
    >
      Got it
    </button>
  </div>
</template>

<style scoped>
.moment-backdrop { animation: backdrop-in .25s ease both; }
.shield-card { animation: card-drop .5s cubic-bezier(.2, 1.3, .4, 1) both; }
.shield-hero { animation: shield-block .6s .35s cubic-bezier(.3, 1.6, .5, 1) both; }
.shield-ripple { opacity: 0; animation: ripple .9s .5s ease-out both; }
.shield-ripple--late { animation-delay: .7s; }
.shield-copy { animation: rise-in .4s .45s ease-out both; }
.shield-copy--late { animation-delay: .6s; }
.shield-copy--button { animation-delay: 1.3s; }
.shield-slot--broken { animation: slot-break .6s .95s ease-in-out both; }
.shield-crack { stroke-dasharray: 1; stroke-dashoffset: 1; animation: crack-draw .35s 1s ease-out forwards; }

@keyframes backdrop-in { from { opacity: 0; } to { opacity: 1; } }
@keyframes card-drop { from { opacity: 0; transform: translateY(-36px) scale(.96); } to { opacity: 1; transform: none; } }
@keyframes shield-block {
  0% { opacity: 0; transform: scale(.4); }
  55% { opacity: 1; transform: scale(1.15) rotate(-6deg); }
  75% { transform: scale(.95) rotate(3deg); }
  100% { opacity: 1; transform: none; }
}
@keyframes ripple { from { opacity: .8; transform: scale(1); } to { opacity: 0; transform: scale(1.9); } }
@keyframes rise-in { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: none; } }
@keyframes slot-break {
  0%, 100% { transform: none; }
  20% { transform: translateX(-3px) rotate(-6deg); }
  40% { transform: translateX(3px) rotate(5deg); }
  60% { transform: translateX(-2px) rotate(-3deg) scale(.94); }
  80% { transform: translateX(1px); }
}
@keyframes crack-draw { to { stroke-dashoffset: 0; } }

@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after { animation: none !important; }
  .shield-crack { stroke-dashoffset: 0; }
  .shield-ripple { display: none; }
}
</style>
