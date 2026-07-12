<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import LmIcon from './LmIcon.vue'
import { useGamificationStore } from '@/features/gamified/stores/gamification'

withDefaults(defineProps<{
  active?: 'courses' | 'assessments' | 'flashcards' | 'dashboard'
}>(), {
  active: 'courses',
})

const router = useRouter()
const gamificationStore = useGamificationStore()

const level = computed(() => gamificationStore.profile?.level ?? 1)
const currentExp = computed(() => gamificationStore.profile?.currentExp ?? 0)
const expRequiredForNextLevel = computed(() => gamificationStore.profile?.expRequiredForNextLevel ?? 100)
const expProgressPercent = computed(() =>
  expRequiredForNextLevel.value <= 0 ? 100 : (currentExp.value / expRequiredForNextLevel.value) * 100,
)
const streak = computed(() => gamificationStore.profile?.currentStreak ?? 0)
const shields = computed(() => gamificationStore.profile?.currentShield ?? 0)

const tabs: { id: 'courses' | 'assessments' | 'flashcards' | 'dashboard'; label: string; icon: 'book' | 'check' | 'card' | 'user' }[] = [
  { id: 'courses',     label: 'Courses',     icon: 'book' },
  { id: 'assessments', label: 'Assessments', icon: 'check' },
  { id: 'flashcards',  label: 'Flashcards',  icon: 'card' },
  { id: 'dashboard',   label: 'Dashboard',   icon: 'user' },
]

function navigate(id: 'courses' | 'assessments' | 'flashcards' | 'dashboard') {
  router.push(`/learn/${id}`)
}

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

const profileOpen = ref(false)
const profileRef = ref<HTMLElement | null>(null)

function onClickOutside(event: MouseEvent) {
  if (profileOpen.value && profileRef.value && !profileRef.value.contains(event.target as Node)) {
    profileOpen.value = false
  }
}

onMounted(() => document.addEventListener('pointerdown', onClickOutside))
onBeforeUnmount(() => document.removeEventListener('pointerdown', onClickOutside))

function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('refreshToken')
  localStorage.removeItem('authUser')
  router.push('/login')
}
</script>

<template>
  <header class="h-[72px] shrink-0 flex items-center gap-3.5 px-6 bg-lm-surface border-b-2 border-lm-line relative z-10">

    <!-- Logo -->
    <div class="flex items-center -gap-1">
      <img src="@/assets/Logo.png" alt="LifeOS" class="h-24" />
      <span class="font-display text-[20px] font-bold tracking-tight text-lm-ink">LifeOS</span>
    </div>

    <!-- Tabs -->
    <nav class="flex gap-1 ml-[18px] p-1 bg-lm-bg-soft border-2 border-lm-line rounded-full">
      <button
        v-for="tab in tabs"
        :key="tab.id"
        @click="navigate(tab.id)"
        :class="[
          'flex items-center gap-1.5 px-3.5 py-1.5 text-sm font-semibold rounded-full transition-all duration-200',
          active === tab.id
            ? 'bg-lm-ink text-lm-bg'
            : 'bg-transparent text-lm-ink hover:bg-lm-line-soft'
        ]"
      >
        <LmIcon :name="tab.icon" :size="16" />
        {{ tab.label }}
      </button>
    </nav>

    <div class="flex-1" />

    <!-- Level chip -->
    <div class="flex items-center gap-2 pl-1.5 pr-3 py-[5px] border-2 border-lm-line rounded-full bg-lm-surface shadow-stamp-sm">
      <div class="w-7 h-7 rounded-full bg-lm-yellow border-2 border-lm-line flex items-center justify-center font-display font-bold text-xs text-lm-ink shrink-0">
        {{ level }}
      </div>
      <div class="flex flex-col gap-0.5">
        <span class="font-mono text-[9px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3 leading-none">LEVEL</span>
        <div class="w-16 h-1.5 bg-lm-bg-soft rounded-full overflow-hidden border border-lm-line">
          <div class="h-full bg-lm-yellow transition-all duration-200" :style="{ width: `${expProgressPercent}%` }" />
        </div>
      </div>
    </div>

    <!-- Streak chip -->
    <div class="flex items-center gap-2 px-3.5 py-[5px] border-2 border-lm-line rounded-full bg-lm-rust-soft shadow-stamp-sm">
      <span class="text-lm-rust"><LmIcon name="flame" :size="20" :filled="true" /></span>
      <div class="flex flex-col leading-none">
        <span class="font-display font-bold text-[16px] text-lm-ink">{{ streak }}</span>
        <span class="font-mono text-[9px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3">DAYS</span>
      </div>
    </div>

    <!-- Shield chip -->
    <div class="flex items-center gap-1.5 px-3.5 py-[5px] border-2 border-lm-line rounded-full bg-lm-blue-soft shadow-stamp-sm">
      <span class="text-lm-blue"><LmIcon name="shield" :size="20" :filled="true" /></span>
      <span class="font-display font-bold text-[16px] text-lm-ink">×{{ shields }}</span>
    </div>

    <!-- Avatar + dropdown -->
    <div ref="profileRef" class="relative shrink-0">
      <button
        type="button"
        class="w-[42px] h-[42px] rounded-full bg-lm-yellow border-2 border-lm-line shadow-stamp-sm flex items-center justify-center font-display font-bold text-sm text-lm-ink cursor-pointer transition-all duration-200 hover:-translate-y-px hover:shadow-stamp-md"
        :aria-expanded="profileOpen"
        aria-haspopup="menu"
        @click="profileOpen = !profileOpen"
      >
        {{ avatarInitials }}
      </button>

      <Transition
        enter-active-class="transition-all duration-150 ease-out"
        enter-from-class="opacity-0 -translate-y-1 scale-95"
        enter-to-class="opacity-100 translate-y-0 scale-100"
        leave-active-class="transition-all duration-100 ease-in"
        leave-from-class="opacity-100 translate-y-0 scale-100"
        leave-to-class="opacity-0 -translate-y-1 scale-95"
      >
        <div
          v-if="profileOpen"
          class="absolute right-0 top-[52px] z-50 w-[220px] overflow-hidden rounded-[14px] border-2 border-lm-line bg-lm-surface shadow-stamp-md"
          role="menu"
        >
          <div class="border-b-2 border-lm-line-soft bg-lm-bg-soft px-4 py-3">
            <p class="font-display text-[13px] font-bold text-lm-ink truncate">{{ displayName }}</p>
            <p v-if="authUser?.email" class="mt-0.5 font-mono text-[10px] text-lm-ink-3 truncate">{{ authUser.email }}</p>
          </div>
          <button
            type="button"
            class="flex w-full cursor-pointer items-center gap-2.5 px-4 py-3 text-left text-[13px] font-semibold text-lm-rust transition-colors duration-150 hover:bg-lm-rust-soft"
            role="menuitem"
            @click="logout"
          >
            <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
              <polyline points="16 17 21 12 16 7" />
              <line x1="21" y1="12" x2="9" y2="12" />
            </svg>
            Log out
          </button>
        </div>
      </Transition>
    </div>
  </header>
</template>
