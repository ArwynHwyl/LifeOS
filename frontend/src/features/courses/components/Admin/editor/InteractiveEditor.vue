<template>
  <div class="border-2 border-lm-line-soft rounded-[14px] bg-[#fffdf8] overflow-hidden">
    <!-- Header -->
    <div class="flex items-center justify-between p-3 py-3.5 border-b-2 border-lm-line-soft">
      <div class="flex items-center gap-2">
        <h3 class="font-display text-[14px] font-[800] text-lm-ink m-0">Interactive</h3>
        <span v-if="type !== 'NONE'" class="font-mono text-[10px] font-bold px-2 py-[2px] border-2 border-lm-line-soft rounded-full bg-lm-bg-soft text-lm-ink-3 uppercase">{{ type }}</span>
      </div>
    </div>

    <div class="p-3.5">
      <template v-if="mode === 'Design'">
        <!-- Type selector -->
        <div class="flex gap-1.5 flex-wrap mb-4">
          <button
            v-for="it in INTERACTION_TYPES"
            :key="it.type"
            @click="$emit('type-change', it.type)"
            class="flex flex-col items-center gap-[2px] p-[8px_12px] border-2 border-lm-line-soft rounded-[10px] bg-lm-surface cursor-pointer min-w-[64px] transition-all duration-120"
            :class="{ 'border-lm-line bg-lm-yellow shadow-stamp-sm': type === it.type }"
          >
            <span v-if="it.icon" class="font-math italic font-bold text-[14px] leading-none">{{ it.icon }}</span>
            <span class="font-display text-[11px] font-bold text-lm-ink">{{ it.label }}</span>
          </button>
        </div>

        <!-- Type-specific form -->
        <div v-if="type === 'NONE'" class="font-mono text-[11px] text-lm-ink-3 uppercase tracking-[0.06em] m-0">
          No interaction — text lesson only
        </div>
        <div v-else-if="type === 'QUIZ'">
          <QuizEditor :config="config" @change="onConfigChange" />
        </div>
        <!-- <div v-else-if="type === 'GRAPH_2D'">
          <Graph2DEditor :config="config" @change="onConfigChange" />
        </div>
        <div v-else-if="type === 'FORMULA_EXPLORER'">
          <FormulaEditor :config="config" @change="onConfigChange" />
        </div>
        <div v-else-if="type === 'VISUAL_LAYER'">
          <VisualLayerEditor :config="config" @change="onConfigChange" />
        </div> -->
        <div v-else-if="type === 'LOGIC_FLOW'">
          <LogicFlowEditor :config="config" @change="onConfigChange" />
        </div>
      </template>

      <template v-else>
        <div class="flex flex-col gap-2">
          <textarea
            v-model="jsonDraft"
            rows="12"
            class="w-full box-border font-mono text-[11px] px-3 py-2.5 border-2 border-lm-line-soft rounded-[8px] bg-lm-bg-soft outline-none text-lm-ink resize-y"
          />
          <div class="flex gap-2">
            <button @click="applyJson" class="px-3.5 py-[7px] font-display text-[12px] font-bold border-2 border-lm-line rounded-full bg-lm-yellow cursor-pointer text-lm-ink shadow-stamp-sm">Apply</button>
            <button @click="resetJson" class="px-3.5 py-[7px] font-display text-[12px] font-bold border-2 border-lm-line rounded-full bg-lm-surface cursor-pointer text-lm-ink">Reset</button>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import QuizEditor from '../interactives/QuizEditor.vue'
import Graph2DEditor from '../interactives/Graph2DEditor.vue'
import FormulaEditor from '../interactives/FormulaEditor.vue'
import VisualLayerEditor from '../interactives/VisualLayerEditor.vue'
import LogicFlowEditor from '../interactives/LogicFlowEditor.vue'

const props = defineProps({
  type: {
    type: String,
    default: 'NONE'
  },
  config: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['change', 'type-change'])

const INTERACTION_TYPES = [
  { type: 'NONE',             label: 'None',           icon: null    },
  { type: 'QUIZ',             label: 'Quiz',           icon: '?'     },
  // { type: 'GRAPH_2D',         label: 'Graph',          icon: '∿'     },
  // { type: 'FORMULA_EXPLORER', label: 'Formula',        icon: 'f(x)'  },
  // { type: 'VISUAL_LAYER',     label: 'Set / Diagram',  icon: '◯◯'    },
  { type: 'LOGIC_FLOW',       label: 'Logic',          icon: '∧∨'    },
]

const mode = ref('Design')
const jsonDraft = ref(JSON.stringify(props.config, null, 2))

watch(() => props.config, (newVal) => {
  jsonDraft.value = JSON.stringify(newVal, null, 2)
}, { deep: true })

function onConfigChange(newConfig) {
  emit('change', newConfig)
}

function applyJson() {
  try {
    const parsed = JSON.parse(jsonDraft.value)
    emit('change', parsed)
  } catch (e) {
    window.alert('Invalid JSON')
  }
}

function resetJson() {
  jsonDraft.value = JSON.stringify(props.config, null, 2)
}
</script>

