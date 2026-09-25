<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import LmIcon from '@/features/learning/components/LmIcon.vue'
import { listAssessments, type AssessmentSummaryDto } from '../services/assessment'

const router = useRouter()

const TAG_VISUALS: Record<string, { icon: string; bgClass: string }> = {
  LOGIC: { icon: '∧', bgClass: 'bg-lm-tri' },
  SETS: { icon: '∪', bgClass: 'bg-lm-cal' },
  VECTOR: { icon: '↗', bgClass: 'bg-lm-geo' },
  MATRIX: { icon: '▦', bgClass: 'bg-lm-alg' },
  PROBABILITY: { icon: 'P', bgClass: 'bg-lm-sta' },
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
const loadError = ref(false)
const filter = ref<'all' | 'available' | 'completed'>('all')
const filters = ['all', 'available', 'completed'] as const
const filteredAssessments = computed(() => assessments.value.filter(a => filter.value === 'all' || (filter.value === 'completed' ? a.attempted : !a.attempted)))

const attemptedCount = computed(() => assessments.value.filter((a) => a.attempted).length)

async function loadAssessments() {
  loading.value = true
  loadError.value = false
  try {
    assessments.value = await listAssessments()
  } catch {
    loadError.value = true
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
  <main class="assessment-page">
    <div class="assessment-container">
      <header class="assessment-hero">
        <div>
          <span class="eyebrow">YOUR LEARNING CHECKPOINT</span>
          <h1>See how far<br />you’ve <span>come.</span></h1>
          <p>Put your understanding into practice.<br />Choose a topic and discover what you know.</p>
        </div>
        <div class="progress-summary">
          <span class="eyebrow">YOUR PROGRESS</span>
          <div><strong>{{ attemptedCount.toString().padStart(2, '0') }}</strong><span>/ {{ assessments.length.toString().padStart(2, '0') }}</span></div>
          <p>assessments completed</p>
          <div class="summary-track"><span :style="{ width: `${assessments.length ? attemptedCount / assessments.length * 100 : 0}%` }" /></div>
        </div>
      </header>
      <div class="assessment-note"><span aria-hidden="true">↗</span><p><strong>A little preparation goes a long way.</strong> Each assessment has one attempt. Review your lessons before you begin.</p></div>
      <div class="assessment-toolbar">
        <h2>Assessments <span>{{ assessments.length }}</span></h2>
        <div class="assessment-filters" aria-label="Filter assessments">
          <button v-for="item in filters" :key="item" :aria-pressed="filter === item" :class="{ active: filter === item }" @click="filter = item">{{ item === 'all' ? 'All topics' : item === 'available' ? 'To do' : 'Completed' }}</button>
        </div>
      </div>
      <div v-if="loading" class="assessment-empty" role="status">Loading your assessments…</div>
      <div v-else-if="loadError" class="assessment-empty" role="alert">Couldn’t load assessments. <button @click="loadAssessments">Try again</button></div>
      <div v-else-if="!filteredAssessments.length" class="assessment-empty">{{ assessments.length ? 'No assessments in this view yet.' : 'Your next challenge is on its way. Check back soon.' }}</div>
      <div v-else class="assessment-grid">
        <button v-for="a in filteredAssessments" :key="a.id" class="assessment-card" @click="openAssessment(a)">
          <div class="card-top"><span :class="['topic-symbol', visualFor(a.tag).bgClass]">{{ visualFor(a.tag).icon }}</span><span class="status-pill" :class="{ completed: a.attempted }">{{ a.attempted ? '✓ Completed' : 'Ready when you are' }}</span></div>
          <span class="eyebrow">{{ a.tag }}</span>
          <h3>{{ a.title }}</h3>
          <p class="card-description">{{ a.description || 'Check your understanding of the key concepts in this topic.' }}</p>
          <div class="card-meta">{{ a.questionCount }} questions <span>·</span> One attempt</div>
          <div class="card-bottom"><span v-if="a.attempted" class="card-score">{{ scorePercent(a) }}% <small>{{ a.score }}/{{ a.totalQuestions }} correct</small></span><span v-else>Start assessment</span><span class="card-action">{{ a.attempted ? 'View results' : 'Let’s go' }} <LmIcon name="arrow" :size="16" /></span></div>
        </button>
      </div>
    </div>
  </main>
</template>

<style scoped>
.assessment-page { overflow:auto; background:#fbf7ef; color:#24251f; }
.assessment-container { max-width:1180px; margin:auto; padding:48px 40px 64px; }
.assessment-hero { display:flex; justify-content:space-between; align-items:center; gap:40px; padding-bottom:36px; }
.eyebrow { font-family:var(--font-mono); font-size:10px; font-weight:600; letter-spacing:.12em; color:#68675d; }
h1,h2,h3 { font-family:var(--font-display); }
h1 { font-size:clamp(38px,5vw,62px); line-height:1.05; letter-spacing:-.045em; font-weight:700; margin:16px 0; }
h1 span { background:linear-gradient(transparent 62%,#ffd333 62%); }
.assessment-hero p { color:#68675d; line-height:1.7; font-size:15px; }
.progress-summary { width:270px; padding:28px; border:1px solid #deded1; border-radius:20px; background:#eeefe5; }
.progress-summary strong { font-size:64px; font-family:var(--font-display); font-weight:600; letter-spacing:-.06em; }
.progress-summary div>span { color:#6b6d5f; margin-left:8px; }
.progress-summary p { margin:0 0 20px; font-size:13px; }
.summary-track { height:5px; border-radius:8px; background:#d9dcce; overflow:hidden; }
.summary-track span { display:block; height:100%; background:#526746; margin:0!important; }
.assessment-note { display:flex; align-items:center; gap:16px; padding:16px 20px; border:1px solid #e6dcc0; border-radius:12px; background:#fff8e3; color:#655b3e; font-size:13px; line-height:1.6; }
.assessment-note>span { font-size:24px; }.assessment-note p { margin:0; }.assessment-note strong { color:#393a2e; }
.assessment-toolbar { display:flex; justify-content:space-between; align-items:center; gap:16px; margin:36px 0 22px; }
h2 { font-size:22px; font-weight:650; }h2 span { font-size:12px; padding:4px 8px; background:#ebe8dd; border-radius:8px; margin-left:6px; }
.assessment-filters { display:flex; padding:4px; border:1px solid #e3dfd3; border-radius:10px; gap:3px; }
.assessment-filters button { padding:8px 14px; border-radius:7px; font-size:12px; color:#656358; cursor:pointer; }.assessment-filters .active { background:#292d24; color:white; }
.assessment-grid { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:20px; }
.assessment-card { display:flex; flex-direction:column; text-align:left; border:1px solid #dedbd1; border-radius:18px; background:#fffefa; padding:26px; cursor:pointer; transition:transform .2s,box-shadow .2s; min-width:0; }
.assessment-card:hover { transform:translateY(-3px); box-shadow:0 10px 24px #302a1710; border-color:#8b927a; }
.card-top { display:flex; align-items:center; justify-content:space-between; gap:12px; margin-bottom:24px; }.topic-symbol { width:52px; height:52px; border-radius:13px; display:grid; place-items:center; font:italic 26px var(--font-math); }.status-pill { font-size:11px; color:#716d5f; background:#f1eee5; border-radius:20px; padding:6px 10px; }.status-pill.completed { color:#416140; background:#e9f0e3; }
h3 { font-size:24px; line-height:1.25; font-weight:650; margin:8px 0 10px; }.card-description { font-size:14px; line-height:1.65; color:#716d62; margin:0 0 20px; }.card-meta { margin-top:auto; font-size:12px; color:#716d62; }.card-meta span { padding:0 9px; }.card-bottom { display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:12px; border-top:1px solid #e9e5db; margin-top:22px; padding-top:18px; font-size:13px; font-weight:600; }.card-action { display:flex; align-items:center; gap:8px; }.card-score { font-size:22px; }.card-score small { font-size:11px; color:#777367; font-weight:400; margin-left:6px; }.assessment-empty { padding:64px 20px; text-align:center; color:#68675d; }.assessment-empty button { text-decoration:underline; }button:focus-visible { outline:3px solid #668255; outline-offset:4px; }
@media(max-width:640px) { .assessment-container { padding:28px 20px; }.assessment-hero { align-items:stretch; flex-direction:column; gap:20px; }.progress-summary { width:100%; padding:20px; }.progress-summary strong { font-size:40px; }.assessment-grid { grid-template-columns:1fr; }.assessment-toolbar { align-items:flex-start; flex-direction:column; }.assessment-filters { width:100%; }.assessment-filters button { flex:1; }.assessment-card { padding:22px; } }
@media(prefers-reduced-motion:reduce) { .assessment-card { transition:none; } }
</style>
