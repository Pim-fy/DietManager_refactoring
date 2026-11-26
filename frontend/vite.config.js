import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const target = loadEnv(mode, '.', 'VITE_').VITE_API_PROXY_TARGET || 'http://localhost:8080'
  return {
    plugins: [react()],
    server: { port: 5186, strictPort: true, proxy: { '/api': target } },
    preview: { port: 5186, strictPort: true, proxy: { '/api': target } },
    test: { include: ['src/**/*.test.js'] },
  }
})
