<script lang="ts">
let logoUid = 0
</script>

<script setup lang="ts">
withDefaults(defineProps<{ size?: number }>(), { size: 40 })
const clipId = `lifeos-logo-${++logoUid}`
</script>

<template>
  <svg :width="size" :height="size" viewBox="0 0 64 64" role="img" aria-label="LifeOS" class="shrink-0">
    <defs>
      <clipPath :id="clipId"><path d="M20 12h11v29h15v11H20z" /></clipPath>
    </defs>
    <rect width="64" height="64" rx="14" fill="#FF9A1F" />
    <path d="M20 12h11v29h15v11H20z" fill="#2a1a10" />
    <g :clip-path="`url(#${clipId})`" stroke="#FF9A1F" stroke-width="3.4">
      <path d="M14 22L38 12M14 31L38 21M26 41L48 37" />
    </g>
  </svg>
</template>
