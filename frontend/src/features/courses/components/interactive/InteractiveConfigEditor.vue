<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import {
  listInteractiveTemplates,
  type InteractionType,
} from '@/features/courses/services/adminCourses'
import InteractiveChallengeShell from '@/features/courses/components/interactive/InteractiveChallengeShell.vue'
import InteractivePreview from '@/features/courses/components/interactive/InteractivePreview.vue'
import type { InteractiveProgressStatus } from '@/features/learning/services/learnerCourses'
import {
  DEFAULT_INTERACTIVE_CONFIGS,
  TEMPLATE_TYPES,
  cloneDefaultConfig,
  clonePracticeDefaultConfig,
  configFromTypeAndRaw,
  ensureVisualOverlapValues,
  generatedOverlapRegions,
  isTemplateInteractionType,
  normalizeInteractiveConfig,
  stringifyInteractiveConfig,
  type FormulaExplorerConfig,
  type FormulaOptionConfig,
  type FormulaStepConfig,
  type InteractiveConfig,
  type InteractiveTemplate,
  type QuizConfig,
  type TemplateInteractionType,
  type VisualLayerConfig,
  type VisualLayerElement,
  type VisualLayerInteraction,
  type VisualLayerOverlapInput,
  type VisualLayerOverlapValue,
  type VisualLayerZone,
} from '@/features/courses/types/interactive'

const props = defineProps<{
  interactionType: InteractionType
  interactionPrompt: string
  interactionConfig: string
}>()

const emit = defineEmits<{
  'update:interactionType': [value: InteractionType]
  'update:interactionPrompt': [value: string]
  'update:interactionConfig': [value: string]
  error: [message: string]
}>()

const templates = ref<InteractiveTemplate[]>([])
const config = ref<InteractiveConfig | null>(configFromTypeAndRaw(props.interactionType, props.interactionConfig))
const studioTabs = ['Design', 'Preview', 'JSON'] as const
const activeStudioTab = ref<'Design' | 'Preview' | 'JSON'>('Design')
const simulatedProgressStatus = ref<InteractiveProgressStatus>('NOT_STARTED')
const jsonDraft = ref(config.value ? stringifyInteractiveConfig(config.value) : '')
const jsonError = ref('')
const selectedFormulaSectionIndex = ref(0)
const visualTool = ref<'select' | 'zone-rectangle' | 'zone-circle' | 'button' | 'hotspot'>('select')
const selectedVisualKind = ref<'zone' | 'element' | 'overlap' | null>(null)
const selectedVisualId = ref<string | null>(null)
type VisualSelectionItem = { kind: 'zone' | 'element'; id: string }
type VisualBoundsDraft = {
  x: number
  y: number
  width: number
  height: number
  labelX?: number
  labelY?: number
}
const visualEditDraft = ref<{
  kind: 'zone' | 'element'
  id: string
  original: VisualBoundsDraft
  draft: VisualBoundsDraft
} | null>(null)
const selectedVisualGroup = ref<VisualSelectionItem[]>([])
const visualGroupEditDraft = ref<{
  original: Record<string, VisualBoundsDraft>
  draft: Record<string, VisualBoundsDraft>
} | null>(null)
const visualToolbarDismissed = ref(false)
const visualMarquee = ref<{
  originX: number
  originY: number
  x: number
  y: number
  width: number
  height: number
} | null>(null)
const visualDraft = ref<{
  kind: 'zone' | 'element'
  shape?: VisualLayerZone['shape']
  elementKind?: VisualLayerElement['kind']
  originX: number
  originY: number
  x: number
  y: number
  width: number
  height: number
} | null>(null)
const visualDrag = ref<{
  kind: 'zone' | 'element' | 'zone-label' | 'resize' | 'group'
  id: string
  offsetX: number
  offsetY: number
  resizeHandle?: VisualResizeHandle
  start?: VisualBoundsDraft
  originPoint?: { x: number; y: number }
  groupStart?: Record<string, VisualBoundsDraft>
} | null>(null)
type VisualResizeHandle = 'nw' | 'n' | 'ne' | 'e' | 'se' | 's' | 'sw' | 'w'
const visualResizeHandles: VisualResizeHandle[] = ['nw', 'n', 'ne', 'e', 'se', 's', 'sw', 'w']
const jsonStats = computed(() => {
  const lines = jsonDraft.value ? jsonDraft.value.split('\n').length : 0
  const chars = jsonDraft.value.length
  return `${lines} lines / ${chars.toLocaleString()} chars`
})

const templateType = computed({
  get: () => props.interactionType,
  set: (value: InteractionType) => {
    emit('update:interactionType', value)
    if (isTemplateInteractionType(value)) {
      setConfig(cloneDefaultConfig(value))
    } else {
      config.value = null
      jsonDraft.value = ''
      emit('update:interactionConfig', '')
    }
  },
})

const prompt = computed({
  get: () => props.interactionPrompt,
  set: (value: string) => emit('update:interactionPrompt', value),
})

const templateChoices = computed(() => {
  if (templates.value.length) return templates.value
  return TEMPLATE_TYPES.map((type) => ({
    type,
    label: labelFor(type),
    description: '',
    defaultConfig: DEFAULT_INTERACTIVE_CONFIGS[type],
    editableFields: [],
  }))
})

const templateCards = computed(() => [
  { type: 'GRAPH_2D' as const, title: 'Graph', description: 'Plot expressions and create point-matching practice.' },
  { type: 'FORMULA_EXPLORER' as const, title: 'Formula', description: 'Let learners adjust variables and inspect results.' },
  { type: 'VISUAL_LAYER' as const, title: 'Set / Diagram Builder', description: 'Create Venn, set, and hotspot diagram activities.' },
  { type: 'QUIZ' as const, title: 'Quiz', description: 'Build a quick multiple-choice check.' },
])

const presetCards = computed(() => [
  { id: 'venn-2', title: '2-set Venn practice', type: 'VISUAL_LAYER' as const, config: twoSetVennPreset },
  { id: 'venn-3', title: '3-set Venn practice', type: 'VISUAL_LAYER' as const, config: threeSetVennPreset },
  { id: 'hotspot', title: 'Hotspot diagram', type: 'VISUAL_LAYER' as const, config: hotspotPreset },
  { id: 'formula-target', title: 'Formula target practice', type: 'FORMULA_EXPLORER' as const, config: formulaTargetPreset },
  { id: 'graph-point', title: 'Graph point match', type: 'GRAPH_2D' as const, config: graphPointPreset },
  { id: 'quick-quiz', title: 'Quick quiz', type: 'QUIZ' as const, config: quickQuizPreset },
])

const previewObjective = computed(() => {
  if (prompt.value.trim()) return prompt.value
  return 'Complete this activity to master the concept.'
})

const selectedFormulaOption = computed(() => {
  if (config.value?.type !== 'FORMULA_EXPLORER') return null
  return config.value.formulaOptions?.[selectedFormulaSectionIndex.value] ?? null
})

const visualOverlapRegions = computed(() => {
  const current = visualConfig()
  return current ? generatedOverlapRegions(current) : []
})
const visualMaskPrefix = `admin-overlap-${Math.random().toString(36).slice(2)}`
const visualZonesForRender = computed(() => {
  const current = visualConfig()
  if (!current) return []
  return current.zones.map((zone) => ({ ...zone, ...(draftForVisual('zone', zone.id) ?? {}), ...(groupDraftForVisual('zone', zone.id) ?? {}) }))
})
const visualElementsForRender = computed(() => {
  const current = visualConfig()
  if (!current) return []
  return current.elements.map((element) => ({ ...element, ...(draftForVisual('element', element.id) ?? {}), ...(groupDraftForVisual('element', element.id) ?? {}) }))
})
const visualOverlapRegionsForRender = computed(() => {
  const current = visualConfig()
  if (!current) return []
  return generatedOverlapRegions({ ...current, zones: visualZonesForRender.value })
})
const selectedVisualDraftBounds = computed(() => {
  if (selectedVisualGroup.value.length > 1) {
    if (visualToolbarDismissed.value) return null
    return visualGroupBounds()
  }
  const draft = visualEditDraft.value
  if (draft && draft.kind === selectedVisualKind.value && draft.id === selectedVisualId.value) return draft.draft
  if (visualToolbarDismissed.value) return null
  const current = visualConfig()
  if (!current || !selectedVisualId.value) return null
  if (selectedVisualKind.value === 'zone') {
    const zone = current.zones.find((item) => item.id === selectedVisualId.value)
    return zone ? boundsFromVisualItem(zone) : null
  }
  if (selectedVisualKind.value === 'element') {
    const element = current.elements.find((item) => item.id === selectedVisualId.value)
    return element ? boundsFromVisualItem(element) : null
  }
  return null
})

watch(
  () => [props.interactionType, props.interactionConfig] as const,
  ([type, raw]) => {
    if (type === 'OTHER') {
      config.value = null
      if (raw !== jsonDraft.value) jsonDraft.value = raw ?? ''
      return
    }
    const next = configFromTypeAndRaw(type, raw)
    if (!next) {
      config.value = null
      jsonDraft.value = ''
      return
    }
    if (JSON.stringify(next) !== JSON.stringify(config.value)) {
      config.value = next
      jsonDraft.value = stringifyInteractiveConfig(next)
    }
  },
)

watch(
  () => config.value?.type === 'FORMULA_EXPLORER' ? (config.value.formulaOptions?.length ?? 0) : 0,
  (length) => {
    if (!length) {
      selectedFormulaSectionIndex.value = 0
      return
    }
    selectedFormulaSectionIndex.value = Math.min(selectedFormulaSectionIndex.value, length - 1)
  },
)

onMounted(async () => {
  window.addEventListener('keydown', handleVisualEditKeydown)
  try {
    templates.value = await listInteractiveTemplates()
  } catch {
    templates.value = []
  }
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleVisualEditKeydown)
})

function setConfig(next: InteractiveConfig) {
  const normalized = normalizeInteractiveConfig(next)
  config.value = normalized
  jsonError.value = ''
  jsonDraft.value = stringifyInteractiveConfig(normalized)
  emit('update:interactionConfig', jsonDraft.value)
}

function selectTemplateCard(type: TemplateInteractionType | 'NONE' | 'OTHER') {
  emit('update:interactionType', type)
  if (type === 'NONE') {
    config.value = null
    jsonDraft.value = ''
    emit('update:interactionConfig', '')
    return
  }
  if (type === 'OTHER') {
    config.value = null
    jsonDraft.value = props.interactionConfig ?? ''
    return
  }
  setConfig(cloneDefaultConfig(type))
}

function applyPreset(preset: { type: TemplateInteractionType; config: () => InteractiveConfig }) {
  const next = preset.config()
  if (config.value?.type === next.type) {
    next.title = config.value.title
  }
  emit('update:interactionType', preset.type)
  setConfig(next)
}

function twoSetVennPreset(): InteractiveConfig {
  const config: VisualLayerConfig = {
    type: 'VISUAL_LAYER',
    mode: 'PRACTICE',
    title: 'Build the Venn diagram',
    canvas: { width: 900, height: 520, backgroundText: '' },
    zones: [
      { id: 'zone_a', label: 'A', shape: 'circle', x: 250, y: 140, width: 280, height: 280, labelX: 34, labelY: 28, color: '#ffd333', highlightColor: '#ff8f1f', highlightOpacity: 0.82 },
      { id: 'zone_b', label: 'B', shape: 'circle', x: 390, y: 140, width: 280, height: 280, labelX: 66, labelY: 28, color: '#8fb3ff', highlightColor: '#4f8cff', highlightOpacity: 0.82 },
    ],
    elements: [],
    interactions: [],
    feedback: { success: 'Correct. The exact Venn regions match.', failure: 'Not yet. Check totals and the intersection.' },
  }
  return { ...config, overlap: ensureVisualOverlapValues({ ...config, overlap: { enabled: true, sourceZoneIds: ['zone_a', 'zone_b'], inputs: [
    { id: 'A_ONLY', label: 'A', zoneIds: ['zone_a'], value: 11, kind: 'total' },
    { id: 'B_ONLY', label: 'B', zoneIds: ['zone_b'], value: 9, kind: 'total' },
    { id: 'A_AND_B', label: 'A intersect B', zoneIds: ['zone_a', 'zone_b'], value: 3, kind: 'intersection' },
  ], values: [] } }, ['zone_a', 'zone_b']) }
}

function threeSetVennPreset(): InteractiveConfig {
  return clonePracticeDefaultConfig('VISUAL_LAYER')
}

function hotspotPreset(): InteractiveConfig {
  return {
    type: 'VISUAL_LAYER',
    mode: 'VISUALIZATION',
    title: 'Hotspot diagram',
    canvas: { width: 900, height: 520, backgroundText: 'Click a label to highlight the matching region.' },
    zones: [
      { id: 'zone_input', label: 'Input', shape: 'rectangle', x: 90, y: 170, width: 190, height: 120, color: '#ffd333', highlightColor: '#ff8f1f', highlightOpacity: 0.82, feedback: 'Inputs are the values supplied to the process.' },
      { id: 'zone_process', label: 'Process', shape: 'rectangle', x: 355, y: 150, width: 190, height: 160, color: '#8fb3ff', highlightColor: '#4f8cff', highlightOpacity: 0.82, feedback: 'The process transforms inputs into outputs.' },
      { id: 'zone_output', label: 'Output', shape: 'rectangle', x: 620, y: 170, width: 190, height: 120, color: '#8fe0aa', highlightColor: '#3aa66b', highlightOpacity: 0.82, feedback: 'Outputs are the results produced by the process.' },
    ],
    elements: [
      { id: 'choice_input', label: 'Input', kind: 'button', x: 110, y: 365, width: 150, height: 48 },
      { id: 'choice_process', label: 'Process', kind: 'button', x: 375, y: 365, width: 150, height: 48 },
      { id: 'choice_output', label: 'Output', kind: 'button', x: 640, y: 365, width: 150, height: 48 },
    ],
    interactions: [
      { triggerId: 'choice_input', effect: 'HIGHLIGHT_ZONE', targetZoneId: 'zone_input', feedback: 'Inputs start the flow.' },
      { triggerId: 'choice_process', effect: 'HIGHLIGHT_ZONE', targetZoneId: 'zone_process', feedback: 'Processing applies the rule.' },
      { triggerId: 'choice_output', effect: 'HIGHLIGHT_ZONE', targetZoneId: 'zone_output', feedback: 'Outputs finish the flow.' },
    ],
  }
}

function formulaTargetPreset(): InteractiveConfig {
  return clonePracticeDefaultConfig('FORMULA_EXPLORER')
}

function graphPointPreset(): InteractiveConfig {
  return clonePracticeDefaultConfig('GRAPH_2D')
}

function quickQuizPreset(): InteractiveConfig {
  return clonePracticeDefaultConfig('QUIZ')
}

function patchConfig(patch: Partial<InteractiveConfig>) {
  if (!config.value) return
  setConfig({ ...config.value, ...patch } as InteractiveConfig)
}

function setMode(mode: 'VISUALIZATION' | 'PRACTICE') {
  if (!isTemplateInteractionType(props.interactionType)) return
  if (props.interactionType === 'QUIZ') {
    setConfig(clonePracticeDefaultConfig('QUIZ'))
    return
  }
  if (mode === 'PRACTICE') {
    const current = config.value
    const next = clonePracticeDefaultConfig(props.interactionType)
    if (current?.type === next.type) {
      setConfig({ ...next, title: current.title } as InteractiveConfig)
    } else {
      setConfig(next)
    }
    return
  }
  const current = config.value
  const next = cloneDefaultConfig(props.interactionType)
  if (current?.type === next.type) {
    setConfig({ ...next, title: current.title } as InteractiveConfig)
  } else {
    setConfig(next)
  }
}

function applyAdvancedJson() {
  jsonError.value = ''
  if (!isTemplateInteractionType(props.interactionType)) {
    if (jsonDraft.value.trim()) {
      try {
        JSON.parse(jsonDraft.value)
      } catch {
        jsonError.value = 'Interaction config must be valid JSON.'
        emit('error', jsonError.value)
        return
      }
    }
    emit('update:interactionConfig', jsonDraft.value.trim())
    return
  }
  try {
    const parsed = JSON.parse(jsonDraft.value) as InteractiveConfig
    if (parsed.type !== props.interactionType) {
      jsonError.value = 'JSON type must match the selected template.'
      emit('error', jsonError.value)
      return
    }
    setConfig(parsed)
  } catch {
    jsonError.value = 'Interaction config must be valid JSON.'
    emit('error', jsonError.value)
  }
}

function formatAdvancedJson() {
  jsonError.value = ''
  if (!jsonDraft.value.trim()) return
  try {
    jsonDraft.value = JSON.stringify(JSON.parse(jsonDraft.value), null, 2)
  } catch {
    jsonError.value = 'Fix JSON syntax before formatting.'
    emit('error', jsonError.value)
  }
}

function resetAdvancedJson() {
  jsonError.value = ''
  jsonDraft.value = config.value ? stringifyInteractiveConfig(config.value) : ''
}

function addVariable() {
  if (config.value?.type !== 'FORMULA_EXPLORER') return
  const nextIndex = config.value.variables.length + 1
  setConfig({
    ...config.value,
    variables: [
      ...config.value.variables,
      { name: `v${nextIndex}`, label: `Variable ${nextIndex}`, min: 0, max: 10, step: 1, initial: 1 },
    ],
  })
}

function updateVariable(index: number, patch: Partial<FormulaExplorerConfig['variables'][number]>) {
  if (config.value?.type !== 'FORMULA_EXPLORER') return
  const variables = config.value.variables.map((variable, currentIndex) => currentIndex === index ? { ...variable, ...patch } : variable)
  setConfig({ ...config.value, variables })
}

function removeVariable(index: number) {
  if (config.value?.type !== 'FORMULA_EXPLORER' || config.value.variables.length <= 1) return
  setConfig({ ...config.value, variables: config.value.variables.filter((_, currentIndex) => currentIndex !== index) })
}

function addFormulaOption() {
  if (config.value?.type !== 'FORMULA_EXPLORER') return
  const options = config.value.formulaOptions ?? []
  const nextIndex = options.length + 1
  setConfig({
    ...config.value,
    formulaOptions: [
      ...options,
      {
        id: `formula-${nextIndex}`,
        label: `Formula ${nextIndex}`,
        formula: config.value.formula,
        description: '',
        steps: [],
      },
    ],
  })
  selectedFormulaSectionIndex.value = options.length
}

function updateFormulaOption(index: number, patch: Partial<FormulaOptionConfig>) {
  if (config.value?.type !== 'FORMULA_EXPLORER') return
  const options = [...(config.value.formulaOptions ?? [])]
  if (!options[index]) return
  options[index] = { ...options[index], ...patch }
  setConfig({ ...config.value, formulaOptions: options })
}

function removeFormulaOption(index: number) {
  if (config.value?.type !== 'FORMULA_EXPLORER') return
  setConfig({
    ...config.value,
    formulaOptions: (config.value.formulaOptions ?? []).filter((_, currentIndex) => currentIndex !== index),
  })
  selectedFormulaSectionIndex.value = Math.max(0, Math.min(selectedFormulaSectionIndex.value, (config.value.formulaOptions?.length ?? 1) - 2))
}

function selectFormulaSection(index: number) {
  selectedFormulaSectionIndex.value = index
}

function addFormulaStep(optionIndex: number) {
  if (config.value?.type !== 'FORMULA_EXPLORER') return
  const option = config.value.formulaOptions?.[optionIndex]
  if (!option) return
  const steps = option.steps ?? []
  updateFormulaOption(optionIndex, {
    steps: [
      ...steps,
      { label: `Step ${steps.length + 1}`, expression: option.formula, explanation: '' },
    ],
  })
}

function updateFormulaStep(optionIndex: number, stepIndex: number, patch: Partial<FormulaStepConfig>) {
  if (config.value?.type !== 'FORMULA_EXPLORER') return
  const option = config.value.formulaOptions?.[optionIndex]
  if (!option) return
  const steps = [...(option.steps ?? [])]
  if (!steps[stepIndex]) return
  steps[stepIndex] = { ...steps[stepIndex], ...patch }
  updateFormulaOption(optionIndex, { steps })
}

function removeFormulaStep(optionIndex: number, stepIndex: number) {
  if (config.value?.type !== 'FORMULA_EXPLORER') return
  const option = config.value.formulaOptions?.[optionIndex]
  if (!option) return
  updateFormulaOption(optionIndex, {
    steps: (option.steps ?? []).filter((_, currentIndex) => currentIndex !== stepIndex),
  })
}

function addQuizOption() {
  if (config.value?.type !== 'QUIZ') return
  const id = String.fromCharCode(97 + config.value.options.length)
  setConfig({
    ...config.value,
    options: [...config.value.options, { id, label: 'New option', correct: false }],
  })
}

function updateQuizOption(index: number, patch: Partial<QuizConfig['options'][number]>) {
  if (config.value?.type !== 'QUIZ') return
  const options = config.value.options.map((option, currentIndex) => {
    if (currentIndex !== index) return option
    return { ...option, ...patch }
  })
  setConfig({ ...config.value, options })
}

function setCorrectQuizOption(index: number) {
  if (config.value?.type !== 'QUIZ') return
  setConfig({
    ...config.value,
    options: config.value.options.map((option, currentIndex) => ({ ...option, correct: currentIndex === index })),
  })
}

function removeQuizOption(index: number) {
  if (config.value?.type !== 'QUIZ' || config.value.options.length <= 2) return
  const options = config.value.options.filter((_, currentIndex) => currentIndex !== index)
  if (!options.some((option) => option.correct)) options[0].correct = true
  setConfig({ ...config.value, options })
}

function visualConfig() {
  return config.value?.type === 'VISUAL_LAYER' ? config.value : null
}

function selectVisual(kind: 'zone' | 'element' | 'overlap', id: string) {
  if (selectedVisualKind.value !== kind || selectedVisualId.value !== id) {
    visualEditDraft.value = null
    visualDrag.value = null
    visualGroupEditDraft.value = null
  }
  selectedVisualGroup.value = []
  visualToolbarDismissed.value = false
  selectedVisualKind.value = kind
  selectedVisualId.value = id
  visualTool.value = 'select'
}

function selectVisualGroup(items: VisualSelectionItem[]) {
  visualEditDraft.value = null
  visualGroupEditDraft.value = null
  visualDrag.value = null
  visualToolbarDismissed.value = false
  selectedVisualId.value = null
  selectedVisualKind.value = null
  selectedVisualGroup.value = items
  visualTool.value = 'select'
}

function clearVisualSelection() {
  selectedVisualId.value = null
  selectedVisualKind.value = null
  selectedVisualGroup.value = []
  visualEditDraft.value = null
  visualGroupEditDraft.value = null
  visualDrag.value = null
  visualToolbarDismissed.value = false
}

function selectedVisualZone() {
  const current = visualConfig()
  return selectedVisualKind.value === 'zone' ? current?.zones.find((zone) => zone.id === selectedVisualId.value) ?? null : null
}

function selectedVisualElement() {
  const current = visualConfig()
  return selectedVisualKind.value === 'element' ? current?.elements.find((element) => element.id === selectedVisualId.value) ?? null : null
}

function selectedOverlapValue() {
  const current = visualConfig()
  return selectedVisualKind.value === 'overlap' ? visualOverlapRegions.value.find((value) => value.id === selectedVisualId.value) ?? current?.overlap?.values.find((value) => value.id === selectedVisualId.value) ?? null : null
}

const negativeOverlapRegions = computed(() => visualOverlapRegions.value.filter((region) => region.value < 0))

function selectedVisualInteraction() {
  const current = visualConfig()
  if (!current || !selectedVisualId.value) return null
  return current.interactions.find((interaction) => interaction.triggerId === selectedVisualId.value) ?? null
}

function draftForVisual(kind: 'zone' | 'element', id: string) {
  const edit = visualEditDraft.value
  return edit?.kind === kind && edit.id === id ? edit.draft : null
}

function visualSelectionKey(kind: 'zone' | 'element', id: string) {
  return `${kind}:${id}`
}

function groupDraftForVisual(kind: 'zone' | 'element', id: string) {
  return visualGroupEditDraft.value?.draft[visualSelectionKey(kind, id)] ?? null
}

function isVisualGroupSelected(kind: 'zone' | 'element', id: string) {
  return selectedVisualGroup.value.some((item) => item.kind === kind && item.id === id)
}

function boundsFromVisualItem(item: { x: number; y: number; width: number; height: number; labelX?: number; labelY?: number }): VisualBoundsDraft {
  return {
    x: item.x,
    y: item.y,
    width: item.width,
    height: item.height,
    labelX: item.labelX,
    labelY: item.labelY,
  }
}

function visualItemBounds(kind: 'zone' | 'element', id: string) {
  const current = visualConfig()
  if (!current) return null
  const groupDraft = groupDraftForVisual(kind, id)
  if (groupDraft) return groupDraft
  const singleDraft = draftForVisual(kind, id)
  if (singleDraft) return singleDraft
  const item = kind === 'zone'
    ? current.zones.find((zone) => zone.id === id)
    : current.elements.find((element) => element.id === id)
  return item ? boundsFromVisualItem(item) : null
}

function visualGroupBounds() {
  const bounds = selectedVisualGroup.value
    .map((item) => visualItemBounds(item.kind, item.id))
    .filter((item): item is VisualBoundsDraft => Boolean(item))
  if (!bounds.length) return null
  const left = Math.min(...bounds.map((item) => item.x))
  const top = Math.min(...bounds.map((item) => item.y))
  const right = Math.max(...bounds.map((item) => item.x + item.width))
  const bottom = Math.max(...bounds.map((item) => item.y + item.height))
  return { x: left, y: top, width: right - left, height: bottom - top }
}

function intersectsVisualBounds(a: VisualBoundsDraft, b: VisualBoundsDraft) {
  return a.x < b.x + b.width && a.x + a.width > b.x && a.y < b.y + b.height && a.y + a.height > b.y
}

function visualObjectsInBounds(bounds: VisualBoundsDraft) {
  const current = visualConfig()
  if (!current) return []
  const zones = current.zones
    .filter((zone) => intersectsVisualBounds(bounds, zone))
    .map((zone) => ({ kind: 'zone' as const, id: zone.id }))
  const elements = current.elements
    .filter((element) => intersectsVisualBounds(bounds, element))
    .map((element) => ({ kind: 'element' as const, id: element.id }))
  return [...zones, ...elements]
}

function visualStyle(item: { x: number; y: number; width: number; height: number }) {
  return {
    left: `${item.x}px`,
    top: `${item.y}px`,
    width: `${item.width}px`,
    height: `${item.height}px`,
  }
}

function visualZoneLabelStyle(zone: VisualLayerZone) {
  return {
    '--zone-label-x': `${zone.labelX ?? 50}%`,
    '--zone-label-y': `${zone.labelY ?? 50}%`,
  }
}

function visualPoint(event: PointerEvent, current: VisualLayerConfig) {
  const target = event.currentTarget as HTMLElement
  const stage = target.classList.contains('visual-admin-stage')
    ? target
    : target.closest('.visual-admin-stage') as HTMLElement | null
  const rect = (stage ?? target).getBoundingClientRect()
  const scaleX = current.canvas.width / rect.width
  const scaleY = current.canvas.height / rect.height
  return {
    x: Math.max(0, Math.min(current.canvas.width, Math.round((event.clientX - rect.left) * scaleX))),
    y: Math.max(0, Math.min(current.canvas.height, Math.round((event.clientY - rect.top) * scaleY))),
  }
}

function clampVisualObject<T extends { x: number; y: number; width: number; height: number }>(item: T, current: VisualLayerConfig): T {
  const width = Math.max(8, Math.min(item.width, current.canvas.width))
  const height = Math.max(8, Math.min(item.height, current.canvas.height))
  return {
    ...item,
    width,
    height,
    x: Math.max(0, Math.min(item.x, current.canvas.width - width)),
    y: Math.max(0, Math.min(item.y, current.canvas.height - height)),
  }
}

function beginVisualEdit(kind: 'zone' | 'element', id: string, item: VisualBoundsDraft) {
  const existing = visualEditDraft.value
  if (existing?.kind === kind && existing.id === id) return existing
  const original = boundsFromVisualItem(item)
  const next = { kind, id, original, draft: { ...original } }
  visualEditDraft.value = next
  visualGroupEditDraft.value = null
  visualToolbarDismissed.value = false
  return next
}

function beginVisualGroupEdit() {
  const current = visualConfig()
  if (!current || selectedVisualGroup.value.length <= 1) return null
  const existing = visualGroupEditDraft.value
  if (existing) return existing
  const original: Record<string, VisualBoundsDraft> = {}
  for (const item of selectedVisualGroup.value) {
    const saved = item.kind === 'zone'
      ? current.zones.find((zone) => zone.id === item.id)
      : current.elements.find((element) => element.id === item.id)
    if (saved) original[visualSelectionKey(item.kind, item.id)] = boundsFromVisualItem(saved)
  }
  const next = { original, draft: Object.fromEntries(Object.entries(original).map(([key, value]) => [key, { ...value }])) }
  visualGroupEditDraft.value = next
  visualEditDraft.value = null
  visualToolbarDismissed.value = false
  return next
}

function updateVisualEditDraft(patch: Partial<VisualBoundsDraft>) {
  const current = visualConfig()
  const edit = visualEditDraft.value
  if (!current || !edit) return
  visualEditDraft.value = {
    ...edit,
    draft: clampVisualObject({ ...edit.draft, ...patch }, current),
  }
}

function updateVisualGroupDraft(dx: number, dy: number, start: Record<string, VisualBoundsDraft>) {
  const current = visualConfig()
  const edit = visualGroupEditDraft.value
  if (!current || !edit) return
  const items = Object.values(start)
  if (!items.length) return
  const left = Math.min(...items.map((item) => item.x))
  const top = Math.min(...items.map((item) => item.y))
  const right = Math.max(...items.map((item) => item.x + item.width))
  const bottom = Math.max(...items.map((item) => item.y + item.height))
  const minDx = -left
  const maxDx = current.canvas.width - right
  const minDy = -top
  const maxDy = current.canvas.height - bottom
  const clampedDx = Math.max(minDx, Math.min(maxDx, dx))
  const clampedDy = Math.max(minDy, Math.min(maxDy, dy))
  visualGroupEditDraft.value = {
    ...edit,
    draft: Object.fromEntries(Object.entries(start).map(([key, item]) => [
      key,
      { ...item, x: item.x + clampedDx, y: item.y + clampedDy },
    ])),
  }
}

function visualToolbarStyle(bounds: VisualBoundsDraft) {
  return {
    left: `${bounds.x + bounds.width / 2}px`,
    top: `${Math.max(0, bounds.y - 12)}px`,
  }
}

function visualResizeHandleStyle(handle: VisualResizeHandle, bounds: VisualBoundsDraft) {
  const x = handle.includes('w') ? bounds.x : handle.includes('e') ? bounds.x + bounds.width : bounds.x + bounds.width / 2
  const y = handle.includes('n') ? bounds.y : handle.includes('s') ? bounds.y + bounds.height : bounds.y + bounds.height / 2
  return {
    left: `${x}px`,
    top: `${y}px`,
  }
}

function handleVisualEditKeydown(event: KeyboardEvent) {
  const target = event.target as HTMLElement | null
  if (target?.closest('input, textarea, select, [contenteditable="true"]')) return
  if (!(event.ctrlKey || event.metaKey) || event.shiftKey || event.key.toLowerCase() !== 'z') return
  if (!visualEditDraft.value && !visualGroupEditDraft.value) return
  event.preventDefault()
  cancelVisualEdit()
}

function beginVisualCanvasPointer(event: PointerEvent) {
  const current = visualConfig()
  if (!current) return
  const point = visualPoint(event, current)
  if (visualTool.value === 'select') {
    ;(event.currentTarget as HTMLElement).setPointerCapture?.(event.pointerId)
    visualMarquee.value = {
      originX: point.x,
      originY: point.y,
      x: point.x,
      y: point.y,
      width: 0,
      height: 0,
    }
    return
  }
  ;(event.currentTarget as HTMLElement).setPointerCapture?.(event.pointerId)
  if (visualTool.value === 'zone-rectangle' || visualTool.value === 'zone-circle') {
    visualDraft.value = {
      kind: 'zone',
      shape: visualTool.value === 'zone-circle' ? 'circle' : 'rectangle',
      originX: point.x,
      originY: point.y,
      x: point.x,
      y: point.y,
      width: 0,
      height: 0,
    }
    return
  }
  visualDraft.value = {
    kind: 'element',
    elementKind: visualTool.value === 'hotspot' ? 'hotspot' : 'button',
    originX: point.x,
    originY: point.y,
    x: point.x,
    y: point.y,
    width: 0,
    height: 0,
  }
}

function moveVisualPointer(event: PointerEvent) {
  const current = visualConfig()
  if (!current) return
  const point = visualPoint(event, current)
  if (visualMarquee.value) {
    const draft = visualMarquee.value
    visualMarquee.value = {
      ...draft,
      x: Math.min(draft.originX, point.x),
      y: Math.min(draft.originY, point.y),
      width: Math.abs(point.x - draft.originX),
      height: Math.abs(point.y - draft.originY),
    }
    return
  }
  if (visualDraft.value) {
    const draft = visualDraft.value
    visualDraft.value = {
      ...draft,
      x: Math.min(draft.originX, point.x),
      y: Math.min(draft.originY, point.y),
      width: Math.abs(point.x - draft.originX),
      height: Math.abs(point.y - draft.originY),
    }
    return
  }
  if (!visualDrag.value) return
  if (visualDrag.value.kind === 'group') {
    if (!visualDrag.value.originPoint || !visualDrag.value.groupStart) return
    updateVisualGroupDraft(point.x - visualDrag.value.originPoint.x, point.y - visualDrag.value.originPoint.y, visualDrag.value.groupStart)
    return
  }
  if (visualDrag.value.kind === 'resize') {
    const drag = visualDrag.value
    const edit = visualEditDraft.value
    if (!edit || !drag.start || !drag.originPoint || !drag.resizeHandle) return
    const dx = point.x - drag.originPoint.x
    const dy = point.y - drag.originPoint.y
    const next = { ...drag.start }
    if (drag.resizeHandle.includes('w')) {
      next.x = drag.start.x + dx
      next.width = drag.start.width - dx
    }
    if (drag.resizeHandle.includes('e')) next.width = drag.start.width + dx
    if (drag.resizeHandle.includes('n')) {
      next.y = drag.start.y + dy
      next.height = drag.start.height - dy
    }
    if (drag.resizeHandle.includes('s')) next.height = drag.start.height + dy
    updateVisualEditDraft(next)
    return
  }
  const x = point.x - visualDrag.value.offsetX
  const y = point.y - visualDrag.value.offsetY
  if (visualDrag.value.kind === 'zone') {
    const zone = current.zones.find((item) => item.id === visualDrag.value?.id)
    if (zone) updateVisualEditDraft({ x, y })
    return
  }
  if (visualDrag.value.kind === 'zone-label') {
    const zone = current.zones.find((item) => item.id === visualDrag.value?.id)
    if (zone) {
      updateVisualZone(zone.id, {
        labelX: Math.round(Math.max(0, Math.min(100, ((point.x - zone.x) / zone.width) * 100))),
        labelY: Math.round(Math.max(0, Math.min(100, ((point.y - zone.y) / zone.height) * 100))),
      })
    }
    return
  }
  const element = current.elements.find((item) => item.id === visualDrag.value?.id)
  if (element) updateVisualEditDraft({ x, y })
}

function endVisualPointer(event: PointerEvent) {
  const current = visualConfig()
  const target = event.currentTarget as HTMLElement
  if (target.hasPointerCapture?.(event.pointerId)) target.releasePointerCapture?.(event.pointerId)
  if (!current) return
  if (visualMarquee.value) {
    const marquee = visualMarquee.value
    visualMarquee.value = null
    if (marquee.width < 6 && marquee.height < 6) {
      clearVisualSelection()
      return
    }
    const selected = visualObjectsInBounds(marquee)
    if (selected.length === 1) {
      selectVisual(selected[0].kind, selected[0].id)
    } else if (selected.length > 1) {
      selectVisualGroup(selected)
    } else {
      clearVisualSelection()
    }
    return
  }
  if (visualDraft.value) {
    const draft = clampVisualObject({
      ...visualDraft.value,
      width: Math.max(visualDraft.value.width, visualDraft.value.kind === 'zone' ? 80 : 96),
      height: Math.max(visualDraft.value.height, visualDraft.value.kind === 'zone' ? 60 : 40),
    }, current)
    if (draft.kind === 'zone') {
    const id = `zone_${current.zones.length + 1}`
      const zone: VisualLayerZone = {
        id,
        label: `Zone ${current.zones.length + 1}`,
        shape: draft.shape ?? 'rectangle',
        x: draft.x,
        y: draft.y,
        width: draft.width,
        height: draft.height,
        color: '#ffd333',
        highlightColor: '#ff8f1f',
        highlightOpacity: 0.82,
        feedback: '',
      }
    setConfig({ ...current, zones: [...current.zones, zone] })
    selectVisual('zone', id)
    } else {
      const id = `trigger_${current.elements.length + 1}`
      const element: VisualLayerElement = {
        id,
        label: draft.elementKind === 'button' ? `Choice ${current.elements.length + 1}` : `Hotspot ${current.elements.length + 1}`,
        kind: draft.elementKind ?? 'button',
        x: draft.x,
        y: draft.y,
        width: draft.width,
        height: draft.height,
      }
      setConfig({ ...current, elements: [...current.elements, element] })
      selectVisual('element', id)
    }
  }
  visualDraft.value = null
  visualDrag.value = null
}

function beginVisualObjectDrag(kind: 'zone' | 'element', id: string, event: PointerEvent) {
  const current = visualConfig()
  if (!current) return
  const point = visualPoint(event, current)
  const savedItem = kind === 'zone'
    ? current.zones.find((zone) => zone.id === id)
    : current.elements.find((element) => element.id === id)
  if (!savedItem) return
  if (isVisualGroupSelected(kind, id) && selectedVisualGroup.value.length > 1) {
    const edit = beginVisualGroupEdit()
    if (!edit) return
    ;(event.currentTarget as HTMLElement).setPointerCapture?.(event.pointerId)
    visualDrag.value = {
      kind: 'group',
      id: '',
      offsetX: 0,
      offsetY: 0,
      originPoint: point,
      groupStart: Object.fromEntries(Object.entries(edit.draft).map(([key, value]) => [key, { ...value }])),
    }
    return
  }
  selectVisual(kind, id)
  ;(event.currentTarget as HTMLElement).setPointerCapture?.(event.pointerId)
  const edit = beginVisualEdit(kind, id, draftForVisual(kind, id) ?? savedItem)
  visualDrag.value = {
    kind,
    id,
    offsetX: point.x - edit.draft.x,
    offsetY: point.y - edit.draft.y,
  }
}

function beginVisualResize(handle: VisualResizeHandle, event: PointerEvent) {
  const current = visualConfig()
  const kind = selectedVisualKind.value
  const id = selectedVisualId.value
  if (!current || (kind !== 'zone' && kind !== 'element') || !id) return
  const savedItem = kind === 'zone'
    ? current.zones.find((zone) => zone.id === id)
    : current.elements.find((element) => element.id === id)
  if (!savedItem) return
  const point = visualPoint(event, current)
  ;(event.currentTarget as HTMLElement).setPointerCapture?.(event.pointerId)
  const edit = beginVisualEdit(kind, id, draftForVisual(kind, id) ?? savedItem)
  visualDrag.value = {
    kind: 'resize',
    id,
    offsetX: 0,
    offsetY: 0,
    resizeHandle: handle,
    start: { ...edit.draft },
    originPoint: point,
  }
}

function beginVisualZoneLabelDrag(id: string) {
  const current = visualConfig()
  if (!current) return
  const zone = current.zones.find((item) => item.id === id)
  if (!zone) return
  selectVisual('zone', id)
  visualDrag.value = {
    kind: 'zone-label',
    id,
    offsetX: 0,
    offsetY: 0,
  }
}

function overlapMaskId(regionId: string) {
  return `${visualMaskPrefix}-${regionId}`
}

function toggleVisualOverlap(enabled: boolean) {
  const current = visualConfig()
  if (!current) return
  if (!enabled) {
    setConfig({ ...current, overlap: { ...(current.overlap ?? { sourceZoneIds: [], inputs: [], values: [] }), enabled: false } })
    if (selectedVisualKind.value === 'overlap') {
      selectedVisualKind.value = null
      selectedVisualId.value = null
    }
    return
  }
  const sourceZoneIds = current.overlap?.sourceZoneIds?.length ? current.overlap.sourceZoneIds : current.zones.slice(0, 5).map((zone) => zone.id)
  setConfig({ ...current, overlap: ensureVisualOverlapValues(current, sourceZoneIds) })
}

function toggleOverlapSource(zoneId: string, enabled: boolean) {
  const current = visualConfig()
  if (!current) return
  const sourceZoneIds = current.overlap?.sourceZoneIds ?? current.zones.slice(0, 5).map((zone) => zone.id)
  const nextSources = enabled
    ? [...sourceZoneIds, zoneId]
    : sourceZoneIds.filter((id) => id !== zoneId)
  setConfig({ ...current, overlap: ensureVisualOverlapValues(current, nextSources) })
}

function refreshVisualOverlap(current: VisualLayerConfig) {
  if (!current.overlap?.enabled) return current
  return { ...current, overlap: ensureVisualOverlapValues(current, current.overlap.sourceZoneIds) }
}

function updateVisualZone(id: string, patch: Partial<VisualLayerZone>) {
  const current = visualConfig()
  if (!current) return
  if (visualEditDraft.value?.kind === 'zone' && visualEditDraft.value.id === id && ('labelX' in patch || 'labelY' in patch)) {
    visualEditDraft.value = {
      ...visualEditDraft.value,
      draft: {
        ...visualEditDraft.value.draft,
        labelX: patch.labelX ?? visualEditDraft.value.draft.labelX,
        labelY: patch.labelY ?? visualEditDraft.value.draft.labelY,
      },
    }
  }
  if (visualEditDraft.value?.kind === 'zone' && visualEditDraft.value.id === id && ('x' in patch || 'y' in patch || 'width' in patch || 'height' in patch)) {
    visualEditDraft.value = null
  }
  setConfig(refreshVisualOverlap({ ...current, zones: current.zones.map((zone) => zone.id === id ? { ...zone, ...patch } : zone) }))
}

function updateVisualElement(id: string, patch: Partial<VisualLayerElement>) {
  const current = visualConfig()
  if (!current) return
  if (visualEditDraft.value?.kind === 'element' && visualEditDraft.value.id === id && ('x' in patch || 'y' in patch || 'width' in patch || 'height' in patch)) {
    visualEditDraft.value = null
  }
  setConfig({ ...current, elements: current.elements.map((element) => element.id === id ? { ...element, ...patch } : element) })
}

function confirmVisualEdit() {
  if (visualGroupEditDraft.value) {
    const current = visualConfig()
    const edit = visualGroupEditDraft.value
    visualToolbarDismissed.value = true
    visualGroupEditDraft.value = null
    visualDrag.value = null
    if (!current) return
    const zones = current.zones.map((zone) => {
      const draft = edit.draft[visualSelectionKey('zone', zone.id)]
      return draft ? { ...zone, ...draft } : zone
    })
    const elements = current.elements.map((element) => {
      const draft = edit.draft[visualSelectionKey('element', element.id)]
      return draft ? { ...element, ...draft } : element
    })
    setConfig(refreshVisualOverlap({ ...current, zones, elements }))
    return
  }
  const edit = visualEditDraft.value
  visualToolbarDismissed.value = true
  if (!edit) return
  const draft = { ...edit.draft }
  visualEditDraft.value = null
  visualDrag.value = null
  if (edit.kind === 'zone') {
    updateVisualZone(edit.id, draft)
  } else {
    updateVisualElement(edit.id, draft)
  }
}

function cancelVisualEdit() {
  visualEditDraft.value = null
  visualGroupEditDraft.value = null
  visualDrag.value = null
  visualToolbarDismissed.value = true
}

function deleteSelectedVisual() {
  if (selectedVisualGroup.value.length > 1) {
    const current = visualConfig()
    const group = selectedVisualGroup.value
    visualToolbarDismissed.value = true
    visualGroupEditDraft.value = null
    visualEditDraft.value = null
    visualDrag.value = null
    if (!current) return
    const selectedKeys = new Set(group.map((item) => visualSelectionKey(item.kind, item.id)))
    setConfig(refreshVisualOverlap({
      ...current,
      zones: current.zones.filter((zone) => !selectedKeys.has(visualSelectionKey('zone', zone.id))),
      elements: current.elements.filter((element) => !selectedKeys.has(visualSelectionKey('element', element.id))),
      interactions: current.interactions.filter((interaction) => (
        !selectedKeys.has(visualSelectionKey('element', interaction.triggerId))
        && (!interaction.targetZoneId || !selectedKeys.has(visualSelectionKey('zone', interaction.targetZoneId)))
      )),
    }))
    selectedVisualGroup.value = []
    visualToolbarDismissed.value = false
    return
  }
  visualToolbarDismissed.value = true
  removeSelectedVisual()
}

function removeSelectedVisual() {
  const current = visualConfig()
  if (!current || !selectedVisualId.value || !selectedVisualKind.value) return
  const id = selectedVisualId.value
  if (selectedVisualKind.value === 'overlap') {
    selectedVisualId.value = null
    selectedVisualKind.value = null
    visualToolbarDismissed.value = false
    return
  }
  visualEditDraft.value = null
  visualDrag.value = null
  setConfig(refreshVisualOverlap({
    ...current,
    zones: selectedVisualKind.value === 'zone' ? current.zones.filter((zone) => zone.id !== id) : current.zones,
    elements: selectedVisualKind.value === 'element' ? current.elements.filter((element) => element.id !== id) : current.elements,
    interactions: current.interactions.filter((interaction) => interaction.triggerId !== id && interaction.targetZoneId !== id),
  }))
  selectedVisualId.value = null
  selectedVisualKind.value = null
  visualToolbarDismissed.value = false
}

function updateOverlapValue(id: string, patch: Partial<VisualLayerOverlapValue>) {
  const current = visualConfig()
  if (!current?.overlap) return
  setConfig({
    ...current,
    overlap: {
      ...current.overlap,
      values: current.overlap.values.map((value) => value.id === id ? { ...value, ...patch } : value),
    },
  })
}

function updateOverlapInput(id: string, patch: Partial<VisualLayerOverlapInput>) {
  const current = visualConfig()
  if (!current?.overlap) return
  const nextOverlap = ensureVisualOverlapValues({
    ...current,
    overlap: {
      ...current.overlap,
      inputs: (current.overlap.inputs ?? []).map((input) => input.id === id ? { ...input, ...patch } : input),
    },
  }, current.overlap.sourceZoneIds)
  setConfig({ ...current, overlap: nextOverlap })
}

function updateVisualInteraction(patch: Partial<VisualLayerInteraction>) {
  const current = visualConfig()
  if (!current || !selectedVisualId.value) return
  const existing = selectedVisualInteraction()
  const next: VisualLayerInteraction = {
    triggerId: selectedVisualId.value,
    effect: 'HIGHLIGHT_ZONE',
    targetZoneId: current.zones[0]?.id,
    feedback: '',
    ...existing,
    ...patch,
  }
  setConfig({
    ...current,
    interactions: [
      ...current.interactions.filter((interaction) => interaction.triggerId !== selectedVisualId.value),
      next,
    ],
  })
}

function updateGraphControl(name: string, patch: Partial<{ min: number; max: number; step: number; initial: number }>) {
  if (config.value?.type !== 'GRAPH_2D') return
  const controls = config.value.controls ?? {}
  setConfig({ ...config.value, controls: { ...controls, [name]: { ...controls[name], ...patch } } } as InteractiveConfig)
}

function labelFor(type: TemplateInteractionType) {
  if (type === 'GRAPH_2D') return '2D graph'
  if (type === 'FORMULA_EXPLORER') return 'Formula explorer'
  if (type === 'VISUAL_LAYER') return 'Set / Diagram Builder'
  return 'Quiz'
}
</script>

<template>
  <div class="interactive-config-shell" :class="{ 'interactive-config-shell--canvas': config?.type === 'FORMULA_EXPLORER' || config?.type === 'VISUAL_LAYER' }">
    <div class="studio-tabs" role="tablist" aria-label="Interactive editor sections">
      <button v-for="tab in studioTabs" :key="tab" type="button" class="studio-tab" :class="{ active: activeStudioTab === tab }" @click="activeStudioTab = tab">
        {{ tab }}
      </button>
    </div>

    <div v-if="activeStudioTab === 'Design'" class="interactive-config-controls space-y-4">
      <section class="studio-section">
        <div class="studio-section__header">
          <div>
            <h3>Template</h3>
            <p>Choose the activity type first, then refine the fields below.</p>
          </div>
          <label class="compact-select">
            <span>Compact picker</span>
            <select v-model="templateType">
              <option value="NONE">None</option>
              <option v-for="template in templateChoices" :key="template.type" :value="template.type">{{ template.label }}</option>
              <option value="OTHER">Other JSON</option>
            </select>
          </label>
        </div>
        <div class="template-card-grid">
          <button
            v-for="card in templateCards"
            :key="card.type"
            type="button"
            class="template-card"
            :class="{ active: templateType === card.type }"
            @click="selectTemplateCard(card.type)"
          >
            <strong>{{ card.title }}</strong>
            <span>{{ card.description }}</span>
          </button>
          <button type="button" class="template-card" :class="{ active: templateType === 'NONE' }" @click="selectTemplateCard('NONE')">
            <strong>None</strong>
            <span>No activity for this lesson.</span>
          </button>
          <button type="button" class="template-card" :class="{ active: templateType === 'OTHER' }" @click="selectTemplateCard('OTHER')">
            <strong>Other JSON</strong>
            <span>Paste custom JSON without using a template editor.</span>
          </button>
        </div>
      </section>

      <section class="studio-section">
        <div class="studio-section__header">
          <div>
            <h3>Presets</h3>
            <p>Apply a complete starter config, then edit it.</p>
          </div>
        </div>
        <div class="preset-grid">
          <button
            v-for="preset in presetCards"
            :key="preset.id"
            type="button"
            class="preset-button"
            @click="applyPreset(preset)"
          >
            {{ preset.title }}
          </button>
        </div>
      </section>

    <section class="studio-section studio-section--fields">
    <div v-if="config && config.type !== 'QUIZ'" class="mode-row" role="group" aria-label="Interactive mode">
      <button type="button" :class="{ active: (config.mode ?? 'VISUALIZATION') === 'VISUALIZATION' }" @click="setMode('VISUALIZATION')">Visualization</button>
      <button type="button" :class="{ active: config.mode === 'PRACTICE' }" @click="setMode('PRACTICE')">Practice</button>
    </div>
    <div v-else-if="config?.type === 'QUIZ'" class="mode-row mode-row--fixed" aria-label="Interactive mode">
      <span>Practice activity</span>
    </div>

    <label class="block">
      <span class="mb-1 block text-[11px] font-bold text-lm-ink-3">Interaction prompt</span>
      <textarea
        v-model="prompt"
        rows="3"
        class="w-full resize-none rounded-[8px] border-2 border-lm-line-soft bg-lm-bg-soft px-3 py-2 text-[12px] text-lm-ink outline-none transition focus:border-lm-line focus:bg-lm-surface focus:ring-2 focus:ring-lm-yellow/40"
      />
    </label>

    <div v-if="config?.type === 'GRAPH_2D'" class="editor-grid">
      <label class="field field--wide">
        <span>Title</span>
        <input :value="config.title" maxlength="120" @input="patchConfig({ title: ($event.target as HTMLInputElement).value })" />
      </label>
      <label class="field field--wide">
        <span>Expression</span>
        <input :value="config.expression" maxlength="160" @input="patchConfig({ expression: ($event.target as HTMLInputElement).value })" />
      </label>
      <label class="field"><span>X min</span><input type="number" :value="config.xMin" @input="patchConfig({ xMin: Number(($event.target as HTMLInputElement).value) })" /></label>
      <label class="field"><span>X max</span><input type="number" :value="config.xMax" @input="patchConfig({ xMax: Number(($event.target as HTMLInputElement).value) })" /></label>
      <label class="field"><span>Y min</span><input type="number" :value="config.yMin" @input="patchConfig({ yMin: Number(($event.target as HTMLInputElement).value) })" /></label>
      <label class="field"><span>Y max</span><input type="number" :value="config.yMax" @input="patchConfig({ yMax: Number(($event.target as HTMLInputElement).value) })" /></label>
      <label class="field"><span>Samples</span><input type="number" min="20" max="500" :value="config.sampleCount" @input="patchConfig({ sampleCount: Number(($event.target as HTMLInputElement).value) })" /></label>
      <template v-if="config.mode === 'PRACTICE'">
        <template v-for="(control, name) in config.controls" :key="name">
          <label class="field"><span>{{ name }} min</span><input type="number" :value="control.min" @input="updateGraphControl(String(name), { min: Number(($event.target as HTMLInputElement).value) })" /></label>
          <label class="field"><span>{{ name }} max</span><input type="number" :value="control.max" @input="updateGraphControl(String(name), { max: Number(($event.target as HTMLInputElement).value) })" /></label>
          <label class="field"><span>{{ name }} step</span><input type="number" :value="control.step" @input="updateGraphControl(String(name), { step: Number(($event.target as HTMLInputElement).value) })" /></label>
          <label class="field"><span>{{ name }} initial</span><input type="number" :value="control.initial" @input="updateGraphControl(String(name), { initial: Number(($event.target as HTMLInputElement).value) })" /></label>
        </template>
        <label class="field"><span>Target x</span><input type="number" :value="config.successCondition?.target.x" @input="patchConfig({ successCondition: { kind: 'POINT_ON_GRAPH', target: { x: Number(($event.target as HTMLInputElement).value), y: config.successCondition?.target.y ?? 0 }, tolerance: config.successCondition?.tolerance ?? 0.1 } })" /></label>
        <label class="field"><span>Target y</span><input type="number" :value="config.successCondition?.target.y" @input="patchConfig({ successCondition: { kind: 'POINT_ON_GRAPH', target: { x: config.successCondition?.target.x ?? 0, y: Number(($event.target as HTMLInputElement).value) }, tolerance: config.successCondition?.tolerance ?? 0.1 } })" /></label>
      </template>
    </div>

    <div v-else-if="config?.type === 'FORMULA_EXPLORER'" class="space-y-3">
      <div class="editor-grid">
        <label class="field field--wide"><span>Title</span><input :value="config.title" maxlength="120" @input="patchConfig({ title: ($event.target as HTMLInputElement).value })" /></label>
        <label class="field field--wide"><span>Formula</span><input :value="config.formula" maxlength="160" @input="patchConfig({ formula: ($event.target as HTMLInputElement).value })" /></label>
        <label class="field"><span>Precision</span><input type="number" min="0" max="6" :value="config.precision" @input="patchConfig({ precision: Number(($event.target as HTMLInputElement).value) })" /></label>
        <template v-if="config.mode === 'PRACTICE'">
          <label class="field"><span>Target result</span><input type="number" :value="config.successCondition?.target" @input="patchConfig({ successCondition: { kind: 'EXPRESSION_EQUALS', target: Number(($event.target as HTMLInputElement).value), tolerance: config.successCondition?.tolerance ?? 0.01 } })" /></label>
          <label class="field"><span>Tolerance</span><input type="number" min="0" :value="config.successCondition?.tolerance ?? 0.01" @input="patchConfig({ successCondition: { kind: 'EXPRESSION_EQUALS', target: config.successCondition?.target ?? 0, tolerance: Number(($event.target as HTMLInputElement).value) } })" /></label>
        </template>
      </div>
      <div class="section-card">
        <div class="section-card__header">
          <div>
            <h4>Variables</h4>
            <p>Slider values students can change. Formula names must match these variables.</p>
          </div>
          <button type="button" class="mini-button" :disabled="config.variables.length >= 10" @click="addVariable">Add variable</button>
        </div>
        <div class="space-y-2">
          <div v-for="(variable, index) in config.variables" :key="`${variable.name}-${index}`" class="nested-row">
            <label class="field"><span>Name</span><input :value="variable.name" maxlength="30" @input="updateVariable(index, { name: ($event.target as HTMLInputElement).value })" /></label>
            <label class="field"><span>Label</span><input :value="variable.label" maxlength="80" @input="updateVariable(index, { label: ($event.target as HTMLInputElement).value })" /></label>
            <label class="field"><span>Min</span><input type="number" :value="variable.min" @input="updateVariable(index, { min: Number(($event.target as HTMLInputElement).value) })" /></label>
            <label class="field"><span>Max</span><input type="number" :value="variable.max" @input="updateVariable(index, { max: Number(($event.target as HTMLInputElement).value) })" /></label>
            <label class="field"><span>Step</span><input type="number" :value="variable.step" @input="updateVariable(index, { step: Number(($event.target as HTMLInputElement).value) })" /></label>
            <label class="field"><span>Initial</span><input type="number" :value="variable.initial" @input="updateVariable(index, { initial: Number(($event.target as HTMLInputElement).value) })" /></label>
            <button type="button" class="mini-button mini-button--icon" :disabled="config.variables.length <= 1" aria-label="Remove variable" title="Remove variable" @click="removeVariable(index)">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M4 7h16" />
                <path d="M10 11v6" />
                <path d="M14 11v6" />
                <path d="M6 7l1 14h10l1-14" />
                <path d="M9 7V4h6v3" />
              </svg>
            </button>
          </div>
        </div>
      </div>

      <div class="formula-builder">
        <aside class="formula-builder__list">
          <div class="section-card__header">
            <div>
              <h4>Layers</h4>
              <p>เลือกปุ่มสูตรที่ต้องการแก้</p>
            </div>
            <button type="button" class="mini-button" :disabled="(config.formulaOptions?.length ?? 0) >= 10" @click="addFormulaOption">Add</button>
          </div>

          <p v-if="!config.formulaOptions?.length" class="empty-note">
            No sections yet. Add one to create clickable formula buttons.
          </p>

          <button
            v-for="(option, optionIndex) in config.formulaOptions"
            :key="`${option.id}-${optionIndex}`"
            type="button"
            class="section-list-item"
            :class="{ active: selectedFormulaSectionIndex === optionIndex }"
            @click="selectFormulaSection(optionIndex)"
          >
            <span>{{ option.label || `Section ${optionIndex + 1}` }}</span>
            <small>{{ option.formula }}</small>
          </button>
        </aside>

        <section class="formula-builder__canvas">
          <div class="canvas-toolbar">
            <div>
              <strong>Learner canvas</strong>
              <span>คลิกปุ่มสูตรใน preview เพื่อจำลองสิ่งที่นักเรียนเห็น</span>
            </div>
          </div>
          <div class="canvas-stage">
            <InteractivePreview :config="config" />
          </div>
        </section>

        <section class="formula-builder__properties">
          <div class="properties-header">
            <strong>Properties</strong>
            <span>{{ selectedFormulaOption?.label || 'No section selected' }}</span>
          </div>
          <div v-if="selectedFormulaOption" class="option-card">
            <div class="option-card__header">
              <strong>{{ selectedFormulaOption.label || `Section ${selectedFormulaSectionIndex + 1}` }}</strong>
              <button type="button" class="mini-button" @click="removeFormulaOption(selectedFormulaSectionIndex)">Remove section</button>
            </div>

            <div class="editor-grid">
              <label class="field"><span>ID</span><input :value="selectedFormulaOption.id" maxlength="40" @input="updateFormulaOption(selectedFormulaSectionIndex, { id: ($event.target as HTMLInputElement).value })" /></label>
              <label class="field"><span>Button label</span><input :value="selectedFormulaOption.label" maxlength="80" @input="updateFormulaOption(selectedFormulaSectionIndex, { label: ($event.target as HTMLInputElement).value })" /></label>
              <label class="field field--wide"><span>Formula</span><input :value="selectedFormulaOption.formula" maxlength="160" @input="updateFormulaOption(selectedFormulaSectionIndex, { formula: ($event.target as HTMLInputElement).value })" /></label>
              <label class="field field--wide"><span>Explanation</span><textarea rows="2" :value="selectedFormulaOption.description ?? ''" maxlength="500" @input="updateFormulaOption(selectedFormulaSectionIndex, { description: ($event.target as HTMLTextAreaElement).value })" /></label>
            </div>

            <div class="subsection">
              <div class="subsection__header">
                <strong>Step-by-step</strong>
                <button type="button" class="mini-button" :disabled="(selectedFormulaOption.steps?.length ?? 0) >= 8" @click="addFormulaStep(selectedFormulaSectionIndex)">Add step</button>
              </div>
              <p v-if="!selectedFormulaOption.steps?.length" class="empty-note">No steps yet.</p>
              <div v-for="(step, stepIndex) in selectedFormulaOption.steps" :key="`${step.label}-${stepIndex}`" class="nested-row">
                <label class="field"><span>Step label</span><input :value="step.label" maxlength="80" @input="updateFormulaStep(selectedFormulaSectionIndex, stepIndex, { label: ($event.target as HTMLInputElement).value })" /></label>
                <label class="field"><span>Expression</span><input :value="step.expression" maxlength="160" @input="updateFormulaStep(selectedFormulaSectionIndex, stepIndex, { expression: ($event.target as HTMLInputElement).value })" /></label>
                <label class="field field--wide"><span>Explanation</span><textarea rows="2" :value="step.explanation ?? ''" maxlength="500" @input="updateFormulaStep(selectedFormulaSectionIndex, stepIndex, { explanation: ($event.target as HTMLTextAreaElement).value })" /></label>
                <button type="button" class="mini-button" @click="removeFormulaStep(selectedFormulaSectionIndex, stepIndex)">Remove step</button>
              </div>
            </div>
          </div>

          <div v-else class="option-card option-card--empty">
            <strong>Select or add a section</strong>
            <p class="empty-note">Properties for the selected formula button will appear here.</p>
          </div>
        </section>
      </div>
    </div>

    <div v-else-if="config?.type === 'VISUAL_LAYER'" class="visual-editor">
      <aside class="visual-editor__layers">
        <label class="field">
          <span>Title</span>
          <input :value="config.title" maxlength="120" @input="patchConfig({ title: ($event.target as HTMLInputElement).value })" />
        </label>
        <label class="field">
          <span>Background text</span>
          <textarea rows="3" :value="config.canvas.backgroundText ?? ''" maxlength="500" @input="patchConfig({ canvas: { ...config.canvas, backgroundText: ($event.target as HTMLTextAreaElement).value } } as Partial<VisualLayerConfig>)" />
        </label>
        <template v-if="config.mode === 'PRACTICE'">
          <label class="field">
            <span>Success feedback</span>
            <textarea rows="2" :value="config.feedback?.success ?? ''" maxlength="500" @input="patchConfig({ feedback: { success: ($event.target as HTMLTextAreaElement).value, failure: config.feedback?.failure ?? '' } } as Partial<VisualLayerConfig>)" />
          </label>
          <label class="field">
            <span>Failure feedback</span>
            <textarea rows="2" :value="config.feedback?.failure ?? ''" maxlength="500" @input="patchConfig({ feedback: { success: config.feedback?.success ?? '', failure: ($event.target as HTMLTextAreaElement).value } } as Partial<VisualLayerConfig>)" />
          </label>
        </template>
        <div class="tool-row">
          <button type="button" class="mini-button" :class="{ active: visualTool === 'select' }" @click="visualTool = 'select'">Select</button>
          <button type="button" class="mini-button" :class="{ active: visualTool === 'zone-rectangle' }" @click="visualTool = 'zone-rectangle'">Zone rect</button>
          <button type="button" class="mini-button" :class="{ active: visualTool === 'zone-circle' }" @click="visualTool = 'zone-circle'">Zone circle</button>
          <button type="button" class="mini-button" :class="{ active: visualTool === 'button' }" @click="visualTool = 'button'">Button</button>
          <button type="button" class="mini-button" :class="{ active: visualTool === 'hotspot' }" @click="visualTool = 'hotspot'">Hotspot</button>
        </div>
        <div class="subsection">
          <label class="check-field">
            <input type="checkbox" :checked="config.overlap?.enabled ?? false" @change="toggleVisualOverlap(($event.target as HTMLInputElement).checked)" />
            <span>Auto overlap values</span>
          </label>
          <div v-if="config.overlap?.enabled" class="overlap-source-list">
            <label v-for="zone in config.zones" :key="zone.id" class="check-field">
              <input
                type="checkbox"
                :checked="config.overlap.sourceZoneIds.includes(zone.id)"
                :disabled="(!config.overlap.sourceZoneIds.includes(zone.id) && config.overlap.sourceZoneIds.length >= 5) || (config.overlap.sourceZoneIds.includes(zone.id) && config.overlap.sourceZoneIds.length <= 1)"
                @change="toggleOverlapSource(zone.id, ($event.target as HTMLInputElement).checked)"
              />
              <span>{{ zone.label }}</span>
            </label>
          </div>
          <p v-if="negativeOverlapRegions.length" class="overlap-warning">
            Some exact regions are negative. Check the inclusive set totals and intersections.
          </p>
          <div v-if="config.overlap?.enabled && config.overlap.inputs?.length" class="overlap-input-grid">
            <label v-for="input in config.overlap.inputs" :key="input.id" class="field">
              <span>{{ input.label }}</span>
              <input
                type="number"
                :value="input.value"
                @input="updateOverlapInput(input.id, { value: Number(($event.target as HTMLInputElement).value) })"
              />
            </label>
          </div>
        </div>
        <div class="layer-group">
          <strong>Zones</strong>
          <button v-for="zone in config.zones" :key="zone.id" type="button" class="layer-item" :class="{ active: (selectedVisualKind === 'zone' && selectedVisualId === zone.id) || isVisualGroupSelected('zone', zone.id) }" @click="selectVisual('zone', zone.id)">
            {{ zone.label }}
          </button>
        </div>
        <div class="layer-group">
          <strong>Triggers</strong>
          <button v-for="element in config.elements" :key="element.id" type="button" class="layer-item" :class="{ active: (selectedVisualKind === 'element' && selectedVisualId === element.id) || isVisualGroupSelected('element', element.id) }" @click="selectVisual('element', element.id)">
            {{ element.label }}
          </button>
        </div>
        <div v-if="visualOverlapRegions.length" class="layer-group">
          <strong>Overlap values</strong>
          <button v-for="region in visualOverlapRegions" :key="region.id" type="button" class="layer-item" :class="{ active: selectedVisualKind === 'overlap' && selectedVisualId === region.id, invalid: region.value < 0 }" @click="selectVisual('overlap', region.id)">
            {{ region.label }}: {{ region.value }}
          </button>
        </div>
      </aside>

      <section class="visual-editor__canvas">
        <div class="canvas-toolbar">
          <div>
            <strong>Visual Layer Canvas</strong>
            <span>{{ visualTool === 'select' ? 'เลือก layer เพื่อแก้ properties' : 'คลิกบน canvas เพื่อเพิ่ม object' }}</span>
          </div>
        </div>
        <div class="visual-admin-stage-wrap">
          <div
            class="visual-admin-stage"
            :class="{ 'visual-admin-stage--drawing': visualTool !== 'select' }"
            :style="{ width: `${config.canvas.width}px`, height: `${config.canvas.height}px` }"
            @pointerdown="beginVisualCanvasPointer"
            @pointermove="moveVisualPointer"
            @pointerup="endVisualPointer"
            @pointercancel="endVisualPointer"
          >
            <p v-if="config.canvas.backgroundText" class="visual-bg-text">{{ config.canvas.backgroundText }}</p>
            <div
              v-if="visualDraft"
              class="admin-draft"
              :class="[
                visualDraft.kind === 'zone' && visualDraft.shape === 'circle' ? 'admin-draft--circle' : '',
                visualDraft.kind === 'element' ? 'admin-draft--element' : '',
              ]"
              :style="visualStyle(visualDraft)"
            />
            <div
              v-if="visualMarquee"
              class="admin-marquee"
              :style="visualStyle(visualMarquee)"
            />
            <button
              v-for="zone in visualZonesForRender"
              :key="zone.id"
              type="button"
              class="admin-zone"
              :class="[`admin-zone--${zone.shape}`, { active: (selectedVisualKind === 'zone' && selectedVisualId === zone.id) || isVisualGroupSelected('zone', zone.id) }]"
              :style="{ ...visualStyle(zone), ...visualZoneLabelStyle(zone), '--zone-color': zone.color, '--zone-highlight-color': zone.highlightColor ?? zone.color, '--zone-highlight-opacity': zone.highlightOpacity ?? 0.82 }"
              @pointerdown.stop="beginVisualObjectDrag('zone', zone.id, $event)"
            >
              <span @pointerdown.stop="beginVisualZoneLabelDrag(zone.id)">{{ zone.label }}</span>
            </button>
            <button
              v-for="element in visualElementsForRender"
              :key="element.id"
              type="button"
              class="admin-trigger"
              :class="{ active: (selectedVisualKind === 'element' && selectedVisualId === element.id) || isVisualGroupSelected('element', element.id), 'admin-trigger--hotspot': element.kind === 'hotspot' }"
              :style="visualStyle(element)"
              @pointerdown.stop="beginVisualObjectDrag('element', element.id, $event)"
            >
              {{ element.label }}
            </button>
            <div
              v-if="selectedVisualDraftBounds"
              class="admin-edit-toolbar"
              :style="visualToolbarStyle(selectedVisualDraftBounds)"
              @pointerdown.stop
            >
              <span v-if="selectedVisualGroup.length > 1" class="admin-edit-toolbar__count">{{ selectedVisualGroup.length }} selected</span>
              <button type="button" class="admin-edit-toolbar__button admin-edit-toolbar__button--danger" @click.stop="deleteSelectedVisual">Delete</button>
              <button type="button" class="admin-edit-toolbar__button" @click.stop="cancelVisualEdit">Cancel</button>
              <button type="button" class="admin-edit-toolbar__button admin-edit-toolbar__button--confirm" @click.stop="confirmVisualEdit">Confirm</button>
            </div>
            <template v-if="selectedVisualDraftBounds && selectedVisualGroup.length <= 1">
              <button
                v-for="handle in visualResizeHandles"
                :key="handle"
                type="button"
                class="admin-resize-handle"
                :class="`admin-resize-handle--${handle}`"
                :style="visualResizeHandleStyle(handle, selectedVisualDraftBounds)"
                :aria-label="`Resize ${handle}`"
                @pointerdown.stop.prevent="beginVisualResize(handle, $event)"
              />
            </template>
            <svg
              v-if="visualOverlapRegionsForRender.length"
              class="admin-overlap-svg"
              :viewBox="`0 0 ${config.canvas.width} ${config.canvas.height}`"
              aria-hidden="true"
            >
              <defs>
                <mask v-for="region in visualOverlapRegionsForRender" :id="overlapMaskId(region.id)" :key="region.id" maskUnits="userSpaceOnUse">
                  <rect width="100%" height="100%" fill="black" />
                  <path :d="region.maskPath" fill="white" />
                </mask>
              </defs>
              <rect
                v-for="region in visualOverlapRegionsForRender"
                v-show="selectedVisualKind === 'overlap' && selectedVisualId === region.id"
                :key="`${region.id}-active`"
                width="100%"
                height="100%"
                class="admin-overlap-active"
                :mask="`url(#${overlapMaskId(region.id)})`"
              />
              <g
                v-for="region in visualOverlapRegionsForRender"
                :key="`${region.id}-label`"
                class="admin-overlap-label"
                :class="{ active: selectedVisualKind === 'overlap' && selectedVisualId === region.id, invalid: region.value < 0 }"
                :transform="`translate(${region.center.x}, ${region.center.y})`"
                @pointerdown.stop="selectVisual('overlap', region.id)"
              >
                <rect x="-26" y="-16" width="52" height="32" rx="8" />
                <text text-anchor="middle" dominant-baseline="central">{{ region.value }}</text>
              </g>
            </svg>
          </div>
        </div>
        <div v-if="config.mode === 'PRACTICE'" class="canvas-stage">
          <InteractivePreview :config="config" />
        </div>
      </section>

      <aside class="visual-editor__properties">
        <div class="properties-header">
          <strong>Properties</strong>
          <span>{{ selectedVisualGroup.length > 1 ? `${selectedVisualGroup.length} selected` : selectedVisualId ?? 'Nothing selected' }}</span>
        </div>
        <template v-if="selectedVisualZone()">
          <label class="field"><span>Zone ID</span><input :value="selectedVisualZone()?.id" disabled /></label>
          <label class="field"><span>Label</span><input :value="selectedVisualZone()?.label" maxlength="80" @input="updateVisualZone(selectedVisualZone()!.id, { label: ($event.target as HTMLInputElement).value })" /></label>
          <label class="field"><span>Shape</span><select :value="selectedVisualZone()?.shape" @change="updateVisualZone(selectedVisualZone()!.id, { shape: ($event.target as HTMLSelectElement).value as VisualLayerZone['shape'] })"><option value="rectangle">Rectangle</option><option value="circle">Circle</option></select></label>
          <label class="field"><span>Color</span><input type="color" :value="selectedVisualZone()?.color" @input="updateVisualZone(selectedVisualZone()!.id, { color: ($event.target as HTMLInputElement).value })" /></label>
          <div class="subsection">
            <strong>On highlight</strong>
            <label class="field"><span>Highlight color</span><input type="color" :value="selectedVisualZone()?.highlightColor ?? selectedVisualZone()?.color" @input="updateVisualZone(selectedVisualZone()!.id, { highlightColor: ($event.target as HTMLInputElement).value })" /></label>
            <label class="field"><span>Highlight opacity: {{ selectedVisualZone()?.highlightOpacity ?? 0.82 }}</span><input type="range" min="0" max="1" step="0.05" :value="selectedVisualZone()?.highlightOpacity ?? 0.82" @input="updateVisualZone(selectedVisualZone()!.id, { highlightOpacity: Number(($event.target as HTMLInputElement).value) })" /></label>
            <label class="field"><span>Zone feedback</span><textarea rows="3" :value="selectedVisualZone()?.feedback ?? ''" maxlength="500" @input="updateVisualZone(selectedVisualZone()!.id, { feedback: ($event.target as HTMLTextAreaElement).value })" /></label>
          </div>
          <div class="editor-grid">
            <label class="field"><span>X</span><input type="number" :value="selectedVisualZone()?.x" @input="updateVisualZone(selectedVisualZone()!.id, { x: Number(($event.target as HTMLInputElement).value) })" /></label>
            <label class="field"><span>Y</span><input type="number" :value="selectedVisualZone()?.y" @input="updateVisualZone(selectedVisualZone()!.id, { y: Number(($event.target as HTMLInputElement).value) })" /></label>
            <label class="field"><span>W</span><input type="number" :value="selectedVisualZone()?.width" @input="updateVisualZone(selectedVisualZone()!.id, { width: Number(($event.target as HTMLInputElement).value) })" /></label>
            <label class="field"><span>H</span><input type="number" :value="selectedVisualZone()?.height" @input="updateVisualZone(selectedVisualZone()!.id, { height: Number(($event.target as HTMLInputElement).value) })" /></label>
          </div>
          <div class="editor-grid">
            <label class="field"><span>Label X %</span><input type="number" min="0" max="100" :value="selectedVisualZone()?.labelX ?? 50" @input="updateVisualZone(selectedVisualZone()!.id, { labelX: Math.max(0, Math.min(100, Number(($event.target as HTMLInputElement).value))) })" /></label>
            <label class="field"><span>Label Y %</span><input type="number" min="0" max="100" :value="selectedVisualZone()?.labelY ?? 50" @input="updateVisualZone(selectedVisualZone()!.id, { labelY: Math.max(0, Math.min(100, Number(($event.target as HTMLInputElement).value))) })" /></label>
          </div>
        </template>
        <template v-else-if="selectedVisualElement()">
          <label class="field"><span>Trigger ID</span><input :value="selectedVisualElement()?.id" disabled /></label>
          <label class="field"><span>Label</span><input :value="selectedVisualElement()?.label" maxlength="120" @input="updateVisualElement(selectedVisualElement()!.id, { label: ($event.target as HTMLInputElement).value })" /></label>
          <label class="field"><span>Kind</span><select :value="selectedVisualElement()?.kind" @change="updateVisualElement(selectedVisualElement()!.id, { kind: ($event.target as HTMLSelectElement).value as VisualLayerElement['kind'] })"><option value="button">Button</option><option value="hotspot">Hotspot</option></select></label>
          <div class="editor-grid">
            <label class="field"><span>X</span><input type="number" :value="selectedVisualElement()?.x" @input="updateVisualElement(selectedVisualElement()!.id, { x: Number(($event.target as HTMLInputElement).value) })" /></label>
            <label class="field"><span>Y</span><input type="number" :value="selectedVisualElement()?.y" @input="updateVisualElement(selectedVisualElement()!.id, { y: Number(($event.target as HTMLInputElement).value) })" /></label>
            <label class="field"><span>W</span><input type="number" :value="selectedVisualElement()?.width" @input="updateVisualElement(selectedVisualElement()!.id, { width: Number(($event.target as HTMLInputElement).value) })" /></label>
            <label class="field"><span>H</span><input type="number" :value="selectedVisualElement()?.height" @input="updateVisualElement(selectedVisualElement()!.id, { height: Number(($event.target as HTMLInputElement).value) })" /></label>
          </div>
          <div class="subsection">
            <strong>On click</strong>
            <label class="field"><span>Effect</span><select :value="selectedVisualInteraction()?.effect ?? 'HIGHLIGHT_ZONE'" @change="updateVisualInteraction({ effect: ($event.target as HTMLSelectElement).value as VisualLayerInteraction['effect'] })"><option value="HIGHLIGHT_ZONE">Highlight zone</option><option value="SHOW_FEEDBACK">Show feedback</option></select></label>
            <label class="field"><span>Target zone</span><select :value="selectedVisualInteraction()?.targetZoneId ?? config.zones[0]?.id" @change="updateVisualInteraction({ targetZoneId: ($event.target as HTMLSelectElement).value })"><option v-for="zone in config.zones" :key="zone.id" :value="zone.id">{{ zone.label }}</option></select></label>
            <label class="field"><span>Feedback</span><textarea rows="3" :value="selectedVisualInteraction()?.feedback ?? ''" maxlength="500" @input="updateVisualInteraction({ feedback: ($event.target as HTMLTextAreaElement).value })" /></label>
          </div>
        </template>
        <template v-else-if="selectedOverlapValue()">
          <label class="field"><span>Region ID</span><input :value="selectedOverlapValue()?.id" disabled /></label>
          <label class="field"><span>Label</span><input :value="selectedOverlapValue()?.label" maxlength="120" @input="updateOverlapValue(selectedOverlapValue()!.id, { label: ($event.target as HTMLInputElement).value })" /></label>
          <label class="field"><span>Computed exact value</span><input :value="selectedOverlapValue()?.value" disabled /></label>
          <label class="field"><span>Feedback</span><textarea rows="3" :value="selectedOverlapValue()?.feedback ?? ''" maxlength="500" @input="updateOverlapValue(selectedOverlapValue()!.id, { feedback: ($event.target as HTMLTextAreaElement).value })" /></label>
        </template>
        <p v-else-if="selectedVisualGroup.length > 1" class="empty-note">ลาก object ในกลุ่มเพื่อขยับพร้อมกัน หรือใช้ toolbar บน canvas</p>
        <p v-else class="empty-note">เลือก zone หรือ trigger เพื่อแก้ค่า</p>
        <button v-if="selectedVisualId && selectedVisualKind !== 'overlap'" type="button" class="mini-button" @click="removeSelectedVisual">Delete selected</button>
      </aside>
    </div>

    <div v-else-if="config?.type === 'QUIZ'" class="space-y-3">
      <label class="field"><span>Title</span><input :value="config.title" maxlength="120" @input="patchConfig({ title: ($event.target as HTMLInputElement).value })" /></label>
      <label class="field"><span>Question</span><textarea rows="3" :value="config.question" maxlength="500" @input="patchConfig({ question: ($event.target as HTMLTextAreaElement).value })" /></label>
      <div class="quiz-answer-grid">
        <div v-for="(option, index) in config.options" :key="`${option.id}-${index}`" class="quiz-answer-card" :class="{ 'quiz-answer-card--correct': option.correct }">
          <div class="quiz-answer-card__main">
            <button
              type="button"
              class="quiz-correct-button"
              :class="{ 'quiz-correct-button--selected': option.correct }"
              :aria-pressed="option.correct"
              :aria-label="`Mark option ${index + 1} as correct`"
              @click="setCorrectQuizOption(index)"
            >
              <span v-if="option.correct" class="quiz-correct-button__dot" />
            </button>
            <input
              class="quiz-answer-input"
              :value="option.label"
              maxlength="250"
              :aria-label="`Option ${index + 1}`"
              @input="updateQuizOption(index, { label: ($event.target as HTMLInputElement).value })"
            />
          </div>
          <div class="quiz-answer-card__meta">
            <label class="quiz-id-chip">
              <span>ID</span>
              <input :value="option.id" maxlength="24" @input="updateQuizOption(index, { id: ($event.target as HTMLInputElement).value })" />
            </label>
            <span class="quiz-answer-state">{{ option.correct ? 'Correct answer' : 'Distractor' }}</span>
            <button type="button" class="mini-button mini-button--icon" :disabled="config.options.length <= 2" aria-label="Remove option" title="Remove option" @click="removeQuizOption(index)">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M4 7h16" />
                <path d="M10 11v6" />
                <path d="M14 11v6" />
                <path d="M6 7l1 14h10l1-14" />
                <path d="M9 7V4h6v3" />
              </svg>
            </button>
          </div>
        </div>
        <button type="button" class="quiz-add-card" :disabled="config.options.length >= 6" @click="addQuizOption">Add option</button>
      </div>
    </div>

    <div v-if="config?.mode === 'PRACTICE' && config.type !== 'VISUAL_LAYER'" class="editor-grid">
      <label class="field"><span>Success feedback</span><textarea rows="2" :value="config.feedback?.success ?? ''" maxlength="500" @input="patchConfig({ feedback: { success: ($event.target as HTMLTextAreaElement).value, failure: config.feedback?.failure ?? '' } })" /></label>
      <label class="field"><span>Failure feedback</span><textarea rows="2" :value="config.feedback?.failure ?? ''" maxlength="500" @input="patchConfig({ feedback: { success: config.feedback?.success ?? '', failure: ($event.target as HTMLTextAreaElement).value } })" /></label>
    </div>
    </section>

    </div>

    <div v-else-if="activeStudioTab === 'Preview'" class="interactive-config-preview">
      <div v-if="config" class="space-y-3">
        <div class="mode-row" role="group" aria-label="Simulated progress status">
          <button type="button" :class="{ active: simulatedProgressStatus === 'NOT_STARTED' }" @click="simulatedProgressStatus = 'NOT_STARTED'">Not started</button>
          <button type="button" :class="{ active: simulatedProgressStatus === 'TRIED' }" @click="simulatedProgressStatus = 'TRIED'">Tried</button>
          <button type="button" :class="{ active: simulatedProgressStatus === 'MASTERED' }" @click="simulatedProgressStatus = 'MASTERED'">Mastered</button>
        </div>
        <InteractiveChallengeShell
          :interaction-type="config.type"
          :objective="previewObjective"
          :status="simulatedProgressStatus"
          :mode="config.mode ?? 'VISUALIZATION'"
        >
          <InteractivePreview :config="config" />
        </InteractiveChallengeShell>
      </div>
      <div v-else class="json-panel">
        <p class="empty-note">Select a template or apply a preset to preview the learner challenge.</p>
      </div>
    </div>

    <div v-else class="json-panel">
      <div class="json-panel__header">
        <div>
          <h4>{{ templateType === 'OTHER' ? 'Custom JSON' : 'Advanced JSON' }}</h4>
          <span>{{ config ? `${config.type} / ${config.mode ?? 'VISUALIZATION'} / ${jsonStats}` : jsonStats }}</span>
        </div>
        <div class="json-panel__actions">
          <button type="button" class="mini-button" @click="formatAdvancedJson">Format</button>
          <button v-if="config" type="button" class="mini-button" @click="resetAdvancedJson">Reset</button>
          <button type="button" class="mini-button mini-button--dark" @click="applyAdvancedJson">Apply</button>
        </div>
      </div>
      <textarea
        v-model="jsonDraft"
        rows="22"
        spellcheck="false"
        class="json-box json-box--advanced"
        placeholder="{&quot;type&quot;:&quot;QUIZ&quot;}"
      />
      <p v-if="jsonError" class="json-error">{{ jsonError }}</p>
    </div>
  </div>
</template>

<style scoped>
.interactive-config-shell {
  display: grid;
  gap: 1rem;
}
.studio-tabs,
.interactive-config-controls,
.interactive-config-preview,
.interactive-config-shell > .json-panel {
  grid-column: 1 / -1;
}
.studio-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  border-bottom: 2px solid #d4cec6;
  padding-bottom: 0.55rem;
}
.studio-tab {
  min-height: 2.25rem;
  border: 2px solid #1a1814;
  border-radius: 999px;
  background: #fbf7ef;
  padding: 0 0.9rem;
  color: #1a1814;
  font-size: 12px;
  font-weight: 900;
}
.studio-tab.active {
  background: #1a1814;
  color: #fbf7ef;
}
.studio-section {
  display: grid;
  gap: 0.75rem;
  border: 2px solid #d4cec6;
  border-radius: 10px;
  background: #fffdf8;
  padding: 0.85rem;
}
.studio-section--fields {
  background: transparent;
}
.studio-section__header {
  display: flex;
  flex-wrap: wrap;
  align-items: end;
  justify-content: space-between;
  gap: 0.75rem;
}
.studio-section__header h3 {
  margin: 0;
  color: #1a1814;
  font-size: 13px;
  font-weight: 900;
}
.studio-section__header p {
  margin: 0.15rem 0 0;
  color: #6b6660;
  font-size: 11px;
  font-weight: 750;
  line-height: 1.35;
}
.compact-select {
  display: grid;
  gap: 0.25rem;
  min-width: min(100%, 220px);
}
.compact-select span {
  color: #6b6660;
  font-size: 10px;
  font-weight: 900;
  text-transform: uppercase;
}
.compact-select select {
  height: 2.2rem;
  border: 2px solid #d4cec6;
  border-radius: 8px;
  background: #f7f2ea;
  padding: 0 0.55rem;
  color: #1a1814;
  font-size: 12px;
  font-weight: 800;
  outline: none;
}
.compact-select select:focus {
  border-color: #1a1814;
  background: #fffdf8;
}
.template-card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 0.5rem;
}
.template-card {
  min-height: 5.4rem;
  border: 2px solid #d4cec6;
  border-radius: 8px;
  background: #fbf7ef;
  padding: 0.65rem;
  text-align: left;
  color: #1a1814;
  transition: border-color 150ms ease, transform 150ms ease, box-shadow 150ms ease;
}
.template-card:hover,
.template-card.active {
  border-color: #1a1814;
  box-shadow: 2px 2px 0 #1a1814;
  transform: translateY(-1px);
}
.template-card strong {
  display: block;
  margin-bottom: 0.25rem;
  font-size: 12px;
  font-weight: 900;
}
.template-card span {
  display: block;
  color: #6b6660;
  font-size: 11px;
  font-weight: 750;
  line-height: 1.35;
}
.preset-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}
.preset-button {
  min-height: 2rem;
  border: 2px solid #d4cec6;
  border-radius: 999px;
  background: #fbf7ef;
  padding: 0 0.7rem;
  color: #1a1814;
  font-size: 11px;
  font-weight: 900;
}
.preset-button:hover {
  border-color: #1a1814;
  background: #ffd333;
}
.interactive-config-controls,
.interactive-config-preview {
  min-width: 0;
}
.preview-label {
  margin-bottom: 0.4rem;
  color: #6b6660;
  font-size: 11px;
  font-weight: 900;
  text-transform: uppercase;
}
@media (min-width: 1200px) {
  .interactive-config-shell {
    grid-template-columns: 1fr;
    align-items: start;
  }
}
.editor-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.6rem;
}
.field {
  display: grid;
  gap: 0.25rem;
  min-width: 0;
}
.field--wide {
  grid-column: 1 / -1;
}
.field span {
  font-size: 11px;
  font-weight: 800;
  color: #6b6660;
}
.field input,
.field select,
.field textarea,
.json-box {
  width: 100%;
  border: 2px solid #d4cec6;
  border-radius: 8px;
  background: #f7f2ea;
  padding: 0.45rem 0.6rem;
  color: #1a1814;
  font-size: 12px;
  outline: none;
}
.field textarea,
.json-box {
  resize: vertical;
}
.field input:focus,
.field select:focus,
.field textarea:focus,
.json-box:focus {
  border-color: #1a1814;
  background: #fffdf8;
}
.json-box {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
  line-height: 1.45;
}
.nested-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.5rem;
  border: 2px solid #e4ded6;
  border-radius: 8px;
  padding: 0.6rem;
}
.nested-row--quiz {
  grid-template-columns: auto 70px minmax(0, 1fr) auto;
  align-items: end;
}
.quiz-answer-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.65rem;
}
.quiz-answer-card,
.quiz-add-card {
  min-width: 0;
  border: 2px solid #1a1814;
  border-radius: 8px;
  background: #fffdf8;
  color: #1a1814;
}
.quiz-answer-card {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
  padding: 0.65rem;
  box-shadow: 2px 2px 0 transparent;
}
.quiz-answer-card--correct {
  background: #ffd333;
  box-shadow: 2px 2px 0 #1a1814;
}
.quiz-answer-card__main {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: 0.6rem;
  min-height: 3.1rem;
}
.quiz-correct-button {
  display: grid;
  width: 1.6rem;
  height: 1.6rem;
  place-items: center;
  border: 2px solid #1a1814;
  border-radius: 999px;
  background: #fffdf8;
  color: #1a1814;
  font-size: 13px;
  font-weight: 950;
}
.quiz-correct-button:hover {
  background: #f0ece4;
}
.quiz-correct-button--selected {
  background: #1a1814;
  color: #ffd333;
}
.quiz-correct-button__dot {
  width: 0.55rem;
  height: 0.55rem;
  border-radius: 999px;
  background: #ffd333;
}
.quiz-answer-input {
  width: 100%;
  min-width: 0;
  border: 0;
  background: transparent;
  color: #1a1814;
  font-size: 14px;
  font-weight: 900;
  outline: none;
}
.quiz-answer-input:focus {
  border-radius: 6px;
  background: rgba(255, 253, 248, 0.7);
  box-shadow: 0 0 0 2px rgba(26, 24, 20, 0.2);
}
.quiz-answer-card__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.45rem;
  padding-left: 1.65rem;
}
.quiz-id-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  border: 2px solid #d4cec6;
  border-radius: 999px;
  background: #fbf7ef;
  padding: 0.15rem 0.45rem;
}
.quiz-id-chip span,
.quiz-answer-state {
  color: #6b6660;
  font-size: 10px;
  font-weight: 900;
  text-transform: uppercase;
}
.quiz-id-chip input {
  width: 2.4rem;
  border: 0;
  background: transparent;
  color: #1a1814;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
  font-weight: 900;
  outline: none;
}
.quiz-answer-state {
  border-radius: 999px;
  background: #f0ece4;
  padding: 0.25rem 0.5rem;
}
.quiz-answer-card--correct .quiz-answer-state {
  background: #dff4df;
  color: #245e3e;
}
.quiz-add-card {
  min-height: 4.5rem;
  border-style: dashed;
  background: #fbf7ef;
  font-size: 13px;
  font-weight: 900;
}
.quiz-add-card:hover:not(:disabled) {
  background: #ffd333;
}
.quiz-add-card:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}
.section-card,
.option-card,
.subsection {
  display: grid;
  gap: 0.75rem;
}
.section-card {
  border: 2px solid #d4cec6;
  border-radius: 10px;
  background: #fffdf8;
  padding: 0.75rem;
}
.section-card__header,
.option-card__header,
.subsection__header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 0.65rem;
}
.section-card__header h4 {
  margin: 0;
  color: #1a1814;
  font-size: 13px;
  font-weight: 900;
}
.section-card__header p,
.empty-note {
  margin: 0;
  color: #6b6660;
  font-size: 11px;
  font-weight: 700;
  line-height: 1.45;
}
.option-card {
  border: 2px solid #e4ded6;
  border-radius: 10px;
  background: #f7f2ea;
  padding: 0.7rem;
}
.option-card__header strong,
.subsection__header strong {
  color: #1a1814;
  font-size: 12px;
  font-weight: 900;
}
.formula-builder {
  display: grid;
  grid-template-columns: minmax(190px, 0.55fr) minmax(420px, 1.45fr) minmax(320px, 0.9fr);
  gap: 0.75rem;
  align-items: start;
}
.formula-builder__list,
.formula-builder__canvas,
.formula-builder__properties {
  min-width: 0;
}
.formula-builder__list {
  display: grid;
  gap: 0.55rem;
  border: 2px solid #d4cec6;
  border-radius: 10px;
  background: #fffdf8;
  padding: 0.65rem;
}
.formula-builder__list,
.formula-builder__properties {
  position: sticky;
  top: 1rem;
  max-height: calc(100vh - 2rem);
  overflow: auto;
}
.formula-builder__canvas {
  display: grid;
  gap: 0.65rem;
}
.canvas-toolbar,
.properties-header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  border: 2px solid #d4cec6;
  border-radius: 10px;
  background: #fffdf8;
  padding: 0.65rem 0.75rem;
}
.canvas-toolbar strong,
.properties-header strong {
  display: block;
  color: #1a1814;
  font-size: 13px;
  font-weight: 900;
}
.canvas-toolbar span,
.properties-header span {
  display: block;
  color: #6b6660;
  font-size: 11px;
  font-weight: 800;
}
.canvas-stage {
  min-height: 520px;
  overflow: auto;
  border: 2px solid #1a1814;
  border-radius: 12px;
  background:
    linear-gradient(90deg, rgba(26, 24, 20, 0.045) 1px, transparent 1px),
    linear-gradient(rgba(26, 24, 20, 0.045) 1px, transparent 1px),
    #f7f2ea;
  background-size: 24px 24px;
  padding: 1rem;
}
.canvas-stage :deep(.interactive-preview) {
  max-width: 100%;
  min-height: 480px;
  background: #fffdf8;
}
.visual-editor {
  display: grid;
  grid-template-columns: minmax(210px, 0.48fr) minmax(640px, 1.7fr) minmax(260px, 0.62fr);
  gap: 0.75rem;
  align-items: start;
}
.visual-editor__layers,
.visual-editor__properties {
  display: grid;
  gap: 0.75rem;
  position: sticky;
  top: 1rem;
  max-height: calc(100vh - 2rem);
  overflow: auto;
  border: 2px solid #d4cec6;
  border-radius: 10px;
  background: #fffdf8;
  padding: 0.75rem;
}
.visual-editor__canvas {
  display: grid;
  gap: 0.65rem;
  min-width: 0;
}
.tool-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
}
.mini-button.active {
  background: #ffd333;
}
.layer-group {
  display: grid;
  gap: 0.45rem;
}
.layer-group strong {
  color: #1a1814;
  font-size: 12px;
  font-weight: 900;
}
.layer-item {
  border: 2px solid #d4cec6;
  border-radius: 8px;
  background: #f7f2ea;
  padding: 0.5rem 0.6rem;
  color: #1a1814;
  font-size: 12px;
  font-weight: 900;
  text-align: left;
}
.layer-item.active,
.layer-item:hover {
  border-color: #1a1814;
  background: #ffd333;
}
.visual-admin-stage-wrap {
  max-width: 100%;
  overflow: auto;
  border: 2px solid #1a1814;
  border-radius: 12px;
  background: #f7f2ea;
  padding: 0.75rem;
}
.visual-admin-stage {
  position: relative;
  overflow: hidden;
  cursor: default;
  user-select: none;
  touch-action: none;
  background:
    linear-gradient(90deg, rgba(26, 24, 20, 0.055) 1px, transparent 1px),
    linear-gradient(rgba(26, 24, 20, 0.055) 1px, transparent 1px),
    #fffdf8;
  background-size: 24px 24px;
}
.visual-admin-stage--drawing {
  cursor: crosshair;
}
.visual-bg-text {
  position: absolute;
  inset: 1rem;
  margin: 0;
  color: #8f887e;
  font-size: 20px;
  font-weight: 900;
  line-height: 1.35;
  pointer-events: none;
}
.admin-zone,
.admin-trigger,
.admin-draft {
  position: absolute;
  display: grid;
  place-items: center;
  border: 2px solid #1a1814;
  border-radius: 8px;
  color: #1a1814;
  font-size: 12px;
  font-weight: 900;
  user-select: none;
}
.admin-zone {
  z-index: 2;
  background: color-mix(in srgb, var(--zone-color) 35%, transparent);
  cursor: move;
}
.admin-zone span {
  position: absolute;
  left: var(--zone-label-x, 50%);
  top: var(--zone-label-y, 50%);
  transform: translate(-50%, -50%);
  cursor: grab;
  pointer-events: auto;
}
.admin-zone--circle,
.admin-draft--circle {
  border-radius: 999px;
}
.admin-zone.active,
.admin-trigger.active {
  box-shadow: 0 0 0 5px rgba(255, 211, 51, 0.45);
}
.admin-zone.active {
  background: var(--zone-highlight-color);
  opacity: var(--zone-highlight-opacity);
}
.admin-trigger {
  z-index: 3;
  background: #ffd333;
  cursor: move;
}
.admin-trigger--hotspot {
  background: rgba(255, 211, 51, 0.45);
}
.admin-overlap-svg {
  position: absolute;
  inset: 0;
  z-index: 5;
  width: 100%;
  height: 100%;
  pointer-events: none;
}
.admin-overlap-active {
  fill: rgba(225, 95, 65, 0.42);
  pointer-events: none;
}
.admin-overlap-label {
  cursor: pointer;
  pointer-events: auto;
}
.admin-overlap-label rect {
  fill: #fffdf8;
  stroke: #1a1814;
  stroke-width: 2;
}
.admin-overlap-label text {
  fill: #1a1814;
  font-size: 13px;
  font-weight: 900;
}
.admin-overlap-label.active rect,
.admin-overlap-label:hover rect {
  fill: #ffd333;
}
.admin-overlap-label.invalid rect {
  fill: #ffe7e0;
  stroke: #c93f24;
}
.admin-overlap-label.invalid text {
  fill: #9b2614;
}
.layer-item.invalid {
  border-color: #c93f24;
  color: #9b2614;
}
.overlap-warning {
  margin: 0;
  color: #9b2614;
  font-size: 12px;
  font-weight: 800;
}
.overlap-input-grid {
  display: grid;
  gap: 8px;
}
.admin-draft {
  border-style: dashed;
  background: rgba(255, 211, 51, 0.24);
  pointer-events: none;
}
.admin-draft--element {
  background: rgba(79, 140, 255, 0.18);
}
.admin-marquee {
  position: absolute;
  z-index: 20;
  border: 2px dashed #1a1814;
  border-radius: 6px;
  background: rgba(255, 211, 51, 0.18);
  pointer-events: none;
}
.admin-edit-toolbar {
  position: absolute;
  z-index: 30;
  display: flex;
  align-items: center;
  gap: 0.35rem;
  transform: translate(-50%, -100%);
  border: 2px solid #1a1814;
  border-radius: 8px;
  background: #fffdf8;
  padding: 0.25rem;
  box-shadow: 0 8px 18px rgba(26, 24, 20, 0.16);
}
.admin-edit-toolbar__count {
  padding: 0 0.35rem;
  color: #6b6660;
  font-size: 11px;
  font-weight: 900;
  white-space: nowrap;
}
.admin-edit-toolbar__button {
  min-height: 1.75rem;
  border: 0;
  border-radius: 6px;
  background: transparent;
  padding: 0 0.5rem;
  color: #1a1814;
  font-size: 11px;
  font-weight: 900;
}
.admin-edit-toolbar__button:hover {
  background: #f7f2ea;
}
.admin-edit-toolbar__button--danger {
  color: #9b2614;
}
.admin-edit-toolbar__button--confirm {
  background: #ffd333;
}
.admin-resize-handle {
  position: absolute;
  z-index: 25;
  width: 12px;
  height: 12px;
  border: 2px solid #1a1814;
  border-radius: 999px;
  background: #fffdf8;
  transform: translate(-50%, -50%);
}
.admin-resize-handle:hover {
  background: #ffd333;
}
.admin-resize-handle--nw,
.admin-resize-handle--se {
  cursor: nwse-resize;
}
.admin-resize-handle--ne,
.admin-resize-handle--sw {
  cursor: nesw-resize;
}
.admin-resize-handle--n,
.admin-resize-handle--s {
  cursor: ns-resize;
}
.admin-resize-handle--e,
.admin-resize-handle--w {
  cursor: ew-resize;
}
.section-list-item {
  display: grid;
  gap: 0.2rem;
  width: 100%;
  border: 2px solid #d4cec6;
  border-radius: 8px;
  background: #f7f2ea;
  padding: 0.55rem 0.65rem;
  text-align: left;
}
.section-list-item span {
  overflow: hidden;
  color: #1a1814;
  font-size: 12px;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.section-list-item small {
  overflow: hidden;
  color: #6b6660;
  font-size: 10px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.section-list-item:hover,
.section-list-item.active {
  border-color: #1a1814;
  background: #ffd333;
}
.subsection {
  border-top: 2px solid #e4ded6;
  padding-top: 0.7rem;
}
.overlap-source-list {
  display: grid;
  gap: 0.35rem;
}
.mini-button {
  min-height: 2rem;
  border: 2px solid #1a1814;
  border-radius: 8px;
  background: #fffdf8;
  padding: 0 0.65rem;
  color: #1a1814;
  font-size: 11px;
  font-weight: 800;
}
.mini-button:hover {
  background: #ffd333;
}
.mini-button--dark {
  background: #1a1814;
  color: #fffdf8;
}
.mini-button--dark:hover {
  background: #3a332d;
}
.mini-button:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}
.json-panel {
  overflow: hidden;
  border: 2px solid #d4cec6;
  border-radius: 10px;
  background: #fffdf8;
}
.json-panel__header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  border-bottom: 2px solid #e4ded6;
  padding: 0.75rem;
}
.json-panel__header h4 {
  margin: 0;
  color: #1a1814;
  font-size: 13px;
  font-weight: 900;
}
.json-panel__header span {
  color: #6b6660;
  font-size: 11px;
  font-weight: 800;
}
.json-panel__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}
.json-box--advanced {
  display: block;
  min-height: 24rem;
  max-height: 70vh;
  overflow: auto;
  border: 0;
  border-radius: 0;
  background: #1f1d1a;
  padding: 0.9rem;
  color: #f8f1e7;
  font-size: 12px;
  line-height: 1.65;
  tab-size: 2;
}
.json-box--advanced:focus {
  background: #1f1d1a;
  color: #f8f1e7;
}
.json-error {
  margin: 0;
  border-top: 2px solid #e4ded6;
  padding: 0.65rem 0.75rem;
  color: #8c3322;
  font-size: 12px;
  font-weight: 900;
}
.mode-row {
  display: inline-grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  overflow: hidden;
  border: 2px solid #1a1814;
  border-radius: 8px;
}
.mode-row button {
  min-height: 2rem;
  background: #fffdf8;
  padding: 0 0.8rem;
  color: #1a1814;
  font-size: 11px;
  font-weight: 800;
}
.mode-row button.active {
  background: #ffd333;
}
.mode-row--fixed {
  grid-template-columns: 1fr;
  width: max-content;
  background: #ffd333;
  padding: 0.45rem 0.8rem;
  color: #1a1814;
  font-size: 11px;
  font-weight: 900;
}
.check-field {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 11px;
  font-weight: 800;
}
@media (max-width: 700px) {
  .editor-grid,
  .nested-row,
  .nested-row--quiz,
  .quiz-answer-grid,
  .formula-builder,
  .visual-editor {
    grid-template-columns: 1fr;
  }
  .formula-builder__list,
  .formula-builder__properties,
  .visual-editor__layers,
  .visual-editor__properties {
    position: static;
    max-height: none;
  }
}
</style>
