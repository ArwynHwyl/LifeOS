<script setup lang="ts">
import { useRouter } from 'vue-router'
import LmIcon from '../components/LmIcon.vue'

const router = useRouter()

const queue = [
  { id: 1, front: 'Quadratic formula',     deck: 'Algebra Formulas',  due: 'now',  tag: 'ALGEBRA' },
  { id: 2, front: 'Pythagorean theorem',   deck: 'Geometry Formulas', due: 'now',  tag: 'GEOMETRY' },
  { id: 3, front: 'Derivative of sin(x)',  deck: 'Derivative Rules',  due: 'now',  tag: 'CALCULUS' },
  { id: 4, front: 'Slope formula',         deck: 'Algebra Formulas',  due: '5m',   tag: 'ALGEBRA' },
  { id: 5, front: 'Area of a circle',      deck: 'Geometry Formulas', due: '12m',  tag: 'GEOMETRY' },
  { id: 6, front: 'sin²θ + cos²θ = ?',     deck: 'Trig Identities',   due: '30m',  tag: 'TRIG' },
  { id: 7, front: 'Power rule',            deck: 'Derivative Rules',  due: '1h',   tag: 'CALCULUS' },
  { id: 8, front: 'Mean vs median',        deck: 'Statistics Basics', due: '2h',   tag: 'STATS' },
]

const decks = [
  { id: 1, name: 'Algebra Formulas',  count: 24, tag: 'ALGEBRA',  bgClass: 'bg-lm-alg', icon: 'x²' },
  { id: 2, name: 'Trig Identities',   count: 18, tag: 'TRIG',     bgClass: 'bg-lm-tri', icon: 'sin' },
  { id: 3, name: 'Derivative Rules',  count: 15, tag: 'CALCULUS', bgClass: 'bg-lm-cal', icon: '∫' },
  { id: 4, name: 'Geometry Formulas', count: 22, tag: 'GEOMETRY', bgClass: 'bg-lm-geo', icon: '△' },
  { id: 5, name: 'Integral Rules',    count: 16, tag: 'CALCULUS', bgClass: 'bg-lm-cal', icon: '∫' },
  { id: 6, name: 'Statistics Basics', count: 14, tag: 'STATS',    bgClass: 'bg-lm-sta', icon: 'σ' },
]

const tagFilters = ['All', 'Algebra', 'Trig', 'Calculus', 'Geometry']
const dueNow = queue.filter(c => c.due === 'now').length
</script>

<template>
  <main class="flex-1 overflow-auto bg-lm-bg relative">
    <div class="absolute inset-0 bg-dot-grid opacity-40 pointer-events-none" />

    <div class="relative max-w-[1280px] mx-auto px-9 py-7">

      <!-- Header -->
      <div class="mb-6">
        <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3">FLASHCARDS</span>
        <h1 class="font-display text-[42px] font-bold tracking-tight text-lm-ink leading-tight mt-1 mb-1.5 m-0">
          Lock formulas
          <span class="inline-block bg-lm-yellow px-2 rounded-[6px] border-2 border-lm-line shadow-stamp-sm -rotate-1 whitespace-nowrap">
            into memory
          </span>
        </h1>
        <p class="text-[15px] text-lm-ink-2 mt-2">Review your queue · practice pre-made math decks</p>
      </div>

      <!-- Queue section -->
      <section class="mb-9">
        <div class="flex items-baseline gap-3 mb-3.5">
          <h2 class="font-display text-[22px] font-bold text-lm-ink m-0">Today's review queue</h2>
          <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3">SPACED REPETITION</span>
          <span class="px-2.5 py-1 text-[11px] font-semibold border border-lm-line rounded-full bg-lm-rust-soft text-lm-ink">{{ dueNow }} due now</span>
        </div>

        <div class="grid gap-[18px]" style="grid-template-columns: 1.4fr 1fr">

          <!-- Hero card -->
          <div class="relative flex items-center gap-6 p-6 bg-lm-yellow border-2 border-lm-line rounded-[24px] shadow-stamp-lg overflow-hidden">
            <svg class="absolute top-[-30px] right-[-30px] opacity-15 pointer-events-none" width="140" height="140" viewBox="0 0 80 80">
              <circle cx="40" cy="40" r="30" stroke="#1a1814" stroke-width="2" fill="none"/>
              <path d="M10 40 H 70 M 40 10 V 70" stroke="#1a1814" stroke-width="1.5" fill="none" stroke-dasharray="3 5"/>
            </svg>
            <svg class="absolute bottom-2.5 left-2.5 opacity-15 pointer-events-none" width="80" height="80" viewBox="0 0 80 80">
              <path d="M2 30 Q 15 5, 28 30 T 54 30 T 78 30" stroke="#1a1814" stroke-width="2" fill="none" stroke-linecap="round"/>
            </svg>

            <!-- Stacked cards preview -->
            <div class="relative w-[130px] h-[160px] shrink-0">
              <div
                v-for="(c, i) in queue.slice(0, 4)"
                :key="c.id"
                class="absolute flex flex-col justify-between p-2.5 bg-lm-surface border-2 border-lm-line rounded-[12px] shadow-stamp-sm"
                :style="{ left: `${i * 8}px`, top: `${i * 4}px`, width: '100px', height: '140px', transform: `rotate(${(i - 1.5) * 4}deg)`, zIndex: i }"
              >
                <span class="font-mono text-[8px] font-semibold tracking-widest uppercase text-lm-ink-3">{{ c.tag }}</span>
                <p class="text-[11px] font-semibold leading-tight text-lm-ink m-0">{{ c.front }}</p>
                <span class="font-mono text-[9px] text-lm-ink-3">FRONT</span>
              </div>
            </div>

            <div class="flex-1 min-w-0 relative">
              <h3 class="font-display text-[36px] font-bold leading-none tracking-tight text-lm-ink m-0">{{ queue.length }} cards waiting</h3>
              <p class="text-[14px] text-lm-ink mt-1.5 mb-0">
                <strong>{{ dueNow }} due right now</strong> · {{ queue.length - dueNow }} coming later today
              </p>
              <p class="font-mono text-[11px] text-lm-ink-2 mt-1 mb-0">EST. 4 MIN · AGAIN / HARD / GOOD / EASY</p>
              <button
                @click="router.push('/learn/flashcards/srs')"
                class="flex items-center gap-2 mt-3.5 px-6 py-3 text-[17px] font-semibold border-2 border-lm-line rounded-full bg-lm-ink text-lm-bg shadow-stamp-sm hover:-translate-y-px hover:shadow-stamp-md transition-all duration-200"
              >
                Start review
                <LmIcon name="arrow" :size="18" />
              </button>
            </div>
          </div>

          <!-- Queue list -->
          <div class="flex flex-col bg-lm-surface border-2 border-lm-line rounded-[24px] shadow-stamp-md overflow-hidden">
            <div class="flex justify-between items-baseline px-[18px] py-3.5 border-b border-lm-line-soft">
              <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3">UP NEXT</span>
              <span class="font-mono text-[10px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3">{{ queue.length }} CARDS</span>
            </div>
            <div class="flex-1 overflow-auto">
              <div
                v-for="(c, i) in queue"
                :key="c.id"
                :class="['flex items-center gap-2.5 px-[18px] py-2.5', i < queue.length - 1 ? 'border-b border-lm-line-soft' : '', c.due === 'now' ? 'bg-lm-yellow-soft' : '']"
              >
                <div :class="['w-2 h-2 rounded-full shrink-0', c.due === 'now' ? 'bg-lm-rust' : 'bg-lm-ink-3']" />
                <div class="flex-1 min-w-0">
                  <p class="text-[13.5px] font-semibold truncate text-lm-ink m-0">{{ c.front }}</p>
                  <span class="font-mono text-[9px] font-semibold tracking-widest uppercase text-lm-ink-3">{{ c.tag }}</span>
                </div>
                <span :class="['px-2.5 py-0.5 text-[11px] font-semibold border border-lm-line rounded-full shrink-0', c.due === 'now' ? 'bg-lm-rust-soft' : 'bg-lm-bg-soft']">
                  {{ c.due === 'now' ? 'NOW' : 'in ' + c.due }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- Decks section -->
      <section>
        <div class="flex items-baseline gap-3 mb-3.5 flex-wrap">
          <h2 class="font-display text-[22px] font-bold text-lm-ink m-0">Pre-made decks</h2>
          <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3">FREE PRACTICE</span>
          <span class="text-[13px] text-lm-ink-2">· pass / not pass · repeat anytime</span>
          <div class="flex-1" />
          <div class="flex gap-1.5">
            <button
              v-for="(t, i) in tagFilters"
              :key="t"
              :class="['px-3 py-1 text-xs font-semibold border-2 border-lm-line rounded-full transition-all duration-200', i === 0 ? 'bg-lm-ink text-lm-bg' : 'bg-lm-surface text-lm-ink hover:bg-lm-bg-soft']"
            >{{ t }}</button>
          </div>
        </div>

        <div class="grid grid-cols-3 gap-[18px]">
          <div
            v-for="d in decks"
            :key="d.id"
            @click="router.push(`/learn/flashcards/set/${d.id}`)"
            class="flex gap-3.5 items-center p-4 bg-lm-surface border-2 border-lm-line rounded-[18px] shadow-stamp-md cursor-pointer hover:-translate-y-0.5 hover:shadow-stamp-lg transition-all duration-200"
          >
            <!-- Stacked deck visual -->
            <div class="relative w-[70px] h-[92px] shrink-0">
              <div :class="['absolute inset-[4px_-4px_-4px_4px] rounded-[8px] border-2 border-lm-line opacity-55', d.bgClass]" />
              <div :class="['absolute inset-0 rounded-[8px] border-2 border-lm-line flex items-center justify-center', d.bgClass]">
                <span class="font-math italic font-bold text-[28px] text-lm-ink">{{ d.icon }}</span>
              </div>
            </div>

            <div class="flex-1 min-w-0">
              <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3">{{ d.tag }}</span>
              <h3 class="font-display text-[17px] font-bold text-lm-ink leading-tight mt-0.5 mb-1 m-0">{{ d.name }}</h3>
              <p class="font-mono text-[11px] text-lm-ink-2 m-0">{{ d.count }} CARDS</p>
              <p class="flex items-center gap-1.5 text-[13px] font-semibold text-lm-ink mt-1.5 m-0">
                Practice <LmIcon name="arrow" :size="14" />
              </p>
            </div>
          </div>
        </div>
      </section>
    </div>
  </main>
</template>
