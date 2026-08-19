<script setup lang="ts">
import { useGamificationStore } from '../stores/gamification'

const gamificationStore = useGamificationStore()
</script>

<template>
  <div class="fixed bottom-6 right-6 z-[60] flex flex-col-reverse gap-3 pointer-events-none">
    <TransitionGroup name="achievement-toast">
      <div
        v-for="toast in gamificationStore.achievementToasts"
        :key="toast.id"
        class="pointer-events-auto w-[300px] bg-lm-surface border-2 border-lm-line rounded-[18px] shadow-stamp-lg px-4 py-3.5 flex items-center gap-3.5 cursor-pointer hover:-translate-y-px transition-all duration-200"
        @click="gamificationStore.dismissAchievementToast(toast.id)"
      >
        <div class="w-12 h-12 rounded-full bg-lm-yellow border-2 border-lm-line shadow-stamp-sm flex items-center justify-center shrink-0 font-display font-bold text-[20px] text-lm-ink">
          {{ toast.achievement.iconGlyph }}
        </div>
        <div class="flex-1 min-w-0">
          <span class="font-mono text-[10px] font-semibold tracking-[0.06em] uppercase text-lm-rust block">ACHIEVEMENT UNLOCKED</span>
          <p class="font-display font-bold text-[15px] text-lm-ink m-0 leading-tight truncate">{{ toast.achievement.name }}</p>
          <p class="text-[12px] text-lm-ink-2 m-0 mt-0.5 truncate">{{ toast.achievement.description }}</p>
        </div>
        <span class="font-mono text-[12px] font-semibold text-lm-ink-2 shrink-0">+{{ toast.achievement.expReward }} XP</span>
      </div>
    </TransitionGroup>
  </div>
</template>

<style scoped>
.achievement-toast-enter-active,
.achievement-toast-leave-active {
  transition: all 0.25s ease;
}
.achievement-toast-enter-from {
  opacity: 0;
  transform: translateX(24px);
}
.achievement-toast-leave-to {
  opacity: 0;
  transform: translateX(24px);
}
</style>
