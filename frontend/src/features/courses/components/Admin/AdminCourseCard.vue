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
  submit: []
}>()

const cover = computed(() => getCoverPreset(props.coverId))

const statusInfo = computed(() => {
  if (props.status === 'published') {
    return { label: 'Published', classes: 'bg-lm-green-soft text-lm-green border-2 border-lm-line shadow-stamp-xs', dot: 'bg-lm-green' }
  }
  if (props.status === 'pending') {
    return { label: 'Pending Review', classes: 'bg-lm-yellow text-lm-ink border-2 border-lm-line shadow-stamp-xs', dot: 'bg-lm-ink' }
  }
  if (props.status === 'revision') {
    return { label: 'Needs Revision', classes: 'bg-lm-red-soft text-lm-red border-2 border-lm-line shadow-stamp-xs', dot: 'bg-lm-red' }
  }
  return { label: 'Draft', classes: 'bg-lm-bg-soft text-lm-ink-3 border-2 border-lm-line-soft', dot: 'bg-lm-ink-3' }
})


</script>

<template>
  <article
    class="group grid cursor-pointer grid-cols-[44px_minmax(0,1fr)_auto_auto_auto] items-center gap-4 rounded-[12px] border-2 border-lm-line bg-lm-surface px-4 py-3 shadow-stamp-sm transition-all duration-200 hover:-translate-y-px hover:shadow-stamp-md"
    @click="emit('open')"
  >
    <div
      class="flex h-11 w-11 items-center justify-center rounded-[10px] border-2 border-lm-line font-math italic text-[19px] font-bold shadow-stamp-xs"
      :class="[cover.bgClass, cover.textClass]"
    >
      {{ cover.symbol }}
    </div>

    <div class="min-w-0">
      <div class="flex min-w-0 items-center gap-2">
        <h3 class="truncate font-display text-[18px] font-bold text-lm-ink">{{ title }}</h3>
        <span class="inline-flex h-6 shrink-0 items-center gap-1.5 rounded-full px-2.5 font-mono text-[9px] font-bold uppercase tracking-[0.04em]" :class="statusInfo.classes">
          <span class="h-1.5 w-1.5 rounded-full" :class="statusInfo.dot" />
          {{ statusInfo.label }}
        </span>
      </div>
      <p class="mt-0.5 truncate font-mono text-[11px] text-lm-ink-3">
        {{ moduleCount }} module{{ moduleCount === 1 ? '' : 's' }} · {{ lastEdited }} · Created by {{ createdBy }}
      </p>
      <p v-if="description" class="mt-0.5 truncate text-[12px] text-lm-ink-2">{{ description }}</p>
    </div>

    <div class="hidden md:block">
      <button
        v-if="status === 'draft' || status === 'revision'"
        type="button"
        class="inline-flex h-9 cursor-pointer items-center justify-center gap-2 rounded-lg border-2 border-lm-purple bg-lm-purple-soft px-3.5 text-[12px] font-bold text-lm-purple shadow-stamp-xs transition-all duration-200 hover:-translate-y-px hover:shadow-stamp-sm"
        @click.stop="emit('submit')"
      >
        <svg class="h-3.5 w-3.5 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M5 12h14M13 6l6 6-6 6" /></svg>
        Submit for Review
      </button>
    </div>

    <button
      type="button"
      class="inline-flex h-9 cursor-pointer items-center justify-center gap-1.5 rounded-lg border-2 border-lm-line bg-lm-surface px-3 text-[12px] font-bold text-lm-ink shadow-stamp-xs transition-all duration-200 hover:-translate-y-px hover:shadow-stamp-sm"
      :class="status === 'pending' ? 'invisible pointer-events-none' : ''"
      @click.stop="emit('edit')"
    >
      <svg class="h-3 w-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M12 20h9" />
        <path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4Z" />
      </svg>
      Edit
    </button>

    <button
      type="button"
      class="flex h-9 w-9 cursor-pointer items-center justify-center rounded-lg border-2 border-lm-line-soft bg-lm-surface text-lm-ink-3 transition-all duration-200 hover:border-lm-red hover:bg-lm-red-soft hover:text-lm-red"
      title="Delete"
      @click.stop="emit('delete')"
    >
      <svg class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M3 6h18" /><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
      </svg>
    </button>
  </article>
</template>
