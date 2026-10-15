<script setup>
import { ref, computed, onMounted } from 'vue'
import { api } from '@/api/client'
import { useAuthStore } from '@/stores/auth'
import { useUserProfileStore } from '@/stores/userProfile'
import ProgressPanel from '@/components/ProgressPanel.vue'

// 新用户上手引导：注册完直接进来，三步走完就能看到自己的进度条。
//  1) 选目标岗位；2) 只在这个岗位要求的几个技能里勾"已经会的"（不用面对全部 70 多个技能）；
//  3) 看到进度，一键开始第一次测评（摸底）。
// 老用户也能从进度面板的提示进来重新选，第 2 步保存时不会动其他岗位方向上的自评技能。
const auth = useAuthStore()
const profile = useUserProfileStore()

const step = ref(1)
const categories = ref([])
const loading = ref(true)
const error = ref('')

const categoryId = ref(profile.targetCategoryId)
const categorySkills = ref([])   // 这个岗位要求的技能（GapItem）
const checked = ref(new Set())
const saving = ref(false)
const nextSkill = ref(null)

const selectedCategory = computed(() => categories.value.find((c) => c.id === categoryId.value))

onMounted(async () => {
  try {
    categories.value = await api.listJobCategories()
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
})

async function chooseCategory(id) {
  categoryId.value = id
  error.value = ''
  saving.value = true
  try {
    await profile.setTargetCategory(id)
    categorySkills.value = await api.gapAnalysis(id, auth.userId)
    checked.value = new Set(categorySkills.value.filter((s) => s.mastered).map((s) => s.skillId))
    step.value = 2
  } catch (e) {
    error.value = e.message
  } finally {
    saving.value = false
  }
}

function toggle(s) {
  if (s.masteredSource === 'quiz_verified') return
  const next = new Set(checked.value)
  next.has(s.skillId) ? next.delete(s.skillId) : next.add(s.skillId)
  checked.value = next
}

async function saveSkills() {
  saving.value = true
  error.value = ''
  try {
    // PUT /skills 是"把自评集合整体设成这些"，所以要带上其他岗位方向上已有的自评，只替换本岗位这几个
    const all = await api.listUserSkills(auth.userId)
    const inCategory = new Set(categorySkills.value.map((s) => s.skillId))
    const keep = all.filter((s) => s.status === 'self_reported' && !inCategory.has(s.skillId)).map((s) => s.skillId)
    await api.setUserSkills(auth.userId, [...keep, ...checked.value])
    step.value = 3
  } catch (e) {
    error.value = e.message
  } finally {
    saving.value = false
  }
}

function onProgressLoaded(p) {
  nextSkill.value = p.nextSkillId ? { id: p.nextSkillId, name: p.nextSkillName } : null
}
</script>

<template>
  <section class="onboarding">
    <h1>欢迎来到灵犀职途 👋</h1>
    <p class="lead">三步就能看到你离目标岗位还有多远。</p>

    <ol class="steps" aria-label="引导步骤">
      <li :class="{ active: step === 1, done: step > 1 }"><span>1</span>选目标岗位</li>
      <li :class="{ active: step === 2, done: step > 2 }"><span>2</span>勾选已会的技能</li>
      <li :class="{ active: step === 3 }"><span>3</span>开始第一次测评</li>
    </ol>

    <p v-if="error" class="error">{{ error }}</p>

    <div v-if="step === 1">
      <h2>你想找什么方向的工作？</h2>
      <p v-if="loading">加载中…</p>
      <div v-else class="cat-grid">
        <button
          v-for="c in categories" :key="c.id" type="button"
          class="cat-card" :class="{ active: categoryId === c.id }"
          :disabled="saving"
          @click="chooseCategory(c.id)"
        >
          <strong>{{ c.name }}</strong>
        </button>
      </div>
    </div>

    <div v-else-if="step === 2">
      <h2>「{{ selectedCategory?.name }}」要求这些技能，你已经会哪些？</h2>
      <p class="hint">按招聘需求从高到低排列，百分比是同类岗位 JD 里要求这个技能的比例。不确定的先别勾，待会儿测一下就知道了。</p>
      <div class="skill-list">
        <label
          v-for="s in categorySkills" :key="s.skillId"
          class="skill-item" :class="{ on: checked.has(s.skillId), locked: s.masteredSource === 'quiz_verified' }"
        >
          <input
            type="checkbox" :checked="checked.has(s.skillId)"
            :disabled="s.masteredSource === 'quiz_verified'"
            @change="toggle(s)"
          />
          <span class="skill-name">{{ s.name }}</span>
          <span v-if="s.masteredSource === 'quiz_verified'" class="tag">已认证</span>
          <span class="weight">{{ Math.round(s.weight * 100) }}%</span>
        </label>
      </div>
      <div class="actions">
        <button type="button" class="ghost" @click="step = 1">← 重新选岗位</button>
        <button type="button" :disabled="saving" @click="saveSkills">
          {{ saving ? '保存中…' : (checked.size ? `我会这 ${checked.size} 项，下一步` : '都还不会，下一步') }}
        </button>
      </div>
    </div>

    <div v-else>
      <h2>这是你现在的起点</h2>
      <ProgressPanel :show-badges="false" @loaded="onProgressLoaded" />
      <div class="actions">
        <RouterLink
          v-if="nextSkill" class="primary-link"
          :to="{ path: '/quiz', query: { skillId: nextSkill.id, name: nextSkill.name } }"
        >开始第一次测评：{{ nextSkill.name }} →</RouterLink>
        <RouterLink to="/path" class="ghost-link">先看看学习路径</RouterLink>
      </div>
    </div>

    <RouterLink v-if="step < 3" to="/" class="skip">跳过，稍后再说</RouterLink>
  </section>
</template>

<style scoped>
.onboarding { max-width: 760px; }
.lead { margin: 4px 0 20px; color: var(--color-muted); }
h2 { margin: 24px 0 12px; color: var(--color-heading); font-size: 1.1rem; }
.steps { display: flex; flex-wrap: wrap; gap: 8px; list-style: none; margin: 0; padding: 0; }
.steps li { display: inline-flex; align-items: center; gap: 8px; padding: 6px 12px 6px 6px; border: 1px solid var(--color-border); border-radius: 999px; color: var(--color-muted); font-size: 0.86rem; font-weight: 650; }
.steps li span { display: grid; place-items: center; width: 24px; height: 24px; border-radius: 50%; background: var(--color-soft); font-size: 0.78rem; }
.steps li.active { border-color: var(--color-primary); color: var(--color-heading); }
.steps li.active span { background: var(--color-primary); color: var(--color-on-primary); }
.steps li.done span { background: var(--color-success); color: var(--color-on-primary); }
.cat-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 10px; }
.cat-card { display: grid; gap: 4px; min-height: 64px; padding: 14px; border: var(--border-thick); border-radius: var(--radius-md); background: var(--paper); box-shadow: var(--shadow-sm); color: var(--ink); text-align: left; }
.cat-card small { color: var(--color-muted); font-weight: 400; font-size: 0.78rem; }
.cat-card:hover:not(:disabled), .cat-card.active, .cat-card.active:hover:not(:disabled) { background: var(--yellow); box-shadow: var(--shadow-md); }
.hint { color: var(--color-muted); font-size: 0.86rem; }
.skill-list { display: grid; gap: 8px; }
.skill-item { display: flex; align-items: center; gap: 10px; min-height: 48px; padding: 8px 14px; border: 1px solid var(--color-border); border-radius: var(--radius-sm); background: var(--color-surface-raised); cursor: pointer; }
.skill-item.on { border-color: var(--color-primary); background: var(--color-soft); }
.skill-item.locked { cursor: default; }
.skill-item input { width: 18px; min-height: 18px; }
.skill-name { color: var(--color-heading); font-weight: 650; }
.tag { padding: 2px 7px; border-radius: var(--radius-sm); background: color-mix(in srgb, var(--color-success) 14%, transparent); color: var(--color-success); font-size: 0.72rem; font-weight: 700; }
.weight { margin-left: auto; color: var(--color-muted); font-size: 0.8rem; font-variant-numeric: tabular-nums; }
.actions { display: flex; flex-wrap: wrap; align-items: center; gap: 12px; margin-top: 18px; }
.ghost, .ghost:hover:not(:disabled) { color: var(--ink); background: var(--paper); box-shadow: var(--shadow-sm); }
.ghost:hover:not(:disabled) { box-shadow: 5px 5px 0 var(--ink); }
.primary-link { display: inline-flex; align-items: center; min-height: 44px; padding: 0 18px; border: var(--border-thick); border-radius: var(--radius-md); background: var(--ink); color: var(--paper); box-shadow: 4px 4px 0 var(--pink); font-weight: 900; text-decoration: none; }
.primary-link:hover { color: var(--paper); text-decoration: none; }
.ghost-link { font-weight: 650; }
.skip { display: inline-block; margin-top: 28px; color: var(--color-muted); font-size: 0.85rem; }
.error { padding: 12px 14px; border-radius: var(--radius-md); background: color-mix(in srgb, var(--color-danger) 9%, var(--color-surface)); }
</style>
