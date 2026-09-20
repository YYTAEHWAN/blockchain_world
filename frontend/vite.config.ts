import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// 개발 서버는 5173 포트. 백엔드(8080)로의 API 호출은 프록시로 넘겨
// CORS 이슈 없이 /api 를 그대로 사용한다.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
