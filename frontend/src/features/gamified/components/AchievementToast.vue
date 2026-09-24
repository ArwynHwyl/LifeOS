<script setup lang="ts">
import { useGamificationStore } from '../stores/gamification'

const gamificationStore = useGamificationStore()
</script>

<template>
  <div class="fixed top-5 left-1/2 z-[120] flex -translate-x-1/2 flex-col gap-3 pointer-events-none">
    <TransitionGroup name="achievement-toast">
      <div
        v-for="toast in gamificationStore.achievementToasts"
        :key="toast.id"
        class="pointer-events-auto w-[360px] max-w-[calc(100vw-2rem)] bg-white rounded-[20px] shadow-[0_20px_44px_-16px_rgba(0,0,0,0.3)] px-4 py-3.5 flex items-center gap-3.5 cursor-pointer transition-transform duration-150 hover:-translate-y-0.5"
        @click="gamificationStore.dismissAchievementToast(toast.id)"
      >
        <div class="w-12 h-12 rounded-full bg-lx-fox flex items-center justify-center shrink-0 font-display font-extrabold text-[19px] text-white">
          {{ toast.achievement.iconGlyph }}
        </div>
        <div class="flex-1 min-w-0">
          <span class="font-mono text-[10px] font-bold tracking-[0.06em] uppercase text-lx-fox-dark block whitespace-nowrap">Achievement unlocked</span>
          <p class="font-display font-extrabold text-[14.5px] text-lx-ink m-0 leading-tight truncate">{{ toast.achievement.name }}</p>
          <p class="text-[12px] font-semibold text-lx-ink-faint m-0 mt-0.5 truncate">{{ toast.achievement.description }}</p>
        </div>
        <span class="font-mono text-[12px] font-bold text-lx-feather-dark shrink-0">+{{ toast.achievement.expReward }} XP</span>
      </div>
    </TransitionGroup>
  </div>
</template>

<style scoped>
.achievement-toast-enter-active { transition: all 0.5s cubic-bezier(0.34, 1.56, 0.64, 1); }
.achievement-toast-leave-active { transition: all 0.25s ease-in; }
.achievement-toast-enter-from,
.achievement-toast-leave-to {
  opacity: 0;
  transform: translateY(-20px) scale(0.96);
}
</style>
