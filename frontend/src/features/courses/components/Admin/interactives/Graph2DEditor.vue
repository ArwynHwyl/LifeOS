<template>
  <div class="flex flex-col gap-3">
    <div>
      <MonoLabel :style="{ marginBottom: '6px' }">Expression</MonoLabel>
      <input
        v-model="expression"
        @input="update"
        class="font-display text-[13px] px-3 py-[9px] border-2 border-lm-line-soft rounded-[8px] bg-lm-bg-soft outline-none text-lm-ink w-full box-border"
        placeholder="e.g. y = 2*x + 1"
      />
    </div>

    <div class="grid grid-cols-2 gap-2.5">
      <div>
        <MonoLabel :style="{ marginBottom: '6px' }">X min</MonoLabel>
        <input
          type="number"
          v-model.number="xMin"
          @input="update"
          class="font-display text-[13px] px-3 py-[9px] border-2 border-lm-line-soft rounded-[8px] bg-lm-bg-soft outline-none text-lm-ink w-full box-border"
        />
      </div>
      <div>
        <MonoLabel :style="{ marginBottom: '6px' }">X max</MonoLabel>
        <input
          type="number"
          v-model.number="xMax"
          @input="update"
          class="font-display text-[13px] px-3 py-[9px] border-2 border-lm-line-soft rounded-[8px] bg-lm-bg-soft outline-none text-lm-ink w-full box-border"
        />
      </div>
      <div>
        <MonoLabel :style="{ marginBottom: '6px' }">Y min</MonoLabel>
        <input
          type="number"
          v-model.number="yMin"
          @input="update"
          class="font-display text-[13px] px-3 py-[9px] border-2 border-lm-line-soft rounded-[8px] bg-lm-bg-soft outline-none text-lm-ink w-full box-border"
        />
      </div>
      <div>
        <MonoLabel :style="{ marginBottom: '6px' }">Y max</MonoLabel>
        <input
          type="number"
          v-model.number="yMax"
          @input="update"
          class="font-display text-[13px] px-3 py-[9px] border-2 border-lm-line-soft rounded-[8px] bg-lm-bg-soft outline-none text-lm-ink w-full box-border"
        />
      </div>
    </div>

    <div>
      <MonoLabel :style="{ marginBottom: '6px' }">Samples</MonoLabel>
      <input
        type="number"
        min="20"
        max="500"
        v-model.number="sampleCount"
        @input="update"
        class="font-display text-[13px] px-3 py-[9px] border-2 border-lm-line-soft rounded-[8px] bg-lm-bg-soft outline-none text-lm-ink w-full box-border"
      />
    </div>

    <p class="m-0 font-mono text-[10px] leading-relaxed text-lm-ink-3 uppercase tracking-wider">
      Graph activities use Visualization mode. Learners explore the function and its controls without answer grading.
    </p>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import MonoLabel from '../MonoLabel.vue'

const props = defineProps({
  config: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['change'])

const expression = ref(props.config?.expression ?? '2 * x + 1')
const xMin = ref(props.config?.xMin ?? -5)
const xMax = ref(props.config?.xMax ?? 5)
const yMin = ref(props.config?.yMin ?? -10)
const yMax = ref(props.config?.yMax ?? 10)
const sampleCount = ref(props.config?.sampleCount ?? 100)

watch(() => props.config, (newVal) => {
  expression.value = newVal?.expression ?? '2 * x + 1'
  xMin.value = newVal?.xMin ?? -5
  xMax.value = newVal?.xMax ?? 5
  yMin.value = newVal?.yMin ?? -10
  yMax.value = newVal?.yMax ?? 10
  sampleCount.value = newVal?.sampleCount ?? 100
}, { deep: true })

function update() {
  emit('change', {
    ...props.config,
    type: 'GRAPH_2D',
    mode: 'VISUALIZATION',
    expression: expression.value,
    xMin: xMin.value,
    xMax: xMax.value,
    yMin: yMin.value,
    yMax: yMax.value,
    sampleCount: sampleCount.value
  })
}
</script>
