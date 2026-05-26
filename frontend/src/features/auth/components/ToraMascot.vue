<script setup lang="ts">
withDefaults(defineProps<{
  mood?: 'happy' | 'wave' | 'think' | 'cheer'
  holding?: 'none' | 'pencil' | 'key' | 'envelope' | 'check'
  size?: number
}>(), { mood: 'happy', holding: 'none', size: 220 })
</script>

<template>
  <div
    class="tora-wrapper"
    :style="{ width: `${size}px`, height: `${Math.round(size * (320 / 280))}px`, display: 'inline-block', position: 'relative' }"
  >
    <svg viewBox="0 0 280 320" width="100%" height="100%" style="overflow:visible">
      <!-- Ground shadow -->
      <ellipse cx="140" cy="305" rx="70" ry="6" fill="#1a1814" opacity="0.18" />

      <!-- TAIL — behind body -->
      <g class="tora-tail">
        <path d="M78 248 Q 50 240 38 215 Q 30 195 42 178 Q 55 165 65 175" stroke="#2a1f14" stroke-width="2.5" fill="#ffb84d" stroke-linejoin="round" />
        <path d="M62 232 Q 56 230 54 226" stroke="#2a1f14" stroke-width="3" fill="none" stroke-linecap="round" />
        <path d="M48 215 Q 44 213 43 208" stroke="#2a1f14" stroke-width="3" fill="none" stroke-linecap="round" />
        <path d="M42 195 Q 40 192 42 188" stroke="#2a1f14" stroke-width="3" fill="none" stroke-linecap="round" />
        <circle cx="58" cy="172" r="10" fill="#fffbed" stroke="#2a1f14" stroke-width="2.5" />
      </g>

      <!-- BODY -->
      <g class="tora-body">
        <!-- Legs / feet -->
        <ellipse cx="105" cy="298" rx="20" ry="9" fill="#2a1f14" />
        <ellipse cx="175" cy="298" rx="20" ry="9" fill="#2a1f14" />
        <ellipse cx="105" cy="295" rx="18" ry="7" fill="#ffb84d" stroke="#2a1f14" stroke-width="2" />
        <ellipse cx="175" cy="295" rx="18" ry="7" fill="#ffb84d" stroke="#2a1f14" stroke-width="2" />
        <ellipse cx="105" cy="296" rx="9" ry="3" fill="#ffd699" />
        <ellipse cx="175" cy="296" rx="9" ry="3" fill="#ffd699" />

        <!-- Torso -->
        <g class="tora-chest">
          <path d="M85 195 Q 80 240 90 280 Q 110 295 140 295 Q 170 295 190 280 Q 200 240 195 195 Z" fill="#ffb84d" stroke="#2a1f14" stroke-width="2.8" stroke-linejoin="round" />
          <ellipse cx="140" cy="250" rx="32" ry="40" fill="#ffd699" />
          <path d="M90 220 Q 95 222 100 220" stroke="#2a1f14" stroke-width="3" fill="none" stroke-linecap="round" />
          <path d="M88 245 Q 95 248 100 245" stroke="#2a1f14" stroke-width="3" fill="none" stroke-linecap="round" />
          <path d="M90 270 Q 96 272 102 270" stroke="#2a1f14" stroke-width="3" fill="none" stroke-linecap="round" />
          <path d="M180 220 Q 185 222 190 220" stroke="#2a1f14" stroke-width="3" fill="none" stroke-linecap="round" />
          <path d="M180 245 Q 185 248 190 245" stroke="#2a1f14" stroke-width="3" fill="none" stroke-linecap="round" />
          <path d="M178 270 Q 184 272 190 270" stroke="#2a1f14" stroke-width="3" fill="none" stroke-linecap="round" />
        </g>

        <!-- Left arm: think (hand up to chin) -->
        <g v-if="mood === 'think'">
          <path d="M80 200 Q 80 175 100 165" stroke="#2a1f14" stroke-width="2.5" fill="#ffb84d" />
          <ellipse cx="102" cy="163" rx="13" ry="14" fill="#ffb84d" stroke="#2a1f14" stroke-width="2.5" />
          <ellipse cx="102" cy="167" rx="6" ry="5" fill="#ffd699" />
        </g>
        <g v-else>
          <path d="M80 200 Q 60 220 62 245" stroke="#2a1f14" stroke-width="2.5" fill="#ffb84d" />
          <ellipse cx="60" cy="252" rx="13" ry="14" fill="#ffb84d" stroke="#2a1f14" stroke-width="2.5" />
          <ellipse cx="60" cy="256" rx="6" ry="5" fill="#ffd699" />
        </g>

        <!-- Right arm: wave -->
        <g v-if="mood === 'wave'" class="tora-wave">
          <path d="M200 195 Q 222 165 232 138" stroke="#2a1f14" stroke-width="2.5" fill="#ffb84d" />
          <ellipse cx="232" cy="135" rx="14" ry="16" fill="#ffb84d" stroke="#2a1f14" stroke-width="2.5" />
          <ellipse cx="232" cy="139" rx="6" ry="5" fill="#ffd699" />
          <circle cx="225" cy="128" r="2.5" fill="#2a1f14" />
          <circle cx="232" cy="125" r="2.5" fill="#2a1f14" />
          <circle cx="239" cy="128" r="2.5" fill="#2a1f14" />
        </g>
        <!-- Right arm: holding item -->
        <g v-else-if="holding !== 'none'">
          <path d="M200 200 Q 218 210 220 225" stroke="#2a1f14" stroke-width="2.5" fill="#ffb84d" />
          <ellipse cx="222" cy="230" rx="13" ry="14" fill="#ffb84d" stroke="#2a1f14" stroke-width="2.5" />
          <ellipse cx="222" cy="234" rx="6" ry="5" fill="#ffd699" />
        </g>
        <!-- Right arm: rest -->
        <g v-else>
          <path d="M200 200 Q 220 220 218 245" stroke="#2a1f14" stroke-width="2.5" fill="#ffb84d" />
          <ellipse cx="220" cy="252" rx="13" ry="14" fill="#ffb84d" stroke="#2a1f14" stroke-width="2.5" />
          <ellipse cx="220" cy="256" rx="6" ry="5" fill="#ffd699" />
        </g>

        <!-- Pencil -->
        <g v-if="holding === 'pencil'" class="tora-item">
          <rect x="218" y="195" width="9" height="42" rx="2" fill="#c44a1a" stroke="#2a1f14" stroke-width="2" />
          <path d="M218 195 L222.5 186 L227 195 Z" fill="#2a1f14" />
          <rect x="218" y="230" width="9" height="7" fill="#ffd333" stroke="#2a1f14" stroke-width="2" />
        </g>
        <!-- Key -->
        <g v-else-if="holding === 'key'" class="tora-item">
          <circle cx="222" cy="220" r="10" fill="#ffd333" stroke="#2a1f14" stroke-width="2.5" />
          <circle cx="222" cy="220" r="4" fill="#2a1f14" />
          <rect x="220" y="229" width="5" height="20" fill="#ffd333" stroke="#2a1f14" stroke-width="2" />
          <rect x="225" y="239" width="5" height="3" fill="#2a1f14" />
          <rect x="225" y="244" width="3.5" height="3" fill="#2a1f14" />
        </g>
        <!-- Check -->
        <g v-else-if="holding === 'check'" class="tora-item">
          <circle cx="222" cy="225" r="18" fill="#3a7d44" stroke="#2a1f14" stroke-width="2.5" />
          <path d="M212 225 L220 233 L233 218" stroke="#fffbed" stroke-width="4" fill="none" stroke-linecap="round" stroke-linejoin="round" />
        </g>

        <!-- HEAD GROUP -->
        <g :transform="mood === 'think' ? 'rotate(-8 140 155)' : ''">
          <!-- Ears — behind head -->
          <g class="tora-ear-l">
            <path d="M70 100 Q 75 70 95 75 Q 102 95 92 110 Z" fill="#ffb84d" stroke="#2a1f14" stroke-width="2.5" stroke-linejoin="round" />
            <path d="M78 95 Q 82 82 92 86 Q 95 96 88 102 Z" fill="#ff9aa8" />
          </g>
          <g class="tora-ear-r">
            <path d="M210 100 Q 205 70 185 75 Q 178 95 188 110 Z" fill="#ffb84d" stroke="#2a1f14" stroke-width="2.5" stroke-linejoin="round" />
            <path d="M202 95 Q 198 82 188 86 Q 185 96 192 102 Z" fill="#ff9aa8" />
          </g>

          <!-- Head -->
          <ellipse cx="140" cy="148" rx="68" ry="62" fill="#ffb84d" stroke="#2a1f14" stroke-width="3" stroke-linejoin="round" />

          <!-- Forehead stripes -->
          <path d="M118 92 Q 116 102 122 110" stroke="#2a1f14" stroke-width="4" fill="none" stroke-linecap="round" />
          <path d="M140 86 L 140 105" stroke="#2a1f14" stroke-width="4.5" fill="none" stroke-linecap="round" />
          <path d="M162 92 Q 164 102 158 110" stroke="#2a1f14" stroke-width="4" fill="none" stroke-linecap="round" />

          <!-- Side stripes -->
          <path d="M82 130 Q 76 138 80 148" stroke="#2a1f14" stroke-width="3.5" fill="none" stroke-linecap="round" />
          <path d="M82 160 Q 78 168 82 175" stroke="#2a1f14" stroke-width="3.5" fill="none" stroke-linecap="round" />
          <path d="M198 130 Q 204 138 200 148" stroke="#2a1f14" stroke-width="3.5" fill="none" stroke-linecap="round" />
          <path d="M198 160 Q 202 168 198 175" stroke="#2a1f14" stroke-width="3.5" fill="none" stroke-linecap="round" />

          <!-- Muzzle -->
          <ellipse cx="140" cy="172" rx="32" ry="22" fill="#ffd699" />

          <!-- Eyes -->
          <g class="tora-eyes">
            <ellipse cx="115" cy="145" rx="11" ry="13" fill="#fffbed" stroke="#2a1f14" stroke-width="2" />
            <ellipse cx="165" cy="145" rx="11" ry="13" fill="#fffbed" stroke="#2a1f14" stroke-width="2" />
            <ellipse cx="116" cy="146" rx="5" ry="8" fill="#2a1f14" />
            <ellipse cx="166" cy="146" rx="5" ry="8" fill="#2a1f14" />
            <circle cx="118" cy="142" r="2.5" fill="#fffbed" />
            <circle cx="168" cy="142" r="2.5" fill="#fffbed" />
            <circle cx="114" cy="148" r="1.2" fill="#fffbed" />
            <circle cx="164" cy="148" r="1.2" fill="#fffbed" />
          </g>

          <!-- Cheeks -->
          <ellipse cx="95" cy="165" rx="9" ry="6" fill="#ff9aa8" opacity="0.65" />
          <ellipse cx="185" cy="165" rx="9" ry="6" fill="#ff9aa8" opacity="0.65" />

          <!-- Nose -->
          <path d="M132 162 L148 162 L140 172 Z" fill="#1a1814" stroke="#2a1f14" stroke-width="1.5" stroke-linejoin="round" />
          <circle cx="137" cy="166" r="1.2" fill="#fffbed" opacity="0.7" />

          <!-- Mouth: cheer -->
          <g v-if="mood === 'cheer'">
            <path d="M115 175 Q 140 200 165 175 Q 140 188 115 175 Z" fill="#1a1814" stroke="#2a1f14" stroke-width="2" />
            <path d="M125 184 Q 140 191 155 184" stroke="#ff9aa8" stroke-width="3" fill="none" stroke-linecap="round" />
          </g>
          <!-- Mouth: think -->
          <path v-else-if="mood === 'think'" d="M125 178 L155 178" stroke="#1a1814" stroke-width="3.5" fill="none" stroke-linecap="round" />
          <!-- Mouth: happy / wave (default) -->
          <g v-else>
            <path d="M140 167 Q 140 175 130 178" stroke="#1a1814" stroke-width="3" fill="none" stroke-linecap="round" />
            <path d="M140 167 Q 140 175 150 178" stroke="#1a1814" stroke-width="3" fill="none" stroke-linecap="round" />
          </g>

          <!-- Whiskers -->
          <g class="tora-whisker">
            <path d="M108 175 Q 95 173 80 174" stroke="#2a1f14" stroke-width="1.5" fill="none" stroke-linecap="round" />
            <path d="M108 180 Q 92 182 78 184" stroke="#2a1f14" stroke-width="1.5" fill="none" stroke-linecap="round" />
            <path d="M108 185 Q 95 190 84 194" stroke="#2a1f14" stroke-width="1.5" fill="none" stroke-linecap="round" />
            <path d="M172 175 Q 185 173 200 174" stroke="#2a1f14" stroke-width="1.5" fill="none" stroke-linecap="round" />
            <path d="M172 180 Q 188 182 202 184" stroke="#2a1f14" stroke-width="1.5" fill="none" stroke-linecap="round" />
            <path d="M172 185 Q 185 190 196 194" stroke="#2a1f14" stroke-width="1.5" fill="none" stroke-linecap="round" />
          </g>

          <!-- Thinking bubble -->
          <g v-if="mood === 'think'">
            <circle cx="68" cy="92" r="4" fill="#fffbed" stroke="#2a1f14" stroke-width="1.5" />
            <circle cx="55" cy="78" r="6" fill="#fffbed" stroke="#2a1f14" stroke-width="1.5" />
            <circle cx="35" cy="58" r="18" fill="#fffbed" stroke="#2a1f14" stroke-width="2" />
            <text x="35" y="65" text-anchor="middle" font-family="Iowan Old Style, serif" font-style="italic" font-weight="700" font-size="20" fill="#2a1f14">?</text>
          </g>
        </g>
      </g>
    </svg>
  </div>
</template>

<style scoped>
.tora-body, .tora-chest, .tora-eyes,
.tora-ear-l, .tora-ear-r, .tora-tail,
.tora-wave, .tora-item, .tora-whisker {
  transform-box: view-box;
}
.tora-body  { animation: tora-bob 4s ease-in-out infinite; }
.tora-chest { animation: tora-breathe 3.4s ease-in-out infinite; transform-origin: 140px 245px; }
.tora-eyes  { animation: tora-blink 5.2s ease-in-out infinite; transform-origin: 140px 145px; }
.tora-ear-l { animation: tora-ear-twitch-l 6s ease-in-out infinite; transform-origin: 88px 100px; }
.tora-ear-r { animation: tora-ear-twitch-r 7s ease-in-out infinite 1.5s; transform-origin: 192px 100px; }
.tora-tail  { animation: tora-wag 1.8s ease-in-out infinite; transform-origin: 78px 248px; }
.tora-wave  { animation: tora-wave-arm 1.4s ease-in-out infinite; transform-origin: 200px 195px; }
.tora-item  { animation: tora-itembob 2.6s ease-in-out infinite; transform-origin: 222px 230px; }
.tora-whisker { animation: tora-whisker-drift 4.8s ease-in-out infinite; }

@keyframes tora-bob {
  0%, 100% { transform: translateY(0); }
  50%      { transform: translateY(-4px); }
}
@keyframes tora-breathe {
  0%, 100% { transform: scale(1, 1); }
  50%      { transform: scale(1.03, 1.02); }
}
@keyframes tora-blink {
  0%, 90%, 100% { transform: scaleY(1); }
  93%, 97%      { transform: scaleY(0.1); }
}
@keyframes tora-ear-twitch-l {
  0%, 22%, 30%, 100% { transform: rotate(0deg); }
  25%, 28%           { transform: rotate(-12deg); }
}
@keyframes tora-ear-twitch-r {
  0%, 40%, 48%, 100% { transform: rotate(0deg); }
  42%, 46%           { transform: rotate(12deg); }
}
@keyframes tora-wag {
  0%, 100% { transform: rotate(-10deg); }
  50%      { transform: rotate(18deg); }
}
@keyframes tora-wave-arm {
  0%, 100% { transform: rotate(-12deg); }
  50%      { transform: rotate(22deg); }
}
@keyframes tora-itembob {
  0%, 100% { transform: translateY(0) rotate(-3deg); }
  50%      { transform: translateY(-3px) rotate(3deg); }
}
@keyframes tora-whisker-drift {
  0%, 100% { transform: translateY(0); }
  50%      { transform: translateY(1px); }
}

@media (prefers-reduced-motion: reduce) {
  .tora-body, .tora-chest, .tora-eyes, .tora-ear-l, .tora-ear-r,
  .tora-tail, .tora-wave, .tora-item, .tora-whisker {
    animation: none !important;
  }
}
</style>
