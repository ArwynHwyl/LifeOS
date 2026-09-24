<script setup lang="ts">
import { computed } from 'vue'
import type { CourseStatus } from '@/types/types'

const props = defineProps<{
  status?: CourseStatus
}>()

const info = computed(() => {
  if (props.status === 'published') return { label: 'Published', cls: 'bg-lx-feather/12 text-lx-feather-dark', dot: 'bg-lx-feather' }
  if (props.status === 'pending') return { label: 'Pending Review', cls: 'bg-lx-fox/12 text-lx-fox-dark', dot: 'bg-lx-fox' }
  if (props.status === 'revision') return { label: 'Needs Revision', cls: 'bg-red-500/10 text-red-600', dot: 'bg-red-500' }
  return { label: 'Draft', cls: 'bg-lx-surface-soft text-lx-ink-soft', dot: 'bg-lx-ink-faint' }
})
</script>

<template>
  <span :class="['inline-flex shrink-0 items-center gap-1.5 rounded-full px-2.5 py-[3px] text-[12px] font-semibold', info.cls]">
    <span :class="['h-1.5 w-1.5 shrink-0 rounded-full', info.dot]" />
    {{ info.label }}
  </span>
</template>
