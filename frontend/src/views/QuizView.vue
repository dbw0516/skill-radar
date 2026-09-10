<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '@/api/client'
import { useUserProfileStore } from '@/stores/userProfile'
import { useAuthStore } from '@/stores/auth'

// 对应④资料与测评匹配引擎：学习资料 + 在线测评答题。
// 提交后端会判分，通过时把 user_skills.status 改成 quiz_verified——
// 这就是设计文档里反复提到的"回写技能画像"闭环，回②差距分析页刷新一下就能看到变化。
//
// 没带 skillId 直接进来这页（比如点导航栏的"在线测评"）时，显示一个技能选择器，
// 不能就晾着用户不知道该干嘛。
const route = useRoute()
const router = useRouter()
const profile = useUserProfileStore()
const auth = useAuthStore()

const skillId = computed(() => (route.query.skillId ? Number(route.query.skillId) : null))
const skillName = computed(() => route.query.name || '')

// --- 技能选择器（没有 skillId 时用） ---
const pickerSkills = ref([])
const pickerLoading = ref(false)
const pickerError = ref('')

async function loadPicker() {
  pickerLoading.value = true
  pickerError.value = ''
  try {
    if (profile.targetCategoryId) {
      // 有目标岗位：只列还没掌握的，最实用
      const gap = await api.gapAnalysis(profile.targetCategoryId, auth.userId)
      pickerSkills.value = gap.filter((g) => !g.mastered)
    } else {
      // 没选目标岗位：列全部技能，一样能测
      const all = await api.listSkills()
      pickerSkills.value = all.map((s) => ({ skillId: s.id, name: s.name, domain: s.domain }))
    }
  } catch (e) {
    pickerError.value = e.message
  } finally {
    pickerLoading.value = false
  }
}

function pick(s) {
  router.push({ path: '/quiz', query: { skillId: s.skillId, name: s.name } })
}

// --- 具体某个技能的测评 ---
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
  if (!skillId.value) {
    loadPicker()
    return
  }
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

    <template v-if="!skillId">
      <p class="hint">选一个技能开始测评{{ profile.targetCategoryId ? '（下面是你目标岗位里还没掌握的）' : '' }}：</p>
      <p v-if="pickerLoading">加载中…</p>
      <p v-else-if="pickerError" class="error">{{ pickerError }}</p>
      <p v-else-if="!pickerSkills.length" class="hint">
        {{ profile.targetCategoryId ? '这个岗位方向的技能你都掌握啦 🎉' : '还没有技能数据。' }}
      </p>
      <div v-else class="chip-row">
        <button v-for="s in pickerSkills" :key="s.skillId" class="skill-chip" @click="pick(s)">{{ s.name }}</button>
      </div>
    </template>

    <template v-else>
      <RouterLink to="/quiz" class="back">← 换一个技能</RouterLink>
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
.back { display: inline-block; margin-bottom: 0.6rem; color: #5b6472; text-decoration: none; font-size: 0.9rem; }
.skill-title { margin: 0.25rem 0 1rem; color: #2b6e5c; }
.chip-row { display: flex; flex-wrap: wrap; gap: 0.5rem; }
.skill-chip { padding: 0.4rem 0.9rem; border-radius: 999px; border: 1px solid #dbdee4; background: #fff; cursor: pointer; font-size: 0.9rem; }
.skill-chip:hover { border-color: #2b6e5c; color: #2b6e5c; }
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
