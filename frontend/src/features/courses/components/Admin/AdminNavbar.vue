<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import AdminIcon from '@/features/courses/components/Admin/AdminIcon.vue'

const props = defineProps<{
  activeItem?: 'dashboard' | 'course' | 'help'
}>()

const router = useRouter()
const hovering = ref(false)
const focused = ref(false)
const expanded = computed(() => hovering.value || focused.value)

const navItems = [
  { id: 'course', label: 'Courses', icon: 'courses', to: '/admin/courses' },
  { id: 'dashboard', label: 'Dashboard', icon: 'dashboard', to: '/admin/dashboard' },
  { id: 'help', label: 'Help', icon: 'help', to: '/admin/help' },
] as const

const user = computed(() => {
  try {
    const raw = localStorage.getItem('authUser')
    return raw ? JSON.parse(raw) as { name?: string; email?: string } : null
  } catch {
    return null
  }
})

const userName = computed(() => user.value?.name || 'Admin')
const userEmail = computed(() => user.value?.email || 'admin@lifeos.app')
const initials = computed(() => userName.value.split(/\s+/).filter(Boolean).slice(0, 2).map((part) => part[0]?.toUpperCase()).join('') || 'AD')

function handleFocusOut(event: FocusEvent) {
  const nextTarget = event.relatedTarget
  if (nextTarget instanceof Node && event.currentTarget instanceof Node && event.currentTarget.contains(nextTarget)) {
    return
  }
  focused.value = false
}

function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('refreshToken')
  localStorage.removeItem('authUser')
  router.push('/login')
}
</script>

<template>
  <aside
    class="relative flex shrink-0 flex-col overflow-hidden bg-lm-ink transition-[width] duration-[0.22s] ease-[cubic-bezier(0.4,0,0.2,1)]"
    :style="{ width: expanded ? '220px' : '64px' }"
    aria-label="Admin navigation"
    @mouseenter="hovering = true"
    @mouseleave="hovering = false"
    @focusin="focused = true"
    @focusout="handleFocusOut"
  >
    <div class="pointer-events-none absolute inset-0 bg-[radial-gradient(circle,rgba(251,247,239,0.06)_1px,transparent_1px)] bg-[length:12px_12px]" />

    <div
      class="relative z-10 flex h-[72px] shrink-0 items-center overflow-hidden"
      :class="expanded ? '-gap-1 px-5' : 'justify-center px-0'"
    >
      <img
        src="@/assets/Logo.png"
        alt="LifeOS"
        :class="expanded ? 'h-24 w-auto' : 'h-24 w-24 object-contain'"
      />
      <span
        v-if="expanded"
        class="overflow-hidden whitespace-nowrap font-display text-[20px] font-bold text-lm-bg"
      >
        LifeOS
      </span>
      <button
        v-if="expanded"
        type="button"
        class="ml-auto grid h-[26px] w-[26px] shrink-0 cursor-pointer place-items-center rounded-[7px] border border-[rgba(251,247,239,0.15)] bg-transparent text-[rgba(251,247,239,0.45)]"
        title="Collapse sidebar"
        @click="hovering = false; focused = false"
      >
        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <path d="M11 19l-7-7 7-7M18 19l-7-7 7-7" />
        </svg>
      </button>
    </div>

    <nav class="relative z-10 flex-1 overflow-auto px-2 pb-4 pt-2">
      <p
        class="mb-2 mt-1 h-3 overflow-hidden px-2 font-mono text-[9px] font-bold uppercase tracking-[0.18em] text-[rgba(251,247,239,0.35)] transition-opacity duration-150"
        :class="expanded ? 'opacity-100' : 'opacity-0'"
      >
        Main Menu
      </p>
      <ul class="m-0 flex list-none flex-col gap-[2px] p-0">
        <li v-for="item in navItems" :key="item.id">
          <RouterLink :to="item.to" custom v-slot="{ navigate }">
            <button
              type="button"
              :title="expanded ? '' : item.label"
              class="flex w-full cursor-pointer items-center overflow-hidden whitespace-nowrap rounded-[12px] border-2 border-transparent bg-transparent text-left font-display text-[13px] font-semibold text-[rgba(251,247,239,0.5)] shadow-none transition-[background-color,border-color,color,box-shadow,padding,gap] duration-180"
              :class="{ 'font-bold shadow-stamp-sm': props.activeItem === item.id }"
              :style="{
                gap: expanded ? '10px' : '0',
                justifyContent: expanded ? 'flex-start' : 'center',
                padding: expanded ? '10px 12px' : '10px 0',
                background: props.activeItem === item.id ? 'var(--lm-yellow)' : 'transparent',
                borderColor: props.activeItem === item.id ? 'rgba(251,247,239,0.3)' : 'transparent',
                color: props.activeItem === item.id ? 'var(--lm-ink)' : 'rgba(251,247,239,0.5)',
              }"
              @click="navigate"
            >
              <span class="flex shrink-0"><AdminIcon :name="item.icon" :size="16" /></span>
              <span
                class="overflow-hidden text-ellipsis transition-[max-width,opacity,transform] duration-200"
                :class="expanded ? 'max-w-32 translate-x-0 opacity-100' : 'max-w-0 -translate-x-1 opacity-0'"
              >
                {{ item.label }}
              </span>
            </button>
          </RouterLink>
        </li>
      </ul>
    </nav>

    <div class="relative z-10 shrink-0" :style="{ padding: expanded ? '0 12px 16px' : '0 8px 16px' }">
      <template v-if="!expanded">
        <div class="flex flex-col items-center gap-1.5">
          <button
            type="button"
            class="grid h-9 w-9 cursor-pointer place-items-center rounded-full border-2 border-[rgba(251,247,239,0.3)] bg-lm-yellow font-display text-[13px] font-bold text-lm-ink"
            :title="`Sign out ${userName}`"
            @click="logout"
          >
            {{ initials }}
          </button>
        </div>
      </template>

      <div v-else class="flex items-center gap-2.5 rounded-[12px] border-2 border-[rgba(251,247,239,0.2)] bg-[rgba(251,247,239,0.1)] px-3 py-2.5">
        <div class="grid h-9 w-9 shrink-0 place-items-center rounded-full border-2 border-[rgba(251,247,239,0.3)] bg-lm-yellow font-display text-[13px] font-bold text-lm-ink">
          {{ initials }}
        </div>
        <div class="min-w-0 flex-1">
          <p class="m-0 truncate font-display text-[13px] font-bold text-lm-bg">{{ userName }}</p>
          <p class="m-0 truncate font-mono text-[10px] font-semibold text-[rgba(251,247,239,0.5)]">{{ userEmail }}</p>
        </div>
        <button
          type="button"
          class="grid h-6 w-6 shrink-0 cursor-pointer place-items-center rounded-[6px] border-none bg-transparent text-[rgba(251,247,239,0.35)]"
          title="Sign out"
          @click="logout"
        >
          <AdminIcon name="logout" :size="14" />
        </button>
      </div>
    </div>
  </aside>
</template>
