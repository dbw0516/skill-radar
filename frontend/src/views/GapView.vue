<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { api } from '@/api/client'
import { useUserProfileStore } from '@/stores/userProfile'
import { useAuthStore } from '@/stores/auth'
import TabBar from '@/components/TabBar.vue'

// 对应②技能差距分析引擎的展示层：技能差距雷达图 + 明细列表，分两个 tab 展示。
// 雷达图按「用户画像怎么建」一节的思路，把细粒度技能按 domain 聚合成几个维度再画，
// 不直接画几十个技能点。
const profile = useUserProfileStore()
const auth = useAuthStore()
const activeTab = ref('radar')
const items = ref([])
const loading = ref(false)
const error = ref('')

async function load() {
  if (!profile.targetCategoryId) return
  loading.value = true
  error.value = ''
  try {
    items.value = await api.gapAnalysis(profile.targetCategoryId, auth.userId)
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
onMounted(load)
watch(() => profile.targetCategoryId, load)

const domains = computed(() => {
  const map = new Map()
  for (const it of items.value) {
    const d = it.domain || '其他'
    if (!map.has(d)) map.set(d, { name: d, total: 0, mastered: 0 })
    const rec = map.get(d)
    rec.total++
    if (it.mastered) rec.mastered++
  }
  return [...map.values()].map((r) => ({ ...r, score: r.total ? Math.round((r.mastered / r.total) * 100) : 0 }))
})

// 雷达图至少要 3 个维度才画得出来。有些岗位技能方向本来就窄（比如移动开发只分
// Android/iOS 两个 domain），按 domain 聚合凑不够 3 个，但技能本身可能有十几个——
// 这种情况退化成按单个技能画（每个技能算一维，掌握=100/未掌握=0），照样能画雷达图；
// 只有技能总数本身也不够 3 个（比如产品经理这种数据本来就很薄的类别）才真的没法画。
const radarAxes = computed(() => {
  if (domains.value.length >= 3) return domains.value
  return items.value.map((it) => ({ name: it.name, score: it.mastered ? 100 : 0 }))
})

const gapList = computed(() => items.value.filter((i) => !i.mastered).sort((a, b) => b.weight - a.weight))
const masteredList = computed(() => items.value.filter((i) => i.mastered))

// --- 雷达图几何：N 个维度均分圆周，半径按 score(0~100) 映射 ---
// 坑：CENTER 到 viewBox 边缘的留白得够放下一整条轴标签文字，不然长一点的 domain 名字
//（比如"Python语言"这种中英文混排的）会被 SVG 默认裁掉一截。所以：
//  1) 半径到边缘留足空当；
//  2) 标签不再一律 text-anchor="middle"，偏水平（左右两侧）的轴改成往外锚定
//     （靠右的轴左对齐、靠左的轴右对齐），这样文字往外长而不是从中点向两边裂开。
const SIZE = 480
const CENTER = SIZE / 2
const RADIUS = 165
const LABEL_R = RADIUS + 26
function axisPoint(i, n, r) {
  const angle = (Math.PI * 2 * i) / n - Math.PI / 2
  return [CENTER + r * Math.cos(angle), CENTER + r * Math.sin(angle)]
}
function labelAnchor(x) {
  const dx = x - CENTER
  if (dx > 15) return 'start'
  if (dx < -15) return 'end'
  return 'middle'
}
const scorePoints = computed(() => {
  const n = radarAxes.value.length
  if (n < 3) return []
  return radarAxes.value.map((d, i) => axisPoint(i, n, (d.score / 100) * RADIUS))
})
const polygonPoints = computed(() => scorePoints.value.map((p) => p.join(',')).join(' '))
const axisLines = computed(() => {
  const n = radarAxes.value.length
  return radarAxes.value.map((d, i) => {
    const [x, y] = axisPoint(i, n, RADIUS)
    const [lx, ly] = axisPoint(i, n, LABEL_R)
    return { x, y, lx, ly, label: d.name, score: d.score, anchor: labelAnchor(lx), low: d.score < 50 }
  })
})
// 之前这里没包 computed()，n 在 <script setup> 执行那一刻（数据还没异步加载回来）
// 就定死了，网格环永远按 n=0 画——等于白纸一张。包上 computed 才会跟着数据更新。
const gridRings = computed(() => {
  const n = radarAxes.value.length
  if (n < 3) return []
  return [1 / 3, 2 / 3, 1].map((f) =>
    Array.from({ length: n }, (_, i) => axisPoint(i, n, f * RADIUS).join(',')).join(' ')
  )
})
</script>

<template>
  <section>
    <h1>技能差距</h1>
    <p class="lead">把岗位要求的技能按大类拼成雷达图：粉色面积越大，离目标岗位越近。</p>
    <p v-if="!profile.targetCategoryId" class="empty">请先在<RouterLink to="/">岗位推荐</RouterLink>页选择一个目标岗位。</p>
    <template v-else>
      <p v-if="loading" class="hint">加载中…</p>
      <p v-else-if="error" class="error">{{ error }}</p>
      <template v-else-if="items.length">
        <div class="tiles">
          <div class="tile green"><span>已掌握</span><strong class="num">{{ masteredList.length }}</strong></div>
          <div class="tile pink"><span>待学技能</span><strong class="num">{{ gapList.length }}</strong></div>
          <div class="tile blue"><span>雷达维度</span><strong class="num">{{ radarAxes.length }}</strong></div>
        </div>

        <TabBar
          :tabs="[{ key: 'radar', label: '技能雷达图' }, { key: 'list', label: '技能明细' }]"
          v-model="activeTab"
        />

        <div v-if="activeTab === 'radar'" class="radar-wrap">
          <svg
            v-if="radarAxes.length >= 3"
            :viewBox="`0 0 ${SIZE} ${SIZE}`" :width="SIZE" :height="SIZE"
            role="img" :aria-label="'技能雷达图：' + axisLines.map((a) => `${a.label} ${a.score}%`).join('，')"
          >
            <polygon v-for="(ring, i) in gridRings" :key="i" :points="ring" :class="i === gridRings.length - 1 ? 'grid-outer' : 'grid-ring'" />
            <line v-for="(a, i) in axisLines" :key="'ax' + i" :x1="CENTER" :y1="CENTER" :x2="a.x" :y2="a.y" class="axis-line" />
            <polygon :points="polygonPoints" class="score-polygon" />
            <rect v-for="(p, i) in scorePoints" :key="'pt' + i" :x="p[0] - 6" :y="p[1] - 6" width="12" height="12" class="vertex" />
            <text v-for="(a, i) in axisLines" :key="'lbl' + i" :x="a.lx" :y="a.ly" class="axis-label" :text-anchor="a.anchor">
              {{ a.label }}<tspan class="axis-score" :class="{ low: a.low }" dx="6">{{ a.score }}%</tspan>
            </text>
          </svg>
          <p v-else class="hint">技能数不足 3 个，暂不画雷达图，看"技能明细" tab 就行。</p>
        </div>

        <div v-else class="lists">
          <div class="list-card">
            <h3 class="list-head pink">待学 <span class="count">{{ gapList.length }}</span></h3>
            <ul>
              <li v-for="g in gapList" :key="g.skillId">
                <span class="name">{{ g.name }}</span>
                <span class="weight" title="同类岗位 JD 中要求这个技能的比例">
                  <span class="weight-bar"><span :style="{ width: Math.round(g.weight * 100) + '%' }"></span></span>
                  <span class="num">{{ Math.round(g.weight * 100) }}%</span>
                </span>
              </li>
            </ul>
          </div>
          <div class="list-card">
            <h3 class="list-head green">已掌握 <span class="count">{{ masteredList.length }}</span></h3>
            <div class="stickers">
              <span v-for="(g, i) in masteredList" :key="g.skillId" class="mastered-chip" :class="g.masteredSource" :style="{ transform: `rotate(${[-3, 2, -2, 3][i % 4]}deg)` }">
                {{ g.name }}<span class="badge">{{ g.masteredSource === 'quiz_verified' ? '已认证' : '自评' }}</span>
              </span>
            </div>
          </div>
        </div>

        <RouterLink to="/path" class="cta">去看学习路径 →</RouterLink>
      </template>
      <p v-else class="hint">这个岗位类别还没有技能数据。</p>
    </template>
  </section>
</template>

<style scoped>
.lead { margin: 0 0 20px; font-weight: 700; color: var(--color-muted); }
.empty { font-weight: 700; }

.tiles { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 16px; }
.tile { display: flex; align-items: center; justify-content: space-between; gap: 10px; padding: 12px 18px; border: var(--border-thick); border-radius: 14px; box-shadow: var(--shadow-sm); font-weight: 900; }
.tile strong { font-size: 2.2rem; line-height: 1; }
.tile.green { background: var(--green); }
.tile.pink { background: var(--pink); }
.tile.blue { background: var(--blue); }

.radar-wrap { display: flex; justify-content: center; padding: 28px 24px; box-shadow: var(--shadow-md); }
.radar-wrap svg { overflow: visible; max-width: 100%; height: auto; }
.grid-outer { fill: var(--yellow); stroke: var(--ink); stroke-width: 3; }
.grid-ring { fill: none; stroke: var(--ink); stroke-width: 2; }
.axis-line { stroke: var(--ink); stroke-width: 2; }
.score-polygon { fill: var(--pink); stroke: var(--ink); stroke-width: 4; stroke-linejoin: round; }
.vertex { fill: var(--ink); }
.axis-label { font-size: 15px; font-weight: 900; fill: var(--ink); }
.axis-score { font-family: var(--font-num); font-size: 14px; font-weight: 400; fill: var(--color-success); }
.axis-score.low { fill: var(--color-danger); }

.lists { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 24px; }
.list-card { overflow: hidden; box-shadow: var(--shadow-md); }
.list-head { display: flex; align-items: center; gap: 10px; margin: 0; padding: 10px 18px; border-bottom: var(--border-thick); font-family: var(--font-display); font-size: 1.8rem; font-weight: 400; }
.list-head.pink { background: var(--pink); }
.list-head.green { background: var(--green); }
.count { display: inline-grid; place-items: center; width: 32px; height: 32px; border-radius: 50%; color: var(--paper); background: var(--ink); font-family: var(--font-num); font-size: 14px; }
.lists ul { list-style: none; padding: 6px 18px 12px; margin: 0; }
.lists li { display: flex; justify-content: space-between; align-items: center; gap: 12px; padding: 12px 0; border-bottom: 2px solid var(--ink); font-size: 1rem; }
.lists li:last-child { border-bottom: 0; }
.name { color: var(--ink); font-weight: 900; }
.weight { display: inline-flex; align-items: center; gap: 10px; }
.weight-bar { display: block; width: 90px; height: 16px; overflow: hidden; border: 2.5px solid var(--ink); border-radius: 999px; background: var(--paper); }
.weight-bar span { display: block; height: 100%; background: var(--yellow); border-right: 2.5px solid var(--ink); }
.stickers { display: flex; flex-wrap: wrap; gap: 16px 12px; padding: 20px 18px 22px; }
.mastered-chip { display: inline-flex; align-items: center; gap: 8px; padding: 7px 8px 7px 14px; border: var(--border-thick); border-radius: 999px; color: var(--paper); background: var(--ink); box-shadow: 3px 3px 0 var(--green); font-weight: 900; }
.mastered-chip .badge { padding: 1px 8px; border-radius: 999px; color: var(--ink); background: var(--yellow); font-size: 0.78rem; }
.mastered-chip.self_reported { color: var(--ink); background: var(--paper); border-style: dashed; box-shadow: none; }
.mastered-chip.self_reported .badge { background: transparent; border: 2px solid var(--ink); }
@media (max-width: 640px) {
  .lists, .tiles { grid-template-columns: 1fr; }
  .radar-wrap { padding: 12px; }
}
</style>
