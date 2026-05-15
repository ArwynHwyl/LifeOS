<script setup lang="ts">
import { computed } from 'vue'
import { getCoverPreset } from '@/features/courses/constants/courseCoverPresets'
import type { CourseStatus } from '@/types/types'

const props = defineProps<{
  title: string
  status: CourseStatus
  moduleCount: number
  lastEdited: string
  createdBy: string
  coverId: string
}>()

const emit = defineEmits<{
  edit: []
}>()

const cover = computed(() => getCoverPreset(props.coverId))
</script>

<template>
  <article
    class="flex flex-col gap-4 rounded-2xl border border-slate-100/80 bg-white p-5 shadow-[0_4px_24px_-4px_rgba(15,23,42,0.08)] sm:flex-row sm:items-center sm:gap-6 sm:p-6"
  >
    <div
      class="flex h-14 w-14 shrink-0 items-center justify-center rounded-xl text-xl font-serif font-semibold"
      :class="[cover.bgClass, cover.textClass]"
      aria-hidden="true"
    >
      {{ cover.symbol }}
    </div>

    <div class="min-w-0 flex-1">
      <div class="flex flex-wrap items-center gap-2">
        <h2 class="text-lg font-semibold tracking-tight text-slate-800">{{ title }}</h2>
        <span
          v-if="status === 'published'"
          class="inline-flex items-center gap-1 rounded-full bg-emerald-50 px-2.5 py-0.5 text-xs font-medium text-emerald-800 ring-1 ring-inset ring-emerald-600/15"
        >
          <svg class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <polyline points="20 6 9 17 4 12" />
          </svg>
          Published
        </span>
        <span
          v-else
          class="inline-flex items-center gap-1 rounded-full bg-slate-100 px-2.5 py-0.5 text-xs font-medium text-slate-600 ring-1 ring-inset ring-slate-400/20"
        >
          Draft
        </span>
      </div>
      <p class="mt-2 text-sm text-slate-500">
        {{ moduleCount }} modules · Last edited {{ lastEdited }} · Created by {{ createdBy }}
      </p>
    </div>

    <div class="flex shrink-0 items-center gap-2 sm:ml-auto">
      <button
        type="button"
        class="inline-flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-700 shadow-sm transition hover:border-slate-300 hover:bg-slate-50"
        @click="emit('edit')"
      >
        <svg class="h-4 w-4 text-slate-500" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
          <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
        </svg>
        Edit
      </button>
      <button
        type="button"
        class="flex h-10 w-10 items-center justify-center rounded-xl border border-red-200/80 bg-red-50 text-red-600 transition hover:border-red-300 hover:bg-red-100"
        aria-label="Delete course"
      >
        <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
          <path d="M3 6h18" />
          <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
          <line x1="10" y1="11" x2="10" y2="17" />
          <line x1="14" y1="11" x2="14" y2="17" />
        </svg>
      </button>
    </div>
  </article>
</template>
