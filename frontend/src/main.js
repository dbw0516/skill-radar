// 字体打包进项目，比赛现场网络不好也不会退回系统字体。
// fontsource 的中文字体按 unicode-range 切成小块，浏览器只下载页面上实际用到的字。
import '@fontsource/noto-sans-sc/500.css'
import '@fontsource/noto-sans-sc/700.css'
import '@fontsource/noto-sans-sc/900.css'
import '@fontsource/zcool-qingke-huangyou/400.css'
import '@fontsource/archivo-black/400.css'
import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import { useAuthStore } from './stores/auth'
import { useUserProfileStore } from './stores/userProfile'

const app = createApp(App)

app.use(createPinia())
app.use(router)

// 刷新页面时 authStore 会从 localStorage 自己恢复登录态，
// 但 userProfile（targetCategoryId）是纯内存的，得手动从恢复出来的 user 上同步一次。
const auth = useAuthStore()
if (auth.user) {
  useUserProfileStore().syncFromUser(auth.user)
}

app.mount('#app')
