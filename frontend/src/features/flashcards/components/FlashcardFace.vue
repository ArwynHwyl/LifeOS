<script setup lang="ts">
import { ref, watch } from 'vue'

const props = defineProps<{
  front: string
  formula: string
  label?: string
  note?: string
  example?: string
  tags?: string[]
  cardKey?: string | number
}>()

const emit = defineEmits<{
  flip: [flipped: boolean]
}>()

const flipped = ref(false)

watch(() => props.cardKey, () => {
  flipped.value = false
})

function toggleFlip() {
  flipped.value = !flipped.value
  emit('flip', flipped.value)
}
</script>

<template>
  <div class="flex-1 flex min-h-0">
    <div class="relative w-full cursor-pointer" style="perspective: 1800px" @click="toggleFlip">
      <div
        class="relative w-full h-full transition-transform duration-500 ease-in-out"
        style="transform-style: preserve-3d"
        :style="{ transform: flipped ? 'rotateY(180deg)' : 'rotateY(0deg)' }"
      >
        <!-- Front face -->
        <div
          class="absolute inset-0 flex flex-col gap-3 p-7 bg-white border border-lx-line rounded-[24px] shadow-[0_1px_2px_rgba(0,0,0,0.04),0_24px_48px_-24px_rgba(0,0,0,0.18)] overflow-hidden"
          style="backface-visibility: hidden"
        >
          <span class="font-mono text-[11px] font-bold tracking-[0.06em] uppercase text-lx-ink-faint relative">Front Side</span>

          <div class="flex-1 flex flex-col items-center justify-center gap-4 text-center relative">
            <h2 class="font-display text-[38px] font-extrabold tracking-tight text-lx-ink leading-tight m-0">{{ front }}</h2>
            <p class="text-[14px] font-semibold text-lx-ink-faint">click the card to reveal the answer</p>
          </div>
        </div>

        <!-- Back face -->
        <div
          class="absolute inset-0 flex flex-col gap-3.5 p-7 bg-white border border-lx-line rounded-[24px] shadow-[0_1px_2px_rgba(0,0,0,0.04),0_24px_48px_-24px_rgba(0,0,0,0.18)] overflow-auto"
          style="backface-visibility: hidden; transform: rotateY(180deg)"
        >
          <div class="flex items-center justify-between shrink-0">
            <span class="font-mono text-[11px] font-bold tracking-[0.06em] uppercase text-lx-ink-faint">Back Side</span>
            <span v-if="label" class="text-[13px] font-semibold text-lx-ink-soft">{{ label }}</span>
          </div>

          <!-- Formula chalkboard -->
          <div class="relative bg-lx-eel rounded-2xl px-[18px] py-[22px] text-center overflow-hidden shrink-0">
            <div class="absolute inset-0 bg-chalk-dots pointer-events-none" />
            <em class="relative font-math italic font-semibold text-[26px] text-white not-italic">{{ formula }}</em>
          </div>

          <!-- When to use -->
          <div v-if="note" class="bg-lx-surface-soft rounded-2xl p-3 shrink-0">
            <span class="font-mono text-[11px] font-bold tracking-[0.06em] uppercase text-lx-ink-faint">When to use</span>
            <p class="text-[14px] font-semibold text-lx-ink mt-1 leading-snug m-0">{{ note }}</p>
          </div>

          <!-- Example -->
          <div v-if="example" class="bg-lx-macaw/10 rounded-2xl p-3 shrink-0">
            <span class="font-mono text-[11px] font-bold tracking-[0.06em] uppercase text-lx-macaw-dark">Example</span>
            <em class="block font-math italic text-[16px] text-lx-ink mt-1">{{ example }}</em>
          </div>

          <!-- Tags -->
          <div v-if="tags?.length" class="flex gap-1.5 flex-wrap mt-auto shrink-0">
            <span
              v-for="t in tags"
              :key="t"
              class="px-2.5 py-1 text-[11px] font-bold rounded-full bg-lx-surface-soft text-lx-ink-soft"
            >#{{ t }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
