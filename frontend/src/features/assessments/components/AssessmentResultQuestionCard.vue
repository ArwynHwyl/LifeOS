<script setup lang="ts">
import LmIcon from '@/features/learning/components/LmIcon.vue'
import type { AssessmentQuestionResultDto } from '../services/assessment'

defineProps<{
  index: number
  question: AssessmentQuestionResultDto
}>()
</script>

<template>
  <div class="bg-white border border-lx-line rounded-[20px] p-6">
    <div class="flex items-start gap-3 mb-4">
      <span
        :class="[
          'shrink-0 flex items-center justify-center w-7 h-7 rounded-full text-white',
          question.correct ? 'bg-lx-feather' : 'bg-red-500'
        ]"
      >
        <LmIcon :name="question.correct ? 'check' : 'close'" :size="14" />
      </span>
      <p class="font-display text-[18px] font-extrabold text-lx-ink leading-snug m-0">{{ question.questionText }}</p>
    </div>

    <div class="flex flex-col gap-2.5 pl-10">
      <div
        v-for="option in question.options"
        :key="option.id"
        :class="[
          'flex items-center gap-3 px-4 py-3 border-2 rounded-2xl',
          option.correct
            ? 'border-lx-feather bg-lx-feather/10'
            : option.selected
              ? 'border-red-400 bg-red-50'
              : 'border-lx-line bg-white'
        ]"
      >
        <span
          :class="[
            'shrink-0 w-5 h-5 rounded-full border-2 flex items-center justify-center',
            option.correct ? 'border-lx-feather bg-lx-feather text-white' : option.selected ? 'border-red-400 bg-red-400 text-white' : 'border-lx-ink-faint'
          ]"
        >
          <LmIcon v-if="option.correct" name="check" :size="11" />
          <LmIcon v-else-if="option.selected" name="close" :size="11" />
        </span>
        <span class="text-[14px] font-semibold text-lx-ink">{{ option.optionText }}</span>
        <span v-if="option.selected && !option.correct" class="ml-auto font-mono text-[10px] font-bold uppercase text-red-500">your answer</span>
        <span v-else-if="option.correct" class="ml-auto font-mono text-[10px] font-bold uppercase text-lx-feather-dark">correct answer</span>
      </div>
    </div>
  </div>
</template>
