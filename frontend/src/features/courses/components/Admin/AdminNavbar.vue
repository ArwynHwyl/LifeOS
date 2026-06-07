<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'

defineProps<{
  activeItem?: 'dashboard' | 'course' | 'help'
}>()

const router = useRouter()
const sidebarHovering = ref(sessionStorage.getItem('adminSidebarHovering') === 'true')
const expanded = computed(() => sidebarHovering.value)

function setSidebarHovering(value: boolean) {
  sidebarHovering.value = value
  sessionStorage.setItem('adminSidebarHovering', String(value))
}

function handleFocusOut(event: FocusEvent) {
  const nextTarget = event.relatedTarget
  if (nextTarget instanceof Node && event.currentTarget instanceof Node && event.currentTarget.contains(nextTarget)) {
    return
  }
  setSidebarHovering(false)
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
    class="relative flex shrink-0 flex-col overflow-visible bg-lm-ink transition-[width] duration-500 ease-[cubic-bezier(0.16,1,0.3,1)]"
    :style="{ width: expanded ? '250px' : '72px' }"
    aria-label="Main navigation"
    @mouseenter="setSidebarHovering(true)"
    @mouseleave="setSidebarHovering(false)"
    @focusin="setSidebarHovering(true)"
    @focusout="handleFocusOut"
  >
    <!-- Dot-grid texture -->
    <div class="pointer-events-none absolute inset-0 bg-chalk-dots" />

    <!-- Logo row -->
    <div
      class="relative z-10 flex h-20 shrink-0 items-center overflow-visible"
      :class="expanded ? 'gap-3 px-5' : 'justify-center px-3'"
    >
      <div
        class="flex shrink-0 items-center justify-center rounded-[14px] border-2 border-lm-bg/30 bg-lm-yellow font-math italic font-bold text-lm-ink transition-[width,height,font-size,box-shadow] duration-500 ease-[cubic-bezier(0.16,1,0.3,1)]"
        :class="expanded ? 'h-12 w-12 text-[24px] shadow-stamp-sm' : 'h-10 w-10 text-[20px] shadow-stamp-xs'"
      >
        π
      </div>

      <span
        v-if="expanded"
        class="whitespace-nowrap font-display text-[26px] font-extrabold tracking-tight text-lm-bg transition-[opacity,transform] duration-200"
      >
        LifeOS
      </span>
    </div>

    <!-- Nav -->
    <nav
      class="relative z-10 flex flex-1 flex-col overflow-y-auto overflow-x-visible"
      :class="expanded ? 'px-3 py-4 gap-6' : 'px-3 py-2 gap-1'"
    >
      <!-- Main Menu section -->
      <div>
        <p
          class="overflow-hidden px-2.5 font-mono text-[12px] font-bold uppercase tracking-[0.22em] text-lm-bg/35 transition-[max-height,opacity,margin] duration-500 ease-[cubic-bezier(0.16,1,0.3,1)]"
          :class="expanded ? 'mb-3 max-h-5 opacity-100 delay-100' : 'mb-0 max-h-0 opacity-0'"
        >
          Main Menu
        </p>
        <ul class="space-y-1">
          <li>
            <RouterLink
              to="/admin/courses"
              :title="expanded ? '' : 'Courses'"
              class="relative flex items-center rounded-xl text-[16px] font-bold transition-[background-color,color,border-color,box-shadow,padding,gap] duration-500 ease-[cubic-bezier(0.16,1,0.3,1)]"
              :class="[
                expanded ? 'gap-3 px-5 py-3.5' : 'justify-center py-2.5 px-0',
                activeItem === 'course'
                  ? expanded
                    ? 'border-2 border-lm-bg/30 bg-lm-yellow text-lm-ink shadow-stamp-sm'
                    : 'border-2 border-lm-bg/30 bg-lm-yellow text-lm-ink shadow-stamp-xs'
                  : 'text-lm-bg/50 hover:bg-lm-bg/10 hover:text-lm-bg/80'
              ]"
            >
              <svg class="h-5 w-5 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" />
                <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" />
              </svg>
              <span
                class="overflow-hidden whitespace-nowrap transition-[max-width,opacity,transform] duration-500 ease-[cubic-bezier(0.16,1,0.3,1)]"
                :class="expanded ? 'max-w-32 translate-x-0 opacity-100 delay-150' : 'max-w-0 -translate-x-1 opacity-0'"
              >
                Courses
              </span>
            </RouterLink>
          </li>
          <li>
            <a
              href="#"
              :title="expanded ? '' : 'Dashboard'"
              class="flex items-center rounded-xl text-[16px] font-bold transition-[background-color,color,border-color,box-shadow,padding,gap] duration-500 ease-[cubic-bezier(0.16,1,0.3,1)]"
              :class="[
                expanded ? 'gap-3 px-5 py-3.5' : 'justify-center py-2.5 px-0',
                activeItem === 'dashboard'
                  ? expanded
                    ? 'border-2 border-lm-bg/30 bg-lm-yellow text-lm-ink shadow-stamp-sm'
                    : 'border-2 border-lm-bg/30 bg-lm-yellow text-lm-ink shadow-stamp-xs'
                  : 'text-lm-bg/50 hover:bg-lm-bg/10 hover:text-lm-bg/80'
              ]"
              @click.prevent
            >
              <svg class="h-5 w-5 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="3" width="7" height="9" rx="1" />
                <rect x="14" y="3" width="7" height="5" rx="1" />
                <rect x="14" y="12" width="7" height="9" rx="1" />
                <rect x="3" y="16" width="7" height="5" rx="1" />
              </svg>
              <span
                class="overflow-hidden whitespace-nowrap transition-[max-width,opacity,transform] duration-500 ease-[cubic-bezier(0.16,1,0.3,1)]"
                :class="expanded ? 'max-w-32 translate-x-0 opacity-100 delay-150' : 'max-w-0 -translate-x-1 opacity-0'"
              >
                Dashboard
              </span>
            </a>
          </li>
        </ul>
      </div>

      <!-- Systems section -->
      <div>
        <p
          class="overflow-hidden px-2.5 font-mono text-[12px] font-bold uppercase tracking-[0.22em] text-lm-bg/35 transition-[max-height,opacity,margin] duration-500 ease-[cubic-bezier(0.16,1,0.3,1)]"
          :class="expanded ? 'mb-3 max-h-5 opacity-100 delay-100' : 'mb-0 max-h-0 opacity-0'"
        >
          Systems
        </p>
        <ul class="space-y-1">
          <li>
            <a
              href="#"
              :title="expanded ? '' : 'Help'"
              class="flex items-center rounded-xl text-[16px] font-bold transition-[background-color,color,border-color,box-shadow,padding,gap] duration-500 ease-[cubic-bezier(0.16,1,0.3,1)]"
              :class="[
                expanded ? 'gap-3 px-5 py-3.5' : 'justify-center py-2.5 px-0',
                activeItem === 'help'
                  ? expanded
                    ? 'border-2 border-lm-bg/30 bg-lm-yellow text-lm-ink shadow-stamp-sm'
                    : 'border-2 border-lm-bg/30 bg-lm-yellow text-lm-ink shadow-stamp-xs'
                  : 'text-lm-bg/50 hover:bg-lm-bg/10 hover:text-lm-bg/80'
              ]"
              @click.prevent
            >
              <svg class="h-5 w-5 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10" />
                <path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3" />
                <line x1="12" y1="17" x2="12.01" y2="17" />
              </svg>
              <span
                class="overflow-hidden whitespace-nowrap transition-[max-width,opacity,transform] duration-500 ease-[cubic-bezier(0.16,1,0.3,1)]"
                :class="expanded ? 'max-w-32 translate-x-0 opacity-100 delay-150' : 'max-w-0 -translate-x-1 opacity-0'"
              >
                Help
              </span>
            </a>
          </li>
        </ul>
      </div>
    </nav>

    <!-- User card -->
    <div
      class="relative z-10 shrink-0 py-5"
      :class="expanded ? 'px-4' : 'px-2'"
    >
      <!-- Collapsed state: avatar -->
      <div v-if="!expanded" class="flex flex-col items-center gap-2">
        <button
          type="button"
          class="flex h-9 w-9 shrink-0 cursor-pointer items-center justify-center rounded-full border-2 border-lm-bg/30 bg-lm-yellow font-display text-[13px] font-bold text-lm-ink"
          title="Sign out"
          @click="logout"
        >
          AD
        </button>
      </div>

      <!-- Expanded state: full user card -->
      <div
        v-else
        class="flex items-center gap-3 rounded-xl border-2 border-lm-bg/20 bg-lm-bg/10 px-4 py-3"
      >
        <div class="flex h-9 w-9 shrink-0 items-center justify-center rounded-full border-2 border-lm-bg/30 bg-lm-yellow font-display text-[13px] font-bold text-lm-ink">
          AD
        </div>
        <div class="min-w-0 flex-1">
          <p class="truncate font-display text-[13px] font-bold text-lm-bg">Admin</p>
          <p class="truncate font-mono text-[10px] font-semibold text-lm-bg/50">admin@lifeos...</p>
        </div>
        <button
          type="button"
          class="flex h-6 w-6 shrink-0 cursor-pointer items-center justify-center rounded-md text-lm-bg/30 transition hover:bg-lm-bg/10 hover:text-lm-bg/60"
          aria-label="Sign out"
          @click="logout"
        >
          <svg class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
            <polyline points="16 17 21 12 16 7" />
            <line x1="21" y1="12" x2="9" y2="12" />
          </svg>
        </button>
      </div>
    </div>
  </aside>
</template>
