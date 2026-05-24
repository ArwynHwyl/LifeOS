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
      <!-- Title + status -->
      <div class="flex items-start justify-between gap-2">
        <h3 class="flex-1 text-[13.5px] font-bold leading-snug text-lm-ink">
          {{ title }}
        </h3>
        <span
          v-if="status === 'published'"
          class="shrink-0 inline-flex items-center gap-1 rounded-full border-2 border-lm-line bg-lm-green-soft px-2 py-0.5 font-mono text-[10px] font-semibold text-lm-green shadow-stamp-sm"
        >
          <span class="h-1.5 w-1.5 rounded-full bg-lm-green" />
          Published
        </span>
        <span
          v-else
          class="shrink-0 inline-flex items-center gap-1 rounded-full border-2 border-lm-line bg-lm-yellow px-2 py-0.5 font-mono text-[10px] font-semibold text-lm-ink shadow-stamp-sm"
        >
          <span class="h-1.5 w-1.5 rounded-full bg-lm-ink" />
          Pending
        </span>
      </div>

      <!-- Description -->
      <p class="mt-2 line-clamp-2 flex-1 text-[11.5px] leading-relaxed text-lm-ink-2">
        {{ description }}
      </p>

      <!-- Footer -->
      <div class="mt-3.5 flex items-center justify-between border-t-2 border-lm-line-soft pt-3">
        <span class="font-mono text-[11px] text-lm-ink-3">{{ moduleCount }} modules · {{ lastEdited }}</span>

        <!-- Approve / Reject on hover -->
        <div
          class="flex items-center gap-1 opacity-0 transition-opacity group-hover:opacity-100"
          @click.stop
        >
          <button
            type="button"
            class="flex h-7 items-center gap-1 rounded-lg px-2 text-[11px] font-semibold text-lm-green transition hover:bg-lm-green-soft"
            title="Approve course"
            @click.stop="emit('approve')"
          >
            <svg class="h-3 w-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
              <polyline points="20 6 9 17 4 12" />
            </svg>
            Approve
          </button>
          <button
            type="button"
            class="flex h-7 items-center gap-1 rounded-lg px-2 text-[11px] font-semibold text-lm-red transition hover:bg-lm-red-soft"
            title="Reject course"
            @click.stop="emit('reject')"
          >
            <svg class="h-3 w-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
              <path d="M18 6 6 18M6 6l12 12" />
            </svg>
            Reject
          </button>
        </div>
      </div>
    </div>
  </article>
</template>
