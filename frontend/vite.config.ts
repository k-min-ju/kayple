import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    // 로컬 개발 시 API 를 로컬 Spring(8080)으로 프록시
    proxy: { '/api': 'http://localhost:8080' },
  },
})
