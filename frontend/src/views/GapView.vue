<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { api } from '@/api/client'
import { useUserProfileStore } from '@/stores/userProfile'

// 对应②技能差距分析引擎的展示层：技能差距雷达图 + 明细列表。
// 雷达图按「用户画像怎么建」一节的思路，把细粒度技能按 domain 聚合成几个维度再画，
// 不直接画几十个技能点。
const profile = useUserProfileStore()
const items = ref([])
const loading = ref(false)
const error = ref('')

async function load() {
  if (!profile.targetCategoryId) return
  loading.value = true
  error.value = ''
  try {
    items.value = await api.gapAnalysis(profile.targetCategoryId)
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

const gapList = computed(() => items.value.filter((i) => !i.mastered))
const masteredList = computed(() => items.value.filter((i) => i.mastered))

// --- 雷达图几何：N 个维度均分圆周，半径按 score(0~100) 映射 ---
const RADIUS = 100
const CENTER = 130
function axisPoint(i, n, r) {
  const angle = (Math.PI * 2 * i) / n - Math.PI / 2
  return [CENTER + r * Math.cos(angle), CENTER + r * Math.sin(angle)]
}
const polygonPoints = computed(() => {
  const n = domains.value.length
  if (n < 3) return ''
  return domains.value
    .map((d, i) => axisPoint(i, n, (d.score / 100) * RADIUS).join(','))
    .join(' ')
})
const axisLines = computed(() => {
  const n = domains.value.length
  return domains.value.map((d, i) => {
    const [x, y] = axisPoint(i, n, RADIUS)
    const [lx, ly] = axisPoint(i, n, RADIUS + 22)
    return { x, y, lx, ly, label: d.name, score: d.score }
  })
})
const gridRings = [0.25, 0.5, 0.75, 1].map((f) => {
  const n = domains.value.length
  return Array.from({ length: n }, (_, i) => axisPoint(i, n, f * RADIUS).join(',')).join(' ')
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
        <div class="radar-wrap">
          <svg viewBox="0 0 260 260" width="260" height="260" v-if="domains.length >= 3">
            <polygon v-for="(ring, i) in gridRings" :key="i" :points="ring" class="grid-ring" />
            <line v-for="(a, i) in axisLines" :key="'ax' + i" x1="130" y1="130" :x2="a.x" :y2="a.y" class="axis-line" />
            <polygon :points="polygonPoints" class="score-polygon" />
            <text v-for="(a, i) in axisLines" :key="'lbl' + i" :x="a.lx" :y="a.ly" class="axis-label" text-anchor="middle">
              {{ a.label }}（{{ a.score }}%）
            </text>
          </svg>
          <p v-else class="hint">维度不足 3 个，暂不画雷达图，看下面的明细列表就行。</p>

          <div class="lists">
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
        </div>
        <RouterLink to="/path" class="cta">去看学习路径 →</RouterLink>
      </template>
      <p v-else>这个岗位类别还没有技能数据。</p>
    </template>
  </section>
</template>

<style scoped>
h1 { margin-bottom: 0.5rem; }
.radar-wrap { display: flex; gap: 2rem; flex-wrap: wrap; align-items: flex-start; }
.grid-ring { fill: none; stroke: #e0e0e0; stroke-width: 1; }
.axis-line { stroke: #e0e0e0; stroke-width: 1; }
.score-polygon { fill: rgba(43, 110, 92, 0.25); stroke: #2b6e5c; stroke-width: 2; }
.axis-label { font-size: 9px; fill: #5b6472; }
.lists { display: flex; gap: 2rem; flex-wrap: wrap; }
.lists h3 { font-size: 0.95rem; margin-bottom: 0.5rem; }
.lists ul { list-style: none; padding: 0; margin: 0; min-width: 180px; }
.lists li { display: flex; justify-content: space-between; gap: 0.75rem; padding: 0.3rem 0; border-bottom: 1px solid #eee; font-size: 0.9rem; }
.weight { color: #ae5f1c; font-variant-numeric: tabular-nums; }
.badge { font-size: 0.75rem; padding: 1px 6px; border-radius: 999px; background: #e3f0eb; color: #2b6e5c; }
.badge.self_reported { background: #f6e8da; color: #ae5f1c; }
.cta { display: inline-block; margin-top: 1.25rem; color: #2b6e5c; font-weight: 600; text-decoration: none; }
.error { color: #b3261e; }
.hint { color: #8891a0; font-size: 0.9rem; }
</style>
