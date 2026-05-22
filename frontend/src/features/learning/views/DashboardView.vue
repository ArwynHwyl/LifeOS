<script setup lang="ts">
import { ref } from 'vue'
import LmIcon from '../components/LmIcon.vue'
import MomentLevelUp from '../components/MomentLevelUp.vue'
import MomentStreakBroken from '../components/MomentStreakBroken.vue'
import MomentShieldUsed from '../components/MomentShieldUsed.vue'

const badges = [
  { name: 'First Steps',     desc: 'Complete your first lesson',      got: true,  glyph: '★',  colorClass: 'bg-lm-yellow' },
  { name: '7-Day Streak',    desc: 'Study 7 days in a row',           got: true,  glyph: '🔥', colorClass: 'bg-lm-rust-soft' },
  { name: 'Quiz Ace',        desc: 'Perfect score on 5 quizzes',      got: true,  glyph: 'A⁺', colorClass: 'bg-lm-green-soft' },
  { name: 'Equation Slayer', desc: 'Master Algebra Basics',           got: true,  glyph: '=',  colorClass: 'bg-lm-blue-soft' },
  { name: 'Speed Solver',    desc: 'Solve 10 questions in 5 min',     got: false, glyph: '⚡', colorClass: 'bg-lm-bg-soft' },
  { name: 'Formula Master',  desc: 'Memorize 50 flashcards',          got: false, glyph: '∑',  colorClass: 'bg-lm-bg-soft' },
  { name: 'Geometry Whiz',   desc: 'Master Geometry course',          got: false, glyph: '△',  colorClass: 'bg-lm-bg-soft' },
  { name: '30-Day Streak',   desc: 'Study 30 days in a row',          got: false, glyph: '🔥', colorClass: 'bg-lm-bg-soft' },
]

const weekDays = ['M', 'T', 'W', 'T', 'F', 'S', 'S']
const activityData = [0,1,2,2,3,4,3,2,1,2,3,3,2,4,3,2,3,4,2,1,3,2,3,4,3,2,1,3,4,2]
const heatClasses = ['bg-lm-bg-soft', 'bg-lm-yellow-soft', 'bg-lm-yellow', 'bg-lm-rust', 'bg-lm-ink']

const showLevelUp     = ref(false)
const showStreakBroken = ref(false)
const showShieldUsed   = ref(false)
</script>

<template>
  <main class="flex-1 overflow-auto bg-lm-bg relative">
    <div class="absolute inset-0 bg-dot-grid opacity-40 pointer-events-none" />

    <div class="relative max-w-[1280px] mx-auto px-9 py-7 flex flex-col gap-5">

      <!-- Hero profile card -->
      <div class="relative flex items-center gap-6 px-7 py-6 bg-lm-yellow border-2 border-lm-line rounded-[24px] shadow-stamp-lg overflow-hidden">
        <svg class="absolute top-[-40px] right-[-50px] opacity-12 pointer-events-none" width="180" height="180" viewBox="0 0 80 80">
          <circle cx="40" cy="40" r="30" stroke="#1a1814" stroke-width="2" fill="none"/>
          <path d="M10 40 H 70 M 40 10 V 70" stroke="#1a1814" stroke-width="1.5" fill="none" stroke-dasharray="3 5"/>
        </svg>
        <svg class="absolute bottom-[-10px] left-[-10px] opacity-15 pointer-events-none" width="80" height="80" viewBox="0 0 80 80">
          <template v-for="i in 25" :key="i">
            <circle :cx="10 + ((i-1) % 5) * 15" :cy="10 + Math.floor((i-1) / 5) * 15" r="1.5" fill="#1a1814"/>
          </template>
        </svg>

        <!-- Avatar -->
        <div class="w-[92px] h-[92px] rounded-full bg-lm-surface border-2 border-lm-line shadow-stamp-sm flex items-center justify-center font-display font-bold text-[38px] text-lm-ink shrink-0">
          JD
        </div>

        <div class="flex-1 min-w-0 relative">
          <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-2">MEMBER SINCE MAR 2026</span>
          <h1 class="font-display text-[36px] font-bold tracking-tight text-lm-ink leading-none mt-0.5 mb-1 m-0">Jamie Doe</h1>
          <p class="text-[16px] text-lm-ink mt-1 m-0">
            Level <strong>12</strong> ·
            <span class="bg-lm-surface border-2 border-lm-line px-2.5 py-0.5 rounded-full font-semibold text-[14px]">Apprentice Mathematician</span>
          </p>
          <div class="flex items-center gap-2.5 mt-3.5 max-w-[460px]">
            <div class="flex-1 h-3.5 rounded-full overflow-hidden border border-lm-line" style="background: rgba(26,24,20,.15)">
              <div class="h-full bg-lm-surface transition-all duration-200" style="width: 68%" />
            </div>
            <span class="font-mono text-[12px] font-semibold text-lm-ink shrink-0">340 / 500 XP</span>
          </div>
          <p class="font-mono text-[11px] text-lm-ink-2 mt-1 m-0">160 XP TO LEVEL 13</p>
        </div>

        <button class="flex items-center gap-2 px-[18px] py-[9px] text-[15px] font-semibold border-2 border-lm-line rounded-full bg-lm-ink text-lm-bg shadow-stamp-sm hover:-translate-y-px hover:shadow-stamp-md transition-all duration-200 shrink-0 relative">
          Edit profile
        </button>
      </div>

      <!-- Streak + shields row -->
      <div class="flex items-center gap-[22px] px-[22px] py-[22px] bg-lm-surface border-2 border-lm-line rounded-[24px] shadow-stamp-md">
        <!-- Streak block -->
        <div class="flex items-center gap-3.5">
          <div class="w-16 h-16 rounded-[12px] bg-lm-rust-soft border-2 border-lm-line text-lm-rust shadow-stamp-sm flex items-center justify-center shrink-0">
            <LmIcon name="flame" :size="36" :filled="true" />
          </div>
          <div>
            <p class="font-display text-[36px] font-bold leading-none text-lm-ink m-0">7 <span class="text-[18px] font-medium text-lm-ink-2">days</span></p>
            <p class="text-[13px] text-lm-ink mt-1 m-0">study streak · <strong class="text-lm-rust">+10% XP boost</strong></p>
          </div>
        </div>

        <div class="w-0.5 self-stretch bg-lm-line-soft" />

        <!-- Week strip -->
        <div>
          <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3">LAST 7 DAYS</span>
          <div class="flex gap-[7px] mt-1.5">
            <div v-for="(d, i) in weekDays" :key="i" class="text-center">
              <div :class="[
                'w-9 h-9 rounded-full border-2 flex items-center justify-center font-display font-bold text-sm',
                i < 6 ? 'bg-lm-yellow border-lm-line shadow-stamp-sm text-lm-ink' : 'bg-lm-surface border-dashed border-lm-line text-lm-ink'
              ]">
                <LmIcon v-if="i < 6" name="check" :size="16" />
                <span v-else class="text-sm">?</span>
              </div>
              <p class="font-mono text-[10px] text-lm-ink-3 mt-1 m-0">{{ d }}</p>
            </div>
          </div>
        </div>

        <div class="flex-1" />

        <!-- Shields -->
        <div class="text-center">
          <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3 block mb-1.5">SHIELDS · 2 / 3</span>
          <div class="flex gap-1.5">
            <div
              v-for="(on, i) in [true, true, false]"
              :key="i"
              :class="[
                'w-11 h-[50px] rounded-[8px] border-2 flex items-center justify-center',
                on ? 'bg-lm-blue-soft border-lm-line border-solid text-lm-blue shadow-stamp-sm' : 'bg-lm-bg-soft border-dashed border-lm-line text-lm-ink-3'
              ]"
            >
              <LmIcon name="shield" :size="26" :filled="on" />
            </div>
          </div>
        </div>
      </div>

      <!-- Trophy case -->
      <section>
        <div class="flex items-baseline gap-3 mb-3.5">
          <h2 class="font-display text-[22px] font-bold text-lm-ink m-0">Trophy case</h2>
          <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3">4 OF 8 UNLOCKED</span>
          <div class="w-24 h-1.5 bg-lm-bg-soft rounded-full overflow-hidden border border-lm-line self-center">
            <div class="h-full bg-lm-yellow" style="width: 50%" />
          </div>
        </div>

        <div class="relative p-[22px] bg-lm-yellow-soft border-2 border-lm-line rounded-[24px] shadow-stamp-md overflow-hidden">
          <div class="absolute inset-0 bg-dot-grid opacity-40 pointer-events-none" />
          <div class="relative grid grid-cols-4 gap-4">
            <div
              v-for="b in badges"
              :key="b.name"
              :class="[
                'flex flex-col items-center gap-2 p-4 text-center rounded-[18px] border-2 transition-all',
                b.got ? 'bg-lm-surface border-lm-line shadow-stamp-sm' : 'bg-transparent border-dashed border-lm-line opacity-55'
              ]"
            >
              <div :class="[
                'w-[60px] h-[60px] rounded-full border-2 border-lm-line flex items-center justify-center font-math italic font-bold text-[24px] text-lm-ink',
                b.got ? `${b.colorClass} shadow-stamp-sm` : 'bg-lm-bg-soft'
              ]">
                <template v-if="b.got">{{ b.glyph }}</template>
                <LmIcon v-else name="lock" :size="20" class="text-lm-ink-2" />
              </div>
              <p class="font-display font-bold text-[14px] leading-tight text-lm-ink m-0">{{ b.name }}</p>
              <p class="text-[11px] text-lm-ink-2 leading-snug m-0">{{ b.desc }}</p>
            </div>
          </div>
        </div>
      </section>

      <!-- Activity grid -->
      <section class="px-[22px] py-[22px] bg-lm-surface border-2 border-lm-line rounded-[24px] shadow-stamp-md">
        <div class="flex items-baseline gap-3 mb-3">
          <h2 class="font-display text-[22px] font-bold text-lm-ink m-0">Recent activity</h2>
          <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3">LAST 30 DAYS · 24 ACTIVE</span>
          <div class="flex-1" />
          <div class="flex items-center gap-1">
            <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3">LESS</span>
            <div v-for="c in heatClasses" :key="c" :class="['w-3 h-3 border border-lm-line rounded-[2px]', c]" />
            <span class="font-mono text-[11px] font-semibold tracking-[0.06em] uppercase text-lm-ink-3">MORE</span>
          </div>
        </div>
        <div class="grid gap-1" style="grid-template-columns: repeat(30, 1fr)">
          <div
            v-for="(v, i) in activityData"
            :key="i"
            :class="['aspect-square border border-lm-line rounded-[4px]', heatClasses[v]]"
          />
        </div>
      </section>

      <!-- Demo moment triggers -->
      <div class="flex gap-3 flex-wrap">
        <button @click="showLevelUp = true"      class="px-4 py-2 text-sm font-semibold border-2 border-lm-line rounded-full bg-lm-yellow shadow-stamp-sm hover:-translate-y-px transition-all duration-200">✨ Demo: Level Up</button>
        <button @click="showStreakBroken = true"  class="px-4 py-2 text-sm font-semibold border-2 border-lm-line rounded-full bg-lm-rust-soft shadow-stamp-sm hover:-translate-y-px transition-all duration-200">💨 Demo: Streak Broken</button>
        <button @click="showShieldUsed = true"    class="px-4 py-2 text-sm font-semibold border-2 border-lm-line rounded-full bg-lm-blue-soft shadow-stamp-sm hover:-translate-y-px transition-all duration-200">🛡 Demo: Shield Used</button>
      </div>
    </div>

    <!-- Moment overlays -->
    <MomentLevelUp      v-if="showLevelUp"      @close="showLevelUp = false" />
    <MomentStreakBroken v-if="showStreakBroken"  @close="showStreakBroken = false" />
    <MomentShieldUsed   v-if="showShieldUsed"    @close="showShieldUsed = false" />
  </main>
</template>
