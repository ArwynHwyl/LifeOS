<script setup lang="ts">
import LmIcon from './LmIcon.vue'
defineEmits<{ close: [] }>()
</script>

<template>
  <div class="absolute inset-0 flex items-center justify-center z-50">
    <!-- Backdrop -->
    <div class="absolute inset-0 bg-[rgba(14,13,11,0.45)]" @click="$emit('close')" />

    <!-- Confetti -->
    <svg class="absolute inset-0 w-full h-full pointer-events-none" viewBox="0 0 1280 800" preserveAspectRatio="none">
      <g v-for="i in 50" :key="i"
        :transform="`translate(${(i * 53 + i * i * 7) % 1280} ${(i * 31 + i * i * 11) % 800}) rotate(${(i * 47) % 360})`"
      >
        <rect v-if="i % 3 === 0" x="-3" y="-3" width="6" height="6"
          :fill="['#ffd333','#c44a1a','#3b6cb5','#3a7d44','#6b4ec1'][i % 5]"
          stroke="#1a1814" stroke-width="1"/>
        <rect v-else-if="i % 3 === 1" x="-2" y="-6" width="4" height="12"
          :fill="['#ffd333','#c44a1a','#3b6cb5','#3a7d44','#6b4ec1'][i % 5]"
          stroke="#1a1814" stroke-width="1"/>
        <circle v-else cx="0" cy="0" r="4"
          :fill="['#ffd333','#c44a1a','#3b6cb5','#3a7d44','#6b4ec1'][i % 5]"
          stroke="#1a1814" stroke-width="1"/>
      </g>
    </svg>

    <!-- Modal -->
    <div class="relative z-10 w-[min(620px,80%)] bg-lm-surface border-2 border-lm-line rounded-[24px] shadow-stamp-lg p-9 flex flex-col items-center gap-[18px] text-center">
      <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3">COURSE COMPLETE</span>

      <h1 class="font-display text-[40px] font-bold tracking-tight text-lm-ink leading-tight m-0">
        You finished
        <span class="inline-block bg-lm-yellow px-2.5 rounded-[8px] border-2 border-lm-line shadow-stamp-sm rotate-[-1.5deg] whitespace-nowrap">
          Algebra Basics!
        </span>
      </h1>

      <!-- Trophy -->
      <div class="relative w-[120px] h-[120px] rounded-full bg-lm-yellow border-[3px] border-lm-line shadow-stamp-md flex items-center justify-center">
        <span class="font-math italic font-bold text-[56px] text-lm-ink">x²</span>
        <span class="absolute -top-1.5 -right-1 text-[22px] text-lm-rust">✦</span>
        <span class="absolute bottom-1 -left-2.5 text-[16px] text-lm-blue">✦</span>
        <span class="absolute top-[22px] -left-4 text-[13px] text-lm-ink">✧</span>
      </div>

      <!-- Stats -->
      <div class="grid grid-cols-3 gap-3 w-full">
        <div v-for="[v, l, c] in [['24 / 24','TOPICS','bg-lm-green-soft'], ['+480 XP','EARNED','bg-lm-yellow-soft'], ['94%','ACCURACY','bg-lm-blue-soft']]"
          :key="l"
          :class="['border-2 border-lm-line rounded-[12px] px-2 py-3 shadow-stamp-sm text-center', c]"
        >
          <p class="font-display text-[22px] font-bold leading-none text-lm-ink m-0">{{ v }}</p>
          <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3 mt-1 block">{{ l }}</span>
        </div>
      </div>

      <!-- Badge unlock -->
      <div class="w-full flex items-center gap-3.5 p-3.5 bg-lm-yellow-soft border-2 border-dashed border-lm-line rounded-[18px]">
        <div class="w-[52px] h-[52px] rounded-full bg-lm-yellow border-2 border-lm-line shadow-stamp-sm flex items-center justify-center font-math italic font-bold text-[22px] text-lm-ink shrink-0">=</div>
        <div class="flex-1 text-left">
          <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3 block">NEW BADGE UNLOCKED</span>
          <p class="font-display font-bold text-[18px] leading-tight text-lm-ink m-0">Equation Slayer</p>
        </div>
        <span class="flex items-center gap-1.5 px-3 py-1 text-sm font-semibold border border-lm-line rounded-full bg-lm-rust-soft text-lm-ink shrink-0">
          <LmIcon name="bolt" :size="12" :filled="true" />
          +50 XP bonus
        </span>
      </div>

      <div class="flex gap-3 w-full mt-1">
        <button @click="$emit('close')" class="flex-1 px-[18px] py-[9px] text-[15px] font-semibold border-2 border-lm-line rounded-full bg-lm-surface shadow-stamp-sm hover:-translate-y-px hover:shadow-stamp-md transition-all duration-200 text-lm-ink">
          Review course
        </button>
        <button @click="$emit('close')" class="flex items-center justify-center gap-2 px-[18px] py-[9px] text-[15px] font-semibold border-2 border-lm-line rounded-full bg-lm-ink text-lm-bg shadow-stamp-sm hover:-translate-y-px hover:shadow-stamp-md transition-all duration-200" style="flex: 1.6">
          Next course
          <LmIcon name="arrow" :size="16" />
        </button>
      </div>
    </div>
  </div>
</template>
