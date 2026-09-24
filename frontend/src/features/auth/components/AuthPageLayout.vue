<script setup lang="ts">
import LogoMark from '@/components/brand/LogoMark.vue'
import ToraMascot, { type ToraHolding, type ToraMood } from '@/components/tora/ToraMascot.vue'

withDefaults(defineProps<{
  bubble: string
  mood?: ToraMood
  holding?: ToraHolding
  cover?: boolean
  wide?: boolean
  medium?: boolean
  tight?: boolean
}>(), { mood: 'happy', holding: 'none', cover: false, wide: false, medium: false, tight: false })

const GLYPHS = [
  { g: 'x²', l: '9%', t: '16%', s: 60, d: 0 },
  { g: '√', l: '82%', t: '11%', s: 74, d: 0.8 },
  { g: 'π', l: '8%', t: '74%', s: 80, d: 1.4 },
  { g: '∫', l: '84%', t: '70%', s: 92, d: 2.1 },
  { g: '∑', l: '16%', t: '46%', s: 44, d: 0.4 },
  { g: 'sin', l: '78%', t: '42%', s: 40, d: 1.6 },
  { g: '∞', l: '48%', t: '7%', s: 52, d: 1.2 },
  { g: 'Δ', l: '56%', t: '90%', s: 46, d: 2.4 },
]
</script>

<template>
  <div class="min-h-screen bg-white font-body lg:flex">
    <!-- Brand panel -->
    <aside class="relative hidden overflow-hidden bg-lx-macaw lg:flex lg:w-[46%] lg:flex-col">
      <div class="pointer-events-none absolute -left-32 -top-32 h-[420px] w-[420px] rounded-full bg-white/10" />
      <div class="pointer-events-none absolute -bottom-40 -right-24 h-[460px] w-[460px] rounded-full bg-white/10" />

      <span
        v-for="item in GLYPHS"
        :key="item.g"
        class="auth-glyph pointer-events-none absolute font-math font-bold italic text-white/20"
        :style="{ left: item.l, top: item.t, fontSize: `${item.s}px`, animationDelay: `${item.d}s` }"
      >{{ item.g }}</span>

      <div class="relative z-10 flex items-center gap-2.5 px-10 pt-9">
        <LogoMark :size="44" />
        <span class="font-display text-[22px] font-bold tracking-tight text-white">LifeOS</span>
      </div>

      <div class="relative z-10 flex flex-1 flex-col items-center justify-center pb-10">
        <div :key="bubble" class="auth-bubble relative mb-1 rounded-[20px] bg-white px-5 py-3 font-display text-[16px] font-semibold whitespace-nowrap text-lx-ink shadow-[0_10px_24px_-10px_rgba(0,0,0,0.3)]">
          {{ bubble }}
          <span class="absolute -bottom-2 left-1/2 h-4 w-4 -translate-x-1/2 rotate-45 rounded-[3px] bg-white" />
        </div>
        <ToraMascot :mood="mood" :holding="holding" :cover="cover" :size="340" />
      </div>
    </aside>

    <!-- Form panel -->
    <main :class="tight ? 'py-8' : 'py-14'" class="flex flex-1 items-center justify-center px-6">
      <div :class="wide ? 'max-w-[500px]' : medium ? 'max-w-[440px]' : 'max-w-[400px]'" class="w-full">
        <div class="mb-8 flex items-center gap-2.5 lg:hidden">
          <LogoMark :size="38" />
          <span class="font-display text-[20px] font-bold tracking-tight text-lx-ink">LifeOS</span>
        </div>
        <slot />
        <div class="mt-6 text-center text-[14px] text-lx-ink-soft">
          <slot name="footer" />
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
.auth-glyph { animation: auth-float 6s ease-in-out infinite; line-height: 1; }
.auth-bubble { animation: auth-pop 0.45s cubic-bezier(0.34, 1.56, 0.64, 1) both; }
@keyframes auth-float { 0%, 100% { translate: 0 0; } 50% { translate: 0 -14px; } }
@keyframes auth-pop { from { opacity: 0; transform: translateY(8px) scale(0.92); } to { opacity: 1; transform: none; } }
@media (prefers-reduced-motion: reduce) { .auth-glyph, .auth-bubble { animation: none; } }
</style>
