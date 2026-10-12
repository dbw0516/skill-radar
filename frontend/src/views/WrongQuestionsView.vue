<script setup>
import { ref, computed, onMounted } from 'vue'
import { api } from '@/api/client'
import { useAuthStore } from '@/stores/auth'

// 错题本 + 间隔复习：答错的题进 wrong_questions，之后按 1 天 / 3 天 / 7 天到期复习，
// 到期答对 3 次才移出（规则见后端 WrongQuestionService）。这里展示每道题的巩固进度，
// "开始复习"跳到测评页的复习模式（/quiz?mode=review）答题。
const auth = useAuthStore()
const items = ref([])
const loading = ref(true)
const error = ref('')

function typeLabel(type) {
  return { single_choice: '选择题', fill_blank: '填空题', short_answer: '简答题' }[type] || ''
}

function parseOptions(raw) {
  try {
    return JSON.parse(raw)
  } catch {
    return []
  }
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    items.value = await api.listWrongQuestions(auth.userId)
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
onMounted(load)

const bySkill = computed(() => {
  const map = new Map()
  for (const it of items.value) {
    if (!map.has(it.skillId)) map.set(it.skillId, { skillId: it.skillId, skillName: it.skillName, items: [] })
    map.get(it.skillId).items.push(it)
  }
  return [...map.values()]
})

const dueCount = computed(() => items.value.filter((it) => it.due).length)

function formatDate(t) {
  const d = new Date(t)
  return `${d.getMonth() + 1}月${d.getDate()}日`
}
</script>

<template>
  <section>
    <h1>错题本</h1>
    <p class="hint">答错的题会自动进来，之后按 <strong>1 天 → 3 天 → 7 天</strong> 的间隔提醒你复习，到期答对 3 次就会自动移出——隔一段时间还记得，才是真的会了。</p>
    <div v-if="!loading && items.length" class="due-banner" :class="{ none: !dueCount }">
      <span v-if="dueCount">📅 今天有 <strong>{{ dueCount }}</strong> 道错题到了复习时间</span>
      <span v-else>✓ 今天没有到期的错题，按计划过几天再来</span>
      <RouterLink v-if="dueCount" :to="{ path: '/quiz', query: { mode: 'review' } }" class="start-review">开始复习 →</RouterLink>
    </div>
    <p v-if="loading">加载中…</p>
    <p v-else-if="error" class="error">{{ error }}</p>
    <p v-else-if="!items.length" class="hint empty">还没有错题，继续保持 🎉</p>
    <div v-else class="groups">
      <div v-for="group in bySkill" :key="group.skillId" class="group">
        <div class="group-head">
          <h3>{{ group.skillName || ('技能 #' + group.skillId) }}</h3>
          <span class="group-links">
            <RouterLink :to="{ path: '/quiz', query: { mode: 'review', skillId: group.skillId, name: group.skillName } }" class="retry">
              复习这组 →
            </RouterLink>
            <RouterLink :to="{ path: '/quiz', query: { skillId: group.skillId, name: group.skillName } }" class="retry muted">
              重新测评
            </RouterLink>
          </span>
        </div>

        <div v-for="it in group.items" :key="it.questionId" class="card">
          <p class="qtext">{{ it.questionText }} <span class="qtype">{{ typeLabel(it.type) }}</span></p>

          <ul v-if="it.type === 'single_choice'" class="options">
            <li
              v-for="(opt, oi) in parseOptions(it.options)"
              :key="oi"
              :class="{ correct: opt === it.correctAnswer, chosen: opt === it.yourAnswer && opt !== it.correctAnswer }"
            >
              {{ opt }}
              <span v-if="opt === it.correctAnswer" class="tag ok">正确答案</span>
              <span v-else-if="opt === it.yourAnswer" class="tag bad">你的选择</span>
            </li>
          </ul>
          <template v-else>
            <p class="answer-line"><span class="label">你的作答：</span>{{ it.yourAnswer || '（未作答）' }}</p>
            <p class="answer-line"><span class="label">正确答案：</span>{{ it.correctAnswer || '（暂无）' }}</p>
          </template>

          <div class="meta">
            <span class="stage" :aria-label="`巩固进度 ${it.reviewStage}/3`">
              <i v-for="n in 3" :key="n" :class="{ on: n <= it.reviewStage }"></i>
            </span>
            <span v-if="it.due" class="due">今天该复习了</span>
            <span v-else>下次复习 {{ formatDate(it.nextReviewAt) }}</span>
            <span>· 答错 {{ it.wrongCount }} 次 · 最近一次 {{ new Date(it.lastWrongAt).toLocaleString() }}</span>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
h1 { margin-bottom: 4px; }
.hint { color: var(--color-muted); margin: 12px 0 20px; }
.hint.empty { padding: 18px; text-align: center; }
.error { margin-top: 20px; padding: 12px 14px; border-radius: var(--radius-md); background: color-mix(in srgb, var(--color-danger) 9%, var(--color-surface)); }
.groups { display: grid; gap: 24px; }
.group-head { display: flex; align-items: baseline; justify-content: space-between; gap: 12px; margin-bottom: 10px; }
.group-head h3 { margin: 0; color: var(--color-heading); font-size: 1rem; }
.group-links { display: inline-flex; gap: 14px; align-items: baseline; }
.retry { color: var(--color-primary); font-size: 0.86rem; font-weight: 650; }
.retry.muted { color: var(--color-muted); font-weight: 600; }
.due-banner { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 10px; margin: 0 0 22px; padding: 14px 16px; border: 1px solid color-mix(in srgb, var(--color-warning) 40%, var(--color-border)); border-radius: var(--radius-md); background: color-mix(in srgb, var(--color-warning) 10%, var(--color-surface)); color: var(--color-heading); }
.due-banner.none { border-color: color-mix(in srgb, var(--color-success) 35%, var(--color-border)); background: color-mix(in srgb, var(--color-success) 9%, var(--color-surface)); }
.start-review { display: inline-flex; align-items: center; min-height: 40px; padding: 0 16px; border-radius: var(--radius-md); background: var(--color-primary); color: var(--color-on-primary); font-weight: 700; text-decoration: none; }
.start-review:hover { color: var(--color-on-primary); }
.stage { display: inline-flex; gap: 3px; margin-right: 2px; }
.stage i { width: 9px; height: 9px; border-radius: 50%; border: 1px solid var(--color-border); background: var(--color-surface); }
.stage i.on { border-color: var(--color-success); background: var(--color-success); }
.due { color: var(--color-warning); font-weight: 700; }
.card { margin-bottom: 12px; padding: 16px; border: 1px solid var(--color-border); border-radius: var(--radius-md); background: var(--color-surface-raised); }
.qtext { color: var(--color-heading); font-weight: 700; margin: 0 0 10px; }
.qtype { color: var(--color-muted); font-weight: 400; font-size: 0.75rem; }
.options { list-style: none; margin: 0 0 10px; padding: 0; display: grid; gap: 6px; }
.options li { display: flex; align-items: center; gap: 8px; padding: 8px 10px; border: 1px solid var(--color-border); border-radius: var(--radius-sm); font-size: 0.9rem; }
.options li.correct { border-color: color-mix(in srgb, var(--color-success) 40%, var(--color-border)); background: color-mix(in srgb, var(--color-success) 10%, var(--color-surface)); }
.options li.chosen { border-color: color-mix(in srgb, var(--color-danger) 40%, var(--color-border)); background: color-mix(in srgb, var(--color-danger) 8%, var(--color-surface)); }
.tag { margin-left: auto; padding: 2px 7px; border-radius: var(--radius-sm); font-size: 0.7rem; font-weight: 700; }
.tag.ok { background: color-mix(in srgb, var(--color-success) 18%, transparent); color: var(--color-success); }
.tag.bad { background: color-mix(in srgb, var(--color-danger) 18%, transparent); color: var(--color-danger); }
.answer-line { margin: 0 0 6px; font-size: 0.9rem; }
.answer-line .label { color: var(--color-muted); margin-right: 4px; }
.meta { display: flex; flex-wrap: wrap; align-items: center; gap: 6px; margin: 10px 0 0; color: var(--color-muted); font-size: 0.78rem; }
</style>
