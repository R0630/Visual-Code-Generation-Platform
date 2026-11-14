import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue(), vueDevTools()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173, // 固定使用 5173 端口
    strictPort: true, // 如果 5173 被占用，直接报错退出，而不是自动换到 5174
    proxy: {
      '/api': {
        target: 'http://localhost:8123',
        changeOrigin: true,
      },
      // 部署作品访问链接（xinggou）：前端用 /xinggou/{key}/，
      // rewrite 后后端实际收到 /api/xinggou/{key}/
      '/xinggou': {
        target: 'http://localhost:8123',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/xinggou/, '/api/xinggou'),
      },
    },
  },
})
