<script setup lang="ts">
defineProps<{
  front: string
  formula: string
  label?: string
  note?: string
  example?: string
  tags?: string[]
}>()
</script>

<template>
  <div class="flex-1 flex gap-[18px] min-h-0">

    <!-- Front face -->
    <div class="flex-1 relative flex flex-col gap-3 p-7 bg-lm-surface border-2 border-lm-line rounded-[24px] shadow-stamp-md overflow-hidden">
      <div class="absolute inset-0 bg-dot-grid opacity-35 pointer-events-none" />
      <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3 relative">FRONT · TAP TO FLIP</span>

      <div class="flex-1 flex flex-col items-center justify-center gap-4 text-center relative">
        <h2 class="font-display text-[36px] font-bold tracking-tight text-lm-ink leading-tight m-0">{{ front }}</h2>
        <svg width="80" height="6" viewBox="0 0 80 6">
          <path d="M2 3 Q 15 0, 28 3 T 54 3 T 78 3" stroke="#1a1814" stroke-width="2" fill="none" stroke-linecap="round"/>
        </svg>
        <p class="text-[14px] text-lm-ink-2">recall the formula</p>
      </div>

      <!-- Dot ornament -->
      <svg class="absolute bottom-2.5 right-2.5 opacity-20 pointer-events-none" width="50" height="50" viewBox="0 0 80 80">
        <template v-for="i in 25" :key="i">
          <circle :cx="10 + ((i - 1) % 5) * 15" :cy="10 + Math.floor((i - 1) / 5) * 15" r="1.5" fill="#1a1814"/>
        </template>
      </svg>
    </div>

    <!-- Back face -->
    <div class="flex flex-col gap-3.5 p-7 bg-lm-surface border-2 border-lm-line rounded-[24px] shadow-stamp-md overflow-hidden" style="flex: 1.3">
      <div class="flex items-center justify-between">
        <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3">BACK · ANSWER</span>
        <span v-if="label" class="text-[13px] text-lm-ink-2">{{ label }}</span>
      </div>

      <!-- Formula chalkboard -->
      <div class="relative bg-lm-ink rounded-[12px] border-2 border-lm-line px-[18px] py-[22px] text-center overflow-hidden">
        <div class="absolute inset-0 bg-chalk-dots pointer-events-none" />
        <em class="relative font-math italic font-semibold text-[28px] text-lm-bg not-italic">{{ formula }}</em>
      </div>

      <!-- When to use -->
      <div v-if="note" class="bg-lm-bg-soft border-2 border-dashed border-lm-line rounded-[12px] p-3">
        <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3">WHEN TO USE</span>
        <p class="text-[14px] text-lm-ink mt-1 leading-snug m-0">{{ note }}</p>
      </div>

      <!-- Example -->
      <div v-if="example" class="bg-lm-yellow-soft border-2 border-dashed border-lm-line rounded-[12px] p-3">
        <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3">EXAMPLE</span>
        <em class="block font-math italic text-[16px] text-lm-ink mt-1">{{ example }}</em>
      </div>

      <!-- Tags -->
      <div v-if="tags?.length" class="flex gap-1.5 flex-wrap mt-auto">
        <span
          v-for="t in tags"
          :key="t"
          class="px-2.5 py-1 text-[11px] font-semibold border border-lm-line rounded-full bg-lm-bg-soft text-lm-ink"
        >#{{ t }}</span>
      </div>
    </div>
  </div>
</template>
