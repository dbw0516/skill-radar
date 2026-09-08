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
.auth { max-width: 340px; }
form { display: flex; flex-direction: column; gap: 0.9rem; margin-top: 1rem; }
label { display: flex; flex-direction: column; gap: 0.3rem; font-size: 0.9rem; color: #5b6472; }
input { padding: 0.5rem 0.6rem; border: 1px solid #dbdee4; border-radius: 6px; font-size: 0.95rem; }
button { margin-top: 0.3rem; padding: 0.55rem; background: #2b6e5c; color: #fff; border: none; border-radius: 6px; cursor: pointer; font-size: 0.95rem; }
button:disabled { background: #b7c4bf; cursor: not-allowed; }
.error { color: #b3261e; font-size: 0.88rem; margin: 0; }
.hint { margin-top: 1.2rem; font-size: 0.85rem; color: #8891a0; }
</style>
