<script setup lang="ts">
import { RouterLink, useRouter } from 'vue-router'

defineProps<{
  activeItem?: 'courses' | 'dashboard' | 'help'
}>()

const router = useRouter()

function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('refreshToken')
  localStorage.removeItem('authUser')
  router.push('/login')
}
</script>

<template>
  <aside class="relative flex w-[220px] shrink-0 flex-col overflow-hidden bg-lm-ink" aria-label="Main navigation">
    <!-- Subtle dot-grid texture -->
    <div class="pointer-events-none absolute inset-0 bg-chalk-dots" />

    <!-- Logo -->
    <div class="relative z-10 flex h-20 shrink-0 items-center gap-3 px-5">
      <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-[12px] bg-lm-yellow border-2 border-lm-bg shadow-stamp-sm font-math italic font-bold text-[22px] text-lm-ink">
        π
      </div>
      <span class="font-display text-[20px] font-bold tracking-tight text-lm-bg">LifeOS</span>
    </div>

    <!-- Nav -->
    <nav class="relative z-10 flex flex-1 flex-col gap-6 overflow-y-auto px-4 py-2">
      <div>
        <p class="mb-2 px-2.5 font-mono text-[9px] font-bold uppercase tracking-[0.18em] text-lm-bg/35">Main Menu</p>
        <ul class="space-y-1">
          <li>
            <RouterLink
              to="/teacher/courses"
              class="relative flex items-center gap-2.5 rounded-xl px-3 py-2.5 text-[13px] font-semibold transition-all duration-200"
              :class="activeItem === 'courses'
                ? 'bg-lm-yellow text-lm-ink shadow-stamp-sm border-2 border-lm-bg/30'
                : 'text-lm-bg/50 hover:bg-lm-bg/10 hover:text-lm-bg/80'"
            >
              <svg class="h-[15px] w-[15px] shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" />
                <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" />
              </svg>
              Courses
            </RouterLink>
          </li>
        </ul>
      </div>

      <div>
        <p class="mb-2 px-2.5 font-mono text-[9px] font-bold uppercase tracking-[0.18em] text-lm-bg/35">Systems</p>
        <ul class="space-y-1">
          <li>
            <a
              href="#"
              class="flex items-center gap-2.5 rounded-xl px-3 py-2.5 text-[13px] font-semibold transition-all duration-200"
              :class="activeItem === 'help'
                ? 'bg-lm-yellow text-lm-ink shadow-stamp-sm border-2 border-lm-bg/30'
                : 'text-lm-bg/50 hover:bg-lm-bg/10 hover:text-lm-bg/80'"
              @click.prevent
            >
              <svg class="h-[15px] w-[15px] shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10" />
                <path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3" />
                <line x1="12" y1="17" x2="12.01" y2="17" />
              </svg>
              Help
            </a>
          </li>
        </ul>
      </div>
    </nav>

    <!-- Teacher user card -->
    <div class="relative z-10 shrink-0 px-4 py-5">
      <div class="flex items-center gap-2.5 rounded-xl border-2 border-lm-bg/20 bg-lm-bg/10 px-3 py-3">
        <div class="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-lm-blue-soft border-2 border-lm-bg/30 font-display text-[13px] font-bold text-lm-ink">
          TC
        </div>
        <div class="min-w-0 flex-1">
          <p class="font-display truncate text-[13px] font-bold text-lm-bg">Teacher</p>
          <p class="font-mono text-[10px] text-lm-bg/50">Review</p>
        </div>
        <button
          type="button"
          class="flex h-6 w-6 shrink-0 items-center justify-center rounded-md text-lm-bg/30 transition hover:bg-lm-bg/10 hover:text-lm-bg/60"
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
