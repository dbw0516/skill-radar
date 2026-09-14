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
.back { margin-bottom: 8px; }
.head-row { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; }
h1 { margin: 0 0 10px; }
.fav-btn { flex: 0 0 auto; min-height: 44px; border-color: var(--color-border); color: var(--color-primary); background: var(--color-surface-raised); white-space: nowrap; }
.fav-btn:hover { background: var(--color-soft); box-shadow: none; }
.fav-btn.active { border-color: color-mix(in srgb, var(--color-warning) 45%, var(--color-border)); background: color-mix(in srgb, var(--color-warning) 12%, var(--color-surface)); color: var(--color-warning); }
.meta { display: flex; gap: 8px; flex-wrap: wrap; color: var(--color-muted); font-size: 0.9rem; margin-bottom: 20px; }
.meta > span { padding: 4px 9px; border: 1px solid var(--color-border); border-radius: var(--radius-sm); background: var(--color-surface-raised); }
.salary { color: var(--color-warning); font-weight: 700; }
.status.open { border-color: color-mix(in srgb, var(--color-success) 30%, var(--color-border)); background: color-mix(in srgb, var(--color-success) 10%, var(--color-surface)); color: var(--color-success); }
.status.closed { border-color: color-mix(in srgb, var(--color-warning) 35%, var(--color-border)); background: color-mix(in srgb, var(--color-warning) 12%, var(--color-surface)); color: var(--color-warning); }
.jd { max-height: 60vh; overflow-y: auto; white-space: pre-wrap; overflow-wrap: anywhere; padding: 20px; border: 1px solid var(--color-border); border-radius: var(--radius-md); color: var(--color-heading); background: var(--color-surface-raised); font-family: inherit; font-size: 0.94rem; line-height: 1.75; }
.error { margin-top: 20px; padding: 14px 16px; border-radius: var(--radius-md); background: color-mix(in srgb, var(--color-danger) 9%, var(--color-surface)); }
@media (max-width: 560px) { .head-row { flex-direction: column; } .fav-btn { width: 100%; } }
</style>
