<script setup lang="ts">
import { RouterLink } from 'vue-router'

defineProps<{
  activeItem?: 'dashboard' | 'course' | 'help'
}>()
</script>

<template>
  <aside class="flex w-[220px] shrink-0 flex-col bg-[#1a1a40]" aria-label="Main navigation">
    <!-- Logo -->
    <div class="flex h-14 shrink-0 items-center gap-3 px-5">
      <div class="flex h-7 w-7 shrink-0 items-center justify-center rounded-lg bg-[#3b5bfd] shadow-md shadow-[#3b5bfd]/40">
        <svg class="h-3.5 w-3.5 text-white" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <path d="M12 2L2 7l10 5 10-5-10-5z" />
          <path d="M2 17l10 5 10-5" />
          <path d="M2 12l10 5 10-5" />
        </svg>
      </div>
      <span class="text-[13px] font-semibold tracking-tight text-white/90">LifeOS</span>
    </div>

    <div class="mx-4 border-t border-white/[0.06]" />

    <!-- Nav -->
    <nav class="flex flex-1 flex-col gap-5 overflow-y-auto px-3 py-4">
      <div>
        <p class="mb-1 px-2.5 text-[9px] font-bold uppercase tracking-[0.14em] text-white/20">Main</p>
        <ul class="space-y-0.5">
          <li>
            <RouterLink
              to="/"
              class="relative flex items-center gap-2.5 rounded-lg px-2.5 py-2 text-[12.5px] font-medium transition-colors"
              :class="activeItem === 'course'
                ? 'bg-white/[0.09] text-white before:absolute before:left-0 before:top-1/2 before:h-[18px] before:w-[3px] before:-translate-y-1/2 before:rounded-r-full before:bg-[#5b4cfa] before:content-[\'\']'
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
              class="flex items-center gap-2.5 rounded-lg px-2.5 py-2 text-[12.5px] font-medium transition-colors"
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
        <p class="mb-1 px-2.5 text-[9px] font-bold uppercase tracking-[0.14em] text-white/20">Systems</p>
        <ul class="space-y-0.5">
          <li>
            <a
              href="#"
              class="flex items-center gap-2.5 rounded-lg px-2.5 py-2 text-[12.5px] font-medium transition-colors"
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
    <div class="shrink-0 border-t border-white/[0.06] px-3 py-3">
      <div class="flex items-center gap-2.5 rounded-xl bg-white/[0.05] px-3 py-2.5">
        <div class="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-[#5b7cff] to-[#5b4cfa] text-[10px] font-bold text-white">
          AD
        </div>
        <div class="min-w-0 flex-1">
          <p class="truncate text-[12px] font-semibold text-white/80">Admin</p>
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
