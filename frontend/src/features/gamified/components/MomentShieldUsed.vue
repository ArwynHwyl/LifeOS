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
    <div class="absolute inset-0 bg-[rgba(14,13,11,0.55)]" @click="$emit('close')" />

    <!-- Notification card -->
    <div class="anim-pop relative z-10 w-[min(620px,80%)] bg-white rounded-[24px] shadow-[0_24px_60px_-20px_rgba(0,0,0,0.4)] px-6 py-5 flex items-center gap-[18px]">
      <div class="anim-ring w-16 h-16 rounded-2xl bg-lx-macaw/10 text-lx-macaw flex items-center justify-center shrink-0" style="--i: 2">
        <LmIcon name="shield" :size="34" :filled="true" />
      </div>
      <div class="flex-1">
        <span class="font-mono text-[11px] font-bold tracking-[0.06em] uppercase text-lx-macaw block">Shield activated</span>
        <h2 class="font-display text-[24px] font-extrabold tracking-tight text-lx-ink mt-0.5 mb-1 m-0">Streak protected!</h2>
        <p class="text-[14px] font-semibold text-lx-ink-soft m-0">You missed yesterday — a shield kept your <strong class="text-lx-ink">{{ streakDays }}-day streak</strong> alive.</p>
      </div>
      <div class="text-right shrink-0">
        <span class="font-mono text-[11px] font-bold tracking-[0.06em] uppercase text-lx-ink-faint block mb-1">Shields left</span>
        <div class="flex gap-1 justify-end">
          <div
            v-for="i in shieldMax"
            :key="i"
            :class="[
              'w-7 h-8 rounded-lg flex items-center justify-center',
              i <= shieldsRemaining ? 'bg-lx-macaw/10 text-lx-macaw' : 'bg-lx-surface-soft text-lx-ink-faint'
            ]"
          >
            <LmIcon name="shield" :size="18" :filled="i <= shieldsRemaining" />
          </div>
        </div>
      </div>
    </div>

    <button
      @click="$emit('close')"
      class="relative z-10 mt-5 px-5 py-2.5 text-[14px] font-extrabold rounded-2xl bg-lx-macaw text-white shadow-[0_4px_0_var(--color-lx-macaw-dark)] transition-transform duration-75 active:translate-y-1 active:shadow-none"
    >
      Got it
    </button>
  </div>
</template>
