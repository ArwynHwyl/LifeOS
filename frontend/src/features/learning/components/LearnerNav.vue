<script setup lang="ts">
import { useRouter } from 'vue-router'
import LmIcon from './LmIcon.vue'

const props = withDefaults(defineProps<{
  active?: 'courses' | 'flashcards' | 'dashboard'
  level?: number
  exp?: number
  expMax?: number
  streak?: number
  shields?: number
}>(), {
  active: 'courses',
  level: 12,
  exp: 340,
  expMax: 500,
  streak: 7,
  shields: 2,
})

const router = useRouter()

const tabs: { id: 'courses' | 'flashcards' | 'dashboard'; label: string; icon: 'book' | 'card' | 'user' }[] = [
  { id: 'courses',    label: 'Courses',    icon: 'book' },
  { id: 'flashcards', label: 'Flashcards', icon: 'card' },
  { id: 'dashboard',  label: 'Dashboard',  icon: 'user' },
]

function navigate(id: 'courses' | 'flashcards' | 'dashboard') {
  router.push(`/learn/${id}`)
}
</script>

<template>
  <header class="h-[72px] shrink-0 flex items-center gap-3.5 px-6 bg-lm-surface border-b-2 border-lm-line relative z-10">

    <!-- Logo -->
    <div class="flex items-center gap-2.5">
      <div class="w-10 h-10 flex items-center justify-center bg-lm-yellow border-2 border-lm-ink rounded-[12px] shadow-stamp-sm font-math italic font-bold text-[22px] text-lm-ink shrink-0">
        π
      </div>
      <span class="font-display text-[20px] font-bold tracking-tight text-lm-ink">LifeOS</span>
    </div>

    <!-- Tabs -->
    <nav class="flex gap-1 ml-[18px] p-1 bg-lm-bg-soft border-2 border-lm-line rounded-full">
      <button
        v-for="tab in tabs"
        :key="tab.id"
        @click="navigate(tab.id)"
        :class="[
          'flex items-center gap-1.5 px-3.5 py-1.5 text-sm font-semibold rounded-full transition-all duration-200',
          active === tab.id
            ? 'bg-lm-ink text-lm-bg'
            : 'bg-transparent text-lm-ink hover:bg-lm-line-soft'
        ]"
      >
        <LmIcon :name="tab.icon" :size="16" />
        {{ tab.label }}
      </button>
    </nav>

    <div class="flex-1" />

    <!-- Level chip -->
    <div class="flex items-center gap-2 pl-1.5 pr-3 py-[5px] border-2 border-lm-line rounded-full bg-lm-surface shadow-stamp-sm">
      <div class="w-7 h-7 rounded-full bg-lm-yellow border-2 border-lm-line flex items-center justify-center font-display font-bold text-xs text-lm-ink shrink-0">
        {{ level }}
      </div>
      <div class="flex flex-col gap-0.5">
        <span class="font-mono text-[9px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3 leading-none">LEVEL</span>
        <div class="w-16 h-1.5 bg-lm-bg-soft rounded-full overflow-hidden border border-lm-line">
          <div class="h-full bg-lm-yellow transition-all duration-200" :style="{ width: `${(exp / expMax) * 100}%` }" />
        </div>
      </div>
    </div>

    <!-- Streak chip -->
    <div class="flex items-center gap-2 px-3.5 py-[5px] border-2 border-lm-line rounded-full bg-lm-rust-soft shadow-stamp-sm">
      <span class="text-lm-rust"><LmIcon name="flame" :size="20" :filled="true" /></span>
      <div class="flex flex-col leading-none">
        <span class="font-display font-bold text-[16px] text-lm-ink">{{ streak }}</span>
        <span class="font-mono text-[9px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3">DAYS</span>
      </div>
    </div>

    <!-- Shield chip -->
    <div class="flex items-center gap-1.5 px-3.5 py-[5px] border-2 border-lm-line rounded-full bg-lm-blue-soft shadow-stamp-sm">
      <span class="text-lm-blue"><LmIcon name="shield" :size="20" :filled="true" /></span>
      <span class="font-display font-bold text-[16px] text-lm-ink">×{{ shields }}</span>
    </div>

    <!-- Avatar -->
    <div class="w-[42px] h-[42px] rounded-full bg-lm-yellow border-2 border-lm-line shadow-stamp-sm flex items-center justify-center font-display font-bold text-sm text-lm-ink shrink-0">
      JD
    </div>
  </header>
</template>
