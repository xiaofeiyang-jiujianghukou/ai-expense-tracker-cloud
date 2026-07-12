import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    host: '0.0.0.0',
    allowedHosts: ['.expense.com', 'localhost'],
    proxy: {
      '/api': {
        target: 'http://gateway:8080',
        changeOrigin: true
      }
    }
  }
})
