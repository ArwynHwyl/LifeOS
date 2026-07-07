<script setup lang="ts">
import { computed } from 'vue'
import type { InteractionType } from '@/features/courses/services/adminCourses'

const props = defineProps<{
  type?: InteractionType | string | null
}>()

const info = computed(() => {
  if (props.type === 'QUIZ') return { label: 'Quiz', bg: 'var(--lm-blue-soft)', color: 'var(--lm-blue)' }
  if (props.type === 'GRAPH_2D') return { label: 'Graph', bg: 'var(--lm-purple-soft)', color: 'var(--lm-purple)' }
  if (props.type === 'FORMULA_EXPLORER') return { label: 'Formula', bg: 'var(--lm-purple-soft)', color: 'var(--lm-purple)' }
  if (props.type === 'VISUAL_LAYER') return { label: 'Visual', bg: 'var(--lm-green-soft)', color: 'var(--lm-green)' }
  if (props.type === 'LOGIC_FLOW') return { label: 'Logic', bg: 'var(--lm-yellow-soft)', color: 'var(--lm-ink)' }
  if (props.type === 'OTHER') return { label: 'Other', bg: 'var(--lm-bg-soft)', color: 'var(--lm-ink-2)' }
  return { label: '', bg: 'transparent', color: 'var(--lm-ink-3)' }
})
</script>

<template>
  <span
    v-if="type && type !== 'NONE'"
    class="shrink-0 rounded-full px-[6px] py-[2px] font-mono text-[9px] font-bold uppercase tracking-[0.04em]"
    :style="{ background: info.bg, color: info.color }"
  >
    {{ info.label }}
  </span>
</template>
