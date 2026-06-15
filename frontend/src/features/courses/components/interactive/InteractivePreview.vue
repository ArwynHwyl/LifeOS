<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { generatedOverlapRegions } from '@/features/courses/types/interactive'
import type {
  FormulaExplorerConfig,
  GeneratedOverlapRegion,
  Graph2DConfig,
  InteractiveConfig,
  LogicFlowConfig,
  QuizConfig,
  VisualLayerConfig,
  VisualLayerZone,
} from '@/features/courses/types/interactive'
import { evaluateExpression } from '@/features/courses/utils/expression'
import { Logic } from '@/features/courses/utils/logic-engine.js'
import LogicCircuit from '@/features/courses/components/Admin/interactives/LogicCircuit.vue'
import LogicSimplify from '@/features/courses/components/Admin/interactives/LogicSimplify.vue'
import type { InteractiveAttemptRequest, LogicAttemptRequest } from '@/features/learning/services/learnerCourses'

const instanceId = 'vl-' + Math.random().toString(36).substring(2, 9)

const props = defineProps<{
  config: InteractiveConfig | null
  serverFeedback?: string
}>()

const emit = defineEmits<{
  started: []
  checked: [payload: { passed: boolean; attempt?: InteractiveAttemptRequest }]
}>()

const selectedAnswer = ref<string | null>(null)
const checked = ref(false)
const started = ref(false)
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
const logicAnswer = ref('')
const logicInputs = ref<Record<string, boolean>>({})
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
    highlightedZoneId.value = null
    highlightedOverlapId.value = null
    visualFeedback.value = ''
    learnerZones.value = []
    selectedLearnerZoneId.value = null
    learnerRegionAnswers.value = {}
    logicAnswer.value = ''
    logicInputs.value = {}
    learnerVisualDrag.value = null
    if (config?.type === 'FORMULA_EXPLORER') {
      formulaValues.value = Object.fromEntries(config.variables.map((variable) => [variable.name, variable.initial]))
      selectedFormulaOptionId.value = config.formulaOptions?.[0]?.id ?? null
      formulaStepIndex.value = 0
    }
    if (config?.type === 'GRAPH_2D') {
      graphValues.value = Object.fromEntries(Object.entries(config.controls ?? {}).map(([name, control]) => [name, control.initial]))
    }
    if (config?.type === 'LOGIC_FLOW' && config.kind === 'CIRCUIT') {
      const parsed = Logic.tryParse(config.expression ?? '')
      const vars = parsed.ast ? Logic.variables(parsed.ast) as string[] : []
      logicInputs.value = Object.fromEntries(vars.map((name) => [name, true]))
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
  if (config.type === 'LOGIC_FLOW') return false
  return false
})

const isDiscreteContinuousQuiz = computed(() => {
  if (props.config?.type !== 'QUIZ') return false
  const labels = props.config.options.map(o => o.label.toLowerCase())
  return labels.includes('discrete') && labels.includes('continuous')
})

const activeFeedback = computed(() => {
  const config = props.config
  if (!config || (config.mode !== 'PRACTICE' && config.type !== 'QUIZ') || !checked.value) return ''
  if (config.type === 'LOGIC_FLOW') return props.serverFeedback || ''
  if (!('feedback' in config)) return ''
  return practicePassed.value ? config.feedback?.success : config.feedback?.failure
})

const logicExpressionParse = computed(() => {
  if (props.config?.type !== 'LOGIC_FLOW' || props.config.kind !== 'CIRCUIT') return { ast: null, error: null }
  return Logic.tryParse(props.config.expression ?? '')
})

const logicCircuitOutput = computed(() => {
  if (!logicExpressionParse.value.ast) return null
  return Boolean(Logic.evaluate(logicExpressionParse.value.ast, logicInputs.value))
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
  const logicAttempt = buildLogicAttempt()
  const attempt = logicAttempt ?? buildInteractiveAttempt()
  emit('checked', attempt ? { passed: practicePassed.value, attempt } : { passed: practicePassed.value })
}

function buildInteractiveAttempt(): InteractiveAttemptRequest | null {
  const config = props.config
  if (!config) return null
  if (config.type === 'QUIZ' && selectedAnswer.value) {
    return { answer: selectedAnswer.value }
  }
  if (config.type === 'FORMULA_EXPLORER') {
    return { values: formulaValues.value }
  }
  if (config.type === 'GRAPH_2D') {
    return { values: graphValues.value }
  }
  if (config.type === 'VISUAL_LAYER') {
    return {
      regionAnswers: Object.fromEntries(
        Object.entries(learnerRegionAnswers.value)
          .map(([id, value]) => [id, Number(value)] as const)
          .filter(([, value]) => Number.isFinite(value)),
      ),
    }
  }
  return null
}

function buildLogicAttempt(): LogicAttemptRequest | null {
  const config = props.config
  if (config?.type !== 'LOGIC_FLOW') return null
  if (config.kind === 'SIMPLIFY') {
    return {
      kind: 'SIMPLIFY',
      answer: logicAnswer.value,
      steps: (config.steps ?? []).map((step) => ({
        lawId: step.lawId ?? step.law,
        from: step.from,
        to: step.to ?? step.result,
      })),
    }
  }
  if (logicCircuitOutput.value === null) return null
  return {
    kind: 'CIRCUIT',
    inputs: logicInputs.value,
    answer: logicCircuitOutput.value,
  }
}

function toggleLogicValve(name: string) {
  markStarted()
  logicInputs.value = {
    ...logicInputs.value,
    [name]: !logicInputs.value[name],
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
  const targetId = interaction?.effect === 'HIGHLIGHT_ZONE' ? interaction.targetZoneId ?? null : null

  if (targetId) {
    const isZone = props.config.zones.some((zone) => zone.id === targetId)
    if (isZone) {
      highlightedZoneId.value = targetId
      highlightedOverlapId.value = null
      const targetZone = props.config.zones.find((zone) => zone.id === targetId)
      visualFeedback.value = interaction?.feedback || targetZone?.feedback || ''
    } else {
      const regions = generatedOverlapRegions(props.config)
      const targetRegion = regions.find((region) => region.id === targetId)
      if (targetRegion) {
        highlightedZoneId.value = null
        highlightedOverlapId.value = targetId
        visualFeedback.value = interaction?.feedback || targetRegion.feedback || targetRegion.label
      } else {
        highlightedZoneId.value = null
        highlightedOverlapId.value = null
        visualFeedback.value = interaction?.feedback || ''
      }
    }
  } else {
    highlightedZoneId.value = null
    highlightedOverlapId.value = null
    visualFeedback.value = interaction?.feedback || ''
  }
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

    <div v-else-if="config.type === 'LOGIC_FLOW'" class="space-y-3">
      <header class="interactive-header">
        <h3>{{ (config as LogicFlowConfig).title }}</h3>
        <code>{{ (config as LogicFlowConfig).kind === 'CIRCUIT' ? (config as LogicFlowConfig).expression : (config as LogicFlowConfig).start }}</code>
      </header>

      <div v-if="(config as LogicFlowConfig).kind === 'CIRCUIT'" class="space-y-3">
        <p v-if="!logicExpressionParse.ast" class="feedback">{{ logicExpressionParse.error }}</p>
        <template v-else>
          <p class="practice-prompt">Click the valves to toggle them open (T) or shut (F).</p>
          <LogicCircuit
            :ast="logicExpressionParse.ast"
            :env="logicInputs"
            :goal="(config as LogicFlowConfig).goal ?? null"
            @toggle="toggleLogicValve"
          />
        </template>
        <template v-if="config.mode === 'PRACTICE'">
          <button type="button" class="check-button" :disabled="!logicExpressionParse.ast" @click="runCheck">Check</button>
          <p v-if="checked && activeFeedback" class="feedback">{{ activeFeedback }}</p>
        </template>
      </div>

      <div v-else class="space-y-3">
        <LogicSimplify
          :start="(config as LogicFlowConfig).start ?? ''"
          :steps="(config as LogicFlowConfig).steps ?? []"
          :success-text="(config as LogicFlowConfig).feedback?.success ?? ''"
        />
        <p v-if="(config as LogicFlowConfig).target" class="practice-prompt">
          Target: <code>{{ (config as LogicFlowConfig).target }}</code>
        </p>
        <label class="logic-answer">
          <span>Final expression</span>
          <input v-model="logicAnswer" placeholder="¬P ∨ Q" @input="checked = false" />
        </label>
        <button type="button" class="check-button" @click="runCheck">Check</button>
        <p v-if="checked && activeFeedback" class="feedback">{{ activeFeedback }}</p>
      </div>
    </div>

    <div v-else-if="config.type === 'VISUAL_LAYER'" class="space-y-3">
      <header class="interactive-header">
        <h3>{{ (config as VisualLayerConfig).title }}</h3>
        <span>Set / Diagram</span>
      </header>
      <template v-if="config.mode === 'PRACTICE'">
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
            v-for="element in (config as VisualLayerConfig).elements.filter(e => e.kind !== 'line')"
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
            v-if="visualOverlapRegions.length || (config as VisualLayerConfig).elements?.some(e => e.kind === 'line')"
            class="visual-overlap-svg"
            :viewBox="`0 0 ${(config as VisualLayerConfig).canvas.width} ${(config as VisualLayerConfig).canvas.height}`"
            aria-hidden="true"
            style="position: absolute; inset: 0; width: 100%; height: 100%; pointer-events: none;"
          >
            <defs>
              <marker
                id="vl-arrow"
                viewBox="0 0 10 10"
                refX="6"
                refY="5"
                markerWidth="6"
                markerHeight="6"
                orient="auto-start-reverse"
              >
                <path d="M 0 2 L 8 5 L 0 8 z" fill="currentColor" />
              </marker>

              <!-- Zone ClipPaths -->
              <clipPath v-for="zone in (config as VisualLayerConfig).zones" :key="`clip-${zone.id}`" :id="`${instanceId}-clip-${zone.id}`">
                <ellipse
                  v-if="zone.shape === 'circle'"
                  :cx="zone.x + zone.width / 2"
                  :cy="zone.y + zone.height / 2"
                  :rx="zone.width / 2"
                  :ry="zone.height / 2"
                />
                <rect
                  v-else
                  :x="zone.x"
                  :y="zone.y"
                  :width="zone.width"
                  :height="zone.height"
                  :rx="10"
                />
              </clipPath>

              <!-- Region Masks -->
              <mask v-for="region in visualOverlapRegions" :key="`mask-${region.id}`" :id="`${instanceId}-mask-${region.id}`">
                <rect x="0" y="0" :width="(config as VisualLayerConfig).canvas.width" :height="(config as VisualLayerConfig).canvas.height" fill="black" />
                <!-- Included intersection -->
                <ellipse
                  v-if="region.zones.length === 1 && region.zones[0].shape === 'circle'"
                  :cx="region.zones[0].x + region.zones[0].width / 2"
                  :cy="region.zones[0].y + region.zones[0].height / 2"
                  :rx="region.zones[0].width / 2"
                  :ry="region.zones[0].height / 2"
                  fill="white"
                />
                <rect
                  v-else-if="region.zones.length === 1 && region.zones[0].shape === 'rectangle'"
                  :x="region.zones[0].x"
                  :y="region.zones[0].y"
                  :width="region.zones[0].width"
                  :height="region.zones[0].height"
                  fill="white"
                />

                <g v-else-if="region.zones.length === 2" :clip-path="`url(#${instanceId}-clip-${region.zones[1].id})`">
                  <ellipse
                    v-if="region.zones[0].shape === 'circle'"
                    :cx="region.zones[0].x + region.zones[0].width / 2"
                    :cy="region.zones[0].y + region.zones[0].height / 2"
                    :rx="region.zones[0].width / 2"
                    :ry="region.zones[0].height / 2"
                    fill="white"
                  />
                  <rect
                    v-else
                    :x="region.zones[0].x"
                    :y="region.zones[0].y"
                    :width="region.zones[0].width"
                    :height="region.zones[0].height"
                    fill="white"
                  />
                </g>

                <g v-else-if="region.zones.length === 3" :clip-path="`url(#${instanceId}-clip-${region.zones[2].id})`">
                  <g :clip-path="`url(#${instanceId}-clip-${region.zones[1].id})`">
                    <ellipse
                      v-if="region.zones[0].shape === 'circle'"
                      :cx="region.zones[0].x + region.zones[0].width / 2"
                      :cy="region.zones[0].y + region.zones[0].height / 2"
                      :rx="region.zones[0].width / 2"
                      :ry="region.zones[0].height / 2"
                      fill="white"
                    />
                    <rect
                      v-else
                      :x="region.zones[0].x"
                      :y="region.zones[0].y"
                      :width="region.zones[0].width"
                      :height="region.zones[0].height"
                      fill="white"
                    />
                  </g>
                </g>

                <!-- Excluded subtracts -->
                <template v-for="exZone in region.excludedZones" :key="`ex-${exZone.id}`">
                  <ellipse
                    v-if="exZone.shape === 'circle'"
                    :cx="exZone.x + exZone.width / 2"
                    :cy="exZone.y + exZone.height / 2"
                    :rx="exZone.width / 2"
                    :ry="exZone.height / 2"
                    fill="black"
                  />
                  <rect
                    v-else
                    :x="exZone.x"
                    :y="exZone.y"
                    :width="exZone.width"
                    :height="exZone.height"
                    fill="black"
                  />
                </template>
              </mask>
            </defs>

            <!-- Lines (Student Interactive View) -->
            <g
              v-for="element in (config as VisualLayerConfig).elements.filter(e => e.kind === 'line')"
              :key="element.id"
              :style="{ color: element.color || '#1a1814' }"
            >
              <!-- Base path for click/trigger (if they have interaction) -->
              <path
                :d="`M ${element.x1 ?? 0} ${element.y1 ?? 0} Q ${element.qx !== undefined ? element.qx : (((element.x1 ?? 0) + (element.x2 ?? 0))/2)} ${element.qy !== undefined ? element.qy : (((element.y1 ?? 0) + (element.y2 ?? 0))/2)} ${element.x2 ?? 0} ${element.y2 ?? 0}`"
                fill="none"
                stroke="transparent"
                stroke-width="16"
                style="pointer-events: auto; cursor: pointer;"
                @click="runVisualInteraction(element.id)"
              />
              <!-- Visible path -->
              <path
                :d="`M ${element.x1 ?? 0} ${element.y1 ?? 0} Q ${element.qx !== undefined ? element.qx : (((element.x1 ?? 0) + (element.x2 ?? 0))/2)} ${element.qy !== undefined ? element.qy : (((element.y1 ?? 0) + (element.y2 ?? 0))/2)} ${element.x2 ?? 0} ${element.y2 ?? 0}`"
                fill="none"
                stroke="currentColor"
                :stroke-width="element.strokeWidth || 3"
                :stroke-dasharray="element.flow && element.flow !== 'none' ? '8 6' : 'none'"
                :class="{
                  'animate-flow-forward': element.flow === 'forward',
                  'animate-flow-backward': element.flow === 'backward'
                }"
                :marker-end="(element.arrow === 'end' || element.arrow === 'both' || (!element.arrow && element.flow === 'forward')) ? 'url(#vl-arrow)' : 'none'"
                :marker-start="(element.arrow === 'start' || element.arrow === 'both' || (!element.arrow && element.flow === 'backward')) ? 'url(#vl-arrow)' : 'none'"
                stroke-linecap="round"
                style="pointer-events: auto; cursor: pointer;"
                @click="runVisualInteraction(element.id)"
              />
              <text
                v-if="element.label && element.label !== 'Line'"
                :x="0.25 * (element.x1 ?? 0) + 0.5 * (element.qx !== undefined ? element.qx : (((element.x1 ?? 0) + (element.x2 ?? 0))/2)) + 0.25 * (element.x2 ?? 0)"
                :y="0.25 * (element.y1 ?? 0) + 0.5 * (element.qy !== undefined ? element.qy : (((element.y1 ?? 0) + (element.y2 ?? 0))/2)) + 0.25 * (element.y2 ?? 0) - 10"
                text-anchor="middle"
                class="font-display text-[11px] font-bold fill-lm-ink pointer-events-none"
                style="user-select: none;"
              >
                {{ element.label }}
              </text>
            </g>

            <!-- Overlaps -->
            <path
              v-for="region in visualOverlapRegions"
              :key="`${region.id}-hit`"
              :d="region.maskPath"
              class="visual-overlap-hit"
              style="pointer-events: auto;"
              @click.stop="runVisualOverlapRegion(region.id)"
            />
            <rect
              v-for="region in visualOverlapRegions"
              v-show="highlightedOverlapId === region.id"
              :key="`${region.id}-active`"
              width="100%"
              height="100%"
              :mask="`url(#${instanceId}-mask-${region.id})`"
              class="visual-overlap-active"
            />
            <template v-if="config.overlap?.enabled">
              <g
                v-for="region in visualOverlapRegions"
                :key="`${region.id}-label`"
                class="visual-overlap-label"
                :class="{ 'visual-overlap-label--active': highlightedOverlapId === region.id }"
                :transform="`translate(${region.center.x}, ${region.center.y})`"
                style="pointer-events: auto;"
                @click.stop="runVisualOverlapRegion(region.id)"
              >
                <rect x="-26" y="-16" width="52" height="32" rx="8" />
                <text text-anchor="middle" dominant-baseline="central">{{ region.value }}</text>
              </g>
            </template>
          </svg>
        </div>
      </div>
      <p v-if="visualFeedback" class="feedback">{{ visualFeedback }}</p>
    </div>

    <div v-else-if="config.type === 'QUIZ'" class="quiz-stage">
      <header v-if="!isDiscreteContinuousQuiz" class="quiz-stage__header">
        <span>{{ (config as QuizConfig).title }}</span>
        <h3>{{ config.question }}</h3>
      </header>
      <div v-if="!isDiscreteContinuousQuiz" class="quiz-question-panel">
        <p>{{ (config as QuizConfig).question }}</p>
      </div>
      <div :class="isDiscreteContinuousQuiz ? 'quiz-custom-grid' : 'quiz-option-grid'">
        <button
          v-for="(option, index) in (config as QuizConfig).options"
          :key="option.id"
          type="button"
          :class="isDiscreteContinuousQuiz ? {
            'quiz-option-custom-card': true,
            'quiz-option-custom-card--selected': selectedAnswer === option.id,
            'quiz-option-custom-card--correct': checked && option.correct,
            'quiz-option-custom-card--wrong': checked && selectedAnswer === option.id && !option.correct,
          } : {
            'quiz-option': true,
            'quiz-option--selected': selectedAnswer === option.id,
            'quiz-option--correct': checked && option.correct,
            'quiz-option--wrong': checked && selectedAnswer === option.id && !option.correct,
          }"
          @click="chooseAnswer(option.id)"
        >
          <template v-if="isDiscreteContinuousQuiz && option.label.toLowerCase() === 'discrete'">
            <div class="custom-card-graphic discrete-graphic">
              <span class="word-count">
                COU<span class="highlight-nt">NT</span><span class="count-line">_</span><sup class="count-sup">123</sup>
              </span>
            </div>
            <h4 class="custom-card-title">Discrete</h4>
            <p class="custom-card-subtitle">COUNTABLE VALUES</p>
          </template>

          <template v-else-if="isDiscreteContinuousQuiz && option.label.toLowerCase() === 'continuous'">
            <div class="custom-card-graphic continuous-graphic">
              <div class="ruler-box">
                <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="ruler-icon-svg">
                  <rect x="2" y="7" width="20" height="10" rx="3" fill="#d0e1fd" stroke="currentColor" stroke-width="2" />
                  <path d="M6 10v4M10 10v4M14 10v4M18 10v4" />
                  <path d="M2 12h20" />
                </svg>
              </div>
            </div>
            <h4 class="custom-card-title">Continuous</h4>
            <p class="custom-card-subtitle">MEASURABLE SCALE</p>
          </template>

          <template v-else>
            <span class="quiz-option__badge">{{ quizOptionLetter(option, index) }}</span>
            <span>{{ option.label }}</span>
          </template>
        </button>
      </div>
      <p v-if="checked" class="quiz-feedback" :class="{ 'quiz-feedback--success': practicePassed, 'quiz-feedback--failure': !practicePassed }">
        {{ activeFeedback || (selectedOption?.correct ? 'Correct.' : 'Not quite.') }}
      </p>
      <footer class="quiz-action-row">
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
.quiz-custom-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1.25rem;
  width: 100%;
  margin-top: 0.5rem;
}
.quiz-option-custom-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 11rem;
  border: 1.5px solid #1a1814;
  border-radius: 16px;
  background: #ffffff;
  padding: 1.5rem;
  color: #1a1814;
  cursor: pointer;
  box-shadow: 3px 3px 0 #1a1814;
  transition: transform 0.15s ease, box-shadow 0.15s ease, background-color 0.15s ease;
}
.quiz-option-custom-card:hover {
  transform: translateY(-2px);
  box-shadow: 4px 4px 0 #1a1814;
}
.quiz-option-custom-card--selected {
  background: #fff4bf !important;
  border-color: #1a1814 !important;
}
.quiz-option-custom-card--correct {
  background: #dff4df !important;
  border-color: #245e3e !important;
  box-shadow: 3px 3px 0 #245e3e !important;
}
.quiz-option-custom-card--wrong {
  background: #f9d3c5 !important;
  border-color: #8c3322 !important;
  box-shadow: 3px 3px 0 #8c3322 !important;
}
.custom-card-graphic {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 4.5rem;
  margin-bottom: 0.5rem;
}
.discrete-graphic {
  font-family: 'Bricolage Grotesque', sans-serif;
  font-weight: 800;
  font-size: 1.5rem;
  letter-spacing: -0.02em;
}
.word-count {
  display: inline-flex;
  align-items: center;
}
.highlight-nt {
  background: #ffd333;
  border: 1.5px solid #1a1814;
  border-radius: 6px;
  padding: 0.1rem 0.35rem;
  margin-left: 0.1rem;
  box-shadow: 1px 1px 0 #1a1814;
}
.count-line {
  margin-left: 0.2rem;
  font-weight: 400;
}
.count-sup {
  font-size: 0.75rem;
  font-weight: 900;
  margin-left: 0.15rem;
  align-self: flex-start;
  margin-top: 0.2rem;
}
.ruler-box {
  display: flex;
  align-items: center;
  justify-content: center;
}
.ruler-icon-svg {
  color: #1a1814;
}
.custom-card-title {
  margin: 0;
  font-size: 1.15rem;
  font-weight: 900;
}
.custom-card-subtitle {
  margin: 0.25rem 0 0 0;
  color: #8f887e;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 10px;
  font-weight: 900;
  letter-spacing: 0.05em;
  text-transform: uppercase;
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
.quiz-check-button {
  margin-left: auto;
  background: #1a1814;
  color: #fffdf8;
}
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
.logic-input-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  align-items: center;
}
.logic-toggle,
.logic-output,
.logic-answer {
  display: grid;
  gap: 0.35rem;
  border: 2px solid #e4ded6;
  border-radius: 8px;
  background: #fffdf8;
  padding: 0.75rem;
  font-size: 13px;
  font-weight: 800;
}
.logic-toggle {
  grid-template-columns: auto auto auto;
  align-items: center;
}
.logic-output {
  min-width: 5.5rem;
  text-align: center;
}
.logic-answer input {
  width: min(100%, 32rem);
  border: 2px solid #1a1814;
  border-radius: 8px;
  background: #fbf7ef;
  padding: 0.65rem 0.75rem;
  font-family: Georgia, serif;
  font-size: 1rem;
  font-weight: 800;
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
  .quiz-check-button {
    width: 100%;
    margin-left: 0;
  }
}

@keyframes flow-forward {
  to {
    stroke-dashoffset: -28;
  }
}
@keyframes flow-backward {
  to {
    stroke-dashoffset: 28;
  }
}
.animate-flow-forward {
  animation: flow-forward 1.2s linear infinite;
}
.animate-flow-backward {
  animation: flow-backward 1.2s linear infinite;
}
</style>
