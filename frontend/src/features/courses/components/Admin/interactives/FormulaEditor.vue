<template>
  <div class="flex flex-col gap-3">
    <div>
      <MonoLabel :style="{ marginBottom: '6px' }">Formula</MonoLabel>
      <input
        v-model="formula"
        @input="update"
        class="font-math italic text-[16px] px-2.5 py-1.75 border-2 border-lm-line-soft rounded-[8px] bg-lm-bg-soft outline-none text-lm-ink box-border w-full"
        placeholder="e.g. a * x^2 + b * x + c"
      />
    </div>

    <div>
      <div class="flex items-center justify-between mb-2">
        <MonoLabel>Variables</MonoLabel>
        <button @click="addVar" class="font-display text-[11px] font-bold border-2 border-lm-line rounded-[8px] bg-lm-surface px-2.5 py-1 cursor-pointer text-lm-ink">+ Variable</button>
      </div>

      <div
        v-for="(v, idx) in variables"
        :key="idx"
        class="grid grid-cols-[60px_60px_60px_60px_60px_60px_1fr] gap-1.5 mb-1.5 items-center"
      >
        <div>
          <MonoLabel :style="{ fontSize: '9px', marginBottom: '3px' }">Name</MonoLabel>
          <input v-model="v.name" @input="update" class="w-full box-border font-display text-[13px] px-2.5 py-1.75 border-2 border-lm-line-soft rounded-[8px] bg-lm-bg-soft outline-none text-lm-ink" />
        </div>
        <div>
          <MonoLabel :style="{ fontSize: '9px', marginBottom: '3px' }">Label</MonoLabel>
          <input v-model="v.label" @input="update" class="w-full box-border font-display text-[13px] px-2.5 py-1.75 border-2 border-lm-line-soft rounded-[8px] bg-lm-bg-soft outline-none text-lm-ink" />
        </div>
        <div>
          <MonoLabel :style="{ fontSize: '9px', marginBottom: '3px' }">Min</MonoLabel>
          <input type="number" v-model.number="v.min" @input="update" class="w-full box-border font-display text-[13px] px-2.5 py-1.75 border-2 border-lm-line-soft rounded-[8px] bg-lm-bg-soft outline-none text-lm-ink" />
        </div>
        <div>
          <MonoLabel :style="{ fontSize: '9px', marginBottom: '3px' }">Max</MonoLabel>
          <input type="number" v-model.number="v.max" @input="update" class="w-full box-border font-display text-[13px] px-2.5 py-1.75 border-2 border-lm-line-soft rounded-[8px] bg-lm-bg-soft outline-none text-lm-ink" />
        </div>
        <div>
          <MonoLabel :style="{ fontSize: '9px', marginBottom: '3px' }">Step</MonoLabel>
          <input type="number" v-model.number="v.step" @input="update" class="w-full box-border font-display text-[13px] px-2.5 py-1.75 border-2 border-lm-line-soft rounded-[8px] bg-lm-bg-soft outline-none text-lm-ink" />
        </div>
        <div>
          <MonoLabel :style="{ fontSize: '9px', marginBottom: '3px' }">Initial</MonoLabel>
          <input type="number" v-model.number="v.initial" @input="update" class="w-full box-border font-display text-[13px] px-2.5 py-1.75 border-2 border-lm-line-soft rounded-[8px] bg-lm-bg-soft outline-none text-lm-ink" />
        </div>
        <button
          v-if="variables.length > 1"
          @click="removeVar(idx)"
          class="self-end grid place-items-center w-7 h-7 rounded-[6px] border border-lm-line-soft bg-transparent cursor-pointer text-lm-ink-3"
        >
          <AdminIcon name="trash" :size="13" />
        </button>
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

const formula = ref(props.config?.formula ?? 'a * x + b')
const variables = ref(props.config?.variables ?? [
  { name: 'a', label: 'a', min: -5, max: 5, step: 0.5, initial: 2 },
  { name: 'b', label: 'b', min: -10, max: 10, step: 1, initial: 1 },
])

watch(() => props.config, (newVal) => {
  formula.value = newVal?.formula ?? 'a * x + b'
  variables.value = newVal?.variables ?? [
    { name: 'a', label: 'a', min: -5, max: 5, step: 0.5, initial: 2 },
    { name: 'b', label: 'b', min: -10, max: 10, step: 1, initial: 1 },
  ]
}, { deep: true })

function update() {
  emit('change', {
    ...props.config,
    type: 'FORMULA_EXPLORER',
    mode: 'VISUALIZATION',
    formula: formula.value,
    variables: variables.value
  })
}

function addVar() {
  variables.value.push({
    name: `v${variables.value.length + 1}`,
    label: `v${variables.value.length + 1}`,
    min: 0,
    max: 10,
    step: 1,
    initial: 1
  })
  update()
}

function removeVar(idx) {
  if (variables.value.length > 1) {
    variables.value = variables.value.filter((_, i) => i !== idx)
    update()
  }
}
</script>

