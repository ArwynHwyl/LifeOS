<template>
  <div class="flex flex-col gap-3.5">
    <div>
      <MonoLabel :style="{ marginBottom: '6px' }">Question</MonoLabel>
      <textarea
        v-model="question"
        @input="update"
        rows="2"
        class="w-full box-border resize-y font-display text-[13px] px-3 py-2 border-2 border-lm-line-soft rounded-[8px] bg-lm-bg-soft outline-none text-lm-ink"
        placeholder="What is the quadratic formula?"
      />
    </div>

    <div>
      <div class="flex items-center justify-between mb-2">
        <MonoLabel>Answer options</MonoLabel>
        <button @click="addOption" class="font-display text-[11px] font-bold border-2 border-lm-line rounded-[8px] bg-lm-surface px-2.5 py-1 cursor-pointer text-lm-ink">+ Add option</button>
      </div>

      <div
        v-for="(opt, idx) in options"
        :key="opt.id"
        class="flex items-center gap-2 mb-2"
      >
        <button
          @click="setCorrect(idx)"
          class="w-[22px] h-[22px] rounded-full border-2 border-lm-line-soft bg-lm-surface grid place-items-center cursor-pointer shrink-0"
          :class="{ 'border-lm-green bg-lm-green': opt.correct }"
        >
          <svg v-if="opt.correct" width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
            <path d="M20 6 9 17l-5-5"/>
          </svg>
        </button>
        <input
          v-model="opt.label"
          @input="update"
          class="flex-1 font-display text-[13px] px-3 py-2 border-2 border-lm-line-soft rounded-[8px] bg-lm-bg-soft outline-none text-lm-ink"
          :placeholder="'Option ' + (idx + 1)"
        />
        <button
          @click="removeOption(idx)"
          :disabled="options.length <= 2"
          class="grid place-items-center w-7 h-7 rounded-[6px] border border-lm-line-soft bg-transparent cursor-pointer text-lm-ink-3 shrink-0 disabled:cursor-not-allowed disabled:opacity-35"
        >
          <AdminIcon name="trash" :size="14" />
        </button>
      </div>
    </div>

    <div class="grid grid-cols-2 gap-2.5">
      <div class="flex flex-col">
        <MonoLabel :style="{ marginBottom: '6px' }">Success feedback</MonoLabel>
        <input v-model="feedback.success" @input="update" class="w-full box-border font-display text-[13px] px-3 py-2 border-2 border-lm-line-soft rounded-[8px] bg-lm-bg-soft outline-none text-lm-ink" />
      </div>
      <div class="flex flex-col">
        <MonoLabel :style="{ marginBottom: '6px' }">Failure feedback</MonoLabel>
        <input v-model="feedback.failure" @input="update" class="w-full box-border font-display text-[13px] px-3 py-2 border-2 border-lm-line-soft rounded-[8px] bg-lm-bg-soft outline-none text-lm-ink" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import MonoLabel from '../MonoLabel.vue'
import AdminIcon from '../AdminIcon.vue'

const props = defineProps({
  config: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['change'])

const question = ref(props.config?.question ?? '')
const options = ref(props.config?.options ?? [
  { id: 'a', label: 'Option A', correct: true },
  { id: 'b', label: 'Option B', correct: false },
  { id: 'c', label: 'Option C', correct: false },
])
const feedback = ref(props.config?.feedback ?? { success: 'Correct!', failure: 'Try again.' })

watch(() => props.config, (newVal) => {
  question.value = newVal?.question ?? ''
  options.value = newVal?.options ?? [
    { id: 'a', label: 'Option A', correct: true },
    { id: 'b', label: 'Option B', correct: false },
    { id: 'c', label: 'Option C', correct: false },
  ]
  feedback.value = newVal?.feedback ?? { success: 'Correct!', failure: 'Try again.' }
}, { deep: true })

function update() {
  emit('change', {
    ...props.config,
    type: 'QUIZ',
    question: question.value,
    options: options.value,
    feedback: feedback.value
  })
}

function setCorrect(idx) {
  options.value = options.value.map((o, i) => ({ ...o, correct: i === idx }))
  update()
}

function addOption() {
  options.value.push({
    id: String.fromCharCode(97 + options.value.length),
    label: `Option ${options.value.length + 1}`,
    correct: false
  })
  update()
}

function removeOption(idx) {
  if (options.value.length > 2) {
    options.value = options.value.filter((_, i) => i !== idx)
    update()
  }
}
</script>

