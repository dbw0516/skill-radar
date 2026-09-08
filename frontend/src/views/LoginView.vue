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
.auth { max-width: 340px; }
form { display: flex; flex-direction: column; gap: 0.9rem; margin-top: 1rem; }
label { display: flex; flex-direction: column; gap: 0.3rem; font-size: 0.9rem; color: #5b6472; }
input { padding: 0.5rem 0.6rem; border: 1px solid #dbdee4; border-radius: 6px; font-size: 0.95rem; }
button { margin-top: 0.3rem; padding: 0.55rem; background: #2b6e5c; color: #fff; border: none; border-radius: 6px; cursor: pointer; font-size: 0.95rem; }
button:disabled { background: #b7c4bf; cursor: not-allowed; }
.error { color: #b3261e; font-size: 0.88rem; margin: 0; }
.hint { margin-top: 1.2rem; font-size: 0.85rem; color: #8891a0; line-height: 1.6; }
</style>
