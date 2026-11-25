import { request as playwrightRequest } from '@playwright/test'

import { changePasswordByApi, e2eAccount, login } from './support/account.js'

/** 고정 계정이 없으면 가입하고, 로그인할 수 있는지 확인한다. */
export default async function globalSetup(config) {
  const { email, password, changedPassword } = e2eAccount()
  const baseURL = config.projects[0].use.baseURL
  const request = await playwrightRequest.newContext({ baseURL })
  try {
    const registered = await request.post('/api/users/register', {
      data: { email, password, userName: 'E2E 고정 계정', gender: 'MALE', birthDate: '1990-01-01', goalType: 'MAINTAIN_WEIGHT' },
    }).catch(error => {
      throw new Error(`E2E 준비 실패: ${baseURL}/api 에 연결할 수 없습니다. 백엔드·프론트 서버를 먼저 실행하세요.\n${error.message}`)
    })
    if (registered.status() >= 502)
      throw new Error(`E2E 준비 실패: 프론트 개발 서버가 백엔드에 연결하지 못했습니다(HTTP ${registered.status()}). 백엔드 서버와 VITE_API_PROXY_TARGET을 확인하세요.`)
    if (registered.status() !== 201 && registered.status() !== 409)
      throw new Error(`E2E 준비 실패: 고정 계정 가입 요청이 HTTP ${registered.status()}로 실패했습니다.\n${await registered.text()}`)

    let loggedIn = await login(request, email, password)
    if (loggedIn.status() === 401) {
      // 비밀번호 변경 테스트가 강제 종료돼 임시 비밀번호로 남았으면 되돌린다.
      const changed = await login(request, email, changedPassword)
      if (changed.ok()) {
        await changePasswordByApi(request, email, changedPassword, password)
        loggedIn = await login(request, email, password)
      }
    }
    if (loggedIn.status() === 401)
      throw new Error('E2E 준비 실패: E2E_EMAIL 계정이 이미 있지만 E2E_PASSWORD로 로그인할 수 없습니다. 비밀번호를 확인하거나 다른 E2E_EMAIL을 쓰세요.')
    if (loggedIn.status() === 429)
      throw new Error('E2E 준비 실패: 로그인 실패가 반복되어 제한되었습니다(계정·IP 기준 10분). 잠시 후 다시 실행하세요.')
    if (!loggedIn.ok()) throw new Error(`E2E 준비 실패: 고정 계정 로그인이 HTTP ${loggedIn.status()}로 실패했습니다.\n${await loggedIn.text()}`)

    // 섭취량 경고 테스트는 만 19세 미만 날짜(권장량 없음)와 오늘(권장량 있음)을 함께 쓴다.
    const { accessToken } = await loggedIn.json()
    const profile = await request.get('/api/users/profile', { headers: { Authorization: `Bearer ${accessToken}` } })
    if (!profile.ok()) throw new Error(`E2E 준비 실패: 고정 계정 프로필 조회가 HTTP ${profile.status()}로 실패했습니다.`)
    const birthYear = Number((await profile.json()).birthDate.slice(0, 4))
    if (new Date().getFullYear() - birthYear < 20)
      throw new Error('E2E 준비 실패: 고정 계정은 만 20세 이상이어야 합니다(섭취량 경고 테스트가 권장량을 계산). 생년월일을 확인하세요.')
  } finally {
    await request.dispose()
  }
}
