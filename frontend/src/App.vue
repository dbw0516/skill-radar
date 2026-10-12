<script setup>
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useUserProfileStore } from '@/stores/userProfile'

const auth = useAuthStore()
const profile = useUserProfileStore()
const router = useRouter()

// 导航按地铁线路编号；用到 AI 的两项挂 AI 标记，演示时评委一眼能看到。
const navItems = [
  { to: '/', label: '岗位推荐' },
  { to: '/gap', label: '技能差距' },
  { to: '/path', label: '学习路径' },
  { to: '/quiz', label: '在线测评', ai: true },
  { to: '/mistakes', label: '错题本' },
  { to: '/interview', label: '模拟面试', ai: true },
]

function logout() {
  auth.logout()
  profile.reset()
  router.push('/login')
}
</script>

<template>
  <header class="app-header">
    <div class="bar">
      <RouterLink to="/" class="brand">
        <svg class="roundel" width="46" height="46" viewBox="0 0 48 48" aria-hidden="true">
          <circle cx="24" cy="24" r="17" fill="none" stroke="#111111" stroke-width="11" />
          <circle cx="24" cy="24" r="17" fill="none" stroke="#ff4d5e" stroke-width="5" />
          <rect x="2" y="17" width="44" height="14" fill="#ffd84d" stroke="#111111" stroke-width="3" />
        </svg>
        <span class="brand-text">
          <strong>技能雷达</strong>
          <span>SKILL RADAR METRO</span>
        </span>
      </RouterLink>
      <nav class="links" aria-label="主导航">
        <RouterLink v-for="(item, i) in navItems" :key="item.to" :to="item.to" class="nav-link">
          <span class="nav-num">{{ i + 1 }}</span>{{ item.label }}<span v-if="item.ai" class="nav-ai">AI</span>
        </RouterLink>
      </nav>
      <div class="account">
        <template v-if="auth.isLoggedIn">
          <RouterLink to="/profile" class="who"><span class="avatar" aria-hidden="true"></span>{{ auth.user.nickname || auth.user.email }}</RouterLink>
          <button class="logout" type="button" @click="logout">退出</button>
        </template>
        <template v-else>
          <RouterLink to="/login" class="who">登录</RouterLink>
          <RouterLink to="/register" class="register">注册</RouterLink>
        </template>
      </div>
    </div>
  </header>
  <main class="app-main">
    <RouterView v-slot="{ Component, route }">
      <Transition name="page" mode="out-in">
        <component :is="Component" :key="route.fullPath" />
      </Transition>
    </RouterView>
  </main>
</template>

<style scoped>
.app-header { width: min(100% - 32px, 1240px); margin: 22px auto 0; }
.bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px 18px;
  padding: 10px 14px;
  border: var(--border-thick);
  border-radius: 14px;
  background: var(--paper);
  box-shadow: 6px 6px 0 var(--ink);
}

.brand { display: inline-flex; align-items: center; gap: 10px; color: var(--ink); }
.brand:hover { color: var(--ink); text-decoration: none; }
.brand-text { display: flex; flex-direction: column; line-height: 1; }
.brand-text strong { font-family: var(--font-display); font-size: 28px; font-weight: 400; }
.brand-text span { margin-top: 4px; font-family: var(--font-num); font-size: 9px; letter-spacing: 0.2em; }

.links { display: flex; flex: 1 1 auto; flex-wrap: nowrap; gap: 2px; }
.nav-link {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 6px;
  min-height: 44px;
  padding: 0 11px 0 7px;
  border: 2.5px solid transparent;
  border-radius: 999px;
  color: var(--ink);
  font-size: 15px;
  font-weight: 900;
}
.nav-link:hover { color: var(--ink); text-decoration: none; background: var(--color-soft); }
.nav-num { display: inline-grid; place-items: center; width: 24px; height: 24px; border: 2.5px solid var(--ink); border-radius: 5px; font-family: var(--font-num); font-size: 12px; font-weight: 400; }
.nav-ai { padding: 0 5px; border: 2px solid var(--ink); border-radius: 4px; background: var(--ai); font-family: var(--font-num); font-size: 10px; font-weight: 400; line-height: 1.5; }
.nav-link.router-link-active { border-color: var(--ink); background: var(--yellow); box-shadow: var(--shadow-sm); }
.nav-link.router-link-active .nav-num { color: var(--yellow); background: var(--ink); }

.account { display: flex; align-items: center; gap: 10px; margin-left: auto; }
.who {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  max-width: 150px;
  min-height: 44px;
  padding: 0 14px 0 6px;
  overflow: hidden;
  border: 2.5px solid var(--ink);
  border-radius: 999px;
  color: var(--ink);
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.who:hover { color: var(--ink); text-decoration: none; background: var(--color-soft); }
.avatar { flex: 0 0 auto; width: 30px; height: 30px; border: 2px solid var(--ink); border-radius: 50%; background: var(--green); }
.register { display: inline-flex; align-items: center; min-height: 44px; padding: 0 16px; border: 2.5px solid var(--ink); border-radius: 10px; color: var(--ink); background: var(--yellow); box-shadow: var(--shadow-sm); }
.register:hover { color: var(--ink); text-decoration: none; }
.logout { min-height: 44px; color: var(--ink); background: var(--paper); box-shadow: var(--shadow-sm); }
.logout:hover:not(:disabled) { box-shadow: 5px 5px 0 var(--ink); }
.logout:active:not(:disabled) { box-shadow: 1px 1px 0 var(--ink); }

/* 一行放不下时，导航整行挪到第二行，可横向滑动，不会断成半截 */
@media (max-width: 1279px) {
  .links { order: 3; flex-basis: 100%; overflow-x: auto; }
}
@media (max-width: 560px) {
  .app-header { width: min(100% - 20px, 680px); }
  .who { max-width: 120px; }
}
</style>
