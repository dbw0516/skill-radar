<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { api } from '@/api/client'
import { useAuthStore } from '@/stores/auth'
import { useUserProfileStore } from '@/stores/userProfile'

// 学习进度条 + 激励面板：目标岗位技能进度、可投岗位数、连续学习打卡、错题复习提醒、下一步推荐、徽章墙。
// 数据全部来自 GET /api/users/{id}/progress，后端从 user_skills / quiz_attempts 等现算。
// 进度条三段：测评认证（实色）、自评掌握（浅色）、学习中（条纹，没掌握的技能按测评正确率和资料阅读给的部分分）。
defineProps({
  showBadges: { type: Boolean, default: true },
})
const emit = defineEmits(['loaded'])

const auth = useAuthStore()
const profile = useUserProfileStore()
const data = ref(null)
const error = ref('')

async function load() {
  if (!auth.userId) return
  error.value = ''
  try {
    data.value = await api.progress(auth.userId)
    emit('loaded', data.value)
  } catch (e) {
    error.value = e.message
  }
}
onMounted(load)
watch(() => profile.targetCategoryId, load)
defineExpose({ reload: load })

const hasTarget = computed(() => data.value?.categoryId != null)
const earnedCount = computed(() => data.value?.badges.filter((b) => b.earned).length ?? 0)
</script>

<template>
  <div v-if="error" class="panel error">{{ error }}</div>
  <div v-else-if="data" class="panel">
    <p class="message">{{ data.message }}</p>

    <div v-if="hasTarget" class="progress-block">
      <div class="progress-head">
        <span class="label">「{{ data.categoryName }}」学习进度</span>
        <span class="pct">{{ data.percent }}%</span>
      </div>
      <div
        class="bar"
        role="progressbar"
        aria-valuemin="0" aria-valuemax="100"
        :aria-valuenow="data.percent"
        :aria-label="`目标岗位技能进度 ${data.percent}%`"
      >
        <div class="seg verified" :style="{ width: data.verifiedShare + '%' }"></div>
        <div class="seg self" :style="{ width: data.selfShare + '%' }"></div>
        <div class="seg learning" :style="{ width: data.learningShare + '%' }"></div>
      </div>
      <div class="progress-meta">
        <span><i class="dot verified"></i>测评认证 {{ data.verifiedSkills }}</span>
        <span><i class="dot self"></i>自评掌握 {{ data.masteredSkills - data.verifiedSkills }}</span>
        <span><i class="dot learning"></i>学习中 {{ data.learningSkills }}</span>
        <span>共 {{ data.totalSkills }} 项 · 按招聘需求加权覆盖 {{ data.weightedPercent }}%</span>
      </div>
      <p v-if="data.nextMilestone" class="milestone">🎯 {{ data.nextMilestone }}</p>
      <p v-if="data.postingsWithSkills" class="milestone">
        💼 匹配度 ≥ 70% 的可投岗位：<strong>{{ data.qualifiedPostings }}</strong> / {{ data.postingsWithSkills }} 条
        <RouterLink to="/" class="inline-link">去看看</RouterLink>
      </p>
    </div>
    <p v-else class="milestone">先<RouterLink to="/onboarding">选一个目标岗位</RouterLink>，就能看到你的学习进度条。</p>

    <div class="stats">
      <div class="stat" :class="{ lit: data.practicedToday }">
        <span class="stat-num">🔥 {{ data.streakDays }}</span>
        <span class="stat-label">连续学习天数</span>
      </div>
      <div class="stat">
        <span class="stat-num">{{ data.practicedToday ? '✓' : '—' }}</span>
        <span class="stat-label">{{ data.practicedToday ? '今日已打卡' : '今日未打卡' }}</span>
      </div>
      <div class="stat">
        <span class="stat-num">{{ data.passedAttempts }}/{{ data.totalAttempts }}</span>
        <span class="stat-label">测评通过/总次数</span>
      </div>
      <div class="stat">
        <span class="stat-num">{{ data.totalAnswered }}</span>
        <span class="stat-label">累计答题</span>
      </div>
    </div>

    <RouterLink v-if="data.dueReviews" class="next due" :to="{ path: '/quiz', query: { mode: 'review' } }">
      📅 今天有 <strong>{{ data.dueReviews }}</strong> 道错题到了复习时间
      <span class="go">开始复习 →</span>
    </RouterLink>
    <RouterLink
      v-if="data.nextSkillId"
      class="next"
      :to="{ path: '/quiz', query: { skillId: data.nextSkillId, name: data.nextSkillName } }"
    >
      下一步推荐：<strong>{{ data.nextSkillName }}</strong>
      <span class="next-w">{{ data.nextSkillWeight }}% 的同类岗位要求</span>
      <span class="go">去学习 →</span>
    </RouterLink>
    <RouterLink v-if="hasTarget" class="next" to="/interview">
      <span class="ai-badge">AI</span>来一场模拟面试<span class="next-w">已完成 {{ data.interviewsFinished }} 场</span>
      <span class="go">开始 →</span>
    </RouterLink>

    <div v-if="showBadges" class="badges">
      <div class="badges-head">我的徽章 <span>{{ earnedCount }}/{{ data.badges.length }}</span></div>
      <div class="badge-grid">
        <div
          v-for="b in data.badges" :key="b.key"
          class="badge" :class="{ earned: b.earned }"
          :title="b.description"
        >
          <span class="badge-icon" aria-hidden="true">{{ b.icon }}</span>
          <span class="badge-name">{{ b.name }}</span>
          <span class="badge-desc">{{ b.description }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.panel { margin: 20px 0; padding: 22px; border: var(--border-thick); border-radius: var(--radius-lg); background: var(--paper); box-shadow: var(--shadow-md); }
.panel.error { background: #ffe3e8; }
.message { display: inline-block; margin: 0 0 18px; padding: 4px 14px; border: var(--border-thick); border-radius: 999px; background: var(--yellow); color: var(--ink); font-weight: 900; transform: rotate(-1deg); }
.progress-head { display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 8px; }
.label { color: var(--ink); font-weight: 900; font-size: 1rem; }
.pct { color: var(--ink); font-family: var(--font-num); font-size: 2rem; line-height: 1; }
.bar { display: flex; height: 26px; overflow: hidden; border: var(--border-thick); border-radius: 999px; background: var(--paper); }
.seg { height: 100%; transition: width 700ms cubic-bezier(.22, 1, .36, 1); }
.seg + .seg { border-left: 2px solid var(--ink); }
.seg.verified { background: var(--green); }
.seg.self { background: var(--yellow); }
.seg.learning { background: repeating-linear-gradient(-45deg, var(--blue) 0 7px, #9dbbff 7px 14px); }
.progress-meta { display: flex; flex-wrap: wrap; gap: 6px 16px; margin-top: 10px; color: var(--color-muted); font-size: 0.85rem; font-weight: 700; }
.dot { display: inline-block; width: 12px; height: 12px; margin-right: 6px; border: 2px solid var(--ink); border-radius: 50%; vertical-align: -1px; }
.dot.verified { background: var(--green); }
.dot.self { background: var(--yellow); }
.dot.learning { background: var(--blue); }
.milestone { margin: 12px 0 0; font-size: 0.95rem; font-weight: 700; color: var(--color-text); }
.milestone strong { color: var(--ink); font-family: var(--font-num); font-weight: 400; }
.inline-link { margin-left: 6px; }
.stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-top: 20px; }
.stat { display: grid; gap: 2px; padding: 14px 12px; border: var(--border-thick); border-radius: var(--radius-md); background: var(--paper); box-shadow: var(--shadow-sm); text-align: center; }
.stat.lit { background: var(--orange); }
.stat-num { color: var(--ink); font-family: var(--font-num); font-size: 1.5rem; line-height: 1.2; }
.stat-label { color: var(--color-muted); font-size: 0.8rem; font-weight: 800; }
.next { display: flex; flex-wrap: wrap; align-items: center; gap: 6px 10px; margin-top: 14px; padding: 12px 16px; border: var(--border-thick); border-radius: var(--radius-md); background: var(--paper); box-shadow: var(--shadow-sm); color: var(--ink); text-decoration: none; font-size: 0.95rem; font-weight: 700; transition: transform 140ms ease, box-shadow 140ms ease; }
.next:first-of-type { margin-top: 20px; }
.next:hover { color: var(--ink); text-decoration: none; transform: translate(-2px, -2px); box-shadow: var(--shadow-md); }
.next.due { background: #ffe3e8; }
.next strong { font-weight: 900; }
.next-w { color: var(--color-muted); font-size: 0.85rem; }
.go { margin-left: auto; color: var(--ink); font-weight: 900; }
.badges { margin-top: 22px; }
.badges-head { margin-bottom: 12px; color: var(--ink); font-weight: 900; font-size: 1rem; }
.badges-head span { color: var(--color-muted); font-family: var(--font-num); font-weight: 400; font-size: 0.85rem; }
.badge-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(130px, 1fr)); gap: 12px; }
.badge { display: grid; justify-items: center; gap: 2px; padding: 12px 8px; border: 2.5px dashed #8a8a8a; border-radius: var(--radius-md); text-align: center; opacity: .55; filter: grayscale(1); }
.badge.earned { opacity: 1; filter: none; border: var(--border-thick); background: var(--color-soft); box-shadow: var(--shadow-sm); }
.badge-icon { font-size: 1.7rem; line-height: 1.2; }
.badge-name { color: var(--ink); font-weight: 900; font-size: 0.9rem; }
.badge-desc { color: var(--color-muted); font-size: 0.75rem; }
@media (max-width: 560px) { .stats { grid-template-columns: repeat(2, 1fr); } .go { margin-left: 0; } }
</style>
