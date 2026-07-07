<script setup lang="ts">
import ToraMascot from './ToraMascot.vue'

withDefaults(defineProps<{
  bubble: string
  mood?: 'happy' | 'wave' | 'think' | 'cheer'
  holding?: 'none' | 'pencil' | 'key' | 'envelope' | 'check'
  wide?: boolean
  medium?: boolean
  tight?: boolean
}>(), { mood: 'happy', holding: 'none', wide: false, medium: false, tight: false })
</script>

<template>
  <div class="relative min-h-screen overflow-hidden bg-lm-bg">
    <!-- Dot grid -->
    <div class="pointer-events-none absolute inset-0 bg-dot-grid opacity-50" />

    <!-- Floating math glyphs -->
    <div class="pointer-events-none absolute inset-0 overflow-hidden">
      <span class="glyph" style="left:8%;top:14%;font-size:56px;color:rgba(196,74,26,0.18);transform:rotate(-8deg);animation-delay:0s">x²</span>
      <span class="glyph" style="left:88%;top:10%;font-size:70px;color:rgba(26,24,20,0.12);transform:rotate(12deg);animation-delay:0.8s">√</span>
      <span class="glyph" style="left:5%;top:78%;font-size:84px;color:rgba(255,211,51,0.45);transform:rotate(-6deg);animation-delay:1.4s">π</span>
      <span class="glyph" style="left:92%;top:72%;font-size:96px;color:rgba(26,24,20,0.10);transform:rotate(8deg);animation-delay:2.1s">∫</span>
      <span class="glyph" style="left:14%;top:44%;font-size:42px;color:rgba(196,74,26,0.14);transform:rotate(14deg);animation-delay:0.4s">∑</span>
      <span class="glyph" style="left:84%;top:40%;font-size:38px;color:rgba(26,24,20,0.12);transform:rotate(-10deg);animation-delay:1.6s">sin</span>
      <span class="glyph" style="left:50%;top:6%;font-size:50px;color:rgba(255,211,51,0.5);transform:rotate(4deg);animation-delay:1.2s">∞</span>
      <span class="glyph" style="left:50%;top:92%;font-size:44px;color:rgba(26,24,20,0.10);transform:rotate(-3deg);animation-delay:2.4s">Δ</span>
      <span class="glyph" style="left:23%;top:88%;font-size:36px;color:rgba(196,74,26,0.18);transform:rotate(12deg);animation-delay:0.6s">÷</span>
      <span class="glyph" style="left:78%;top:88%;font-size:38px;color:rgba(26,24,20,0.13);transform:rotate(-8deg);animation-delay:1.9s">∠</span>
    </div>

    <!-- Brand — top left -->
    <div class="absolute left-8 top-6 z-10 flex items-center -gap-1">
      <img src="@/assets/Logo.png" alt="LifeOS" class="h-25" />
      <span class="font-display text-[20px] font-bold text-lm-ink">LifeOS</span>
    </div>

    <!-- Main content -->
    <div :class="tight ? 'py-6' : 'py-20'" class="relative z-10 flex min-h-screen items-center justify-center px-8">
      <div class="flex items-center gap-8">
        <!-- Mascot column -->
        <div class="flex flex-col items-center gap-0 pb-2">
          <!-- Speech bubble -->
          <div class="relative translate-y-4 -rotate-[1.5deg] rounded-[14px] border-2 border-lm-line bg-lm-surface px-4 py-2.5 font-display text-[14px] font-bold whitespace-nowrap shadow-stamp-sm text-lm-ink">
            {{ bubble }}
            <!-- Bubble tail -->
            <svg width="22" height="20" viewBox="0 0 22 20" class="absolute -bottom-[17px] left-7">
              <path d="M2 2 L 18 2 L 8 18 Z" fill="white" stroke="#1a1814" stroke-width="2" stroke-linejoin="round" />
              <path d="M3 2 L 17 2" stroke="white" stroke-width="3" />
            </svg>
          </div>
          <ToraMascot :mood="mood" :holding="holding" :size="260" />
        </div>

        <!-- Form column -->
        <div>
          <div :class="wide ? 'w-[560px]' : medium ? 'w-[500px]' : 'w-[440px]'" class="rounded-[24px] border-[2.5px] border-lm-line bg-lm-surface p-8 shadow-stamp-lg">
            <slot />
          </div>
          <div class="mt-[18px] text-center text-[13px] text-lm-ink-2">
            <slot name="footer" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.glyph {
  position: absolute;
  font-family: 'Iowan Old Style', 'Cambria', 'Times New Roman', serif;
  font-style: italic;
  font-weight: 700;
  line-height: 1;
  transform-origin: center center;
  animation: glyph-float 6s ease-in-out infinite;
}

@keyframes glyph-float {
  0%, 100% { translate: 0 0; }
  50%       { translate: 0 -14px; }
}
</style>
