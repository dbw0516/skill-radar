<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '@/api/client'
import { useUserProfileStore } from '@/stores/userProfile'

// 岗位详情——①岗位推荐列表点进来的那个页面，展示 JD 原文。
const route = useRoute()
const profile = useUserProfileStore()
const posting = ref(null)
const loading = ref(true)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    posting.value = await api.postingDetail(route.params.id)
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
onMounted(load)
watch(() => route.params.id, load)

function goToGap() {
  if (posting.value?.categoryId) profile.setTargetCategory(posting.value.categoryId)
}
</script>

<template>
  <section>
    <RouterLink to="/" class="back">← 返回岗位推荐</RouterLink>
    <p v-if="loading">加载中…</p>
    <p v-else-if="error" class="error">{{ error }}</p>
    <template v-else-if="posting">
      <h1>{{ posting.title }}</h1>
      <div class="meta">
        <span>{{ posting.companyName }}</span>
        <span>{{ posting.location || '地点未标注' }}</span>
        <span class="salary">{{ posting.salaryText || '薪资未标注' }}</span>
        <span class="status" :class="posting.status">{{ posting.status === 'open' ? '在招' : '已关闭' }}</span>
      </div>
      <pre class="jd">{{ posting.rawText }}</pre>
      <RouterLink to="/gap" class="cta" @click="goToGap">看这个方向的技能差距 →</RouterLink>
    </template>
  </section>
</template>

<style scoped>
.back { display: inline-block; margin-bottom: 1rem; color: #5b6472; text-decoration: none; font-size: 0.9rem; }
h1 { margin: 0 0 0.6rem; }
.meta { display: flex; gap: 1rem; flex-wrap: wrap; color: #5b6472; font-size: 0.9rem; margin-bottom: 1rem; }
.salary { color: #ae5f1c; }
.status { padding: 1px 8px; border-radius: 999px; font-size: 0.8rem; }
.status.open { background: #e3f0eb; color: #2b6e5c; }
.status.closed { background: #f6e8da; color: #ae5f1c; }
.jd { white-space: pre-wrap; word-break: break-word; background: #fff; border: 1px solid #dbdee4; border-radius: 8px; padding: 1rem 1.2rem; font-family: inherit; font-size: 0.92rem; line-height: 1.7; color: #1c232e; max-height: 60vh; overflow-y: auto; }
.cta { display: inline-block; margin-top: 1.2rem; color: #2b6e5c; font-weight: 600; text-decoration: none; }
.error { color: #b3261e; }
</style>
