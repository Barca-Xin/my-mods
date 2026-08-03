import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => ({
  plugins: [vue()],
  // 静态模式部署到 GitHub Pages 子路径（username.github.io/repo/），资源用相对路径
  base: mode === 'static' ? './' : '/',
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
      '/files': 'http://localhost:8080'
    }
  }
}))
