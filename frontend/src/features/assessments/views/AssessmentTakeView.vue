<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
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
import ToraMascot from '@/components/tora/ToraMascot.vue'
import CountUp from '@/components/motion/CountUp.vue'
import ConfettiBurst from '@/components/motion/ConfettiBurst.vue'

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
const confettiKey = ref(0)
const mainEl = ref<HTMLElement | null>(null)

const questions = computed(() => detail.value?.questions ?? [])
const passed = computed(() => (result.value?.percentage ?? 0) >= 70)
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
    if (passed.value) confettiKey.value += 1
    await nextTick()
    mainEl.value?.scrollTo({ top: 0, behavior: 'smooth' })
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  void load()
})
</script>

<template>
  <main ref="mainEl" class="flex-1 bg-white">

    <div v-if="loading" class="max-w-[820px] mx-auto px-9 py-16 text-center text-[14px] text-lx-ink-faint">
      Loading assessment…
    </div>

    <div v-else-if="loadError" class="max-w-[820px] mx-auto px-9 py-16 text-center text-[14px] text-lx-ink-faint">
      Couldn't load this assessment. <button class="underline font-bold" @click="load">Try again</button>
    </div>

    <!-- Results -->
    <div v-else-if="result" class="max-w-[820px] mx-auto px-9 py-8">
      <button
        @click="router.push('/learn/assessments')"
        class="flex items-center gap-1.5 px-3.5 py-2 mb-5 text-sm font-extrabold rounded-2xl bg-lx-surface-soft hover:bg-lx-line transition-colors duration-150 text-lx-ink"
      >
        <LmIcon name="back" :size="14" />
        Back to assessments
      </button>

      <div :class="['anim-pop relative rounded-[24px] p-8 sm:px-28 mb-6 text-center text-white', passed ? 'bg-lx-feather' : 'bg-lx-eel']">
        <ConfettiBurst :fire="confettiKey" :count="56" :spread="260" />
        <div class="absolute bottom-0 right-8 hidden sm:block">
          <ToraMascot :mood="passed ? 'cheer' : 'oops'" :size="150" :track="false" />
        </div>
        <span class="font-mono text-[11px] font-bold tracking-[0.08em] uppercase text-white/75">Result</span>
        <h1 class="font-display text-[30px] font-extrabold leading-tight mt-1 mb-2 m-0">{{ result.title }}</h1>
        <p class="font-display text-[52px] font-extrabold leading-none m-0"><CountUp :value="Math.round(result.percentage)" suffix="%" :duration="1100" /></p>
        <p class="text-[14px] font-semibold text-white/85 mt-1.5 mb-0">{{ result.score }} out of {{ result.totalQuestions }} correct</p>
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
    <div v-else-if="detail" class="max-w-[820px] mx-auto px-9 py-8">
      <button
        @click="router.push('/learn/assessments')"
        class="flex items-center gap-1.5 px-3.5 py-2 mb-5 text-sm font-extrabold rounded-2xl bg-lx-surface-soft hover:bg-lx-line transition-colors duration-150 text-lx-ink"
      >
        <LmIcon name="back" :size="14" />
        Exit
      </button>

      <div class="bg-white border border-lx-line rounded-[24px] p-7 mb-6">
        <h1 class="font-display text-[28px] font-extrabold text-lx-ink leading-tight mt-1 mb-1.5 m-0">{{ detail.title }}</h1>
        <p v-if="detail.description" class="text-[14px] text-lx-ink-soft m-0">{{ detail.description }}</p>
        <p class="font-mono text-[11px] font-bold text-lx-ink-faint mt-3 mb-2 m-0">
          {{ answeredCount }} / {{ questions.length }} ANSWERED
        </p>
        <div class="h-2.5 w-full overflow-hidden rounded-full bg-lx-surface-soft">
          <div
            class="h-full rounded-full bg-lx-macaw transition-[width] duration-500 ease-[cubic-bezier(0.34,1.4,0.64,1)]"
            :class="{ '!bg-lx-feather': allAnswered }"
            :style="{ width: questions.length ? `${(answeredCount / questions.length) * 100}%` : '0%' }"
          />
        </div>
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
          class="press flex items-center gap-2 px-8 py-3.5 text-[16px] font-extrabold rounded-2xl bg-lx-feather text-white shadow-[0_4px_0_var(--color-lx-feather-dark)] transition-transform duration-75 active:translate-y-1 active:shadow-none disabled:opacity-40 disabled:pointer-events-none"
        >
          Submit assessment
        </button>
        <p v-if="!allAnswered" class="font-mono text-[11px] font-bold text-lx-ink-faint">
          Answer all {{ questions.length }} questions to submit
        </p>
      </div>
    </div>

    <!-- Confirm submit overlay -->
    <div v-if="showConfirm" class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-6">
      <div class="w-full max-w-[420px] bg-white rounded-[24px] p-6 text-center shadow-[0_24px_60px_-20px_rgba(0,0,0,0.4)]">
        <h2 class="font-display text-[21px] font-extrabold text-lx-ink m-0 mb-2">Submit assessment?</h2>
        <p class="text-[14px] text-lx-ink-soft m-0 mb-5">
          You won't be able to retake this assessment once submitted. Make sure you're happy with your answers.
        </p>
        <div class="flex gap-3 justify-center">
          <button
            @click="showConfirm = false"
            :disabled="submitting"
            class="px-5 py-2.5 text-[14px] font-extrabold rounded-2xl bg-lx-surface-soft text-lx-ink hover:bg-lx-line transition-colors duration-150 disabled:opacity-50"
          >
            Cancel
          </button>
          <button
            @click="confirmSubmit"
            :disabled="submitting"
            class="px-5 py-2.5 text-[14px] font-extrabold rounded-2xl bg-lx-feather text-white shadow-[0_4px_0_var(--color-lx-feather-dark)] transition-transform duration-75 active:translate-y-1 active:shadow-none disabled:opacity-50"
          >
            {{ submitting ? 'Submitting…' : 'Submit for good' }}
          </button>
        </div>
      </div>
    </div>
  </main>
</template>
