<script setup lang="ts">
import { computed } from 'vue'
import { getCoverPreset } from '@/features/courses/constants/courseCoverPresets'
import type { CourseStatus } from '@/types/types'

const props = defineProps<{
  title: string
  description: string
  status: CourseStatus
  moduleCount: number
  lastEdited: string
  coverId: string
}>()

const emit = defineEmits<{
  open: []
  approve: []
  reject: []
}>()

const cover = computed(() => getCoverPreset(props.coverId))
</script>

<template>
  <article
    class="group flex cursor-pointer flex-col overflow-hidden rounded-[18px] border-2 border-lm-line bg-lm-surface shadow-stamp-sm transition-all duration-200 hover:-translate-y-px hover:shadow-stamp-md"
    @click="emit('open')"
  >
    <!-- Colored top band -->
    <div
      class="relative flex h-[140px] shrink-0 items-center justify-center overflow-hidden border-b-2 border-lm-line"
      :class="cover.bgClass"
    >
      <span
        class="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 select-none font-display text-[110px] font-bold leading-none opacity-[0.15]"
        :class="cover.textClass"
      >{{ cover.symbol }}</span>
      <span class="relative z-10 font-display text-[42px] font-bold leading-none" :class="cover.textClass">
        {{ cover.symbol }}
      </span>
    </div>

    <!-- Card body -->
    <div class="flex flex-1 flex-col p-4">
      <!-- Title + badge inline -->
      <div class="flex flex-wrap items-center gap-2">
        <h3 class="text-[13.5px] font-bold leading-snug text-lm-ink">{{ title }}</h3>
        <span
          v-if="status === 'published'"
          class="inline-flex items-center gap-1 rounded-full border border-lm-line-soft bg-lm-green-soft px-2 py-0.5 font-mono text-[10px] font-semibold text-lm-green"
        >
          <span class="h-1.5 w-1.5 rounded-full bg-lm-green" />
          Published
        </span>
        <span
          v-else-if="status === 'pending'"
          class="inline-flex items-center gap-1 rounded-full border border-lm-line-soft bg-lm-yellow/60 px-2 py-0.5 font-mono text-[10px] font-semibold text-lm-ink"
        >
          <span class="h-1.5 w-1.5 rounded-full bg-lm-ink" />
          Pending Review
        </span>
        <span
          v-else-if="status === 'revision'"
          class="inline-flex items-center gap-1 rounded-full border border-lm-line-soft bg-lm-red-soft px-2 py-0.5 font-mono text-[10px] font-semibold text-lm-red"
        >
          <span class="h-1.5 w-1.5 rounded-full bg-lm-red" />
          Needs Revision
        </span>
        <span
          v-else
          class="inline-flex items-center gap-1 rounded-full border border-lm-line-soft bg-lm-bg-soft px-2 py-0.5 font-mono text-[10px] font-semibold text-lm-ink-3"
        >
          <span class="h-1.5 w-1.5 rounded-full bg-lm-ink-3" />
          Draft
        </span>
      </div>

      <!-- Description -->
      <p class="mt-2 line-clamp-2 flex-1 text-[11.5px] leading-relaxed text-lm-ink-2">
        {{ description }}
      </p>

      <!-- Footer -->
      <div class="mt-3.5 flex items-center justify-between border-t-2 border-lm-line-soft pt-3">
        <span class="font-mono text-[11px] text-lm-ink-3">{{ moduleCount }} modules · {{ lastEdited }}</span>

      </div>
    </div>
  </article>
</template>
