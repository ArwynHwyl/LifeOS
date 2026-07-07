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

const effectiveMode = computed(() => props.interactionType === 'QUIZ' ? 'PRACTICE' : props.mode)

const eyebrowLabel = computed(() => effectiveMode.value === 'PRACTICE' ? 'Challenge' : 'Explore')

const actionLabel = computed(() => {
  if (props.interactionType === 'QUIZ') return 'CLASSIFY'
  if (props.interactionType === 'GRAPH_2D') return 'GRAPH'
  if (props.interactionType === 'FORMULA_EXPLORER') return 'FORMULA'
  if (props.interactionType === 'VISUAL_LAYER') return 'DIAGRAM'
  if (props.interactionType === 'LOGIC_FLOW') return 'LOGIC'
  return 'ACTIVITY'
})

const feedbackLabel = computed(() => effectiveMode.value === 'PRACTICE'
  ? 'Mastered. You can keep exploring or move to the next lesson.'
  : 'Explored. You can review it again or move to the practice.'
)
</script>

<template>
  <div class="challenge-container">
    <!-- Outer Divider -->
    <div class="challenge-divider">
      <span class="challenge-badge">{{ eyebrowLabel.toUpperCase() }}: {{ actionLabel.toUpperCase() }}</span>
      <div class="challenge-divider-line" />
      <span class="challenge-activity-label">INTERACTIVE ACTIVITY</span>
    </div>

    <!-- Main Challenge Card -->
    <section class="challenge-section" :class="`challenge-section--${status.toLowerCase().replace('_', '-')}`">
      <div class="challenge-section__body">
        <p v-if="objective" class="challenge-section__objective">"{{ objective }}"</p>

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
  </div>
</template>

<style scoped>
.challenge-container {
  width: 100%;
  margin: 1.5rem 0;
}

.challenge-divider {
  display: flex;
  align-items: center;
  margin-bottom: 1.5rem;
}

.challenge-badge {
  display: inline-flex;
  align-items: center;
  border: 2px solid #1a1814;
  border-radius: 8px;
  background: #a63a13;
  padding: 0.35rem 0.85rem;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
  font-weight: 900;
  color: #fffdf8;
  box-shadow: 2px 2px 0 #1a1814;
  letter-spacing: 0.04em;
}

.challenge-divider-line {
  flex: 1;
  height: 1.5px;
  background: #e4ded6;
  margin: 0 1rem;
}

.challenge-activity-label {
  color: #8f887e;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 10px;
  font-weight: 900;
  letter-spacing: 0.06em;
}

.challenge-section {
  display: grid;
  width: 100%;
  min-width: 0;
  max-width: 100%;
  box-sizing: border-box;
  overflow: hidden;
  border: 1.5px solid #1a1814;
  border-radius: 20px;
  background: #fcfaf4;
  color: #1a1814;
}

.challenge-section__body {
  display: grid;
  gap: 1.25rem;
  padding: 2rem 1.75rem;
}

.challenge-section__objective {
  margin: 0;
  color: #1a1814;
  text-align: center;
  font-family: Georgia, 'Times New Roman', serif;
  font-size: 1.15rem;
  font-style: italic;
  font-weight: 600;
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
  padding: 0.5rem 0.75rem;
  border: 1.5px solid #245e3e;
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
  .challenge-divider {
    flex-wrap: wrap;
    gap: 0.5rem;
  }
  .challenge-divider-line {
    display: none;
  }
  .challenge-section {
    border-radius: 14px;
  }
  .challenge-section__body {
    padding: 1.25rem 1rem;
  }
}
</style>
