<script setup lang="ts">
import type { AssessmentQuestionDto } from '../services/assessment'

const props = defineProps<{
  index: number
  question: AssessmentQuestionDto
  modelValue: number | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: number]
}>()

function select(optionId: number) {
  emit('update:modelValue', optionId)
}
</script>

<template>
  <fieldset class="question-card" :aria-labelledby="`question-label-${question.id}`">
    <div class="flex items-start gap-3 mb-4">
      <span class="shrink-0 flex items-center justify-center w-7 h-7 rounded-full bg-lm-bg-soft border-2 border-lm-line font-mono text-[12px] font-semibold text-lm-ink">
        {{ index }}
      </span>
      <h2 :id="`question-label-${question.id}`" class="font-display text-[19px] font-bold text-lm-ink leading-snug m-0">{{ question.questionText }}</h2>
    </div>

    <div class="flex flex-col gap-2.5 sm:pl-10">
      <label
        v-for="option in question.options"
        :key="option.id"
        :class="[
          'flex items-center gap-3 px-4 py-3 border-2 rounded-[14px] cursor-pointer transition-all duration-200',
          props.modelValue === option.id
            ? 'border-lm-ink bg-lm-yellow-soft'
            : 'border-lm-line-soft bg-lm-bg-soft hover:border-lm-line'
        ]"
      >
        <input
          type="radio"
          class="sr-only"
          :name="`question-${question.id}`"
          :value="option.id"
          :checked="props.modelValue === option.id"
          @change="select(option.id)"
        />
        <span
          :class="[
            'shrink-0 w-4.5 h-4.5 rounded-full border-2 flex items-center justify-center',
            props.modelValue === option.id ? 'border-lm-ink' : 'border-lm-ink-3'
          ]"
        >
          <span v-if="props.modelValue === option.id" class="w-2.5 h-2.5 rounded-full bg-lm-ink" />
        </span>
        <span class="text-[14px] text-lm-ink">{{ option.optionText }}</span>
      </label>
    </div>
  </fieldset>
</template>

<style scoped>
.question-card { min-width: 0; padding: 26px; background: #fffefa; border: 1px solid #dedbd0; border-radius: 16px; scroll-margin-top: 24px; }
.question-card label { border-width: 1px; border-radius: 10px; min-height: 48px; }
.question-card label:has(input:focus-visible) { outline: 3px solid #668255; outline-offset: 3px; }
.question-card label:has(input:checked) { background: #edf2e4; border-color: #667d4e; }
@media(max-width:640px) { .question-card { padding: 20px 16px; } }
</style>
