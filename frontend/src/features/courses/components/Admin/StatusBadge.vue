<script setup lang="ts">
import { computed } from 'vue'
import type { CourseStatus } from '@/types/types'

const props = defineProps<{
  status?: CourseStatus
}>()

const statusInfo = computed(() => {
  if (props.status === 'published') {
    return { label: 'Published', bg: 'var(--lm-green-soft)', color: 'var(--lm-green)', border: 'var(--lm-green)', dot: 'var(--lm-green)' }
  }
  if (props.status === 'pending') {
    return { label: 'Pending Review', bg: 'var(--lm-yellow)', color: 'var(--lm-ink)', border: 'var(--lm-line)', dot: 'var(--lm-ink)' }
  }
  if (props.status === 'revision') {
    return { label: 'Needs Revision', bg: 'var(--lm-red-soft)', color: 'var(--lm-red)', border: 'var(--lm-red)', dot: 'var(--lm-red)' }
  }
  return { label: 'Draft', bg: 'var(--lm-bg-soft)', color: 'var(--lm-ink-3)', border: 'var(--lm-line-soft)', dot: 'var(--lm-ink-3)' }
})

const badgeStyle = computed(() => ({
  background: statusInfo.value.bg,
  color: statusInfo.value.color,
  border: `2px solid ${statusInfo.value.border}`,
  boxShadow: props.status === 'draft' ? 'none' : 'var(--shadow-stamp-sm)',
}))
</script>

<template>
  <span
    class="inline-flex shrink-0 items-center gap-1 rounded-full px-2 py-[2px] font-mono text-[10px] font-bold uppercase tracking-[0.04em]"
    :style="badgeStyle"
  >
    <span class="h-[6px] w-[6px] shrink-0 rounded-full" :style="{ background: statusInfo.dot }" />
    {{ statusInfo.label }}
  </span>
</template>
