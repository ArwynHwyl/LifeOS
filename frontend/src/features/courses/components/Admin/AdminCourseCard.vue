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
  createdBy: string
}>()

const emit = defineEmits<{
  edit: []
  open: []
  delete: []
}>()

const cover = computed(() => getCoverPreset(props.coverId))

const statusInfo = computed(() => {
  if (props.status === 'published') {
    return { label: 'Published', classes: 'bg-emerald-50 text-emerald-700 ring-emerald-600/20', dot: 'bg-emerald-500' }
  }
  if (props.status === 'pending') {
    return { label: 'Pending Review', classes: 'bg-violet-50 text-violet-700 ring-violet-600/20', dot: 'bg-violet-500' }
  }
  if (props.status === 'revision') {
    return { label: 'Needs Revision', classes: 'bg-red-50 text-red-700 ring-red-600/20', dot: 'bg-red-500' }
  }
  return { label: 'Draft', classes: 'bg-slate-100 text-slate-600 ring-slate-300', dot: 'bg-slate-400' }
})

const completion = computed(() => {
  if (props.status === 'published') return 78
  if (props.status === 'pending') return 46
  if (props.status === 'revision') return 28
  return Math.min(65, Math.max(12, props.moduleCount * 18))
})
</script>

<template>
  <article
    class="group grid cursor-pointer grid-cols-[auto_minmax(0,1fr)_160px_112px_auto] items-center gap-5 rounded-xl border border-white bg-white px-5 py-4 shadow-sm shadow-slate-200/60 ring-1 ring-slate-900/[0.04] transition hover:border-[#c8c2ff] hover:shadow-md"
    :class="status === 'pending' ? 'ring-[#a895ff]/35' : status === 'revision' ? 'ring-red-200' : ''"
    @click="emit('open')"
  >
    <div class="flex h-11 w-11 items-center justify-center rounded-lg font-serif text-[22px] font-bold" :class="[cover.bgClass, cover.textClass]">
      {{ cover.symbol }}
    </div>

    <div class="min-w-0">
      <div class="flex min-w-0 items-center gap-2">
        <h3 class="truncate text-[14px] font-bold text-slate-800 transition group-hover:text-[#5748e8]">{{ title }}</h3>
        <span class="inline-flex shrink-0 items-center gap-1 rounded-full px-2 py-0.5 text-[10px] font-bold ring-1 ring-inset" :class="statusInfo.classes">
          <span class="h-1.5 w-1.5 rounded-full" :class="statusInfo.dot" />
          {{ statusInfo.label }}
        </span>
      </div>
      <p class="mt-1 truncate text-[11px] font-medium text-slate-400">
        {{ moduleCount }} module{{ moduleCount === 1 ? '' : 's' }} · {{ lastEdited }} · Created by {{ createdBy }}
      </p>
      <p v-if="description" class="mt-1 truncate text-[11px] text-slate-400">{{ description }}</p>
    </div>

    <div class="hidden md:block">
      <div v-if="status !== 'draft'" class="flex items-center justify-between text-[10px] font-semibold text-slate-400">
        <span>Completion</span>
        <span>{{ completion }}%</span>
      </div>
      <div v-if="status !== 'draft'" class="mt-2 h-1.5 overflow-hidden rounded-full bg-slate-100">
        <div class="h-full rounded-full bg-[#5b4cfa]" :style="{ width: `${completion}%` }" />
      </div>
      <p v-else class="text-center text-[12px] font-semibold text-slate-300">-</p>
    </div>

    <button
      type="button"
      class="inline-flex items-center justify-center gap-1.5 rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-[12px] font-bold text-slate-600 transition hover:border-[#cbc4ff] hover:bg-white hover:text-[#5748e8]"
      @click.stop="emit('edit')"
    >
      <svg class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M12 20h9" />
        <path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4Z" />
      </svg>
      Edit
    </button>

    <button
      type="button"
      class="flex h-9 w-9 items-center justify-center rounded-lg border border-slate-200 bg-slate-50 text-slate-400 transition hover:border-red-200 hover:bg-red-50 hover:text-red-500"
      title="Delete"
      @click.stop="emit('delete')"
    >
      <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <circle cx="12" cy="12" r="1" />
        <circle cx="19" cy="12" r="1" />
        <circle cx="5" cy="12" r="1" />
      </svg>
    </button>
  </article>
</template>
