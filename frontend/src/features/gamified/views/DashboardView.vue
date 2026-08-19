<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import LmIcon from '../../learning/components/LmIcon.vue'
import MomentLevelUp from '../components/MomentLevelUp.vue'
import MomentStreakBroken from '../components/MomentStreakBroken.vue'
import MomentShieldUsed from '../components/MomentShieldUsed.vue'
import {
  getAchievements,
  getRecentActivity,
  type AchievementDto,
  type DailyActivityDto,
} from '../services/gamification'
import { useGamificationStore } from '../stores/gamification'

const router = useRouter()
const gamificationStore = useGamificationStore()

const profile = computed(() => gamificationStore.profile)
const achievements = ref<AchievementDto[]>([])
const recentActivity = ref<DailyActivityDto[]>([])

onMounted(async () => {
  void gamificationStore.fetchProfile()
  try {
    achievements.value = await getAchievements()
  } catch {
    achievements.value = []
  }
  try {
    recentActivity.value = await getRecentActivity(7)
  } catch {
    recentActivity.value = []
  }
})

const authUser = computed(() => {
  const raw = localStorage.getItem('authUser')
  if (!raw) return null
  try {
    return JSON.parse(raw) as { username?: string; firstName?: string; lastName?: string; email?: string }
  } catch {
    return null
  }
})

const displayName = computed(() => {
  const user = authUser.value
  if (!user) return 'Learner'
  const fullName = [user.firstName, user.lastName].filter(Boolean).join(' ')
  return fullName || user.username || 'Learner'
})

const avatarInitials = computed(() =>
  displayName.value.split(' ').map((w) => w[0] ?? '').slice(0, 2).join('').toUpperCase() || 'L',
)

const memberSinceLabel = computed(() => {
  if (!profile.value?.memberSince) return ''
  const date = new Date(profile.value.memberSince)
  return `MEMBER SINCE ${date.toLocaleDateString('en-US', { month: 'short', year: 'numeric' }).toUpperCase()}`
})

const xpProgressPercent = computed(() => {
  const p = profile.value
  if (!p || p.expRequiredForNextLevel <= 0) return 100
  return Math.min(100, (p.currentExp / p.expRequiredForNextLevel) * 100)
})

const xpToNextLevelLabel = computed(() => {
  const p = profile.value
  if (!p) return ''
  if (p.expRequiredForNextLevel <= 0) return 'MAX LEVEL REACHED'
  return `${p.expRequiredForNextLevel - p.currentExp} XP TO LEVEL ${p.level + 1}`
})

const WEEKDAY_LABELS = ['Su', 'Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa']

const weekStrip = computed(() =>
  recentActivity.value.map((day) => ({
    label: WEEKDAY_LABELS[new Date(day.date).getDay()],
    active: day.subtopicsCompleted > 0,
  })),
)

const unlockedCount = computed(() => achievements.value.filter((a) => a.unlocked).length)
const trophyProgressPercent = computed(() =>
  achievements.value.length === 0 ? 0 : (unlockedCount.value / achievements.value.length) * 100,
)

const showLevelUp     = ref(false)
const showStreakBroken = ref(false)
const showShieldUsed   = ref(false)
</script>

<template>
  <main class="flex-1 overflow-auto bg-lm-bg relative">
    <div class="absolute inset-0 bg-dot-grid opacity-40 pointer-events-none" />

    <div class="relative max-w-[1280px] mx-auto px-9 py-7 flex flex-col gap-5">

      <!-- Hero profile card -->
      <div class="relative flex items-center gap-6 px-7 py-6 bg-lm-yellow border-2 border-lm-line rounded-[24px] shadow-stamp-lg overflow-hidden">
        <svg class="absolute top-[-40px] right-[-50px] opacity-12 pointer-events-none" width="180" height="180" viewBox="0 0 80 80">
          <circle cx="40" cy="40" r="30" stroke="#1a1814" stroke-width="2" fill="none"/>
          <path d="M10 40 H 70 M 40 10 V 70" stroke="#1a1814" stroke-width="1.5" fill="none" stroke-dasharray="3 5"/>
        </svg>
        <svg class="absolute bottom-[-10px] left-[-10px] opacity-15 pointer-events-none" width="80" height="80" viewBox="0 0 80 80">
          <template v-for="i in 25" :key="i">
            <circle :cx="10 + ((i-1) % 5) * 15" :cy="10 + Math.floor((i-1) / 5) * 15" r="1.5" fill="#1a1814"/>
          </template>
        </svg>

        <!-- Avatar -->
        <div class="w-[92px] h-[92px] rounded-full bg-lm-surface border-2 border-lm-line shadow-stamp-sm flex items-center justify-center font-display font-bold text-[38px] text-lm-ink shrink-0">
          {{ avatarInitials }}
        </div>

        <div class="flex-1 min-w-0 relative">
          <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-2">{{ memberSinceLabel }}</span>
          <h1 class="font-display text-[36px] font-bold tracking-tight text-lm-ink leading-none mt-0.5 mb-1 m-0">{{ displayName }}</h1>
          <p v-if="profile" class="text-[16px] text-lm-ink mt-1 m-0">
            Level <strong>{{ profile.level }}</strong> ·
            <span class="bg-lm-surface border-2 border-lm-line px-2.5 py-0.5 rounded-full font-semibold text-[14px]">{{ profile.rankName }}</span>
          </p>
          <div v-if="profile" class="flex items-center gap-2.5 mt-3.5 max-w-[460px]">
            <div class="flex-1 h-3.5 rounded-full overflow-hidden border border-lm-line" style="background: rgba(26,24,20,.15)">
              <div class="h-full bg-lm-surface transition-all duration-200" :style="{ width: `${xpProgressPercent}%` }" />
            </div>
            <span class="font-mono text-[12px] font-semibold text-lm-ink shrink-0">{{ profile.currentExp }} / {{ profile.expRequiredForNextLevel }} XP</span>
          </div>
          <p v-if="profile" class="font-mono text-[11px] text-lm-ink-2 mt-1 m-0">{{ xpToNextLevelLabel }}</p>
        </div>

        <button
          class="flex items-center gap-2 px-[18px] py-[9px] text-[15px] font-semibold border-2 border-lm-line rounded-full bg-lm-ink text-lm-bg shadow-stamp-sm hover:-translate-y-px hover:shadow-stamp-md transition-all duration-200 shrink-0 relative"
          @click="router.push('/learn/levels')"
        >
          View level roadmap
        </button>
      </div>

      <!-- Streak + shields row -->
      <div v-if="profile" class="flex items-center gap-[22px] px-[22px] py-[22px] bg-lm-surface border-2 border-lm-line rounded-[24px] shadow-stamp-md">
        <!-- Streak block -->
        <div class="flex items-center gap-3.5">
          <div class="w-16 h-16 rounded-[12px] bg-lm-rust-soft border-2 border-lm-line text-lm-rust shadow-stamp-sm flex items-center justify-center shrink-0">
            <LmIcon name="flame" :size="36" :filled="true" />
          </div>
          <div>
            <p class="font-display text-[36px] font-bold leading-none text-lm-ink m-0">{{ profile.currentStreak }} <span class="text-[18px] font-medium text-lm-ink-2">days</span></p>
            <p class="text-[13px] text-lm-ink mt-1 m-0">
              study streak
              <template v-if="profile.streakBonusPercent > 0"> · <strong class="text-lm-rust">+{{ profile.streakBonusPercent }}% XP boost</strong></template>
            </p>
          </div>
        </div>

        <div class="w-0.5 self-stretch bg-lm-line-soft" />

        <!-- Week strip -->
        <div>
          <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3">LAST 7 DAYS</span>
          <div class="flex gap-[7px] mt-1.5">
            <div v-for="(d, i) in weekStrip" :key="i" class="text-center">
              <div :class="[
                'w-9 h-9 rounded-full border-2 flex items-center justify-center font-display font-bold text-sm',
                d.active ? 'bg-lm-yellow border-lm-line shadow-stamp-sm text-lm-ink' : 'bg-lm-surface border-dashed border-lm-line text-lm-ink'
              ]">
                <LmIcon v-if="d.active" name="check" :size="16" />
                <span v-else class="text-sm">·</span>
              </div>
              <p class="font-mono text-[10px] text-lm-ink-3 mt-1 m-0">{{ d.label }}</p>
            </div>
          </div>
        </div>

        <div class="flex-1" />

        <!-- Shields -->
        <div class="text-center">
          <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3 block mb-1.5">SHIELDS · {{ profile.currentShield }} / {{ profile.shieldMax }}</span>
          <div class="flex gap-1.5">
            <div
              v-for="i in profile.shieldMax"
              :key="i"
              :class="[
                'w-11 h-[50px] rounded-[8px] border-2 flex items-center justify-center',
                i <= profile.currentShield ? 'bg-lm-blue-soft border-lm-line border-solid text-lm-blue shadow-stamp-sm' : 'bg-lm-bg-soft border-dashed border-lm-line text-lm-ink-3'
              ]"
            >
              <LmIcon name="shield" :size="26" :filled="i <= profile.currentShield" />
            </div>
          </div>
        </div>
      </div>

      <!-- Trophy case -->
      <section>
        <div class="flex items-baseline gap-3 mb-3.5">
          <h2 class="font-display text-[22px] font-bold text-lm-ink m-0">Trophy case</h2>
          <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3">{{ unlockedCount }} OF {{ achievements.length }} UNLOCKED</span>
          <div class="w-24 h-1.5 bg-lm-bg-soft rounded-full overflow-hidden border border-lm-line self-center">
            <div class="h-full bg-lm-yellow" :style="{ width: `${trophyProgressPercent}%` }" />
          </div>
        </div>

        <div class="relative p-[22px] bg-lm-yellow-soft border-2 border-lm-line rounded-[24px] shadow-stamp-md overflow-hidden">
          <div class="absolute inset-0 bg-dot-grid opacity-40 pointer-events-none" />
          <div class="relative grid grid-cols-4 gap-4">
            <div
              v-for="a in achievements"
              :key="a.code"
              :class="[
                'flex flex-col items-center gap-2 p-4 text-center rounded-[18px] border-2 transition-all',
                a.unlocked ? 'bg-lm-surface border-lm-line shadow-stamp-sm' : 'bg-transparent border-dashed border-lm-line opacity-55'
              ]"
            >
              <div :class="[
                'w-[60px] h-[60px] rounded-full border-2 border-lm-line flex items-center justify-center font-math italic font-bold text-[24px] text-lm-ink',
                a.unlocked ? 'bg-lm-yellow shadow-stamp-sm' : 'bg-lm-bg-soft'
              ]">
                <template v-if="a.unlocked">{{ a.iconGlyph }}</template>
                <LmIcon v-else name="lock" :size="20" class="text-lm-ink-2" />
              </div>
              <p class="font-display font-bold text-[14px] leading-tight text-lm-ink m-0">{{ a.name }}</p>
              <p class="text-[11px] text-lm-ink-2 leading-snug m-0">{{ a.description }}</p>
            </div>
          </div>
        </div>
      </section>

      <!-- Demo moment triggers -->
      <div class="flex gap-3 flex-wrap">
        <button @click="showLevelUp = true"      class="px-4 py-2 text-sm font-semibold border-2 border-lm-line rounded-full bg-lm-yellow shadow-stamp-sm hover:-translate-y-px transition-all duration-200">✨ Demo: Level Up</button>
        <button @click="showStreakBroken = true"  class="px-4 py-2 text-sm font-semibold border-2 border-lm-line rounded-full bg-lm-rust-soft shadow-stamp-sm hover:-translate-y-px transition-all duration-200">💨 Demo: Streak Broken</button>
        <button @click="showShieldUsed = true"    class="px-4 py-2 text-sm font-semibold border-2 border-lm-line rounded-full bg-lm-blue-soft shadow-stamp-sm hover:-translate-y-px transition-all duration-200">🛡 Demo: Shield Used</button>
      </div>
    </div>

    <!-- Moment overlays (demo: fed with representative mock data, not live state) -->
    <MomentLevelUp
      v-if="showLevelUp"
      :from-level="6"
      :to-level="7"
      rank-name="Strategist"
      :current-exp="0"
      :exp-required-for-next-level="340"
      :shield-max="3"
      @close="showLevelUp = false"
    />
    <MomentStreakBroken v-if="showStreakBroken"  :streak-days-lost="7" :longest-streak="14" @close="showStreakBroken = false" />
    <MomentShieldUsed   v-if="showShieldUsed"    :streak-days="7" :shields-remaining="1" :shield-max="2" @close="showShieldUsed = false" />
  </main>
</template>
