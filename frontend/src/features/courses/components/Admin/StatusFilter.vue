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
      class="inline-flex h-11 min-w-[150px] cursor-pointer items-center justify-between gap-2 rounded-2xl border-2 border-lm-line-soft bg-lm-bg-soft px-4 text-[14px] font-semibold text-lm-ink-2 transition-colors duration-150 hover:border-lm-line"
      @click="open = !open"
    >
      {{ currentLabel }}
      <AdminIcon name="chevron" :size="12" />
    </button>
    <div
      v-if="open"
      class="absolute right-0 top-[calc(100%+6px)] z-20 w-[190px] overflow-hidden rounded-2xl border border-lm-line bg-lm-surface p-1.5 shadow-stamp-lg"
    >
      <button
        v-for="[value, label] in statuses"
        :key="value"
        type="button"
        class="flex w-full cursor-pointer items-center gap-2.5 rounded-xl border-none bg-transparent px-3 py-2.5 text-left text-[14px] font-medium text-lm-ink-2 transition-colors duration-100 hover:bg-lm-bg-soft"
        :class="{ '!bg-lx-macaw/10 !font-semibold !text-lx-macaw-dark': modelValue === value }"
        @click="select(value)"
      >
        {{ label }}
      </button>
    </div>
    <div v-if="open" class="fixed inset-0 z-10" aria-hidden="true" @click="open = false" />
  </div>
</template>
