<template>
  <div class="flex flex-col gap-4">
    <!-- mode toggle -->
    <div>
      <p class="font-mono text-[10px] font-bold tracking-[0.12em] uppercase text-lm-ink-3 m-0 mb-2 text-left">Logic activity type</p>
      <div class="flex gap-2">
        <button
          @click="changeKind('CIRCUIT')"
          class="flex-1 py-2 px-1 font-display text-[12px] font-bold border-2 border-lm-line-soft rounded-[10px] bg-lm-surface text-lm-ink cursor-pointer shadow-none transition-all duration-150"
          :class="{ 'border-lm-line bg-lm-yellow shadow-stamp-sm': kind === 'CIRCUIT' }"
        >
          🚰 Valve circuit
        </button>
        <button
          @click="changeKind('SIMPLIFY')"
          class="flex-1 py-2 px-1 font-display text-[12px] font-bold border-2 border-lm-line-soft rounded-[10px] bg-lm-surface text-lm-ink cursor-pointer shadow-none transition-all duration-150"
          :class="{ 'border-lm-line bg-lm-yellow shadow-stamp-sm': kind === 'SIMPLIFY' }"
        >
          ∴ Simplify statement
        </button>
      </div>
      <p class="font-display text-[12px] text-lm-ink-3 m-[8px_2px_0] text-left">
        {{ kind === 'CIRCUIT'
          ? 'Beginner: learners flip valves T/F and watch water flow through the gates into the output tank.'
          : 'Advanced: learners simplify a statement one law at a time — a live truth table proves each form stays equivalent.' }}
      </p>
    </div>

    <!-- CIRCUIT MODE -->
    <div v-if="kind === 'CIRCUIT'" class="flex flex-col gap-4">
      <!-- expression -->
      <div>
        <p class="font-mono text-[10px] font-bold tracking-[0.12em] uppercase text-lm-ink-3 m-0 mb-2 text-left">Statement</p>
        <input
          v-model="expr"
          @input="onExprInput"
          class="w-full box-border font-math italic text-[18px] font-bold px-3.5 py-2.5 border-2 border-lm-line rounded-[10px] bg-lm-surface outline-none text-lm-ink"
          :class="{ 'border-lm-red': !exprParse.ast }"
          placeholder="(P ∧ Q) ∨ ¬R"
        />
        <div class="flex flex-wrap gap-1.5 mt-2">
          <button
            v-for="[sym, name] in LFE_OPS"
            :key="sym"
            @click="insertOp(sym)"
            :title="name"
            class="font-math italic text-[16px] min-w-[38px] font-bold border-2 border-lm-line rounded-[8px] bg-lm-surface px-2.75 py-1.25 cursor-pointer text-lm-ink"
          >
            {{ sym }}
          </button>
          <button @click="clearExpr" class="font-display text-[11px] font-bold border-2 border-lm-line rounded-[8px] bg-lm-surface px-2.75 py-1.25 cursor-pointer text-lm-ink-3">Clear</button>
        </div>
        <p v-if="!exprParse.ast" class="font-mono text-[10px] text-lm-red m-[6px_0_0] text-left">⚠ {{ exprParse.error }}</p>
      </div>

      <!-- goal -->
      <div>
        <p class="font-mono text-[10px] font-bold tracking-[0.12em] uppercase text-lm-ink-3 m-0 mb-2 text-left">Learner goal</p>
        <div class="flex gap-2 flex-wrap">
          <button
            v-for="[g, l] in [['TRUE', 'Make it flow (output T)'], ['FALSE', 'Stop the flow (output F)'], ['EXPLORE', 'Free explore']]"
            :key="g"
            @click="setGoal(g)"
            class="py-1.75 px-3.5 font-display text-[12px] font-bold border-2 border-lm-line-soft rounded-full bg-lm-surface text-lm-ink cursor-pointer transition-all duration-150"
            :class="{ 'border-lm-line bg-lm-yellow shadow-stamp-sm': goal === g }"
          >
            {{ l }}
          </button>
        </div>
      </div>

      <!-- live preview -->
      <div class="border-2 border-lm-line rounded-[14px] bg-[#fffdf8] overflow-hidden shadow-stamp-sm">
        <div class="flex items-center justify-between px-3.5 py-2.25 border-b-2 border-lm-line-soft bg-lm-bg-soft">
          <p class="font-mono text-[10px] font-bold tracking-[0.12em] uppercase text-lm-ink-3 m-0 mb-0">Learner preview · click valves to toggle</p>
          <span
            v-if="goal !== 'EXPLORE' && exprParse.ast"
            class="font-mono text-[9px] font-bold px-[9px] py-[3px] rounded-full border-2 border-lm-line bg-lm-bg-soft text-lm-ink-3"
            :class="{ 'bg-lm-green-soft text-lm-green border-lm-green': isGoalMet }"
          >
            {{ isGoalMet ? '✓ GOAL MET' : 'GOAL: ' + goal }}
          </span>
        </div>
        <div class="p-[10px_8px]">
          <LogicCircuit
            v-if="exprParse.ast"
            :ast="exprParse.ast"
            :env="env"
            :goal="goal === 'EXPLORE' ? null : goal"
            @toggle="toggleValve"
          />
          <p v-else class="font-mono text-[11px] text-lm-ink-3 text-center p-6">Fix the statement to preview the circuit.</p>
        </div>
      </div>

      <!-- truth table + feedback -->
      <div class="grid grid-cols-[auto_1fr] gap-3.5 items-start">
        <div class="border-2 border-lm-line-soft rounded-[12px] bg-lm-surface p-[10px_12px] text-left">
          <p class="font-mono text-[10px] font-bold tracking-[0.12em] uppercase text-lm-ink-3 m-0 mb-1.5">Truth table</p>
          <div v-if="exprParse.ast" class="max-h-[240px] overflow-y-auto">
            <table class="border-collapse">
              <thead>
                <tr>
                  <th v-for="v in variables" :key="v" class="font-math italic text-[12px] normal-case color-lm-ink-3 p-[4px_8px]">{{ v }}</th>
                  <th class="font-mono text-[9px] font-bold tracking-wider color-lm-ink-3 p-[4px_8px] uppercase border-l-[1.5px] border-lm-line-soft">OUT</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(r, i) in truthTableRows" :key="i">
                  <td v-for="v in variables" :key="v" class="text-center p-[3px_8px] font-mono text-[11px] text-lm-ink-3">{{ r.env[v] ? 'T' : 'F' }}</td>
                  <td class="text-center p-[3px_8px] font-display text-[12px] font-[800] text-lm-ink-3 border-l-[1.5px] border-lm-line-soft" :class="{ 'text-lm-blue': r.val }">{{ r.val ? 'T' : 'F' }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <p v-else class="font-mono text-[11px] text-lm-ink-3 m-0">None</p>
        </div>

        <div v-if="goal !== 'EXPLORE'" class="flex flex-col gap-2.5">
          <div>
            <p class="font-mono text-[10px] font-bold tracking-[0.12em] uppercase text-lm-ink-3 m-0 mb-1">Success feedback</p>
            <input v-model="cFb.success" @input="emitChange" class="w-full box-border font-display not-italic text-[13px] font-semibold px-3.5 py-2.5 border-2 border-lm-line-soft rounded-[10px] bg-lm-bg-soft outline-none text-lm-ink" />
          </div>
          <div>
            <p class="font-mono text-[10px] font-bold tracking-[0.12em] uppercase text-lm-ink-3 m-0 mb-1">Hint / failure</p>
            <input v-model="cFb.failure" @input="emitChange" class="w-full box-border font-display not-italic text-[13px] font-semibold px-3.5 py-2.5 border-2 border-lm-line-soft rounded-[10px] bg-lm-bg-soft outline-none text-lm-ink" />
          </div>
        </div>
      </div>
    </div>

    <!-- SIMPLIFY MODE -->
    <div v-else class="flex flex-col gap-4">
      <!-- start -->
      <div>
        <p class="font-mono text-[10px] font-bold tracking-[0.12em] uppercase text-lm-ink-3 m-0 mb-2 text-left">Starting statement</p>
        <input
          v-model="start"
          @input="onStartInput"
          class="w-full box-border font-math italic text-[18px] font-bold px-3.5 py-2.5 border-2 border-lm-line rounded-[10px] bg-lm-surface outline-none text-lm-ink"
          :class="{ 'border-lm-red': !startParse.ast }"
        />
        <p v-if="!startParse.ast" class="font-mono text-[10px] text-lm-red m-[6px_0_0] text-left">⚠ {{ startParse.error }}</p>
      </div>

      <!-- steps -->
      <div>
        <div class="flex items-center justify-between mb-2">
          <p class="font-mono text-[10px] font-bold tracking-[0.12em] uppercase text-lm-ink-3 m-0 mb-0">Simplification steps ({{ steps.length }})</p>
          <button @click="addStep" class="font-display text-[11px] font-bold border-2 border-lm-line rounded-[8px] bg-lm-surface px-2.75 py-1.25 cursor-pointer text-lm-ink">+ Add step</button>
        </div>

        <div class="flex flex-col gap-2">
          <div
            v-for="(s, i) in steps"
            :key="i"
            class="border-2 border-lm-line-soft rounded-[12px] bg-lm-surface p-[10px_12px] flex flex-col gap-1.5"
          >
            <div class="flex items-center gap-2">
              <span class="font-mono text-[10px] font-bold text-lm-ink-3">{{ i + 1 }}</span>
              <select v-model="s.law" @change="emitChange" class="flex-1 font-display text-[12px] font-bold px-2 py-1.5 border-2 border-lm-line-soft rounded-[8px] bg-lm-bg-soft text-lm-ink outline-none">
                <option v-for="l in Logic.LAWS" :key="l.id" :value="l.id">{{ l.name }}</option>
              </select>
              <span
                class="font-mono text-[9px] font-bold px-2 py-[3px] rounded-full border-2 border-lm-red bg-lm-red-soft text-lm-red shrink-0 cursor-default"
                :class="{ 'border-lm-green bg-lm-green-soft text-lm-green': isEquiv(s.result) }"
                :title="isEquiv(s.result) ? 'Result is logically equivalent to the start' : 'Result is NOT equivalent — check it'"
              >
                {{ isEquiv(s.result) ? '≡' : '✕' }}
              </span>
              <button @click="removeStep(i)" class="grid place-items-center w-7 h-7 rounded-[6px] border border-lm-line-soft bg-transparent cursor-pointer text-lm-ink-3 shrink-0">
                <AdminIcon name="trash" :size="13" />
              </button>
            </div>
            <input v-model="s.result" @input="emitChange" class="w-full box-border font-math italic text-[16px] font-bold px-3 py-2 border-2 border-lm-line-soft rounded-[10px] bg-lm-bg-soft outline-none text-lm-ink" :class="{ 'border-lm-red': !isParsedCorrectly(s.result) }" placeholder="resulting statement" />
            <input v-model="s.note" @input="emitChange" :placeholder="getLawNote(s.law)" class="w-full box-border font-display text-[12px] px-2.5 py-1.5 border-2 border-lm-line-soft rounded-[8px] bg-lm-surface text-lm-ink-2 outline-none" />
          </div>
        </div>

        <div class="mt-3.5">
          <p class="font-mono text-[10px] font-bold tracking-[0.12em] uppercase text-lm-ink-3 m-0 mb-1">Completion message</p>
          <input v-model="sFb" @input="emitChange" class="w-full box-border font-display not-italic text-[13px] font-semibold px-3.5 py-2.5 border-2 border-lm-line-soft rounded-[10px] bg-lm-bg-soft outline-none text-lm-ink" />
        </div>
      </div>

      <!-- live preview -->
      <div class="border-2 border-lm-line rounded-[14px] bg-[#fffdf8] overflow-hidden shadow-stamp-sm">
        <div class="flex items-center justify-between px-3.5 py-2.25 border-b-2 border-lm-line-soft bg-lm-bg-soft">
          <p class="font-mono text-[10px] font-bold tracking-[0.12em] uppercase text-lm-ink-3 m-0 mb-0">Learner preview · pick the law for each step</p>
        </div>
        <div class="p-[10px_8px]">
          <LogicSimplify
            v-if="startParse.ast"
            :start="start"
            :steps="steps"
            :success-text="sFb"
          />
          <p v-else class="font-mono text-[11px] text-lm-ink-3 text-center p-6">Fix the starting statement to preview.</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import MonoLabel from '../MonoLabel.vue'
import AdminIcon from '../AdminIcon.vue'
import LogicCircuit from './LogicCircuit.vue'
import LogicSimplify from './LogicSimplify.vue'
import { Logic } from '@/features/courses/utils/logic-engine.js'

const props = defineProps({
  config: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['change'])

const LFE_OPS = [['¬', 'NOT'], ['∧', 'AND'], ['∨', 'OR'], ['→', 'IMPLIES'], ['↔', 'IFF'], ['⊕', 'XOR'], ['(', ''], [')', '']]

const kind = ref(props.config?.kind || 'CIRCUIT')

// CIRCUIT state
const expr = ref(props.config?.expression || '(P ∧ Q) ∨ ¬R')
const goal = ref(props.config?.goal || 'TRUE')
const cFb = ref(props.config?.feedback ? { ...props.config.feedback } : { success: 'Nice — the water reached the tank!', failure: 'Not flowing yet. Try other valve positions.' })

// SIMPLIFY state
const start = ref(props.config?.start || '(P ∧ ¬Q) ∨ (P ∧ Q)')
const steps = ref(props.config?.steps ? JSON.parse(JSON.stringify(props.config.steps)) : [
  { law: 'DISTRIBUTIVE', result: 'P ∧ (¬Q ∨ Q)', note: '' },
  { law: 'COMPLEMENT', result: 'P ∧ T', note: '' },
  { law: 'IDENTITY', result: 'P', note: '' },
])
const sFb = ref(props.config?.feedback?.success || 'Different pipes — same water. The truth table never changed.')

const env = ref({})

const exprParse = computed(() => Logic.tryParse(expr.value))
const startParse = computed(() => Logic.tryParse(start.value))

const variables = computed(() => exprParse.value.ast ? Logic.variables(exprParse.value.ast) : [])

// Re-generate variables env when expression changes
watch(expr, () => {
  if (!exprParse.value.ast) return
  const vars = Logic.variables(exprParse.value.ast)
  const nextEnv = {}
  vars.forEach(v => {
    nextEnv[v] = v in env.value ? env.value[v] : true
  })
  env.value = nextEnv
}, { immediate: true })

watch(() => props.config, (newVal) => {
  if (!newVal || Object.keys(newVal).length === 0) return
  kind.value = newVal.kind || 'CIRCUIT'
  if (kind.value === 'CIRCUIT') {
    expr.value = newVal.expression || ''
    goal.value = newVal.goal || 'TRUE'
    cFb.value = newVal.feedback ? { ...newVal.feedback } : { success: '', failure: '' }
  } else {
    start.value = newVal.start || ''
    steps.value = newVal.steps ? JSON.parse(JSON.stringify(newVal.steps)) : []
    sFb.value = newVal.feedback?.success || ''
  }
}, { deep: true })

const isGoalMet = computed(() => {
  if (!exprParse.value.ast) return false
  const result = Logic.evaluate(exprParse.value.ast, env.value)
  return goal.value === 'TRUE' ? result : !result
})

const truthTableRows = computed(() => {
  if (!exprParse.value.ast) return []
  return Logic.truthTable(exprParse.value.ast).rows
})

function changeKind(newKind) {
  kind.value = newKind
  emitChange()
}

function onExprInput() {
  emitChange()
}

function onStartInput() {
  emitChange()
}

function insertOp(sym) {
  const e = expr.value
  expr.value = (e + (e && !e.endsWith(' ') ? ' ' : '') + sym + ' ').replace(/\s+/g, ' ')
  emitChange()
}

function clearExpr() {
  expr.value = ''
  emitChange()
}

function setGoal(g) {
  goal.value = g
  emitChange()
}

function toggleValve(name) {
  env.value[name] = !env.value[name]
}

function addStep() {
  const last = steps.value.length ? steps.value[steps.value.length - 1].result : start.value
  steps.value.push({
    law: 'IDENTITY',
    result: last,
    note: ''
  })
  emitChange()
}

function removeStep(i) {
  steps.value = steps.value.filter((_, j) => j !== i)
  emitChange()
}

function getLawNote(lawId) {
  return Logic.LAW_BY_ID[lawId]?.note || 'plain-language explanation (optional)'
}

function isEquiv(resStr) {
  if (!startParse.value.ast) return false
  const rp = Logic.tryParse(resStr)
  return rp.ast ? Logic.equivalent(rp.ast, startParse.value.ast) : false
}

function isParsedCorrectly(resStr) {
  return Logic.tryParse(resStr).ast !== null
}

function emitChange() {
  if (kind.value === 'CIRCUIT') {
    emit('change', {
      type: 'LOGIC_FLOW',
      kind: 'CIRCUIT',
      mode: goal.value === 'EXPLORE' ? 'VISUALIZATION' : 'PRACTICE',
      expression: expr.value,
      variables: variables.value,
      goal: goal.value,
      feedback: cFb.value
    })
  } else {
    emit('change', {
      type: 'LOGIC_FLOW',
      kind: 'SIMPLIFY',
      mode: 'PRACTICE',
      start: start.value,
      steps: steps.value,
      feedback: { success: sFb.value }
    })
  }
}
</script>
