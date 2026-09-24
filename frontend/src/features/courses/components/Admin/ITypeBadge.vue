<script setup lang="ts">
import { computed } from 'vue'
import type { InteractionType } from '@/features/courses/services/adminCourses'

const props = defineProps<{
  type?: InteractionType | string | null
}>()

const info = computed(() => {
  if (props.type === 'QUIZ') return { label: 'Quiz', cls: 'bg-lx-macaw/12 text-lx-macaw-dark' }
  if (props.type === 'GRAPH_2D') return { label: 'Graph', cls: 'bg-lx-beetle/15 text-lx-beetle-dark' }
  if (props.type === 'FORMULA_EXPLORER') return { label: 'Formula', cls: 'bg-lx-beetle/15 text-lx-beetle-dark' }
  if (props.type === 'VISUAL_LAYER') return { label: 'Visual', cls: 'bg-lx-feather/12 text-lx-feather-dark' }
  if (props.type === 'LOGIC_FLOW') return { label: 'Logic', cls: 'bg-lx-fox/12 text-lx-fox-dark' }
  if (props.type === 'OTHER') return { label: 'Other', cls: 'bg-lx-surface-soft text-lx-ink-soft' }
  return { label: '', cls: '' }
})
</script>

<template>
  <span
    v-if="type && type !== 'NONE'"
    :class="['shrink-0 rounded-full px-2 py-[2px] text-[11px] font-semibold', info.cls]"
  >
    {{ info.label }}
  </span>
</template>
