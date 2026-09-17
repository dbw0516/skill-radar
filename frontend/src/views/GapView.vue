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

const gapList = computed(() => items.value.filter((i) => !i.mastered))
const masteredList = computed(() => items.value.filter((i) => i.mastered))

// --- 雷达图几何：N 个维度均分圆周，半径按 score(0~100) 映射 ---
// 坑：CENTER 到 viewBox 边缘的留白得够放下一整条轴标签文字，不然长一点的 domain 名字
//（比如"Python语言"这种中英文混排的，比原来纯中文的"后端框架"宽不少）会被 SVG 默认
// 裁掉一截，比如"Python语言（0%）"显示成"Python语言（0%"——踩过这个坑，所以：
//  1) 半径到边缘留 60px 空当（不是刚好贴着标签点）；
//  2) 标签不再一律 text-anchor="middle"，偏水平（左右两侧）的轴改成往外锚定
//     （靠右的轴左对齐、靠左的轴右对齐），这样文字往外长而不是从中点向两边裂开。
const RADIUS = 100
const CENTER = 170
const LABEL_R = RADIUS + 20
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
const polygonPoints = computed(() => {
  const n = radarAxes.value.length
  if (n < 3) return ''
  return radarAxes.value
    .map((d, i) => axisPoint(i, n, (d.score / 100) * RADIUS).join(','))
    .join(' ')
})
const axisLines = computed(() => {
  const n = radarAxes.value.length
  return radarAxes.value.map((d, i) => {
    const [x, y] = axisPoint(i, n, RADIUS)
    const [lx, ly] = axisPoint(i, n, LABEL_R)
    return { x, y, lx, ly, label: d.name, score: d.score, anchor: labelAnchor(lx) }
  })
})
// 之前这里没包 computed()，n 在 <script setup> 执行那一刻（数据还没异步加载回来）
// 就定死了，网格环永远按 n=0 画——等于白纸一张。包上 computed 才会跟着数据更新。
const gridRings = computed(() => {
  const n = radarAxes.value.length
  if (n < 3) return []
  return [0.25, 0.5, 0.75, 1].map((f) =>
    Array.from({ length: n }, (_, i) => axisPoint(i, n, f * RADIUS).join(',')).join(' ')
  )
})
</script>

<template>
  <section>
    <h1>技能差距</h1>
    <p v-if="!profile.targetCategoryId">请先在"岗位推荐"页选择一个目标岗位。</p>
    <template v-else>
      <p v-if="loading">加载中…</p>
      <p v-else-if="error" class="error">{{ error }}</p>
      <template v-else-if="items.length">
        <TabBar
          :tabs="[{ key: 'radar', label: '技能雷达图' }, { key: 'list', label: '技能明细' }]"
          v-model="activeTab"
        />

        <div v-if="activeTab === 'radar'" class="radar-wrap">
          <svg viewBox="0 0 340 340" width="340" height="340" v-if="radarAxes.length >= 3">
            <polygon v-for="(ring, i) in gridRings" :key="i" :points="ring" class="grid-ring" />
            <line v-for="(a, i) in axisLines" :key="'ax' + i" x1="170" y1="170" :x2="a.x" :y2="a.y" class="axis-line" />
            <polygon :points="polygonPoints" class="score-polygon" />
            <text v-for="(a, i) in axisLines" :key="'lbl' + i" :x="a.lx" :y="a.ly" class="axis-label" :text-anchor="a.anchor">
              {{ a.label }}（{{ a.score }}%）
            </text>
          </svg>
          <p v-else class="hint">技能数不足 3 个，暂不画雷达图，看"技能明细" tab 就行。</p>
        </div>

        <div v-else class="lists">
          <div>
            <h3>待学（{{ gapList.length }}）</h3>
            <ul>
              <li v-for="g in gapList" :key="g.skillId">
                <span class="name">{{ g.name }}</span>
                <span class="weight">权重 {{ Math.round(g.weight * 100) }}%</span>
              </li>
            </ul>
          </div>
          <div>
            <h3>已掌握（{{ masteredList.length }}）</h3>
            <ul>
              <li v-for="g in masteredList" :key="g.skillId">
                <span class="name">{{ g.name }}</span>
                <span class="badge" :class="g.masteredSource">{{ g.masteredSource === 'quiz_verified' ? '已认证' : '自评' }}</span>
              </li>
            </ul>
          </div>
        </div>

        <RouterLink to="/path" class="cta">去看学习路径 →</RouterLink>
      </template>
      <p v-else>这个岗位类别还没有技能数据。</p>
    </template>
  </section>
</template>

<style scoped>
h1 { margin-bottom: 4px; }
.radar-wrap { display: flex; justify-content: center; padding: 24px; margin-top: 4px; }
.radar-wrap svg { overflow: visible; max-width: 100%; height: auto; }
.grid-ring { fill: none; stroke: var(--color-border); stroke-width: 1; }
.axis-line { stroke: var(--color-border); stroke-width: 1; }
.score-polygon { fill: color-mix(in srgb, var(--color-primary) 20%, transparent); stroke: var(--color-primary); stroke-width: 2.5; }
.axis-label { font-size: 9px; fill: var(--color-muted); }
.lists { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; }
.lists > div { padding: 18px; }
.lists h3 { margin-bottom: 12px; color: var(--color-heading); font-size: 0.98rem; }
.lists ul { list-style: none; padding: 0; margin: 0; }
.lists li { display: flex; justify-content: space-between; align-items: center; gap: 12px; padding: 10px 0; border-bottom: 1px solid var(--color-border); font-size: 0.9rem; }
.lists li:last-child { border-bottom: 0; }
.name { color: var(--color-heading); font-weight: 650; }
.weight { color: var(--color-warning); font-variant-numeric: tabular-nums; font-weight: 650; }
.badge { padding: 3px 8px; border: 1px solid var(--color-border); border-radius: var(--radius-sm); background: var(--color-soft); color: var(--color-primary); font-size: 0.75rem; font-weight: 700; }
.badge.self_reported { border-color: color-mix(in srgb, var(--color-warning) 35%, transparent); background: color-mix(in srgb, var(--color-warning) 12%, var(--color-surface)); color: var(--color-warning); }
.cta { color: var(--color-primary); }
.error { margin-top: 20px; padding: 14px 16px; border-radius: var(--radius-md); background: color-mix(in srgb, var(--color-danger) 9%, var(--color-surface)); }
.hint { color: var(--color-muted); font-size: 0.9rem; }
@media (max-width: 640px) { .lists { grid-template-columns: 1fr; } .radar-wrap { padding: 12px; } }
</style>
