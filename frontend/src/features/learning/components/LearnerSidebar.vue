<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import LmIcon from './LmIcon.vue'
import LogoMark from '@/components/brand/LogoMark.vue'

withDefaults(defineProps<{
  active?: 'courses' | 'assessments' | 'flashcards' | 'dashboard' | 'levels'
}>(), {
  active: 'courses',
})

const router = useRouter()

const tabs: { id: 'courses' | 'assessments' | 'flashcards' | 'dashboard' | 'levels'; label: string; icon: 'book' | 'check' | 'card' | 'dashboard' | 'trophy' }[] = [
  { id: 'courses',     label: 'Courses',     icon: 'book' },
  { id: 'assessments', label: 'Assessments', icon: 'check' },
  { id: 'flashcards',  label: 'Flashcards',  icon: 'card' },
  { id: 'dashboard',   label: 'Dashboard',   icon: 'dashboard' },
  { id: 'levels',      label: 'Levels',      icon: 'trophy' },
]

function navigate(id: 'courses' | 'assessments' | 'flashcards' | 'dashboard' | 'levels') {
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
  <aside class="w-[224px] shrink-0 h-full flex flex-col gap-6 border-r border-lx-line bg-white px-3.5 py-5">

    <!-- Logo -->
    <div class="flex items-center gap-2.5 px-2">
      <LogoMark :size="34" />
      <span class="font-display text-[17px] font-extrabold tracking-tight text-lx-ink">LifeOS</span>
    </div>

    <!-- Nav -->
    <nav class="flex flex-col gap-1">
      <button
        v-for="tab in tabs"
        :key="tab.id"
        type="button"
        @click="navigate(tab.id)"
        :class="[
          'hover-wiggle press flex items-center gap-2.5 rounded-2xl px-3 py-2.5 text-[13.5px] font-bold transition-colors duration-150',
          active === tab.id
            ? 'bg-lx-feather text-white shadow-[0_4px_0_var(--color-lx-feather-dark)]'
            : 'text-lx-ink-faint hover:bg-lx-surface-soft hover:text-lx-ink-soft'
        ]"
      >
        <span class="wiggle-target inline-flex"><LmIcon :name="tab.icon" :size="18" /></span>
        {{ tab.label }}
      </button>
    </nav>

    <div class="flex-1" />

    <!-- Avatar + dropdown -->
    <div ref="profileRef" class="relative shrink-0 border-t border-lx-line pt-3">
      <button
        type="button"
        class="flex w-full items-center gap-2.5 rounded-2xl px-2 py-2 cursor-pointer transition-colors duration-150 hover:bg-lx-surface-soft"
        :aria-expanded="profileOpen"
        aria-haspopup="menu"
        @click="profileOpen = !profileOpen"
      >
        <div class="w-8 h-8 rounded-full bg-lx-macaw flex items-center justify-center font-display font-extrabold text-[11px] text-white shrink-0">
          {{ avatarInitials }}
        </div>
        <span class="text-[12.5px] font-bold text-lx-ink truncate">{{ displayName }}</span>
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
          class="absolute left-0 bottom-[54px] z-50 w-[212px] overflow-hidden rounded-2xl border border-lx-line bg-white shadow-[0_12px_28px_-12px_rgba(0,0,0,0.25)]"
          role="menu"
        >
          <div class="border-b border-lx-line bg-lx-surface-soft px-4 py-3">
            <p class="font-display text-[13px] font-extrabold text-lx-ink truncate">{{ displayName }}</p>
            <p v-if="authUser?.email" class="mt-0.5 font-mono text-[10px] text-lx-ink-faint truncate">{{ authUser.email }}</p>
          </div>
          <button
            type="button"
            class="flex w-full cursor-pointer items-center gap-2.5 px-4 py-3 text-left text-[13px] font-bold text-red-500 transition-colors duration-150 hover:bg-red-50"
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
  </aside>
</template>
