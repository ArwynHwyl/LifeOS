import type { InteractionType } from '@/features/courses/services/adminCourses'

export type TemplateInteractionType = 'GRAPH_2D' | 'FORMULA_EXPLORER' | 'VISUAL_LAYER' | 'QUIZ' | 'THREE_JS'
export type InteractiveMode = 'VISUALIZATION' | 'PRACTICE'
export type SuccessConditionKind = 'QUIZ_CORRECT_OPTION' | 'EXPRESSION_EQUALS' | 'POINT_ON_GRAPH' | 'TRANSFORM_MATCH'

export interface PracticeFeedback {
  success: string
  failure: string
}

export interface NumericControlConfig {
  min: number
  max: number
  step: number
  initial: number
}

export type TransformControlName = 'rotationX' | 'rotationY' | 'rotationZ' | 'scale'

export interface QuizCorrectOptionCondition {
  kind: 'QUIZ_CORRECT_OPTION'
  correctOptionId?: string
}

export interface ExpressionEqualsCondition {
  kind: 'EXPRESSION_EQUALS'
  target: number
  tolerance?: number
}

export interface PointOnGraphCondition {
  kind: 'POINT_ON_GRAPH'
  target: { x: number; y: number }
  tolerance?: number
}

export interface TransformMatchCondition {
  kind: 'TRANSFORM_MATCH'
  target: Partial<Record<TransformControlName, number>>
  tolerance?: number
}

export type SuccessCondition =
  | QuizCorrectOptionCondition
  | ExpressionEqualsCondition
  | PointOnGraphCondition
  | TransformMatchCondition

export interface PracticeConfigBase {
  mode: 'PRACTICE'
  prompt: string
  successCondition: SuccessCondition
  feedback: PracticeFeedback
}

export interface Graph2DConfig {
  type: 'GRAPH_2D'
  mode?: InteractiveMode
  title: string
  expression: string
  xMin: number
  xMax: number
  yMin: number
  yMax: number
  sampleCount: number
  controls?: Record<string, NumericControlConfig>
  prompt?: string
  successCondition?: PointOnGraphCondition
  feedback?: PracticeFeedback
}

export interface FormulaVariableConfig {
  name: string
  label: string
  min: number
  max: number
  step: number
  initial: number
}

export interface FormulaStepConfig {
  label: string
  expression: string
  explanation?: string
}

export interface FormulaOptionConfig {
  id: string
  label: string
  formula: string
  description?: string
  steps?: FormulaStepConfig[]
}

export interface FormulaExplorerConfig {
  type: 'FORMULA_EXPLORER'
  mode?: InteractiveMode
  title: string
  formula: string
  variables: FormulaVariableConfig[]
  precision: number
  formulaOptions?: FormulaOptionConfig[]
  prompt?: string
  successCondition?: ExpressionEqualsCondition
  feedback?: PracticeFeedback
}

export interface QuizOptionConfig {
  id: string
  label: string
  correct: boolean
}

export interface QuizConfig {
  type: 'QUIZ'
  mode?: InteractiveMode
  title: string
  question: string
  options: QuizOptionConfig[]
  explanation?: string
  prompt?: string
  successCondition?: QuizCorrectOptionCondition
  feedback?: PracticeFeedback
}

export interface ThreeJsConfig {
  type: 'THREE_JS'
  mode?: InteractiveMode
  title: string
  shape: 'cube' | 'sphere' | 'pyramid'
  color: string
  rotationSpeed?: number
  controls?: Partial<Record<TransformControlName, NumericControlConfig>>
  prompt?: string
  successCondition?: TransformMatchCondition
  feedback?: PracticeFeedback
}

export type VisualLayerShape = 'rectangle' | 'circle'
export type VisualLayerElementKind = 'button' | 'hotspot'
export type VisualLayerEffect = 'HIGHLIGHT_ZONE' | 'SHOW_FEEDBACK'

export interface VisualLayerZone {
  id: string
  label: string
  shape: VisualLayerShape
  x: number
  y: number
  width: number
  height: number
  labelX?: number
  labelY?: number
  color: string
  highlightColor?: string
  highlightOpacity?: number
  feedback?: string
}

export interface VisualLayerElement {
  id: string
  label: string
  kind: VisualLayerElementKind
  x: number
  y: number
  width: number
  height: number
}

export interface VisualLayerInteraction {
  triggerId: string
  effect: VisualLayerEffect
  targetZoneId?: string
  feedback?: string
}

export interface VisualLayerOverlapValue {
  id: string
  label: string
  zoneIds: string[]
  value: number
  feedback?: string
}

export interface VisualLayerOverlapInput {
  id: string
  label: string
  zoneIds: string[]
  value: number
  kind: 'total' | 'intersection'
}

export interface VisualLayerOverlapConfig {
  enabled: boolean
  sourceZoneIds: string[]
  inputs?: VisualLayerOverlapInput[]
  values: VisualLayerOverlapValue[]
}

export interface VisualLayerConfig {
  type: 'VISUAL_LAYER'
  mode?: InteractiveMode
  title: string
  prompt?: string
  canvas: {
    width: number
    height: number
    backgroundText?: string
  }
  zones: VisualLayerZone[]
  elements: VisualLayerElement[]
  interactions: VisualLayerInteraction[]
  overlap?: VisualLayerOverlapConfig
  feedback?: PracticeFeedback
}

export type InteractiveConfig = Graph2DConfig | FormulaExplorerConfig | VisualLayerConfig | QuizConfig | ThreeJsConfig

export interface InteractiveTemplate {
  type: TemplateInteractionType
  label: string
  description: string
  defaultConfig: InteractiveConfig
  visualizationDefaultConfig?: InteractiveConfig
  practiceDefaultConfig?: InteractiveConfig
  editableFields: Array<{
    path: string
    label: string
    inputType: string
    required: boolean
    min: number | null
    max: number | null
    maxLength: number | null
    options: string[] | null
  }>
}

export const TEMPLATE_TYPES: TemplateInteractionType[] = ['GRAPH_2D', 'FORMULA_EXPLORER', 'VISUAL_LAYER', 'QUIZ', 'THREE_JS']

export const DEFAULT_INTERACTIVE_CONFIGS: Record<TemplateInteractionType, InteractiveConfig> = {
  GRAPH_2D: {
    type: 'GRAPH_2D',
    mode: 'VISUALIZATION',
    title: 'Function graph',
    expression: 'sin(x)',
    xMin: -6.28,
    xMax: 6.28,
    yMin: -2,
    yMax: 2,
    sampleCount: 160,
  },
  FORMULA_EXPLORER: {
    type: 'FORMULA_EXPLORER',
    mode: 'VISUALIZATION',
    title: 'Area explorer',
    formula: 'width * height',
    variables: [
      { name: 'width', label: 'Width', min: 1, max: 20, step: 1, initial: 6 },
      { name: 'height', label: 'Height', min: 1, max: 20, step: 1, initial: 4 },
    ],
    precision: 2,
  },
  VISUAL_LAYER: {
    type: 'VISUAL_LAYER',
    mode: 'VISUALIZATION',
    title: 'Visual hotspot layer',
    canvas: {
      width: 900,
      height: 520,
      backgroundText: 'ลากวาด zone บนพื้นที่นี้ แล้วสร้างปุ่ม/ hotspot เพื่อสั่ง highlight',
    },
    zones: [
      { id: 'zone_a', label: 'Zone A', shape: 'circle', x: 270, y: 150, width: 220, height: 220, color: '#ffd333', highlightColor: '#ff8f1f', highlightOpacity: 0.82, feedback: 'Zone A highlighted.' },
      { id: 'zone_b', label: 'Zone B', shape: 'circle', x: 410, y: 150, width: 220, height: 220, color: '#8fb3ff', highlightColor: '#4f8cff', highlightOpacity: 0.82, feedback: 'Zone B highlighted.' },
    ],
    elements: [
      { id: 'choice_a', label: 'Highlight A', kind: 'button', x: 40, y: 40, width: 150, height: 48 },
    ],
    interactions: [
      { triggerId: 'choice_a', effect: 'HIGHLIGHT_ZONE', targetZoneId: 'zone_a', feedback: 'Zone A highlighted.' },
    ],
  },
  QUIZ: {
    type: 'QUIZ',
    mode: 'PRACTICE',
    title: 'Quick check',
    prompt: 'Choose the value that satisfies the equation.',
    question: 'Which value makes 2x + 5 = 13 true?',
    options: [
      { id: 'a', label: 'x = 3', correct: false },
      { id: 'b', label: 'x = 4', correct: true },
      { id: 'c', label: 'x = 6', correct: false },
    ],
    explanation: 'Subtract 5 from both sides, then divide by 2.',
    successCondition: { kind: 'QUIZ_CORRECT_OPTION' },
    feedback: { success: 'Correct.', failure: 'Not quite. Try solving for x first.' },
  },
  THREE_JS: {
    type: 'THREE_JS',
    mode: 'VISUALIZATION',
    title: 'Rotating cube',
    shape: 'cube',
    color: '#4f8cff',
    rotationSpeed: 0.8,
  },
}

export const PRACTICE_DEFAULT_CONFIGS: Record<TemplateInteractionType, InteractiveConfig> = {
  GRAPH_2D: {
    type: 'GRAPH_2D',
    mode: 'PRACTICE',
    title: 'Match the line',
    prompt: 'Adjust the slope until the graph passes through the target point.',
    expression: 'm * x',
    controls: { m: { min: 0, max: 5, step: 0.5, initial: 1 } },
    xMin: 0,
    xMax: 5,
    yMin: 0,
    yMax: 10,
    sampleCount: 120,
    successCondition: { kind: 'POINT_ON_GRAPH', target: { x: 2, y: 6 }, tolerance: 0.15 },
    feedback: { success: 'Correct. The curve reaches the target point.', failure: 'Not yet. Adjust the controls and compare the curve to the target.' },
  },
  FORMULA_EXPLORER: {
    type: 'FORMULA_EXPLORER',
    mode: 'PRACTICE',
    title: 'Target magnitude',
    prompt: 'Adjust x and y until the magnitude equals 5.',
    formula: 'sqrt(x^2 + y^2)',
    variables: [
      { name: 'x', label: 'X component', min: 0, max: 10, step: 1, initial: 0 },
      { name: 'y', label: 'Y component', min: 0, max: 10, step: 1, initial: 0 },
    ],
    precision: 2,
    successCondition: { kind: 'EXPRESSION_EQUALS', target: 5, tolerance: 0.01 },
    feedback: { success: 'Correct. The magnitude is 5.', failure: 'Not yet. Look for a Pythagorean triple.' },
  },
  VISUAL_LAYER: {
    type: 'VISUAL_LAYER',
    mode: 'PRACTICE',
    title: 'Build the Venn diagram',
    prompt: 'Add the required circles, arrange the overlaps, then enter the value for each visible region.',
    canvas: {
      width: 900,
      height: 520,
      backgroundText: '',
    },
    zones: [
      { id: 'zone_a', label: 'A', shape: 'circle', x: 250, y: 130, width: 260, height: 260, labelX: 35, labelY: 30, color: '#ffd333', highlightColor: '#ff8f1f', highlightOpacity: 0.82, feedback: '' },
      { id: 'zone_b', label: 'B', shape: 'circle', x: 390, y: 130, width: 260, height: 260, labelX: 65, labelY: 30, color: '#8fb3ff', highlightColor: '#4f8cff', highlightOpacity: 0.82, feedback: '' },
      { id: 'zone_c', label: 'C', shape: 'circle', x: 320, y: 250, width: 260, height: 260, labelX: 50, labelY: 75, color: '#8fe0aa', highlightColor: '#3aa66b', highlightOpacity: 0.82, feedback: '' },
    ],
    elements: [],
    interactions: [],
    overlap: ensureVisualOverlapValues({
      type: 'VISUAL_LAYER',
      mode: 'PRACTICE',
      title: 'Build the Venn diagram',
      canvas: { width: 900, height: 520, backgroundText: '' },
      zones: [
        { id: 'zone_a', label: 'A', shape: 'circle', x: 250, y: 130, width: 260, height: 260, color: '#ffd333' },
        { id: 'zone_b', label: 'B', shape: 'circle', x: 390, y: 130, width: 260, height: 260, color: '#8fb3ff' },
        { id: 'zone_c', label: 'C', shape: 'circle', x: 320, y: 250, width: 260, height: 260, color: '#8fe0aa' },
      ],
      elements: [],
      interactions: [],
    }, ['zone_a', 'zone_b', 'zone_c']),
    feedback: { success: 'Correct. The regions match the expected values.', failure: 'Not yet. Check that every required overlap exists and each region value is correct.' },
  },
  QUIZ: DEFAULT_INTERACTIVE_CONFIGS.QUIZ,
  THREE_JS: {
    type: 'THREE_JS',
    mode: 'PRACTICE',
    title: 'Match the target rotation',
    prompt: 'Rotate the object until it matches the target orientation.',
    shape: 'cube',
    color: '#4f8cff',
    controls: {
      rotationX: { min: 0, max: 180, step: 15, initial: 0 },
      rotationY: { min: 0, max: 180, step: 15, initial: 0 },
      rotationZ: { min: 0, max: 180, step: 15, initial: 0 },
    },
    successCondition: { kind: 'TRANSFORM_MATCH', target: { rotationX: 45, rotationY: 90, rotationZ: 0 }, tolerance: 5 },
    feedback: { success: 'Correct. The object matches the target orientation.', failure: 'Not yet. Compare the current orientation with the target.' },
  },
}

export function isTemplateInteractionType(value: InteractionType): value is TemplateInteractionType {
  return TEMPLATE_TYPES.includes(value as TemplateInteractionType)
}

export function cloneDefaultConfig(type: TemplateInteractionType): InteractiveConfig {
  return JSON.parse(JSON.stringify(DEFAULT_INTERACTIVE_CONFIGS[type])) as InteractiveConfig
}

export function clonePracticeDefaultConfig(type: TemplateInteractionType): InteractiveConfig {
  return JSON.parse(JSON.stringify(PRACTICE_DEFAULT_CONFIGS[type])) as InteractiveConfig
}

export function parseInteractiveConfig(type: InteractionType, raw: string | null | undefined): InteractiveConfig | null {
  if (!isTemplateInteractionType(type) || !raw?.trim()) return null
  try {
    const parsed = JSON.parse(raw) as InteractiveConfig
    if (parsed.type !== type) return null
    return { mode: 'VISUALIZATION', ...parsed } as InteractiveConfig
  } catch {
    return null
  }
}

export function stringifyInteractiveConfig(config: InteractiveConfig) {
  return JSON.stringify(config, null, 2)
}

export function configFromTypeAndRaw(type: InteractionType, raw: string | null | undefined) {
  if (!isTemplateInteractionType(type)) return null
  return parseInteractiveConfig(type, raw) ?? cloneDefaultConfig(type)
}

export interface GeneratedOverlapRegion extends VisualLayerOverlapValue {
  zones: VisualLayerZone[]
  excludedZones: VisualLayerZone[]
  center: { x: number; y: number }
  maskPath: string
}

export function overlapIdToken(zoneId: string) {
  const stripped = zoneId.replace(/^zone[_-]?/i, '')
  const token = stripped.replace(/[^A-Za-z0-9]+/g, '_').replace(/^_+|_+$/g, '').toUpperCase()
  return token || zoneId.replace(/[^A-Za-z0-9]+/g, '_').toUpperCase()
}

export function overlapRegionId(zoneIds: string[]) {
  const tokens = [...zoneIds].sort().map(overlapIdToken)
  return tokens.length === 1 ? `${tokens[0]}_ONLY` : tokens.join('_AND_')
}

export function overlapRegionLabel(zoneIds: string[], zones: VisualLayerZone[]) {
  const labels = [...zoneIds].sort().map((zoneId) => zones.find((zone) => zone.id === zoneId)?.label || overlapIdToken(zoneId))
  return labels.length === 1 ? `${labels[0]} only` : labels.join(' ∩ ')
}

export function overlapInputLabel(zoneIds: string[], zones: VisualLayerZone[]) {
  return [...zoneIds].sort().map((zoneId) => zones.find((zone) => zone.id === zoneId)?.label || overlapIdToken(zoneId)).join(' ∩ ')
}

export function exactOverlapRegionLabel(zoneIds: string[], zones: VisualLayerZone[], sourceCount: number) {
  const base = overlapInputLabel(zoneIds, zones)
  if (zoneIds.length === 1) return `${base} only`
  return zoneIds.length < sourceCount ? `${base} only` : base
}

export function overlapCombinations(sourceZoneIds: string[]) {
  const ids = [...sourceZoneIds].sort()
  const combinations: string[][] = []
  for (let mask = 1; mask < (1 << ids.length); mask += 1) {
    combinations.push(ids.filter((_, index) => (mask & (1 << index)) !== 0))
  }
  return combinations
}

function sameOverlapSet(left: string[], right: string[]) {
  return left.length === right.length && left.every((zoneId) => right.includes(zoneId))
}

function isSubsetOverlapSet(subset: string[], superset: string[]) {
  return subset.every((zoneId) => superset.includes(zoneId))
}

function legacyExactToInclusiveInputs(values: VisualLayerOverlapValue[], combinations: string[][], zones: VisualLayerZone[]): VisualLayerOverlapInput[] {
  return combinations.map((zoneIds) => {
    const inclusiveValue = values
      .filter((value) => isSubsetOverlapSet(zoneIds, value.zoneIds))
      .reduce((sum, value) => sum + (Number.isFinite(value.value) ? Number(value.value) : 0), 0)
    return {
      id: overlapRegionId(zoneIds),
      label: overlapInputLabel(zoneIds, zones),
      zoneIds,
      value: inclusiveValue,
      kind: zoneIds.length === 1 ? 'total' : 'intersection',
    }
  })
}

export function deriveExactOverlapValues(
  inputs: VisualLayerOverlapInput[],
  sourceZoneIds: string[],
  zones: VisualLayerZone[],
  existingValues: VisualLayerOverlapValue[] = [],
) {
  const combinations = overlapCombinations(sourceZoneIds)
  const inputFor = (zoneIds: string[]) => inputs.find((input) => sameOverlapSet(input.zoneIds, zoneIds))
  return combinations.map((zoneIds) => {
    const id = overlapRegionId(zoneIds)
    const exactValue = combinations
      .filter((candidate) => isSubsetOverlapSet(zoneIds, candidate))
      .reduce((sum, candidate) => {
        const input = inputFor(candidate)
        const value = Number.isFinite(input?.value) ? Number(input?.value) : 0
        const sign = (candidate.length - zoneIds.length) % 2 === 0 ? 1 : -1
        return sum + sign * value
      }, 0)
    const saved = existingValues.find((value) => value.id === id)
    return {
      id,
      label: saved?.label || exactOverlapRegionLabel(zoneIds, zones, sourceZoneIds.length),
      zoneIds,
      value: exactValue,
      feedback: saved?.feedback ?? '',
    }
  })
}

export function ensureVisualOverlapValues(config: VisualLayerConfig, sourceZoneIds = config.overlap?.sourceZoneIds ?? config.zones.slice(0, 5).map((zone) => zone.id)) {
  const sources = sourceZoneIds.filter((zoneId, index, all) => all.indexOf(zoneId) === index && config.zones.some((zone) => zone.id === zoneId)).slice(0, 5)
  const combinations = overlapCombinations(sources)
  const existingInputs = config.overlap?.inputs?.length
    ? config.overlap.inputs
    : legacyExactToInclusiveInputs(config.overlap?.values ?? [], combinations, config.zones)
  const inputs = combinations.map((zoneIds) => {
    const id = overlapRegionId(zoneIds)
    const saved = existingInputs.find((input) => input.id === id || sameOverlapSet(input.zoneIds, zoneIds))
    return {
      id,
      label: saved?.label || overlapInputLabel(zoneIds, config.zones),
      zoneIds,
      value: Number.isFinite(saved?.value) ? Number(saved?.value) : 0,
      kind: zoneIds.length === 1 ? 'total' as const : 'intersection' as const,
    }
  })
  return {
    enabled: true,
    sourceZoneIds: sources,
    inputs,
    values: deriveExactOverlapValues(inputs, sources, config.zones, config.overlap?.values ?? []),
  }
}

export function pointInVisualZone(point: { x: number; y: number }, zone: VisualLayerZone) {
  if (zone.shape === 'rectangle') {
    return point.x >= zone.x && point.x <= zone.x + zone.width && point.y >= zone.y && point.y <= zone.y + zone.height
  }
  const radiusX = zone.width / 2
  const radiusY = zone.height / 2
  const centerX = zone.x + radiusX
  const centerY = zone.y + radiusY
  const dx = (point.x - centerX) / radiusX
  const dy = (point.y - centerY) / radiusY
  return dx * dx + dy * dy <= 1
}

export function overlapRegionCenter(regionZoneIds: string[], sourceZones: VisualLayerZone[]) {
  const included = sourceZones.filter((zone) => regionZoneIds.includes(zone.id))
  const excluded = sourceZones.filter((zone) => !regionZoneIds.includes(zone.id))
  const bounds = included.reduce(
    (next, zone) => ({
      left: Math.max(next.left, zone.x),
      top: Math.max(next.top, zone.y),
      right: Math.min(next.right, zone.x + zone.width),
      bottom: Math.min(next.bottom, zone.y + zone.height),
    }),
    { left: -Infinity, top: -Infinity, right: Infinity, bottom: Infinity },
  )
  const fallback = included.reduce(
    (sum, zone) => ({ x: sum.x + zone.x + zone.width / 2, y: sum.y + zone.y + zone.height / 2 }),
    { x: 0, y: 0 },
  )
  const fallbackPoint = { x: fallback.x / included.length, y: fallback.y / included.length }
  if (!Number.isFinite(bounds.left) || bounds.left >= bounds.right || bounds.top >= bounds.bottom) return fallbackPoint
  const step = Math.max(8, Math.min(bounds.right - bounds.left, bounds.bottom - bounds.top) / 10)
  const matches: Array<{ x: number; y: number }> = []
  for (let y = bounds.top; y <= bounds.bottom; y += step) {
    for (let x = bounds.left; x <= bounds.right; x += step) {
      const point = { x, y }
      if (included.every((zone) => pointInVisualZone(point, zone)) && excluded.every((zone) => !pointInVisualZone(point, zone))) {
        matches.push(point)
      }
    }
  }
  if (!matches.length) return fallbackPoint
  const total = matches.reduce((sum, point) => ({ x: sum.x + point.x, y: sum.y + point.y }), { x: 0, y: 0 })
  return { x: total.x / matches.length, y: total.y / matches.length }
}

export function overlapRegionMaskPath(regionZoneIds: string[], sourceZones: VisualLayerZone[], canvas: { width: number; height: number }) {
  const included = sourceZones.filter((zone) => regionZoneIds.includes(zone.id))
  const excluded = sourceZones.filter((zone) => !regionZoneIds.includes(zone.id))
  if (!included.length) return ''
  const bounds = sourceZones.reduce(
    (next, zone) => ({
      left: Math.min(next.left, zone.x),
      top: Math.min(next.top, zone.y),
      right: Math.max(next.right, zone.x + zone.width),
      bottom: Math.max(next.bottom, zone.y + zone.height),
    }),
    { left: canvas.width, top: canvas.height, right: 0, bottom: 0 },
  )
  const cell = 6
  const left = Math.max(0, Math.floor(bounds.left / cell) * cell)
  const top = Math.max(0, Math.floor(bounds.top / cell) * cell)
  const right = Math.min(canvas.width, Math.ceil(bounds.right / cell) * cell)
  const bottom = Math.min(canvas.height, Math.ceil(bounds.bottom / cell) * cell)
  const parts: string[] = []
  for (let y = top; y < bottom; y += cell) {
    for (let x = left; x < right; x += cell) {
      const width = Math.min(cell, right - x)
      const height = Math.min(cell, bottom - y)
      const point = { x: x + width / 2, y: y + height / 2 }
      if (included.every((zone) => pointInVisualZone(point, zone)) && excluded.every((zone) => !pointInVisualZone(point, zone))) {
        parts.push(`M${x} ${y}h${width}v${height}h-${width}z`)
      }
    }
  }
  return parts.join('')
}

export function generatedOverlapRegions(config: VisualLayerConfig): GeneratedOverlapRegion[] {
  if (!config.overlap?.enabled) return []
  const values = config.overlap.inputs?.length
    ? deriveExactOverlapValues(config.overlap.inputs, config.overlap.sourceZoneIds, config.zones, config.overlap.values)
    : config.overlap.values
  const sourceZones = config.overlap.sourceZoneIds
    .map((zoneId) => config.zones.find((zone) => zone.id === zoneId))
    .filter((zone): zone is VisualLayerZone => Boolean(zone))
    .slice(0, 5)
  const maskPathById = new Map(values.map((value) => [
    value.id,
    overlapRegionMaskPath(value.zoneIds, sourceZones, config.canvas),
  ]))
  const totalInputFor = (zoneId: string) => config.overlap?.inputs?.find((input) => sameOverlapSet(input.zoneIds, [zoneId]))
  const isIsolatedSingleZone = (zoneId: string) => !values.some((value) => (
    value.zoneIds.length > 1
    && value.zoneIds.includes(zoneId)
    && Boolean(maskPathById.get(value.id))
  ))
  return values.map((value) => {
    const zones = value.zoneIds
      .map((zoneId) => sourceZones.find((zone) => zone.id === zoneId))
      .filter((zone): zone is VisualLayerZone => Boolean(zone))
    const displayValue = value.zoneIds.length === 1 && isIsolatedSingleZone(value.zoneIds[0])
      ? totalInputFor(value.zoneIds[0])?.value ?? value.value
      : value.value
    return {
      ...value,
      value: displayValue,
      zones,
      excludedZones: sourceZones.filter((zone) => !value.zoneIds.includes(zone.id)),
      center: overlapRegionCenter(value.zoneIds, sourceZones),
      maskPath: maskPathById.get(value.id) ?? '',
    }
  }).filter((region) => region.zones.length === region.zoneIds.length && region.maskPath)
}
