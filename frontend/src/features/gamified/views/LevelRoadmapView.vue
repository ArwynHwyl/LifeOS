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
  <main class="flex-1 bg-white">

    <!-- Top bar -->
    <div class="flex items-center gap-3.5 px-6 py-3.5 bg-white border-b border-lx-line">
      <button
        @click="router.back()"
        class="flex items-center gap-1.5 px-3.5 py-2 text-sm font-extrabold rounded-2xl bg-lx-surface-soft hover:bg-lx-line transition-colors duration-150 text-lx-ink shrink-0"
      >
        <LmIcon name="back" :size="14" />
        Back to dashboard
      </button>
      <div class="flex-1 text-center">
        <span class="font-display font-extrabold text-[18px] text-lx-ink leading-tight block">Level Roadmap</span>
      </div>
      <div class="w-[132px] shrink-0" />
    </div>

    <div class="max-w-[760px] mx-auto px-9 py-8 flex flex-col gap-0">
      <p v-if="loading" class="text-center text-lx-ink-faint py-10">Loading roadmap…</p>

      <div
        v-for="(entry, i) in levels"
        :key="entry.level"
        class="anim-rise relative flex gap-5"
        :style="{ '--i': Math.min(i, 10) }"
      >
        <!-- Rail: badge + connecting line -->
        <div class="flex flex-col items-center shrink-0">
          <div
            :class="[
              'w-14 h-14 rounded-full flex items-center justify-center font-display font-extrabold text-lg shrink-0', entry.achieved && !levels[i + 1]?.achieved ? 'anim-ring' : '',
              entry.achieved ? 'bg-lx-feather text-white' : 'bg-lx-surface-soft text-lx-ink-faint'
            ]"
          >
            <template v-if="entry.achieved">{{ entry.level }}</template>
            <LmIcon v-else name="lock" :size="20" />
          </div>
          <div v-if="i < levels.length - 1" class="w-px flex-1 min-h-[28px] bg-lx-line my-1" />
        </div>

        <!-- Content -->
        <div class="flex-1 pb-6">
          <p v-if="isRankStart(i)" class="font-mono text-[11px] font-bold tracking-[0.06em] uppercase text-lx-fox-dark mb-1">
            Rank - {{ entry.rankName }}
          </p>
          <div
            :class="[
              'flex items-center justify-between gap-4 px-5 py-4 rounded-2xl transition-opacity',
              entry.achieved ? 'bg-lx-surface-soft' : 'opacity-55'
            ]"
          >
            <div>
              <p class="font-display font-extrabold text-[15.5px] text-lx-ink m-0">Level {{ entry.level }} · {{ entry.rankName }}</p>
              <p class="text-[13px] font-semibold text-lx-ink-soft mt-1 m-0">{{ entry.unlockDescription }}</p>
            </div>
            <div class="flex items-center gap-3 shrink-0">
              <div class="text-center">
                <span class="font-mono text-[10px] font-bold tracking-[0.06em] uppercase text-lx-ink-faint block">Shield max</span>
                <span class="flex items-center gap-1 justify-center font-display font-extrabold text-[15px] text-lx-macaw">
                  <LmIcon name="shield" :size="14" :filled="true" />{{ entry.shieldMaxTotal }}
                </span>
              </div>
              <div class="text-center">
                <span class="font-mono text-[10px] font-bold tracking-[0.06em] uppercase text-lx-ink-faint block">EXP</span>
                <span class="font-display font-extrabold text-[15px] text-lx-ink">{{ entry.expRequiredToReach }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </main>
</template>
