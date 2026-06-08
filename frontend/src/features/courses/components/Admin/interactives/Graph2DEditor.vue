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
        <MonoLabel :style="{ marginBottom: '6px' }">Target X</MonoLabel>
        <input
          type="number"
          v-model.number="targetX"
          @input="update"
          class="font-display text-[13px] px-3 py-[9px] border-2 border-lm-line-soft rounded-[8px] bg-lm-bg-soft outline-none text-lm-ink w-full box-border"
        />
      </div>
      <div>
        <MonoLabel :style="{ marginBottom: '6px' }">Target Y</MonoLabel>
        <input
          type="number"
          v-model.number="targetY"
          @input="update"
          class="font-display text-[13px] px-3 py-[9px] border-2 border-lm-line-soft rounded-[8px] bg-lm-bg-soft outline-none text-lm-ink w-full box-border"
        />
      </div>
    </div>

    <div class="border-2 border-dashed border-lm-line-soft rounded-[10px] py-6 text-center bg-lm-bg-soft text-lm-ink-3 font-mono text-[11px] tracking-wider uppercase">
      Graph canvas renders in the full Vue app (p5.js)
    </div>
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

const expression = ref(props.config?.expression ?? 'y = 2*x + 1')
const targetX = ref(props.config?.targetX ?? 3)
const targetY = ref(props.config?.targetY ?? 7)

watch(() => props.config, (newVal) => {
  expression.value = newVal?.expression ?? 'y = 2*x + 1'
  targetX.value = newVal?.targetX ?? 3
  targetY.value = newVal?.targetY ?? 7
}, { deep: true })

function update() {
  emit('change', {
    ...props.config,
    type: 'GRAPH_2D',
    mode: 'PRACTICE',
    expression: expression.value,
    targetX: targetX.value,
    targetY: targetY.value
  })
}
</script>

