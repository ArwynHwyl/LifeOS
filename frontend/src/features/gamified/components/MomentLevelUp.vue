<script setup lang="ts">
import { computed } from 'vue'
import LmIcon from '../../learning/components/LmIcon.vue'
import ToraMascot from '@/components/tora/ToraMascot.vue'
import ConfettiBurst from '@/components/motion/ConfettiBurst.vue'
import CountUp from '@/components/motion/CountUp.vue'

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
    <div class="absolute inset-0 bg-[rgba(14,13,11,0.55)]" @click="$emit('close')" />

    <ConfettiBurst :fire="1" mode="rain" :count="90" />

    <!-- Modal -->
    <div class="anim-pop relative z-10 w-[min(560px,76%)] bg-lx-feather rounded-[28px] shadow-[0_32px_72px_-24px_rgba(0,0,0,0.45)] p-8 flex flex-col items-center gap-[18px] text-center ">

      <div class="relative -mt-32 -mb-4"><ToraMascot mood="cheer" :size="170" :track="false" /></div>
      <span class="relative font-mono text-[11px] font-bold tracking-[0.08em] uppercase text-white/80">Level up!</span>

      <!-- Level transition -->
      <div class="relative flex items-center gap-5">
        <div class="w-[80px] h-[80px] rounded-full bg-white/15 flex items-center justify-center font-display font-extrabold text-[32px] text-white/60">{{ fromLevel }}</div>
        <span class="font-display text-[32px] text-white/70">→</span>
        <div class="relative w-[112px] h-[112px] rounded-full bg-white flex items-center justify-center font-display font-extrabold text-[48px] text-lx-feather-dark">
          <CountUp :value="toLevel" :duration="1000" />
          <span class="absolute -top-2.5 -right-1 text-[22px] text-lx-fox">✦</span>
          <span class="absolute bottom-0.5 -left-3.5 text-[16px] text-lx-macaw">✦</span>
        </div>
      </div>

      <h2 class="relative font-display text-[28px] font-extrabold tracking-tight text-white m-0">You reached Level {{ toLevel }}!</h2>

      <!-- XP bar -->
      <div class="relative w-4/5">
        <div class="h-3.5 bg-white/25 rounded-full overflow-hidden">
          <div class="h-full bg-white rounded-full" :style="{ width: xpPercent + '%' }" />
        </div>
        <div class="flex justify-between mt-1.5">
          <span class="font-mono text-[11px] font-bold tracking-[0.04em] uppercase text-white/80">{{ currentExp }} / {{ expRequiredForNextLevel }} XP</span>
          <span class="font-mono text-[11px] font-bold tracking-[0.04em] uppercase text-white/80">Next level: {{ toLevel + 1 }}</span>
        </div>
      </div>

      <!-- Unlocks -->
      <div class="relative w-[88%] bg-white/12 rounded-2xl p-3.5 text-left">
        <span class="font-mono text-[11px] font-bold tracking-[0.06em] uppercase text-white/70">Unlocked</span>
        <ul class="mt-1.5 ml-[18px] p-0 text-[14px] font-semibold leading-relaxed text-white">
          <li><strong>Rank:</strong> {{ rankName }}</li>
          <li><strong>Shield capacity</strong> ×{{ shieldMax }}</li>
        </ul>
      </div>

      <button @click="$emit('close')" class="relative flex items-center gap-2 px-7 py-3 text-[16px] font-extrabold rounded-2xl bg-white text-lx-feather-dark shadow-[0_4px_0_rgba(0,0,0,0.14)] transition-transform duration-75 active:translate-y-1 active:shadow-none mt-1">
        Continue
        <LmIcon name="arrow" :size="18" />
      </button>
    </div>
  </div>
</template>
