<script setup lang="ts">
import AdminIcon from '@/features/courses/components/Admin/AdminIcon.vue'
import MonoLabel from '@/features/courses/components/Admin/MonoLabel.vue'

defineProps<{
  label: string
  value: string | number
  color?: string
  sub: string
  icon: {
    name: string
    bg: string
    color: string
  }
}>()
</script>

<template>
  <div class="rounded-[18px] border-2 border-lm-line bg-lm-surface p-5 shadow-stamp-sm">
    <div class="mb-5 flex items-start justify-between">
      <MonoLabel>{{ label }}</MonoLabel>
      <div
        class="grid h-8 w-8 place-items-center rounded-[8px] border border-lm-line-soft"
        :style="{ background: icon.bg, color: icon.color }"
      >
        <AdminIcon :name="icon.name" :size="16" />
      </div>
    </div>
    <p class="m-0 mb-[4px] font-display text-[28px] font-bold leading-none" :style="{ color: color || 'var(--lm-ink)' }">
      {{ value }}
    </p>
    <p class="m-0 font-mono text-[11px] font-semibold text-lm-ink-3">{{ sub }}</p>
  </div>
</template>
