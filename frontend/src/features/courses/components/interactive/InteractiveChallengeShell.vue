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

const eyebrowLabel = computed(() => effectiveMode.value === 'PRACTICE' ? 'Challenge' : 'Explore')
const headingLabel = computed(() => `${eyebrowLabel.value}: ${typeLabel.value}`)

const feedbackLabel = computed(() => effectiveMode.value === 'PRACTICE'
  ? 'Mastered. You can keep exploring or move to the next lesson.'
  : 'Explored. You can review it again or move to the practice.'
)
</script>

<template>
  <section class="challenge-section" :class="`challenge-section--${status.toLowerCase().replace('_', '-')}`">
    <header class="challenge-section__header">
      <div class="challenge-section__title-row">
        <span class="challenge-section__type">{{ headingLabel }}</span>
      </div>
      <span class="challenge-section__status">Interactive</span>
    </header>

    <div class="challenge-section__body">
      <p class="challenge-section__objective">{{ objective }}</p>

      <div class="challenge-section__content">
        <slot />
      </div>

      <div v-if="status === 'MASTERED'" class="challenge-section__feedback">
        <svg class="challenge-section__check-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
          <polyline points="20 6 9 17 4 12" />
        </svg>
        <p>{{ feedbackLabel }}</p>
      </div>
    </div>
  </section>
</template>

<style scoped>
.challenge-section {
  display: grid;
  width: 100%;
  min-width: 0;
  max-width: 100%;
  box-sizing: border-box;
  overflow: hidden;
  border: 2px solid #1a1814;
  border-radius: 22px;
  background: #f4eee6;
  color: #1a1814;
  box-shadow: 6px 6px 0 #1a1814;
}

.challenge-section__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  min-height: 4.8rem;
  border-bottom: 2px solid #1a1814;
  background: #c94718;
  padding: 1.15rem 1.55rem;
  color: #fffdf8;
}
.challenge-section__title-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  min-width: 0;
}
.challenge-section__type {
  font-family: "Bricolage Grotesque", "Plus Jakarta Sans", system-ui, sans-serif;
  font-size: clamp(1.35rem, 2.6vw, 1.8rem);
  font-weight: 900;
  line-height: 1.1;
}

.challenge-section__status {
  flex-shrink: 0;
  border-radius: 999px;
  background: rgba(255, 253, 248, 0.22);
  padding: 0.35rem 0.8rem;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 10px;
  font-weight: 900;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: #fffdf8;
}
.challenge-section__body {
  display: grid;
  gap: 1.3rem;
  padding: 2.1rem 1.7rem 2rem;
}

.challenge-section__objective {
  margin: 0;
  color: #1a1814;
  text-align: center;
  font-size: 1rem;
  font-weight: 900;
  line-height: 1.45;
}

.challenge-section__content {
  min-width: 0;
  max-width: 100%;
}

.challenge-section__feedback {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  border-radius: 8px;
  background: #edfbf2;
  padding: 0.6rem 0.85rem;
}
.challenge-section__check-icon {
  width: 0.95rem;
  height: 0.95rem;
  color: #245e3e;
  flex-shrink: 0;
}
.challenge-section__feedback p {
  min-width: 0;
  margin: 0;
  color: #245e3e;
  font-size: 12.5px;
  font-weight: 800;
  line-height: 1.5;
}

/* Strip borders from nested interactive preview */
.challenge-section :slotted(*) {
  min-width: 0;
  max-width: 100%;
  box-sizing: border-box;
}
.challenge-section :deep(.interactive-preview) {
  border: none !important;
  background: transparent !important;
  padding: 0 !important;
  box-shadow: none !important;
}

@media (max-width: 720px) {
  .challenge-section {
    border-radius: 16px;
  }
  .challenge-section__header {
    min-height: 0;
    padding: 1rem;
  }
  .challenge-section__body {
    padding: 1.35rem 1rem 1.2rem;
  }
  .challenge-section__status {
    font-size: 9px;
    padding: 0.3rem 0.55rem;
  }
}
</style>
