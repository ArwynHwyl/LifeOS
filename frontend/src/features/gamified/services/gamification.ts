import api from '@/services/api'

export interface LearnerProfileSummaryDto {
  level: number
  rankName: string
  currentExp: number
  expRequiredForNextLevel: number
  totalExp: number
  currentStreak: number
  longestStreak: number
  streakBonusPercent: number
  currentShield: number
  shieldMax: number
  memberSince: string
}

export interface LevelRoadmapEntryDto {
  level: number
  rankName: string
  expRequiredToReach: number
  shieldMaxTotal: number
  unlockDescription: string
  achieved: boolean
}

export interface AchievementDto {
  code: string
  name: string
  description: string
  iconGlyph: string
  expReward: number
  unlocked: boolean
  unlockedAt: string | null
}

export type LearnerEventType = 'SHIELD_CONSUMED' | 'STREAK_LOST'

export interface PendingLearnerEventDto {
  id: number
  eventType: LearnerEventType
  shieldsRemaining: number | null
  shieldMax: number | null
  streakDaysLost: number | null
  longestStreak: number | null
  createdAt: string
}

export async function getLearnerProfile() {
  const { data } = await api.get<LearnerProfileSummaryDto>('/v1/learner/gamification/profile')
  return data
}

export async function getLevelRoadmap() {
  const { data } = await api.get<LevelRoadmapEntryDto[]>('/v1/learner/gamification/levels')
  return data
}

export async function getAchievements() {
  const { data } = await api.get<AchievementDto[]>('/v1/learner/gamification/achievements')
  return data
}

export async function getPendingNotifications() {
  const { data } = await api.get<PendingLearnerEventDto[]>('/v1/learner/gamification/notifications/pending')
  return data
}

export async function acknowledgeNotification(eventId: number) {
  await api.post(`/v1/learner/gamification/notifications/${eventId}/ack`)
}

export interface DailyActivityDto {
  date: string
  subtopicsCompleted: number
  expEarned: number
}

export async function getRecentActivity(days = 7) {
  const { data } = await api.get<DailyActivityDto[]>('/v1/learner/gamification/activity', {
    params: { days },
  })
  return data
}
