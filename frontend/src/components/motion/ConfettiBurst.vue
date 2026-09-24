<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'

interface Piece { id: number; dx: string; dy: string; rot: string; color: string; w: number; h: number; round: boolean; delay: number }

const props = withDefaults(defineProps<{
  fire: number
  count?: number
  spread?: number
  mode?: 'burst' | 'rain'
}>(), { count: 36, spread: 180, mode: 'burst' })

const COLORS = ['#58cc02', '#1cb0f6', '#ff9600', '#ce82ff', '#ffc800', '#ff4b4b']
const pieces = ref<Piece[]>([])
let timer: ReturnType<typeof setTimeout> | undefined
let seq = 0

function launch() {
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
  const list: Piece[] = []
  for (let i = 0; i < props.count; i++) {
    const rain = props.mode === 'rain'
    const angle = rain ? 0 : Math.random() * Math.PI * 2
    const dist = props.spread * (0.45 + Math.random() * 0.75)
    list.push({
      id: ++seq,
      dx: rain ? `${(Math.random() - 0.5) * 240}px` : `${Math.cos(angle) * dist}px`,
      dy: rain ? '0px' : `${Math.sin(angle) * dist - 30}px`,
      rot: `${(Math.random() - 0.5) * 900}deg`,
      color: COLORS[i % COLORS.length],
      w: 6 + Math.random() * 6,
      h: 8 + Math.random() * 8,
      round: Math.random() > 0.7,
      delay: rain ? Math.random() * 900 : Math.random() * 90,
    })
  }
  pieces.value = list
  clearTimeout(timer)
  timer = setTimeout(() => { pieces.value = [] }, props.mode === 'rain' ? 3600 : 1500)
}

watch(() => props.fire, (v) => { if (v > 0) launch() })
onMounted(() => { if (props.fire > 0) launch() })
onBeforeUnmount(() => clearTimeout(timer))
</script>

<template>
  <div class="pointer-events-none absolute inset-0 z-[70] overflow-visible" aria-hidden="true">
    <span
      v-for="p in pieces"
      :key="p.id"
      class="absolute block"
      :style="{
        left: mode === 'rain' ? `${Math.random() * 100}%` : '50%',
        top: mode === 'rain' ? '0' : '50%',
        width: `${p.w}px`,
        height: `${p.h}px`,
        background: p.color,
        borderRadius: p.round ? '50%' : '2px',
        '--dx': p.dx,
        '--dy': p.dy,
        '--rot': p.rot,
        animation: mode === 'rain'
          ? `confetti-fall ${2.2 + Math.random() * 1.2}s ${p.delay}ms cubic-bezier(0.3, 0.5, 0.6, 1) forwards`
          : `confetti-fly 1.1s ${p.delay}ms cubic-bezier(0.15, 0.8, 0.3, 1) forwards`,
      }"
    />
  </div>
</template>
