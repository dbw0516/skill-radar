<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '@/api/client'
import { useAuthStore } from '@/stores/auth'
import { useUserProfileStore } from '@/stores/userProfile'

const router = useRouter()
const auth = useAuthStore()
const profile = useUserProfileStore()

const email = ref('')
const password = ref('')
const nickname = ref('')
const error = ref('')
const loading = ref(false)

async function submit() {
  loading.value = true
  error.value = ''
  try {
    const user = await api.register({ email: email.value, password: password.value, nickname: nickname.value })
    auth.setUser(user)
    profile.syncFromUser(user)
    router.push('/')
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="auth">
    <h1>注册</h1>
    <form @submit.prevent="submit">
      <label>昵称<input v-model="nickname" type="text" placeholder="选填" autocomplete="nickname" /></label>
      <label>邮箱<input v-model="email" type="email" required autocomplete="email" /></label>
      <label>密码<input v-model="password" type="password" required minlength="6" autocomplete="new-password" /></label>
      <p v-if="error" class="error">{{ error }}</p>
      <button type="submit" :disabled="loading">{{ loading ? '注册中…' : '注册' }}</button>
    </form>
    <p class="hint">已经有账号？<RouterLink to="/login">去登录</RouterLink></p>
  </section>
</template>

<style scoped>
.auth { max-width: 460px; }
.auth::before { content: 'SKILL RADAR / CREATE PROFILE'; display: block; margin-bottom: 12px; color: var(--color-primary); font-size: 0.72rem; font-weight: 800; letter-spacing: 0.12em; }
form { display: flex; flex-direction: column; gap: 16px; max-width: 380px; margin-top: 24px; }
label { display: flex; flex-direction: column; gap: 6px; color: var(--color-heading); font-size: 0.9rem; font-weight: 650; }
button { width: 100%; margin-top: 4px; }
.error { margin: 0; padding: 10px 12px; border-radius: var(--radius-sm); background: color-mix(in srgb, var(--color-danger) 9%, var(--color-surface)); font-size: 0.88rem; }
.hint { margin-top: 20px; color: var(--color-muted); font-size: 0.85rem; }
</style>
