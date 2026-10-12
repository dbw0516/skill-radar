<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '@/api/client'
import { useUserProfileStore } from '@/stores/userProfile'
import { useAuthStore } from '@/stores/auth'

// 岗位详情——①岗位推荐列表点进来的那个页面，展示 JD 原文 + 收藏 + 我和这条岗位的技能匹配度。
const route = useRoute()
const router = useRouter()
const profile = useUserProfileStore()
const auth = useAuthStore()
const posting = ref(null)
const loading = ref(true)
const error = ref('')
const favorited = ref(false)
const favoriteBusy = ref(false)
const match = ref(null)

async function load() {
  loading.value = true
  error.value = ''
  try {
    posting.value = await api.postingDetail(route.params.id)
    match.value = null
    if (auth.isLoggedIn) {
      const [res, m] = await Promise.all([
        api.checkFavorite(auth.userId, route.params.id),
        api.postingMatch(route.params.id, auth.userId).catch(() => null),
      ])
      favorited.value = res.favorited
      match.value = m
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
      <div v-if="match && (match.matched.length + match.missing.length)" class="match-card">
        <div class="match-head">
          <span class="match-pct" :class="{ good: match.percent >= 70 }">{{ match.percent }}%</span>
          <div>
            <strong>你和这个岗位的技能匹配度</strong>
            <p class="match-sub">
              {{ match.basis === 'posting' ? '按这条 JD 里提到的技能计算' : '这条 JD 没有识别出具体技能，按同类岗位的常见要求估算' }}
              <template v-if="match.percent >= 70"> · 已达到可投水平 👍</template>
            </p>
          </div>
        </div>
        <div v-if="match.matched.length" class="chips">
          <span v-for="s in match.matched" :key="s.id" class="chip have">✓ {{ s.name }}</span>
        </div>
        <div v-if="match.missing.length" class="chips">
          <span class="chips-label">还差：</span>
          <RouterLink
            v-for="s in match.missing" :key="s.id" class="chip lack"
            :to="{ path: '/quiz', query: { skillId: s.id, name: s.name } }"
            title="去学习并测评这个技能"
          >{{ s.name }} →</RouterLink>
        </div>
      </div>
      <p v-else-if="!auth.isLoggedIn" class="match-login"><RouterLink :to="{ path: '/login', query: { redirect: route.fullPath } }">登录</RouterLink>后可以看到你和这个岗位的技能匹配度。</p>
      <pre class="jd">{{ posting.rawText }}</pre>
      <RouterLink to="/gap" class="cta" @click="goToGap">看这个方向的技能差距 →</RouterLink>
    </template>
  </section>
</template>

<style scoped>
.back { margin-bottom: 8px; }
.head-row { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; }
h1 { margin: 0 0 10px; }
.fav-btn, .fav-btn:hover:not(:disabled) { flex: 0 0 auto; min-height: 44px; color: var(--ink); background: var(--paper); box-shadow: var(--shadow-sm); white-space: nowrap; }
.fav-btn:hover:not(:disabled) { box-shadow: 5px 5px 0 var(--ink); }
.fav-btn.active, .fav-btn.active:hover:not(:disabled) { background: var(--yellow); }
.meta { display: flex; gap: 8px; flex-wrap: wrap; color: var(--color-muted); font-size: 0.9rem; margin-bottom: 20px; }
.meta > span { padding: 4px 9px; border: 1px solid var(--color-border); border-radius: var(--radius-sm); background: var(--color-surface-raised); }
.salary { color: var(--color-warning); font-weight: 700; }
.status.open { border-color: color-mix(in srgb, var(--color-success) 30%, var(--color-border)); background: color-mix(in srgb, var(--color-success) 10%, var(--color-surface)); color: var(--color-success); }
.status.closed { border-color: color-mix(in srgb, var(--color-warning) 35%, var(--color-border)); background: color-mix(in srgb, var(--color-warning) 12%, var(--color-surface)); color: var(--color-warning); }
.jd { max-height: 60vh; overflow-y: auto; white-space: pre-wrap; overflow-wrap: anywhere; padding: 20px; border: var(--border-thick); border-radius: var(--radius-md); color: var(--color-heading); background: var(--color-surface-raised); font-family: inherit; font-size: 0.94rem; line-height: 1.75; }
.match-card { margin-bottom: 22px; padding: 18px 20px; border: var(--border-thick); border-radius: var(--radius-md); background: var(--paper); box-shadow: var(--shadow-md); }
.match-head { display: flex; align-items: center; gap: 14px; }
.match-head strong { color: var(--color-heading); }
.match-pct { flex: 0 0 auto; display: grid; place-items: center; width: 72px; height: 72px; border: var(--border-thick); border-radius: 50%; background: var(--orange); box-shadow: var(--shadow-sm); color: var(--ink); font-family: var(--font-num); font-size: 1.15rem; }
.match-pct.good { background: var(--green); }
.match-sub { margin: 4px 0 0; color: var(--color-muted); font-size: 0.82rem; }
.chips { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; margin-top: 12px; }
.chips-label { color: var(--color-muted); font-size: 0.85rem; }
.chip { padding: 4px 10px; border: 1px solid var(--color-border); border-radius: 999px; font-size: 0.84rem; font-weight: 650; text-decoration: none; }
.chip.have { border-color: color-mix(in srgb, var(--color-success) 35%, var(--color-border)); background: color-mix(in srgb, var(--color-success) 9%, var(--color-surface)); color: var(--color-success); }
.chip.lack { color: var(--color-primary); background: var(--color-surface); }
.chip.lack:hover { border-color: var(--color-primary); background: var(--color-soft); }
.match-login { margin: 0 0 16px; color: var(--color-muted); font-size: 0.88rem; }
.error { margin-top: 20px; padding: 14px 16px; border-radius: var(--radius-md); background: color-mix(in srgb, var(--color-danger) 9%, var(--color-surface)); }
@media (max-width: 560px) { .head-row { flex-direction: column; } .fav-btn { width: 100%; } }
</style>
