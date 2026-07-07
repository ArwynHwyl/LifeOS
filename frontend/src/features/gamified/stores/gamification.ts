import { defineStore } from 'pinia'
import {
  acknowledgeNotification,
  getLearnerProfile,
  getPendingNotifications,
  type LearnerProfileSummaryDto,
  type PendingLearnerEventDto,
} from '../services/gamification'

export const useGamificationStore = defineStore('gamification', {
  state: () => ({
    profile: null as LearnerProfileSummaryDto | null,
    pendingQueue: [] as PendingLearnerEventDto[],
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
  },
})
