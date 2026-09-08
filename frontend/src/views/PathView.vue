<script setup>
import { ref, onMounted, watch } from 'vue'
import { api } from '@/api/client'
import { useUserProfileStore } from '@/stores/userProfile'
import { useAuthStore } from '@/stores/auth'

// 对应③学习路径规划引擎的展示层：按阶段（拓扑排序分层）展示，
// 每个技能点击进去是对应的资料 + 测评（④引擎），不在这一页展开。
const profile = useUserProfileStore()
const auth = useAuthStore()
const stages = ref([])
const loading = ref(false)
const error = ref('')

async function load() {
  if (!profile.targetCategoryId) return
  loading.value = true
  error.value = ''
  try {
    stages.value = await api.learningPath(profile.targetCategoryId, auth.userId)
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
onMounted(load)
watch(() => profile.targetCategoryId, load)
</script>

<template>
  <section>
    <h1>学习路径</h1>
    <p v-if="!profile.targetCategoryId">请先在"岗位推荐"页选择一个目标岗位。</p>
    <template v-else>
      <p v-if="loading">加载中…</p>
      <p v-else-if="error" class="error">{{ error }}</p>
      <p v-else-if="!stages.length" class="done">技能都已掌握，暂时没有待学的了 🎉</p>
      <ol v-else class="timeline">
        <li v-for="s in stages" :key="s.stage" class="stage">
          <div class="stage-head">
            <span class="stage-num">阶段 {{ s.stage }}</span>
            <span class="stage-hint">同一阶段内可以并行学</span>
          </div>
          <div class="skill-row">
            <RouterLink v-for="sk in s.skills" :key="sk.skillId" :to="{ path: '/quiz', query: { skillId: sk.skillId, name: sk.name } }" class="skill-chip">
              {{ sk.name }}
              <span class="w">{{ Math.round(sk.weight * 100) }}%</span>
            </RouterLink>
          </div>
        </li>
      </ol>
    </template>
  </section>
</template>

<style scoped>
.timeline { list-style: none; padding: 0; margin: 1rem 0 0; display: flex; flex-direction: column; gap: 1.25rem; }
.stage { border-left: 3px solid #2b6e5c; padding-left: 1rem; }
.stage-head { display: flex; align-items: baseline; gap: 0.6rem; margin-bottom: 0.5rem; }
.stage-num { font-weight: 700; color: #1c232e; }
.stage-hint { font-size: 0.8rem; color: #8891a0; }
.skill-row { display: flex; flex-wrap: wrap; gap: 0.5rem; }
.skill-chip { display: inline-flex; align-items: center; gap: 0.4rem; padding: 0.35rem 0.75rem; border-radius: 999px; background: #e3f0eb; color: #2b6e5c; text-decoration: none; font-size: 0.9rem; font-weight: 500; }
.skill-chip .w { font-size: 0.75rem; color: #5b8c7c; font-variant-numeric: tabular-nums; }
.error { color: #b3261e; }
.done { color: #2b6e5c; }
</style>
