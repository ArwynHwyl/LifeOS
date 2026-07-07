<template>
  <div class="w-full">
    <svg :viewBox="`0 0 ${W} ${H}`" class="block w-full" :style="{ height: height || 'auto' }">
      <defs>
        <linearGradient id="lc-tank" x1="0" y1="1" x2="0" y2="0">
          <stop offset="0%" stop-color="#3b6cb5" />
          <stop offset="100%" stop-color="#5b9bd8" />
        </linearGradient>
      </defs>

      <!-- supply main (always wet) -->
      <line
        :x1="PADX - 48"
        :y1="PADY - 10"
        :x2="PADX - 48"
        :y2="H - PADY + 10"
        stroke="#3b6cb5"
        stroke-width="9"
        stroke-linecap="round"
        opacity="0.5"
      />
      <line
        :x1="PADX - 48"
        :y1="PADY - 10"
        :x2="PADX - 48"
        :y2="H - PADY + 10"
        stroke="#5b9bd8"
        stroke-width="3"
        stroke-linecap="round"
        stroke-dasharray="3 9"
        class="lc-wet"
      />
      <text
        :x="PADX - 48"
        :y="PADY - 22"
        text-anchor="middle"
        class="font-mono text-[10px] font-bold fill-lm-ink-3 tracking-wider"
      >
        SUPPLY
      </text>

      <!-- stubs from supply to each leaf valve -->
      <line
        v-for="n in leafNodes"
        :key="'stub' + n.id"
        :x1="PADX - 48"
        :y1="yOf(n.slot)"
        :x2="n.x - n.rw"
        :y2="yOf(n.slot)"
        stroke="#3b6cb5"
        stroke-width="7"
        stroke-linecap="round"
        opacity="0.5"
      />

      <!-- pipes (dry base + wet overlay) -->
      <g v-for="(e, i) in edges" :key="'e' + i">
        <path
          :d="pipePath(e)"
          fill="none"
          :stroke="e.wet ? '#3b6cb5' : '#d8d2c4'"
          stroke-width="8"
          stroke-linecap="round"
          :opacity="e.wet ? 0.55 : 0.9"
        />
        <path
          v-if="e.wet"
          :d="pipePath(e)"
          fill="none"
          stroke="#5b9bd8"
          stroke-width="3"
          stroke-linecap="round"
          stroke-dasharray="3 9"
          class="lc-wet"
        />
      </g>

      <!-- root → tank pipe -->
      <g>
        <line
          :x1="rootNode.x + rootNode.rw"
          :y1="yOf(rootNode.slot)"
          :x2="outX"
          :y2="tankY"
          :stroke="result ? '#3b6cb5' : '#d8d2c4'"
          stroke-width="8"
          stroke-linecap="round"
          :opacity="result ? 0.55 : 0.9"
        />
        <line
          v-if="result"
          :x1="rootNode.x + rootNode.rw"
          :y1="yOf(rootNode.slot)"
          :x2="outX"
          :y2="tankY"
          stroke="#5b9bd8"
          stroke-width="3"
          stroke-linecap="round"
          stroke-dasharray="3 9"
          class="lc-wet"
        />
      </g>

      <!-- nodes -->
      <g v-for="n in nodes" :key="n.id">
        <g
          v-if="n.kind === 'var' || n.kind === 'const'"
          @click="n.kind === 'var' && interactive ? $emit('toggle', n.name) : null"
          :style="{ cursor: n.kind === 'var' && interactive ? 'pointer' : 'default' }"
        >
          <rect
            :x="n.x - n.rw"
            :y="yOf(n.slot) - n.rh"
            :width="n.rw * 2"
            :height="n.rh * 2"
            :rx="10"
            :fill="n.val ? 'var(--lm-blue-soft)' : 'var(--lm-surface)'"
            stroke="var(--lm-line)"
            stroke-width="2.5"
            class="[filter:drop-shadow(2px_3px_0_rgba(26,24,20,.9))]"
          />
          <text
            :x="n.x"
            :y="yOf(n.slot) - 4"
            text-anchor="middle"
            dominant-baseline="central"
            class="font-math italic text-[22px] font-bold fill-lm-ink pointer-events-none"
          >
            {{ n.label }}
          </text>
          <text
            v-if="n.kind === 'var'"
            :x="n.x"
            :y="yOf(n.slot) + 13"
            text-anchor="middle"
            dominant-baseline="central"
            class="font-mono text-[9px] font-bold tracking-[0.06em] pointer-events-none"
            :style="{ fill: n.val ? '#3b6cb5' : 'var(--lm-ink-3)' }"
          >
            {{ n.val ? 'OPEN · T' : 'SHUT · F' }}
          </text>
        </g>

        <g v-else-if="n.kind === 'not'">
          <circle
            :cx="n.x"
            :cy="yOf(n.slot)"
            :r="n.rw"
            :fill="n.val ? 'var(--lm-blue-soft)' : 'var(--lm-surface)'"
            stroke="var(--lm-line)"
            stroke-width="2.5"
            class="[filter:drop-shadow(2px_3px_0_rgba(26,24,20,.9))]"
          />
          <text
            :x="n.x"
            :y="yOf(n.slot)"
            text-anchor="middle"
            dominant-baseline="central"
            class="font-math italic text-[20px] font-bold fill-lm-rust"
          >
            ¬
          </text>
        </g>

        <g v-else>
          <rect
            :x="n.x - n.rw"
            :y="yOf(n.slot) - n.rh"
            :width="n.rw * 2"
            :height="n.rh * 2"
            :rx="13"
            :fill="n.val ? 'var(--lm-blue-soft)' : 'var(--lm-surface)'"
            stroke="var(--lm-line)"
            stroke-width="2.5"
            class="[filter:drop-shadow(2px_3px_0_rgba(26,24,20,.9))]"
          />
          <text
            :x="n.x"
            :y="yOf(n.slot)"
            text-anchor="middle"
            dominant-baseline="central"
            class="font-math italic text-[22px] font-bold fill-lm-ink pointer-events-none"
          >
            {{ n.glyph }}
          </text>
        </g>
      </g>

      <!-- output tank -->
      <g>
        <rect
          :x="tankX"
          :y="tankY - 42"
          width="64"
          height="84"
          :rx="12"
          fill="var(--lm-surface)"
          stroke="var(--lm-line)"
          stroke-width="2.5"
          class="[filter:drop-shadow(3px_4px_0_rgba(26,24,20,.9))]"
        />
        <clipPath :id="'lc-tankclip-' + uniqueId">
          <rect :x="tankX + 4" :y="tankY - 38" width="56" height="76" rx="8" />
        </clipPath>
        <rect
          :x="tankX + 4"
          :y="result ? tankY - 26 : tankY + 36"
          width="56"
          :height="result ? 64 : 2"
          fill="url(#lc-tank)"
          :clip-path="`url(#lc-tankclip-${uniqueId})`"
          class="transition-all duration-500 ease-[cubic-bezier(.4,0,.2,1)]"
        />
        <text :x="tankX + 32" :y="tankY - 56" text-anchor="middle" class="font-mono text-[10px] font-bold fill-lm-ink-3 tracking-wider">
          OUTPUT
        </text>
        <text
          :x="tankX + 32"
          :y="tankY + 2"
          text-anchor="middle"
          dominant-baseline="central"
          class="font-display text-[22px] font-[800]"
          :style="{ fill: result ? '#fff' : 'var(--lm-ink-3)' }"
        >
          {{ result ? 'T' : 'F' }}
        </text>
      </g>
    </svg>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Logic } from '@/features/courses/utils/logic-engine.js'

const props = defineProps({
  ast: {
    type: Object,
    default: null
  },
  env: {
    type: Object,
    default: () => ({})
  },
  goal: {
    type: String,
    default: null
  },
  height: {
    type: String,
    default: ''
  },
  interactive: {
    type: Boolean,
    default: true
  }
})

defineEmits(['toggle'])

const uniqueId = Math.random().toString(36).substring(2, 9)

const COL = 124
const ROW = 74
const PADX = 76
const PADR = 168
const PADY = 46
const LC_GLYPH = { and: '∧', or: '∨', xor: '⊕', imp: '→', iff: '↔' }

function lcDepth(n) {
  if (n.t === 'var' || n.t === 'const') return 0
  if (n.t === 'not') return lcDepth(n.a) + 1
  return Math.max(lcDepth(n.a), lcDepth(n.b)) + 1
}

const D = computed(() => Math.max(lcDepth(props.ast), 1))

const circuitLayout = computed(() => {
  if (!props.ast) return { nodes: [], edges: [], leafCount: 0, rootNode: null }
  const nodes = []
  const edges = []
  let leafSlot = 0
  let idc = 0

  function place(node) {
    const id = idc++
    const val = Logic.evaluate(node, props.env)
    const depth = lcDepth(node)
    const colX = PADX + depth * COL
    if (node.t === 'var' || node.t === 'const') {
      const y = leafSlot++
      const rec = {
        id,
        kind: node.t,
        label: node.t === 'var' ? node.name : (node.val ? 'T' : 'F'),
        name: node.t === 'var' ? node.name : null,
        val,
        x: colX,
        slot: y,
        rw: 27,
        rh: 24
      }
      nodes.push(rec)
      return rec
    }
    if (node.t === 'not') {
      const child = place(node.a)
      const rec = {
        id,
        kind: 'not',
        val,
        x: colX,
        slot: child.slot,
        rw: 22,
        rh: 22
      }
      nodes.push(rec)
      edges.push({ from: child, to: rec, wet: child.val })
      return rec
    }
    const a = place(node.a)
    const b = place(node.b)
    const rec = {
      id,
      kind: 'gate',
      op: node.op,
      glyph: LC_GLYPH[node.op],
      val,
      x: colX,
      slot: (a.slot + b.slot) / 2,
      rw: 27,
      rh: 27
    }
    nodes.push(rec)
    edges.push({ from: a, to: rec, wet: a.val })
    edges.push({ from: b, to: rec, wet: b.val })
    return rec
  }

  const rootNode = place(props.ast)
  return { nodes, edges, leafCount: Math.max(leafSlot, 1), rootNode }
})

const nodes = computed(() => circuitLayout.value.nodes)
const edges = computed(() => circuitLayout.value.edges)
const leafCount = computed(() => circuitLayout.value.leafCount)
const rootNode = computed(() => circuitLayout.value.rootNode)

const leafNodes = computed(() => nodes.value.filter(n => n.kind === 'var' || n.kind === 'const'))

const W = computed(() => PADX + D.value * COL + PADR)
const H = computed(() => Math.max(PADY * 2 + leafCount.value * ROW, 200))

function yOf(slot) {
  return PADY + (slot + 0.5) * ROW
}

const outX = computed(() => rootNode.value ? rootNode.value.x + 70 : 0)
const tankX = computed(() => outX.value + 6)
const tankY = computed(() => H.value / 2)
const result = computed(() => rootNode.value ? rootNode.value.val : false)

function pipePath(e) {
  const x1 = e.from.x + e.from.rw
  const y1 = yOf(e.from.slot)
  const x2 = e.to.x - e.to.rw
  const y2 = yOf(e.to.slot)
  const mx = (x1 + x2) / 2
  return `M ${x1} ${y1} C ${mx} ${y1}, ${mx} ${y2}, ${x2} ${y2}`
}

// Ensure style exists in head
if (typeof document !== 'undefined' && !document.getElementById('logic-circuit-css')) {
  const st = document.createElement('style')
  st.id = 'logic-circuit-css'
  st.textContent = `
    @keyframes lc-flow { to { stroke-dashoffset: -28; } }
    .lc-wet { animation: lc-flow .7s linear infinite; }
    @media (prefers-reduced-motion: reduce) { .lc-wet { animation: none; } }
  `
  document.head.appendChild(st)
}
</script>

