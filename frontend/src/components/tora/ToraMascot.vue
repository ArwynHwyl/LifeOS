<script lang="ts">
let toraUid = 0
</script>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

export type ToraMood = 'happy' | 'wave' | 'think' | 'cheer' | 'oops'
export type ToraHolding = 'none' | 'pencil' | 'key' | 'envelope' | 'check'

const props = withDefaults(defineProps<{
  mood?: ToraMood
  holding?: ToraHolding
  size?: number
  cover?: boolean
  crop?: 'full' | 'head'
  track?: boolean
}>(), { mood: 'happy', holding: 'none', size: 260, cover: false, crop: 'full', track: true })

const uid = `tora-${++toraUid}`
const root = ref<SVGSVGElement | null>(null)
const look = ref({ x: 0, y: 0 })

const VIEWBOX = { full: '0 0 300 340', head: '46 42 208 204' } as const
const heightRatio = computed(() => (props.crop === 'head' ? 204 / 208 : 340 / 300))

const gaze = computed(() => {
  if (props.mood === 'think') return { x: 6, y: -6 }
  if (props.mood === 'oops') return { x: 0, y: 6 }
  return look.value
})

const showMouth = computed<'open' | 'big' | 'flat' | 'sad'>(() => {
  if (props.mood === 'cheer') return 'big'
  if (props.mood === 'think') return 'flat'
  if (props.mood === 'oops') return 'sad'
  return 'open'
})

const blush = computed(() => props.mood !== 'oops' && props.mood !== 'think')

const leftArm = computed<'rest' | 'up' | 'cover'>(() => {
  if (props.cover) return 'cover'
  if (props.mood === 'cheer') return 'up'
  return 'rest'
})
const rightArm = computed<'rest' | 'wave' | 'think' | 'up' | 'cover'>(() => {
  if (props.cover) return 'cover'
  if (props.mood === 'cheer') return 'up'
  if (props.mood === 'think') return 'think'
  if (props.mood === 'wave' && props.holding === 'none') return 'wave'
  return 'rest'
})

let frame = 0
let reduced = false
function onPointer(event: PointerEvent) {
  if (!root.value || frame) return
  frame = requestAnimationFrame(() => {
    frame = 0
    if (!root.value) return
    const rect = root.value.getBoundingClientRect()
    const cx = rect.left + rect.width * 0.5
    const cy = rect.top + rect.height * (props.crop === 'head' ? 0.5 : 0.44)
    const clamp = (v: number) => Math.max(-1, Math.min(1, v))
    look.value = { x: clamp((event.clientX - cx) / 320) * 6, y: clamp((event.clientY - cy) / 320) * 5 }
  })
}

onMounted(() => {
  reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  if (props.track && !reduced) window.addEventListener('pointermove', onPointer, { passive: true })
})
onBeforeUnmount(() => {
  window.removeEventListener('pointermove', onPointer)
  if (frame) cancelAnimationFrame(frame)
})
</script>

<template>
  <div class="tora" :class="[`tora--${mood}`, { 'tora--cover': cover }]" :style="{ width: `${size}px`, height: `${Math.round(size * heightRatio)}px` }">
    <svg ref="root" :viewBox="VIEWBOX[crop]" width="100%" height="100%" style="overflow: visible" role="img" aria-label="Tora the tiger">
      <defs>
        <clipPath :id="`${uid}-mouth`">
          <path d="M124 198 Q150 246 176 198 Q150 208 124 198Z" />
        </clipPath>
      </defs>

      <g class="tora-figure">
        <ellipse v-if="crop === 'full'" class="tora-shadow" cx="150" cy="330" rx="80" ry="7" fill="#000" opacity="0.1" />

        <template v-if="crop === 'full'">
          <!-- Tail -->
          <g class="tora-tail">
            <path d="M200 314 Q266 322 266 268 Q266 238 250 220" stroke="#FF9A1F" stroke-width="26" stroke-linecap="round" fill="none" />
            <path d="M232 314 L240 304 M254 298 L264 292 M258 262 L268 258" stroke="#33241A" stroke-width="7" stroke-linecap="round" fill="none" />
            <circle cx="250" cy="220" r="13" fill="#33241A" />
          </g>

          <!-- Torso -->
          <g class="tora-torso">
            <ellipse cx="112" cy="326" rx="27" ry="13" fill="#FF9A1F" />
            <ellipse cx="188" cy="326" rx="27" ry="13" fill="#FF9A1F" />
            <path d="M96 236 Q150 224 204 236 Q234 284 222 326 L78 326 Q66 284 96 236Z" fill="#FF9A1F" />
            <ellipse cx="150" cy="290" rx="43" ry="40" fill="#FFF0D4" />
            <path d="M84 266 Q98 268 108 280 M82 292 Q96 292 106 300 M216 266 Q202 268 192 280 M218 292 Q204 292 194 300" stroke="#33241A" stroke-width="7" stroke-linecap="round" fill="none" />
          </g>

        </template>

        <!-- Head -->
        <g class="tora-head" :style="{ transform: `translate(${gaze.x * 0.35}px, ${gaze.y * 0.3}px)` }">
          <!-- Ears -->
          <g class="tora-ear tora-ear--l">
            <circle cx="82" cy="88" r="30" fill="#FF9A1F" />
            <circle cx="84" cy="90" r="17" fill="#FFC9B0" />
          </g>
          <g class="tora-ear tora-ear--r">
            <circle cx="218" cy="88" r="30" fill="#FF9A1F" />
            <circle cx="216" cy="90" r="17" fill="#FFC9B0" />
          </g>

          <path d="M52 152 C52 92 92 62 150 62 C208 62 248 92 248 152 C248 208 208 236 150 236 C92 236 52 208 52 152Z" fill="#FF9A1F" />
          <!-- Stripes -->
          <g stroke="#33241A" stroke-linecap="round" fill="none">
            <path d="M150 64 L150 100" stroke-width="9" />
            <path d="M120 68 Q124 88 136 100" stroke-width="7" />
            <path d="M180 68 Q176 88 164 100" stroke-width="7" />
            <path d="M54 138 Q76 140 90 152" stroke-width="7" />
            <path d="M56 168 Q74 168 88 178" stroke-width="7" />
            <path d="M246 138 Q224 140 210 152" stroke-width="7" />
            <path d="M244 168 Q226 168 212 178" stroke-width="7" />
          </g>
          <!-- Muzzle -->
          <ellipse cx="150" cy="196" rx="64" ry="42" fill="#FFF0D4" />

          <g v-if="blush">
            <ellipse cx="82" cy="192" rx="13" ry="8" fill="#FF7A8A" opacity="0.4" />
            <ellipse cx="218" cy="192" rx="13" ry="8" fill="#FF7A8A" opacity="0.4" />
          </g>

          <!-- Brows -->
          <g stroke="#33241A" stroke-width="6" stroke-linecap="round" fill="none">
            <g v-if="mood === 'think'">
              <path d="M92 100 Q110 90 128 98" />
              <path d="M172 108 Q190 106 208 112" />
            </g>
            <g v-else-if="mood === 'oops'">
              <path d="M92 112 Q110 104 128 96" />
              <path d="M172 96 Q190 104 208 112" />
            </g>
            <g v-else-if="mood === 'cheer'">
              <path d="M92 104 Q110 92 128 104" />
              <path d="M172 104 Q190 92 208 104" />
            </g>
          </g>

          <!-- Eyes -->
          <g v-if="!cover">
            <g class="tora-eye tora-eye--l">
              <ellipse cx="112" cy="144" rx="21" ry="27" fill="#fff" />
              <g class="tora-pupil" :style="{ transform: `translate(${gaze.x}px, ${gaze.y}px)` }">
                <circle cx="112" cy="146" r="13.5" fill="#33241A" />
                <circle cx="117" cy="139" r="5" fill="#fff" />
                <circle cx="107" cy="152" r="2.4" fill="#fff" opacity="0.8" />
              </g>
            </g>
            <g class="tora-eye tora-eye--r">
              <ellipse cx="188" cy="144" rx="21" ry="27" fill="#fff" />
              <g class="tora-pupil" :style="{ transform: `translate(${gaze.x}px, ${gaze.y}px)` }">
                <circle cx="188" cy="146" r="13.5" fill="#33241A" />
                <circle cx="193" cy="139" r="5" fill="#fff" />
                <circle cx="183" cy="152" r="2.4" fill="#fff" opacity="0.8" />
              </g>
            </g>
          </g>
          <g v-else>
            <path d="M94 148 Q112 158 130 148" stroke="#33241A" stroke-width="5" stroke-linecap="round" fill="none" />
            <path d="M170 148 Q188 158 206 148" stroke="#33241A" stroke-width="5" stroke-linecap="round" fill="none" />
          </g>

          <!-- Nose + mouth -->
          <path d="M136 174 Q150 166 164 174 Q160 188 150 190 Q140 188 136 174Z" fill="#D9536B" />
          <ellipse cx="146" cy="174" rx="4" ry="2" fill="#fff" opacity="0.5" />
          <path d="M150 190 L150 198" stroke="#33241A" stroke-width="4" stroke-linecap="round" />
          <g v-if="showMouth === 'open' || showMouth === 'big'">
            <path :d="showMouth === 'big' ? 'M120 196 Q150 256 180 196 Q150 208 120 196Z' : 'M124 198 Q150 246 176 198 Q150 208 124 198Z'" fill="#4A1F1F" />
            <g :clip-path="showMouth === 'open' ? `url(#${uid}-mouth)` : undefined">
              <ellipse cx="150" :cy="showMouth === 'big' ? 230 : 222" rx="15" ry="9" fill="#FF8FA3" />
            </g>
          </g>
          <path v-else-if="showMouth === 'flat'" d="M134 204 Q150 210 168 200" stroke="#33241A" stroke-width="4.5" stroke-linecap="round" fill="none" />
          <path v-else d="M130 214 Q150 198 170 214" stroke="#33241A" stroke-width="4.5" stroke-linecap="round" fill="none" />

          <!-- Sweat drop -->
          <path v-if="mood === 'oops'" class="tora-sweat" d="M226 104 Q236 120 226 128 Q216 120 226 104Z" fill="#7DD3FC" />
        </g>

        <template v-if="crop === 'full'">
          <!-- Left arm -->
          <Transition name="tora-arm" mode="out-in">
            <g v-if="leftArm === 'rest'" key="l-rest" class="tora-arm">
              <path d="M104 254 Q78 276 84 308" stroke="#FF9A1F" stroke-width="30" stroke-linecap="round" fill="none" />
              <ellipse cx="84" cy="311" rx="9" ry="6" fill="#FFF0D4" />
            </g>
            <g v-else-if="leftArm === 'up'" key="l-up" class="tora-arm tora-arm--cheer-l">
              <path d="M104 252 Q30 240 32 176" stroke="#FF9A1F" stroke-width="30" stroke-linecap="round" fill="none" />
              <ellipse cx="32" cy="166" rx="9" ry="6" fill="#FFF0D4" />
            </g>
          </Transition>

          <!-- Right arm -->
          <Transition name="tora-arm" mode="out-in">
            <g v-if="rightArm === 'rest'" key="r-rest" class="tora-arm">
              <path d="M196 254 Q222 276 216 308" stroke="#FF9A1F" stroke-width="30" stroke-linecap="round" fill="none" />
              <ellipse cx="216" cy="311" rx="9" ry="6" fill="#FFF0D4" />
            </g>
            <g v-else-if="rightArm === 'wave'" key="r-wave" class="tora-arm">
              <g class="tora-wave">
                <path d="M200 252 Q266 250 268 196" stroke="#FF9A1F" stroke-width="30" stroke-linecap="round" fill="none" />
                <ellipse cx="268" cy="186" rx="9" ry="6" fill="#FFF0D4" />
                <path d="M260 180 L260 174 M268 178 L268 172 M276 180 L276 174" stroke="#33241A" stroke-width="3" stroke-linecap="round" />
              </g>
            </g>
            <g v-else-if="rightArm === 'think'" key="r-think" class="tora-arm">
              <path d="M198 254 Q214 228 182 226" stroke="#FF9A1F" stroke-width="30" stroke-linecap="round" fill="none" />
              <ellipse cx="174" cy="226" rx="8" ry="10" fill="#FFF0D4" />
            </g>
            <g v-else-if="rightArm === 'up'" key="r-up" class="tora-arm tora-arm--cheer-r">
              <path d="M196 252 Q270 240 268 176" stroke="#FF9A1F" stroke-width="30" stroke-linecap="round" fill="none" />
              <ellipse cx="268" cy="166" rx="9" ry="6" fill="#FFF0D4" />
            </g>
          </Transition>

          <!-- Held item -->
          <g v-if="holding !== 'none' && !cover && mood !== 'cheer'" class="tora-held">
            <g v-if="holding === 'envelope'">
              <rect x="196" y="268" width="50" height="34" rx="7" fill="#fff" />
              <path d="M198 273 L221 291 L244 273" stroke="#D9D9D9" stroke-width="3" fill="none" stroke-linejoin="round" stroke-linecap="round" />
            </g>
            <g v-else-if="holding === 'key'">
              <circle cx="214" cy="278" r="12" fill="none" stroke="#FFC800" stroke-width="7" />
              <path d="M224 284 L248 302 M238 294 L244 288 M244 299 L250 293" stroke="#FFC800" stroke-width="7" stroke-linecap="round" fill="none" />
            </g>
            <g v-else-if="holding === 'pencil'" transform="rotate(38 222 286)">
              <rect x="212" y="256" width="20" height="60" rx="4" fill="#FFC800" />
              <rect x="212" y="256" width="20" height="10" rx="4" fill="#FF8FA3" />
              <path d="M212 316 L222 334 L232 316Z" fill="#FFF0D4" />
              <path d="M218 326 L222 334 L226 326Z" fill="#33241A" />
            </g>
            <g v-else-if="holding === 'check'">
              <circle cx="222" cy="282" r="20" fill="#58CC02" />
              <path d="M212 283 L220 291 L234 275" stroke="#fff" stroke-width="6" fill="none" stroke-linecap="round" stroke-linejoin="round" />
            </g>
            <ellipse cx="216" cy="311" rx="9" ry="6" fill="#FFF0D4" />
          </g>
        </template>

        <!-- Covering paws -->
        <Transition name="tora-cover">
          <g v-if="cover && crop === 'full'" class="tora-cover-paws">
            <path d="M104 254 Q60 214 100 160" stroke="#FF9A1F" stroke-width="32" stroke-linecap="round" fill="none" />
            <path d="M196 254 Q240 214 200 160" stroke="#FF9A1F" stroke-width="32" stroke-linecap="round" fill="none" />
            <ellipse cx="108" cy="146" rx="26" ry="24" fill="#FF9A1F" />
            <ellipse cx="192" cy="146" rx="26" ry="24" fill="#FF9A1F" />
            <ellipse cx="110" cy="152" rx="12" ry="9" fill="#FFF0D4" />
            <ellipse cx="190" cy="152" rx="12" ry="9" fill="#FFF0D4" />
            <path d="M98 138 L98 132 M108 134 L108 128 M118 138 L118 132 M182 138 L182 132 M192 134 L192 128 M202 138 L202 132" stroke="#33241A" stroke-width="3" stroke-linecap="round" />
          </g>
        </Transition>

        <!-- Cheer sparkles -->
        <g v-if="mood === 'cheer' && crop === 'full'" class="tora-sparkles">
          <path class="sp sp1" d="M40 90 L44 102 L56 106 L44 110 L40 122 L36 110 L24 106 L36 102Z" fill="#FFC800" />
          <path class="sp sp2" d="M262 70 L265 79 L274 82 L265 85 L262 94 L259 85 L250 82 L259 79Z" fill="#1CB0F6" />
          <path class="sp sp3" d="M254 200 L257 208 L265 211 L257 214 L254 222 L251 214 L243 211 L251 208Z" fill="#CE82FF" />
          <circle class="sp sp4" cx="30" cy="206" r="5" fill="#58CC02" />
          <circle class="sp sp5" cx="272" cy="130" r="4" fill="#FF8FA3" />
        </g>
      </g>
    </svg>
  </div>
</template>

<style scoped>
.tora { display: inline-block; position: relative; }
.tora svg { display: block; }

.tora-figure { animation: tora-float 3.6s ease-in-out infinite; }
.tora--cheer .tora-figure { animation: tora-hop 0.9s cubic-bezier(0.3, 0.7, 0.4, 1) infinite; }
.tora--oops .tora-figure { animation: none; }
.tora--oops .tora-head { animation: tora-shake 0.5s ease-in-out 1; }

.tora-torso { transform-origin: 150px 326px; animation: tora-breathe 3.6s ease-in-out infinite; }
.tora-head { transition: transform 0.14s ease-out; }
.tora-pupil { transition: transform 0.14s ease-out; }

.tora-eye { transform-box: fill-box; transform-origin: center; animation: tora-blink 4.6s infinite; }
.tora-eye--r { animation-delay: 0.04s; }
.tora-ear { transform-box: fill-box; transform-origin: 50% 90%; }
.tora-ear--l { animation: tora-twitch 7s infinite; }
.tora-ear--r { animation: tora-twitch 9s 2s infinite; }
.tora-tail { transform-origin: 204px 312px; animation: tora-tail 2.4s ease-in-out infinite alternate; }
.tora-wave { transform-origin: 198px 252px; animation: tora-wave 1s ease-in-out infinite; }
.tora-held { animation: tora-held 3.6s ease-in-out infinite; }
.tora-sweat { animation: tora-drip 1.6s ease-in infinite; }

.tora-arm--cheer-l { transform-origin: 104px 252px; animation: tora-cheer-l 0.9s ease-in-out infinite; }
.tora-arm--cheer-r { transform-origin: 196px 252px; animation: tora-cheer-r 0.9s ease-in-out infinite; }

.sp { transform-box: fill-box; transform-origin: center; animation: tora-twinkle 1.4s ease-in-out infinite; }
.sp2 { animation-delay: 0.3s; } .sp3 { animation-delay: 0.6s; } .sp4 { animation-delay: 0.9s; } .sp5 { animation-delay: 0.2s; }

.tora-arm-enter-active { transition: opacity 0.16s ease, transform 0.2s cubic-bezier(0.34, 1.56, 0.64, 1); }
.tora-arm-leave-active { transition: opacity 0.1s ease; }
.tora-arm-enter-from, .tora-arm-leave-to { opacity: 0; }
.tora-arm-enter-from { transform: translateY(10px); }

.tora-cover-enter-active { transition: transform 0.32s cubic-bezier(0.34, 1.4, 0.64, 1), opacity 0.2s ease; }
.tora-cover-leave-active { transition: transform 0.22s ease-in, opacity 0.18s ease; }
.tora-cover-enter-from, .tora-cover-leave-to { transform: translateY(90px); opacity: 0; }

@keyframes tora-float { 0%, 100% { transform: translateY(0); } 50% { transform: translateY(-5px); } }
@keyframes tora-hop {
  0%, 100% { transform: translateY(0) scale(1.02, 0.97); }
  40% { transform: translateY(-18px) scale(0.98, 1.03); }
  75% { transform: translateY(0) scale(1.02, 0.97); }
}
@keyframes tora-breathe { 0%, 100% { transform: scale(1, 1); } 50% { transform: scale(1.012, 1.02); } }
@keyframes tora-blink { 0%, 92%, 100% { transform: scaleY(1); } 95% { transform: scaleY(0.08); } }
@keyframes tora-twitch { 0%, 90%, 100% { transform: rotate(0); } 93% { transform: rotate(-12deg); } 96% { transform: rotate(5deg); } }
@keyframes tora-tail { from { transform: rotate(-7deg); } to { transform: rotate(9deg); } }
@keyframes tora-wave { 0%, 100% { transform: rotate(-4deg); } 50% { transform: rotate(-22deg); } }
@keyframes tora-held { 0%, 100% { transform: translateY(0); } 50% { transform: translateY(-2px); } }
@keyframes tora-shake { 0%, 100% { transform: translateX(0) rotate(0); } 20% { transform: translateX(-6px) rotate(-3deg); } 40% { transform: translateX(6px) rotate(3deg); } 60% { transform: translateX(-4px) rotate(-2deg); } 80% { transform: translateX(3px) rotate(1deg); } }
@keyframes tora-drip { 0% { transform: translateY(0); opacity: 1; } 100% { transform: translateY(22px); opacity: 0; } }
@keyframes tora-cheer-l { 0%, 100% { transform: rotate(4deg); } 50% { transform: rotate(-9deg); } }
@keyframes tora-cheer-r { 0%, 100% { transform: rotate(-4deg); } 50% { transform: rotate(9deg); } }
@keyframes tora-twinkle { 0%, 100% { transform: scale(0.4); opacity: 0.4; } 50% { transform: scale(1.15); opacity: 1; } }

@media (prefers-reduced-motion: reduce) {
  .tora-figure, .tora-torso, .tora-eye, .tora-ear, .tora-tail, .tora-wave, .tora-held, .tora-arm--cheer-l, .tora-arm--cheer-r, .sp, .tora--oops .tora-head { animation: none !important; }
}
</style>
