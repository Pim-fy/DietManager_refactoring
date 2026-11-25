import { test, expect } from '@playwright/test'
import { mkdirSync } from 'node:fs'
import { tmpdir } from 'node:os'
import path from 'node:path'

import { changePasswordByApi, e2eAccount } from './support/account.js'

const screenshotFolder = process.env.E2E_SCREENSHOT_DIR || path.join(tmpdir(), 'dietmanager-e2e')
mkdirSync(screenshotFolder, { recursive: true })
const sessionKey = 'dietmanager.access-token.v1'
// 고정 계정 하나를 반복해 쓴다(global-setup.js가 없으면 가입). 기록은 지울 수 없어(soft delete) 실행마다 쌓이므로,
// 섭취·계획은 기록이 없는 지난 날짜를 골라 쓰고, 신체 기록은 실행마다 다른 몸무게 값으로 이번 실행의 행만 찾는다.
const { email, password, changedPassword } = e2eAccount()
const runTag = Date.now().toString(36).slice(-6)
const runtimeErrors = new WeakMap()

function localDate(date = new Date()) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

/** 화면에 로그인한 세션의 토큰으로 API를 직접 부른다(준비·정리용). */
async function apiOf(page, request) {
  const token = await page.evaluate(key => sessionStorage.getItem(key), sessionKey)
  const headers = { Authorization: `Bearer ${token}` }
  async function body(response) {
    expect(response.ok(), `${response.url()} → HTTP ${response.status()}`).toBe(true)
    return response.status() === 204 ? null : response.json()
  }
  return {
    get: async path => body(await request.get(`/api${path}`, { headers })),
    put: async (path, data) => body(await request.put(`/api${path}`, { headers, data })),
    post: async (path, data) => body(await request.post(`/api${path}`, { headers, data })),
  }
}

/** from~to에서 섭취 기록(삭제 포함)과 식단 계획이 하나도 없는 날짜를 고른다. 이전 실행의 기록과 겹치지 않게 한다. */
async function emptyDate(api, from, to) {
  const start = Date.parse(`${from}T00:00:00Z`)
  const days = Math.floor((Date.parse(`${to}T00:00:00Z`) - start) / 86400000) + 1
  for (let tries = 0; tries < 20; tries++) {
    const date = new Date(start + Math.floor(Math.random() * days) * 86400000).toISOString().slice(0, 10)
    const [daily, plans] = await Promise.all([api.get(`/intakes?date=${date}`), api.get(`/meal-plans?date=${date}`)])
    if (!daily.items.length && !daily.deletedItems.length && !plans.length) return date
  }
  throw new Error(`${from}~${to}에서 기록이 없는 날짜를 찾지 못했습니다.`)
}

async function showDate(page, label, date) {
  await page.getByLabel(label, { exact: true }).fill(date)
  await expect(page.getByLabel(label, { exact: true })).toHaveValue(date)
}

test.beforeEach(async ({ page }) => {
  const errors = []
  runtimeErrors.set(page, errors)
  page.on('pageerror', error => errors.push(error.message))
  page.on('console', message => {
    if (message.type() === 'error' && !message.text().startsWith('Failed to load resource:')) errors.push(message.text())
  })
  await page.goto('/')
  await expect(page).toHaveTitle('DietManager · 건강 기록')
  await page.getByLabel('이메일').fill(email)
  await page.getByLabel('비밀번호', { exact: true }).fill(password)
  await page.locator('form').getByRole('button', { name: '로그인', exact: true }).click()
  await expect(page.getByRole('heading', { name: '대시보드', exact: true })).toBeVisible()
})
test.afterEach(async ({ page }) => {
  await expect(page).toHaveURL(/^http:\/\/127\.0\.0\.1:5186\/(\w+)?(\?.*)?$/)
  await expect(page.locator('vite-error-overlay')).toHaveCount(0)
  expect(runtimeErrors.get(page)).toEqual([])
})

test('동시 401 요청을 한 번 갱신하고 로그아웃한다', async ({ page }) => {
  let refreshCalls = 0
  page.on('request', request => { if (request.url().endsWith('/api/users/refresh')) refreshCalls++ })
  await page.evaluate(key => sessionStorage.setItem(key, 'expired-access-token'), sessionKey)
  await page.reload()
  await expect(page.getByRole('region', { name: '일별 영양 합계' })).toBeVisible()
  expect(refreshCalls).toBe(1)
  const token = await page.evaluate(key => sessionStorage.getItem(key), sessionKey)
  expect(token).not.toBe('expired-access-token')
  await page.getByRole('button', { name: '로그아웃', exact: true }).click()
  await expect(page.getByRole('heading', { name: '로그인', exact: true })).toBeVisible()
  await expect.poll(async () => (await page.context().cookies()).some(cookie => cookie.name === 'dietmanager_refresh')).toBe(false)
})

test('신체 기록 추가·수정·삭제·복구와 모바일 화면', async ({ page }) => {
  const errors = []
  page.on('pageerror', error => errors.push(error.message))
  // 오늘 날짜 기록이 실행마다 쌓이므로 이번 실행만의 몸무게(40~50kg, 수정 후 +10kg)로 행을 찾는다.
  const today = localDate()
  const weight = Number((40 + (Date.now() % 100000) / 10000).toFixed(4))
  const edited = Number((weight + 10).toFixed(4))
  const row = value => page.getByRole('row').filter({ has: page.getByRole('cell', { name: String(value), exact: true }) })
  await page.getByRole('navigation', { name: '주 메뉴' }).getByRole('link', { name: '신체 기록', exact: true }).click()
  await page.getByLabel('키 (cm)').fill('170')
  await page.getByLabel('몸무게 (kg)').fill(String(weight))
  await page.getByRole('button', { name: '기록 저장', exact: true }).click()
  await expect(page.getByText('신체 기록을 저장했습니다.')).toBeVisible()
  // 새 기록 폼의 키 칸은 최근 기록의 키로 채워진다(방금 저장한 기록이 가장 최근이다).
  await expect(page.getByLabel('키 (cm)')).toHaveValue('170')
  await row(weight).getByRole('button', { name: `신체 기록 ${today} ${weight}kg 수정`, exact: true }).click()
  await page.getByLabel('몸무게 (kg)').fill(String(edited))
  await page.getByRole('button', { name: '기록 저장', exact: true }).click()
  await expect(page.locator('.latest')).toContainText(`${edited} kg`)
  await row(edited).getByRole('button', { name: `신체 기록 ${today} ${edited}kg 삭제`, exact: true }).click()
  await expect(page.getByRole('heading', { name: '삭제한 신체 기록' })).toBeVisible()
  await page.locator('.undo-list p').filter({ hasText: `${today} · ${edited} kg` })
    .getByRole('button', { name: `신체 기록 ${today} ${edited}kg 복구`, exact: true }).click()
  await expect(page.locator('.latest')).toContainText(`${edited} kg`)
  await page.setViewportSize({ width: 390, height: 844 })
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
  await page.screenshot({ path: `${screenshotFolder}/maintenance-mobile.png`, fullPage: true })
  expect(errors).toEqual([])
})

test('실제 식품 검색·즐겨찾기·섭취 기록·계획 전환', async ({ page, request }) => {
  const errors = []
  page.on('pageerror', error => errors.push(error.message))
  const api = await apiOf(page, request)
  const date = await emptyDate(api, '2010-01-01', '2023-12-31')
  // 이전 실행의 즐겨찾기를 해제해 '내 식품'에는 이번 실행에서 추가한 식품만 남긴다.
  for (const item of await api.get('/foods/favorites')) await api.put('/foods/favorites', { foodCode: item.food.foodCode, favorite: false })
  await page.getByRole('navigation', { name: '주 메뉴' }).getByRole('link', { name: '섭취 기록', exact: true }).click()
  await showDate(page, '조회 날짜', date)
  await page.getByRole('button', { name: '음식 추가', exact: true }).click()
  await page.getByLabel('식품명').fill('국밥')
  const searchResponse = page.waitForResponse(response => response.url().includes('/api/foods?'))
  await page.getByRole('button', { name: '검색', exact: true }).click()
  const response = await searchResponse
  expect(response.status()).toBe(200)
  await expect(page.locator('.food-results li').first()).toBeVisible()
  const food = (await response.json()).items.find(item => item.baseAmount > 0 && ['g', 'ml'].includes(item.baseUnit))
  expect(food).toBeTruthy()
  await page.getByRole('button', { name: `${food.foodName} 즐겨찾기 추가`, exact: true }).click()
  await expect(page.getByText('즐겨찾기에 추가했습니다.')).toBeVisible()
  await page.getByRole('button', { name: `${food.foodName} 선택`, exact: true }).click()
  await page.getByLabel(`섭취량 (${food.baseUnit})`).fill('100')
  await page.getByRole('button', { name: '섭취 기록 저장', exact: true }).click()
  await expect(page.getByText('섭취 기록을 저장했습니다.')).toBeVisible()
  await page.getByRole('button', { name: `${food.foodName} 삭제`, exact: true }).click()
  await page.getByRole('button', { name: `${food.foodName} 복구`, exact: true }).click()
  await expect(page.getByText('기록을 복구했습니다.')).toBeVisible()
  await page.getByRole('button', { name: `${food.foodName} 영양정보 수정`, exact: true }).click()
  await page.getByLabel('음식명', { exact: true }).fill('API 수정 국밥')
  await page.getByLabel('기준 칼로리 (kcal)').fill('100')
  await page.getByRole('button', { name: '영양정보 저장', exact: true }).click()
  await expect(page.getByRole('button', { name: 'API 수정 국밥 영양정보 수정', exact: true })).toBeVisible()
  await expect(page.getByText('식품 DB · 영양정보 수정함', { exact: true })).toBeVisible()
  await page.getByRole('navigation', { name: '주 메뉴' }).getByRole('link', { name: '식단 계획', exact: true }).click()
  await showDate(page, '계획 조회 날짜', date)
  await page.getByRole('button', { name: '계획 추가', exact: true }).first().click()
  await page.getByRole('tab', { name: '내 식품', exact: true }).click()
  await page.getByRole('button', { name: `즐겨찾기 ${food.foodName} 선택`, exact: true }).click()
  await page.getByLabel(`계획량 (${food.baseUnit})`).fill('50')
  await page.getByRole('button', { name: '계획 저장', exact: true }).click()
  await expect(page.getByText('식단 계획을 저장했습니다.')).toBeVisible()
  await page.getByRole('button', { name: '섭취로 기록', exact: true }).click()
  await expect(page.getByText('섭취 기록으로 옮김', { exact: true })).toBeVisible()
  await expect(page.getByRole('button', { name: '섭취로 기록', exact: true })).toHaveCount(0)
  await page.screenshot({ path: `${screenshotFolder}/maintenance-plan.png`, fullPage: true })
  expect(errors).toEqual([])
})

test('비밀번호 변경 후 재로그인 안내', async ({ page, request }) => {
  let changed = false
  try {
    await page.getByRole('navigation', { name: '주 메뉴' }).getByRole('link', { name: '내 정보', exact: true }).click()
    const section = page.locator('section').filter({ has: page.getByRole('heading', { name: '비밀번호 변경', exact: true }) })
    await section.getByLabel('현재 비밀번호').fill(password)
    await section.getByLabel('새 비밀번호', { exact: true }).fill(changedPassword)
    await section.getByLabel('새 비밀번호 확인').fill(changedPassword)
    const changeResponse = page.waitForResponse(response => response.url().endsWith('/api/users/password') && response.request().method() === 'PATCH')
    await section.getByRole('button', { name: '변경사항 저장' }).click()
    changed = (await changeResponse).ok()
    await expect(page.getByText('비밀번호를 변경했습니다. 새 비밀번호로 다시 로그인해주세요.')).toBeVisible()
    await page.getByLabel('이메일').fill(email)
    await page.getByLabel('비밀번호', { exact: true }).fill(changedPassword)
    await page.locator('form').getByRole('button', { name: '로그인', exact: true }).click()
    await expect(page.getByRole('heading', { name: '대시보드', exact: true })).toBeVisible()
  } finally {
    // 성공·실패와 관계없이 고정 계정 비밀번호를 E2E_PASSWORD로 되돌린다. 강제 종료로 남으면 다음 globalSetup이 되돌린다.
    if (changed) await changePasswordByApi(request, email, changedPassword, password)
  }
})

test('식품 API 한도 안내 후 재시도', async ({ page }) => {
  await page.getByRole('navigation', { name: '주 메뉴' }).getByRole('link', { name: '섭취 기록', exact: true }).click()
  await page.getByRole('button', { name: '음식 추가', exact: true }).click()
  await page.route('**/api/foods?*', route => route.fulfill({ status: 429, contentType: 'application/json',
    body: JSON.stringify({ code: 'FOOD_API_LIMIT', message: '식품 API 호출 한도에 도달했습니다. 잠시 후 다시 시도해주세요.' }) }))
  await page.getByLabel('식품명').fill('국밥')
  await page.getByRole('button', { name: '검색', exact: true }).click()
  await expect(page.getByRole('dialog').getByRole('alert')).toContainText('호출 한도')
  await page.unroute('**/api/foods?*')
  await page.getByRole('button', { name: '검색 다시 시도' }).click()
  await expect(page.locator('.food-results li').first()).toBeVisible()
})

test('창 바깥 클릭은 닫고, 안에서 시작한 드래그는 바깥에서 놓아도 닫지 않는다', async ({ page }) => {
  await page.getByRole('navigation', { name: '주 메뉴' }).getByRole('link', { name: '섭취 기록', exact: true }).click()
  await page.getByRole('button', { name: '음식 추가', exact: true }).click()
  const dialog = page.getByRole('dialog')
  await expect(dialog).toBeVisible()
  const box = await dialog.getByLabel('식품명').boundingBox()
  await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2)
  await page.mouse.down()
  await page.mouse.move(5, 5)
  await page.mouse.up()
  await expect(dialog).toBeVisible()
  await page.mouse.click(5, 5)
  await expect(dialog).toBeHidden()
})

test('다른 식품 고르기로 돌아오면 검색어와 결과가 남아 있다', async ({ page }) => {
  // 실제 식품 API 호출을 늘리지 않도록 검색 응답을 모의한다.
  await page.route(/\/api\/foods\?/, route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({
    items: [{ foodCode: 'MOCK-1', foodName: '모의 식품', makerName: null, servingSize: '100g', baseAmount: 100, baseUnit: 'g',
      calories: 100, carbohydrate: 10, protein: 5, fat: 2 }],
    totalCount: 1, page: 1, size: 10,
  }) }))
  await page.getByRole('navigation', { name: '주 메뉴' }).getByRole('link', { name: '섭취 기록', exact: true }).click()
  await page.getByRole('button', { name: '음식 추가', exact: true }).click()
  await page.getByLabel('식품명').fill('모의')
  await page.getByRole('button', { name: '검색', exact: true }).click()
  await page.getByRole('button', { name: '모의 식품 선택', exact: true }).click()
  await expect(page.getByLabel('섭취량 (g)')).toBeVisible()
  await expect(page.getByRole('tab', { name: '식품 검색', exact: true })).toBeHidden()
  await page.getByRole('button', { name: '다른 식품 고르기', exact: true }).click()
  await expect(page.getByRole('tab', { name: '식품 검색', exact: true })).toHaveAttribute('aria-selected', 'true')
  await expect(page.getByLabel('식품명')).toHaveValue('모의')
  await expect(page.getByRole('button', { name: '모의 식품 선택', exact: true })).toBeVisible()
})

test('음식 직접 입력·영양정보 수정·섭취량 변경·복구·새로고침', async ({ page, request }) => {
  const date = await emptyDate(await apiOf(page, request), '2010-01-01', '2023-12-31')
  await page.getByRole('navigation', { name: '주 메뉴' }).getByRole('link', { name: '섭취 기록', exact: true }).click()
  await showDate(page, '조회 날짜', date)
  await page.getByText('권장량과 기준치는 어떻게 계산하나요?').click()
  const reference = page.getByRole('table', { name: '1일 영양성분 기준치' })
  await expect(reference.getByRole('row')).toHaveCount(9)
  await expect(reference.getByRole('row', { name: '나트륨 2,000 mg' })).toBeVisible()
  await page.getByRole('button', { name: '점심에 음식 추가', exact: true }).click()
  await page.getByRole('tab', { name: '직접 입력', exact: true }).click()
  await page.getByLabel('음식명', { exact: true }).fill('직접 음식')
  await page.getByLabel('섭취량', { exact: true }).fill('150')
  await page.getByLabel('기준 칼로리 (kcal)').fill('200')
  await page.getByLabel('기준 탄수화물 (g)').fill('30')
  await page.getByLabel('기준 지방 (g)').fill('0')
  const savedResponse = page.waitForResponse(response => response.url().endsWith('/api/intakes/manual') && response.request().method() === 'POST')
  await page.getByRole('button', { name: '직접 입력 저장' }).click()
  const created = await (await savedResponse).json()
  expect(created.calories).toBe(300)
  expect(created.protein).toBeNull()
  await expect(page.getByText('음식명과 영양정보를 저장했습니다.')).toBeVisible()
  await page.getByRole('button', { name: '직접 음식 영양정보 수정', exact: true }).click()
  await page.getByLabel('음식명', { exact: true }).fill('직접 수정 음식')
  await page.getByLabel('기준 칼로리 (kcal)').fill('250')
  await page.getByRole('button', { name: '영양정보 저장' }).click()
  await expect(page.getByRole('button', { name: '직접 수정 음식 수정', exact: true })).toBeVisible()
  await page.getByRole('button', { name: '직접 수정 음식 수정', exact: true }).click()
  await page.getByLabel('섭취량 (g)', { exact: true }).fill('200')
  await page.getByRole('button', { name: '수정 저장' }).click()
  await expect(page.locator('.intake-list')).toContainText('500 kcal')
  await page.getByRole('button', { name: '직접 수정 음식 삭제', exact: true }).click()
  await page.getByRole('button', { name: '직접 수정 음식 복구', exact: true }).click()
  await expect(page.locator('.intake-list')).toContainText('500 kcal')
  await page.reload()
  // 조회 날짜가 주소에 담겨 있어 새로고침해도 같은 날짜로 열린다.
  await expect(page.getByLabel('조회 날짜', { exact: true })).toHaveValue(date)
  await expect(page.locator('.intake-list')).toContainText('500 kcal')
  await expect(page.getByText('직접 입력 · 영양정보 수정함', { exact: true })).toBeVisible()
  await page.getByRole('button', { name: '직접 수정 음식 영양정보 수정', exact: true }).click()
  await expect(page.getByLabel('기준 칼로리 (kcal)')).toHaveValue('250')
  await page.setViewportSize({ width: 390, height: 844 })
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
  await page.screenshot({ path: `${screenshotFolder}/manual-nutrition-mobile.png`, fullPage: true })
})


const menu = page => page.getByRole('navigation', { name: '주 메뉴' })

test('섭취 기록 조회 날짜는 주소에 담겨 새로고침해도 유지되고, 잘못된 값은 오늘로 바로잡는다', async ({ page }) => {
  await menu(page).getByRole('link', { name: '섭취 기록', exact: true }).click()
  await expect(page).toHaveURL(/\/intakes$/)
  await showDate(page, '조회 날짜', '2020-03-15')
  await expect(page).toHaveURL(/\/intakes\?date=2020-03-15$/)
  await page.reload()
  await expect(page.getByLabel('조회 날짜', { exact: true })).toHaveValue('2020-03-15')
  await expect(page).toHaveURL(/\/intakes\?date=2020-03-15$/)
  // 메뉴로 다시 들어오면 오늘로 열린다.
  await menu(page).getByRole('link', { name: '식단 계획', exact: true }).click()
  await menu(page).getByRole('link', { name: '섭취 기록', exact: true }).click()
  await expect(page.getByLabel('조회 날짜', { exact: true })).toHaveValue(localDate())
  await expect(page).toHaveURL(/\/intakes$/)
  // 형식이 틀리거나 미래인 날짜는 오늘로 열고 주소도 바로잡는다.
  for (const bad of ['abc', '2026-02-30', '2999-01-01']) {
    await page.goto(`/intakes?date=${bad}`)
    await expect(page.getByLabel('조회 날짜', { exact: true })).toHaveValue(localDate())
    await expect(page).toHaveURL(/\/intakes$/)
  }
  // 식단 계획은 미래 날짜도 허용한다.
  await page.goto('/plans?date=2999-01-01')
  await expect(page.getByLabel('계획 조회 날짜', { exact: true })).toHaveValue('2999-01-01')
  await expect(page).toHaveURL(/\/plans\?date=2999-01-01$/)
})

test('기록 추이의 기간과 기준 날짜는 주소에 담겨 새로고침해도 유지된다', async ({ page }) => {
  await menu(page).getByRole('link', { name: '기록 추이', exact: true }).click()
  await expect(page).toHaveURL(/\/trends$/)
  const period = page.getByRole('group', { name: '기간', exact: true })
  await expect(period.getByRole('button', { name: '주간', exact: true })).toHaveAttribute('aria-pressed', 'true')
  await period.getByRole('button', { name: '월간', exact: true }).click()
  await expect(period.getByRole('button', { name: '월간', exact: true })).toHaveAttribute('aria-pressed', 'true')
  await showDate(page, '기준 날짜', '2020-03-15')
  await expect(page).toHaveURL(/\/trends\?period=month&date=2020-03-15$/)
  await page.reload()
  await expect(page.getByRole('group', { name: '기간', exact: true }).getByRole('button', { name: '월간', exact: true })).toHaveAttribute('aria-pressed', 'true')
  await expect(page.getByLabel('기준 날짜', { exact: true })).toHaveValue('2020-03-15')
  await expect(page.getByText('3월 1일 – 3월 31일')).toBeVisible()
})

test('기록 추이 ‹ › 이동은 주간은 7일, 월간은 한 달 단위로 범위를 바꾼다', async ({ page }) => {
  await page.goto('/trends?date=2020-03-15')
  const range = page.locator('.heading-inline p')
  await expect(range).toHaveText('3월 9일 – 3월 15일')
  await page.getByRole('button', { name: '이전 주', exact: true }).click()
  await expect(range).toHaveText('3월 2일 – 3월 8일')
  await expect(page).toHaveURL(/\/trends\?date=2020-03-08$/)
  await page.getByRole('button', { name: '다음 주', exact: true }).click()
  await page.getByRole('button', { name: '다음 주', exact: true }).click()
  await expect(range).toHaveText('3월 16일 – 3월 22일')
  await expect(page).toHaveURL(/\/trends\?date=2020-03-22$/)

  await page.getByRole('group', { name: '기간', exact: true }).getByRole('button', { name: '월간', exact: true }).click()
  await expect(range).toHaveText('3월 1일 – 3월 31일')
  await page.getByRole('button', { name: '이전 달', exact: true }).click()
  await expect(range).toHaveText('2월 1일 – 2월 29일')
  await expect(page).toHaveURL(/\/trends\?(?=.*period=month)(?=.*date=2020-02-22(&|$))/)
  await page.getByRole('button', { name: '다음 달', exact: true }).click()
  await page.getByRole('button', { name: '다음 달', exact: true }).click()
  await expect(range).toHaveText('4월 1일 – 4월 30일')
  await expect(page).toHaveURL(/\/trends\?(?=.*period=month)(?=.*date=2020-04-22(&|$))/)
})

test('모바일: 하단 탭은 5개이고 내 정보는 상단 바에서 연다', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 })
  const tabs = menu(page).getByRole('link')
  await expect(tabs).toHaveText(['대시보드', '섭취 기록', '식단 계획', '신체 기록', '기록 추이'])
  await expect(menu(page).getByRole('link', { name: '내 정보', exact: true })).toHaveCount(0)
  const account = page.locator('a.header-account')
  await expect(account).toBeVisible()
  await account.click()
  await expect(page.getByRole('heading', { name: '내 정보', exact: true })).toBeVisible()
  await expect(account).toHaveAttribute('aria-current', 'page')
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
  await page.setViewportSize({ width: 1280, height: 900 })
  await expect(account).toBeHidden()
  await expect(menu(page).getByRole('link', { name: '내 정보', exact: true })).toHaveAttribute('aria-current', 'page')
})

test('삭제한 신체 기록은 5개만 보이고 더 보기로 펼쳤다 접는다', async ({ page }) => {
  const deleted = Array.from({ length: 7 }, (_, index) => ({
    measurementId: 9000 + index, measuredDate: `2019-01-${String(20 - index).padStart(2, '0')}`, height: 170, weight: 60 + index, deleted: true,
  }))
  await page.route('**/api/users/body-measurements/deleted', route => route.fulfill({
    status: 200, contentType: 'application/json', body: JSON.stringify(deleted),
  }))
  await menu(page).getByRole('link', { name: '신체 기록', exact: true }).click()
  const list = page.locator('.undo-list')
  await expect(list.locator('p')).toHaveCount(5)
  await list.getByRole('button', { name: '더 보기 (남은 2개)', exact: true }).click()
  await expect(list.locator('p')).toHaveCount(7)
  await expect(list.getByRole('button', { name: /복구$/ })).toHaveCount(7)
  await list.getByRole('button', { name: '접기', exact: true }).click()
  await expect(list.locator('p')).toHaveCount(5)
})

test('음식 추가 창은 뒤로가기로 창만 닫히고, 한 번 더 누르면 이전 화면으로 간다', async ({ page }) => {
  await menu(page).getByRole('link', { name: '섭취 기록', exact: true }).click()
  await showDate(page, '조회 날짜', '2020-03-15')
  await page.getByRole('button', { name: '음식 추가', exact: true }).click()
  await expect(page.getByRole('dialog')).toBeVisible()
  await page.goBack()
  await expect(page.getByRole('dialog')).toBeHidden()
  await expect(page).toHaveURL(/\/intakes\?date=2020-03-15$/)
  await expect(page.getByRole('heading', { name: '섭취 기록', exact: true })).toBeVisible()
  await expect(page.getByLabel('조회 날짜', { exact: true })).toHaveValue('2020-03-15')
  await page.goBack()
  await expect(page.getByRole('heading', { name: '대시보드', exact: true })).toBeVisible()
})

test('코드로 창을 닫으면 남은 기록 단계가 없어 뒤로가기 한 번에 이전 화면으로 간다', async ({ page }) => {
  const dialogStep = () => page.evaluate(() => Boolean(history.state?.dialog))
  await menu(page).getByRole('link', { name: '섭취 기록', exact: true }).click()
  await showDate(page, '조회 날짜', '2020-03-15')
  await page.getByRole('button', { name: '음식 추가', exact: true }).click()
  await expect(page.getByRole('dialog')).toBeVisible()
  expect(await dialogStep()).toBe(true)
  await page.getByRole('dialog').getByRole('button', { name: '닫기', exact: true }).click()
  await expect(page.getByRole('dialog')).toBeHidden()
  await expect.poll(dialogStep).toBe(false)
  await page.goBack()
  await expect(page.getByRole('heading', { name: '대시보드', exact: true })).toBeVisible()
})

test('섭취량 경고: 권장량이 없으면 1일 기준치, 있으면 권장량과 비교', async ({ page, request }) => {
  async function addManual(name, calories, carbohydrate) {
    await page.getByRole('button', { name: '음식 추가', exact: true }).click()
    await page.getByRole('tab', { name: '직접 입력', exact: true }).click()
    await page.getByLabel('음식명', { exact: true }).fill(name)
    await page.getByLabel('섭취량', { exact: true }).fill('100')
    await page.getByLabel('기준 칼로리 (kcal)').fill(String(calories))
    await page.getByLabel('기준 탄수화물 (g)').fill(String(carbohydrate))
    await page.getByRole('button', { name: '직접 입력 저장' }).click()
    await expect(page.getByText('음식명과 영양정보를 저장했습니다.')).toBeVisible()
  }
  const warning = page.getByLabel('섭취량 경고')
  // 권장량 없음: 만 19세 미만 시절의 빈 날짜. 신체 기록·활동량이 이미 있어도 권장량을 계산하지 않아 1일 기준치와 비교한다.
  const api = await apiOf(page, request)
  const birthYear = Number((await api.get('/users/profile')).birthDate.slice(0, 4))
  const minorDate = await emptyDate(api, `${birthYear + 1}-01-01`, `${birthYear + 17}-12-31`)
  await page.getByRole('navigation', { name: '주 메뉴' }).getByRole('link', { name: '섭취 기록', exact: true }).click()
  await showDate(page, '조회 날짜', minorDate)
  await addManual('경고 확인 음식', 100, 10)
  await expect(warning).toContainText('1일 기준치 이내로 섭취했습니다.')
  await addManual('탄수화물 많은 음식', 100, 390)
  await expect(warning.getByRole('alert')).toHaveText('탄수화물: 1일 기준치(324g) 대비 76g 초과 (123.5%)')

  // 권장량 있음: 오늘. 신체 기록·활동량을 저장한 뒤 비교한다(오늘 합계는 실행마다 쌓이지만 칼로리 초과 여부만 본다).
  await page.getByRole('navigation', { name: '주 메뉴' }).getByRole('link', { name: '신체 기록', exact: true }).click()
  await page.getByLabel('키 (cm)').fill('170')
  await page.getByLabel('몸무게 (kg)').fill('70')
  await page.getByRole('button', { name: '기록 저장' }).click()
  await expect(page.getByText('신체 기록을 저장했습니다.')).toBeVisible()
  await page.getByRole('navigation', { name: '주 메뉴' }).getByRole('link', { name: '내 정보', exact: true }).click()
  await page.getByLabel('활동량').selectOption('MODERATE')
  await page.getByRole('button', { name: '활동량 저장' }).click()
  await expect(page.getByText('활동량을 저장했습니다.')).toBeVisible()
  await page.getByRole('navigation', { name: '주 메뉴' }).getByRole('link', { name: '섭취 기록', exact: true }).click()
  await showDate(page, '조회 날짜', localDate())
  await addManual(`칼로리 많은 음식 ${runTag}`, 5000, 0)
  await expect(warning.getByRole('alert')).toContainText(/칼로리: 권장량\([\d,.]+kcal\) 대비 [\d,.]+kcal 초과/)
})

test('화면별 조회를 분리하고 신체 조회 실패가 다른 화면을 막지 않는다', async ({ page }) => {
  const calls = []
  await page.getByRole('navigation', { name: '주 메뉴' }).getByRole('link', { name: '섭취 기록', exact: true }).click()
  await expect(page.getByRole('heading', { name: '섭취 기록', exact: true })).toBeVisible()
  // 첫 화면(대시보드)은 신체 기록도 불러오므로, 섭취 기록 화면으로 옮긴 뒤부터 요청을 센다.
  await page.waitForLoadState('networkidle')
  page.on('request', request => { if (request.url().includes('/api/users/')) calls.push(request.url()) })
  await page.getByRole('navigation', { name: '주 메뉴' }).getByRole('link', { name: '내 정보', exact: true }).click()
  await expect(page.getByRole('heading', { name: '내 정보', exact: true })).toBeVisible()
  expect(calls.some(url => url.includes('/account'))).toBeTruthy()
  expect(calls.some(url => url.includes('/body-measurements'))).toBeFalsy()
  const statisticsResponse = page.waitForResponse(response => response.url().includes('/api/statistics?'))
  await page.getByRole('navigation', { name: '주 메뉴' }).getByRole('link', { name: '기록 추이', exact: true }).click()
  expect((await statisticsResponse).status()).toBe(200)
  await expect(page.getByRole('heading', { name: '기록 추이', exact: true })).toBeVisible()

  await page.route('**/api/users/body-measurements', route => route.fulfill({
    status: 503, contentType: 'application/json', body: JSON.stringify({ code: 'TEST_UNAVAILABLE', message: '신체 조회 검증 오류' }),
  }))
  await page.getByRole('navigation', { name: '주 메뉴' }).getByRole('link', { name: '신체 기록', exact: true }).click()
  await expect(page.getByRole('alert')).toContainText('신체 조회 검증 오류')
  await page.getByRole('navigation', { name: '주 메뉴' }).getByRole('link', { name: '섭취 기록', exact: true }).click()
  await expect(page.getByRole('heading', { name: '섭취 기록', exact: true })).toBeVisible()
})

test('대시보드 요약과 세부 메뉴 이동', async ({ page, request }) => {
  const api = await apiOf(page, request)
  // 오늘 간식 계획을 이번 실행만의 계획량으로 하나 만들고, 대시보드에서 섭취로 옮긴다.
  const foods = await api.get(`/foods?${new URLSearchParams({ keyword: '국밥', page: 1, size: 10 })}`)
  const food = foods.items.find(item => item.baseAmount > 0 && ['g', 'ml'].includes(item.baseUnit))
  expect(food).toBeTruthy()
  const amount = Number((20 + (Date.now() % 100000) / 1000).toFixed(2))
  const plan = await api.post('/meal-plans', { plannedDate: localDate(), mealType: 'SNACK', amount, foodCode: food.foodCode })
  await page.reload()

  const summary = page.getByRole('region', { name: '일별 영양 합계' })
  const plans = page.getByRole('region', { name: '오늘 식단 계획' })
  await expect(page.getByRole('heading', { name: '하루 섭취', exact: true })).toBeVisible()
  for (const name of ['오늘 식단 계획', '몸무게 변화', '이번 주 칼로리 추이'])
    await expect(page.getByRole('heading', { name, exact: true })).toBeVisible()
  await expect(page.getByRole('navigation', { name: '주 메뉴' }).getByRole('link', { name: '대시보드', exact: true })).toHaveAttribute('aria-current', 'page')

  const kcal = async () => Number((await summary.locator('.kcal').innerText()).replace(/[^\d]/g, ''))
  const before = await kcal()
  const row = plans.getByRole('listitem').filter({ hasText: new RegExp(`(^|[^0-9.])${amount} ${food.baseUnit}`) })
  await row.getByRole('button', { name: /섭취로 기록$/ }).click()
  await expect(row.getByText('섭취 기록으로 옮김', { exact: true })).toBeVisible()
  await expect(row.getByRole('button')).toHaveCount(0)
  if (plan.nutrition.calories > 0) await expect.poll(kcal).toBeGreaterThan(before)

  const nav = page.getByRole('navigation', { name: '주 메뉴' })
  for (const [region, link, heading] of [
    [null, '섭취 기록으로', '섭취 기록'],
    ['오늘 식단 계획', '식단 계획으로', '식단 계획'],
    ['몸무게 변화', '신체 기록으로', '신체 기록'],
    ['이번 주 칼로리 추이', '기록 추이로', '기록 추이'],
  ]) {
    const scope = region ? page.getByRole('region', { name: region }) : page.getByRole('main')
    await scope.getByRole('link', { name: link, exact: true }).click()
    await expect(page.getByRole('heading', { name: heading, exact: true })).toBeVisible()
    await expect(nav.getByRole('link', { name: heading, exact: true })).toHaveAttribute('aria-current', 'page')
    await nav.getByRole('link', { name: '대시보드', exact: true }).click()
    await expect(page.getByRole('heading', { name: '대시보드', exact: true })).toBeVisible()
  }

  await page.setViewportSize({ width: 390, height: 844 })
  await expect(summary).toBeVisible()
  await expect(page.locator('a.header-account')).toBeVisible()
  await expect(nav.getByRole('link')).toHaveCount(5)
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
  await page.screenshot({ path: `${screenshotFolder}/dashboard-mobile.png`, fullPage: true })
})

test('시작하기 안내: 신체 기록과 활동량이 있으면 숨기고 없으면 보여준다', async ({ page }) => {
  const setup = page.getByRole('region', { name: '권장 섭취량을 계산하려면 두 가지가 필요합니다' })
  await expect(page.getByRole('region', { name: '일별 영양 합계' })).toBeVisible()
  await expect(setup).toHaveCount(0)

  await page.route('**/api/users/body-measurements', route => route.fulfill({ status: 200, contentType: 'application/json', body: '[]' }))
  await page.route('**/api/nutrition/preferences', route => route.fulfill({
    status: 200, contentType: 'application/json', body: JSON.stringify({ activityLevel: null }),
  }))
  await page.reload()
  await expect(setup).toBeVisible()
  await expect(setup.getByRole('link', { name: '신체 기록 입력하기', exact: true })).toBeVisible()
  await expect(setup.getByRole('link', { name: '활동량 설정하기', exact: true })).toBeVisible()
  await setup.getByRole('link', { name: '활동량 설정하기', exact: true }).click()
  await expect(page.getByRole('heading', { name: '내 정보', exact: true })).toBeVisible()
})

const calorieChart = page => page.locator('.trend').filter({ has: page.locator('h3', { hasText: '칼로리' }) })

test('기록 추이·대시보드 칼로리 그래프에 날짜별 권장 칼로리 기준선을 그린다', async ({ page }) => {
  const today = localDate()
  // 실제 통계 응답에서 권장량은 그대로 두고 오늘 섭취만 채워 그래프가 항상 그려지게 한다(데이터는 만들지 않는다).
  let todayRecommended
  await page.route('**/api/statistics?*', async route => {
    const response = await route.fetch()
    const body = await response.json()
    for (const day of body.days) {
      if (day.date !== today) continue
      todayRecommended = day.recommendedCalories
      Object.assign(day, { records: 1, calories: { value: 1500, missingCount: 0 } })
    }
    await route.fulfill({ response, json: body })
  })

  await page.goto('/trends')
  // 고정 계정은 신체 기록·활동량이 있어 오늘 권장량이 계산된다.
  await expect.poll(() => todayRecommended).toEqual(expect.any(Number))
  const chart = calorieChart(page)
  await expect(chart.locator('.target-line')).not.toHaveCount(0)
  await expect(chart.getByText('섭취', { exact: true })).toBeVisible()
  await expect(chart.getByText('권장량', { exact: true })).toBeVisible()
  await expect(chart.locator('circle.dot title').filter({ hasText: '권장' })).not.toHaveCount(0)
  await expect(page.getByRole('columnheader', { name: '권장 (kcal)', exact: true })).toBeVisible()

  await menu(page).getByRole('link', { name: '대시보드', exact: true }).click()
  const week = page.getByRole('region', { name: '이번 주 칼로리 추이' })
  await expect(week.locator('.target-line')).not.toHaveCount(0)
  await expect(week.getByText('권장량', { exact: true })).toBeVisible()
})

test('권장 칼로리가 없는 날은 기준선을 끊고, 권장량 필드가 없는 응답이면 기준선을 생략한다', async ({ page }) => {
  // 2020-03-09(월)~03-15(일). 셋째·넷째 날은 권장량을 계산할 수 없는 날이다.
  const recommended = [2000, 2000, null, null, 1800, 1800, 1800]
  let legacy = false
  await page.route('**/api/statistics?*', route => {
    const params = new URL(route.request().url()).searchParams
    const start = params.get('start')
    const total = value => ({ value, missingCount: 0 })
    const days = recommended.map((value, index) => {
      const date = new Date(Date.parse(`${start}T00:00:00Z`) + index * 86400000).toISOString().slice(0, 10)
      const day = { date, records: 1, calories: total(1500 + index * 50), carbohydrate: total(100), protein: total(50), fat: total(30), weight: null }
      return legacy ? day : { ...day, recommendedCalories: value }
    })
    return route.fulfill({ json: { start, end: params.get('end'), days } })
  })

  await page.goto('/trends?date=2020-03-15')
  const chart = calorieChart(page)
  await expect(chart.locator('.target-line')).toHaveCount(2)
  await expect(chart.getByText('권장량', { exact: true })).toBeVisible()
  const row = date => page.getByRole('row').filter({ has: page.getByRole('cell', { name: date, exact: true }) })
  await expect(row('2020-03-09').getByRole('cell').nth(3)).toHaveText('2,000')
  await expect(row('2020-03-11').getByRole('cell').nth(3)).toHaveText('–')
  await expect(row('2020-03-13').getByRole('cell').nth(3)).toHaveText('1,800')

  legacy = true
  await page.reload()
  await expect(chart.locator('circle.dot')).toHaveCount(7)
  await expect(chart.locator('.target-line')).toHaveCount(0)
  await expect(chart.getByText('권장량', { exact: true })).toHaveCount(0)
  await expect(row('2020-03-09').getByRole('cell').nth(3)).toHaveText('–')
})

test('스크롤해도 대시보드의 하루 섭취 블록이 아래 내용과 겹치지 않고, 섭취 기록에서는 따라온다', async ({ page }) => {
  await page.setViewportSize({ width: 1280, height: 600 })
  await page.goto('/dashboard')
  await expect(page.locator('.dash-meals')).toBeVisible()
  await page.mouse.wheel(0, 300)
  await expect.poll(() => page.evaluate(() => scrollY)).toBeGreaterThan(0)
  const label = await page.locator('.day-label').boundingBox()
  const meals = await page.locator('.dash-meals').boundingBox()
  expect(label.y + label.height).toBeLessThanOrEqual(meals.y)

  await page.goto('/intakes')
  await expect(page.locator('.day-label')).toBeVisible()
  await page.mouse.wheel(0, 400)
  await expect.poll(() => page.evaluate(() => scrollY)).toBeGreaterThan(0)
  await expect.poll(async () => Math.round((await page.locator('.day-label').boundingBox()).y)).toBe(24)
})
