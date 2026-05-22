<script setup lang="ts">
import LmIcon from './LmIcon.vue'
defineEmits<{ close: [] }>()
</script>

<template>
  <div class="absolute inset-0 flex items-center justify-center z-50">
    <div class="absolute inset-0 bg-[rgba(14,13,11,0.55)]" @click="$emit('close')" />

    <!-- Confetti -->
    <svg class="absolute inset-0 w-full h-full pointer-events-none" viewBox="0 0 1280 800" preserveAspectRatio="none">
      <g v-for="i in 45" :key="i"
        :transform="`translate(${(i * 67 + i * i * 11) % 1280} ${(i * 43 + i * i * 7) % 800}) rotate(${(i * 53) % 360})`"
      >
        <rect x="-3" y="-5" width="6" height="10"
          :fill="['#ffd333','#c44a1a','#3b6cb5','#3a7d44','#6b4ec1'][i % 5]"
          stroke="#1a1814" stroke-width="1"/>
      </g>
    </svg>

    <!-- Modal -->
    <div class="relative z-10 w-[min(560px,76%)] bg-lm-yellow border-2 border-lm-line rounded-[24px] shadow-stamp-lg p-8 flex flex-col items-center gap-[18px] text-center overflow-hidden">
      <svg class="absolute top-0 left-0 opacity-15 pointer-events-none" width="120" height="120" viewBox="0 0 80 80">
        <path d="M2 30 Q 15 5, 28 30 T 54 30 T 78 30" stroke="#1a1814" stroke-width="2" fill="none" stroke-linecap="round"/>
      </svg>
      <svg class="absolute bottom-2 right-2 opacity-18 pointer-events-none" width="70" height="70" viewBox="0 0 80 80">
        <template v-for="i in 25" :key="i">
          <circle :cx="10 + ((i-1) % 5) * 15" :cy="10 + Math.floor((i-1) / 5) * 15" r="1.5" fill="#1a1814"/>
        </template>
      </svg>

      <span class="relative font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-2">LEVEL UP!</span>

      <!-- Level transition -->
      <div class="relative flex items-center gap-[22px]">
        <div class="w-[84px] h-[84px] rounded-full bg-lm-surface border-2 border-lm-line flex items-center justify-center font-display font-bold text-[36px] text-lm-ink-3 opacity-55">12</div>
        <span class="font-display text-[38px] text-lm-ink">→</span>
        <div class="relative w-[116px] h-[116px] rounded-full bg-lm-ink border-[3px] border-lm-line shadow-stamp-md flex items-center justify-center font-display font-bold text-[52px] text-lm-bg">
          13
          <span class="absolute -top-2.5 -right-1 text-[22px] text-lm-yellow">✦</span>
          <span class="absolute bottom-0.5 -left-3.5 text-[16px] text-lm-rust">✦</span>
          <span class="absolute top-4 -left-5 text-[13px] text-lm-bg">✧</span>
        </div>
      </div>

      <h2 class="relative font-display text-[30px] font-bold tracking-tight text-lm-ink m-0">You reached Level 13!</h2>

      <!-- XP bar -->
      <div class="relative w-4/5">
        <div class="h-4 bg-lm-surface rounded-full overflow-hidden border border-lm-line">
          <div class="h-full bg-lm-ink" style="width: 100%" />
        </div>
        <div class="flex justify-between mt-1.5">
          <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-2">500 / 500 XP</span>
          <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-2">NEXT: 600 XP</span>
        </div>
      </div>

      <!-- Unlocks -->
      <div class="relative w-[88%] bg-lm-surface border-2 border-dashed border-lm-line rounded-[18px] p-3.5 text-left">
        <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3">UNLOCKED</span>
        <ul class="mt-1.5 ml-[18px] p-0 text-[14px] leading-relaxed text-lm-ink">
          <li><strong>+1 shield</strong> (max is now ×3)</li>
          <li><strong>Calculus 1</strong> course (intermediate)</li>
          <li>New <strong>badge slot</strong></li>
        </ul>
      </div>

      <button @click="$emit('close')" class="relative flex items-center gap-2 px-6 py-3 text-[17px] font-semibold border-2 border-lm-line rounded-full bg-lm-ink text-lm-bg shadow-stamp-sm hover:-translate-y-px hover:shadow-stamp-md transition-all duration-200 mt-1">
        Continue
        <LmIcon name="arrow" :size="18" />
      </button>
    </div>
  </div>
</template>
