<script setup>
import { ref, computed, onMounted } from 'vue'
import { api } from '@/api/client'
import { useAuthStore } from '@/stores/auth'

// 错题本：QuizService 判分时把每道答错的题记进 wrong_questions，后来答对了会自动从表里删掉——
// 所以这里读到的永远是"现在还没订正"的题，纯只读展示，练回去还是得去"在线测评"页重新提交。
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
</script>

<template>
  <section>
    <h1>错题本</h1>
    <p class="hint">测评时答错的题会自动出现在这里；同一道题后来答对了，会自动从这个列表移出，不用手动清理。</p>
    <p v-if="loading">加载中…</p>
    <p v-else-if="error" class="error">{{ error }}</p>
    <p v-else-if="!items.length" class="hint empty">还没有错题，继续保持 🎉</p>
    <div v-else class="groups">
      <div v-for="group in bySkill" :key="group.skillId" class="group">
        <div class="group-head">
          <h3>{{ group.skillName || ('技能 #' + group.skillId) }}</h3>
          <RouterLink :to="{ path: '/quiz', query: { skillId: group.skillId, name: group.skillName } }" class="retry">
            重新测评 →
          </RouterLink>
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

          <p class="meta">答错 {{ it.wrongCount }} 次 · 最近一次 {{ new Date(it.lastWrongAt).toLocaleString() }}</p>
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
.retry { color: var(--color-primary); font-size: 0.86rem; font-weight: 650; }
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
.meta { margin: 10px 0 0; color: var(--color-muted); font-size: 0.78rem; }
</style>
