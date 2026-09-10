<script setup>
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useUserProfileStore } from '@/stores/userProfile'

const auth = useAuthStore()
const profile = useUserProfileStore()
const router = useRouter()

function logout() {
  auth.logout()
  profile.reset()
  router.push('/login')
}
</script>

<template>
  <nav>
    <div class="links">
      <RouterLink to="/">岗位推荐</RouterLink>
      <RouterLink to="/gap">技能差距</RouterLink>
      <RouterLink to="/path">学习路径</RouterLink>
      <RouterLink to="/quiz">在线测评</RouterLink>
    </div>
    <div class="account">
      <template v-if="auth.isLoggedIn">
        <RouterLink to="/profile" class="who">{{ auth.user.nickname || auth.user.email }}</RouterLink>
        <button class="logout" @click="logout">退出登录</button>
      </template>
      <template v-else>
        <RouterLink to="/login">登录</RouterLink>
        <RouterLink to="/register">注册</RouterLink>
      </template>
    </div>
  </nav>
  <main>
    <RouterView />
  </main>
</template>

<style scoped>
nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1.5rem;
  padding: 1rem 1.5rem;
  border-bottom: 1px solid #e0e0e0;
}
.links { display: flex; gap: 1.5rem; }
.account { display: flex; align-items: center; gap: 1rem; font-size: 0.9rem; }
nav a {
  text-decoration: none;
  color: #333;
}
nav a.router-link-exact-active {
  color: #2b6e5c;
  font-weight: 600;
}
.who { color: #5b6472; }
.logout { background: none; border: none; color: #2b6e5c; cursor: pointer; font-size: 0.9rem; padding: 0; }
main {
  padding: 1.5rem;
  max-width: 860px;
  margin: 0 auto;
}
</style>
