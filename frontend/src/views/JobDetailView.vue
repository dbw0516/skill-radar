<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '@/api/client'
import { useUserProfileStore } from '@/stores/userProfile'
import { useAuthStore } from '@/stores/auth'

// 岗位详情——①岗位推荐列表点进来的那个页面，展示 JD 原文 + 收藏。
const route = useRoute()
const router = useRouter()
const profile = useUserProfileStore()
const auth = useAuthStore()
const posting = ref(null)
const loading = ref(true)
const error = ref('')
const favorited = ref(false)
const favoriteBusy = ref(false)

async function load() {
  loading.value = true
  error.value = ''
  try {
    posting.value = await api.postingDetail(route.params.id)
    if (auth.isLoggedIn) {
      const res = await api.checkFavorite(auth.userId, route.params.id)
      favorited.value = res.favorited
    }
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

async function toggleFavorite() {
  if (!auth.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  favoriteBusy.value = true
  try {
    if (favorited.value) {
      await api.removeFavorite(auth.userId, posting.value.id)
      favorited.value = false
    } else {
      await api.addFavorite(auth.userId, posting.value.id)
      favorited.value = true
    }
  } catch {
    // 收藏失败不弹错误打断阅读，按钮状态就是没变，用户重试一下就行
  } finally {
    favoriteBusy.value = false
  }
}
</script>

<template>
  <section>
    <RouterLink to="/" class="back">← 返回岗位推荐</RouterLink>
    <p v-if="loading">加载中…</p>
    <p v-else-if="error" class="error">{{ error }}</p>
    <template v-else-if="posting">
      <div class="head-row">
        <h1>{{ posting.title }}</h1>
        <button
          class="fav-btn" :class="{ active: favorited }"
          :disabled="favoriteBusy"
          @click="toggleFavorite"
        >
          {{ favorited ? '★ 已收藏' : '☆ 收藏' }}
        </button>
      </div>
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
.head-row { display: flex; justify-content: space-between; align-items: flex-start; gap: 1rem; }
h1 { margin: 0 0 0.6rem; }
.fav-btn { flex: 0 0 auto; padding: 0.4rem 0.9rem; border: 1px solid #dbdee4; border-radius: 999px; background: #fff; color: #5b6472; cursor: pointer; font-size: 0.88rem; white-space: nowrap; }
.fav-btn.active { border-color: #ae5f1c; background: #f6e8da; color: #ae5f1c; }
.fav-btn:disabled { cursor: not-allowed; opacity: 0.6; }
.meta { display: flex; gap: 1rem; flex-wrap: wrap; color: #5b6472; font-size: 0.9rem; margin-bottom: 1rem; }
.salary { color: #ae5f1c; }
.status { padding: 1px 8px; border-radius: 999px; font-size: 0.8rem; }
.status.open { background: #e3f0eb; color: #2b6e5c; }
.status.closed { background: #f6e8da; color: #ae5f1c; }
.jd { white-space: pre-wrap; word-break: break-word; background: #fff; border: 1px solid #dbdee4; border-radius: 8px; padding: 1rem 1.2rem; font-family: inherit; font-size: 0.92rem; line-height: 1.7; color: #1c232e; max-height: 60vh; overflow-y: auto; }
.cta { display: inline-block; margin-top: 1.2rem; color: #2b6e5c; font-weight: 600; text-decoration: none; }
.error { color: #b3261e; }
</style>
