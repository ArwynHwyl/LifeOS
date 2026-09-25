<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import ToraMascot from '@/components/tora/ToraMascot.vue'

const props = defineProps<{
  title: string
  exp: number
  completed: number
  total: number
  nextTitle?: string
  leveledUp: boolean
  newLevel: number
}>()
const emit = defineEmits<{ close: []; next: [] }>()
const dialog = ref<HTMLDialogElement | null>(null)
const displayedExp = ref(0)
const percent = computed(() => props.total ? Math.min(100, props.completed / props.total * 100) : 0)
let frame = 0
const colors = ['#ffd333', '#78975b', '#c5adf0', '#f2a77d', '#98c9ba']
const particles = Array.from({ length: 32 }, (_, i) => ({
  '--x': `${Math.cos(i * 2.4) * (130 + i % 5 * 35)}px`,
  '--y': `${Math.sin(i * 2.4) * (120 + i % 4 * 45) - 65}px`,
  '--turn': `${(i % 2 ? 1 : -1) * (180 + i * 23)}deg`,
  '--delay': `${i % 7 * 35}ms`,
  '--color': colors[i % colors.length],
}))

onMounted(() => {
  dialog.value?.showModal()
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
    displayedExp.value = props.exp
    return
  }
  const start = performance.now()
  function tick(now: number) {
    const t = Math.min(1, Math.max(0, (now - start - 350) / 850))
    displayedExp.value = Math.round(props.exp * (1 - (1 - t) ** 3))
    if (t < 1) frame = requestAnimationFrame(tick)
  }
  frame = requestAnimationFrame(tick)
})
onUnmounted(() => cancelAnimationFrame(frame))
</script>

<template>
  <dialog ref="dialog" class="reward-dialog" aria-labelledby="reward-heading" aria-describedby="reward-description" @cancel.prevent="emit('close')">
    <div class="reward-card">
      <div class="reward-confetti" aria-hidden="true"><i v-for="(style, i) in particles" :key="i" :style="style" /></div>
      <button class="reward-close" type="button" aria-label="Close completion reward" @click="emit('close')">×</button>
      <div class="reward-medal" aria-hidden="true">
        <div class="medal-halo" />
        <ToraMascot mood="cheer" :size="140" :track="false" />
        <span class="medal-spark medal-spark--one">✦</span><span class="medal-spark medal-spark--two">✧</span>
      </div>
      <p class="reward-eyebrow">ONE STEP FURTHER</p>
      <h2 id="reward-heading">{{ completed === total ? 'Course complete!' : 'You’ve got it!' }}</h2>
      <p id="reward-description" class="reward-description"><strong>{{ title }}</strong><br />Subtopic completed. Nice work!</p>
      <div v-if="exp > 0" class="reward-xp" :aria-label="`${exp} XP earned`"><span aria-hidden="true">✦ +{{ displayedExp }} <small>XP earned</small></span></div>
      <div v-else class="reward-xp reward-xp--saved">✓ Progress saved</div>
      <p v-if="leveledUp" class="reward-level">Level {{ newLevel }} unlocked <span>↗</span></p>
      <div class="reward-progress">
        <div><span>Your learning journey</span><strong>{{ completed }} / {{ total }} completed</strong></div>
        <div class="reward-track"><span :style="{ '--progress': `${percent}%`, '--previous': `${total ? Math.max(0, completed - 1) / total * 100 : 0}%` }" /></div>
      </div>
      <div class="reward-encouragement"><ToraMascot crop="head" :size="28" :track="false" aria-hidden="true" /><p>{{ completed === total ? 'Look at you. Every little step added up.' : 'Small steps. Real progress. Keep it going!' }}</p></div>
      <button v-if="nextTitle" class="reward-primary" type="button" @click="emit('next')">Next lesson <span>→</span></button>
      <button v-else class="reward-primary" type="button" @click="emit('close')">Keep learning <span>✓</span></button>
      <p v-if="nextTitle" class="reward-next">Up next: {{ nextTitle }}</p>
      <button v-if="nextTitle" type="button" class="reward-secondary" @click="emit('close')">Stay on this lesson</button>
    </div>
  </dialog>
</template>

<style scoped>
.reward-dialog { margin: auto; padding: 0; border: 0; border-radius: 24px; width: min(440px, calc(100% - 32px)); max-height: calc(100dvh - 32px); overflow: auto; color: #293626; background: #fffdf6; box-shadow: 0 28px 100px #17211040; animation: reward-arrive .5s cubic-bezier(.2,.8,.2,1) both; }
.reward-dialog::backdrop { background: #20271c80; backdrop-filter: blur(5px); animation: backdrop-arrive .25s ease both; }
.reward-card { position: relative; isolation: isolate; overflow: hidden; padding: 28px 32px 24px; text-align: center; background: radial-gradient(ellipse at 50% 15%, #fff0ad80, transparent 55%); }
.reward-close { position: absolute; top: 12px; right: 12px; z-index: 2; width: 36px; height: 36px; border: 1px solid #e4e1d4; border-radius: 50%; background: #fffdf6; font-size: 24px; cursor: pointer; }
.reward-medal { position: relative; width: 140px; margin: 0 auto 8px; animation: medal-pop .7s .1s cubic-bezier(.2,1.5,.4,1) both; }
.reward-medal svg { position: relative; display: block; width: 100%; filter: drop-shadow(0 6px 0 #b58e1820); }
.medal-halo { position: absolute; inset: -15px; border-radius: 50%; background: radial-gradient(circle, #ffdf6390, transparent 70%); animation: halo-bloom 1s ease-out both; }
.medal-spark { position: absolute; color: #c39716; font-size: 26px; animation: sparkle .8s .4s both; }.medal-spark--one { top: 3px; left: -25px; }.medal-spark--two { right: -24px; top: 50px; animation-delay: .65s; }
.reward-eyebrow { margin: 6px 0 10px; font-family: var(--font-mono); font-size: 10px; letter-spacing: .16em; color: #72735e; }
h2 { margin: 0; font-family: var(--font-display); font-size: clamp(28px, 6vw, 36px); font-weight: 750; letter-spacing: -.04em; line-height: 1.1; }
.reward-description { margin: 12px 0 16px; color: #727164; font-size: 13px; line-height: 1.7; }.reward-description strong { color: #454b3c; font-weight: 550; overflow-wrap: anywhere; }
.reward-xp { display: inline-flex; padding: 10px 22px; background: #ffdf61; border: 1px solid #e7c244; border-radius: 12px; font-size: 30px; font-weight: 750; font-family: var(--font-display); font-variant-numeric: tabular-nums; animation: xp-arrive .45s .3s both; }.reward-xp small { font: 600 12px var(--font-display); }.reward-xp--saved { font-size: 17px; background: #edf2e4; border-color: #d7e1c9; }
.reward-level { font-size: 12px; font-weight: 700; color: #56733d; margin: 10px 0 0; }
.reward-progress { margin-top: 22px; }.reward-progress>div:first-child { display: flex; justify-content: space-between; gap: 8px; font-size: 10px; color: #777869; }.reward-progress strong { font-weight: 600; color: #47583b; }
.reward-track { height: 7px; margin-top: 9px; overflow: hidden; border-radius: 8px; background: #e8eadf; }.reward-track span { display: block; height: 100%; width: var(--progress); border-radius: inherit; background: #78975b; animation: progress-fill .8s .5s both; }
.reward-encouragement { display: flex; justify-content: center; align-items: center; gap: 8px; margin: 18px 0; }.reward-encouragement img { width: 30px; height: 36px; }.reward-encouragement p { font-size: 11px; color: #737565; margin: 0; }
.reward-primary { display: flex; justify-content: space-between; align-items: center; width: 100%; min-height: 46px; padding: 12px 18px; background: #293626; color: #fffdf6; border: 0; border-radius: 10px; font-size: 14px; font-weight: 600; cursor: pointer; }.reward-primary:hover { background: #415238; }.reward-next { margin: 9px 0 0; font-size: 10px; color: #747565; overflow-wrap: anywhere; }.reward-secondary { margin-top: 8px; min-height: 36px; font-size: 12px; text-decoration: underline; text-underline-offset: 3px; color: #646a58; cursor: pointer; }
button:focus-visible { outline: 3px solid #78975b; outline-offset: 3px; }
.reward-confetti { position: absolute; top: 90px; left: 50%; pointer-events: none; z-index: -1; }.reward-confetti i { position: absolute; width: 7px; height: 12px; border-radius: 2px; background: var(--color); animation: confetti-burst 1.6s var(--delay) cubic-bezier(.15,.65,.3,1) both; }.reward-confetti i:nth-child(3n) { border-radius: 50%; width: 7px; height: 7px; }
@keyframes reward-arrive { from { opacity: 0; transform: translateY(24px) scale(.94); } to { opacity: 1; transform: none; } }
@keyframes backdrop-arrive { from { opacity: 0; } to { opacity: 1; } }
@keyframes medal-pop { from { opacity: 0; transform: scale(.4) rotate(-18deg); } to { opacity: 1; transform: none; } }
@keyframes halo-bloom { from { opacity: 0; transform: scale(.4); } to { opacity: 1; transform: scale(1.2); } }
@keyframes sparkle { 0% { opacity: 0; transform: scale(0) rotate(-40deg); } 60% { opacity: 1; transform: scale(1.3); } 100% { transform: scale(1); } }
@keyframes xp-arrive { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: none; } }
@keyframes progress-fill { from { width: var(--previous); } to { width: var(--progress); } }
@keyframes confetti-burst { 0% { opacity: 0; transform: translate(0,0) scale(.2); } 12% { opacity: 1; } 70% { opacity: 1; } 100% { opacity: 0; transform: translate(var(--x),var(--y)) rotate(var(--turn)); } }
@media(max-width:480px) { .reward-card { padding: 24px 22px 18px; }.reward-medal { width: 112px; height: 127px; }.reward-medal :deep(.tora) { transform: scale(.8); transform-origin: top left; } }
@media(prefers-reduced-motion:reduce) { *, *::before, *::after, .reward-dialog::backdrop { animation: none!important; }.reward-confetti { display: none; } }
</style>
