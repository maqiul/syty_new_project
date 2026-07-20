import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'
import { resolve } from 'path'

export default defineConfig({
  plugins: [uni()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
    },
  },
  server: {
    port: 3000,
    host: '0.0.0.0',
  },
  css: {
    preprocessorOptions: {
      scss: {
        // 全局注入 uview-plus 的 SCSS mixin 和主题变量
        additionalData: '@import "uview-plus/theme.scss";',
      },
    },
  },
})
