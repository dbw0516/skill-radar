<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { api } from '@/api/client'
import { useAuthStore } from '@/stores/auth'
import TabBar from '@/components/TabBar.vue'

// 个人中心：基本资料（含所在地区/意向就业地区，供①岗位推荐做地点排序）+ 技能自评清单
// （设计文档「用户画像怎么建」一节说的"初始化来自专业选择 + 自评清单"，这里就是那个入口）。
// 两块内容分开放在 tab 里，不用一次性把整页拉得很长。
const auth = useAuthStore()
const activeTab = ref('basic')

const majors = ref([])
const form = reactive({ nickname: '', majorId: null, location: '', targetLocation: '' })
const savingProfile = ref(false)
const profileSaved = ref(false)
const profileError = ref('')

const skills = ref([])
const skillsLoading = ref(true)
const savingSkills = ref(false)
const skillsSaved = ref(false)
const skillsError = ref('')
const checked = reactive(new Set())

function fillFormFromUser() {
  const u = auth.user
  if (!u) return
  form.nickname = u.nickname || ''
  form.majorId = u.majorId || null
  form.location = u.location || ''
  form.targetLocation = u.targetLocation || ''
}

const skillsByDomain = computed(() => {
  const map = new Map()
  for (const s of skills.value) {
    if (!map.has(s.domain)) map.set(s.domain, [])
    map.get(s.domain).push(s)
  }
  return [...map.entries()]
})

async function loadSkills() {
  skillsLoading.value = true
  try {
    skills.value = await api.listUserSkills(auth.userId)
    checked.clear()
    for (const s of skills.value) {
      if (s.status) checked.add(s.skillId)
    }
  } catch (e) {
    skillsError.value = e.message
  } finally {
    skillsLoading.value = false
  }
}

onMounted(async () => {
  fillFormFromUser()
  try {
    majors.value = await api.listMajors()
  } catch {
    // 专业列表拿不到就先留空，不影响其他资料照常填
  }
  await loadSkills()
})

async function saveProfile() {
  savingProfile.value = true
  profileError.value = ''
  profileSaved.value = false
  try {
    const updated = await api.updateProfile(auth.userId, { ...form })
    auth.setUser(updated)
    profileSaved.value = true
  } catch (e) {
    profileError.value = e.message
  } finally {
    savingProfile.value = false
  }
}

function toggle(skillId, isVerified) {
  if (isVerified) return // 测评认证过的不让在这里取消
  if (checked.has(skillId)) checked.delete(skillId)
  else checked.add(skillId)
}

async function saveSkills() {
  savingSkills.value = true
  skillsError.value = ''
  skillsSaved.value = false
  try {
    skills.value = await api.setUserSkills(auth.userId, [...checked])
    skillsSaved.value = true
  } catch (e) {
    skillsError.value = e.message
  } finally {
    savingSkills.value = false
  }
}
</script>

<template>
  <section>
    <h1>个人中心</h1>
    <TabBar
      :tabs="[{ key: 'basic', label: '基本资料' }, { key: 'skills', label: '技能自评' }]"
      v-model="activeTab"
    />

    <form v-if="activeTab === 'basic'" @submit.prevent="saveProfile">
      <label>昵称<input v-model="form.nickname" type="text" /></label>
      <label>专业
        <select v-model="form.majorId">
          <option :value="null">未选择</option>
          <option v-for="m in majors" :key="m.id" :value="m.id">{{ m.name }}</option>
        </select>
      </label>
      <label>所在地区<input v-model="form.location" type="text" placeholder="例如：北京-朝阳区" /></label>
      <label>意向就业地区<input v-model="form.targetLocation" type="text" placeholder="例如：北京，不填表示不限" /></label>
      <p class="hint">意向就业地区会让"岗位推荐"页里地点匹配的招聘信息排在前面。</p>
      <p v-if="profileError" class="error">{{ profileError }}</p>
      <p v-if="profileSaved" class="ok">已保存。</p>
      <button type="submit" :disabled="savingProfile">{{ savingProfile ? '保存中…' : '保存资料' }}</button>
    </form>

    <div v-else>
      <p class="hint">勾选你已经掌握的技能——这是自评，会标"自评"角标；橙色"已认证"的是测评通过的，没法在这里取消，只能靠测评本身的表现改变。</p>
      <p v-if="skillsLoading">加载中…</p>
      <p v-else-if="skillsError" class="error">{{ skillsError }}</p>
      <template v-else>
        <div v-for="[domain, list] in skillsByDomain" :key="domain" class="domain-group">
          <h3>{{ domain }}</h3>
          <label v-for="s in list" :key="s.skillId" class="skill-row" :class="{ locked: s.status === 'quiz_verified' }">
            <input
              type="checkbox"
              :checked="checked.has(s.skillId)"
              :disabled="s.status === 'quiz_verified'"
              @change="toggle(s.skillId, s.status === 'quiz_verified')"
            />
            {{ s.name }}
            <span v-if="s.status === 'quiz_verified'" class="badge verified">已认证</span>
            <span v-else-if="checked.has(s.skillId)" class="badge self">自评</span>
          </label>
        </div>
        <p v-if="skillsSaved" class="ok">已保存。</p>
        <button @click="saveSkills" :disabled="savingSkills">{{ savingSkills ? '保存中…' : '保存技能自评' }}</button>
      </template>
    </div>
  </section>
</template>

<style scoped>
h1 { margin-bottom: 4px; }
form { display: grid; gap: 16px; max-width: 520px; margin-top: 20px; }
label { display: flex; flex-direction: column; gap: 6px; color: var(--color-heading); font-size: 0.9rem; font-weight: 650; }
.hint { max-width: 520px; margin: 0; padding: 12px 14px; border-left: 3px solid var(--color-primary); border-radius: 0 var(--radius-sm) var(--radius-sm) 0; background: var(--color-soft); font-size: 0.86rem; }
.error { margin: 0; padding: 10px 12px; border-radius: var(--radius-sm); background: color-mix(in srgb, var(--color-danger) 9%, var(--color-surface)); font-size: 0.88rem; }
.ok { margin: 0; padding: 10px 12px; border-radius: var(--radius-sm); background: color-mix(in srgb, var(--color-success) 10%, var(--color-surface)); font-size: 0.88rem; }
button { align-self: flex-start; margin-top: 4px; }
.domain-group { max-width: 640px; margin-bottom: 18px; padding: 18px; border: 1px solid var(--color-border); border-radius: var(--radius-md); background: var(--color-surface-raised); }
.domain-group h3 { margin: 0 0 10px; color: var(--color-heading); font-size: 0.9rem; font-weight: 750; }
.skill-row { display: flex; align-items: center; gap: 10px; min-height: 44px; padding: 8px 0; border-top: 1px solid var(--color-border); font-size: 0.92rem; cursor: pointer; }
.skill-row:first-of-type { border-top: 0; }
.skill-row.locked { cursor: default; }
.badge { padding: 3px 8px; border: 1px solid var(--color-border); border-radius: var(--radius-sm); font-size: 0.72rem; font-weight: 700; }
.badge.verified { border-color: color-mix(in srgb, var(--color-success) 30%, transparent); background: color-mix(in srgb, var(--color-success) 10%, var(--color-surface)); color: var(--color-success); }
.badge.self { border-color: color-mix(in srgb, var(--color-warning) 30%, transparent); background: color-mix(in srgb, var(--color-warning) 12%, var(--color-surface)); color: var(--color-warning); }
</style>
