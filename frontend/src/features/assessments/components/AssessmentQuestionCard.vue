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
  <div class="anim-rise bg-white border border-lx-line rounded-[20px] p-6" :style="{ '--i': Math.min(index, 6) }">
    <div class="flex items-start gap-3 mb-4">
      <span class="shrink-0 flex items-center justify-center w-7 h-7 rounded-full bg-lx-surface-soft font-mono text-[12px] font-bold text-lx-ink">
        {{ index }}
      </span>
      <p class="font-display text-[18px] font-extrabold text-lx-ink leading-snug m-0">{{ question.questionText }}</p>
    </div>

    <div class="flex flex-col gap-2.5 pl-10">
      <label
        v-for="option in question.options"
        :key="option.id"
        :class="[
          'relative flex items-center gap-3 px-4 py-3 border-2 rounded-2xl cursor-pointer transition-all duration-150 focus-within:ring-2 focus-within:ring-lx-macaw/40',
          props.modelValue === option.id
            ? 'anim-option-pop border-lx-macaw bg-lx-macaw/10'
            : 'press border-lx-line bg-white hover:border-lx-ink-faint hover:bg-lx-surface-soft'
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
            props.modelValue === option.id ? 'border-lx-macaw' : 'border-lx-ink-faint'
          ]"
        >
          <span v-if="props.modelValue === option.id" class="anim-pop w-2.5 h-2.5 rounded-full bg-lx-macaw" />
        </span>
        <span class="text-[14px] font-semibold text-lx-ink">{{ option.optionText }}</span>
      </label>
    </div>
  </div>
</template>
