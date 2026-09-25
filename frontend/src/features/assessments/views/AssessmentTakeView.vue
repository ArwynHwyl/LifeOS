<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
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
const submitError = ref('')
const confirmDialog = ref<HTMLDialogElement | null>(null)
watch(showConfirm, async (open) => {
  await nextTick()
  if (open) confirmDialog.value?.showModal()
  else confirmDialog.value?.close()
})
function jumpToQuestion(id: number) {
  const card = document.getElementById(`assessment-question-${id}`)
  card?.scrollIntoView({ block: 'start' })
  card?.focus({ preventScroll: true })
}

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
  submitError.value = ''
  try {
    result.value = await submitAssessment(
      assessmentId,
      questions.value.map((q) => ({ questionId: q.id, selectedOptionId: answers.value[q.id] ?? null })),
    )
    showConfirm.value = false
    gamificationStore.handleReward(result.value.reward)
  } catch {
    submitError.value = 'Your assessment could not be submitted. Your answers are still here. Please try again.'
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  void load()
})
</script>

<template>
  <main class="assessment-take flex-1 overflow-auto bg-lm-bg relative">
    <div class="absolute inset-0 bg-dot-grid opacity-0 pointer-events-none" />

    <div v-if="loading" class="relative max-w-[820px] mx-auto px-9 py-16 text-center text-[14px] text-lm-ink-2">
      Loading assessment…
    </div>

    <div v-else-if="loadError" class="relative max-w-[820px] mx-auto px-9 py-16 text-center text-[14px] text-lm-ink-2">
      Couldn't load this assessment. <button class="underline" @click="load">Try again</button>
    </div>

    <!-- Results -->
    <div v-else-if="result" class="relative max-w-[820px] mx-auto px-5 sm:px-9 py-8">
      <button
        @click="router.push('/learn/assessments')"
        class="flex items-center gap-1.5 px-3 py-1.5 mb-5 text-sm font-semibold border-2 border-lm-line rounded-full bg-lm-surface shadow-stamp-sm hover:-translate-y-px transition-all duration-200 text-lm-ink"
      >
        <LmIcon name="back" :size="14" />
        Back to assessments
      </button>

      <div class="result-summary bg-lm-yellow border-2 border-lm-line rounded-[24px] shadow-stamp-lg p-7 mb-6 text-center">
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
    <div v-else-if="detail" class="relative max-w-[820px] mx-auto px-5 sm:px-9 py-8">
      <button
        @click="router.push('/learn/assessments')"
        class="flex items-center gap-1.5 px-3 py-1.5 mb-5 text-sm font-semibold border-2 border-lm-line rounded-full bg-lm-surface shadow-stamp-sm hover:-translate-y-px transition-all duration-200 text-lm-ink"
      >
        <LmIcon name="back" :size="14" />
        Back to assessments
      </button>

      <div class="take-intro bg-lm-surface border-2 border-lm-line rounded-[24px] shadow-stamp-md p-7 mb-6">
        <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3">ASSESSMENT</span>
        <h1 class="font-display text-[32px] font-bold text-lm-ink leading-tight mt-1 mb-1.5 m-0">{{ detail.title }}</h1>
        <p v-if="detail.description" class="text-[14px] text-lm-ink-2 m-0">{{ detail.description }}</p>
        <p class="font-mono text-[11px] text-lm-ink-3 mt-3 mb-0">
          {{ answeredCount }} / {{ questions.length }} ANSWERED · ONE ATTEMPT ONLY
        </p>
      </div>

      <nav class="question-navigator" aria-label="Question navigation">
        <div class="navigator-heading"><strong>Your progress</strong><span aria-live="polite">{{ answeredCount }} of {{ questions.length }} answered</span></div>
        <div class="answer-track"><span :style="{ width: `${questions.length ? answeredCount / questions.length * 100 : 0}%` }" /></div>
        <div class="question-shortcuts"><button v-for="(q, i) in questions" :key="q.id" :class="{ answered: answers[q.id] != null }" :aria-label="`Question ${i + 1}, ${answers[q.id] != null ? 'answered' : 'unanswered'}`" @click="jumpToQuestion(q.id)">{{ i + 1 }}<span v-if="answers[q.id] != null" aria-hidden="true"> ✓</span></button></div>
      </nav>
      <div class="flex flex-col gap-5">
        <AssessmentQuestionCard
          v-for="(q, i) in questions"
          :key="q.id"
          :index="i + 1"
          :id="`assessment-question-${q.id}`"
          tabindex="-1"
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
    <dialog ref="confirmDialog" class="submit-dialog" aria-labelledby="confirm-title" @cancel="submitting ? $event.preventDefault() : showConfirm = false" @close="showConfirm = false">
      <div class="w-full max-w-[420px] bg-lm-surface border-2 border-lm-line rounded-[20px] shadow-stamp-lg p-6 text-center">
        <h2 id="confirm-title" class="font-display text-[22px] font-bold text-lm-ink m-0 mb-2">Submit assessment?</h2>
        <p class="text-[14px] text-lm-ink-2 m-0 mb-5">
          You won't be able to retake this assessment once submitted. Make sure you're happy with your answers.
        </p>
        <p v-if="submitError" role="alert" class="submit-error">{{ submitError }}</p>
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
            {{ submitting ? 'Submitting…' : 'Submit answers' }}
          </button>
        </div>
      </div>
    </dialog>
  </main>
</template>

<style scoped>
.assessment-take { background: #f7f5ee; }
.take-intro { border: 0; border-bottom: 1px solid #dedbd0; border-radius: 0; box-shadow: none; padding: 16px 0 28px; background: transparent; }
.take-intro h1 { font-size: clamp(28px,4vw,40px); letter-spacing: -.035em; margin: 12px 0; }
.result-summary { background: #eaf0df; border: 1px solid #d3dec4; box-shadow: none; }
.question-navigator { padding: 20px; border: 1px solid #dedbd0; border-radius: 14px; background: #fffefa; margin-bottom: 24px; }
.navigator-heading { display: flex; justify-content: space-between; flex-wrap: wrap; gap: 8px; font-size: 12px; }.navigator-heading span { color: #676b5d; }
.answer-track { height: 5px; border-radius: 6px; overflow: hidden; background: #eaece2; margin: 14px 0; }.answer-track span { display: block; height: 100%; background: #536c3e; }
.question-shortcuts { display: flex; flex-wrap: wrap; gap: 8px; }.question-shortcuts button { min-width: 38px; min-height: 38px; border: 1px solid #dedbd0; border-radius: 8px; font-size: 12px; cursor: pointer; }.question-shortcuts button.answered { background: #e8efdc; color: #3b532d; border-color: #adbfa0; }
.submit-dialog { margin: auto; padding: 0; border: 0; border-radius: 20px; max-width: min(420px,calc(100% - 32px)); background: transparent; }.submit-dialog::backdrop { background: #20251cb3; backdrop-filter: blur(3px); }.submit-error { color: #9c3524; font-size: 13px; margin-bottom: 18px; }
button:focus-visible { outline: 3px solid #668255; outline-offset: 4px; }
</style>
