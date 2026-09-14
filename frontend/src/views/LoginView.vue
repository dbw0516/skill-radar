<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '@/api/client'
import { useAuthStore } from '@/stores/auth'
import { useUserProfileStore } from '@/stores/userProfile'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const profile = useUserProfileStore()

const email = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)

async function submit() {
  loading.value = true
  error.value = ''
  try {
    const user = await api.login({ email: email.value, password: password.value })
    auth.setUser(user)
    profile.syncFromUser(user)
    router.push(route.query.redirect || '/')
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="auth">
    <h1>登录</h1>
    <form @submit.prevent="submit">
      <label>邮箱<input v-model="email" type="email" required autocomplete="email" /></label>
      <label>密码<input v-model="password" type="password" required autocomplete="current-password" /></label>
      <p v-if="error" class="error">{{ error }}</p>
      <button type="submit" :disabled="loading">{{ loading ? '登录中…' : '登录' }}</button>
    </form>
    <p class="hint">
      还没有账号？<RouterLink to="/register">去注册</RouterLink><br />
      本地测试可以用演示账号：demo@example.com / demo1234
    </p>
  </section>
</template>

<style scoped>
.auth { max-width: 460px; }
.auth::before { content: 'SKILL RADAR / ACCOUNT'; display: block; margin-bottom: 12px; color: var(--color-primary); font-size: 0.72rem; font-weight: 800; letter-spacing: 0.12em; }
form { display: flex; flex-direction: column; gap: 16px; max-width: 380px; margin-top: 24px; }
label { display: flex; flex-direction: column; gap: 6px; color: var(--color-heading); font-size: 0.9rem; font-weight: 650; }
button { width: 100%; margin-top: 4px; }
.error { margin: 0; padding: 10px 12px; border-radius: var(--radius-sm); background: color-mix(in srgb, var(--color-danger) 9%, var(--color-surface)); font-size: 0.88rem; }
.hint { max-width: 380px; margin-top: 20px; color: var(--color-muted); font-size: 0.85rem; line-height: 1.6; }
</style>
