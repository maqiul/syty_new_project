import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 3000,
    proxy: {
      '/api/platform': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/api/user': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/api/role': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/api/permission': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/api/menu': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/api/operate-log': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/api/v1': {
        target: 'http://localhost:8081',
        changeOrigin: true
      }
    }
  }
})
