import { defineStore } from 'pinia'

const STORAGE_KEY = 'skill-radar-user'

function loadStoredUser() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

// 登录状态先用 localStorage 存用户信息，没有真正的会话令牌——
// 后端也还没做真实鉴权（见 AuthUser.java 里的注释），先满足"能注册登录、
// 数据落到共享数据库里"这个最基本的需求，以后要加固再升级成 JWT。
export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: loadStoredUser(),
  }),
  getters: {
    isLoggedIn: (state) => !!state.user,
    userId: (state) => state.user?.id,
  },
  actions: {
    setUser(user) {
      this.user = user
      localStorage.setItem(STORAGE_KEY, JSON.stringify(user))
    },
    logout() {
      this.user = null
      localStorage.removeItem(STORAGE_KEY)
    },
  },
})
