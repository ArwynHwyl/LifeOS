<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import LmIcon from '@/features/learning/components/LmIcon.vue'
import { listAssessments, type AssessmentSummaryDto } from '../services/assessment'

const router = useRouter()

const TAG_VISUALS: Record<string, { icon: string; bgClass: string }> = {
  ALGEBRA: { icon: 'x²', bgClass: 'bg-lm-alg' },
  TRIG: { icon: 'sin', bgClass: 'bg-lm-tri' },
  CALCULUS: { icon: '∫', bgClass: 'bg-lm-cal' },
  GEOMETRY: { icon: '△', bgClass: 'bg-lm-geo' },
  STATS: { icon: 'σ', bgClass: 'bg-lm-sta' },
}
const DEFAULT_VISUAL = { icon: '?', bgClass: 'bg-lm-bg-soft' }

function visualFor(tag: string) {
  return TAG_VISUALS[tag] ?? DEFAULT_VISUAL
}

const assessments = ref<AssessmentSummaryDto[]>([])
const loading = ref(true)

const attemptedCount = computed(() => assessments.value.filter((a) => a.attempted).length)

async function loadAssessments() {
  loading.value = true
  try {
    assessments.value = await listAssessments()
  } finally {
    loading.value = false
  }
}

function openAssessment(assessment: AssessmentSummaryDto) {
  router.push(`/learn/assessments/${assessment.id}`)
}

function scorePercent(assessment: AssessmentSummaryDto) {
  if (assessment.score == null || !assessment.totalQuestions) return 0
  return Math.round((assessment.score / assessment.totalQuestions) * 100)
}

onMounted(() => {
  void loadAssessments()
})
</script>

<template>
  <main class="flex-1 overflow-auto bg-lm-bg relative">
    <div class="absolute inset-0 bg-dot-grid opacity-40 pointer-events-none" />

    <div class="relative max-w-[1280px] mx-auto px-9 py-7">

      <!-- Header -->
      <div class="mb-6">
        <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3">ASSESSMENTS</span>
        <h1 class="font-display text-[42px] font-bold tracking-tight text-lm-ink leading-tight mt-1 mb-1.5 m-0">
          Prove what you know
          <span class="inline-block bg-lm-yellow px-2 rounded-[6px] border-2 border-lm-line shadow-stamp-sm -rotate-1 whitespace-nowrap">
            one sitting
          </span>
        </h1>
        <p class="text-[15px] text-lm-ink-2 mt-2">
          {{ attemptedCount }} of {{ assessments.length }} completed · each assessment can only be taken once
        </p>
      </div>

      <div v-if="loading" class="py-16 text-center text-[14px] text-lm-ink-2">Loading assessments…</div>

      <p v-else-if="assessments.length === 0" class="py-16 text-center text-[14px] text-lm-ink-2">
        No assessments available yet.
      </p>

      <div v-else class="grid grid-cols-2 gap-[18px]">
        <div
          v-for="a in assessments"
          :key="a.id"
          @click="openAssessment(a)"
          class="flex gap-3.5 items-center p-5 bg-lm-surface border-2 border-lm-line rounded-[18px] shadow-stamp-md cursor-pointer hover:-translate-y-0.5 hover:shadow-stamp-lg transition-all duration-200"
        >
          <div class="relative w-[70px] h-[92px] shrink-0">
            <div :class="['absolute inset-[4px_-4px_-4px_4px] rounded-[8px] border-2 border-lm-line opacity-55', visualFor(a.tag).bgClass]" />
            <div :class="['absolute inset-0 rounded-[8px] border-2 border-lm-line flex items-center justify-center', visualFor(a.tag).bgClass]">
              <span class="font-math italic font-bold text-[28px] text-lm-ink">{{ visualFor(a.tag).icon }}</span>
            </div>
          </div>

          <div class="flex-1 min-w-0">
            <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3">{{ a.tag }}</span>
            <h3 class="font-display text-[18px] font-bold text-lm-ink leading-tight mt-0.5 mb-1 m-0">{{ a.title }}</h3>
            <p v-if="a.description" class="text-[13px] text-lm-ink-2 mb-1.5 m-0 line-clamp-1">{{ a.description }}</p>
            <p class="font-mono text-[11px] text-lm-ink-2 m-0">{{ a.questionCount }} QUESTIONS</p>

            <div v-if="a.attempted" class="flex items-center gap-2 mt-2">
              <span class="px-2.5 py-1 text-[11px] font-semibold border border-lm-line rounded-full bg-lm-green-soft text-lm-ink">
                {{ a.score }}/{{ a.totalQuestions }} · {{ scorePercent(a) }}%
              </span>
              <span class="flex items-center gap-1 text-[13px] font-semibold text-lm-ink">
                View results <LmIcon name="arrow" :size="14" />
              </span>
            </div>
            <p v-else class="flex items-center gap-1.5 text-[13px] font-semibold text-lm-ink mt-2 m-0">
              Start assessment <LmIcon name="arrow" :size="14" />
            </p>
          </div>
        </div>
      </div>
    </div>
  </main>
</template>
