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
// 两种模式：
//  - 正式测评（默认）：没带 skillId 进来时先显示技能选择器；
//  - 错题复习（?mode=review）：题目来自错题本，带 skillId 出该技能的错题，不带就出今天到期的；
//    复习只推进间隔复习进度，不认证技能。
// 交卷走 SSE 流式接口，边判边显示"已判 3/8 题"；结果逐题展示对错、正确答案和 AI 点评。
const route = useRoute()
const router = useRouter()
const profile = useUserProfileStore()
const auth = useAuthStore()

const reviewMode = computed(() => route.query.mode === 'review')
const skillId = computed(() => (route.query.skillId ? Number(route.query.skillId) : null))
const skillName = computed(() => route.query.name || '')

// --- 技能选择器（正式测评且没有 skillId 时用） ---
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

// --- 具体某个技能的测评 / 错题复习 ---
const resources = ref([])
const viewedResourceIds = ref(new Set())
const questions = ref([])
// questionId -> 选择题存 selectedIndex(Number)，填空题/简答题存答案文本(String)
const answers = reactive({})
const result = ref(null)
const loading = ref(false)
const submitting = ref(false)
const gradingProgress = ref(null) // { done, total }
const error = ref('')
// 提交后进度条从"提交前"的值动画增长到"提交后"——先渲染旧值，下一帧再设新值触发 CSS transition
const animatedProgress = ref(0)

const showQuiz = computed(() => reviewMode.value || skillId.value)
const detailsById = computed(() => {
  const map = {}
  for (const d of result.value?.details || []) map[d.questionId] = d
  return map
})

function parseOptions(raw) {
  try {
    return JSON.parse(raw)
  } catch {
    return []
  }
}

function typeLabel(type) {
  return { single_choice: '选择题', fill_blank: '填空题', short_answer: '简答题' }[type] || ''
}

function resetAnswers() {
  result.value = null
  gradingProgress.value = null
  Object.keys(answers).forEach((k) => delete answers[k])
}

async function load() {
  if (!showQuiz.value) {
    loadPicker()
    return
  }
  loading.value = true
  error.value = ''
  resetAnswers()
  try {
    if (reviewMode.value) {
      questions.value = await api.reviewQuestions(auth.userId, skillId.value)
      resources.value = []
    } else {
      const [res, qs, viewed] = await Promise.all([
        api.skillResources(skillId.value),
        api.skillQuestions(skillId.value),
        api.listViewedResources(auth.userId).catch(() => []),
      ])
      resources.value = res
      questions.value = qs
      viewedResourceIds.value = new Set(viewed)
    }
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
onMounted(load)
watch(() => [skillId.value, reviewMode.value], load)

// 点开资料就记一次"已读"，计入这个技能的部分进度；上报失败不影响用户看资料
function markViewed(r) {
  if (viewedResourceIds.value.has(r.id)) return
  viewedResourceIds.value = new Set([...viewedResourceIds.value, r.id])
  api.markResourceViewed(auth.userId, r.id).catch(() => {})
}

const allAnswered = computed(() =>
  questions.value.length > 0 && questions.value.every((q) => {
    const v = answers[q.id]
    if (q.type === 'fill_blank' || q.type === 'short_answer') return typeof v === 'string' && v.trim().length > 0
    return v !== undefined
  })
)

function answerPayload(q) {
  if (q.type === 'fill_blank' || q.type === 'short_answer') return { questionId: q.id, answerText: answers[q.id] }
  return { questionId: q.id, selectedIndex: answers[q.id] }
}

const hasShortAnswer = computed(() => questions.value.some((q) => q.type === 'short_answer'))
const submitLabel = computed(() => {
  if (!submitting.value) return reviewMode.value ? '提交复习' : '提交测评'
  const p = gradingProgress.value
  if (!p) return '提交中…'
  return hasShortAnswer.value ? `AI 判分中 ${p.done}/${p.total}…` : `判分中 ${p.done}/${p.total}…`
})

async function submit() {
  submitting.value = true
  gradingProgress.value = null
  error.value = ''
  try {
    result.value = await api.submitQuizStream({
      userId: auth.userId,
      skillId: skillId.value,
      mode: reviewMode.value ? 'review' : 'quiz',
      answers: questions.value.map(answerPayload),
    }, (p) => { gradingProgress.value = p })
    if (result.value.passed) {
      profile.markMastered(skillId.value)
    }
    animatedProgress.value = result.value.progressBefore ?? 0
    requestAnimationFrame(() => requestAnimationFrame(() => {
      animatedProgress.value = result.value?.progressAfter ?? 0
    }))
    window.scrollTo({ top: 0, behavior: 'smooth' })
  } catch (e) {
    error.value = e.message
  } finally {
    submitting.value = false
  }
}

function optionClass(q, oi) {
  const d = detailsById.value[q.id]
  if (!d) return {}
  const opt = parseOptions(q.options)[oi]
  return { correct: opt === d.correctAnswer, chosen: answers[q.id] === oi && opt !== d.correctAnswer }
}
</script>

<template>
  <section>
    <h1>{{ reviewMode ? '错题复习' : '在线测评' }}</h1>
    <p v-if="!reviewMode" class="ai-banner">
      <span class="ai-badge">AI</span>
      <span>选择题、填空题由系统精确判分；<strong>简答题交给 DeepSeek 大模型</strong>，对照参考答案的要点逐条比对，交卷后给出点评和要点覆盖分析。</span>
    </p>

    <template v-if="!showQuiz">
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
      <RouterLink :to="reviewMode ? '/mistakes' : '/quiz'" class="back">
        {{ reviewMode ? '← 返回错题本' : '← 换一个技能' }}
      </RouterLink>
      <h2 class="skill-title">
        {{ reviewMode ? (skillName ? skillName + ' · 错题' : '今日到期的错题') : (skillName || ('技能 #' + skillId)) }}
      </h2>
      <p v-if="reviewMode" class="hint">按 1 天 / 3 天 / 7 天的间隔复习，到期答对 3 次就移出错题本。复习不影响技能认证。</p>
      <p v-if="loading">加载中…</p>
      <p v-else-if="error && !questions.length" class="error">{{ error }}</p>
      <template v-else>
        <div v-if="resources.length" class="resources">
          <h3>学习资料 <span class="res-count">已读 {{ resources.filter((r) => viewedResourceIds.has(r.id)).length }}/{{ resources.length }}</span></h3>
          <ul>
            <li v-for="r in resources" :key="r.id">
              <a :href="r.url" target="_blank" rel="noopener" @click="markViewed(r)" @auxclick="markViewed(r)">{{ r.title }}</a>
              <span class="res-meta">
                <span v-if="viewedResourceIds.has(r.id)" class="read">✓ 已读</span>
                <span class="type">{{ r.type }}</span>
              </span>
            </li>
          </ul>
        </div>

        <div v-if="result" class="result feedback-focus" :class="{ pass: result.passed || (result.mode === 'review' && result.correctCount === result.totalCount), fail: !result.passed && !(result.mode === 'review' && result.correctCount === result.totalCount) }">
          <template v-if="result.mode === 'review'">
            <strong>复习完成</strong>
            答对 {{ result.correctCount }} / {{ result.totalCount }} 题——每道题的复习进度见下方。
          </template>
          <template v-else>
            <strong>{{ result.passed ? '通过 ✓' : '未通过' }}</strong>
            答对 {{ result.correctCount }} / {{ result.totalCount }} 题
            <span v-if="result.passed">——已回写到你的技能画像，去<RouterLink to="/gap">技能差距</RouterLink>页能看到更新。</span>
            <span v-else>——下面有每道题的点评，答错的已放进错题本。</span>
          </template>
          <p v-if="result.encouragement" class="cheer">{{ result.encouragement }}</p>
          <div v-if="result.progressAfter != null" class="mini-progress">
            <div class="mini-bar" role="progressbar" aria-valuemin="0" aria-valuemax="100" :aria-valuenow="result.progressAfter">
              <div class="mini-fill" :style="{ width: animatedProgress + '%' }"></div>
            </div>
            <span class="mini-pct">{{ result.progressAfter }}%</span>
            <RouterLink to="/profile" class="mini-link">查看学习进度 →</RouterLink>
          </div>
          <p v-if="result.qualifiedAfter != null && result.postingsWithSkills" class="jobs-line">
            匹配度达标的可投岗位：
            <template v-if="result.qualifiedAfter > result.qualifiedBefore">
              <s>{{ result.qualifiedBefore }}</s> → <strong>{{ result.qualifiedAfter }}</strong>
            </template>
            <strong v-else>{{ result.qualifiedAfter }}</strong>
            / {{ result.postingsWithSkills }} 条
          </p>
          <div v-if="result.newBadges?.length" class="new-badges">
            <span class="new-badges-title">🎉 解锁新徽章</span>
            <span v-for="b in result.newBadges" :key="b.key" class="new-badge" :title="b.description">
              {{ b.icon }} {{ b.name }}
            </span>
          </div>
          <div class="result-actions">
            <button type="button" class="ghost" @click="load">{{ reviewMode ? '再复习一组' : '再测一次' }}</button>
            <RouterLink v-if="result.correctCount < result.totalCount" to="/mistakes" class="ghost-link">去错题本 →</RouterLink>
          </div>
        </div>

        <div v-if="questions.length" class="quiz">
          <h3>{{ reviewMode ? '复习题' : '测评题' }}（{{ questions.length }} 题）</h3>
          <div
            v-for="(q, qi) in questions" :key="q.id"
            class="question" :class="detailsById[q.id] ? (detailsById[q.id].correct ? 'q-right' : 'q-wrong') : ''"
          >
            <p class="qtext">
              {{ qi + 1 }}. {{ q.questionText }} <span class="qtype">{{ typeLabel(q.type) }}</span>
              <span v-if="q.type === 'short_answer'" class="ai-badge">AI 判分</span>
              <span v-if="detailsById[q.id]" class="verdict" :class="detailsById[q.id].correct ? 'ok' : 'bad'">
                {{ detailsById[q.id].correct ? '✓ 答对' : '✗ 答错' }}
              </span>
            </p>

            <template v-if="q.type === 'single_choice'">
              <label v-for="(opt, oi) in parseOptions(q.options)" :key="oi" class="option" :class="optionClass(q, oi)">
                <input type="radio" :name="'q' + q.id" :value="oi" v-model="answers[q.id]" :disabled="!!result" />
                {{ opt }}
                <span v-if="optionClass(q, oi).correct" class="opt-tag ok">正确答案</span>
                <span v-else-if="optionClass(q, oi).chosen" class="opt-tag bad">你的选择</span>
              </label>
            </template>

            <template v-else-if="q.type === 'fill_blank'">
              <input type="text" class="fill-input" placeholder="填入你的答案" v-model="answers[q.id]" :disabled="!!result" />
              <p v-if="detailsById[q.id] && !detailsById[q.id].correct" class="answer-line">
                <span class="label">正确答案：</span>{{ detailsById[q.id].correctAnswer || '（暂无）' }}
              </p>
            </template>

            <template v-else-if="q.type === 'short_answer'">
              <textarea
                class="short-input"
                rows="3"
                placeholder="填入你的答案，提交后由 AI 对照参考答案要点判分"
                v-model="answers[q.id]"
                :disabled="!!result"
              ></textarea>
              <div v-if="detailsById[q.id]?.feedback" class="ai-feedback">
                <p class="ai-label"><span class="ai-badge">AI</span>点评</p>
                <p class="ai-text">{{ detailsById[q.id].feedback }}</p>
                <details v-if="detailsById[q.id].keyPointsCovered" class="ai-detail">
                  <summary>要点覆盖分析</summary>
                  <p>{{ detailsById[q.id].keyPointsCovered }}</p>
                </details>
              </div>
              <div v-if="result" class="reference">
                <p class="reference-label">参考答案／要点</p>
                <p class="reference-text">{{ q.referenceAnswer || '（暂无参考答案）' }}</p>
              </div>
            </template>

            <p v-if="detailsById[q.id]?.reviewNote" class="review-note">📌 {{ detailsById[q.id].reviewNote }}</p>
          </div>
          <p v-if="error" class="error">{{ error }}</p>
          <div v-if="!result" class="submit-row">
            <button class="submit-button" :disabled="!allAnswered || submitting" @click="submit">{{ submitLabel }}</button>
            <div v-if="submitting && gradingProgress" class="grading-bar" :class="{ ai: hasShortAnswer }" aria-hidden="true">
              <div class="grading-fill" :style="{ width: (gradingProgress.done / gradingProgress.total * 100) + '%' }"></div>
            </div>
          </div>
        </div>
        <p v-else-if="reviewMode" class="hint">今天没有到期的错题 🎉 按计划过几天再来巩固，或者去<RouterLink to="/mistakes">错题本</RouterLink>挑一组提前复习。</p>
        <p v-else class="hint">这个技能还没有测评题，团队正在补充中。</p>
      </template>
    </template>
  </section>
</template>

<style scoped>
.ai-banner { display: flex; align-items: flex-start; gap: 12px; margin: 4px 0 20px; padding: 14px 18px; border: var(--border-thick); border-radius: var(--radius-md); background: var(--ai-soft); box-shadow: 4px 4px 0 var(--ai); font-weight: 700; }
.ai-banner .ai-badge { margin-top: 2px; }
.back { margin-bottom: 4px; }
.skill-title { margin: 4px 0 20px; color: var(--ink); font-family: var(--font-display); font-size: 2rem; font-weight: 400; }
.chip-row { margin-top: 18px; }
.resources { margin-top: 20px; padding: 18px 20px; box-shadow: var(--shadow-sm); }
.resources h3, .quiz h3 { margin: 0 0 10px; color: var(--ink); font-size: 1.1rem; font-weight: 900; }
.res-count { margin-left: 8px; padding: 1px 8px; border: 2px solid var(--ink); border-radius: 999px; background: var(--yellow); font-size: 0.78rem; font-weight: 900; }
.resources ul { list-style: none; padding: 0; margin: 0; }
.resources li { display: flex; justify-content: space-between; gap: 12px; align-items: baseline; padding: 10px 0; border-top: 2px solid var(--ink); }
.resources li:first-child { border-top: 0; }
.res-meta { display: inline-flex; gap: 8px; align-items: baseline; flex: 0 0 auto; }
.resources .type { color: var(--color-muted); font-size: 0.78rem; font-weight: 700; }
.read { color: var(--color-success); font-size: 0.78rem; font-weight: 900; }
.quiz { margin-top: 22px; padding: 20px; box-shadow: var(--shadow-sm); }
.question { margin: 16px 0; padding: 18px; border: var(--border-thick); border-radius: var(--radius-md); }
.question.q-right { background: #e3f8ec; box-shadow: 4px 4px 0 var(--green); }
.question.q-wrong { background: #ffe8ec; box-shadow: 4px 4px 0 var(--red); }
.qtext { display: flex; flex-wrap: wrap; align-items: center; gap: 6px 8px; margin: 0 0 12px; color: var(--ink); font-size: 1.05rem; font-weight: 900; }
.qtype { padding: 1px 8px; border: 2px solid var(--ink); border-radius: 6px; background: var(--paper); color: var(--ink); font-size: 0.75rem; font-weight: 800; }
.verdict { padding: 2px 10px; border: 2.5px solid var(--ink); border-radius: 999px; font-size: 0.8rem; font-weight: 900; white-space: nowrap; }
.verdict.ok, .opt-tag.ok { background: var(--green); color: var(--ink); }
.verdict.bad, .opt-tag.bad { background: var(--red); color: var(--ink); }
.option { display: flex; align-items: center; gap: 10px; min-height: 44px; margin: 6px 0; padding: 8px 12px; border: 2.5px solid transparent; border-radius: 10px; font-weight: 700; cursor: pointer; }
.option:hover { border-color: var(--ink); background: var(--color-soft); }
.option.correct { border-color: var(--ink); background: #c9f2dc; }
.option.chosen { border-color: var(--ink); background: #ffd0d8; }
.opt-tag { margin-left: auto; padding: 1px 8px; border: 2px solid var(--ink); border-radius: 6px; font-size: 0.72rem; font-weight: 900; white-space: nowrap; }
.fill-input, .short-input { width: 100%; font: inherit; }
.short-input { resize: vertical; }
.short-input:focus { box-shadow: 4px 4px 0 var(--ai); }
.answer-line { margin: 10px 0 0; font-weight: 700; }
.answer-line .label { color: var(--color-muted); margin-right: 4px; }
.ai-feedback { margin-top: 14px; padding: 14px 16px; border: var(--border-thick); border-radius: var(--radius-md); background: var(--ai-soft); box-shadow: 4px 4px 0 var(--ai); }
.ai-label, .reference-label { display: flex; align-items: center; gap: 8px; margin: 0 0 8px; color: var(--ink); font-size: 0.95rem; font-weight: 900; }
.ai-text { margin: 0; color: var(--ink); font-weight: 600; }
.ai-detail { margin-top: 10px; font-size: 0.9rem; color: var(--color-text); }
.ai-detail summary { cursor: pointer; font-weight: 900; }
.ai-detail p { margin: 6px 0 0; white-space: pre-wrap; }
.reference { margin-top: 12px; padding: 12px 16px; border: 2.5px dashed var(--ink); border-radius: var(--radius-md); background: var(--paper); }
.reference-text { margin: 0; white-space: pre-wrap; color: var(--color-text); }
.review-note { margin: 10px 0 0; color: var(--color-muted); font-size: 0.85rem; font-weight: 700; }
.submit-row { display: grid; gap: 12px; margin-top: 8px; }
.submit-button { justify-self: start; min-height: 52px; padding: 0 28px; font-size: 1.05rem; }
.grading-bar { height: 22px; max-width: 360px; overflow: hidden; border: var(--border-thick); border-radius: 999px; background: var(--paper); }
.grading-fill { height: 100%; background: var(--ink); transition: width 300ms ease; }
.grading-bar.ai .grading-fill { background: repeating-linear-gradient(-45deg, var(--ai) 0 8px, #7a55ff 8px 16px); background-size: 22px 22px; animation: grading-stripes 700ms linear infinite; }
@keyframes grading-stripes { to { background-position: 22px 0; } }
.result { margin: 22px 0; padding: 18px 20px; border: var(--border-thick); border-radius: var(--radius-md); color: var(--ink); font-size: 0.98rem; font-weight: 700; box-shadow: var(--shadow-md); }
.result strong { margin-right: 6px; font-family: var(--font-display); font-size: 1.6rem; font-weight: 400; }
.feedback-focus { animation: feedback-focus-in 360ms cubic-bezier(.22, 1, .36, 1) both; }
.result.pass { background: var(--green); }
.result.fail { background: var(--orange); }
.cheer { margin: 10px 0 0; color: var(--ink); font-weight: 900; }
.mini-progress { display: flex; align-items: center; gap: 10px; margin-top: 12px; }
.mini-bar { flex: 1; height: 18px; overflow: hidden; border: var(--border-thick); border-radius: 999px; background: var(--paper); }
.mini-fill { height: 100%; background: var(--ink); transition: width 900ms cubic-bezier(.22, 1, .36, 1); }
.mini-pct { color: var(--ink); font-family: var(--font-num); font-size: 1.2rem; }
.mini-link { color: var(--ink); font-size: 0.9rem; white-space: nowrap; }
.jobs-line { margin: 10px 0 0; color: var(--ink); }
.jobs-line s { opacity: 0.6; }
.jobs-line strong { font-family: var(--font-num); font-size: 1.1rem; }
.new-badges { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; margin-top: 12px; }
.new-badges-title { color: var(--ink); font-weight: 900; }
.new-badge { padding: 4px 12px; border: var(--border-thick); border-radius: 999px; background: var(--yellow); color: var(--ink); font-size: 0.88rem; font-weight: 900; box-shadow: var(--shadow-sm); animation: badge-pop 480ms cubic-bezier(.34, 1.56, .64, 1) both; }
.result-actions { display: flex; flex-wrap: wrap; align-items: center; gap: 14px; margin-top: 16px; }
.ghost { min-height: 44px; color: var(--ink); background: var(--paper); box-shadow: var(--shadow-sm); }
.ghost:hover:not(:disabled) { box-shadow: 5px 5px 0 var(--ink); }
.ghost:active:not(:disabled) { box-shadow: 1px 1px 0 var(--ink); }
.ghost-link { color: var(--ink); font-weight: 900; }
@keyframes badge-pop { from { opacity: 0; transform: scale(.6); } to { opacity: 1; transform: scale(1); } }
.error { margin-top: 20px; padding: 12px 14px; border: var(--border-thick); border-radius: var(--radius-md); background: #ffe3e8; }
.hint { color: var(--color-muted); font-weight: 700; }
@keyframes feedback-focus-in { from { opacity: 0; transform: translateY(10px) scale(.985); } to { opacity: 1; transform: translateY(0) scale(1); } }
</style>
