<script setup lang="ts">
withDefaults(defineProps<{
  name: 'book' | 'card' | 'user' | 'flame' | 'shield' | 'bolt' | 'arrow' | 'back' | 'check' | 'close' | 'lock' | 'refresh' | 'plus' | 'star' | 'search'
  size?: number
  filled?: boolean
  sw?: number
}>(), { size: 20, filled: false, sw: 2 })
</script>

<template>
  <svg v-if="name === 'book'" :width="size" :height="size" viewBox="0 0 24 24" fill="none" stroke="currentColor" :stroke-width="sw" stroke-linecap="round" stroke-linejoin="round">
    <path d="M4 5a2 2 0 0 1 2-2h6v18H6a2 2 0 0 1-2-2z"/><path d="M20 5a2 2 0 0 0-2-2h-6v18h6a2 2 0 0 0 2-2z"/>
  </svg>
  <svg v-else-if="name === 'card'" :width="size" :height="size" viewBox="0 0 24 24" fill="none" stroke="currentColor" :stroke-width="sw" stroke-linecap="round" stroke-linejoin="round">
    <rect x="3" y="6" width="18" height="13" rx="2"/><path d="M3 10h18"/>
  </svg>
  <svg v-else-if="name === 'user'" :width="size" :height="size" viewBox="0 0 24 24" fill="none" stroke="currentColor" :stroke-width="sw" stroke-linecap="round" stroke-linejoin="round">
    <circle cx="12" cy="9" r="3.5"/><path d="M5 20c1-4 4-6 7-6s6 2 7 6"/>
  </svg>
  <svg v-else-if="name === 'flame'" :width="size" :height="size" viewBox="0 0 24 24" :fill="filled ? 'currentColor' : 'none'" stroke="currentColor" :stroke-width="sw" stroke-linejoin="round">
    <path d="M12 3c1 3 4 4 4 8a4 4 0 0 1-8 0c0-2 1-3 2-4 0 1 1 2 2 2 0-2-1-4 0-6z"/>
    <path d="M7 14a5 5 0 0 0 10 0"/>
  </svg>
  <svg v-else-if="name === 'shield'" :width="size" :height="size" viewBox="0 0 24 24" :fill="filled ? 'currentColor' : 'none'" stroke="currentColor" :stroke-width="sw" stroke-linejoin="round">
    <path d="M12 3L20 6V12C20 16 16 20 12 21C8 20 4 16 4 12V6Z"/>
  </svg>
  <svg v-else-if="name === 'bolt'" :width="size" :height="size" viewBox="0 0 24 24" :fill="filled ? 'currentColor' : 'none'" stroke="currentColor" :stroke-width="sw" stroke-linejoin="round">
    <path d="M13 3L5 14h5L9 21l9-12h-5z"/>
  </svg>
  <svg v-else-if="name === 'arrow'" :width="size" :height="size" viewBox="0 0 24 24" fill="none" stroke="currentColor" :stroke-width="sw" stroke-linecap="round" stroke-linejoin="round">
    <path d="M5 12h14M13 6l6 6-6 6"/>
  </svg>
  <svg v-else-if="name === 'back'" :width="size" :height="size" viewBox="0 0 24 24" fill="none" stroke="currentColor" :stroke-width="sw" stroke-linecap="round" stroke-linejoin="round">
    <path d="M19 12H5M11 18l-6-6 6-6"/>
  </svg>
  <svg v-else-if="name === 'check'" :width="size" :height="size" viewBox="0 0 24 24" fill="none" stroke="currentColor" :stroke-width="sw + 0.4" stroke-linecap="round" stroke-linejoin="round">
    <path d="M4 13l5 5L20 6"/>
  </svg>
  <svg v-else-if="name === 'close'" :width="size" :height="size" viewBox="0 0 24 24" fill="none" stroke="currentColor" :stroke-width="sw + 0.4" stroke-linecap="round" stroke-linejoin="round">
    <path d="M5 5l14 14M19 5L5 19"/>
  </svg>
  <svg v-else-if="name === 'lock'" :width="size" :height="size" viewBox="0 0 24 24" fill="none" stroke="currentColor" :stroke-width="sw" stroke-linecap="round" stroke-linejoin="round">
    <rect x="5" y="10" width="14" height="10" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/>
  </svg>
  <svg v-else-if="name === 'refresh'" :width="size" :height="size" viewBox="0 0 24 24" fill="none" stroke="currentColor" :stroke-width="sw" stroke-linecap="round" stroke-linejoin="round">
    <path d="M3 12a9 9 0 1 1 3 6.7M3 21v-6h6"/>
  </svg>
  <svg v-else-if="name === 'plus'" :width="size" :height="size" viewBox="0 0 24 24" fill="none" stroke="currentColor" :stroke-width="sw + 0.4" stroke-linecap="round" stroke-linejoin="round">
    <path d="M12 5v14M5 12h14"/>
  </svg>
  <svg v-else-if="name === 'star'" :width="size" :height="size" viewBox="0 0 24 24" :fill="filled ? 'currentColor' : 'none'" stroke="currentColor" :stroke-width="sw - 0.2" stroke-linejoin="round">
    <path d="M12 3l2.6 5.6 6 .8-4.3 4.2 1 6-5.3-3-5.3 3 1-6-4.3-4.2 6-.8z"/>
  </svg>
  <svg v-else-if="name === 'search'" :width="size" :height="size" viewBox="0 0 24 24" fill="none" stroke="currentColor" :stroke-width="sw" stroke-linecap="round" stroke-linejoin="round">
    <circle cx="11" cy="11" r="7"/><path d="M21 21l-4.3-4.3"/>
  </svg>
</template>
