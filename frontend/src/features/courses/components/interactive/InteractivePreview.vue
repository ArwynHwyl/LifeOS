<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { generatedOverlapRegions } from '@/features/courses/types/interactive'
import type {
  FormulaExplorerConfig,
  Graph2DConfig,
  InteractiveConfig,
  QuizConfig,
  ThreeJsConfig,
  VisualLayerConfig,
} from '@/features/courses/types/interactive'
import { evaluateExpression } from '@/features/courses/utils/expression'

const props = defineProps<{
  config: InteractiveConfig | null
}>()

const selectedAnswer = ref<string | null>(null)
const checked = ref(false)
const formulaValues = ref<Record<string, number>>({})
const graphValues = ref<Record<string, number>>({})
const transformValues = ref<Record<string, number>>({})
const selectedFormulaOptionId = ref<string | null>(null)
const formulaStepIndex = ref(0)
const highlightedZoneId = ref<string | null>(null)
const highlightedOverlapId = ref<string | null>(null)
const visualFeedback = ref('')

watch(
  () => props.config,
  (config) => {
    selectedAnswer.value = null
    checked.value = false
    highlightedZoneId.value = null
    highlightedOverlapId.value = null
    visualFeedback.value = ''
    if (config?.type === 'FORMULA_EXPLORER') {
      formulaValues.value = Object.fromEntries(config.variables.map((variable) => [variable.name, variable.initial]))
      selectedFormulaOptionId.value = config.formulaOptions?.[0]?.id ?? null
      formulaStepIndex.value = 0
    }
    if (config?.type === 'GRAPH_2D') {
      graphValues.value = Object.fromEntries(Object.entries(config.controls ?? {}).map(([name, control]) => [name, control.initial]))
    }
    if (config?.type === 'THREE_JS') {
      transformValues.value = Object.fromEntries(Object.entries(config.controls ?? {}).map(([name, control]) => [name, control?.initial ?? 0]))
    }
  },
  { immediate: true, deep: true },
)

const graphPath = computed(() => {
  if (props.config?.type !== 'GRAPH_2D') return ''
  return makeGraphPath(props.config, graphValues.value)
})

const formulaResult = computed(() => {
  if (props.config?.type !== 'FORMULA_EXPLORER') return null
  try {
    return evaluateExpression(activeFormulaExpression.value, formulaValues.value).toFixed(props.config.precision)
  } catch {
    return 'Invalid formula'
  }
})

const selectedFormulaOption = computed(() => {
  if (props.config?.type !== 'FORMULA_EXPLORER') return null
  return props.config.formulaOptions?.find((option) => option.id === selectedFormulaOptionId.value) ?? props.config.formulaOptions?.[0] ?? null
})

const activeFormulaExpression = computed(() => {
  if (props.config?.type !== 'FORMULA_EXPLORER') return ''
  return selectedFormulaOption.value?.formula ?? props.config.formula
})

const activeFormulaStep = computed(() => {
  const steps = selectedFormulaOption.value?.steps ?? []
  return steps[formulaStepIndex.value] ?? null
})

const formulaStepResult = computed(() => {
  if (props.config?.type !== 'FORMULA_EXPLORER' || !activeFormulaStep.value) return null
  try {
    return evaluateExpression(activeFormulaStep.value.expression, formulaValues.value).toFixed(props.config.precision)
  } catch {
    return 'Invalid step'
  }
})

const selectedOption = computed(() => {
  if (props.config?.type !== 'QUIZ') return null
  return props.config.options.find((option) => option.id === selectedAnswer.value) ?? null
})

const practicePassed = computed(() => {
  const config = props.config
  if (!config || config.mode !== 'PRACTICE') return false
  if (config.type === 'QUIZ') return Boolean(selectedOption.value?.correct)
  if (config.type === 'FORMULA_EXPLORER' && config.successCondition?.kind === 'EXPRESSION_EQUALS') {
    const value = Number(formulaResult.value)
    const tolerance = config.successCondition.tolerance ?? 0
    return Number.isFinite(value) && Math.abs(value - config.successCondition.target) <= tolerance
  }
  if (config.type === 'GRAPH_2D' && config.successCondition?.kind === 'POINT_ON_GRAPH') {
    const { x, y } = config.successCondition.target
    const tolerance = config.successCondition.tolerance ?? 0
    try {
      const value = evaluateExpression(config.expression, { x, ...graphValues.value })
      return Math.abs(value - y) <= tolerance
    } catch {
      return false
    }
  }
  if (config.type === 'THREE_JS' && config.successCondition?.kind === 'TRANSFORM_MATCH') {
    const tolerance = config.successCondition.tolerance ?? 0
    return Object.entries(config.successCondition.target).every(([name, target]) => {
      const value = transformValues.value[name]
      return typeof target === 'number' && typeof value === 'number' && Math.abs(value - target) <= tolerance
    })
  }
  return false
})

const activeFeedback = computed(() => {
  const config = props.config
  if (!config || config.mode !== 'PRACTICE' || !checked.value) return ''
  if (!('feedback' in config)) return ''
  return practicePassed.value ? config.feedback?.success : config.feedback?.failure
})

const targetGraphPoint = computed(() => {
  const config = props.config
  if (config?.type !== 'GRAPH_2D' || config.mode !== 'PRACTICE' || config.successCondition?.kind !== 'POINT_ON_GRAPH') return null
  const target = config.successCondition.target
  return {
    x: ((target.x - config.xMin) / (config.xMax - config.xMin)) * 520,
    y: 260 - ((target.y - config.yMin) / (config.yMax - config.yMin)) * 260,
  }
})

const visualOverlapRegions = computed(() => {
  if (props.config?.type !== 'VISUAL_LAYER') return []
  return generatedOverlapRegions(props.config)
})

function makeGraphPath(config: Graph2DConfig, variables: Record<string, number>) {
  const width = 520
  const height = 260
  const count = Math.max(20, Math.min(500, config.sampleCount))
  const points: Array<[number, number]> = []
  for (let index = 0; index < count; index += 1) {
    const x = config.xMin + ((config.xMax - config.xMin) * index) / (count - 1)
    try {
      const y = evaluateExpression(config.expression, { x, ...variables })
      if (!Number.isFinite(y)) continue
      const px = ((x - config.xMin) / (config.xMax - config.xMin)) * width
      const py = height - ((y - config.yMin) / (config.yMax - config.yMin)) * height
      if (py < -height || py > height * 2) continue
      points.push([px, py])
    } catch {
      return ''
    }
  }
  return points.map(([x, y], index) => `${index === 0 ? 'M' : 'L'} ${x.toFixed(2)} ${y.toFixed(2)}`).join(' ')
}

function axisPosition(min: number, max: number, size: number, reverse = false) {
  if (min > 0 || max < 0) return null
  const value = ((0 - min) / (max - min)) * size
  return reverse ? size - value : value
}

function updateFormulaValue(variable: FormulaExplorerConfig['variables'][number], event: Event) {
  formulaValues.value = {
    ...formulaValues.value,
    [variable.name]: Number((event.target as HTMLInputElement).value),
  }
  checked.value = false
}

function selectFormulaOption(optionId: string) {
  selectedFormulaOptionId.value = optionId
  formulaStepIndex.value = 0
}

function setFormulaStep(index: number) {
  const steps = selectedFormulaOption.value?.steps ?? []
  formulaStepIndex.value = Math.max(0, Math.min(steps.length - 1, index))
}

function updateGraphValue(name: string, event: Event) {
  graphValues.value = {
    ...graphValues.value,
    [name]: Number((event.target as HTMLInputElement).value),
  }
  checked.value = false
}

function updateTransformValue(name: string, event: Event) {
  transformValues.value = {
    ...transformValues.value,
    [name]: Number((event.target as HTMLInputElement).value),
  }
  checked.value = false
}

function chooseAnswer(optionId: string) {
  selectedAnswer.value = optionId
  checked.value = false
}

function runVisualInteraction(triggerId: string) {
  if (props.config?.type !== 'VISUAL_LAYER') return
  const interaction = props.config.interactions.find((item) => item.triggerId === triggerId)
  const targetZoneId = interaction?.effect === 'HIGHLIGHT_ZONE' ? interaction.targetZoneId ?? null : null
  const targetZone = props.config.zones.find((zone) => zone.id === targetZoneId)
  highlightedZoneId.value = targetZoneId
  highlightedOverlapId.value = null
  visualFeedback.value = interaction?.feedback || targetZone?.feedback || ''
}

function runVisualOverlapRegion(regionId: string) {
  const region = visualOverlapRegions.value.find((item) => item.id === regionId)
  if (!region) return
  highlightedZoneId.value = null
  highlightedOverlapId.value = region.id
  visualFeedback.value = region.feedback || region.label
}

function visualObjectStyle(item: { x: number; y: number; width: number; height: number }) {
  return {
    left: `${item.x}px`,
    top: `${item.y}px`,
    width: `${item.width}px`,
    height: `${item.height}px`,
  }
}

function visualZoneLabelStyle(zone: VisualLayerConfig['zones'][number]) {
  return {
    '--zone-label-x': `${zone.labelX ?? 50}%`,
    '--zone-label-y': `${zone.labelY ?? 50}%`,
  }
}

function transformStyle(config: ThreeJsConfig, values: Record<string, number>) {
  const rotationX = values.rotationX ?? 18
  const rotationY = values.rotationY ?? 0
  const rotationZ = values.rotationZ ?? 0
  const scale = values.scale ?? 1
  return {
    backgroundColor: config.color,
    color: config.color,
    transform: `rotateX(${rotationX}deg) rotateY(${rotationY}deg) rotateZ(${rotationZ}deg) scale(${scale})`,
  }
}
</script>

<template>
  <section class="interactive-preview">
    <div v-if="!config" class="interactive-empty">Unsupported or invalid interactive config.</div>

    <div v-else-if="config.type === 'GRAPH_2D'" class="space-y-3">
      <header class="interactive-header">
        <h3>{{ (config as Graph2DConfig).title }}</h3>
        <code>{{ (config as Graph2DConfig).expression }}</code>
      </header>
      <p v-if="config.mode === 'PRACTICE'" class="practice-prompt">{{ config.prompt }}</p>
      <svg class="graph-canvas" viewBox="0 0 520 260" role="img" :aria-label="(config as Graph2DConfig).title">
        <defs>
          <pattern id="graph-grid" width="26" height="26" patternUnits="userSpaceOnUse">
            <path d="M 26 0 L 0 0 0 26" fill="none" stroke="#e4ded6" stroke-width="1" />
          </pattern>
        </defs>
        <rect width="520" height="260" fill="url(#graph-grid)" />
        <line
          v-if="axisPosition((config as Graph2DConfig).xMin, (config as Graph2DConfig).xMax, 520) !== null"
          :x1="axisPosition((config as Graph2DConfig).xMin, (config as Graph2DConfig).xMax, 520) ?? 0"
          y1="0"
          :x2="axisPosition((config as Graph2DConfig).xMin, (config as Graph2DConfig).xMax, 520) ?? 0"
          y2="260"
          stroke="#9e9892"
          stroke-width="1.5"
        />
        <line
          v-if="axisPosition((config as Graph2DConfig).yMin, (config as Graph2DConfig).yMax, 260, true) !== null"
          x1="0"
          :y1="axisPosition((config as Graph2DConfig).yMin, (config as Graph2DConfig).yMax, 260, true) ?? 0"
          x2="520"
          :y2="axisPosition((config as Graph2DConfig).yMin, (config as Graph2DConfig).yMax, 260, true) ?? 0"
          stroke="#9e9892"
          stroke-width="1.5"
        />
        <circle v-if="targetGraphPoint" :cx="targetGraphPoint.x" :cy="targetGraphPoint.y" r="7" fill="#e15f41" stroke="#1a1814" stroke-width="2" />
        <path v-if="graphPath" :d="graphPath" fill="none" stroke="#1a1814" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" />
        <text v-else x="260" y="135" text-anchor="middle" class="graph-error">Invalid expression</text>
      </svg>
      <div v-if="config.mode === 'PRACTICE'" class="space-y-3">
        <label v-for="(control, name) in (config as Graph2DConfig).controls" :key="name" class="formula-slider">
          <span>{{ name }}: <strong>{{ graphValues[String(name)] }}</strong></span>
          <input type="range" :min="control.min" :max="control.max" :step="control.step" :value="graphValues[String(name)]" @input="updateGraphValue(String(name), $event)" />
        </label>
        <button type="button" class="check-button" @click="checked = true">Check</button>
        <p v-if="checked" class="feedback">{{ activeFeedback }}</p>
      </div>
    </div>

    <div v-else-if="config.type === 'FORMULA_EXPLORER'" class="space-y-4">
      <header class="interactive-header">
        <h3>{{ (config as FormulaExplorerConfig).title }}</h3>
        <code>{{ activeFormulaExpression }}</code>
      </header>
      <p v-if="config.mode === 'PRACTICE'" class="practice-prompt">{{ config.prompt }}</p>
      <div v-if="(config as FormulaExplorerConfig).formulaOptions?.length" class="formula-choice-row">
        <button
          v-for="option in (config as FormulaExplorerConfig).formulaOptions"
          :key="option.id"
          type="button"
          class="formula-choice"
          :class="{ 'formula-choice--active': selectedFormulaOption?.id === option.id }"
          @click="selectFormulaOption(option.id)"
        >
          {{ option.label }}
        </button>
      </div>
      <p v-if="selectedFormulaOption?.description" class="practice-prompt">{{ selectedFormulaOption.description }}</p>
      <div class="formula-equation">
        <span>สูตร</span>
        <code>{{ activeFormulaExpression }}</code>
        <span>=</span>
        <strong>{{ formulaResult }}</strong>
      </div>
      <div class="formula-result">{{ formulaResult }}</div>
      <template v-if="config.mode === 'PRACTICE'">
        <label v-for="variable in (config as FormulaExplorerConfig).variables" :key="variable.name" class="formula-slider">
          <span>{{ variable.label }}: <strong>{{ formulaValues[variable.name] }}</strong></span>
          <input
            type="range"
            :min="variable.min"
            :max="variable.max"
            :step="variable.step"
            :value="formulaValues[variable.name]"
            @input="updateFormulaValue(variable, $event)"
          />
        </label>
      </template>
      <div v-if="selectedFormulaOption?.steps?.length" class="step-panel">
        <div class="step-nav">
          <button type="button" class="mini-step-button" :disabled="formulaStepIndex === 0" @click="setFormulaStep(formulaStepIndex - 1)">ก่อนหน้า</button>
          <span>ขั้นที่ {{ formulaStepIndex + 1 }} / {{ selectedFormulaOption.steps.length }}</span>
          <button type="button" class="mini-step-button" :disabled="formulaStepIndex >= selectedFormulaOption.steps.length - 1" @click="setFormulaStep(formulaStepIndex + 1)">ถัดไป</button>
        </div>
        <div v-if="activeFormulaStep" class="step-card">
          <h4>{{ activeFormulaStep.label }}</h4>
          <code>{{ activeFormulaStep.expression }}</code>
          <strong>{{ formulaStepResult }}</strong>
          <p v-if="activeFormulaStep.explanation">{{ activeFormulaStep.explanation }}</p>
        </div>
      </div>
      <template v-if="config.mode === 'PRACTICE'">
        <button type="button" class="check-button" @click="checked = true">Check</button>
        <p v-if="checked" class="feedback">{{ activeFeedback }}</p>
      </template>
    </div>

    <div v-else-if="config.type === 'VISUAL_LAYER'" class="space-y-3">
      <header class="interactive-header">
        <h3>{{ (config as VisualLayerConfig).title }}</h3>
        <span>Visual layer</span>
      </header>
      <div class="visual-layer-stage-wrap">
        <div
          class="visual-layer-stage"
          :style="{
            width: `${(config as VisualLayerConfig).canvas.width}px`,
            height: `${(config as VisualLayerConfig).canvas.height}px`,
          }"
        >
          <p v-if="(config as VisualLayerConfig).canvas.backgroundText" class="visual-layer-bg-text">
            {{ (config as VisualLayerConfig).canvas.backgroundText }}
          </p>
          <button
            v-for="zone in (config as VisualLayerConfig).zones"
            :key="zone.id"
            type="button"
            class="visual-zone"
            :class="[
              `visual-zone--${zone.shape}`,
              { 'visual-zone--active': highlightedZoneId === zone.id },
            ]"
            :style="{ ...visualObjectStyle(zone), ...visualZoneLabelStyle(zone), '--zone-color': zone.color, '--zone-highlight-color': zone.highlightColor ?? zone.color, '--zone-highlight-opacity': zone.highlightOpacity ?? 0.82 }"
            @click="runVisualInteraction(zone.id)"
          >
            <span>{{ zone.label }}</span>
          </button>
          <button
            v-for="element in (config as VisualLayerConfig).elements"
            :key="element.id"
            type="button"
            class="visual-trigger"
            :class="{ 'visual-trigger--hotspot': element.kind === 'hotspot' }"
            :style="visualObjectStyle(element)"
            @click="runVisualInteraction(element.id)"
          >
            {{ element.label }}
          </button>
          <svg
            v-if="visualOverlapRegions.length"
            class="visual-overlap-svg"
            :viewBox="`0 0 ${(config as VisualLayerConfig).canvas.width} ${(config as VisualLayerConfig).canvas.height}`"
            aria-hidden="true"
          >
            <path
              v-for="region in visualOverlapRegions"
              :key="`${region.id}-hit`"
              :d="region.maskPath"
              class="visual-overlap-hit"
              @click.stop="runVisualOverlapRegion(region.id)"
            />
            <path
              v-for="region in visualOverlapRegions"
              v-show="highlightedOverlapId === region.id"
              :key="`${region.id}-active`"
              :d="region.maskPath"
              class="visual-overlap-active"
            />
            <g
              v-for="region in visualOverlapRegions"
              :key="`${region.id}-label`"
              class="visual-overlap-label"
              :class="{ 'visual-overlap-label--active': highlightedOverlapId === region.id }"
              :transform="`translate(${region.center.x}, ${region.center.y})`"
              @click.stop="runVisualOverlapRegion(region.id)"
            >
              <rect x="-26" y="-16" width="52" height="32" rx="8" />
              <text text-anchor="middle" dominant-baseline="central">{{ region.value }}</text>
            </g>
          </svg>
        </div>
      </div>
      <p v-if="visualFeedback" class="feedback">{{ visualFeedback }}</p>
    </div>

    <div v-else-if="config.type === 'QUIZ'" class="space-y-4">
      <header class="interactive-header">
        <h3>{{ (config as QuizConfig).title }}</h3>
      </header>
      <p v-if="config.mode === 'PRACTICE'" class="practice-prompt">{{ config.prompt }}</p>
      <p class="quiz-question">{{ (config as QuizConfig).question }}</p>
      <div class="grid gap-2 sm:grid-cols-2">
        <button
          v-for="option in (config as QuizConfig).options"
          :key="option.id"
          type="button"
          class="quiz-option"
          :class="{
            'quiz-option--selected': selectedAnswer === option.id,
            'quiz-option--correct': checked && option.correct,
            'quiz-option--wrong': checked && selectedAnswer === option.id && !option.correct,
          }"
          @click="chooseAnswer(option.id)"
        >
          {{ option.label }}
        </button>
      </div>
      <button type="button" class="check-button" :disabled="!selectedAnswer" @click="checked = true">Check</button>
      <p v-if="checked" class="feedback">
        {{ config.mode === 'PRACTICE' ? activeFeedback : (selectedOption?.correct ? 'Correct.' : 'Not quite.') }}
        <span v-if="(config as QuizConfig).explanation">{{ (config as QuizConfig).explanation }}</span>
      </p>
    </div>

    <div v-else-if="config.type === 'THREE_JS'" class="space-y-3">
      <header class="interactive-header">
        <h3>{{ (config as ThreeJsConfig).title }}</h3>
        <span>{{ (config as ThreeJsConfig).shape }}</span>
      </header>
      <p v-if="config.mode === 'PRACTICE'" class="practice-prompt">{{ config.prompt }}</p>
      <div class="scene" :class="{ 'scene--practice': config.mode === 'PRACTICE' }" :style="{ '--speed': `${Math.max(0.1, 3.2 - ((config as ThreeJsConfig).rotationSpeed ?? 0.8))}s` }">
        <div v-if="config.mode === 'PRACTICE'" class="target-wrap">
          <span>Target</span>
          <div
            class="scene-shape scene-shape--target"
            :class="`scene-shape--${(config as ThreeJsConfig).shape}`"
            :style="transformStyle(config as ThreeJsConfig, (config as ThreeJsConfig).successCondition?.target ?? {})"
          />
        </div>
        <div class="target-wrap">
          <span v-if="config.mode === 'PRACTICE'">Current</span>
          <div
            class="scene-shape"
            :class="[`scene-shape--${(config as ThreeJsConfig).shape}`, { 'scene-shape--animated': config.mode !== 'PRACTICE' }]"
            :style="config.mode === 'PRACTICE'
              ? transformStyle(config as ThreeJsConfig, transformValues)
              : { backgroundColor: (config as ThreeJsConfig).color, color: (config as ThreeJsConfig).color }"
          />
        </div>
      </div>
      <div v-if="config.mode === 'PRACTICE'" class="space-y-3">
        <label v-for="(control, name) in (config as ThreeJsConfig).controls" :key="name" class="formula-slider">
          <span>{{ name }}: <strong>{{ transformValues[String(name)] }}</strong></span>
          <input type="range" :min="control?.min" :max="control?.max" :step="control?.step" :value="transformValues[String(name)]" @input="updateTransformValue(String(name), $event)" />
        </label>
        <button type="button" class="check-button" @click="checked = true">Check</button>
        <p v-if="checked" class="feedback">{{ activeFeedback }}</p>
      </div>
    </div>
  </section>
</template>

<style scoped>
.interactive-preview {
  border: 2px solid #1a1814;
  border-radius: 10px;
  background: #fbf7ef;
  padding: 1rem;
  color: #1a1814;
}
.interactive-empty {
  color: #8c3322;
  font-size: 13px;
  font-weight: 700;
}
.interactive-header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}
.interactive-header h3 {
  margin: 0;
  font-size: 1rem;
  font-weight: 800;
}
.interactive-header code,
.interactive-header span {
  border-radius: 6px;
  background: #f0ece4;
  padding: 0.2rem 0.45rem;
  font-size: 11px;
  font-weight: 700;
}
.graph-canvas {
  display: block;
  width: 100%;
  min-height: 220px;
  border: 2px solid #d4cec6;
  border-radius: 8px;
  background: #fffdf8;
}
.graph-error {
  fill: #8c3322;
  font-size: 13px;
  font-weight: 700;
}
.formula-result {
  display: inline-flex;
  min-width: 8rem;
  border: 2px solid #1a1814;
  border-radius: 8px;
  background: #ffd333;
  padding: 0.6rem 0.8rem;
  font-size: 1.4rem;
  font-weight: 900;
}
.formula-equation {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.45rem;
  border: 2px solid #d4cec6;
  border-radius: 8px;
  background: #fffdf8;
  padding: 0.65rem 0.75rem;
  color: #4f4942;
  font-size: 13px;
  font-weight: 900;
}
.formula-equation code {
  border-radius: 6px;
  background: #f0ece4;
  padding: 0.25rem 0.45rem;
  color: #1a1814;
  font-size: 12px;
}
.formula-equation strong {
  border-radius: 6px;
  background: #ffd333;
  padding: 0.25rem 0.55rem;
  color: #1a1814;
}
.formula-choice-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}
.formula-choice,
.mini-step-button {
  min-height: 2.2rem;
  border: 2px solid #1a1814;
  border-radius: 8px;
  background: #fffdf8;
  padding: 0 0.75rem;
  color: #1a1814;
  font-size: 12px;
  font-weight: 900;
}
.formula-choice--active,
.formula-choice:hover,
.mini-step-button:hover:not(:disabled) {
  background: #ffd333;
}
.mini-step-button:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}
.visual-layer-stage-wrap {
  max-width: 100%;
  overflow: auto;
  border: 2px solid #d4cec6;
  border-radius: 10px;
  background: #fffdf8;
  padding: 0.75rem;
}
.visual-layer-stage {
  position: relative;
  overflow: hidden;
  background:
    linear-gradient(90deg, rgba(26, 24, 20, 0.055) 1px, transparent 1px),
    linear-gradient(rgba(26, 24, 20, 0.055) 1px, transparent 1px),
    #fbf7ef;
  background-size: 24px 24px;
}
.visual-overlap-svg {
  position: absolute;
  inset: 0;
  z-index: 4;
  width: 100%;
  height: 100%;
  pointer-events: none;
}
.visual-overlap-hit,
.visual-overlap-label {
  pointer-events: auto;
  cursor: pointer;
}
.visual-overlap-hit {
  fill: rgba(255, 211, 51, 0.01);
}
.visual-overlap-active {
  fill: rgba(225, 95, 65, 0.48);
  pointer-events: none;
}
.visual-overlap-label rect {
  fill: #fffdf8;
  stroke: #1a1814;
  stroke-width: 2;
}
.visual-overlap-label text {
  fill: #1a1814;
  font-size: 13px;
  font-weight: 900;
}
.visual-overlap-label--active rect {
  fill: #ffd333;
}
.visual-layer-bg-text {
  position: absolute;
  inset: 1rem;
  margin: 0;
  color: #8f887e;
  font-size: 18px;
  font-weight: 900;
  line-height: 1.35;
  pointer-events: none;
}
.visual-zone,
.visual-trigger {
  position: absolute;
  display: grid;
  place-items: center;
  border: 2px solid #1a1814;
  border-radius: 8px;
  color: #1a1814;
  font-size: 12px;
  font-weight: 900;
}
.visual-zone {
  background: color-mix(in srgb, var(--zone-color) 28%, transparent);
  opacity: 1;
  outline: 0 solid transparent;
}
.visual-zone span {
  pointer-events: none;
}
.visual-zone--circle {
  border-radius: 999px;
}
.visual-zone span {
  position: absolute;
  left: var(--zone-label-x, 50%);
  top: var(--zone-label-y, 50%);
  transform: translate(-50%, -50%);
}
.visual-zone--active {
  background: var(--zone-highlight-color);
  opacity: var(--zone-highlight-opacity);
  box-shadow: 0 0 0 5px rgba(255, 211, 51, 0.38);
}
.visual-trigger {
  background: #ffd333;
}
.visual-trigger--hotspot {
  background: rgba(255, 211, 51, 0.45);
}
.step-panel {
  border: 2px solid #d4cec6;
  border-radius: 8px;
  background: #fffdf8;
  padding: 0.75rem;
}
.step-nav {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 0.6rem;
  color: #6b6660;
  font-size: 12px;
  font-weight: 900;
}
.step-card {
  display: grid;
  gap: 0.5rem;
  margin-top: 0.75rem;
}
.step-card h4,
.step-card p {
  margin: 0;
}
.step-card h4 {
  font-size: 0.95rem;
  font-weight: 900;
}
.step-card code {
  width: fit-content;
  border-radius: 6px;
  background: #f0ece4;
  padding: 0.25rem 0.45rem;
  font-size: 12px;
  font-weight: 800;
}
.step-card strong {
  width: fit-content;
  border: 2px solid #1a1814;
  border-radius: 8px;
  background: #ffd333;
  padding: 0.45rem 0.65rem;
  font-size: 1.1rem;
  font-weight: 900;
}
.step-card p {
  color: #4f4942;
  font-size: 13px;
  font-weight: 700;
}
.formula-slider {
  display: grid;
  gap: 0.35rem;
  font-size: 13px;
  font-weight: 700;
}
.formula-slider input {
  width: 100%;
}
.quiz-question {
  margin: 0;
  font-size: 1rem;
  font-weight: 800;
}
.quiz-option {
  min-height: 3rem;
  border: 2px solid #1a1814;
  border-radius: 8px;
  background: #fffdf8;
  padding: 0.65rem 0.8rem;
  text-align: left;
  font-size: 13px;
  font-weight: 800;
  transition: transform 150ms ease, background 150ms ease;
}
.quiz-option:hover,
.quiz-option--selected {
  background: #ffd333;
  transform: translateY(-1px);
}
.quiz-option--correct {
  background: #cdebc5;
}
.quiz-option--wrong {
  background: #f5c7bd;
}
.check-button {
  height: 2.4rem;
  border: 2px solid #1a1814;
  border-radius: 8px;
  background: #1a1814;
  padding: 0 1rem;
  color: #fbf7ef;
  font-size: 13px;
  font-weight: 800;
}
.check-button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}
.feedback {
  margin: 0;
  border-left: 3px solid #1a1814;
  padding-left: 0.75rem;
  font-size: 13px;
  font-weight: 700;
}
.practice-prompt {
  margin: 0;
  color: #4f4942;
  font-size: 13px;
  font-weight: 700;
}
.scene {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2rem;
  min-height: 260px;
  overflow: hidden;
  border: 2px solid #d4cec6;
  border-radius: 8px;
  background: radial-gradient(circle at 50% 35%, #ffffff 0, #f0ece4 52%, #d4cec6 100%);
  perspective: 700px;
}
.scene--practice {
  flex-wrap: wrap;
}
.target-wrap {
  display: grid;
  min-width: 160px;
  min-height: 190px;
  place-items: center;
  gap: 0.75rem;
  color: #6b6660;
  font-size: 11px;
  font-weight: 900;
  text-transform: uppercase;
}
.scene-shape {
  width: 120px;
  height: 120px;
  border: 2px solid #1a1814;
  box-shadow: 18px 18px 0 rgba(26, 24, 20, 0.16);
  transform-style: preserve-3d;
}
.scene-shape--animated {
  animation: scene-spin var(--speed) linear infinite;
}
.scene-shape--target {
  opacity: 0.55;
}
.scene-shape--sphere {
  border-radius: 999px;
  background-image: radial-gradient(circle at 35% 30%, rgba(255, 255, 255, 0.75), transparent 32%);
}
.scene-shape--pyramid {
  width: 0;
  height: 0;
  border-right: 70px solid transparent;
  border-bottom: 130px solid currentColor;
  border-left: 70px solid transparent;
  background: transparent !important;
  box-shadow: none;
}
@keyframes scene-spin {
  from {
    transform: rotateX(18deg) rotateY(0deg);
  }
  to {
    transform: rotateX(18deg) rotateY(360deg);
  }
}
</style>
