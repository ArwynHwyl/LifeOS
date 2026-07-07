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
    <div class="relative z-10 w-[min(620px,80%)] bg-lm-surface border-2 border-lm-line rounded-[24px] shadow-stamp-lg px-6 py-5 flex items-center gap-[18px]">
      <div class="w-16 h-16 rounded-full bg-lm-blue-soft border-2 border-lm-line text-lm-blue shadow-stamp-sm flex items-center justify-center shrink-0">
        <LmIcon name="shield" :size="36" :filled="true" />
      </div>
      <div class="flex-1">
        <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-blue block">SHIELD ACTIVATED</span>
        <h2 class="font-display text-[26px] font-bold tracking-tight text-lm-ink mt-0.5 mb-1 m-0">Streak protected!</h2>
        <p class="text-[14px] text-lm-ink-2 m-0">You missed yesterday — a shield kept your <strong>{{ streakDays }}-day streak</strong> alive.</p>
      </div>
      <div class="text-right shrink-0">
        <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3 block mb-1">SHIELDS LEFT</span>
        <div class="flex gap-1 justify-end">
          <div
            v-for="i in shieldMax"
            :key="i"
            :class="[
              'w-7 h-8 rounded-[4px] border-2 flex items-center justify-center',
              i <= shieldsRemaining
                ? 'bg-lm-blue-soft border-lm-line border-solid text-lm-blue'
                : 'bg-lm-bg-soft border-dashed border-lm-line text-lm-ink-3'
            ]"
          >
            <LmIcon name="shield" :size="18" :filled="i <= shieldsRemaining" />
          </div>
        </div>
      </div>
    </div>

    <button
      @click="$emit('close')"
      class="relative z-10 mt-5 px-[18px] py-[9px] text-[15px] font-semibold border-2 border-lm-line rounded-full bg-lm-ink text-lm-bg shadow-stamp-sm hover:-translate-y-px hover:shadow-stamp-md transition-all duration-200"
    >
      Got it
    </button>
  </div>
</template>
