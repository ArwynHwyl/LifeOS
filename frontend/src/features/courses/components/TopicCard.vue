<script setup lang="ts">
import { computed, ref } from 'vue'

const props = defineProps<{
  name: string
  content: string
}>()

const emit = defineEmits<{
  delete: []
  edit: []
}>()

const expanded = ref(false)

const isLongContent = computed(() => props.content.trim().length > 180)
const showCollapsedPreview = computed(() => isLongContent.value && !expanded.value)
</script>

<template>
  <article
    class="overflow-hidden rounded-2xl border border-slate-100/80 bg-white shadow-[0_4px_24px_-4px_rgba(15,23,42,0.08)]"
  >
    <header
      class="flex items-center justify-between gap-3 border-b border-slate-100 bg-slate-50/60 px-4 py-3 sm:px-5"
    >
      <h3 class="min-w-0 truncate text-sm font-semibold tracking-tight text-slate-800 sm:text-base">
        {{ name }}
      </h3>

      <div class="flex shrink-0 items-center gap-1.5 sm:gap-2">
        <button
          type="button"
          class="flex h-9 w-9 items-center justify-center rounded-xl border border-red-200/80 bg-red-50 text-red-600 transition hover:border-red-300 hover:bg-red-100"
          aria-label="Delete topic"
          @click="emit('delete')"
        >
          <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
            <path d="M3 6h18" />
            <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
            <line x1="10" y1="11" x2="10" y2="17" />
            <line x1="14" y1="11" x2="14" y2="17" />
          </svg>
        </button>

        <button
          type="button"
          class="flex h-9 w-9 items-center justify-center rounded-xl border border-slate-200 bg-white text-slate-700 shadow-sm transition hover:border-slate-300 hover:bg-slate-50"
          aria-label="Edit topic"
          @click="emit('edit')"
        >
          <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
            <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
            <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
          </svg>
        </button>

        <button
          v-if="isLongContent"
          type="button"
          class="flex h-9 w-9 items-center justify-center rounded-xl border border-slate-200 bg-white text-slate-600 shadow-sm transition hover:border-slate-300 hover:bg-slate-50"
          :aria-expanded="expanded"
          :aria-label="expanded ? 'Collapse topic content' : 'Expand topic content'"
          @click="expanded = !expanded"
        >
          <svg
            class="h-4 w-4 transition-transform duration-200"
            :class="{ 'rotate-180': expanded }"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            aria-hidden="true"
          >
            <polyline points="6 9 12 15 18 9" />
          </svg>
        </button>
      </div>
    </header>

    <div class="px-4 py-4 sm:px-5 sm:py-5">
      <p
        v-if="showCollapsedPreview"
        class="line-clamp-3 whitespace-pre-wrap text-sm leading-relaxed text-slate-600"
      >
        {{ content }}
      </p>
      <p
        v-else
        class="whitespace-pre-wrap text-sm leading-relaxed text-slate-600"
      >
        {{ content }}
      </p>
      <p
        v-if="showCollapsedPreview"
        class="mt-2 text-xs font-medium text-[#5b4cfa]"
      >
        Expand to read full content
      </p>
    </div>
  </article>
</template>
