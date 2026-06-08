<template>
  <div class="flex flex-col gap-4">
    <!-- Preset cards -->
    <div>
      <p class="font-mono text-[10px] font-bold tracking-[0.12em] uppercase text-lm-ink-3 m-0 mb-2">Quick start</p>
      <div class="grid grid-cols-4 gap-2 mt-2">
        <button
          v-for="p in VL_PRESETS"
          :key="p.id"
          @click="applyPreset(p)"
          class="flex flex-col items-center gap-1 p-[10px_6px] border-2 border-lm-line-soft rounded-[12px] bg-lm-surface cursor-pointer transition-all duration-150"
          :class="{ 'border-lm-line bg-lm-yellow shadow-stamp-sm': activePreset === p.id }"
        >
          <!-- SVG icon representation -->
          <svg v-if="p.id === 'venn-2'" viewBox="0 0 80 48" width="80" height="48" class="block max-w-full">
            <circle cx="28" cy="24" r="17" fill="#ffd333" fill-opacity=".35" stroke="#ffd333" stroke-width="1.5"/>
            <circle cx="52" cy="24" r="17" fill="#8fb3ff" fill-opacity=".35" stroke="#8fb3ff" stroke-width="1.5"/>
          </svg>
          <svg v-else-if="p.id === 'venn-3'" viewBox="0 0 80 56" width="80" height="56" class="block max-w-full">
            <circle cx="30" cy="22" r="15" fill="#ffd333" fill-opacity=".35" stroke="#ffd333" stroke-width="1.5"/>
            <circle cx="50" cy="22" r="15" fill="#8fb3ff" fill-opacity=".35" stroke="#8fb3ff" stroke-width="1.5"/>
            <circle cx="40" cy="38" r="15" fill="#8fe0aa" fill-opacity=".35" stroke="#8fe0aa" stroke-width="1.5"/>
          </svg>
          <svg v-else-if="p.id === 'hotspot'" viewBox="0 0 80 48" width="80" height="48" class="block max-w-full">
            <rect x="4" y="12" width="20" height="16" rx="3" fill="#ffd333" fill-opacity=".35" stroke="#ffd333" stroke-width="1.5"/>
            <rect x="30" y="8" width="20" height="24" rx="3" fill="#8fb3ff" fill-opacity=".35" stroke="#8fb3ff" stroke-width="1.5"/>
            <rect x="56" y="12" width="20" height="16" rx="3" fill="#8fe0aa" fill-opacity=".35" stroke="#8fe0aa" stroke-width="1.5"/>
            <line x1="24" y1="20" x2="30" y2="20" stroke="var(--lm-ink-3)" stroke-width="1.5" stroke-dasharray="2 2"/>
            <line x1="50" y1="20" x2="56" y2="20" stroke="var(--lm-ink-3)" stroke-width="1.5" stroke-dasharray="2 2"/>
          </svg>
          <svg v-else viewBox="0 0 80 48" width="80" height="48" class="block max-w-full">
            <rect x="8" y="6" width="64" height="36" rx="6" fill="none" stroke="var(--lm-line-soft)" stroke-width="1.5" stroke-dasharray="4 3"/>
            <text x="40" y="28" text-anchor="middle" style="font-size: 10px; fill: var(--lm-ink-3); font-weight: 600;">Empty</text>
          </svg>
          <span class="font-display text-[11px] font-bold text-lm-ink">{{ p.label }}</span>
          <span class="font-mono text-[9px] text-lm-ink-3 text-center">{{ p.desc }}</span>
        </button>
      </div>
    </div>

    <!-- Drawing tools -->
    <div class="flex items-center gap-1 p-[6px_8px] bg-lm-bg-soft rounded-[10px] border-2 border-lm-line-soft">
      <p class="font-mono text-[10px] font-bold tracking-[0.12em] uppercase text-lm-ink-3 m-0 mr-1.5 tracking-wider mb-0">Tools</p>
      <button
        v-for="[t, l] in [['select', 'Select'], ['zone-circle', '○ Circle'], ['zone-rect', '□ Rect'], ['button', 'Button'], ['hotspot', 'Hotspot']]"
        :key="t"
        @click="tool = t"
        class="h-7 px-2.5 rounded-[7px] border-2 border-transparent bg-transparent font-display text-[11px] font-bold text-lm-ink cursor-pointer transition-all duration-100"
        :class="{ 'border-lm-line bg-lm-yellow shadow-stamp-sm': tool === t }"
      >
        {{ l }}
      </button>
    </div>

    <!-- Canvas + Properties (2-col) -->
    <div class="grid grid-cols-1 gap-3" :class="{ 'grid-cols-[1fr_220px]': selectedZone || selectedElement }">
      <!-- Canvas -->
      <div class="border-2 border-lm-line rounded-[14px] overflow-hidden bg-[#fffdf8] shadow-stamp-sm">
        <svg
          viewBox="0 0 900 520"
          class="block w-full h-auto"
          :style="{ cursor: tool !== 'select' ? 'crosshair' : 'default' }"
          @click="onCanvasClick"
        >
          <!-- Grid background -->
          <defs>
            <pattern id="vl-dots" width="20" height="20" patternUnits="userSpaceOnUse">
              <circle cx="10" cy="10" r="1.2" fill="rgba(26,24,20,0.08)" />
            </pattern>
          </defs>
          <rect width="900" height="520" fill="url(#vl-dots)" />

          <!-- Zones -->
          <g
            v-for="z in zones"
            :key="z.id"
            @click.stop="selectZone(z.id)"
            class="cursor-pointer"
          >
            <ellipse
              v-if="z.shape === 'circle'"
              :cx="z.x + z.width / 2"
              :cy="z.y + z.height / 2"
              :rx="z.width / 2"
              :ry="z.height / 2"
              :fill="highlightZone === z.id ? (z.highlightColor || z.color) : z.color"
              :fill-opacity="highlightZone === z.id ? (z.highlightOpacity || 0.82) : 0.28"
              :stroke="selectedId === z.id && selectedKind === 'zone' ? 'var(--lm-ink)' : z.color"
              :stroke-width="selectedId === z.id && selectedKind === 'zone' ? 3 : 2.5"
              :stroke-dasharray="selectedId === z.id && selectedKind === 'zone' ? '8 4' : 'none'"
            />
            <rect
              v-else
              :x="z.x"
              :y="z.y"
              :width="z.width"
              :height="z.height"
              :rx="10"
              :fill="highlightZone === z.id ? (z.highlightColor || z.color) : z.color"
              :fill-opacity="highlightZone === z.id ? (z.highlightOpacity || 0.82) : 0.28"
              :stroke="selectedId === z.id && selectedKind === 'zone' ? 'var(--lm-ink)' : z.color"
              :stroke-width="selectedId === z.id && selectedKind === 'zone' ? 3 : 2.5"
              :stroke-dasharray="selectedId === z.id && selectedKind === 'zone' ? '8 4' : 'none'"
            />
            <text
              :x="z.x + z.width / 2"
              :y="z.y + z.height / 2"
              text-anchor="middle"
              dominant-baseline="central"
              class="font-display font-[800] fill-lm-ink pointer-events-none select-none"
              :style="{ fontSize: z.shape === 'circle' ? '26px' : '18px' }"
            >
              {{ z.label }}
            </text>
          </g>

          <!-- Elements -->
          <g
            v-for="el in elements"
            :key="el.id"
            @click.stop="onElementClick(el)"
            class="cursor-pointer"
          >
            <rect
              :x="el.x"
              :y="el.y"
              :width="el.width"
              :height="el.height"
              :rx="el.kind === 'hotspot' ? 999 : 10"
              fill="var(--lm-surface)"
              :stroke="selectedId === el.id && selectedKind === 'element' ? 'var(--lm-ink)' : 'var(--lm-line)'"
              :stroke-width="2"
            />
            <text
              :x="el.x + el.width / 2"
              :y="el.y + el.height / 2"
              text-anchor="middle"
              dominant-baseline="central"
              class="font-display text-[13px] font-bold fill-lm-ink pointer-events-none"
            >
              {{ el.label }}
            </text>
          </g>

          <!-- Overlap badges -->
          <g
            v-for="r in exactRegions"
            :key="r.id"
            :transform="`translate(${r.center.x}, ${r.center.y})`"
          >
            <rect
              x="-26"
              y="-16"
              width="52"
              height="32"
              rx="9"
              fill="var(--lm-surface)"
              :stroke="r.value < 0 ? 'var(--lm-red)' : 'var(--lm-line)'"
              stroke-width="2"
              class="[filter:drop-shadow(1px_2px_0_rgba(26,24,20,0.15))]"
            />
            <text
              text-anchor="middle"
              dominant-baseline="central"
              class="font-display text-[15px] font-[800] fill-lm-ink"
              :class="{ 'fill-lm-red': r.value < 0 }"
            >
              {{ r.value }}
            </text>
          </g>

          <!-- Empty state -->
          <text
            v-if="zones.length === 0"
            x="450"
            y="260"
            text-anchor="middle"
            dominant-baseline="central"
            class="font-mono text-[13px] fill-lm-ink-3 tracking-[0.06em]"
          >
            Select a preset or use tools to draw zones
          </text>
        </svg>
      </div>

      <!-- Properties Panel -->
      <div v-if="selectedZone || selectedElement" class="border-2 border-lm-line-soft rounded-[14px] bg-lm-surface p-3.5 flex flex-col gap-2.5 overflow-auto max-h-[520px]">
        <p class="font-mono text-[10px] font-bold tracking-[0.12em] uppercase text-lm-ink-3 m-0 color-lm-ink tracking-wider mb-0">Properties</p>

        <!-- Zone Props -->
        <div v-if="selectedZone" class="flex flex-col gap-2.5">
          <div class="flex items-center gap-2 mb-[2px]">
            <div class="w-3.5 h-3.5 rounded-full border-2 border-lm-line shrink-0" :style="{ background: selectedZone.color }" />
            <span class="font-display text-[13px] font-bold text-lm-ink">{{ selectedZone.label }}</span>
            <span class="font-mono text-[9px] text-lm-ink-3">{{ selectedZone.shape }}</span>
          </div>
          
          <div>
            <p class="font-mono text-[9px] font-bold tracking-[0.12em] uppercase text-lm-ink-3 m-0 mb-1">Label</p>
            <input v-model="selectedZone.label" @input="onZoneChange" class="w-full box-border font-display text-[12px] px-2.5 py-1.75 border-2 border-lm-line-soft rounded-[8px] bg-lm-bg-soft outline-none text-lm-ink" />
          </div>

          <div>
            <p class="font-mono text-[9px] font-bold tracking-[0.12em] uppercase text-lm-ink-3 m-0 mb-1">Color</p>
            <div class="flex gap-1.5">
              <button
                v-for="c in VL_COLORS"
                :key="c"
                @click="updateSelectedZoneColor(c)"
                class="w-6 h-6 rounded-full border-2 border-lm-line-soft cursor-pointer"
                :style="{ background: c }"
                :class="{ 'border-3 border-lm-ink': selectedZone.color === c }"
              />
            </div>
          </div>

          <div class="grid grid-cols-2 gap-1.5">
            <div>
              <p class="font-mono text-[9px] font-bold tracking-[0.12em] uppercase text-lm-ink-3 m-0 mb-1">X</p>
              <input type="number" v-model.number="selectedZone.x" @input="onZoneChange" class="w-full box-border font-display text-[12px] px-2.5 py-1.75 border-2 border-lm-line-soft rounded-[8px] bg-lm-bg-soft outline-none text-lm-ink" />
            </div>
            <div>
              <p class="font-mono text-[9px] font-bold tracking-[0.12em] uppercase text-lm-ink-3 m-0 mb-1">Y</p>
              <input type="number" v-model.number="selectedZone.y" @input="onZoneChange" class="w-full box-border font-display text-[12px] px-2.5 py-1.75 border-2 border-lm-line-soft rounded-[8px] bg-lm-bg-soft outline-none text-lm-ink" />
            </div>
            <div>
              <p class="font-mono text-[9px] font-bold tracking-[0.12em] uppercase text-lm-ink-3 m-0 mb-1">Width</p>
              <input type="number" v-model.number="selectedZone.width" @input="onZoneChange" class="w-full box-border font-display text-[12px] px-2.5 py-1.75 border-2 border-lm-line-soft rounded-[8px] bg-lm-bg-soft outline-none text-lm-ink" />
            </div>
            <div>
              <p class="font-mono text-[9px] font-bold tracking-[0.12em] uppercase text-lm-ink-3 m-0 mb-1">Height</p>
              <input type="number" v-model.number="selectedZone.height" @input="onZoneChange" class="w-full box-border font-display text-[12px] px-2.5 py-1.75 border-2 border-lm-line-soft rounded-[8px] bg-lm-bg-soft outline-none text-lm-ink" />
            </div>
          </div>

          <div>
            <p class="font-mono text-[9px] font-bold tracking-[0.12em] uppercase text-lm-ink-3 m-0 mb-1">Shape</p>
            <select v-model="selectedZone.shape" @change="onZoneChange" class="w-full box-border font-display text-[12px] px-2.5 py-1.75 border-2 border-lm-line-soft rounded-[8px] bg-lm-bg-soft outline-none text-lm-ink">
              <option value="circle">Circle</option>
              <option value="rectangle">Rectangle</option>
            </select>
          </div>

          <div>
            <p class="font-mono text-[9px] font-bold tracking-[0.12em] uppercase text-lm-ink-3 m-0 mb-1">Highlight opacity</p>
            <div class="flex items-center gap-2">
              <input
                type="range"
                min="0"
                max="1"
                step="0.05"
                v-model.number="selectedZone.highlightOpacity"
                @input="onZoneChange"
                class="flex-1"
              />
              <span class="font-mono text-[10px] text-lm-ink-3 min-w-[28px]">{{ selectedZone.highlightOpacity ?? 0.82 }}</span>
            </div>
          </div>

          <div>
            <p class="font-mono text-[9px] font-bold tracking-[0.12em] uppercase text-lm-ink-3 m-0 mb-1">Zone feedback</p>
            <textarea
              v-model="selectedZone.feedback"
              @input="onZoneChange"
              rows="2"
              placeholder="Shown when zone is highlighted…"
              class="w-full box-border font-display text-[12px] px-2.5 py-1.75 border-2 border-lm-line-soft rounded-[8px] bg-lm-bg-soft outline-none text-lm-ink resize-y"
            />
          </div>

          <button @click="deleteZone(selectedZone.id)" class="font-display text-[11px] font-bold border-2 border-lm-red rounded-[8px] bg-lm-red-soft p-[4px_10px] cursor-pointer text-lm-red mt-1">
            <span class="flex items-center gap-1.5">
              <AdminIcon name="trash" :size="12" /> Delete zone
            </span>
          </button>
        </div>

        <!-- Element Props -->
        <div v-else-if="selectedElement" class="flex flex-col gap-2.5">
          <span class="font-display text-[13px] font-bold text-lm-ink">{{ selectedElement.label }}</span>
          <span class="font-mono text-[9px] text-lm-ink-3">{{ selectedElement.kind }}</span>

          <div>
            <p class="font-mono text-[9px] font-bold tracking-[0.12em] uppercase text-lm-ink-3 m-0 mb-1">Label</p>
            <input v-model="selectedElement.label" @input="onElementChange" class="w-full box-border font-display text-[12px] px-2.5 py-1.75 border-2 border-lm-line-soft rounded-[8px] bg-lm-bg-soft outline-none text-lm-ink" />
          </div>

          <div v-if="selectedElementInteraction" class="flex flex-col gap-2.5">
            <div>
              <p class="font-mono text-[9px] font-bold tracking-[0.12em] uppercase text-lm-ink-3 m-0 mb-1">On click → Highlight</p>
              <select v-model="selectedElementInteraction.targetZoneId" @change="onInteractionChange" class="w-full box-border font-display text-[12px] px-2.5 py-1.75 border-2 border-lm-line-soft rounded-[8px] bg-lm-bg-soft outline-none text-lm-ink">
                <option v-for="z in zones" :key="z.id" :value="z.id">{{ z.label }}</option>
              </select>
            </div>

            <div>
              <p class="font-mono text-[9px] font-bold tracking-[0.12em] uppercase text-lm-ink-3 m-0 mb-1">Feedback</p>
              <textarea
                v-model="selectedElementInteraction.feedback"
                @input="onInteractionChange"
                rows="2"
                class="w-full box-border font-display text-[12px] px-2.5 py-1.75 border-2 border-lm-line-soft rounded-[8px] bg-lm-bg-soft outline-none text-lm-ink resize-y"
              />
            </div>
          </div>

          <button @click="deleteElement(selectedElement.id)" class="font-display text-[11px] font-bold border-2 border-lm-red rounded-[8px] bg-lm-red-soft p-[4px_10px] cursor-pointer text-lm-red mt-1">
            <span class="flex items-center gap-1.5">
              <AdminIcon name="trash" :size="12" /> Delete trigger
            </span>
          </button>
        </div>
      </div>
    </div>

    <!-- Overlap values section -->
    <div v-if="overlap?.enabled && overlap.inputs?.length > 0" class="border-2 border-lm-line-soft rounded-[14px] bg-[#fffdf8] overflow-hidden">
      <div class="flex items-center justify-between p-[10px_14px] border-b-2 border-lm-line-soft bg-lm-bg-soft">
        <div class="flex items-center gap-2">
          <span class="w-[22px] h-[22px] rounded-[7px] bg-lm-purple-soft border-2 border-lm-line grid place-items-center font-math text-[13px] font-bold italic text-lm-purple">∩</span>
          <p class="font-mono text-[10px] font-bold tracking-[0.12em] uppercase text-lm-ink-3 m-0 mb-0">Overlap values</p>
        </div>
        <span class="font-mono text-[9px] text-lm-ink-3">Inclusion-exclusion</span>
      </div>

      <div class="p-3.5">
        <div class="flex flex-wrap gap-2.5 mb-3.5">
          <div
            v-for="inp_item in overlap.inputs"
            :key="inp_item.id"
            class="flex flex-col gap-1 min-w-[100px]"
          >
            <p class="font-math text-[12px] font-bold italic text-lm-ink-2 m-0">{{ inp_item.label }}</p>
            <input
              type="number"
              v-model.number="inp_item.value"
              @input="onOverlapInputChange"
              class="w-[80px] font-display text-[14px] font-bold p-[6px_10px] border-2 border-lm-line rounded-[8px] bg-lm-surface outline-none text-lm-ink shadow-stamp-sm text-center"
            />
            <span class="font-mono text-[8px] text-lm-ink-3 uppercase">{{ inp_item.kind }}</span>
          </div>
        </div>

        <!-- Computed exact regions -->
        <div v-if="exactRegions.length > 0" class="mt-3.5">
          <div class="flex items-center gap-2.5 mb-2.5">
            <p class="font-mono text-[10px] font-bold tracking-[0.12em] uppercase text-lm-ink-3 m-0 text-[9px] mb-0">Exact regions</p>
            <div class="flex-1 border-t border-lm-line-soft" />
          </div>

          <div class="flex flex-wrap gap-2">
            <div
              v-for="r in exactRegions"
              :key="r.id"
              class="flex items-center gap-2 p-[6px_12px] border-2 border-lm-line-soft rounded-[10px] bg-lm-bg-soft"
              :class="{ 'border-lm-red bg-lm-red-soft': r.value < 0 }"
            >
              <!-- Zone dots -->
              <div class="flex">
                <div
                  v-for="(zid, i) in r.zoneIds"
                  :key="zid"
                  class="w-2.5 h-2.5 rounded-full border-[1.5px] border-lm-line"
                  :style="{
                    background: zones.find(z => z.id === zid)?.color || '#ccc',
                    marginLeft: i > 0 ? '-3px' : '0px',
                    zIndex: r.zoneIds.length - i
                  }"
                />
              </div>
              <span class="font-display text-[11px] font-semibold text-lm-ink-2">{{ r.label }}</span>
              <span class="font-display text-[14px] font-[800] text-lm-ink min-w-[20px] text-right" :class="{ 'text-lm-red': r.value < 0 }">
                {{ r.value }}
              </span>
            </div>
          </div>

          <p v-if="exactRegions.some(r => r.value < 0)" class="font-display text-[11px] font-semibold text-lm-red mt-2 flex items-center gap-1.5 text-left">
            <span class="w-3.5 h-3.5 rounded-full bg-lm-red text-white grid place-items-center text-[9px] font-[800] shrink-0">!</span>
            Some regions are negative — check the totals and intersections.
          </p>
        </div>
      </div>
    </div>

    <!-- Zones list -->
    <div>
      <div class="flex items-center justify-between mb-2">
        <p class="font-mono text-[10px] font-bold tracking-[0.12em] uppercase text-lm-ink-3 m-0">Zones ({{ zones.length }})</p>
        <button @click="addZone" class="font-display text-[11px] font-bold border-2 border-lm-line rounded-[8px] bg-lm-surface px-2.5 py-1 cursor-pointer text-lm-ink">+ Add zone</button>
      </div>

      <div v-if="zones.length === 0" class="text-[12px] text-lm-ink-3 m-0 text-left">
        No zones. Select a preset or add manually.
      </div>
      <div v-else class="flex flex-col gap-1">
        <div
          v-for="z in zones"
          :key="z.id"
          @click="selectZone(z.id)"
          class="flex items-center gap-2 p-[8px_12px] w-full box-border text-left border-2 border-lm-line-soft rounded-[10px] cursor-pointer transition-all duration-120 bg-lm-surface"
          :class="{ 'border-lm-line bg-lm-yellow shadow-stamp-sm': selectedId === z.id && selectedKind === 'zone' }"
        >
          <div class="w-3.5 h-3.5 rounded-full border-2 border-lm-line shrink-0" :style="{ background: z.color }" />
          <span class="flex-1 font-display text-[12px] font-bold text-lm-ink">{{ z.label }}</span>
          <span class="font-mono text-[9px] text-lm-ink-3">{{ z.shape }} {{ z.width }}×{{ z.height }}</span>
          <button @click.stop="deleteZone(z.id)" class="grid place-items-center w-6 h-6 rounded-[6px] border border-lm-line-soft bg-transparent cursor-pointer text-lm-ink-3 shrink-0">
            <AdminIcon name="trash" :size="11" />
          </button>
        </div>
      </div>
    </div>

    <!-- Elements list -->
    <div v-if="elements.length > 0">
      <div class="flex items-center justify-between mb-2">
        <p class="font-mono text-[10px] font-bold tracking-[0.12em] uppercase text-lm-ink-3 m-0">Triggers ({{ elements.length }})</p>
      </div>
      <div class="flex flex-col gap-1">
        <div
          v-for="el in elements"
          :key="el.id"
          @click="onElementListClick(el)"
          class="flex items-center gap-2 p-[8px_12px] w-full box-border text-left border-2 border-lm-line-soft rounded-[10px] cursor-pointer transition-all duration-120 bg-lm-surface"
          :class="{ 'border-lm-line bg-lm-yellow shadow-stamp-sm': selectedId === el.id && selectedKind === 'element' }"
        >
          <span class="w-[22px] h-[22px] rounded-[6px] bg-lm-bg-soft border-2 border-lm-line-soft grid place-items-center font-mono text-[8px] text-lm-ink-3 shrink-0">▸</span>
          <span class="flex-1 font-display text-[12px] font-bold text-lm-ink">{{ el.label }}</span>
          <span v-if="getInteraction(el.id)" class="font-mono text-[9px] text-lm-ink-3">
            → {{ zones.find(z => z.id === getInteraction(el.id).targetZoneId)?.label || '?' }}
          </span>
          <button @click.stop="deleteElement(el.id)" class="grid place-items-center w-6 h-6 rounded-[6px] border border-lm-line-soft bg-transparent cursor-pointer text-lm-ink-3 shrink-0">
            <AdminIcon name="trash" :size="11" />
          </button>
        </div>
      </div>
    </div>

    <!-- Feedback practice mode -->
    <div v-if="overlap?.enabled" class="grid grid-cols-2 gap-2.5 text-left">
      <div>
        <p class="font-mono text-[10px] font-bold tracking-[0.12em] uppercase text-lm-ink-3 m-0 mb-1">Success feedback</p>
        <input v-model="feedback.success" @input="onFeedbackChange" class="w-full box-border font-display text-[12px] px-2.5 py-1.75 border-2 border-lm-line-soft rounded-[8px] bg-lm-bg-soft outline-none text-lm-ink" placeholder="Correct!" />
      </div>
      <div>
        <p class="font-mono text-[10px] font-bold tracking-[0.12em] uppercase text-lm-ink-3 m-0 mb-1">Failure feedback</p>
        <input v-model="feedback.failure" @input="onFeedbackChange" class="w-full box-border font-display text-[12px] px-2.5 py-1.75 border-2 border-lm-line-soft rounded-[8px] bg-lm-bg-soft outline-none text-lm-ink" placeholder="Try again." />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import MonoLabel from '../MonoLabel.vue'
import AdminIcon from '../AdminIcon.vue'

const props = defineProps({
  config: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['change'])

const VL_COLORS = ['#ffd333', '#8fb3ff', '#8fe0aa', '#ff9aa8', '#e3d4ff']

const VL_PRESETS = [
  { id:'venn-2', label:'2-set Venn', desc:'Two overlapping sets',
    zones:[
      { id:'zone_a', label:'A', shape:'circle', x:250, y:130, width:260, height:260, color:'#ffd333', highlightColor:'#ff8f1f', highlightOpacity:0.82 },
      { id:'zone_b', label:'B', shape:'circle', x:390, y:130, width:260, height:260, color:'#8fb3ff', highlightColor:'#4f8cff', highlightOpacity:0.82 },
    ], elements:[], interactions:[],
    overlap:{ enabled:true, sourceZoneIds:['zone_a','zone_b'],
      inputs:[
        { id:'A', label:'n(A)', zoneIds:['zone_a'], value:11, kind:'total' },
        { id:'B', label:'n(B)', zoneIds:['zone_b'], value:9, kind:'total' },
        { id:'A_AND_B', label:'n(A∩B)', zoneIds:['zone_a','zone_b'], value:3, kind:'intersection' },
      ]},
    feedback:{ success:'Correct! The Venn regions match.', failure:'Check set totals and the intersection.' },
  },
  { id:'venn-3', label:'3-set Venn', desc:'Three overlapping sets',
    zones:[
      { id:'zone_a', label:'A', shape:'circle', x:250, y:100, width:260, height:260, color:'#ffd333', highlightColor:'#ff8f1f', highlightOpacity:0.82 },
      { id:'zone_b', label:'B', shape:'circle', x:390, y:100, width:260, height:260, color:'#8fb3ff', highlightColor:'#4f8cff', highlightOpacity:0.82 },
      { id:'zone_c', label:'C', shape:'circle', x:320, y:220, width:260, height:260, color:'#8fe0aa', highlightColor:'#3aa66b', highlightOpacity:0.82 },
    ], elements:[], interactions:[],
    overlap:{ enabled:true, sourceZoneIds:['zone_a','zone_b','zone_c'],
      inputs:[
        { id:'A', label:'n(A)', zoneIds:['zone_a'], value:15, kind:'total' },
        { id:'B', label:'n(B)', zoneIds:['zone_b'], value:12, kind:'total' },
        { id:'C', label:'n(C)', zoneIds:['zone_c'], value:10, kind:'total' },
        { id:'A_AND_B', label:'n(A∩B)', zoneIds:['zone_a','zone_b'], value:4, kind:'intersection' },
        { id:'A_AND_C', label:'n(A∩C)', zoneIds:['zone_a','zone_c'], value:3, kind:'intersection' },
        { id:'B_AND_C', label:'n(B∩C)', zoneIds:['zone_b','zone_c'], value:2, kind:'intersection' },
        { id:'ABC', label:'n(A∩B∩C)', zoneIds:['zone_a','zone_b','zone_c'], value:1, kind:'intersection' },
      ]},
    feedback:{ success:'All regions match!', failure:'Check totals and intersections carefully.' },
  },
  { id:'hotspot', label:'Hotspot', desc:'Labeled regions with buttons',
    zones:[
      { id:'zone_input', label:'Input', shape:'rectangle', x:90, y:160, width:190, height:130, color:'#ffd333', highlightColor:'#ff8f1f', highlightOpacity:0.82 },
      { id:'zone_process', label:'Process', shape:'rectangle', x:355, y:140, width:190, height:170, color:'#8fb3ff', highlightColor:'#4f8cff', highlightOpacity:0.82 },
      { id:'zone_output', label:'Output', shape:'rectangle', x:620, y:160, width:190, height:130, color:'#8fe0aa', highlightColor:'#3aa66b', highlightOpacity:0.82 },
    ],
    elements:[
      { id:'btn_input', label:'Show Input', kind:'button', x:110, y:370, width:150, height:44 },
      { id:'btn_process', label:'Show Process', kind:'button', x:375, y:370, width:150, height:44 },
      { id:'btn_output', label:'Show Output', kind:'button', x:640, y:370, width:150, height:44 },
    ],
    interactions:[
      { triggerId:'btn_input', effect:'HIGHLIGHT_ZONE', targetZoneId:'zone_input', feedback:'Inputs are the values supplied.' },
      { triggerId:'btn_process', effect:'HIGHLIGHT_ZONE', targetZoneId:'zone_process', feedback:'The process transforms inputs.' },
      { triggerId:'btn_output', effect:'HIGHLIGHT_ZONE', targetZoneId:'zone_output', feedback:'Outputs are the results.' },
    ],
    overlap:{ enabled:false, sourceZoneIds:[], inputs:[] },
    feedback:{ success:'', failure:'' },
  },
  { id:'blank', label:'Blank', desc:'Start from scratch',
    zones:[], elements:[], interactions:[],
    overlap:{ enabled:false, sourceZoneIds:[], inputs:[] },
    feedback:{ success:'', failure:'' },
  },
]

const defaultPreset = VL_PRESETS[0]

const activePreset = ref('venn-2')
const zones = ref(props.config?.zones ? JSON.parse(JSON.stringify(props.config.zones)) : JSON.parse(JSON.stringify(defaultPreset.zones)))
const elements = ref(props.config?.elements ? JSON.parse(JSON.stringify(props.config.elements)) : JSON.parse(JSON.stringify(defaultPreset.elements)))
const interactions = ref(props.config?.interactions ? JSON.parse(JSON.stringify(props.config.interactions)) : JSON.parse(JSON.stringify(defaultPreset.interactions)))
const overlap = ref(props.config?.overlap ? JSON.parse(JSON.stringify(props.config.overlap)) : JSON.parse(JSON.stringify(defaultPreset.overlap)))
const feedback = ref(props.config?.feedback ? JSON.parse(JSON.stringify(props.config.feedback)) : JSON.parse(JSON.stringify(defaultPreset.feedback)))

const selectedId = ref(null)
const selectedKind = ref(null)
const highlightZone = ref(null)
const tool = ref('select')

watch(() => props.config, (newVal) => {
  if (!newVal || Object.keys(newVal).length === 0) return
  zones.value = newVal.zones ? JSON.parse(JSON.stringify(newVal.zones)) : []
  elements.value = newVal.elements ? JSON.parse(JSON.stringify(newVal.elements)) : []
  interactions.value = newVal.interactions ? JSON.parse(JSON.stringify(newVal.interactions)) : []
  overlap.value = newVal.overlap ? JSON.parse(JSON.stringify(newVal.overlap)) : { enabled: false, sourceZoneIds: [], inputs: [] }
  feedback.value = newVal.feedback ? JSON.parse(JSON.stringify(newVal.feedback)) : { success: '', failure: '' }
}, { deep: true })

const selectedZone = computed(() => selectedKind.value === 'zone' ? zones.value.find(z => z.id === selectedId.value) : null)
const selectedElement = computed(() => selectedKind.value === 'element' ? elements.value.find(e => e.id === selectedId.value) : null)
const selectedElementInteraction = computed(() => selectedElement.value ? interactions.value.find(i => i.triggerId === selectedElement.value.id) : null)

function applyPreset(preset) {
  activePreset.value = preset.id
  zones.value = JSON.parse(JSON.stringify(preset.zones))
  elements.value = JSON.parse(JSON.stringify(preset.elements))
  interactions.value = JSON.parse(JSON.stringify(preset.interactions))
  overlap.value = JSON.parse(JSON.stringify(preset.overlap))
  feedback.value = JSON.parse(JSON.stringify(preset.feedback))
  selectedId.value = null
  selectedKind.value = null
  highlightZone.value = null
  tool.value = 'select'
  emitChange()
}

function emitChange() {
  emit('change', {
    type: 'VISUAL_LAYER',
    mode: overlap.value?.enabled ? 'PRACTICE' : 'VISUALIZATION',
    canvas: { width: 900, height: 520, backgroundText: '' },
    zones: zones.value,
    elements: elements.value,
    interactions: interactions.value,
    overlap: overlap.value,
    feedback: feedback.value
  })
}

function selectZone(id) {
  selectedId.value = id
  selectedKind.value = 'zone'
  highlightZone.value = null
}

function selectElement(id) {
  selectedId.value = id
  selectedKind.value = 'element'
}

function clearSelection() {
  selectedId.value = null
  selectedKind.value = null
  highlightZone.value = null
}

function onCanvasClick(e) {
  if (e.target.tagName === 'svg' || e.target.tagName === 'rect' && e.target.getAttribute('width') === '900') {
    clearSelection()
  }
}

function updateSelectedZoneColor(c) {
  if (selectedZone.value) {
    selectedZone.value.color = c
    selectedZone.value.highlightColor = c
    emitChange()
  }
}

function onZoneChange() {
  emitChange()
}

function deleteZone(id) {
  zones.value = zones.value.filter(z => z.id !== id)
  interactions.value = interactions.value.filter(i => i.targetZoneId !== id)
  if (overlap.value?.sourceZoneIds) {
    overlap.value.sourceZoneIds = overlap.value.sourceZoneIds.filter(s => s !== id)
  }
  if (selectedId.value === id) clearSelection()
  emitChange()
}

function addZone() {
  const idx = zones.value.length
  const newZone = {
    id: `zone_${idx + 1}`,
    label: String.fromCharCode(65 + idx),
    shape: 'circle',
    x: 200 + idx * 80,
    y: 150,
    width: 220,
    height: 220,
    color: VL_COLORS[idx % VL_COLORS.length],
    highlightColor: VL_COLORS[idx % VL_COLORS.length],
    highlightOpacity: 0.82
  }
  zones.value.push(newZone)
  selectZone(newZone.id)
  emitChange()
}

function onElementClick(el) {
  selectElement(el.id)
  const inter = interactions.value.find(i => i.triggerId === el.id)
  if (inter?.targetZoneId) highlightZone.value = inter.targetZoneId
}

function onElementListClick(el) {
  selectElement(el.id)
  const inter = interactions.value.find(i => i.triggerId === el.id)
  if (inter?.targetZoneId) highlightZone.value = inter.targetZoneId
}

function onElementChange() {
  emitChange()
}

function onInteractionChange() {
  emitChange()
}

function deleteElement(id) {
  elements.value = elements.value.filter(e => e.id !== id)
  interactions.value = interactions.value.filter(i => i.triggerId !== id)
  if (selectedId.value === id) clearSelection()
  emitChange()
}

function getInteraction(elementId) {
  return interactions.value.find(i => i.triggerId === elementId)
}

function onOverlapInputChange() {
  emitChange()
}

function onFeedbackChange() {
  emitChange()
}

// Overlap math
function vlCombinations(ids) {
  const r = []
  for (let m = 1; m < (1 << ids.length); m++) {
    r.push(ids.filter((_, i) => m & (1 << i)))
  }
  return r
}

function vlExactValues(inputs, sourceIds, zonesList) {
  const combos = vlCombinations(sourceIds)
  return combos.map(zids => {
    let val = 0
    combos.forEach(other => {
      if (zids.every(id => other.includes(id))) {
         const inp = inputs.find(i => i.zoneIds.length === other.length && other.every(id => i.zoneIds.includes(id)))
         val += ((other.length - zids.length) % 2 === 0 ? 1 : -1) * (inp ? inp.value : 0)
      }
    })
    const labels = zids.map(id => zonesList.find(z => z.id === id)?.label || id.replace('zone_', '').toUpperCase())
    const label = labels.length === 1 ? `${labels[0]} only` : labels.join(' ∩ ')
    return { zoneIds: zids, value: val, label, id: zids.join('_') }
  })
}

function vlRegionCenter(regionIds, allZones) {
  const inc = allZones.filter(z => regionIds.includes(z.id))
  const exc = allZones.filter(z => !regionIds.includes(z.id))
  if (!inc.length) return { x: 450, y: 260 }
  const cx = inc.reduce((s, z) => s + z.x + z.width / 2, 0) / inc.length
  const cy = inc.reduce((s, z) => s + z.y + z.height / 2, 0) / inc.length
  if (!exc.length) return { x: cx, y: cy }
  const ex = exc.reduce((s, z) => s + z.x + z.width / 2, 0) / exc.length
  const ey = exc.reduce((s, z) => s + z.y + z.height / 2, 0) / exc.length
  const dx = cx - ex
  const dy = cy - ey
  const len = Math.sqrt(dx * dx + dy * dy) || 1
  const push = inc.length === 1 ? 55 : 22
  return { x: cx + (dx / len) * push, y: cy + (dy / len) * push }
}

const exactRegions = computed(() => {
  if (!overlap.value?.enabled || !overlap.value.inputs?.length) return []
  const sourceZones = zones.value.filter(z => overlap.value.sourceZoneIds?.includes(z.id))
  if (!sourceZones.length) return []
  const exact = vlExactValues(overlap.value.inputs, overlap.value.sourceZoneIds, zones.value)
  return exact.map(r => ({ ...r, center: vlRegionCenter(r.zoneIds, sourceZones) }))
})
</script>
