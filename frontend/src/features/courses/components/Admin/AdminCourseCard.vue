<script setup lang="ts">
import { computed } from 'vue'
import AdminIcon from '@/features/courses/components/Admin/AdminIcon.vue'
import StatusBadge from '@/features/courses/components/Admin/StatusBadge.vue'
import { getCoverPreset } from '@/features/courses/constants/courseCoverPresets'
import type { CourseStatus } from '@/types/types'

const props = defineProps<{
  title: string
  description: string
  status: CourseStatus
  moduleCount: number
  lastEdited: string
  coverId: string
  createdBy: string
}>()

const emit = defineEmits<{
  edit: []
  open: []
  delete: []
  submit: []
}>()

const cover = computed(() => getCoverPreset(props.coverId))
</script>

<template>
  <article
    class="grid cursor-pointer grid-cols-[48px_minmax(0,1fr)_auto_auto_auto] items-center gap-5 rounded-[20px] border border-lm-line bg-lm-surface px-5 py-4 transition-all duration-150 hover:-translate-y-0.5 hover:shadow-stamp-md"
    @click="emit('open')"
  >
    <div
      class="grid h-12 w-12 shrink-0 place-items-center rounded-2xl font-math text-[21px] font-bold italic"
      :class="[cover.bgClass, cover.textClass]"
    >
      {{ cover.symbol }}
    </div>

    <div class="min-w-0">
      <div class="mb-1 flex min-w-0 items-center gap-2">
        <h3 class="m-0 truncate font-display text-[16px] font-bold text-lm-ink">{{ title }}</h3>
        <StatusBadge :status="status" />
      </div>
      <p class="m-0 truncate text-[13px] font-medium text-lm-ink-3">
        {{ moduleCount }} module{{ moduleCount === 1 ? '' : 's' }} · {{ lastEdited }} · Created by {{ createdBy }}
      </p>
      <p v-if="description" class="m-0 mt-0.5 truncate text-[13px] text-lm-ink-3">{{ description }}</p>
    </div>

    <div class="hidden min-w-[120px] md:block">
      <button
        v-if="status === 'draft' || status === 'revision'"
        type="button"
        class="inline-flex cursor-pointer items-center gap-1.5 rounded-xl bg-lm-purple-soft px-3.5 py-2 text-[13px] font-semibold text-lm-purple transition-colors hover:brightness-95"
        @click.stop="emit('submit')"
      >
        <AdminIcon name="arrow" :size="12" />
        Submit for Review
      </button>
    </div>

    <button
      type="button"
      class="inline-flex cursor-pointer items-center gap-1.5 rounded-xl border border-lm-line bg-lm-surface px-3.5 py-2 text-[13px] font-semibold text-lm-ink transition-colors hover:bg-lm-bg-soft"
      :class="status === 'pending' ? 'invisible pointer-events-none' : ''"
      @click.stop="emit('edit')"
    >
      <AdminIcon name="edit" :size="13" />
      Edit
    </button>

    <button
      type="button"
      class="grid h-9 w-9 cursor-pointer place-items-center rounded-xl border border-lm-line bg-lm-surface text-lm-ink-3 transition-all duration-150 hover:border-lm-red hover:bg-lm-red-soft hover:text-lm-red"
      title="Delete"
      @click.stop="emit('delete')"
    >
      <AdminIcon name="trash" :size="16" />
    </button>
  </article>
</template>
