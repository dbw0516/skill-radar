<script setup>
import { computed, ref, onMounted, onBeforeUnmount } from 'vue'
import { RouterLink } from 'vue-router'
import { layoutMetro } from '@/metro'

// 学习路径地铁线路图。布局算法在 src/metro.js（有单测），这里只负责画：
// 底下一层 SVG 画区间色带、线路、编号方块和终点站方块；上面一层用真正的链接摆站点，
// 点站点直接进这个技能的测评，键盘也能 Tab 到。
const props = defineProps({
  // [{ skillId, name, domain, weight(0~1), stage(0=已掌握), mastered, masteredSource, score(0~100) }]
  stations: { type: Array, required: true },
  nextSkillId: { type: [Number, String], default: null },
  terminusName: { type: String, default: '目标岗位' },
})

// 量出外层卡片的宽度交给布局：站点少时把线路图拉满卡片，站点多时保持自然宽度、横向滚动
const scrollEl = ref(null)
const boxWidth = ref(0)
let observer = null
onMounted(() => {
  // 先同步量一次，不等 ResizeObserver 的第一次回调（它要等下一帧渲染才触发）
  boxWidth.value = scrollEl.value.clientWidth
  observer = new ResizeObserver(([entry]) => { boxWidth.value = Math.floor(entry.contentRect.width) })
  observer.observe(scrollEl.value)
})
onBeforeUnmount(() => observer?.disconnect())

const map = computed(() => layoutMetro(props.stations, { nextSkillId: props.nextSkillId, fitWidth: boxWidth.value }))
const nextStop = computed(() => map.value.stops.find((s) => s.state === 'next'))

function pillWidth(text) {
  return Math.max(84, text.length * 15 + 34)
}

function stopMeta(s) {
  if (s.state === 'mastered') return s.masteredSource === 'quiz_verified' ? '已认证' : '自评'
  const w = Math.round((s.weight ?? 0) * 100)
  return `JD ${w}% · 进度 ${s.score ?? 0}%`
}

function stopLabel(s) {
  const state = s.state === 'mastered' ? '已到达' : s.state === 'next' ? '下一站' : '待到达'
  return `${s.name}，${s.line.number}号线 ${s.domain}，${state}，${stopMeta(s)}，点击进入测评`
}
</script>

<template>
  <div ref="scrollEl" class="metro-scroll">
    <div
      class="metro"
      role="group"
      aria-label="学习路径线路图"
      :style="{ width: map.width + 'px', height: map.height + 'px' }"
    >
      <svg class="metro-svg" :width="map.width" :height="map.height" :viewBox="`0 0 ${map.width} ${map.height}`" aria-hidden="true">
        <defs>
          <pattern id="metro-hatch" width="12" height="12" patternUnits="userSpaceOnUse" patternTransform="rotate(45)">
            <rect x="0" y="0" width="4" height="12" fill="#e6e6e6" />
          </pattern>
        </defs>

        <!-- 区间色带 -->
        <rect
          v-for="(col, i) in map.columns" :key="'band' + col.stage"
          :x="col.x" y="0" :width="col.width" :height="map.height"
          :fill="col.stage === 0 ? 'url(#metro-hatch)' : i % 2 ? '#f4f4f4' : '#ffffff'"
        />
        <line
          v-for="col in map.columns.slice(1)" :key="'sep' + col.stage"
          :x1="col.x" :x2="col.x" y1="62" :y2="map.height - 10"
          stroke="#111111" stroke-width="2" stroke-dasharray="8 8"
        />
        <line
          :x1="map.terminus.x - 28" :x2="map.terminus.x - 28" y1="62" :y2="map.height - 10"
          stroke="#111111" stroke-width="2" stroke-dasharray="8 8"
        />

        <!-- 区间标签：带硬投影的胶囊 -->
        <g v-for="col in map.columns" :key="'pill' + col.stage">
          <rect :x="col.x + col.width / 2 - pillWidth(col.label) / 2 + 4" y="14" :width="pillWidth(col.label)" height="32" rx="16" fill="#111111" />
          <rect
            :x="col.x + col.width / 2 - pillWidth(col.label) / 2" y="10" :width="pillWidth(col.label)" height="32" rx="16"
            :fill="col.stage === 0 ? '#ffffff' : col.stage % 2 ? '#ff7ab6' : '#9dbbff'" stroke="#111111" stroke-width="3"
          />
          <text :x="col.x + col.width / 2" y="32" text-anchor="middle" class="pill-text">{{ col.label }}</text>
          <text :x="col.x + col.width / 2" y="62" text-anchor="middle" class="pill-sub">{{ col.sub }}</text>
        </g>
        <g>
          <rect :x="map.terminus.x + map.terminus.width / 2 - 42 + 4" y="14" width="84" height="32" rx="16" fill="#ff7ab6" />
          <rect :x="map.terminus.x + map.terminus.width / 2 - 42" y="10" width="84" height="32" rx="16" fill="#111111" stroke="#111111" stroke-width="3" />
          <text :x="map.terminus.x + map.terminus.width / 2" y="32" text-anchor="middle" class="pill-text on-dark">终点</text>
          <text :x="map.terminus.x + map.terminus.width / 2" y="62" text-anchor="middle" class="pill-sub">TERMINUS</text>
        </g>

        <!-- 线路：先画一层黑色粗描边，再压彩色线芯 -->
        <line v-for="l in map.lines" :key="'o' + l.domain" :x1="l.x1" :x2="l.x2" :y1="l.y" :y2="l.y" stroke="#111111" stroke-width="22" stroke-linecap="round" />
        <line v-for="l in map.lines" :key="'c' + l.domain" :x1="l.x1" :x2="l.x2" :y1="l.y" :y2="l.y" :stroke="l.color" stroke-width="12" stroke-linecap="round" />

        <!-- 线路编号方块 -->
        <g v-for="l in map.lines" :key="'b' + l.domain">
          <rect :x="l.x1 - 15 + 3" :y="l.y - 15 + 3" width="30" height="30" rx="4" fill="#111111" />
          <rect :x="l.x1 - 15" :y="l.y - 15" width="30" height="30" rx="4" :fill="l.color" stroke="#111111" stroke-width="3" />
          <text :x="l.x1" :y="l.y + 6" text-anchor="middle" class="badge-text">{{ l.number }}</text>
        </g>

        <!-- 终点站：黑色大方块 + 粉色硬投影 -->
        <rect :x="map.terminus.x + 7" :y="map.terminus.y + 7" :width="map.terminus.width" :height="map.terminus.height" rx="14" fill="#ff7ab6" />
        <rect :x="map.terminus.x" :y="map.terminus.y" :width="map.terminus.width" :height="map.terminus.height" rx="14" fill="#111111" />
      </svg>

      <div
        class="terminus"
        :style="{ left: map.terminus.x + 'px', top: map.terminus.y + 'px', width: map.terminus.width + 'px', height: map.terminus.height + 'px' }"
      >
        <span class="terminus-mark" aria-hidden="true">终</span>
        <strong class="terminus-name">{{ terminusName }}</strong>
        <span class="terminus-sub">终点站</span>
      </div>

      <RouterLink
        v-for="s in map.stops" :key="s.skillId"
        class="stop" :class="s.state"
        :style="{ left: s.x - map.slotW / 2 + 'px', top: s.y - 22 + 'px', width: map.slotW + 'px' }"
        :to="{ path: '/quiz', query: { skillId: s.skillId, name: s.name } }"
        :aria-label="stopLabel(s)"
      >
        <span class="dot" aria-hidden="true">
          <svg v-if="s.state === 'mastered'" width="16" height="16" viewBox="0 0 16 16"><path d="M3 8.5 L6.5 12 L13 4.5" fill="none" stroke="#ffffff" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" /></svg>
        </span>
        <span class="stop-name">{{ s.name }}</span>
        <span class="stop-meta">{{ stopMeta(s) }}</span>
      </RouterLink>

      <span
        v-if="nextStop"
        class="next-sticker" aria-hidden="true"
        :style="{ left: nextStop.x + 20 + 'px', top: nextStop.y - 54 + 'px' }"
      >下一站</span>
    </div>
  </div>

  <div class="legend">
    <span v-for="l in map.lines" :key="l.domain" class="legend-item">
      <span class="line-badge" :style="{ background: l.color }">{{ l.number }}</span>{{ l.domain }}
    </span>
    <span class="legend-gap"></span>
    <span class="legend-item"><span class="legend-dot mastered" aria-hidden="true"></span>已到达</span>
    <span class="legend-item"><span class="legend-dot next" aria-hidden="true"></span>下一站</span>
    <span class="legend-item"><span class="legend-dot todo" aria-hidden="true"></span>待到达</span>
  </div>
</template>

<style scoped>
.metro-scroll { overflow-x: auto; border: var(--border-thick); border-radius: var(--radius-md); background: var(--paper); }
.metro { position: relative; min-width: 100%; }
.metro-svg { position: absolute; inset: 0; display: block; }
.pill-text { font-family: var(--font-body); font-size: 14px; font-weight: 900; fill: #111111; }
.pill-text.on-dark { fill: #ffd84d; }
.pill-sub { font-family: var(--font-body); font-size: 12px; font-weight: 700; fill: #4a4a4a; letter-spacing: 0.04em; }
.badge-text { font-family: var(--font-num); font-size: 15px; fill: #111111; }

.terminus { position: absolute; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 6px; padding: 12px; color: var(--paper); text-align: center; }
.terminus-mark { font-family: var(--font-display); font-size: 48px; line-height: 1; color: var(--yellow); }
.terminus-name { font-size: 17px; font-weight: 900; line-height: 1.3; word-break: break-all; }
.terminus-sub { font-size: 12px; font-weight: 700; color: #d9d9d9; }

.stop { position: absolute; display: flex; flex-direction: column; align-items: center; color: var(--ink); text-align: center; }
.stop:hover { color: var(--ink); text-decoration: none; }
.dot { display: grid; place-items: center; width: 44px; height: 44px; }
.dot::before { content: ''; grid-area: 1 / 1; border-radius: 50%; transition: transform 140ms ease, box-shadow 140ms ease; }
.dot svg { grid-area: 1 / 1; position: relative; }
.todo .dot::before { width: 28px; height: 28px; border: 5px solid var(--ink); background: var(--paper); }
.mastered .dot::before { width: 34px; height: 34px; border: 3px solid var(--paper); background: var(--ink); box-shadow: 0 0 0 2px var(--ink); }
.next .dot::before { width: 42px; height: 42px; border: 6px solid var(--ink); background: radial-gradient(circle, var(--red) 0 7px, var(--ink) 7px 9px, var(--paper) 9px); }
.next .dot::after { content: ''; grid-area: 1 / 1; width: 42px; height: 42px; border: 4px solid var(--red); border-radius: 50%; animation: pulse 2s ease-out infinite; }
@keyframes pulse { from { transform: scale(1); opacity: 0.9; } to { transform: scale(2.1); opacity: 0; } }
.stop:hover .dot::before, .stop:focus-visible .dot::before { transform: scale(1.12); }

.stop-name {
  display: -webkit-box;
  max-width: calc(100% - 12px);
  margin-top: 2px;
  overflow: hidden;
  font-size: 15px;
  font-weight: 900;
  line-height: 1.25;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}
.stop:hover .stop-name { text-decoration: underline; text-decoration-thickness: 3px; text-decoration-color: var(--yellow); text-underline-offset: 3px; }
.next .stop-name { font-size: 17px; }
.mastered .stop-name { color: #3a3a3a; }
.stop-meta { margin-top: 2px; font-size: 12px; font-weight: 700; color: var(--color-muted); white-space: nowrap; }

.next-sticker {
  position: absolute;
  padding: 3px 12px;
  border: var(--border-thick);
  border-radius: 6px;
  background: var(--yellow);
  box-shadow: var(--shadow-sm);
  font-size: 15px;
  font-weight: 900;
  transform: rotate(-6deg);
  pointer-events: none;
  white-space: nowrap;
}

.legend { display: flex; flex-wrap: wrap; align-items: center; gap: 10px 20px; margin-top: 14px; font-size: 14px; font-weight: 800; }
.legend-item { display: inline-flex; align-items: center; gap: 8px; }
.legend-gap { flex: 1 1 auto; }
.legend-dot { display: inline-block; width: 18px; height: 18px; border-radius: 50%; }
.legend-dot.mastered { background: var(--ink); }
.legend-dot.next { border: 4px solid var(--ink); background: var(--red); }
.legend-dot.todo { border: 4px solid var(--ink); background: var(--paper); }
</style>
