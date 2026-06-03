<script setup lang="ts">
import { computed } from 'vue'
import type { InteractionType } from '@/features/courses/services/adminCourses'
import type { InteractiveProgressStatus } from '@/features/learning/services/learnerCourses'
import type { InteractiveMode } from '@/features/courses/types/interactive'

const props = defineProps<{
  interactionType: InteractionType
  objective: string
  status: InteractiveProgressStatus
  mode?: InteractiveMode
}>()

const typeLabel = computed(() => {
  if (props.interactionType === 'GRAPH_2D') return 'Graph'
  if (props.interactionType === 'FORMULA_EXPLORER') return 'Formula'
  if (props.interactionType === 'VISUAL_LAYER') return 'Set / Diagram'
  if (props.interactionType === 'QUIZ') return 'Quiz'
  return 'Activity'
})

const effectiveMode = computed(() => props.interactionType === 'QUIZ' ? 'PRACTICE' : props.mode)

const statusLabel = computed(() => {
  if (effectiveMode.value !== 'PRACTICE') {
    if (props.status === 'MASTERED') return 'Explored'
    if (props.status === 'TRIED') return 'Explored'
    return 'Not explored'
  }
  if (props.status === 'MASTERED') return 'Mastered'
  if (props.status === 'TRIED') return 'In progress'
  return 'Not started'
})

const eyebrowLabel = computed(() => effectiveMode.value === 'PRACTICE' ? 'Challenge' : 'Explore')

const feedbackLabel = computed(() => effectiveMode.value === 'PRACTICE'
  ? 'Mastered. You can keep exploring or move to the next lesson.'
  : 'Explored. You can review it again or move to the practice.'
)
</script>

<template>
  <section class="challenge-card" :class="`challenge-card--${status.toLowerCase().replace('_', '-')}`">
    <header class="challenge-card__header">
      <div>
        <span class="challenge-card__eyebrow">{{ eyebrowLabel }}</span>
        <h2>{{ typeLabel }}</h2>
      </div>
      <span class="challenge-card__status">{{ statusLabel }}</span>
    </header>
    <p class="challenge-card__objective">{{ objective }}</p>
    <slot />
    <p v-if="status === 'MASTERED'" class="challenge-card__feedback">{{ feedbackLabel }}</p>
  </section>
</template>

<style scoped>
.challenge-card {
  display: grid;
  gap: 0.85rem;
  width: 100%;
  min-width: 0;
  max-width: 100%;
  box-sizing: border-box;
  overflow: hidden;
  border: 2px solid #1a1814;
  border-radius: 10px;
  background: #fffdf8;
  padding: 1rem;
  color: #1a1814;
  box-shadow: 3px 3px 0 #1a1814;
}
.challenge-card--mastered {
  border-color: #2f7d4f;
  box-shadow: 3px 3px 0 #2f7d4f;
}
.challenge-card__header {
  display: flex;
  min-width: 0;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}
.challenge-card__header > div {
  min-width: 0;
}
.challenge-card__eyebrow {
  display: block;
  color: #6b6660;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}
.challenge-card h2 {
  margin: 0.1rem 0 0;
  font-size: 1.05rem;
  font-weight: 900;
}
.challenge-card__status {
  flex: 0 0 auto;
  border: 2px solid #1a1814;
  border-radius: 999px;
  background: #fbf7ef;
  padding: 0.22rem 0.65rem;
  font-size: 12px;
  font-weight: 900;
}
.challenge-card--tried .challenge-card__status {
  background: #ffd333;
}
.challenge-card--mastered .challenge-card__status {
  background: #dff4df;
  color: #245e3e;
}
.challenge-card__objective,
.challenge-card__feedback {
  min-width: 0;
  margin: 0;
  color: #4f4942;
  font-size: 13px;
  font-weight: 750;
  line-height: 1.55;
}
.challenge-card :slotted(*) {
  min-width: 0;
  max-width: 100%;
  box-sizing: border-box;
}
.challenge-card__feedback {
  color: #245e3e;
}
</style>
