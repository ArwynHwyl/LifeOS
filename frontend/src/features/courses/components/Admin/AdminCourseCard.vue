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
    return { label: 'Published', classes: 'bg-lm-green-soft text-lm-green border-2 border-lm-line shadow-stamp-sm', dot: 'bg-lm-green' }
  }
  if (props.status === 'pending') {
    return { label: 'Pending Review', classes: 'bg-lm-yellow text-lm-ink border-2 border-lm-line shadow-stamp-sm', dot: 'bg-lm-ink' }
  }
  if (props.status === 'revision') {
    return { label: 'Needs Revision', classes: 'bg-lm-red-soft text-lm-red border-2 border-lm-line shadow-stamp-sm', dot: 'bg-lm-red' }
  }
  return { label: 'Draft', classes: 'bg-lm-bg-soft text-lm-ink-3 border-2 border-lm-line-soft', dot: 'bg-lm-ink-3' }
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
    class="group grid cursor-pointer grid-cols-[auto_minmax(0,1fr)_160px_112px_auto] items-center gap-5 rounded-[18px] border-2 border-lm-line bg-lm-surface px-5 py-4 shadow-stamp-sm transition-all duration-200 hover:-translate-y-px hover:shadow-stamp-md"
    @click="emit('open')"
  >
    <div
      class="flex h-11 w-11 items-center justify-center rounded-[12px] border-2 border-lm-line font-display text-[22px] font-bold shadow-stamp-sm"
      :class="[cover.bgClass, cover.textClass]"
    >
      {{ cover.symbol }}
    </div>

    <div class="min-w-0">
      <div class="flex min-w-0 items-center gap-2">
        <h3 class="truncate text-[14px] font-bold text-lm-ink">{{ title }}</h3>
        <span class="inline-flex shrink-0 items-center gap-1 rounded-full px-2 py-0.5 text-[10px] font-bold" :class="statusInfo.classes">
          <span class="h-1.5 w-1.5 rounded-full" :class="statusInfo.dot" />
          {{ statusInfo.label }}
        </span>
      </div>
      <p class="mt-1 truncate font-mono text-[11px] text-lm-ink-3">
        {{ moduleCount }} module{{ moduleCount === 1 ? '' : 's' }} · {{ lastEdited }} · Created by {{ createdBy }}
      </p>
      <p v-if="description" class="mt-1 truncate text-[11px] text-lm-ink-3">{{ description }}</p>
    </div>

    <div class="hidden md:block">
      <div v-if="status !== 'draft'" class="flex items-center justify-between font-mono text-[10px] font-semibold text-lm-ink-3">
        <span>Completion</span>
        <span>{{ completion }}%</span>
      </div>
      <div v-if="status !== 'draft'" class="mt-2 h-1.5 overflow-hidden rounded-full border border-lm-line-soft bg-lm-bg-soft">
        <div class="h-full rounded-full bg-lm-yellow" :style="{ width: `${completion}%` }" />
      </div>
      <p v-else class="text-center font-mono text-[12px] font-semibold text-lm-ink-3">—</p>
    </div>

    <button
      type="button"
      class="inline-flex items-center justify-center gap-1.5 rounded-lg border-2 border-lm-line bg-lm-surface px-3 py-2 text-[12px] font-bold text-lm-ink shadow-stamp-sm transition-all duration-200 hover:-translate-y-px hover:shadow-stamp-md"
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
      class="flex h-9 w-9 items-center justify-center rounded-lg border-2 border-lm-line-soft bg-lm-surface text-lm-ink-3 transition-all duration-200 hover:border-lm-red hover:bg-lm-red-soft hover:text-lm-red"
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
