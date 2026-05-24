<script setup lang="ts">
import { RouterLink } from 'vue-router'

defineProps<{
  activeItem?: 'dashboard' | 'course' | 'help'
}>()
</script>

<template>
  <aside class="relative flex w-[220px] shrink-0 flex-col overflow-hidden bg-[#1d1a49]" aria-label="Main navigation">
    <div class="pointer-events-none absolute -right-16 -top-16 h-56 w-56 rounded-full border border-[#8176ff]/45" />
    <div class="pointer-events-none absolute -bottom-28 -left-20 h-72 w-72 rounded-full border border-[#8176ff]/35" />
    <!-- Logo -->
    <div class="relative z-10 flex h-20 shrink-0 items-center gap-3 px-5">
      <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-[#5b4cfa] shadow-md shadow-[#5b4cfa]/35">
        <svg class="h-3.5 w-3.5 text-white" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <path d="M12 2L2 7l10 5 10-5-10-5z" />
          <path d="M2 17l10 5 10-5" />
          <path d="M2 12l10 5 10-5" />
        </svg>
      </div>
      <span class="font-serif text-[20px] font-bold tracking-tight text-white">LifeOS</span>
    </div>

    <!-- Nav -->
    <nav class="relative z-10 flex flex-1 flex-col gap-8 overflow-y-auto px-4 py-3">
      <div>
        <p class="mb-3 px-2.5 text-[10px] font-bold uppercase tracking-[0.18em] text-white/35">Main Menu</p>
        <ul class="space-y-0.5">
          <li>
            <RouterLink
              to="/courses"
              class="relative flex items-center gap-2.5 rounded-lg px-3 py-2.5 text-[13px] font-bold transition-colors"
              :class="activeItem === 'course'
                ? 'bg-white/[0.13] text-white'
                : 'text-white/40 hover:bg-white/[0.05] hover:text-white/65'"
            >
              <svg class="h-[15px] w-[15px] shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" />
                <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" />
              </svg>
              Courses
            </RouterLink>
          </li>
          <li>
            <a
              href="#"
              class="flex items-center gap-2.5 rounded-lg px-3 py-2.5 text-[13px] font-bold transition-colors"
              :class="activeItem === 'dashboard'
                ? 'bg-white/[0.09] text-white'
                : 'text-white/40 hover:bg-white/[0.05] hover:text-white/65'"
              @click.prevent
            >
              <svg class="h-[15px] w-[15px] shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="3" width="7" height="9" rx="1" />
                <rect x="14" y="3" width="7" height="5" rx="1" />
                <rect x="14" y="12" width="7" height="9" rx="1" />
                <rect x="3" y="16" width="7" height="5" rx="1" />
              </svg>
              Dashboard
            </a>
          </li>
        </ul>
      </div>
      
      <div>
        <p class="mb-3 px-2.5 text-[10px] font-bold uppercase tracking-[0.18em] text-white/35">Setting</p>
        <ul class="space-y-0.5">
          <li>
            <a
              href="#"
              class="flex items-center gap-2.5 rounded-lg px-3 py-2.5 text-[13px] font-bold transition-colors"
              :class="activeItem === 'help'
                ? 'bg-white/[0.09] text-white'
                : 'text-white/40 hover:bg-white/[0.05] hover:text-white/65'"
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

    <!-- User -->
    <div class="relative z-10 shrink-0 px-4 py-5">
      <div class="flex items-center gap-2.5 rounded-xl bg-[#5b4cfa] px-3 py-3 shadow-lg shadow-black/15">
        <div class="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-white/20 text-[13px] font-bold text-white">
          AD
        </div>
        <div class="min-w-0 flex-1">
          <p class="truncate text-[13px] font-bold text-white">Admin</p>
          <p class="truncate text-[10px] font-semibold text-white/60">admin@gmail.com</p>
        </div>
        <button
          type="button"
          class="flex h-6 w-6 shrink-0 items-center justify-center rounded-md text-white/25 transition hover:bg-white/10 hover:text-white/50"
          aria-label="Sign out"
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
