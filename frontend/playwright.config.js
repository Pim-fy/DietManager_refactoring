import { defineConfig } from '@playwright/test'
export default defineConfig({
  testDir: './e2e', timeout: 90000, workers: 1, fullyParallel: false,
  reporter: 'list',
  use: { baseURL: 'http://127.0.0.1:5186', channel: 'chrome', viewport: { width: 1280, height: 900 } },
})
