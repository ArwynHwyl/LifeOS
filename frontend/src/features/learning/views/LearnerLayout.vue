<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import LearnerSidebar from '../components/LearnerSidebar.vue'
import AchievementToast from '@/features/gamified/components/AchievementToast.vue'
import MomentLevelUp from '@/features/gamified/components/MomentLevelUp.vue'
import MomentShieldUsed from '@/features/gamified/components/MomentShieldUsed.vue'
import MomentStreakBroken from '@/features/gamified/components/MomentStreakBroken.vue'
import { useGamificationStore } from '@/features/gamified/stores/gamification'

const route = useRoute()
const gamificationStore = useGamificationStore()

const activeTab = computed<'courses' | 'assessments' | 'flashcards' | 'dashboard' | 'levels'>(() => {
  const path = route.path
  if (path.includes('/assessments')) return 'assessments'
  if (path.includes('/flashcards')) return 'flashcards'
  if (path.includes('/levels')) return 'levels'
  if (path.includes('/dashboard')) return 'dashboard'
  return 'courses'
})

onMounted(() => {
  void gamificationStore.initialize()
})
</script>

<template>
  <div class="learning-app flex h-screen overflow-hidden bg-white font-body">
    <LearnerSidebar :active="activeTab" />
    <router-view v-slot="{ Component, route: current }">
      <Transition name="page" mode="out-in">
        <component :is="Component" :key="current.path" class="flex-1 min-h-0 min-w-0 overflow-auto" />
      </Transition>
    </router-view>

    <MomentLevelUp
      v-if="gamificationStore.pendingLevelUp"
      :from-level="gamificationStore.pendingLevelUp.fromLevel"
      :to-level="gamificationStore.pendingLevelUp.toLevel"
      :rank-name="gamificationStore.profile?.rankName"
      :current-exp="gamificationStore.profile?.currentExp"
      :exp-required-for-next-level="gamificationStore.profile?.expRequiredForNextLevel"
      :shield-max="gamificationStore.profile?.shieldMax"
      @close="gamificationStore.dismissLevelUp"
    />
    <MomentShieldUsed
      v-else-if="gamificationStore.activeNotification?.eventType === 'SHIELD_CONSUMED'"
      :streak-days="gamificationStore.profile?.currentStreak ?? 0"
      :shields-remaining="gamificationStore.activeNotification.shieldsRemaining ?? 0"
      :shield-max="gamificationStore.activeNotification.shieldMax ?? 0"
      @close="gamificationStore.dismissActiveNotification"
    />
    <MomentStreakBroken
      v-else-if="gamificationStore.activeNotification?.eventType === 'STREAK_LOST'"
      :streak-days-lost="gamificationStore.activeNotification.streakDaysLost ?? 0"
      :longest-streak="gamificationStore.activeNotification.longestStreak ?? 0"
      @close="gamificationStore.dismissActiveNotification"
    />
    <AchievementToast />
  </div>
</template>

<style scoped>
.page-enter-active { transition: opacity 0.28s ease, transform 0.32s cubic-bezier(0.22, 1, 0.36, 1); }
.page-leave-active { transition: opacity 0.12s ease; }
.page-enter-from { opacity: 0; transform: translateY(10px); }
.page-leave-to { opacity: 0; }
@media (prefers-reduced-motion: reduce) {
  .page-enter-active, .page-leave-active { transition: none; }
}
</style>
