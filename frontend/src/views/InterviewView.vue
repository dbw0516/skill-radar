<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '@/api/client'
import { useAuthStore } from '@/stores/auth'
import { useUserProfileStore } from '@/stores/userProfile'

// AI 模拟面试：DeepSeek 扮演目标岗位的面试官，5 道题、每题最多追问一次，结束后出总评和逐题点评。
// 面试状态存在后端（interview_sessions），URL 带 ?id= 就能接着上次没答完的继续，刷新也不丢。
//
// 限时模式：面试官每问一句（新题或追问），先进入"思考"倒计时（不能输入，可提前开始），
// 再进入"作答"倒计时，时间到自动提交已经写下的内容——还原真实面试"想几秒就得开口"的压力。
// 计时只在前端做；每次回答把用时/限时/是否超时带给后端，AI 点评和总评会参考。
// 选了哪一档按面试 id 记在 localStorage，刷新或中断回来仍是限时（当前这一问会重新计时）。
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const profile = useUserProfileStore()

// 打字比说话慢，作答时间比口头面试放宽了一些
const PRESETS = {
  off: { label: '不限时', desc: '想多久都行，适合第一次熟悉题目' },
  relaxed: { label: '限时 · 宽松', desc: '每问思考 60 秒，作答 5 分钟', think: 60, answer: 300 },
  standard: { label: '限时 · 标准', desc: '每问思考 30 秒，作答 3 分钟', think: 30, answer: 180 },
  tight: { label: '限时 · 紧张', desc: '每问思考 15 秒，作答 90 秒，接近真实面试节奏', think: 15, answer: 90 },
}
const PRESET_KEY = 'skill-radar-interview-preset'
const sessionPresetKey = (id) => `skill-radar-interview-timer-${id}`

function readStorage(key) {
  try {
    return window.localStorage.getItem(key)
  } catch {
    return null
  }
}
function writeStorage(key, value) {
  try {
    window.localStorage.setItem(key, value)
  } catch {
    // 隐私模式等拿不到 localStorage：不影响面试，只是刷新后会变回不限时
  }
}

const session = ref(null)
const history = ref([])
const loading = ref(true)
const busy = ref(false)
const error = ref('')
const draft = ref('')
const chatEl = ref(null)
const answerEl = ref(null)

const presetChoice = ref(PRESETS[readStorage(PRESET_KEY)] ? readStorage(PRESET_KEY) : 'standard')
const sessionPreset = ref('off')
const timing = computed(() => (PRESETS[sessionPreset.value]?.answer ? PRESETS[sessionPreset.value] : null))

const sessionId = computed(() => (route.query.id ? Number(route.query.id) : null))
const finished = computed(() => session.value?.status === 'finished')

// ---------- 计时 ----------
const phase = ref(null) // 'think' | 'answer' | null
const deadline = ref(0)
const now = ref(Date.now())
let ticker = null
let answerStartedAt = 0
let retryTiming = null // 提交失败时保留这次的用时信息，重发时沿用

const remaining = computed(() => Math.max(0, Math.ceil((deadline.value - now.value) / 1000)))
const phaseTotal = computed(() => (phase.value === 'think' ? timing.value?.think : timing.value?.answer) || 1)
const phasePct = computed(() => Math.max(0, Math.min(100, ((deadline.value - now.value) / 1000 / phaseTotal.value) * 100)))

function startTicker() {
  if (!ticker) ticker = setInterval(tick, 250)
}
function stopTimer() {
  phase.value = null
  clearInterval(ticker)
  ticker = null
}
function tick() {
  now.value = Date.now()
  if (remaining.value > 0) return
  if (phase.value === 'think') beginAnswer()
  else if (phase.value === 'answer') timeUp()
}
function beginThink() {
  if (!timing.value || finished.value) return
  phase.value = 'think'
  now.value = Date.now()
  deadline.value = now.value + timing.value.think * 1000
  startTicker()
}
function beginAnswer() {
  if (!timing.value) return
  phase.value = 'answer'
  answerStartedAt = Date.now()
  now.value = answerStartedAt
  deadline.value = answerStartedAt + timing.value.answer * 1000
  startTicker()
  nextTick(() => answerEl.value?.focus())
}
function timeUp() {
  if (busy.value) return
  if (draft.value.trim()) send(null, { timedOut: true })
  else send('（时间到，没有作答）', { timedOut: true })
}
onUnmounted(stopTimer)

// 面试官刚问完（最后一条是面试官的）且面试没结束，就开始这一问的计时
function maybeStartQuestionTimer() {
  const last = session.value?.messages?.at(-1)
  if (timing.value && !finished.value && last?.role === 'interviewer') beginThink()
}

async function load() {
  stopTimer()
  loading.value = true
  error.value = ''
  try {
    if (sessionId.value) {
      session.value = await api.getInterview(sessionId.value, auth.userId)
      sessionPreset.value = readStorage(sessionPresetKey(sessionId.value)) || 'off'
      scrollToBottom()
      maybeStartQuestionTimer()
    } else {
      session.value = null
      history.value = await api.listInterviews(auth.userId)
    }
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
onMounted(load)
watch(sessionId, load)

async function start(preset = presetChoice.value) {
  busy.value = true
  error.value = ''
  try {
    const s = await api.startInterview(auth.userId, profile.targetCategoryId)
    writeStorage(PRESET_KEY, preset)
    writeStorage(sessionPresetKey(s.id), preset)
    router.push({ path: '/interview', query: { id: s.id } })
  } catch (e) {
    error.value = e.message
  } finally {
    busy.value = false
  }
}

function scrollToBottom() {
  nextTick(() => {
    if (chatEl.value) chatEl.value.scrollTop = chatEl.value.scrollHeight
  })
}

async function send(text, { timedOut = false } = {}) {
  const usingDraft = text == null
  const answer = (usingDraft ? draft.value : text).trim()
  if (!answer || busy.value) return
  let timingInfo = retryTiming || {}
  if (timing.value && phase.value) {
    const seconds = phase.value === 'answer' ? Math.round((Date.now() - answerStartedAt) / 1000) : 0
    timingInfo = { seconds: Math.min(seconds, timing.value.answer), timeLimit: timing.value.answer, timedOut }
  }
  stopTimer()
  busy.value = true
  error.value = ''
  // 先把自己的回答显示出来，AI 返回后再整体替换成服务端的对话记录；失败的话撤回并保留输入，方便重发
  session.value.messages = [...session.value.messages, { role: 'candidate', content: answer, pending: true, ...timingInfo }]
  if (usingDraft) draft.value = ''
  scrollToBottom()
  try {
    session.value = await api.answerInterview(session.value.id, auth.userId, answer, timingInfo)
    retryTiming = null
    scrollToBottom()
    maybeStartQuestionTimer()
  } catch (e) {
    session.value.messages = session.value.messages.filter((m) => !m.pending)
    if (usingDraft) draft.value = answer
    retryTiming = timingInfo.timeLimit ? timingInfo : null
    error.value = e.message + '——计时已暂停，可以修改后重新发送'
  } finally {
    busy.value = false
  }
}

async function finishEarly() {
  if (!confirm('确定现在结束面试吗？会按已经回答的题生成评价。')) return
  stopTimer()
  busy.value = true
  error.value = ''
  try {
    session.value = await api.finishInterview(session.value.id, auth.userId)
  } catch (e) {
    error.value = e.message
    maybeStartQuestionTimer()
  } finally {
    busy.value = false
  }
}

function onKeydown(e) {
  if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) send()
}

// 每道题（含追问）总用时，只统计限时作答的回答
const secondsByQuestion = computed(() => {
  const map = {}
  for (const m of session.value?.messages || []) {
    if (m.role === 'candidate' && m.seconds != null) map[m.questionIndex] = (map[m.questionIndex] || 0) + m.seconds
  }
  return map
})

function clock(sec) {
  return `${Math.floor(sec / 60)}:${String(sec % 60).padStart(2, '0')}`
}

function stars(n) {
  return '★'.repeat(n) + '☆'.repeat(5 - n)
}

function formatTime(t) {
  return t ? new Date(t).toLocaleString() : ''
}
</script>

<template>
  <section class="interview">
    <h1 class="title"><span class="ai-badge title-ai">AI</span>模拟面试</h1>
    <p v-if="loading">加载中…</p>

    <!-- 首页：开始新面试 + 历史记录 -->
    <template v-else-if="!sessionId">
      <div class="intro">
        <p class="intro-lead"><span class="ai-badge">AI 面试官</span>由 DeepSeek 大模型扮演面试官，针对你的目标岗位提 5 道技术题，根据你的回答追问，最后给出评分、逐题点评和改进建议。</p>
        <ul>
          <li>像真实面试一样用自己的话回答，讲原理、讲经验都可以</li>
          <li>不会的题直接点"这题不会"，不影响继续</li>
          <li>大约 10~15 分钟，中途可以随时结束</li>
        </ul>
        <fieldset class="modes">
          <legend>作答方式</legend>
          <label v-for="(p, key) in PRESETS" :key="key" class="mode" :class="{ on: presetChoice === key }">
            <input v-model="presetChoice" type="radio" name="preset" :value="key" />
            <span><strong>{{ p.label }}</strong><small>{{ p.desc }}</small></span>
          </label>
        </fieldset>
        <p v-if="!profile.targetCategoryId" class="warn">还没有目标岗位，先<RouterLink to="/onboarding">选一个</RouterLink>再来面试。</p>
        <button v-else type="button" class="start" :disabled="busy" @click="start()">{{ busy ? '准备题目中…' : '开始面试' }}</button>
      </div>
      <p v-if="error" class="error">{{ error }}</p>

      <div v-if="history.length" class="history">
        <h2>面试记录</h2>
        <RouterLink v-for="h in history" :key="h.id" :to="{ path: '/interview', query: { id: h.id } }" class="history-row">
          <span class="h-name">{{ h.categoryName }}</span>
          <span class="h-meta">{{ formatTime(h.createdAt) }} · 答了 {{ h.answeredQuestions }}/{{ h.totalQuestions }} 题</span>
          <span v-if="h.status === 'finished'" class="h-score">{{ h.overallScore ?? '—' }} 分</span>
          <span v-else class="h-cont">继续 →</span>
        </RouterLink>
      </div>
    </template>

    <!-- 一场具体的面试 -->
    <template v-else-if="session">
      <div class="session-head">
        <RouterLink to="/interview" class="back">← 返回</RouterLink>
        <span class="topic">「{{ session.categoryName }}」</span>
        <span v-if="!finished" class="q-progress">第 {{ session.currentIndex + 1 }} / {{ session.totalQuestions }} 题</span>
        <span v-if="timing" class="mode-tag">⏱ {{ PRESETS[sessionPreset].label }}</span>
        <button v-if="!finished" type="button" class="link-btn" :disabled="busy" @click="finishEarly">结束面试</button>
      </div>

      <div ref="chatEl" class="chat" aria-live="polite">
        <div v-for="(m, i) in session.messages" :key="i" class="msg" :class="m.role">
          <span class="who"><span v-if="m.role === 'interviewer'" class="ai-badge">AI</span>{{ m.role === 'interviewer' ? '面试官' : '我' }}</span>
          <p class="bubble">{{ m.content }}</p>
          <span v-if="m.role === 'candidate' && m.seconds != null" class="used" :class="{ late: m.timedOut }">
            {{ m.timedOut ? '⏰ 时间到自动提交 · ' : '' }}用时 {{ clock(m.seconds) }} / {{ clock(m.timeLimit) }}
          </span>
        </div>
        <div v-if="busy && !finished" class="msg interviewer">
          <span class="who"><span class="ai-badge">AI</span>面试官</span>
          <p class="bubble typing">正在思考<span>.</span><span>.</span><span>.</span></p>
        </div>
      </div>

      <p v-if="error" class="error">{{ error }}</p>

      <div v-if="!finished" class="composer">
        <div
          v-if="phase" class="timer" :class="[phase, { urgent: phase === 'answer' && remaining <= 30 }]"
          role="timer" :aria-label="`${phase === 'think' ? '思考时间' : '作答剩余时间'} ${clock(remaining)}`"
        >
          <span class="timer-label">{{ phase === 'think' ? '🤔 思考时间' : '✍️ 作答剩余' }}</span>
          <span class="timer-clock">{{ clock(remaining) }}</span>
          <div class="timer-bar"><div class="timer-fill" :style="{ width: phasePct + '%' }"></div></div>
          <button v-if="phase === 'think'" type="button" class="timer-go" @click="beginAnswer">想好了，开始作答</button>
          <span v-else class="timer-hint">时间到会自动提交已写的内容</span>
        </div>
        <textarea
          ref="answerEl"
          v-model="draft" rows="4" :disabled="busy || phase === 'think'"
          :placeholder="phase === 'think' ? '先在脑子里组织一下：结论是什么、分几点讲。思考时间结束后开始作答' : '输入你的回答（Ctrl + Enter 发送）'"
          @keydown="onKeydown"
        ></textarea>
        <div class="composer-actions">
          <button type="button" class="ghost" :disabled="busy" @click="send('这道题我不太会。')">这题不会</button>
          <button type="button" :disabled="busy || !draft.trim()" @click="send()">{{ busy ? '面试官思考中…' : '发送回答' }}</button>
        </div>
      </div>

      <!-- 结束后的评价报告 -->
      <div v-else class="report">
        <div class="report-head">
          <span v-if="session.overallScore != null" class="score-ring" :class="{ good: session.overallScore >= 70 }">{{ session.overallScore }}</span>
          <div>
            <h2><span class="ai-badge">AI</span>面试评价</h2>
            <p>{{ session.report?.summary }}</p>
          </div>
        </div>
        <div class="report-cols" v-if="session.report?.strengths?.length || session.report?.improvements?.length">
          <div>
            <h3>👍 做得好的</h3>
            <ul><li v-for="(s, i) in session.report.strengths" :key="i">{{ s }}</li></ul>
          </div>
          <div>
            <h3>📈 建议提升</h3>
            <ul><li v-for="(s, i) in session.report.improvements" :key="i">{{ s }}</li></ul>
          </div>
        </div>
        <h3>逐题点评</h3>
        <div v-for="q in session.questionReviews" :key="q.index" class="q-review">
          <p class="q-title">
            第 {{ q.index + 1 }} 题 <span class="q-skill">{{ q.skillName }}</span>
            <span v-if="q.score" class="q-stars" :aria-label="`${q.score} 分（满分 5 分）`">{{ stars(q.score) }}</span>
            <span v-else class="q-skip">未作答</span>
            <span v-if="secondsByQuestion[q.index] != null" class="q-time">用时 {{ clock(secondsByQuestion[q.index]) }}</span>
          </p>
          <p class="q-text">{{ q.question }}</p>
          <p v-if="q.comment" class="q-comment">{{ q.comment }}</p>
          <details v-if="q.keyPoints">
            <summary>参考要点</summary>
            <p>{{ q.keyPoints }}</p>
          </details>
        </div>
        <div class="report-actions">
          <button type="button" :disabled="busy" @click="start(sessionPreset)">再来一场</button>
          <RouterLink to="/path" class="ghost-link">按建议去学习 →</RouterLink>
        </div>
      </div>
    </template>
    <p v-else-if="error" class="error">{{ error }}</p>
  </section>
</template>

<style scoped>
.interview { width: 100%; }
.title { display: flex; align-items: center; gap: 14px; }
.title-ai { padding: 4px 12px; font-size: 1.4rem; box-shadow: var(--shadow-sm); }
.intro { margin-top: 18px; padding: 22px; border: var(--border-thick); border-radius: var(--radius-lg); background: var(--ai-soft); box-shadow: 5px 5px 0 var(--ai); }
.intro p { margin: 0 0 12px; }
.intro-lead { font-size: 1.05rem; font-weight: 800; }
.intro-lead .ai-badge { margin-right: 8px; vertical-align: 2px; }
.intro ul { margin: 0 0 18px; padding-left: 20px; color: var(--color-text); font-weight: 700; }
.warn { color: var(--color-warning); font-weight: 800; }
.modes { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 12px; margin: 0 0 18px; padding: 0; border: 0; }
.modes legend { margin-bottom: 10px; padding: 0; color: var(--ink); font-weight: 900; }
.mode { display: flex; align-items: flex-start; gap: 10px; padding: 12px 14px; border: var(--border-thick); border-radius: var(--radius-md); background: var(--paper); cursor: pointer; transition: transform 140ms ease, box-shadow 140ms ease; }
.mode:hover { transform: translate(-1px, -1px); box-shadow: var(--shadow-sm); }
.mode.on { background: var(--yellow); box-shadow: var(--shadow-sm); }
.mode input { margin-top: 3px; }
.mode span { display: grid; gap: 2px; }
.mode strong { color: var(--ink); font-weight: 900; }
.mode small { color: var(--color-text); font-size: 0.8rem; font-weight: 600; }
.start { min-height: 52px; padding: 0 30px; font-size: 1.05rem; box-shadow: 5px 5px 0 var(--ai); }
.start:hover:not(:disabled) { box-shadow: 7px 7px 0 var(--ai); }
.mode-tag { padding: 2px 10px; border: 2.5px solid var(--ink); border-radius: 999px; background: var(--paper); color: var(--ink); font-size: 0.8rem; font-weight: 900; }
.timer { display: flex; flex-wrap: wrap; align-items: center; gap: 8px 12px; margin-bottom: 12px; padding: 12px 16px; border: var(--border-thick); border-radius: var(--radius-md); background: var(--paper); box-shadow: var(--shadow-sm); }
.timer.think { background: #e3ecff; }
.timer-label { color: var(--ink); font-weight: 900; }
.timer-clock { color: var(--ink); font-family: var(--font-num); font-size: 1.5rem; line-height: 1; }
.timer-bar { flex: 1 1 160px; height: 16px; overflow: hidden; border: var(--border-thick); border-radius: 999px; background: var(--paper); }
.timer-fill { height: 100%; background: var(--ink); transition: width 250ms linear; }
.timer.urgent { background: #ffe3e8; }
.timer.urgent .timer-clock { color: var(--color-danger); }
.timer.urgent .timer-fill { background: var(--red); }
.timer-go { min-height: 40px; padding: 0 14px; font-size: 0.9rem; }
.timer-hint { color: var(--color-muted); font-size: 0.8rem; font-weight: 700; }
.used { color: var(--color-muted); font-size: 0.75rem; font-weight: 700; }
.used.late { color: var(--color-warning); }
.q-time { margin-left: 8px; color: var(--color-muted); font-size: 0.8rem; font-weight: 700; }
.history { margin-top: 32px; }
.history h2 { margin-bottom: 12px; color: var(--ink); font-family: var(--font-display); font-size: 1.8rem; font-weight: 400; }
.history-row { display: flex; flex-wrap: wrap; align-items: center; gap: 6px 14px; margin-bottom: 12px; padding: 12px 16px; border: var(--border-thick); border-radius: var(--radius-md); background: var(--paper); color: var(--ink); text-decoration: none; transition: transform 140ms ease, box-shadow 140ms ease; }
.history-row:hover { color: var(--ink); text-decoration: none; transform: translate(-2px, -2px); box-shadow: var(--shadow-md); }
.h-name { font-weight: 900; }
.h-meta { color: var(--color-muted); font-size: 0.85rem; font-weight: 700; }
.h-score { margin-left: auto; padding: 2px 12px; border: 2.5px solid var(--ink); border-radius: 999px; background: var(--yellow); font-family: var(--font-num); }
.h-cont { margin-left: auto; font-weight: 900; }
.session-head { display: flex; flex-wrap: wrap; align-items: center; gap: 10px; margin: 8px 0 14px; }
.topic { color: var(--ink); font-weight: 900; }
.q-progress { padding: 2px 10px; border: 2.5px solid var(--ink); border-radius: 999px; background: var(--yellow); font-size: 0.85rem; font-weight: 900; }
.link-btn, .link-btn:hover:not(:disabled) { margin-left: auto; min-height: 38px; padding: 0 14px; color: var(--ink); background: var(--paper); box-shadow: var(--shadow-sm); font-size: 0.85rem; }
.link-btn:hover:not(:disabled) { background: #ffe3e8; }
.chat { display: grid; gap: 16px; max-height: 58vh; overflow-y: auto; padding: 18px; border: var(--border-thick); border-radius: var(--radius-lg); background: var(--paper); box-shadow: var(--shadow-md); }
.msg { display: grid; gap: 6px; max-width: min(86%, 780px); }
.msg.candidate { justify-self: end; text-align: right; }
.who { display: inline-flex; align-items: center; gap: 6px; color: var(--ink); font-size: 0.8rem; font-weight: 900; }
.candidate .who { justify-content: flex-end; }
.bubble { margin: 0; padding: 12px 16px; border: var(--border-thick); border-radius: var(--radius-md); color: var(--ink); white-space: pre-wrap; text-align: left; line-height: 1.7; font-weight: 600; }
.interviewer .bubble { background: var(--ai-soft); box-shadow: 4px 4px 0 var(--ai); border-top-left-radius: 2px; }
.candidate .bubble { background: var(--yellow); box-shadow: var(--shadow-sm); border-top-right-radius: 2px; }
.typing span { animation: blink 1.2s infinite both; }
.typing span:nth-child(2) { animation-delay: .2s; }
.typing span:nth-child(3) { animation-delay: .4s; }
@keyframes blink { 0%, 80%, 100% { opacity: .2; } 40% { opacity: 1; } }
.composer { margin-top: 18px; }
.composer textarea { resize: vertical; }
.composer-actions { display: flex; justify-content: flex-end; gap: 12px; margin-top: 12px; }
.ghost { color: var(--ink); background: var(--paper); box-shadow: var(--shadow-sm); }
.ghost:hover:not(:disabled) { box-shadow: 5px 5px 0 var(--ink); }
.ghost:active:not(:disabled) { box-shadow: 1px 1px 0 var(--ink); }
.report { margin-top: 22px; padding: 22px; border: var(--border-thick); border-radius: var(--radius-lg); background: var(--paper); box-shadow: 6px 6px 0 var(--ai); }
.report-head { display: flex; gap: 20px; align-items: center; }
.report-head h2 { display: flex; align-items: center; gap: 8px; margin: 0 0 6px; color: var(--ink); font-family: var(--font-display); font-size: 1.8rem; font-weight: 400; }
.report-head p { margin: 0; font-weight: 700; }
.score-ring { flex: 0 0 auto; display: grid; place-items: center; width: 92px; height: 92px; border: var(--border-thick); border-radius: 50%; background: var(--orange); box-shadow: var(--shadow-sm); color: var(--ink); font-family: var(--font-num); font-size: 2rem; }
.score-ring.good { background: var(--green); }
.report-cols { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-top: 20px; }
.report-cols > div { padding: 14px 16px; border: var(--border-thick); border-radius: var(--radius-md); }
.report-cols > div:first-child { background: #e3f8ec; }
.report-cols > div:last-child { background: var(--color-soft); }
.report h3 { margin: 20px 0 10px; color: var(--ink); font-weight: 900; }
.report-cols h3 { margin-top: 0; }
.report ul { margin: 0; padding-left: 18px; font-weight: 600; }
.report li { margin-bottom: 6px; }
.q-review { padding: 16px 0; border-top: 2px solid var(--ink); }
.q-title { display: flex; flex-wrap: wrap; align-items: center; gap: 6px; margin: 0 0 6px; color: var(--ink); font-weight: 900; }
.q-skill { padding: 1px 8px; border: 2px solid var(--ink); border-radius: 6px; font-size: 0.78rem; font-weight: 800; }
.q-stars { color: #d98a00; letter-spacing: 1px; }
.q-skip { color: var(--color-muted); font-size: 0.8rem; }
.q-text { margin: 0 0 8px; font-weight: 600; }
.q-comment { margin: 0 0 8px; padding: 10px 14px; border: 2.5px solid var(--ink); border-radius: 10px; background: var(--ai-soft); font-weight: 600; }
details { color: var(--color-text); font-size: 0.9rem; }
details summary { cursor: pointer; font-weight: 900; }
details p { margin: 6px 0 0; white-space: pre-wrap; }
.report-actions { display: flex; flex-wrap: wrap; align-items: center; gap: 14px; margin-top: 20px; }
.ghost-link { color: var(--ink); font-weight: 900; }
.error { margin-top: 14px; padding: 12px 14px; border: var(--border-thick); border-radius: var(--radius-md); background: #ffe3e8; }
@media (max-width: 560px) { .report-cols { grid-template-columns: 1fr; } .msg { max-width: 100%; } }
</style>
