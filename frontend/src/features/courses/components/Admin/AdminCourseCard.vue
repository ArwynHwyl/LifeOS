<script setup lang="ts">
import { computed } from 'vue'
import { getCoverPreset } from '@/features/courses/constants/courseCoverPresets'
import type { CourseStatus } from '@/types/types'

const props = defineProps<{
  title: string
  description: string
  status: CourseStatus
  moduleCount: number
  lastEdited: string
  coverId: string
}>()

const emit = defineEmits<{
  edit: []
  open: []
  delete: []
}>()

const cover = computed(() => getCoverPreset(props.coverId))
</script>

<template>
  <article
    class="group flex cursor-pointer flex-col overflow-hidden rounded-2xl bg-white ring-1 ring-slate-900/[0.06] shadow-sm
           transition-all duration-200 hover:-translate-y-0.5 hover:shadow-xl hover:ring-[#5b4cfa]/25"
    @click="emit('open')"
  >
    <!-- Full-width colored top band -->
    <div
      class="relative flex h-[140px] shrink-0 items-center justify-center overflow-hidden"
      :class="cover.bgClass"
    >
      <!-- Ghost symbol for depth -->
      <span
        class="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 select-none font-serif text-[110px] font-bold leading-none opacity-[0.15]"
        :class="cover.textClass"
      >
        {{ cover.symbol }}
      </span>
      <!-- Main symbol -->
      <span class="relative z-10 font-serif text-[42px] font-bold leading-none" :class="cover.textClass">
        {{ cover.symbol }}
      </span>
    </div>

    <!-- Card body -->
    <div class="flex flex-1 flex-col p-4">
      <!-- Title + status -->
      <div class="flex items-start justify-between gap-2">
        <h3 class="flex-1 text-[13.5px] font-bold leading-snug text-slate-900 transition-colors group-hover:text-[#5b4cfa]">
          {{ title }}
        </h3>
        <span
          v-if="status === 'published'"
          class="shrink-0 inline-flex items-center gap-1 rounded-full bg-emerald-50 px-2 py-0.5 text-[10px] font-semibold text-emerald-700 ring-1 ring-inset ring-emerald-600/20"
        >
          <span class="h-1.5 w-1.5 rounded-full bg-emerald-500" />
          Published
        </span>
        <span
          v-else
          class="shrink-0 inline-flex items-center gap-1 rounded-full bg-amber-50 px-2 py-0.5 text-[10px] font-semibold text-amber-700 ring-1 ring-inset ring-amber-500/20"
        >
          <span class="h-1.5 w-1.5 rounded-full bg-amber-400" />
          Draft
        </span>
      </div>

      <!-- Description -->
      <p class="mt-2 line-clamp-2 flex-1 text-[11.5px] leading-relaxed text-slate-400">
        {{ description }}
      </p>

      <!-- Footer -->
      <div class="mt-3.5 flex items-center justify-between border-t border-slate-100 pt-3">
        <span class="text-[11px] text-slate-400">{{ moduleCount }} modules · {{ lastEdited }}</span>

        <!-- Hover actions -->
        <div
          class="flex items-center gap-0.5 opacity-0 transition-opacity group-hover:opacity-100"
          @click.stop
        >
          <button
            type="button"
            class="flex h-7 w-7 items-center justify-center rounded-lg text-slate-400 transition hover:bg-slate-100 hover:text-slate-600"
            title="Edit"
            @click.stop="emit('edit')"
          >
            <svg class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
              <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
            </svg>
          </button>
          <button
            type="button"
            class="flex h-7 w-7 items-center justify-center rounded-lg text-slate-400 transition hover:bg-red-50 hover:text-red-500"
            title="Delete"
            @click.stop="emit('delete')"
          >
            <svg class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M3 6h18" />
              <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
            </svg>
          </button>
        </div>
      </div>
    </div>
  </article>
</template>
