<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import LmIcon from '../../learning/components/LmIcon.vue'
import CountUp from '@/components/motion/CountUp.vue'
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
  <main class="flex-1 bg-white">
    <div class="max-w-[1180px] mx-auto px-8 py-8 flex flex-col gap-4">

      <!-- Hero profile card -->
      <div class="anim-rise flex items-center gap-6 px-7 py-6 bg-white border border-lx-line rounded-[24px]">
        <!-- Avatar -->
        <div class="w-[88px] h-[88px] rounded-full bg-lx-macaw flex items-center justify-center font-display font-extrabold text-[34px] text-white shrink-0">
          {{ avatarInitials }}
        </div>

        <div class="flex-1 min-w-0">
          <span class="font-mono text-[11px] font-bold tracking-[0.06em] uppercase text-lx-ink-faint">{{ memberSinceLabel }}</span>
          <h1 class="font-display text-[32px] font-extrabold tracking-tight text-lx-ink leading-none mt-0.5 mb-1.5 m-0">{{ displayName }}</h1>
          <p v-if="profile" class="text-[15px] font-semibold text-lx-ink-soft mt-1 m-0 flex items-center gap-2">
            Level <CountUp :value="profile.level" :duration="700" />
            <span class="bg-lx-surface-soft px-2.5 py-0.5 rounded-full font-extrabold text-[13px] text-lx-ink">{{ profile.rankName }}</span>
          </p>
          <div v-if="profile" class="flex items-center gap-2.5 mt-3.5 max-w-[460px]">
            <div class="flex-1 h-2.5 rounded-full overflow-hidden bg-lx-surface-soft">
              <div class="bar-grow h-full bg-lx-feather rounded-full" :style="{ width: `${xpProgressPercent}%`, '--i': 2 }" />
            </div>
            <span class="font-mono text-[12px] font-bold text-lx-ink-soft shrink-0"><CountUp :value="profile.currentExp" /> / {{ profile.expRequiredForNextLevel }} XP</span>
          </div>
          <p v-if="profile" class="font-mono text-[10.5px] font-bold text-lx-ink-faint mt-1.5 m-0">{{ xpToNextLevelLabel }}</p>
        </div>

        <button
          class="flex items-center gap-2 px-5 py-2.5 text-[14px] font-extrabold rounded-2xl bg-lx-macaw text-white shadow-[0_4px_0_var(--color-lx-macaw-dark)] transition-transform duration-75 active:translate-y-1 active:shadow-none shrink-0"
          @click="router.push('/learn/levels')"
        >
          View level roadmap
        </button>
      </div>

      <!-- Streak + shields row -->
      <div v-if="profile" class="anim-rise flex items-center gap-6 px-7 py-6 bg-white border border-lx-line rounded-[24px]" style="--i: 1">
        <!-- Streak block -->
        <div class="flex items-center gap-3.5">
          <div class="w-16 h-16 rounded-2xl bg-lx-fox/10 text-lx-fox flex items-center justify-center shrink-0">
            <LmIcon name="flame" :size="34" :filled="true" class="anim-flame" />
          </div>
          <div>
            <p class="font-display text-[32px] font-extrabold leading-none text-lx-ink m-0"><CountUp :value="profile.currentStreak" /> <span class="text-[16px] font-semibold text-lx-ink-faint">days</span></p>
            <p class="text-[13px] font-semibold text-lx-ink-soft mt-1 m-0">
              study streak
              <template v-if="profile.streakBonusPercent > 0"> · <strong class="text-lx-fox-dark">+{{ profile.streakBonusPercent }}% XP boost</strong></template>
            </p>
          </div>
        </div>

        <div class="w-px self-stretch bg-lx-line" />

        <!-- Week strip -->
        <div>
          <span class="font-mono text-[11px] font-bold tracking-[0.06em] uppercase text-lx-ink-faint">Last 7 days</span>
          <div class="flex gap-[7px] mt-1.5">
            <div v-for="(d, i) in weekStrip" :key="i" class="anim-pop text-center" :style="{ '--i': i + 2 }">
              <div :class="[
                'w-9 h-9 rounded-full flex items-center justify-center font-display font-bold text-sm',
                d.active ? 'bg-lx-fox text-white' : 'bg-lx-surface-soft text-lx-ink-faint'
              ]">
                <LmIcon v-if="d.active" name="check" :size="16" />
                <span v-else class="text-sm">·</span>
              </div>
              <p class="font-mono text-[10px] font-bold text-lx-ink-faint mt-1 m-0">{{ d.label }}</p>
            </div>
          </div>
        </div>

        <div class="flex-1" />

        <!-- Shields -->
        <div class="flex flex-col items-end shrink-0">
          <span class="font-mono text-[11px] font-bold tracking-[0.06em] uppercase text-lx-ink-faint block mb-2">Shields · {{ profile.currentShield }} / {{ profile.shieldMax }}</span>
          <div class="flex flex-wrap justify-end gap-1.5 max-w-[300px]">
            <div
              v-for="i in profile.shieldMax"
              :key="i"
              :style="{ '--i': i + 2 }"
              :class="[
                'anim-pop w-10 h-11 rounded-xl flex items-center justify-center',
                i <= profile.currentShield ? 'bg-lx-macaw/10 text-lx-macaw' : 'bg-lx-surface-soft text-lx-ink-faint'
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
          <h2 class="font-display text-[19px] font-extrabold text-lx-ink m-0">Trophy case</h2>
          <span class="font-mono text-[11px] font-bold tracking-[0.06em] uppercase text-lx-ink-faint">{{ unlockedCount }} of {{ achievements.length }} unlocked</span>
          <div class="w-24 h-1.5 bg-lx-surface-soft rounded-full overflow-hidden self-center">
            <div class="h-full bg-lx-feather rounded-full" :style="{ width: `${trophyProgressPercent}%` }" />
          </div>
        </div>

        <div class="anim-rise p-6 bg-white border border-lx-line rounded-[24px]" style="--i: 2">
          <div class="grid grid-cols-4 gap-4">
            <div
              v-for="(a, i) in achievements"
              :key="a.code"
              :style="{ '--i': i + 3 }"
              :class="[
                'anim-pop hover-wiggle flex flex-col items-center gap-2 p-4 text-center rounded-2xl transition-transform duration-150',
                a.unlocked ? 'bg-lx-surface-soft hover:-translate-y-0.5' : 'opacity-50'
              ]"
            >
              <div :class="[
                'wiggle-target w-[56px] h-[56px] rounded-full flex items-center justify-center font-math italic font-bold text-[22px] text-white',
                a.unlocked ? ['bg-lx-macaw', 'bg-lx-fox', 'bg-lx-beetle', 'bg-lx-feather'][i % 4] : 'bg-lx-eel'
              ]">
                <template v-if="a.unlocked">{{ a.iconGlyph }}</template>
                <LmIcon v-else name="lock" :size="20" />
              </div>
              <p class="font-display font-extrabold text-[13.5px] leading-tight text-lx-ink m-0">{{ a.name }}</p>
              <p class="text-[11px] font-semibold text-lx-ink-faint leading-snug m-0">{{ a.description }}</p>
            </div>
          </div>
        </div>
      </section>

      <!-- Demo moment triggers -->
      <div class="flex gap-2.5 flex-wrap pt-2">
        <button @click="showLevelUp = true"      class="px-3.5 py-2 text-xs font-bold rounded-2xl bg-lx-surface-soft text-lx-ink-soft hover:bg-lx-line transition-colors duration-150">Demo: Level Up</button>
        <button @click="showStreakBroken = true"  class="px-3.5 py-2 text-xs font-bold rounded-2xl bg-lx-surface-soft text-lx-ink-soft hover:bg-lx-line transition-colors duration-150">Demo: Streak Broken</button>
        <button @click="showShieldUsed = true"    class="px-3.5 py-2 text-xs font-bold rounded-2xl bg-lx-surface-soft text-lx-ink-soft hover:bg-lx-line transition-colors duration-150">Demo: Shield Used</button>
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
