<script setup lang="ts">
import LmIcon from './LmIcon.vue'
defineEmits<{ close: [] }>()
</script>

<template>
  <div class="absolute inset-0 flex flex-col items-center z-50 pt-10" @click.self="$emit('close')">
    <div class="absolute inset-0 bg-[rgba(14,13,11,0.55)]" @click="$emit('close')" />

    <!-- Notification card (top-center) -->
    <div class="relative z-10 w-[min(620px,80%)] bg-lm-surface border-2 border-lm-line rounded-[24px] shadow-stamp-lg px-6 py-5 flex items-center gap-[18px]">
      <div class="w-16 h-16 rounded-full bg-lm-blue-soft border-2 border-lm-line text-lm-blue shadow-stamp-sm flex items-center justify-center shrink-0">
        <LmIcon name="shield" :size="36" :filled="true" />
      </div>
      <div class="flex-1">
        <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-blue block">SHIELD ACTIVATED</span>
        <h2 class="font-display text-[26px] font-bold tracking-tight text-lm-ink mt-0.5 mb-1 m-0">Streak protected!</h2>
        <p class="text-[14px] text-lm-ink-2 m-0">You missed yesterday — a shield kept your <strong>7-day streak</strong> alive.</p>
      </div>
      <div class="text-right shrink-0">
        <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3 block mb-1">SHIELDS LEFT</span>
        <div class="flex gap-1 justify-end">
          <div class="w-7 h-8 rounded-[4px] bg-lm-blue-soft border-2 border-lm-line text-lm-blue flex items-center justify-center">
            <LmIcon name="shield" :size="18" :filled="true" />
          </div>
          <div class="w-7 h-8 rounded-[4px] bg-lm-bg-soft border-2 border-dashed border-lm-line text-lm-ink-3 flex items-center justify-center">
            <LmIcon name="shield" :size="18" />
          </div>
        </div>
      </div>
    </div>

    <!-- Animation storyboard -->
    <div class="relative z-10 w-[min(880px,92%)] bg-lm-surface border-2 border-lm-line rounded-[24px] shadow-stamp-md px-5 py-4 mt-auto mb-8">
      <div class="flex items-baseline gap-2.5 mb-2.5">
        <h3 class="font-display text-[18px] font-bold text-lm-ink m-0">Animation storyboard</h3>
        <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3">~1.2s · PLAYS ONCE</span>
      </div>
      <div class="flex gap-2.5 justify-center items-center">
        <template v-for="(frame, i) in [
          { n: '1', t: '0.0s', label: 'streak at risk',   hi: false },
          { n: '2', t: '0.4s', label: 'shield flies in',  hi: false },
          { n: '3', t: '0.6s', label: 'impact + glow',    hi: true  },
          { n: '4', t: '1.2s', label: 'streak safe ✓',    hi: false },
        ]" :key="i">
          <div :class="['w-[130px] p-2.5 border-2 border-lm-line rounded-[12px] flex flex-col gap-1.5', frame.hi ? 'bg-lm-yellow-soft shadow-stamp-md' : 'bg-lm-surface shadow-stamp-sm']">
            <div class="flex justify-between">
              <span class="font-mono text-[10px] font-semibold uppercase text-lm-ink-3">FRAME {{ frame.n }}</span>
              <span class="font-mono text-[10px] font-semibold uppercase text-lm-ink-3">{{ frame.t }}</span>
            </div>
            <div class="h-[78px] flex items-center justify-center relative">
              <div v-if="frame.hi" class="absolute w-[90px] h-[90px] rounded-full" style="background: radial-gradient(closest-side, #ffd333 0%, transparent 70%)" />
              <span class="relative text-lm-rust"><LmIcon name="flame" :size="44" :filled="true" /></span>
              <span class="absolute text-lm-blue" :style="{ opacity: i === 0 ? 0 : 1, transform: i === 0 ? 'translateX(-90px)' : i === 1 ? 'translateX(-35px)' : 'translateX(0)' }">
                <LmIcon name="shield" :size="32" :filled="true" />
              </span>
            </div>
            <p class="text-[11px] text-center text-lm-ink-2 font-semibold m-0">{{ frame.label }}</p>
          </div>
          <span v-if="i < 3" class="font-display text-[22px] text-lm-ink-3 shrink-0">→</span>
        </template>
      </div>
    </div>
  </div>
</template>
