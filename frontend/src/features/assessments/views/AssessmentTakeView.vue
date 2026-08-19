<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import LmIcon from '@/features/learning/components/LmIcon.vue'
import AssessmentQuestionCard from '../components/AssessmentQuestionCard.vue'
import AssessmentResultQuestionCard from '../components/AssessmentResultQuestionCard.vue'
import {
  getAssessmentDetail,
  submitAssessment,
  type AssessmentDetailDto,
  type AssessmentResultDto,
} from '../services/assessment'
import { useGamificationStore } from '@/features/gamified/stores/gamification'

const gamificationStore = useGamificationStore()

const route = useRoute()
const router = useRouter()

const assessmentId = Number(route.params.assessmentId)

const detail = ref<AssessmentDetailDto | null>(null)
const result = ref<AssessmentResultDto | null>(null)
const answers = ref<Record<number, number>>({})
const loading = ref(true)
const loadError = ref(false)
const submitting = ref(false)
const showConfirm = ref(false)

const questions = computed(() => detail.value?.questions ?? [])
const answeredCount = computed(() => Object.keys(answers.value).length)
const allAnswered = computed(() => questions.value.length > 0 && answeredCount.value === questions.value.length)

async function load() {
  loading.value = true
  loadError.value = false
  try {
    detail.value = await getAssessmentDetail(assessmentId)
    if (detail.value.attempted) {
      result.value = detail.value.result
    }
  } catch {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

function selectAnswer(questionId: number, optionId: number) {
  answers.value[questionId] = optionId
}

async function confirmSubmit() {
  if (submitting.value) return
  submitting.value = true
  try {
    result.value = await submitAssessment(
      assessmentId,
      questions.value.map((q) => ({ questionId: q.id, selectedOptionId: answers.value[q.id] ?? null })),
    )
    showConfirm.value = false
    gamificationStore.handleReward(result.value.reward)
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  void load()
})
</script>

<template>
  <main class="flex-1 overflow-auto bg-lm-bg relative">
    <div class="absolute inset-0 bg-dot-grid opacity-40 pointer-events-none" />

    <div v-if="loading" class="relative max-w-[820px] mx-auto px-9 py-16 text-center text-[14px] text-lm-ink-2">
      Loading assessment…
    </div>

    <div v-else-if="loadError" class="relative max-w-[820px] mx-auto px-9 py-16 text-center text-[14px] text-lm-ink-2">
      Couldn't load this assessment. <button class="underline" @click="load">Try again</button>
    </div>

    <!-- Results -->
    <div v-else-if="result" class="relative max-w-[820px] mx-auto px-9 py-8">
      <button
        @click="router.push('/learn/assessments')"
        class="flex items-center gap-1.5 px-3 py-1.5 mb-5 text-sm font-semibold border-2 border-lm-line rounded-full bg-lm-surface shadow-stamp-sm hover:-translate-y-px transition-all duration-200 text-lm-ink"
      >
        <LmIcon name="back" :size="14" />
        Back to assessments
      </button>

      <div class="bg-lm-yellow border-2 border-lm-line rounded-[24px] shadow-stamp-lg p-7 mb-6 text-center">
        <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3">RESULT</span>
        <h1 class="font-display text-[36px] font-bold text-lm-ink leading-tight mt-1 mb-2 m-0">{{ result.title }}</h1>
        <p class="font-display text-[48px] font-bold text-lm-ink leading-none m-0">{{ Math.round(result.percentage) }}%</p>
        <p class="text-[15px] text-lm-ink mt-1.5 mb-0">{{ result.score }} out of {{ result.totalQuestions }} correct</p>
      </div>

      <div class="flex flex-col gap-4">
        <AssessmentResultQuestionCard
          v-for="(q, i) in result.questionResults"
          :key="q.id"
          :index="i + 1"
          :question="q"
        />
      </div>
    </div>

    <!-- Take form -->
    <div v-else-if="detail" class="relative max-w-[820px] mx-auto px-9 py-8">
      <button
        @click="router.push('/learn/assessments')"
        class="flex items-center gap-1.5 px-3 py-1.5 mb-5 text-sm font-semibold border-2 border-lm-line rounded-full bg-lm-surface shadow-stamp-sm hover:-translate-y-px transition-all duration-200 text-lm-ink"
      >
        <LmIcon name="back" :size="14" />
        Exit
      </button>

      <div class="bg-lm-surface border-2 border-lm-line rounded-[24px] shadow-stamp-md p-7 mb-6">
        <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3">ASSESSMENT</span>
        <h1 class="font-display text-[32px] font-bold text-lm-ink leading-tight mt-1 mb-1.5 m-0">{{ detail.title }}</h1>
        <p v-if="detail.description" class="text-[14px] text-lm-ink-2 m-0">{{ detail.description }}</p>
        <p class="font-mono text-[11px] text-lm-ink-3 mt-3 mb-0">
          {{ answeredCount }} / {{ questions.length }} ANSWERED · ONE ATTEMPT ONLY
        </p>
      </div>

      <div class="flex flex-col gap-4">
        <AssessmentQuestionCard
          v-for="(q, i) in questions"
          :key="q.id"
          :index="i + 1"
          :question="q"
          :model-value="answers[q.id] ?? null"
          @update:model-value="(optionId) => selectAnswer(q.id, optionId)"
        />
      </div>

      <div class="flex flex-col items-center gap-2 mt-7 pb-4">
        <button
          @click="showConfirm = true"
          :disabled="!allAnswered"
          class="flex items-center gap-2 px-8 py-3.5 text-[17px] font-semibold border-2 border-lm-line rounded-full bg-lm-ink text-lm-bg shadow-stamp-sm hover:-translate-y-px hover:shadow-stamp-md transition-all duration-200 disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:translate-y-0 disabled:hover:shadow-stamp-sm"
        >
          Submit assessment
        </button>
        <p v-if="!allAnswered" class="font-mono text-[11px] text-lm-ink-3">
          Answer all {{ questions.length }} questions to submit
        </p>
      </div>
    </div>

    <!-- Confirm submit overlay -->
    <div v-if="showConfirm" class="fixed inset-0 z-50 flex items-center justify-center bg-lm-ink/40 px-6">
      <div class="w-full max-w-[420px] bg-lm-surface border-2 border-lm-line rounded-[20px] shadow-stamp-lg p-6 text-center">
        <h2 class="font-display text-[22px] font-bold text-lm-ink m-0 mb-2">Submit assessment?</h2>
        <p class="text-[14px] text-lm-ink-2 m-0 mb-5">
          You won't be able to retake this assessment once submitted. Make sure you're happy with your answers.
        </p>
        <div class="flex gap-3 justify-center">
          <button
            @click="showConfirm = false"
            :disabled="submitting"
            class="px-5 py-2.5 text-[14px] font-semibold border-2 border-lm-line rounded-full bg-lm-surface text-lm-ink hover:-translate-y-px transition-all duration-200 disabled:opacity-50"
          >
            Cancel
          </button>
          <button
            @click="confirmSubmit"
            :disabled="submitting"
            class="px-5 py-2.5 text-[14px] font-semibold border-2 border-lm-line rounded-full bg-lm-ink text-lm-bg hover:-translate-y-px transition-all duration-200 disabled:opacity-50"
          >
            {{ submitting ? 'Submitting…' : 'Submit for good' }}
          </button>
        </div>
      </div>
    </div>
  </main>
</template>
