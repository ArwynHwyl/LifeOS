<script setup lang="ts">
defineProps<{
  activeItem?: 'dashboard' | 'course' | 'help'
}>()
</script>

<template>
  <aside
    class="relative flex w-[260px] shrink-0 flex-col border-r border-white/5 bg-[#1a1a40] text-white"
    aria-label="Main navigation"
  >
    <!-- subtle curved line pattern -->
    <div
      class="pointer-events-none absolute inset-0 overflow-hidden opacity-[0.12]"
      aria-hidden="true"
    >
      <svg class="h-full w-full" preserveAspectRatio="none">
        <defs>
          <linearGradient id="nav-line-g" x1="0%" y1="0%" x2="100%" y2="0%">
            <stop offset="0%" stop-color="#7c9fff" stop-opacity="0" />
            <stop offset="50%" stop-color="#a78bfa" stop-opacity="1" />
            <stop offset="100%" stop-color="#7c9fff" stop-opacity="0" />
          </linearGradient>
        </defs>
        <path
          d="M-20 80 Q 80 40 180 90 T 380 70"
          fill="none"
          stroke="url(#nav-line-g)"
          stroke-width="1"
        />
        <path
          d="M-10 200 Q 100 160 220 210 T 420 180"
          fill="none"
          stroke="url(#nav-line-g)"
          stroke-width="1"
        />
        <path
          d="M0 340 Q 90 300 200 350 T 400 320"
          fill="none"
          stroke="url(#nav-line-g)"
          stroke-width="1"
        />
      </svg>
    </div>

    <div class="relative z-10 flex flex-1 flex-col px-5 pb-6 pt-8">
      <div class="mb-10 flex items-center gap-3 px-1">
        <span class="h-10 w-10 shrink-0 rounded-full bg-[#3b5bfd]" />
        <span class="text-lg font-semibold tracking-tight">LifeOS</span>
      </div>

      <nav class="-ml-2.5 w-[calc(100%+0.625rem)] flex flex-1 flex-col gap-6 text-[14px]">
        <div class="flex flex-col gap-1">
          <h3
            class="m-0 flex items-center gap-2 rounded-xl pl-2 pr-2 py-2.5 text-[15px] font-semibold tracking-tight text-white"
          >
            <span class="flex w-9 shrink-0 justify-center">
              <svg
                class="h-5 w-5 text-[#a78bfa]"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                aria-hidden="true"
              >
                <rect x="3" y="3" width="7" height="7" rx="1" />
                <rect x="14" y="3" width="7" height="7" rx="1" />
                <rect x="3" y="14" width="7" height="7" rx="1" />
                <rect x="14" y="14" width="7" height="7" rx="1" />
              </svg>
            </span>
            Main menu
          </h3>
          <div class="flex items-stretch pl-2 pr-2">
            <div class="relative flex w-9 shrink-0 justify-center" aria-hidden="true">
              <div class="absolute left-1/2 top-0 bottom-0 w-px -translate-x-1/2 bg-white/10" />
            </div>
            <ul class="flex min-w-0 flex-1 flex-col gap-0.5 pl-1">
            <li>
              <a
                href="#"
                class="flex items-center gap-2 rounded-lg py-2 pl-1.5 pr-2 text-[13px] font-medium transition"
                :class="
                  activeItem === 'course'
                    ? 'bg-[#5b4cfa]/30 text-white'
                    : 'text-slate-400 hover:bg-white/5 hover:text-white'
                "
                @click.prevent
              >
                <svg
                  class="h-4 w-4 shrink-0 opacity-90"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                  aria-hidden="true"
                >
                  <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" />
                  <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" />
                  <line x1="8" y1="7" x2="16" y2="7" />
                  <line x1="8" y1="11" x2="14" y2="11" />
                </svg>
                Course
              </a>
            </li>
            <li>
              <a
                href="#"
                class="flex items-center gap-2 rounded-lg py-2 pl-1.5 pr-2 text-[13px] font-medium text-slate-400 transition hover:bg-white/5 hover:text-white"
                :class="{ 'bg-[#5b4cfa]/25 text-white': activeItem === 'dashboard' }"
                @click.prevent
              >
                <svg
                  class="h-4 w-4 shrink-0 opacity-90"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                  aria-hidden="true"
                >
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
        </div>

        <div class="flex flex-col gap-1">
          <h3
            class="m-0 flex items-center gap-2 rounded-xl pl-2 pr-2 py-2.5 text-[15px] font-semibold tracking-tight text-white"
          >
            <span class="flex w-9 shrink-0 justify-center">
              <svg
                class="h-5 w-5 text-[#a78bfa]"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                aria-hidden="true"
              >
                <rect x="4" y="4" width="16" height="16" rx="2" ry="2" />
                <rect x="9" y="9" width="6" height="6" rx="1" ry="1" />
                <path d="M9 1v3m0 14v3M15 1v3m0 14v3M20 9h3m0 4h3M1 9h3m0 4h3" />
              </svg>
            </span>
            Systems
          </h3>
          <div class="flex items-stretch pl-2 pr-2">
            <div class="relative flex w-9 shrink-0 justify-center" aria-hidden="true">
              <div class="absolute left-1/2 top-0 bottom-0 w-px -translate-x-1/2 bg-white/10" />
            </div>
            <ul class="flex min-w-0 flex-1 flex-col gap-0.5 pl-1">
            <li>
              <a
                href="#"
                class="flex items-center gap-2 rounded-lg py-2 pl-1.5 pr-2 text-[13px] font-medium text-slate-400 transition hover:bg-white/5 hover:text-white"
                :class="{ 'bg-[#5b4cfa]/25 text-white': activeItem === 'help' }"
                @click.prevent
              >
                <svg
                  class="h-4 w-4 shrink-0 opacity-90"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                  aria-hidden="true"
                >
                  <circle cx="12" cy="12" r="10" />
                  <path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3" />
                  <line x1="12" y1="17" x2="12.01" y2="17" />
                </svg>
                Help
              </a>
            </li>
          </ul>
          </div>
        </div>
      </nav>

      <div class="mt-auto rounded-2xl bg-[#25255a] p-4 shadow-inner">
        <div class="flex items-center gap-3">
          <div
            class="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-[#5b7cff] to-[#5b4cfa] text-sm font-semibold"
          >
            AD
          </div>
          <div class="min-w-0 flex-1">
            <p class="truncate font-semibold text-white">Admin</p>
            <p class="truncate text-xs text-slate-400">admin@gmail.com</p>
          </div>
        </div>
        <button
          type="button"
          class="mt-4 flex w-full items-center justify-center gap-2 rounded-xl border border-white/10 bg-white/5 py-2.5 text-sm font-medium text-slate-200 transition hover:bg-white/10"
        >
          <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
            <polyline points="16 17 21 12 16 7" />
            <line x1="21" y1="12" x2="9" y2="12" />
          </svg>
          Sign Out
        </button>
      </div>
    </div>
  </aside>
</template>
