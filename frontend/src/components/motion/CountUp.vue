<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = withDefaults(defineProps<{
  value: number
  duration?: number
  suffix?: string
}>(), { duration: 900, suffix: '' })

const shown = ref(0)
let frame = 0
let current = 0

function run(target: number) {
  cancelAnimationFrame(frame)
  const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  if (reduce || props.duration <= 0) {
    shown.value = target
    current = target
    return
  }
  const from = current
  const start = performance.now()
  const step = (now: number) => {
    const t = Math.min(1, (now - start) / props.duration)
    const eased = 1 - Math.pow(1 - t, 3)
    current = from + (target - from) * eased
    shown.value = Math.round(current)
    if (t < 1) frame = requestAnimationFrame(step)
  }
  frame = requestAnimationFrame(step)
}

onMounted(() => run(props.value))
watch(() => props.value, (v) => run(v))
onBeforeUnmount(() => cancelAnimationFrame(frame))
</script>

<template>
  <span class="tabular-nums">{{ shown }}{{ suffix }}</span>
</template>
