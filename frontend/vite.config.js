import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    // 允许通过 Cloudflare Tunnel 的 *.trycloudflare.com 域名访问 dev server，
    // 不加这个 Vite 会拒绝并提示 "Blocked request. This host is not allowed."
    allowedHosts: true,
  },
})
