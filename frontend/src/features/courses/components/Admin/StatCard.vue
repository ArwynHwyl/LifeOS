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
  <div class="rounded-[20px] border border-lm-line bg-lm-surface p-5 transition-shadow duration-150 hover:shadow-stamp-md">
    <div class="mb-4 flex items-start justify-between">
      <MonoLabel>{{ label }}</MonoLabel>
      <div
        class="grid h-9 w-9 place-items-center rounded-xl"
        :style="{ background: icon.bg, color: icon.color }"
      >
        <AdminIcon :name="icon.name" :size="17" />
      </div>
    </div>
    <p class="m-0 mb-1 font-display text-[32px] font-bold leading-none" :style="{ color: color || 'var(--lm-ink)' }">
      {{ value }}
    </p>
    <p class="m-0 text-[13px] font-medium text-lm-ink-3">{{ sub }}</p>
  </div>
</template>
