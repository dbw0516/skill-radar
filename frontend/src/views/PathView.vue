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
      <p v-else-if="!stages.length" class="done">技能都已掌握，暂时没有待学内容。</p>
      <TransitionGroup v-else tag="ol" name="stage" class="timeline">
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
      </TransitionGroup>
    </template>
  </section>
</template>

<style scoped>
.timeline { list-style: none; padding: 0; margin: 24px 0 0; display: grid; gap: 16px; counter-reset: stage; }
.stage { position: relative; padding: 20px 20px 20px 74px; border: 1px solid var(--color-border); border-radius: var(--radius-md); background: var(--color-surface-raised); }
.stage::before { content: counter(stage); counter-increment: stage; position: absolute; top: 20px; left: 20px; display: grid; place-items: center; width: 36px; height: 36px; border-radius: var(--radius-sm); color: var(--color-on-primary); background: var(--color-primary); font-weight: 760; }
.stage-head { display: flex; align-items: baseline; flex-wrap: wrap; gap: 8px; margin-bottom: 12px; }
.stage-num { font-weight: 750; color: var(--color-heading); }
.stage-hint { font-size: 0.82rem; color: var(--color-muted); }
.skill-row { display: flex; flex-wrap: wrap; gap: 8px; }
.skill-chip { display: inline-flex; align-items: center; gap: 8px; min-height: 40px; padding: 0 12px; border: 1px solid var(--color-border); border-radius: var(--radius-sm); background: var(--color-surface); color: var(--color-primary); font-size: 0.9rem; font-weight: 650; }
.skill-chip:hover { border-color: var(--color-primary); background: var(--color-soft); }
.skill-chip .w { padding-left: 8px; border-left: 1px solid var(--color-border); font-size: 0.75rem; color: var(--color-muted); font-variant-numeric: tabular-nums; }
.error, .done { margin-top: 20px; padding: 14px 16px; border-radius: var(--radius-md); }
.error { background: color-mix(in srgb, var(--color-danger) 9%, var(--color-surface)); }
.done { color: var(--color-success); background: color-mix(in srgb, var(--color-success) 10%, var(--color-surface)); font-weight: 650; }
@media (max-width: 560px) { .stage { padding: 68px 16px 16px; } .stage::before { top: 16px; left: 16px; } }
</style>
