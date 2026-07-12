<script setup lang="ts">
import LmIcon from '@/features/learning/components/LmIcon.vue'
import type { AssessmentQuestionResultDto } from '../services/assessment'

defineProps<{
  index: number
  question: AssessmentQuestionResultDto
}>()
</script>

<template>
  <div class="bg-lm-surface border-2 border-lm-line rounded-[18px] shadow-stamp-md p-6">
    <div class="flex items-start gap-3 mb-4">
      <span
        :class="[
          'shrink-0 flex items-center justify-center w-7 h-7 rounded-full border-2 border-lm-line',
          question.correct ? 'bg-lm-green text-lm-bg' : 'bg-lm-red text-lm-bg'
        ]"
      >
        <LmIcon :name="question.correct ? 'check' : 'close'" :size="14" />
      </span>
      <p class="font-display text-[19px] font-bold text-lm-ink leading-snug m-0">{{ question.questionText }}</p>
    </div>

    <div class="flex flex-col gap-2.5 pl-10">
      <div
        v-for="option in question.options"
        :key="option.id"
        :class="[
          'flex items-center gap-3 px-4 py-3 border-2 rounded-[14px]',
          option.correct
            ? 'border-lm-green bg-lm-green-soft'
            : option.selected
              ? 'border-lm-red bg-lm-red-soft'
              : 'border-lm-line-soft bg-lm-bg-soft'
        ]"
      >
        <span
          :class="[
            'shrink-0 w-5 h-5 rounded-full border-2 flex items-center justify-center',
            option.correct ? 'border-lm-green bg-lm-green text-lm-bg' : option.selected ? 'border-lm-red bg-lm-red text-lm-bg' : 'border-lm-ink-3'
          ]"
        >
          <LmIcon v-if="option.correct" name="check" :size="11" />
          <LmIcon v-else-if="option.selected" name="close" :size="11" />
        </span>
        <span class="text-[14px] text-lm-ink">{{ option.optionText }}</span>
        <span v-if="option.selected && !option.correct" class="ml-auto font-mono text-[10px] font-semibold uppercase text-lm-red">your answer</span>
        <span v-else-if="option.correct" class="ml-auto font-mono text-[10px] font-semibold uppercase text-lm-green">correct answer</span>
      </div>
    </div>
  </div>
</template>
