<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import LmIcon from '../../learning/components/LmIcon.vue'
import { getLevelRoadmap, type LevelRoadmapEntryDto } from '../services/gamification'

const router = useRouter()

const levels = ref<LevelRoadmapEntryDto[]>([])
const loading = ref(true)

onMounted(async () => {
  try {
    levels.value = await getLevelRoadmap()
  } finally {
    loading.value = false
  }
})

function isRankStart(index: number): boolean {
  if (index === 0) return true
  return levels.value[index].rankName !== levels.value[index - 1].rankName
}
</script>

<template>
  <main class="flex-1 overflow-auto bg-lm-bg relative">
    <div class="absolute inset-0 bg-dot-grid opacity-40 pointer-events-none" />

    <!-- Top bar -->
    <div class="relative flex items-center gap-3.5 px-6 py-3.5 bg-lm-surface border-b-2 border-lm-line">
      <button
        @click="router.back()"
        class="flex items-center gap-1.5 px-3 py-1.5 text-sm font-semibold border-2 border-lm-line rounded-full bg-lm-surface shadow-stamp-sm hover:-translate-y-px transition-all duration-200 text-lm-ink shrink-0"
      >
        <LmIcon name="back" :size="14" />
        Back to dashboard
      </button>
      <div class="flex-1 text-center">
        <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3 block">PROGRESSION</span>
        <span class="font-display font-bold text-[18px] text-lm-ink leading-tight block">Level Roadmap</span>
      </div>
      <div class="w-[132px] shrink-0" />
    </div>

    <div class="relative max-w-[760px] mx-auto px-9 py-8 flex flex-col gap-0">
      <p v-if="loading" class="text-center text-lm-ink-2 py-10">Loading roadmap…</p>

      <div v-for="(entry, i) in levels" :key="entry.level" class="relative flex gap-5">
        <!-- Rail: badge + connecting line -->
        <div class="flex flex-col items-center shrink-0">
          <div
            :class="[
              'w-14 h-14 rounded-full border-2 border-lm-line flex items-center justify-center font-display font-bold text-lg shrink-0',
              entry.achieved ? 'bg-lm-yellow text-lm-ink shadow-stamp-sm' : 'bg-lm-bg-soft text-lm-ink-3'
            ]"
          >
            <template v-if="entry.achieved">{{ entry.level }}</template>
            <LmIcon v-else name="lock" :size="20" />
          </div>
          <div v-if="i < levels.length - 1" class="w-0.5 flex-1 min-h-[28px] bg-lm-line-soft my-1" />
        </div>

        <!-- Content -->
        <div class="flex-1 pb-6">
          <p v-if="isRankStart(i)" class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-rust mb-1">
            NEW RANK · {{ entry.rankName }}
          </p>
          <div
            :class="[
              'flex items-center justify-between gap-4 px-5 py-4 border-2 rounded-[18px] transition-all',
              entry.achieved ? 'bg-lm-surface border-lm-line shadow-stamp-sm' : 'bg-transparent border-dashed border-lm-line opacity-70'
            ]"
          >
            <div>
              <p class="font-display font-bold text-[16px] text-lm-ink m-0">Level {{ entry.level }} · {{ entry.rankName }}</p>
              <p class="text-[13px] text-lm-ink-2 mt-1 m-0">{{ entry.unlockDescription }}</p>
            </div>
            <div class="flex items-center gap-3 shrink-0">
              <div class="text-center">
                <span class="font-mono text-[10px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3 block">SHIELD MAX</span>
                <span class="flex items-center gap-1 justify-center font-display font-bold text-[15px] text-lm-blue">
                  <LmIcon name="shield" :size="14" :filled="true" />{{ entry.shieldMaxTotal }}
                </span>
              </div>
              <div class="text-center">
                <span class="font-mono text-[10px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3 block">EXP</span>
                <span class="font-display font-bold text-[15px] text-lm-ink">{{ entry.expRequiredToReach }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </main>
</template>
