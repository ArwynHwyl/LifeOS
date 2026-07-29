import { defineStore } from 'pinia'
import {
  acknowledgeNotification,
  getLearnerProfile,
  getPendingNotifications,
  type AchievementDto,
  type GamificationRewardDto,
  type LearnerProfileSummaryDto,
  type PendingLearnerEventDto,
} from '../services/gamification'

const ACHIEVEMENT_TOAST_DURATION_MS = 5000

export interface AchievementToast {
  id: number
  achievement: AchievementDto
}

export interface PendingLevelUp {
  fromLevel: number
  toLevel: number
}

let achievementToastSeq = 0

export const useGamificationStore = defineStore('gamification', {
  state: () => ({
    profile: null as LearnerProfileSummaryDto | null,
    pendingQueue: [] as PendingLearnerEventDto[],
    achievementToasts: [] as AchievementToast[],
    pendingLevelUp: null as PendingLevelUp | null,
  }),
  getters: {
    activeNotification: (state) => state.pendingQueue[0] ?? null,
  },
  actions: {
    async fetchProfile() {
      try {
        this.profile = await getLearnerProfile()
      } catch {
        this.profile = null
      }
    },
    async fetchPendingNotifications() {
      try {
        this.pendingQueue = await getPendingNotifications()
      } catch {
        this.pendingQueue = []
      }
    },
    async initialize() {
      await Promise.all([this.fetchProfile(), this.fetchPendingNotifications()])
    },
    async dismissActiveNotification() {
      const notification = this.activeNotification
      if (!notification) return
      this.pendingQueue = this.pendingQueue.slice(1)
      try {
        await acknowledgeNotification(notification.id)
      } catch {
        // best-effort ack; if it fails the notification simply reappears next load
      }
    },
    enqueueAchievements(achievements: AchievementDto[]) {
      for (const achievement of achievements) {
        const id = ++achievementToastSeq
        this.achievementToasts.push({ id, achievement })
        setTimeout(() => this.dismissAchievementToast(id), ACHIEVEMENT_TOAST_DURATION_MS)
      }
    },
    dismissAchievementToast(id: number) {
      this.achievementToasts = this.achievementToasts.filter((toast) => toast.id !== id)
    },
    handleReward(reward: GamificationRewardDto | undefined) {
      if (!reward) return
      if (reward.achievementsUnlocked.length) {
        this.enqueueAchievements(reward.achievementsUnlocked)
      }
      if (reward.leveledUp) {
        const fromLevel = this.profile?.level ?? reward.newLevel - 1
        this.pendingLevelUp = { fromLevel, toLevel: reward.newLevel }
        void this.fetchProfile()
      } else if (reward.expAwarded > 0) {
        void this.fetchProfile()
      }
    },
    dismissLevelUp() {
      this.pendingLevelUp = null
    },
  },
})
