<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import LmIcon from '@/features/learning/components/LmIcon.vue'
import { listAssessments, type AssessmentSummaryDto } from '../services/assessment'

const router = useRouter()

const TAG_VISUALS: Record<string, { icon: string; bgClass: string }> = {
  ALGEBRA: { icon: 'x²', bgClass: 'bg-lx-macaw' },
  TRIG: { icon: 'sin', bgClass: 'bg-lx-fox' },
  CALCULUS: { icon: '∫', bgClass: 'bg-lx-beetle' },
  GEOMETRY: { icon: '△', bgClass: 'bg-lx-feather' },
  STATS: { icon: 'σ', bgClass: 'bg-lx-eel' },
}
const DEFAULT_VISUAL = { icon: '?', bgClass: 'bg-lx-eel' }

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
  <main class="flex-1 bg-white">
    <div class="max-w-[1180px] mx-auto px-8 py-8">

      <!-- Header -->
      <div class="mb-6">
        <h1 class="font-display text-[34px] font-extrabold tracking-tight text-lx-ink leading-tight mt-1 m-0">
          Prove what you know
        </h1>
        <p class="text-[13.5px] font-semibold text-lx-ink-soft mt-1.5">
          {{ attemptedCount }} of {{ assessments.length }} completed (each assessment can only be taken once)
        </p>
      </div>

      <div v-if="loading" class="py-16 text-center text-[14px] text-lx-ink-faint">Loading assessments…</div>

      <p v-else-if="assessments.length === 0" class="py-16 text-center text-[14px] text-lx-ink-faint">
        No assessments available yet.
      </p>

      <div v-else class="grid grid-cols-2 gap-4">
        <button
          v-for="a in assessments"
          :key="a.id"
          type="button"
          @click="openAssessment(a)"
          class="anim-rise press group flex gap-4 items-center p-5 text-left bg-white border border-lx-line rounded-[20px] cursor-pointer transition-all duration-150 hover:-translate-y-1 hover:shadow-[0_16px_32px_-18px_rgba(0,0,0,0.25)]"
        >
          <div :class="['flex items-center justify-center w-16 h-16 shrink-0 rounded-2xl transition-transform duration-300 group-hover:-rotate-6 group-hover:scale-110', visualFor(a.tag).bgClass]">
            <span class="font-display font-extrabold text-[24px] text-white">{{ visualFor(a.tag).icon }}</span>
          </div>

          <div class="flex-1 min-w-0">
            <span class="font-mono text-[10.5px] font-bold tracking-[0.06em] uppercase text-lx-ink-faint">{{ a.tag }}</span>
            <h3 class="font-display text-[17px] font-extrabold text-lx-ink leading-tight mt-0.5 mb-1 m-0">{{ a.title }}</h3>
            <p v-if="a.description" class="text-[13px] text-lx-ink-soft mb-1.5 m-0 line-clamp-1">{{ a.description }}</p>
            <p class="font-mono text-[10.5px] font-bold text-lx-ink-faint m-0">{{ a.questionCount }} QUESTIONS</p>

            <div v-if="a.attempted" class="flex items-center gap-2 mt-2.5">
              <span class="px-2.5 py-1 text-[11px] font-extrabold rounded-full bg-lx-feather/10 text-lx-feather-dark">
                {{ a.score }}/{{ a.totalQuestions }} · {{ scorePercent(a) }}%
              </span>
              <span class="flex items-center gap-1 text-[13px] font-extrabold text-lx-macaw">
                View results <LmIcon name="arrow" :size="14" />
              </span>
            </div>
            <p v-else class="flex items-center gap-1.5 text-[13px] font-extrabold text-lx-macaw mt-2.5 m-0">
              Start assessment
              <LmIcon name="arrow" :size="14" class="transition-transform duration-150 group-hover:translate-x-0.5" />
            </p>
          </div>
        </button>
      </div>
    </div>
  </main>
</template>
