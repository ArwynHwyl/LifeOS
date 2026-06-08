<script setup lang="ts">
import { computed, ref } from 'vue'
import AdminIcon from '@/features/courses/components/Admin/AdminIcon.vue'
import type { CourseStatus } from '@/types/types'

const props = defineProps<{
  modelValue: 'all' | CourseStatus
}>()

const emit = defineEmits<{
  'update:modelValue': [value: 'all' | CourseStatus]
}>()

const open = ref(false)
const statuses: Array<['all' | CourseStatus, string]> = [
  ['all', 'All status'],
  ['published', 'Published'],
  ['pending', 'Pending Review'],
  ['revision', 'Needs Revision'],
  ['draft', 'Draft'],
]

const currentLabel = computed(() => statuses.find(([value]) => value === props.modelValue)?.[1] ?? 'All')

function select(value: 'all' | CourseStatus) {
  emit('update:modelValue', value)
  open.value = false
}
</script>

<template>
  <div class="relative">
    <button
      type="button"
      class="inline-flex h-11 min-w-[140px] cursor-pointer items-center justify-between gap-2 rounded-[12px] border-2 border-lm-line-soft bg-lm-bg-soft px-3.5 font-display text-[12px] font-bold text-lm-ink-2"
      @click="open = !open"
    >
      {{ currentLabel }}
      <AdminIcon name="chevron" :size="12" />
    </button>
    <div
      v-if="open"
      class="absolute right-0 top-[calc(100%+6px)] z-20 w-[170px] overflow-hidden rounded-[12px] border-2 border-lm-line bg-lm-surface shadow-stamp-md"
    >
      <button
        v-for="[value, label] in statuses"
        :key="value"
        type="button"
        class="flex w-full cursor-pointer items-center gap-2.5 border-none bg-transparent px-3.5 py-2.5 text-left font-display text-[12px] font-semibold text-lm-ink-2"
        :class="{ 'bg-[rgba(255,211,51,0.35)] font-bold text-lm-ink': modelValue === value }"
        @click="select(value)"
      >
        {{ label }}
      </button>
    </div>
    <div v-if="open" class="fixed inset-0 z-10" aria-hidden="true" @click="open = false" />
  </div>
</template>
