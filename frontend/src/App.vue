<script setup>
import { computed, ref, watch } from 'vue'
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useUserProfileStore } from '@/stores/userProfile'
import { readTheme, writeTheme } from '@/theme'

const auth = useAuthStore()
const profile = useUserProfileStore()
const router = useRouter()
const theme = ref(readTheme(window.localStorage))
const themeMenuOpen = ref(false)
const themeLabels = { growth: '成长', blueprint: '蓝图', night: '夜航', sunrise: '暖阳', mono: '墨白' }
const themeOptions = [
  { id: 'growth', label: '成长', description: '紫色成长感' },
  { id: 'blueprint', label: '蓝图', description: '清爽职业蓝' },
  { id: 'night', label: '夜航', description: '深色专注模式' },
  { id: 'sunrise', label: '暖阳', description: '温暖行动感' },
  { id: 'mono', label: '墨白', description: '高对比工作台' },
]
const themeLabel = computed(() => themeLabels[theme.value])

watch(theme, (value) => {
  document.documentElement.dataset.theme = value
  writeTheme(window.localStorage, value)
}, { immediate: true })

function toggleThemeMenu() {
  themeMenuOpen.value = !themeMenuOpen.value
}

function selectTheme(value) {
  theme.value = value
  themeMenuOpen.value = false
}

function logout() {
  auth.logout()
  profile.reset()
  router.push('/login')
}
</script>

<template>
  <nav class="app-nav">
    <RouterLink to="/" class="brand">技能雷达<span>SKILL RADAR</span></RouterLink>
    <div class="links">
      <RouterLink to="/">岗位推荐</RouterLink>
      <RouterLink to="/gap">技能差距</RouterLink>
      <RouterLink to="/path">学习路径</RouterLink>
      <RouterLink to="/quiz">在线测评</RouterLink>
      <RouterLink to="/mistakes">错题本</RouterLink>
    </div>
    <div class="account">
      <div class="theme-picker">
        <button
          class="theme-toggle"
          type="button"
          aria-haspopup="menu"
          :aria-expanded="themeMenuOpen"
          :aria-label="`选择主题，当前为${themeLabel}主题`"
          @click="toggleThemeMenu"
        >
        <span class="theme-dot" aria-hidden="true"></span>{{ themeLabel }}主题
          <span class="theme-chevron" aria-hidden="true">⌄</span>
        </button>
        <div v-if="themeMenuOpen" class="theme-menu" role="menu" aria-label="主题选择">
          <div class="theme-menu-title">界面主题</div>
          <button
            v-for="option in themeOptions"
            :key="option.id"
            class="theme-option"
            type="button"
            role="menuitemradio"
            :aria-checked="theme === option.id"
            @click="selectTheme(option.id)"
          >
            <span class="theme-swatch" :class="`swatch-${option.id}`" aria-hidden="true"></span>
            <span class="theme-option-copy">
              <strong>{{ option.label }}</strong>
              <small>{{ option.description }}</small>
            </span>
            <span v-if="theme === option.id" class="theme-check" aria-hidden="true">✓</span>
          </button>
        </div>
      </div>
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
  <main class="app-main">
    <RouterView v-slot="{ Component, route }">
      <Transition name="page" mode="out-in">
        <component :is="Component" :key="route.fullPath" />
      </Transition>
    </RouterView>
  </main>
</template>

<style scoped>
nav {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 28px;
  min-height: 76px;
  padding: 14px max(20px, calc((100vw - 1120px) / 2));
  border-bottom: 1px solid var(--color-border);
  background: color-mix(in srgb, var(--color-surface) 94%, transparent);
  color: var(--color-text);
}
.brand { color: var(--color-heading); text-decoration: none; font-weight: 800; letter-spacing: .02em; display: inline-flex; flex-direction: column; line-height: 1.05; }
.brand span { color: var(--color-primary); font-size: .62rem; letter-spacing: .16em; margin-top: .3rem; }
.links { display: flex; gap: 4px; justify-content: center; }
.links a { min-height: 44px; display: inline-flex; align-items: center; padding: 0 13px; border: 1px solid transparent; border-radius: var(--radius-md); color: var(--color-muted); font-size: .92rem; font-weight: 650; }
.links a:hover { color: var(--color-heading); background: var(--color-soft); }
.links a.router-link-exact-active { color: var(--color-primary); border-color: var(--color-border); background: var(--color-surface-raised); }
.account { display: flex; align-items: center; justify-content: flex-end; gap: 10px; font-size: 0.9rem; }
.account a { color: var(--color-muted); font-weight: 600; }
.account a:hover { color: var(--color-primary); }
.who { max-width: 150px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.logout { min-height: 40px; padding: 0 11px; border-color: var(--color-border); color: var(--color-primary); background: transparent; font-size: 0.86rem; }
.logout:hover { color: var(--color-on-primary); background: var(--color-primary); }
.theme-picker { position: relative; }
.theme-toggle { display: inline-flex; align-items: center; gap: .45rem; min-height: 40px; padding: 0 .7rem; border: 1px solid var(--color-border); background: var(--color-surface-raised); color: var(--color-text); border-radius: var(--radius-md); cursor: pointer; font-size: .78rem; }
.theme-toggle:hover { border-color: var(--color-primary); background: var(--color-soft); transform: none; box-shadow: none; }
.theme-dot { width: .6rem; height: .6rem; border-radius: 50%; background: var(--color-accent); box-shadow: 0 0 0 3px var(--color-soft); }
.theme-chevron { margin-left: 2px; color: var(--color-muted); font-size: .9rem; line-height: 1; }
.theme-menu { position: absolute; z-index: 20; top: calc(100% + 10px); right: 0; width: 246px; padding: 8px; border: 1px solid var(--color-border); border-radius: var(--radius-md); background: var(--color-surface); box-shadow: var(--color-shadow); }
.theme-menu-title { padding: 7px 9px 8px; color: var(--color-muted); font-size: .72rem; font-weight: 800; letter-spacing: .08em; text-transform: uppercase; }
.theme-option { display: grid; grid-template-columns: 18px 1fr auto; align-items: center; gap: 10px; width: 100%; min-height: 52px; padding: 8px 9px; border: 1px solid transparent; border-radius: var(--radius-sm); color: var(--color-text); background: transparent; text-align: left; }
.theme-option:hover { color: var(--color-heading); background: var(--color-soft); box-shadow: none; }
.theme-option[aria-checked='true'] { border-color: var(--color-border); background: var(--color-surface-raised); }
.theme-swatch { width: 16px; height: 16px; border-radius: 50%; border: 2px solid var(--color-surface); box-shadow: 0 0 0 1px var(--color-border); }
.swatch-growth { background: #7657d8; }
.swatch-blueprint { background: #0369a1; }
.swatch-night { background: #67b4cf; }
.swatch-sunrise { background: #dc2626; }
.swatch-mono { background: #18181b; }
.theme-option-copy { display: grid; gap: 1px; }
.theme-option-copy strong { font-size: .84rem; font-weight: 750; }
.theme-option-copy small { color: var(--color-muted); font-size: .72rem; }
.theme-check { color: var(--color-success); font-weight: 800; }
@media (max-width: 900px) { nav { grid-template-columns: auto 1fr; gap: 12px 20px; } .account { grid-column: 2; grid-row: 1; } .links { grid-column: 1 / -1; grid-row: 2; justify-content: flex-start; overflow-x: auto; } }
@media (max-width: 560px) { nav { display: flex; flex-wrap: wrap; padding: 12px 10px; } .links { order: 3; width: 100%; justify-content: flex-start; overflow-x: auto; } .account { margin-left: auto; } .who { display: none; } .theme-toggle { padding: 0 9px; } .theme-menu { right: -6px; width: min(246px, calc(100vw - 20px)); } }
</style>
