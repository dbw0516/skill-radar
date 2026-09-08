<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '@/api/client'
import { useUserProfileStore } from '@/stores/userProfile'
import { useAuthStore } from '@/stores/auth'

// 对应④资料与测评匹配引擎：学习资料 + 在线测评答题。
// 提交后端会判分，通过时把 user_skills.status 改成 quiz_verified——
// 这就是设计文档里反复提到的"回写技能画像"闭环，回②差距分析页刷新一下就能看到变化。
const route = useRoute()
const profile = useUserProfileStore()
const auth = useAuthStore()

const skillId = computed(() => Number(route.query.skillId))
const skillName = computed(() => route.query.name || '')

const resources = ref([])
const questions = ref([])
const answers = reactive({}) // questionId -> selectedIndex
const result = ref(null)
const loading = ref(false)
const submitting = ref(false)
const error = ref('')

function parseOptions(raw) {
  try {
    return JSON.parse(raw)
  } catch {
    return []
  }
}

async function load() {
  if (!skillId.value) return
  loading.value = true
  error.value = ''
  result.value = null
  Object.keys(answers).forEach((k) => delete answers[k])
  try {
    const [res, qs] = await Promise.all([
      api.skillResources(skillId.value),
      api.skillQuestions(skillId.value),
    ])
    resources.value = res
    questions.value = qs
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
onMounted(load)
watch(skillId, load)

const allAnswered = computed(() =>
  questions.value.length > 0 && questions.value.every((q) => answers[q.id] !== undefined)
)

async function submit() {
  submitting.value = true
  error.value = ''
  try {
    result.value = await api.submitQuiz({
      userId: auth.userId,
      skillId: skillId.value,
      answers: questions.value.map((q) => ({ questionId: q.id, selectedIndex: answers[q.id] })),
    })
    if (result.value.passed) {
      profile.markMastered(skillId.value)
    }
  } catch (e) {
    error.value = e.message
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section>
    <h1>在线测评</h1>
    <p v-if="!skillId">从"学习路径"页点一个技能进来，这里会显示对应的学习资料和测评题。</p>
    <template v-else>
      <h2 class="skill-title">{{ skillName || ('技能 #' + skillId) }}</h2>
      <p v-if="loading">加载中…</p>
      <p v-else-if="error" class="error">{{ error }}</p>
      <template v-else>
        <div v-if="resources.length" class="resources">
          <h3>学习资料</h3>
          <ul>
            <li v-for="r in resources" :key="r.id">
              <a :href="r.url" target="_blank" rel="noopener">{{ r.title }}</a>
              <span class="type">{{ r.type }}</span>
            </li>
          </ul>
        </div>

        <div v-if="result" class="result" :class="{ pass: result.passed, fail: !result.passed }">
          <strong>{{ result.passed ? '通过 ✓' : '未通过' }}</strong>
          答对 {{ result.correctCount }} / {{ result.totalCount }} 题
          <span v-if="result.passed">——已回写到你的技能画像，去<RouterLink to="/gap">技能差距</RouterLink>页能看到更新。</span>
          <span v-else>——再复习一下学习资料，随时可以重新测评。</span>
        </div>

        <div v-if="questions.length" class="quiz">
          <h3>测评题（{{ questions.length }} 题）</h3>
          <div v-for="(q, qi) in questions" :key="q.id" class="question">
            <p class="qtext">{{ qi + 1 }}. {{ q.questionText }}</p>
            <label v-for="(opt, oi) in parseOptions(q.options)" :key="oi" class="option">
              <input type="radio" :name="'q' + q.id" :value="oi" v-model="answers[q.id]" />
              {{ opt }}
            </label>
          </div>
          <button :disabled="!allAnswered || submitting" @click="submit">
            {{ submitting ? '提交中…' : '提交测评' }}
          </button>
        </div>
        <p v-else class="hint">这个技能还没有测评题，团队正在补充中。</p>
      </template>
    </template>
  </section>
</template>

<style scoped>
.skill-title { margin: 0.25rem 0 1rem; color: #2b6e5c; }
.resources ul, .quiz { margin-top: 0.5rem; }
.resources ul { list-style: none; padding: 0; }
.resources li { display: flex; gap: 0.6rem; align-items: baseline; padding: 0.25rem 0; }
.resources .type { font-size: 0.75rem; color: #8891a0; }
.question { margin: 1rem 0; }
.qtext { font-weight: 600; margin-bottom: 0.4rem; }
.option { display: block; padding: 0.2rem 0; cursor: pointer; }
button { margin-top: 1rem; padding: 0.5rem 1.25rem; background: #2b6e5c; color: #fff; border: none; border-radius: 6px; cursor: pointer; font-size: 0.95rem; }
button:disabled { background: #b7c4bf; cursor: not-allowed; }
.result { margin: 1rem 0; padding: 0.75rem 1rem; border-radius: 8px; font-size: 0.9rem; }
.result.pass { background: #e3f0eb; color: #2b6e5c; }
.result.fail { background: #f6e8da; color: #ae5f1c; }
.error { color: #b3261e; }
.hint { color: #8891a0; }
</style>
