<script setup lang="ts">
import LogoMark from '@/components/brand/LogoMark.vue'
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
    class="relative flex shrink-0 flex-col overflow-hidden border-r border-lm-line bg-white transition-[width] duration-[0.22s] ease-[cubic-bezier(0.4,0,0.2,1)]"
    :style="{ width: expanded ? '232px' : '68px' }"
    aria-label="Admin navigation"
    @mouseenter="hovering = true"
    @mouseleave="hovering = false"
    @focusin="focused = true"
    @focusout="handleFocusOut"
  >
    <div
      class="flex h-[72px] shrink-0 items-center overflow-hidden"
      :class="expanded ? 'gap-2.5 px-5' : 'justify-center px-0'"
    >
      <LogoMark :size="36" />
      <span v-if="expanded" class="overflow-hidden whitespace-nowrap font-display text-[19px] font-bold tracking-tight text-lm-ink">LifeOS</span>
    </div>

    <nav class="flex-1 overflow-auto px-3 pb-4 pt-2">
      <p
        class="mb-2 mt-1 h-4 overflow-hidden px-2 text-[12px] font-semibold text-lm-ink-3 transition-opacity duration-150"
        :class="expanded ? 'opacity-100' : 'opacity-0'"
      >
        Main Menu
      </p>
      <ul class="m-0 flex list-none flex-col gap-1 p-0">
        <li v-for="item in navItems" :key="item.id">
          <RouterLink :to="item.to" custom v-slot="{ navigate }">
            <button
              type="button"
              :title="expanded ? '' : item.label"
              class="flex w-full cursor-pointer items-center overflow-hidden whitespace-nowrap rounded-2xl text-left text-[14px] font-semibold transition-colors duration-150"
              :class="props.activeItem === item.id
                ? 'bg-lx-feather text-white shadow-[0_4px_0_var(--color-lx-feather-dark)]'
                : 'text-lm-ink-3 hover:bg-lm-bg-soft hover:text-lm-ink-2'"
              :style="{
                gap: expanded ? '10px' : '0',
                justifyContent: expanded ? 'flex-start' : 'center',
                padding: expanded ? '11px 14px' : '11px 0',
              }"
              @click="navigate"
            >
              <span class="flex shrink-0"><AdminIcon :name="item.icon" :size="18" /></span>
              <span
                class="overflow-hidden text-ellipsis transition-[max-width,opacity,transform] duration-200"
                :class="expanded ? 'max-w-36 translate-x-0 opacity-100' : 'max-w-0 -translate-x-1 opacity-0'"
              >
                {{ item.label }}
              </span>
            </button>
          </RouterLink>
        </li>
      </ul>
    </nav>

    <div class="shrink-0 border-t border-lm-line-soft" :style="{ padding: expanded ? '12px' : '12px 8px' }">
      <button
        v-if="!expanded"
        type="button"
        class="mx-auto grid h-10 w-10 cursor-pointer place-items-center rounded-full bg-lx-macaw font-display text-[13px] font-bold text-white"
        :title="`Sign out ${userName}`"
        @click="logout"
      >
        {{ initials }}
      </button>

      <div v-else class="flex items-center gap-2.5 rounded-2xl px-2 py-1.5">
        <div class="grid h-10 w-10 shrink-0 place-items-center rounded-full bg-lx-macaw font-display text-[13px] font-bold text-white">
          {{ initials }}
        </div>
        <div class="min-w-0 flex-1">
          <p class="m-0 truncate text-[13.5px] font-semibold text-lm-ink">{{ userName }}</p>
          <p class="m-0 truncate text-[12px] text-lm-ink-3">{{ userEmail }}</p>
        </div>
        <button
          type="button"
          class="grid h-8 w-8 shrink-0 cursor-pointer place-items-center rounded-lg border-none bg-transparent text-lm-ink-3 transition-colors hover:bg-lm-bg-soft hover:text-lm-ink"
          title="Sign out"
          @click="logout"
        >
          <AdminIcon name="logout" :size="16" />
        </button>
      </div>
    </div>
  </aside>
</template>
