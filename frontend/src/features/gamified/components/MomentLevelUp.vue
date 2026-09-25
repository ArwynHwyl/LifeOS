<script setup lang="ts">
import { computed } from 'vue'
import LmIcon from '../../learning/components/LmIcon.vue'

const props = withDefaults(defineProps<{
  fromLevel: number
  toLevel: number
  rankName?: string
  currentExp?: number
  expRequiredForNextLevel?: number
  shieldMax?: number
}>(), {
  rankName: '',
  currentExp: 0,
  expRequiredForNextLevel: 0,
  shieldMax: 0,
})

defineEmits<{ close: [] }>()

const xpPercent = computed(() => {
  if (props.expRequiredForNextLevel <= 0) return 100
  return Math.min(100, Math.round((props.currentExp / props.expRequiredForNextLevel) * 100))
})
</script>

<template>
  <div class="absolute inset-0 flex items-center justify-center z-50">
    <div class="moment-backdrop absolute inset-0 bg-[rgba(14,13,11,0.55)]" @click="$emit('close')" />

    <!-- Confetti -->
    <svg class="absolute inset-0 w-full h-full pointer-events-none" viewBox="0 0 1280 800" preserveAspectRatio="none">
      <g v-for="i in 45" :key="i"
        :transform="`translate(${(i * 67 + i * i * 11) % 1280} ${(i * 43 + i * i * 7) % 800}) rotate(${(i * 53) % 360})`"
      >
        <g class="confetti-piece" :style="{ animationDelay: `${(i % 9) * 70}ms`, animationDuration: `${1400 + (i % 5) * 180}ms` }">
          <rect x="-3" y="-5" width="6" height="10"
            :fill="['#ffd333','#c44a1a','#3b6cb5','#3a7d44','#6b4ec1'][i % 5]"
            stroke="#1a1814" stroke-width="1"/>
        </g>
      </g>
    </svg>

    <!-- Modal -->
    <div class="levelup-card relative z-10 w-[min(560px,76%)] bg-lm-yellow border-2 border-lm-line rounded-[24px] shadow-stamp-lg p-8 flex flex-col items-center gap-[18px] text-center overflow-hidden">
      <svg class="absolute top-0 left-0 opacity-15 pointer-events-none" width="120" height="120" viewBox="0 0 80 80">
        <path d="M2 30 Q 15 5, 28 30 T 54 30 T 78 30" stroke="#1a1814" stroke-width="2" fill="none" stroke-linecap="round"/>
      </svg>
      <svg class="absolute bottom-2 right-2 opacity-18 pointer-events-none" width="70" height="70" viewBox="0 0 80 80">
        <template v-for="i in 25" :key="i">
          <circle :cx="10 + ((i-1) % 5) * 15" :cy="10 + Math.floor((i-1) / 5) * 15" r="1.5" fill="#1a1814"/>
        </template>
      </svg>

      <span class="levelup-eyebrow relative font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-2">LEVEL UP!</span>

      <!-- Level transition -->
      <div class="relative flex items-center gap-[22px]">
        <div class="levelup-from w-[84px] h-[84px] rounded-full bg-lm-surface border-2 border-lm-line flex items-center justify-center font-display font-bold text-[36px] text-lm-ink-3 opacity-55">{{ fromLevel }}</div>
        <span class="levelup-arrow font-display text-[38px] text-lm-ink">→</span>
        <div class="levelup-to relative w-[116px] h-[116px] rounded-full bg-lm-ink border-[3px] border-lm-line shadow-stamp-md flex items-center justify-center font-display font-bold text-[52px] text-lm-bg">
          {{ toLevel }}
          <span class="levelup-ring absolute -inset-[3px] rounded-full border-[3px] border-lm-ink pointer-events-none" />
          <span class="levelup-spark absolute -top-2.5 -right-1 text-[22px] text-lm-yellow">✦</span>
          <span class="levelup-spark levelup-spark--2 absolute bottom-0.5 -left-3.5 text-[16px] text-lm-rust">✦</span>
          <span class="levelup-spark levelup-spark--3 absolute top-4 -left-5 text-[13px] text-lm-bg">✧</span>
        </div>
      </div>

      <h2 class="levelup-rise relative font-display text-[30px] font-bold tracking-tight text-lm-ink m-0">You reached Level {{ toLevel }}!</h2>

      <!-- XP bar -->
      <div class="levelup-rise levelup-rise--2 relative w-4/5">
        <div class="h-4 bg-lm-surface rounded-full overflow-hidden border border-lm-line">
          <div class="levelup-xp h-full bg-lm-ink" :style="{ width: xpPercent + '%' }" />
        </div>
        <div class="flex justify-between mt-1.5">
          <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-2">{{ currentExp }} / {{ expRequiredForNextLevel }} XP</span>
          <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-2">NEXT LEVEL: {{ toLevel + 1 }}</span>
        </div>
      </div>

      <!-- Unlocks -->
      <div class="levelup-rise levelup-rise--3 relative w-[88%] bg-lm-surface border-2 border-dashed border-lm-line rounded-[18px] p-3.5 text-left">
        <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3">UNLOCKED</span>
        <ul class="mt-1.5 ml-[18px] p-0 text-[14px] leading-relaxed text-lm-ink">
          <li><strong>Rank:</strong> {{ rankName }}</li>
          <li><strong>Shield capacity</strong> ×{{ shieldMax }}</li>
        </ul>
      </div>

      <button @click="$emit('close')" class="levelup-rise levelup-rise--4 relative flex items-center gap-2 px-6 py-3 text-[17px] font-semibold border-2 border-lm-line rounded-full bg-lm-ink text-lm-bg shadow-stamp-sm hover:-translate-y-px hover:shadow-stamp-md transition-all duration-200 mt-1">
        Continue
        <LmIcon name="arrow" :size="18" />
      </button>
    </div>
  </div>
</template>

<style scoped>
.moment-backdrop { animation: backdrop-in .25s ease both; }
.levelup-card { animation: card-pop .55s cubic-bezier(.2, 1.4, .4, 1) both; }
.levelup-eyebrow { animation: rise-in .35s .2s ease-out both; }
.levelup-from { animation: from-fade .5s .3s ease-out both; }
.levelup-arrow { animation: arrow-slide .4s .45s ease-out both; }
.levelup-to { animation: badge-pop .7s .55s cubic-bezier(.2, 1.6, .4, 1) both; }
.levelup-ring { animation: ring-pulse 1.1s .9s ease-out both; }
.levelup-spark { animation: sparkle .7s .95s both, twinkle 2.4s 1.7s ease-in-out infinite; }
.levelup-spark--2 { animation-delay: 1.1s, 1.9s; }
.levelup-spark--3 { animation-delay: 1.25s, 2.1s; }
.levelup-rise { animation: rise-in .4s .8s ease-out both; }
.levelup-rise--2 { animation-delay: .9s; }
.levelup-rise--3 { animation-delay: 1s; }
.levelup-rise--4 { animation-delay: 1.1s; }
.levelup-xp { animation: xp-fill .9s 1.1s cubic-bezier(.3, .8, .3, 1) both; }
.confetti-piece {
  transform-box: fill-box;
  transform-origin: center;
  animation-name: confetti-fall;
  animation-timing-function: cubic-bezier(.2, .7, .4, 1);
  animation-fill-mode: both;
}

@keyframes backdrop-in { from { opacity: 0; } to { opacity: 1; } }
@keyframes card-pop { from { opacity: 0; transform: translateY(28px) scale(.9); } to { opacity: 1; transform: none; } }
@keyframes rise-in { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: none; } }
@keyframes from-fade { from { opacity: 0; transform: scale(1.1); } to { opacity: .55; transform: none; } }
@keyframes arrow-slide { from { opacity: 0; transform: translateX(-14px); } to { opacity: 1; transform: none; } }
@keyframes badge-pop { from { opacity: 0; transform: scale(.3) rotate(-25deg); } to { opacity: 1; transform: none; } }
@keyframes ring-pulse { from { opacity: .7; transform: scale(1); } to { opacity: 0; transform: scale(1.7); } }
@keyframes sparkle { 0% { opacity: 0; transform: scale(0) rotate(-40deg); } 60% { opacity: 1; transform: scale(1.4); } 100% { opacity: 1; transform: scale(1); } }
@keyframes twinkle { 0%, 100% { transform: scale(1); } 50% { transform: scale(.7) rotate(20deg); } }
@keyframes xp-fill { from { width: 0; } }
@keyframes confetti-fall { 0% { opacity: 0; transform: translateY(-160px) rotate(-90deg); } 15% { opacity: 1; } 100% { opacity: 1; transform: none; } }

@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after { animation: none !important; }
}
</style>
