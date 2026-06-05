<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { generatedOverlapRegions } from '@/features/courses/types/interactive'
import type {
  FormulaExplorerConfig,
  GeneratedOverlapRegion,
  Graph2DConfig,
  InteractiveConfig,
  QuizConfig,
  VisualLayerConfig,
  VisualLayerZone,
} from '@/features/courses/types/interactive'
import { evaluateExpression } from '@/features/courses/utils/expression'

const props = defineProps<{
  config: InteractiveConfig | null
}>()

const emit = defineEmits<{
  started: []
  checked: [payload: { passed: boolean }]
}>()

const selectedAnswer = ref<string | null>(null)
const checked = ref(false)
const started = ref(false)
const quizHintVisible = ref(false)
const formulaValues = ref<Record<string, number>>({})
const graphValues = ref<Record<string, number>>({})
const selectedFormulaOptionId = ref<string | null>(null)
const formulaStepIndex = ref(0)
const highlightedZoneId = ref<string | null>(null)
const highlightedOverlapId = ref<string | null>(null)
const visualFeedback = ref('')
const learnerZones = ref<VisualLayerZone[]>([])
const selectedLearnerZoneId = ref<string | null>(null)
const learnerRegionAnswers = ref<Record<string, string>>({})
type LearnerResizeHandle = 'nw' | 'n' | 'ne' | 'e' | 'se' | 's' | 'sw' | 'w'
const learnerResizeHandles: LearnerResizeHandle[] = ['nw', 'n', 'ne', 'e', 'se', 's', 'sw', 'w']
const learnerVisualDrag = ref<{
  id: string
  kind: 'move' | 'resize'
  offsetX: number
  offsetY: number
  resizeHandle?: LearnerResizeHandle
  start?: VisualLayerZone
  originPoint?: { x: number; y: number }
} | null>(null)

watch(
  () => props.config,
  (config) => {
    selectedAnswer.value = null
    checked.value = false
    started.value = false
    quizHintVisible.value = false
    highlightedZoneId.value = null
    highlightedOverlapId.value = null
    visualFeedback.value = ''
    learnerZones.value = []
    selectedLearnerZoneId.value = null
    learnerRegionAnswers.value = {}
    learnerVisualDrag.value = null
    if (config?.type === 'FORMULA_EXPLORER') {
      formulaValues.value = Object.fromEntries(config.variables.map((variable) => [variable.name, variable.initial]))
      selectedFormulaOptionId.value = config.formulaOptions?.[0]?.id ?? null
      formulaStepIndex.value = 0
    }
    if (config?.type === 'GRAPH_2D') {
      graphValues.value = Object.fromEntries(Object.entries(config.controls ?? {}).map(([name, control]) => [name, control.initial]))
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
  if (!config) return false
  if (config.type === 'QUIZ') return Boolean(selectedOption.value?.correct)
  if (config.mode !== 'PRACTICE') return false
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
  if (config.type === 'VISUAL_LAYER') return visualPracticePassed()
  return false
})

const activeFeedback = computed(() => {
  const config = props.config
  if (!config || (config.mode !== 'PRACTICE' && config.type !== 'QUIZ') || !checked.value) return ''
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

const visualPracticeSourceZones = computed(() => {
  if (props.config?.type !== 'VISUAL_LAYER' || props.config.mode !== 'PRACTICE') return []
  const sourceIds = props.config.overlap?.sourceZoneIds?.length
    ? props.config.overlap.sourceZoneIds
    : props.config.zones.map((zone) => zone.id)
  return sourceIds
    .map((zoneId) => props.config?.type === 'VISUAL_LAYER' ? props.config.zones.find((zone) => zone.id === zoneId) : null)
    .filter((zone): zone is VisualLayerZone => Boolean(zone))
    .slice(0, 3)
})

const learnerVisualConfig = computed<VisualLayerConfig | null>(() => {
  if (props.config?.type !== 'VISUAL_LAYER' || props.config.mode !== 'PRACTICE' || !props.config.overlap?.enabled) return null
  const sourceZoneIds = props.config.overlap.sourceZoneIds.filter((zoneId) => learnerZones.value.some((zone) => zone.id === zoneId))
  if (!sourceZoneIds.length) return null
  return {
    ...props.config,
    zones: learnerZones.value,
    elements: [],
    interactions: [],
    overlap: {
      ...props.config.overlap,
      sourceZoneIds,
    },
  }
})

const learnerOverlapRegions = computed(() => learnerVisualConfig.value ? generatedOverlapRegions(learnerVisualConfig.value) : [])

const expectedVisualPracticeRegions = computed(() => {
  if (props.config?.type !== 'VISUAL_LAYER' || props.config.mode !== 'PRACTICE' || !props.config.overlap?.enabled) return []
  const totalInputByZone = new Map((props.config.overlap.inputs ?? [])
    .filter((input) => input.zoneIds.length === 1)
    .map((input) => [input.zoneIds[0], input.value]))
  const expectedValues = props.config.overlap.values?.length
    ? props.config.overlap.values
    : generatedOverlapRegions(props.config)
  return expectedValues.filter((region) => {
    if (region.zoneIds.length === 1) return (totalInputByZone.get(region.zoneIds[0]) ?? region.value) !== 0
    return region.value !== 0
  })
})

const visibleExpectedLearnerRegions = computed(() => {
  const expectedIds = new Set(expectedVisualPracticeRegions.value.map((region) => region.id))
  return learnerOverlapRegions.value.filter((region) => expectedIds.has(region.id))
})

const selectedLearnerZone = computed(() => learnerZones.value.find((zone) => zone.id === selectedLearnerZoneId.value) ?? null)

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

function markStarted() {
  if (started.value || !props.config) return
  started.value = true
  emit('started')
}

function runCheck() {
  markStarted()
  checked.value = true
  emit('checked', { passed: practicePassed.value })
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

function chooseAnswer(optionId: string) {
  selectedAnswer.value = optionId
  checked.value = false
}

function quizOptionLetter(option: QuizConfig['options'][number], index: number) {
  return option.id.length === 1 ? option.id.toUpperCase() : String.fromCharCode(65 + index)
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

function visualPracticePassed() {
  const config = props.config
  if (config?.type !== 'VISUAL_LAYER' || config.mode !== 'PRACTICE' || !config.overlap?.enabled) return false
  const requiredZoneIds = visualPracticeSourceZones.value.map((zone) => zone.id)
  if (!requiredZoneIds.every((zoneId) => learnerZones.value.some((zone) => zone.id === zoneId))) return false
  const learnerRegionById = new Map(learnerOverlapRegions.value.map((region) => [region.id, region]))
  return expectedVisualPracticeRegions.value.every((expected) => {
    if (!learnerRegionById.has(expected.id)) return false
    const answer = Number(learnerRegionAnswers.value[expected.id])
    return Number.isFinite(answer) && answer === expected.value
  })
}

function visualStagePoint(event: PointerEvent, config: VisualLayerConfig) {
  const target = event.currentTarget as HTMLElement
  const stage = target.classList.contains('visual-layer-stage')
    ? target
    : target.closest('.visual-layer-stage') as HTMLElement | null
  const rect = (stage ?? target).getBoundingClientRect()
  const scaleX = config.canvas.width / rect.width
  const scaleY = config.canvas.height / rect.height
  return {
    x: Math.max(0, Math.min(config.canvas.width, Math.round((event.clientX - rect.left) * scaleX))),
    y: Math.max(0, Math.min(config.canvas.height, Math.round((event.clientY - rect.top) * scaleY))),
  }
}

function clampLearnerZone(zone: VisualLayerZone, config: VisualLayerConfig): VisualLayerZone {
  const size = Math.max(80, Math.min(Math.max(zone.width, zone.height), Math.min(config.canvas.width, config.canvas.height)))
  return {
    ...zone,
    shape: 'circle',
    width: size,
    height: size,
    x: Math.max(0, Math.min(zone.x, config.canvas.width - size)),
    y: Math.max(0, Math.min(zone.y, config.canvas.height - size)),
  }
}

function learnerZoneAddPlacement(zone: VisualLayerZone, index: number, config: VisualLayerConfig): VisualLayerZone {
  const size = Math.min(240, Math.max(190, Math.min(zone.width, zone.height)))
  const centerX = config.canvas.width / 2
  const placements = [
    { x: centerX - size - 80, y: 110, labelX: 34, labelY: 28 },
    { x: centerX + 80, y: 110, labelX: 66, labelY: 28 },
    { x: centerX - size / 2, y: 280, labelX: 50, labelY: 76 },
  ]
  const placement = placements[index] ?? {
    x: zone.x + index * 30,
    y: zone.y + index * 30,
    labelX: zone.labelX,
    labelY: zone.labelY,
  }
  return clampLearnerZone({
    ...zone,
    x: placement.x,
    y: placement.y,
    width: size,
    height: size,
    labelX: placement.labelX,
    labelY: placement.labelY,
  }, config)
}

function addLearnerZone(zone: VisualLayerZone) {
  if (props.config?.type !== 'VISUAL_LAYER') return
  if (learnerZones.value.some((item) => item.id === zone.id)) return
  const sourceIndex = visualPracticeSourceZones.value.findIndex((item) => item.id === zone.id)
  const nextZone = learnerZoneAddPlacement(zone, sourceIndex >= 0 ? sourceIndex : learnerZones.value.length, props.config)
  learnerZones.value = [
    ...learnerZones.value,
    nextZone,
  ]
  selectedLearnerZoneId.value = zone.id
  checked.value = false
}

function removeLearnerZone(zoneId: string) {
  learnerZones.value = learnerZones.value.filter((zone) => zone.id !== zoneId)
  if (selectedLearnerZoneId.value === zoneId) selectedLearnerZoneId.value = null
  learnerRegionAnswers.value = {}
  checked.value = false
}

function resetLearnerZone(zoneId: string) {
  if (props.config?.type !== 'VISUAL_LAYER') return
  const sourceZone = visualPracticeSourceZones.value.find((zone) => zone.id === zoneId)
  if (!sourceZone) return
  const sourceIndex = visualPracticeSourceZones.value.findIndex((zone) => zone.id === zoneId)
  learnerZones.value = learnerZones.value.map((zone) => (
    zone.id === zoneId ? learnerZoneAddPlacement(sourceZone, sourceIndex, props.config as VisualLayerConfig) : zone
  ))
  learnerRegionAnswers.value = {}
  selectedLearnerZoneId.value = zoneId
  checked.value = false
}

function updateLearnerRegionAnswer(regionId: string, event: Event) {
  learnerRegionAnswers.value = {
    ...learnerRegionAnswers.value,
    [regionId]: (event.target as HTMLInputElement).value,
  }
  checked.value = false
}

function beginLearnerZoneDrag(zone: VisualLayerZone, event: PointerEvent) {
  if (props.config?.type !== 'VISUAL_LAYER') return
  const point = visualStagePoint(event, props.config)
  ;(event.currentTarget as HTMLElement).setPointerCapture?.(event.pointerId)
  selectedLearnerZoneId.value = zone.id
  learnerVisualDrag.value = {
    id: zone.id,
    kind: 'move',
    offsetX: point.x - zone.x,
    offsetY: point.y - zone.y,
  }
}

function beginLearnerZoneResize(zone: VisualLayerZone, handle: LearnerResizeHandle, event: PointerEvent) {
  if (props.config?.type !== 'VISUAL_LAYER') return
  const point = visualStagePoint(event, props.config)
  ;(event.currentTarget as HTMLElement).setPointerCapture?.(event.pointerId)
  selectedLearnerZoneId.value = zone.id
  learnerVisualDrag.value = {
    id: zone.id,
    kind: 'resize',
    offsetX: 0,
    offsetY: 0,
    resizeHandle: handle,
    start: { ...zone },
    originPoint: point,
  }
}

function selectLearnerStage(event: PointerEvent) {
  if (event.target === event.currentTarget) selectedLearnerZoneId.value = null
}

function moveLearnerVisualPointer(event: PointerEvent) {
  const config = props.config
  const drag = learnerVisualDrag.value
  if (config?.type !== 'VISUAL_LAYER' || !drag) return
  const point = visualStagePoint(event, config)
  learnerZones.value = learnerZones.value.map((zone) => {
    if (zone.id !== drag.id) return zone
    if (drag.kind === 'resize' && drag.start && drag.originPoint && drag.resizeHandle) {
      const dx = point.x - drag.originPoint.x
      const dy = point.y - drag.originPoint.y
      const directionX = drag.resizeHandle.includes('w') ? -1 : drag.resizeHandle.includes('e') ? 1 : 0
      const directionY = drag.resizeHandle.includes('n') ? -1 : drag.resizeHandle.includes('s') ? 1 : 0
      const rawDelta = Math.max(directionX * dx, directionY * dy)
      const nextSize = Math.max(80, drag.start.width + rawDelta)
      const anchorX = drag.resizeHandle.includes('w') ? drag.start.x + drag.start.width : drag.start.x
      const anchorY = drag.resizeHandle.includes('n') ? drag.start.y + drag.start.height : drag.start.y
      return clampLearnerZone({
        ...zone,
        x: drag.resizeHandle.includes('w') ? anchorX - nextSize : drag.start.x,
        y: drag.resizeHandle.includes('n') ? anchorY - nextSize : drag.start.y,
        width: nextSize,
        height: nextSize,
      }, config)
    }
    return clampLearnerZone({ ...zone, x: point.x - drag.offsetX, y: point.y - drag.offsetY }, config)
  })
  checked.value = false
}

function endLearnerVisualPointer(event: PointerEvent) {
  const target = event.currentTarget as HTMLElement
  if (target.hasPointerCapture?.(event.pointerId)) target.releasePointerCapture?.(event.pointerId)
  learnerVisualDrag.value = null
}

function learnerRegionInputStyle(region: GeneratedOverlapRegion) {
  const config = props.config?.type === 'VISUAL_LAYER' ? props.config : null
  const canvasCenter = config ? { x: config.canvas.width / 2, y: config.canvas.height / 2 } : { x: 450, y: 260 }
  const shift = learnerRegionInputShift(region)
  const x = Math.max(42, Math.min((config?.canvas.width ?? 900) - 42, region.center.x + shift.x))
  const y = Math.max(30, Math.min((config?.canvas.height ?? 520) - 30, region.center.y + shift.y + (region.zoneIds.length === 1 ? (region.center.y < canvasCenter.y ? -8 : 8) : 0)))
  return {
    left: `${x}px`,
    top: `${y}px`,
  }
}

function learnerRegionInputShift(region: GeneratedOverlapRegion) {
  const ids = region.zoneIds.join('|')
  if (region.zoneIds.length === 3) return { x: 0, y: 34 }
  if (ids.includes('zone_a') && ids.includes('zone_b')) return { x: 0, y: -30 }
  if (ids.includes('zone_a') && ids.includes('zone_c')) return { x: -38, y: 18 }
  if (ids.includes('zone_b') && ids.includes('zone_c')) return { x: 38, y: 18 }
  if (ids.includes('zone_a')) return { x: -42, y: -8 }
  if (ids.includes('zone_b')) return { x: 42, y: -8 }
  if (ids.includes('zone_c')) return { x: 0, y: 34 }
  return { x: 0, y: 0 }
}

function learnerRegionLabel(region: GeneratedOverlapRegion) {
  const labels = region.zoneIds.map((zoneId) => visualPracticeSourceZones.value.find((zone) => zone.id === zoneId)?.label ?? zoneId.replace(/^zone_/, '').toUpperCase())
  if (labels.length === 1) return `${labels[0]} only`
  if (labels.length === 3) return labels.join('')
  return `${labels.join('')} only`
}

function learnerToolbarStyle(zone: VisualLayerZone) {
  return {
    left: `${zone.x + zone.width / 2}px`,
    top: `${Math.max(6, zone.y - 10)}px`,
  }
}

function learnerResizeHandleStyle(handle: LearnerResizeHandle, zone: VisualLayerZone) {
  const x = handle.includes('w') ? zone.x : handle.includes('e') ? zone.x + zone.width : zone.x + zone.width / 2
  const y = handle.includes('n') ? zone.y : handle.includes('s') ? zone.y + zone.height : zone.y + zone.height / 2
  return {
    left: `${x}px`,
    top: `${y}px`,
  }
}

</script>

<template>
  <section
    class="interactive-preview"
    :class="{ 'interactive-preview--quiz': config?.type === 'QUIZ' }"
    @pointerdown.capture="markStarted"
    @input.capture="markStarted"
    @change.capture="markStarted"
  >
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
        <button type="button" class="check-button" @click="runCheck">Check</button>
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
        <button type="button" class="check-button" @click="runCheck">Check</button>
        <p v-if="checked" class="feedback">{{ activeFeedback }}</p>
      </template>
    </div>

    <div v-else-if="config.type === 'VISUAL_LAYER'" class="space-y-3">
      <header class="interactive-header">
        <h3>{{ (config as VisualLayerConfig).title }}</h3>
        <span>Set / Diagram</span>
      </header>
      <template v-if="config.mode === 'PRACTICE'">
        <p class="practice-prompt">{{ (config as VisualLayerConfig).prompt }}</p>
        <div class="visual-practice-toolbar">
          <button
            v-for="zone in visualPracticeSourceZones"
            :key="zone.id"
            type="button"
            class="visual-add-button"
            :disabled="learnerZones.some((item) => item.id === zone.id)"
            @click="addLearnerZone(zone)"
          >
            Add {{ zone.label }}
          </button>
        </div>
        <div class="visual-layer-stage-wrap">
          <div
            class="visual-layer-stage visual-layer-stage--practice"
            :style="{
              width: `${(config as VisualLayerConfig).canvas.width}px`,
              height: `${(config as VisualLayerConfig).canvas.height}px`,
            }"
            @pointermove="moveLearnerVisualPointer"
            @pointerup="endLearnerVisualPointer"
            @pointercancel="endLearnerVisualPointer"
            @pointerdown="selectLearnerStage"
          >
            <p v-if="!learnerZones.length" class="visual-layer-bg-text">Add circles to build the diagram.</p>
            <div
              v-for="zone in learnerZones"
              :key="zone.id"
              class="visual-zone visual-zone--circle visual-zone--editable"
              :class="{ 'visual-zone--selected': selectedLearnerZoneId === zone.id }"
              :style="{ ...visualObjectStyle(zone), ...visualZoneLabelStyle(zone), '--zone-color': zone.color, '--zone-highlight-color': zone.highlightColor ?? zone.color, '--zone-highlight-opacity': zone.highlightOpacity ?? 0.82 }"
              @pointerdown.stop="beginLearnerZoneDrag(zone, $event)"
            >
              <span>{{ zone.label }}</span>
            </div>
            <div
              v-if="selectedLearnerZone"
              class="visual-zone-toolbar"
              :style="learnerToolbarStyle(selectedLearnerZone)"
              @pointerdown.stop
            >
              <button type="button" class="visual-zone-toolbar__button" @click.stop="resetLearnerZone(selectedLearnerZone.id)">Reset</button>
              <button type="button" class="visual-zone-toolbar__button visual-zone-toolbar__button--danger" @click.stop="removeLearnerZone(selectedLearnerZone.id)">Delete</button>
            </div>
            <template v-if="selectedLearnerZone">
              <button
                v-for="handle in learnerResizeHandles"
                :key="handle"
                type="button"
                class="visual-zone-resize"
                :class="`visual-zone-resize--${handle}`"
                :style="learnerResizeHandleStyle(handle, selectedLearnerZone)"
                :aria-label="`Resize ${handle}`"
                @pointerdown.stop.prevent="beginLearnerZoneResize(selectedLearnerZone, handle, $event)"
              />
            </template>
            <svg
              v-if="learnerOverlapRegions.length"
              class="visual-overlap-svg"
              :viewBox="`0 0 ${(config as VisualLayerConfig).canvas.width} ${(config as VisualLayerConfig).canvas.height}`"
              aria-hidden="true"
            >
              <path
                v-for="region in learnerOverlapRegions"
                :key="`${region.id}-learner-region`"
                :d="region.maskPath"
                class="visual-overlap-hit"
              />
            </svg>
            <label
              v-for="region in visibleExpectedLearnerRegions"
              :key="`${region.id}-answer`"
              class="visual-region-answer"
              :style="learnerRegionInputStyle(region)"
            >
              <span>{{ learnerRegionLabel(region) }}</span>
              <input
                type="number"
                :value="learnerRegionAnswers[region.id] ?? ''"
                @input="updateLearnerRegionAnswer(region.id, $event)"
              />
            </label>
          </div>
        </div>
        <button type="button" class="check-button" @click="runCheck">Check</button>
        <p v-if="checked" class="feedback">{{ activeFeedback }}</p>
      </template>
      <div v-if="config.mode !== 'PRACTICE'" class="visual-layer-stage-wrap">
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

    <div v-else-if="config.type === 'QUIZ'" class="quiz-stage">
      <header class="quiz-stage__header">
        <span>{{ (config as QuizConfig).title }}</span>
        <h3>{{ config.prompt }}</h3>
      </header>
      <div class="quiz-question-panel">
        <p>{{ (config as QuizConfig).question }}</p>
      </div>
      <div class="quiz-option-grid">
        <button
          v-for="(option, index) in (config as QuizConfig).options"
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
          <span class="quiz-option__badge">{{ quizOptionLetter(option, index) }}</span>
          <span>{{ option.label }}</span>
        </button>
      </div>
      <div v-if="(config as QuizConfig).explanation && (quizHintVisible || checked)" class="quiz-hint">
        <strong>{{ checked ? 'Explanation' : 'Hint' }}</strong>
        <span>{{ (config as QuizConfig).explanation }}</span>
      </div>
      <p v-if="checked" class="quiz-feedback" :class="{ 'quiz-feedback--success': practicePassed, 'quiz-feedback--failure': !practicePassed }">
        {{ activeFeedback || (selectedOption?.correct ? 'Correct.' : 'Not quite.') }}
      </p>
      <footer class="quiz-action-row">
        <button
          v-if="(config as QuizConfig).explanation"
          type="button"
          class="quiz-secondary-button"
          @click="quizHintVisible = !quizHintVisible"
        >
          {{ quizHintVisible ? 'Hide hint' : 'Show hint' }}
        </button>
        <button type="button" class="quiz-check-button" :disabled="!selectedAnswer" @click="runCheck">Check</button>
      </footer>
    </div>

  </section>
</template>

<style scoped>
.interactive-preview {
  width: 100%;
  min-width: 0;
  max-width: 100%;
  box-sizing: border-box;
  overflow: hidden;
  border: 2px solid #1a1814;
  border-radius: 10px;
  background: #fbf7ef;
  padding: 1rem;
  color: #1a1814;
}
.interactive-preview--quiz {
  border: 0;
  border-radius: 0;
  background: transparent;
  padding: 0;
  box-shadow: none;
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
  width: 100%;
  min-width: 0;
  box-sizing: border-box;
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
.visual-layer-stage--practice {
  touch-action: none;
  user-select: none;
}
.visual-practice-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}
.visual-add-button {
  min-height: 2.25rem;
  border: 2px solid #1a1814;
  border-radius: 8px;
  background: #ffd333;
  padding: 0 0.75rem;
  color: #1a1814;
  font-size: 12px;
  font-weight: 900;
}
.visual-add-button:disabled {
  cursor: not-allowed;
  opacity: 0.45;
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
.quiz-stage {
  display: grid;
  gap: 1.25rem;
  width: min(100%, 860px);
  margin: 0 auto;
}
.quiz-stage__header {
  display: grid;
  gap: 0.35rem;
}
.quiz-stage__header span {
  color: #8f887e;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}
.quiz-stage__header h3 {
  margin: 0;
  color: #1a1814;
  font-size: clamp(1.25rem, 2vw, 1.75rem);
  font-weight: 950;
  line-height: 1.2;
}
.quiz-question-panel {
  display: grid;
  min-height: 8.5rem;
  place-items: center;
  overflow: hidden;
  border: 2px solid #1a1814;
  border-radius: 18px;
  background:
    radial-gradient(circle, rgba(255, 253, 248, 0.08) 1px, transparent 1.5px),
    #171611;
  background-size: 18px 18px;
  padding: 1.5rem;
  color: #fffdf8;
  box-shadow: 3px 3px 0 #1a1814;
}
.quiz-question-panel p {
  max-width: 100%;
  margin: 0;
  text-align: center;
  font-family: Georgia, 'Times New Roman', serif;
  font-size: clamp(1.65rem, 4vw, 3rem);
  font-style: italic;
  font-weight: 900;
  line-height: 1.15;
  overflow-wrap: anywhere;
}
.quiz-option-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.9rem;
}
.quiz-option {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: 1rem;
  min-height: 5rem;
  border: 2px solid #1a1814;
  border-radius: 14px;
  background: #fffdf8;
  padding: 0.85rem 1.2rem;
  color: #1a1814;
  text-align: left;
  font-size: 1.1rem;
  font-weight: 900;
  line-height: 1.25;
  box-shadow: 3px 3px 0 #1a1814;
  transition: transform 150ms ease, box-shadow 150ms ease, background 150ms ease;
}
.quiz-option:hover {
  transform: translateY(-1px);
  box-shadow: 4px 4px 0 #1a1814;
}
.quiz-option--selected {
  background: #ffd333;
}
.quiz-option--correct {
  background: #dff4df;
}
.quiz-option--wrong {
  background: #f9d3c5;
}
.quiz-option__badge {
  display: inline-grid;
  width: 2.35rem;
  height: 2.35rem;
  place-items: center;
  border: 2px solid #1a1814;
  border-radius: 999px;
  background: #fbf7ef;
  color: #1a1814;
  font-size: 0.85rem;
  font-weight: 950;
}
.quiz-option--selected .quiz-option__badge {
  background: #1a1814;
  color: #ffd333;
}
.quiz-hint,
.quiz-feedback {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  width: min(100%, 520px);
  border: 2px solid #1a1814;
  border-radius: 12px;
  background: #fffdf8;
  padding: 0.85rem 1rem;
  color: #1a1814;
  font-size: 0.9rem;
  font-weight: 750;
  line-height: 1.45;
  box-shadow: 3px 3px 0 #1a1814;
}
.quiz-hint strong,
.quiz-feedback {
  font-weight: 900;
}
.quiz-feedback--success {
  background: #dff4df;
  color: #245e3e;
}
.quiz-feedback--failure {
  background: #f9d3c5;
  color: #8c3322;
}
.quiz-action-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  border-top: 2px solid #e4ded6;
  padding-top: 0.75rem;
}
.quiz-secondary-button,
.quiz-check-button {
  min-height: 3rem;
  border: 2px solid #1a1814;
  border-radius: 999px;
  padding: 0 1.25rem;
  font-size: 0.95rem;
  font-weight: 950;
  box-shadow: 3px 3px 0 #1a1814;
  transition: transform 150ms ease, box-shadow 150ms ease;
}
.quiz-secondary-button {
  background: #fffdf8;
  color: #1a1814;
}
.quiz-check-button {
  margin-left: auto;
  background: #1a1814;
  color: #fffdf8;
}
.quiz-secondary-button:hover,
.quiz-check-button:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 4px 4px 0 #1a1814;
}
.quiz-check-button:disabled {
  cursor: not-allowed;
  opacity: 0.45;
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
.visual-zone--editable {
  z-index: 6;
  cursor: move;
  pointer-events: auto;
}
.visual-zone--editable:hover {
  background: color-mix(in srgb, var(--zone-color) 42%, transparent);
  box-shadow: inset 0 0 0 2px rgba(26, 24, 20, 0.2);
}
.visual-zone--selected {
  background: color-mix(in srgb, var(--zone-highlight-color) 48%, transparent);
  box-shadow:
    0 0 0 5px rgba(255, 211, 51, 0.38),
    inset 0 0 0 2px rgba(26, 24, 20, 0.18);
}
.visual-zone-toolbar {
  position: absolute;
  z-index: 14;
  display: flex;
  gap: 0.25rem;
  transform: translate(-50%, -100%);
  border: 2px solid #1a1814;
  border-radius: 8px;
  background: #fffdf8;
  padding: 0.25rem;
  box-shadow: 0 8px 18px rgba(26, 24, 20, 0.16);
}
.visual-zone-toolbar__button {
  min-height: 1.7rem;
  border: 0;
  border-radius: 6px;
  background: transparent;
  padding: 0 0.45rem;
  color: #1a1814;
  font-size: 11px;
  font-weight: 900;
}
.visual-zone-toolbar__button:hover {
  background: #f7f2ea;
}
.visual-zone-toolbar__button--danger {
  color: #9b2614;
}
.visual-zone-resize {
  position: absolute;
  z-index: 13;
  border: 2px solid #1a1814;
  border-radius: 999px;
  background: #fffdf8;
  color: #1a1814;
  font-weight: 900;
  width: 0.8rem;
  height: 0.8rem;
  transform: translate(-50%, -50%);
}
.visual-zone-resize:hover {
  background: #ffd333;
}
.visual-zone-resize--nw,
.visual-zone-resize--se {
  cursor: nwse-resize;
}
.visual-zone-resize--ne,
.visual-zone-resize--sw {
  cursor: nesw-resize;
}
.visual-zone-resize--n,
.visual-zone-resize--s {
  cursor: ns-resize;
}
.visual-zone-resize--e,
.visual-zone-resize--w {
  cursor: ew-resize;
}
.visual-region-answer {
  position: absolute;
  z-index: 9;
  display: grid;
  width: 4.45rem;
  gap: 0.15rem;
  transform: translate(-50%, -50%);
  pointer-events: auto;
}
.visual-region-answer span {
  overflow: hidden;
  border-radius: 6px;
  background: rgba(255, 253, 248, 0.9);
  padding: 0.08rem 0.2rem;
  font-size: 9px;
  font-weight: 900;
  text-align: center;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.visual-region-answer input {
  width: 100%;
  border: 2px solid #1a1814;
  border-radius: 6px;
  background: #fffdf8;
  padding: 0.22rem 0.25rem;
  color: #1a1814;
  font-size: 12px;
  font-weight: 900;
  text-align: center;
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
@media (max-width: 700px) {
  .quiz-stage {
    gap: 0.9rem;
  }
  .quiz-question-panel {
    min-height: 6.5rem;
    border-radius: 14px;
    padding: 1rem;
  }
  .quiz-option-grid {
    grid-template-columns: 1fr;
    gap: 0.65rem;
  }
  .quiz-option {
    min-height: 4.25rem;
    padding: 0.75rem 0.9rem;
    font-size: 1rem;
  }
  .quiz-action-row {
    align-items: stretch;
  }
  .quiz-secondary-button,
  .quiz-check-button {
    width: 100%;
    margin-left: 0;
  }
}
</style>
