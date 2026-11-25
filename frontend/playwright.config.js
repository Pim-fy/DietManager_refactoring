import { defineConfig } from '@playwright/test'
import { existsSync, readFileSync } from 'node:fs'
import { parseEnv } from 'node:util'

// 고정 테스트 계정(E2E_EMAIL, E2E_PASSWORD)은 frontend/.env.local에 둘 수 있다. 이미 설정된 환경 변수가 우선한다.
const envFile = new URL('./.env.local', import.meta.url)
if (existsSync(envFile)) {
  for (const [key, value] of Object.entries(parseEnv(readFileSync(envFile, 'utf8')))) {
    if (key.startsWith('E2E_') && !process.env[key]) process.env[key] = value
  }
}

export default defineConfig({
  testDir: './e2e', timeout: 90000, workers: 1, fullyParallel: false,
  reporter: 'list',
  globalSetup: './e2e/global-setup.js',
  use: { baseURL: 'http://127.0.0.1:5186', channel: 'chrome', viewport: { width: 1280, height: 900 } },
})
