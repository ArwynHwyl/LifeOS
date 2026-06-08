<template>
  <div class="flex flex-wrap gap-4 items-start">
    <!-- Transformation track -->
    <div class="flex-[1_1_340px] min-w-[320px] flex flex-col gap-3">
      <!-- progress -->
      <div class="flex items-center gap-2">
        <p class="font-mono text-[10px] font-bold tracking-[0.1em] uppercase text-lm-ink-3 m-0 whitespace-nowrap shrink-0">
          Step {{ Math.min(pos + (done ? 0 : 1), steps.length) }} / {{ steps.length }}
        </p>
        <div class="flex-1 h-1.5 rounded-full bg-lm-bg-soft border-[1.5px] border-lm-line-soft overflow-hidden">
          <div class="h-full bg-lm-yellow transition-[width] duration-350" :style="{ width: `${steps.length ? (pos / steps.length) * 100 : 0}%` }" />
        </div>
        <button v-if="interactive && pos > 0" @click="resetStepper" class="font-mono text-[9px] font-bold tracking-[0.06em] uppercase border-2 border-lm-line-soft rounded-full bg-lm-surface text-lm-ink-3 px-2.5 py-[3px] cursor-pointer">
          Reset
        </button>
      </div>

      <!-- start line -->
      <div class="border-2 border-lm-line-soft rounded-[14px] bg-lm-surface p-[14px_18px] bg-[#fffdf8]">
        <p class="font-mono text-[9px] font-bold tracking-[0.1em] uppercase text-lm-ink-3 m-0 mb-1.5 text-left">Start</p>
        <p class="font-math italic text-[24px] font-bold text-lm-ink m-0 text-left">{{ lsPretty(start) }}</p>
      </div>

      <!-- completed steps -->
      <div
        v-for="(s, i) in steps.slice(0, pos)"
        :key="i"
        class="border-2 border-lm-line-soft rounded-[14px] bg-lm-surface p-[12px_16px] border-lm-line shadow-stamp-sm ls-pop"
      >
        <div class="flex items-center gap-2 mb-1.75">
          <span class="font-mono text-[9px] font-bold tracking-[0.04em] uppercase px-[9px] py-[2px] rounded-full border-2 border-lm-line bg-lm-yellow text-lm-ink">{{ getLawName(s.law) }}</span>
          <span class="font-display text-[12px] text-lm-ink-2">{{ s.note || getLawNote(s.law) }}</span>
        </div>
        <p class="font-math italic text-[24px] font-bold text-lm-ink m-0 text-left">{{ lsPretty(s.result) }}</p>
      </div>

      <!-- frontier — pick a law -->
      <div v-if="!done && interactive" class="border-2 border-lm-line-soft rounded-[14px] bg-lm-surface p-[14px_16px] border-dashed">
        <div class="flex items-center justify-between mb-2.5">
          <p class="font-display text-[13px] font-bold text-lm-ink m-0">Which law transforms this next?</p>
          <button @click="showHint = !showHint" class="font-mono text-[9px] font-bold tracking-[0.06em] uppercase border-2 border-lm-line rounded-full bg-lm-bg-soft text-lm-ink px-2.5 py-[3px] cursor-pointer">
            {{ showHint ? 'Hide hint' : 'Hint' }}
          </button>
        </div>
        <p v-if="showHint && nextStep" class="font-display text-[12px] text-lm-blue m-0 mb-2.5 font-semibold text-left">
          → Try {{ getLawName(nextStep.law) }}: {{ getLawNote(nextStep.law) }}
        </p>
        <div class="flex flex-wrap gap-1.75">
          <button
            v-for="id in palette"
            :key="id"
            @click="pick(id)"
            class="flex flex-col items-start gap-[3px] p-[8px_12px] cursor-pointer border-2 border-lm-line rounded-[10px] bg-lm-surface shadow-stamp-sm"
            :class="{ 'ls-shake': wrong === id }"
            :title="getLawForms(id)"
          >
            <span class="font-display text-[12px] font-bold text-lm-ink whitespace-nowrap leading-none">{{ getLawName(id) }}</span>
            <span class="font-math italic text-[11px] text-lm-ink-3 whitespace-nowrap leading-none">{{ getLawForms(id).split('·')[0].trim() }}</span>
          </button>
        </div>
      </div>

      <!-- completion -->
      <div v-if="done" class="border-2 border-lm-line-soft rounded-[14px] bg-lm-surface p-[16px_18px] border-lm-green bg-lm-green-soft ls-pop">
        <div class="flex items-center gap-2 mb-1.5">
          <span class="w-6 h-6 rounded-full bg-lm-green grid place-items-center shrink-0">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="3.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M20 6 9 17l-5-5" />
            </svg>
          </span>
          <p class="font-display text-[15px] font-[800] text-lm-green m-0">Fully simplified!</p>
        </div>
        <p class="font-display text-[13px] text-lm-ink-2 m-0 text-left">
          {{ successText || 'Different pipes — same water. The truth table never changed.' }}
        </p>
      </div>
    </div>

    <!-- Equivalence proof -->
    <div class="flex-[1_1_240px] min-w-[230px] sticky top-0">
      <div class="border-2 border-lm-line-soft rounded-[14px] bg-lm-surface overflow-hidden">
        <div class="flex items-center justify-between p-[9px_12px] bg-lm-bg-soft border-b border-lm-line-soft">
          <p class="font-mono text-[10px] font-bold tracking-[0.1em] uppercase text-lm-ink-3 m-0">Equivalence proof</p>
          <span
            class="inline-flex items-center gap-1.25 font-mono text-[9px] font-bold tracking-[0.04em] uppercase px-[9px] py-[3px] border-2 border-lm-line rounded-full bg-lm-red-soft text-lm-red"
            :class="{ 'bg-lm-green-soft text-lm-green': proofResult.identical }"
          >
            {{ proofResult.identical ? '≡ IDENTICAL' : '✕ DIFFERS' }}
          </span>
        </div>
        <table class="w-full border-collapse">
          <thead>
            <tr class="border-b border-lm-line-soft">
              <th
                v-for="v in proofResult.vars"
                :key="v"
                class="font-math italic text-[12px] normal-case color-lm-ink-3 p-[4px_8px]"
              >
                {{ v }}
              </th>
              <th class="font-mono text-[9px] font-bold tracking-[0.06em] color-lm-ink-3 p-[4px_8px] uppercase border-left border-l border-lm-line-soft">Start</th>
              <th class="font-mono text-[9px] font-bold tracking-[0.06em] color-lm-ink-3 p-[4px_8px] uppercase">Now</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="(r, i) in proofResult.rows"
              :key="i"
              class="border-b border-lm-line-soft last:border-b-0"
              :class="{ 'bg-lm-red-soft': r.va !== r.vb }"
            >
              <td
                v-for="v in proofResult.vars"
                :key="v"
                class="text-center p-[3px_8px] font-mono text-[11px] text-lm-ink-3"
              >
                {{ r.env[v] ? 'T' : 'F' }}
              </td>
              <td class="text-center p-[3px_8px] font-display text-[12px] font-bold text-lm-ink-3 border-left border-l border-lm-line-soft" :class="{ 'text-lm-blue': r.va }">
                {{ r.va ? 'T' : 'F' }}
              </td>
              <td class="text-center p-[3px_8px] font-display text-[12px] font-bold text-lm-ink-3" :class="{ 'text-lm-blue': r.vb }">
                {{ r.vb ? 'T' : 'F' }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <p class="font-display text-[11px] text-lm-ink-3 m-[8px_2px_0] leading-normal text-left">
        Every legal move keeps both columns identical — that’s what “logically equivalent” means.
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { Logic } from '@/features/courses/utils/logic-engine.js'

const props = defineProps({
  start: {
    type: String,
    default: ''
  },
  steps: {
    type: Array,
    default: () => []
  },
  successText: {
    type: String,
    default: ''
  },
  interactive: {
    type: Boolean,
    default: true
  }
})

const pos = ref(0)
const wrong = ref(null)
const showHint = ref(false)

watch(() => [props.start, props.steps], () => {
  pos.value = 0
  wrong.value = null
  showHint.value = false
}, { deep: true })

const done = computed(() => pos.value >= props.steps.length)
const current = computed(() => pos.value === 0 ? props.start : props.steps[pos.value - 1].result)
const nextStep = computed(() => props.steps[pos.value])

const palette = computed(() => {
  const used = [...new Set(props.steps.map(s => s.law))]
  const distract = Logic.LAWS.map(l => l.id).filter(id => !used.includes(id)).slice(0, 3)
  const all = [...used, ...distract]
  const seed = (props.start || '').split('').reduce((acc, c) => acc + c.charCodeAt(0), 0)
  return all.map((id, i) => ({ id, k: (seed * (i + 7)) % 97 })).sort((a, b) => a.k - b.k).map(x => x.id)
})

function pick(lawId) {
  if (done.value || !nextStep.value) return
  if (lawId === nextStep.value.law) {
    pos.value++
    wrong.value = null
    showHint.value = false
  } else {
    wrong.value = lawId
    setTimeout(() => {
      if (wrong.value === lawId) wrong.value = null
    }, 350)
  }
}

function resetStepper() {
  pos.value = 0
  showHint.value = false
}

function lsPretty(str) {
  const r = Logic.tryParse(str || '')
  return r.ast ? Logic.toString(r.ast) : (str || '—')
}

function getLawName(id) {
  return Logic.LAW_BY_ID[id]?.name || id
}

function getLawNote(id) {
  return Logic.LAW_BY_ID[id]?.note || ''
}

function getLawForms(id) {
  return Logic.LAW_BY_ID[id]?.forms || ''
}

const proofResult = computed(() => {
  const a = Logic.tryParse(props.start).ast
  const b = Logic.tryParse(current.value).ast
  if (!a || !b) return { vars: [], rows: [], identical: false }
  const vars = [...new Set([...Logic.variables(a), ...Logic.variables(b)])].sort()
  const n = vars.length
  if (n > 4) return { vars: [], rows: [], identical: false }

  const rows = []
  let identical = true
  for (let m = 0; m < (1 << n); m++) {
    const env = {}
    vars.forEach((v, i) => { env[v] = !!(m & (1 << (n - 1 - i))) })
    const va = Logic.evaluate(a, env)
    const vb = Logic.evaluate(b, env)
    if (va !== vb) identical = false
    rows.push({ env, va, vb })
  }

  return { vars, rows, identical }
})

// Ensure style injected
if (typeof document !== 'undefined' && !document.getElementById('logic-simplify-css')) {
  const st = document.createElement('style')
  st.id = 'logic-simplify-css'
  st.textContent = `
    @keyframes ls-pop { 0%{transform:translateY(6px);opacity:0} 100%{transform:translateY(0);opacity:1} }
    @keyframes ls-shake { 0%,100%{transform:translateX(0)} 25%{transform:translateX(-4px)} 75%{transform:translateX(4px)} }
    .ls-pop { animation: ls-pop .32s cubic-bezier(.4,0,.2,1); }
    .ls-shake { animation: ls-shake .3s; }
  `
  document.head.appendChild(st)
}
</script>
