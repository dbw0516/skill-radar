<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { api } from '@/api/client'
import { useUserProfileStore } from '@/stores/userProfile'
import { useAuthStore } from '@/stores/auth'
import { assignLines } from '@/metro'
import ProgressPanel from '@/components/ProgressPanel.vue'
import MetroMap from '@/components/MetroMap.vue'

// 对应③学习路径规划引擎的展示层，画成地铁线路图：
// 技能大类 = 线路，学习阶段（拓扑排序分层）= 区间，已掌握的技能在"已到达"区，终点站是目标岗位。
// 点站点进这个技能的资料 + 测评（④引擎）。
const profile = useUserProfileStore()
const auth = useAuthStore()
const stages = ref([])
const gapItems = ref([])
const loading = ref(false)
const error = ref('')
// 进度面板加载完带出来：每个技能的部分进度（0~100）、岗位名、加权覆盖率、推荐的下一个技能
const progress = ref(null)
const skillScores = computed(() =>
  Object.fromEntries((progress.value?.skills || []).map((s) => [s.skillId, s.score]))
)
function onProgressLoaded(p) {
  progress.value = p
}

async function load() {
  if (!profile.targetCategoryId) return
  loading.value = true
  error.value = ''
  try {
    const [path, gap] = await Promise.all([
      api.learningPath(profile.targetCategoryId, auth.userId),
      api.gapAnalysis(profile.targetCategoryId, auth.userId),
    ])
    stages.value = path
    gapItems.value = gap
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
onMounted(load)
watch(() => profile.targetCategoryId, load)

const stations = computed(() => {
  const arrived = gapItems.value
    .filter((g) => g.mastered)
    .map((g) => ({ ...g, stage: 0, score: 100 }))
  const todo = stages.value.flatMap((s) =>
    s.skills.map((sk) => ({ ...sk, stage: s.stage, mastered: false, score: skillScores.value[sk.skillId] ?? 0 }))
  )
  return [...arrived, ...todo]
})

const todoStops = computed(() =>
  stations.value
    .filter((s) => !s.mastered)
    .sort((a, b) => a.stage - b.stage || (b.weight ?? 0) - (a.weight ?? 0))
)
const arrivedCount = computed(() => stations.value.length - todoStops.value.length)
const lineByDomain = computed(() => new Map(assignLines(stations.value).map((l) => [l.domain, l])))
const lineOf = (s) => lineByDomain.value.get(s.domain || '其他')
const terminusName = computed(() => progress.value?.categoryName || '目标岗位')
const hasLaterStage = computed(() => stages.value.length > 1)
</script>

<template>
  <section class="path-page">
    <p v-if="!profile.targetCategoryId" class="empty">
      请先在<RouterLink to="/">岗位推荐</RouterLink>页选择一个目标岗位，才能生成学习路线。
    </p>
    <template v-else>
      <header class="sign">
        <div class="sign-main">
          <div class="sign-top">
            <span class="sign-badges" aria-hidden="true">
              <span v-for="l in lineByDomain.values()" :key="l.domain" class="line-badge" :style="{ background: l.color }">{{ l.number }}</span>
            </span>
            <span class="sign-en">LEARNING ROUTE · 学习路径</span>
          </div>
          <h1 class="sign-title">
            <span class="sign-pre">本次列车开往</span>
            {{ terminusName }}
          </h1>
        </div>
        <div class="sign-stats">
          <div class="tile pink"><span>剩余站点</span><strong class="num">{{ todoStops.length }}<small>站</small></strong></div>
          <div class="tile green"><span>已到达</span><strong class="num">{{ arrivedCount }}<small>站</small></strong></div>
          <div class="tile blue" title="已掌握技能按招聘需求（JD 提及比例）加权的覆盖率">
            <span>岗位匹配度</span><strong class="num">{{ progress?.weightedPercent ?? '—' }}<small v-if="progress">%</small></strong>
          </div>
        </div>
      </header>

      <p v-if="loading" class="hint">线路图生成中…</p>
      <p v-else-if="error" class="error">{{ error }}</p>
      <template v-else-if="stations.length">
        <p v-if="!todoStops.length" class="done">全部站点都已到达，可以去模拟面试检验一下了。</p>
        <MetroMap :stations="stations" :next-skill-id="progress?.nextSkillId ?? null" :terminus-name="terminusName" />

        <div class="below">
          <div v-if="todoStops.length" class="timetable">
            <div class="card-head pink-head">
              <h2>时刻表</h2>
              <span class="head-en">STOPS · {{ todoStops.length }}</span>
            </div>
            <div class="table-scroll">
              <table>
                <thead>
                  <tr>
                    <th scope="col">线路</th>
                    <th scope="col">站名</th>
                    <th scope="col">区间</th>
                    <th scope="col" title="同类岗位 JD 中要求这个技能的比例">JD 要求</th>
                    <th scope="col">进度</th>
                    <th scope="col"><span class="sr-only">操作</span></th>
                  </tr>
                </thead>
                <TransitionGroup tag="tbody" name="stage">
                  <tr v-for="s in todoStops" :key="s.skillId" :class="{ current: s.skillId === (progress?.nextSkillId ?? todoStops[0].skillId) }">
                    <td><span class="line-badge" :style="{ background: lineOf(s)?.color }">{{ lineOf(s)?.number }}</span></td>
                    <td class="name">{{ s.name }}</td>
                    <td>第 {{ s.stage }} 区</td>
                    <td class="num">{{ Math.round((s.weight ?? 0) * 100) }}%</td>
                    <td>
                      <span class="mini">
                        <span class="mini-bar"><span :style="{ width: s.score + '%' }"></span></span>
                        <span class="num">{{ s.score }}%</span>
                      </span>
                    </td>
                    <td>
                      <RouterLink :to="{ path: '/quiz', query: { skillId: s.skillId, name: s.name } }" class="go">进站测评 →</RouterLink>
                    </td>
                  </tr>
                </TransitionGroup>
              </table>
            </div>
          </div>

          <aside class="tips">
            <span class="sticker tips-tag">乘车提示</span>
            <p class="tips-big">同一区内的站点，没有先后顺序。</p>
            <p>同一区的技能互不依赖，可以同时学。在某一站通过测评就算「出站」，这项技能会自动标记为已认证。</p>
            <p v-if="hasLaterStage">后面区间的站点依赖前面的技能，要先经过前一区。</p>
          </aside>
        </div>
      </template>
      <p v-else class="hint">这个岗位类别还没有技能数据。</p>

      <h2 class="progress-title">学习进度</h2>
      <ProgressPanel :show-badges="false" @loaded="onProgressLoaded" />
    </template>
  </section>
</template>

<style scoped>
.empty { font-size: 1.05rem; font-weight: 700; }

.sign { display: flex; flex-wrap: wrap; gap: 20px; margin-bottom: 28px; }
.sign-main { flex: 999 1 520px; min-width: 0; overflow: hidden; border: var(--border-thick); border-radius: var(--radius-lg); background: var(--paper); box-shadow: var(--shadow-md); }
.sign-top { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 10px 20px; padding: 10px 18px; background: var(--ink); }
.sign-badges { display: flex; flex-wrap: wrap; gap: 6px; }
.sign-badges .line-badge { border-color: var(--paper); }
.sign-en { color: var(--yellow); font-family: var(--font-num); font-size: 12px; letter-spacing: 0.14em; }
.sign-title { margin: 0; padding: 18px 22px 22px; font-family: var(--font-display); font-size: clamp(2.2rem, 5vw, 3.6rem); font-weight: 400; line-height: 1.1; }
.sign-pre { display: block; margin-bottom: 6px; font-family: var(--font-body); font-size: 1.1rem; font-weight: 900; }
.sign-stats { display: flex; flex: 1 1 260px; flex-direction: column; gap: 12px; }
.tile { display: flex; flex: 1; align-items: center; justify-content: space-between; gap: 12px; padding: 10px 18px; border: var(--border-thick); border-radius: 14px; box-shadow: var(--shadow-sm); font-weight: 900; }
.tile strong { font-size: 2.4rem; line-height: 1; }
.tile small { margin-left: 4px; font-family: var(--font-body); font-size: 1rem; font-weight: 900; }
.tile.pink { background: var(--pink); }
.tile.green { background: var(--green); }
.tile.blue { background: var(--blue); }

.done { padding: 12px 16px; border: var(--border-thick); border-radius: var(--radius-md); background: var(--green); font-weight: 900; }

.below { display: flex; flex-wrap: wrap; align-items: stretch; gap: 24px; margin-top: 28px; }
.timetable { flex: 2 1 560px; min-width: 0; overflow: hidden; border: var(--border-thick); border-radius: var(--radius-lg); background: var(--paper); box-shadow: var(--shadow-md); }
.card-head { display: flex; align-items: center; justify-content: space-between; gap: 8px; padding: 10px 18px; border-bottom: var(--border-thick); }
.card-head h2 { margin: 0; font-family: var(--font-display); font-size: 1.9rem; font-weight: 400; line-height: 1.1; }
.pink-head { background: var(--pink); }
.head-en { font-family: var(--font-num); font-size: 12px; letter-spacing: 0.12em; }
.table-scroll { overflow-x: auto; }
table { width: 100%; min-width: 560px; border-collapse: collapse; font-size: 15px; }
th { padding: 10px 14px; color: var(--paper); background: var(--ink); font-size: 13px; font-weight: 900; text-align: left; }
td { padding: 11px 14px; border-top: 2px solid var(--ink); vertical-align: middle; }
td.name { font-weight: 900; }
tr.current { background: var(--yellow); }
.mini { display: inline-flex; align-items: center; gap: 8px; }
.mini-bar { display: block; width: 64px; height: 14px; overflow: hidden; border: 2px solid var(--ink); border-radius: 999px; background: var(--paper); }
.mini-bar span { display: block; height: 100%; background: var(--ink); }
.go { color: var(--ink); white-space: nowrap; }
.sr-only { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); white-space: nowrap; }

.tips { display: flex; flex: 1 1 280px; flex-direction: column; gap: 12px; padding: 22px; border: var(--border-thick); border-radius: var(--radius-lg); background: var(--yellow); box-shadow: var(--shadow-md); }
.tips-tag { align-self: flex-start; color: var(--paper); background: var(--ink); box-shadow: none; }
.tips p { margin: 0; font-weight: 700; }
.tips .tips-big { font-family: var(--font-display); font-size: 1.9rem; font-weight: 400; line-height: 1.25; }

.progress-title { margin: 40px 0 14px; font-family: var(--font-display); font-size: 2rem; font-weight: 400; }
</style>
