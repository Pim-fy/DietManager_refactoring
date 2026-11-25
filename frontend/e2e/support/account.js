// E2E 고정 테스트 계정. 값은 환경 변수(E2E_EMAIL, E2E_PASSWORD)나 frontend/.env.local에서 읽는다.

const MISSING_MESSAGE = [
  'E2E 고정 테스트 계정이 설정되지 않았습니다.',
  '환경 변수 E2E_EMAIL, E2E_PASSWORD를 설정하거나 frontend/.env.local에 아래 두 줄을 추가하세요(.env.example 참고).',
  '  E2E_EMAIL=e2e-fixed@example.test',
  '  E2E_PASSWORD=<8자 이상 64자 이하 비밀번호>',
  '계정이 없으면 첫 실행 때 자동으로 가입합니다.',
].join('\n')

/** 고정 계정 정보. 값이 없으면 설정 안내와 함께 실패한다. */
export function e2eAccount() {
  const email = process.env.E2E_EMAIL?.trim()
  const password = process.env.E2E_PASSWORD
  if (!email || !password) throw new Error(MISSING_MESSAGE)
  if (password.length < 8 || password.length > 64) throw new Error('E2E_PASSWORD는 8자 이상 64자 이하여야 합니다(회원가입 규칙).')
  // 비밀번호 변경 테스트가 잠시 쓰는 비밀번호. 매번 같아야 강제 종료 뒤 globalSetup이 원래 비밀번호로 되돌릴 수 있다.
  const changedPassword = `${password.slice(0, 58)}-e2e#2`
  return { email, password, changedPassword }
}

/** /api/users/login 응답을 그대로 돌려준다. */
export function login(request, email, password) {
  return request.post('/api/users/login', { data: { email, password } })
}

/** 로그인해 받은 토큰으로 비밀번호를 바꾼다. 실패하면 원인을 담아 예외를 던진다. */
export async function changePasswordByApi(request, email, currentPassword, newPassword) {
  const loggedIn = await login(request, email, currentPassword)
  if (!loggedIn.ok()) throw new Error(`비밀번호 복구용 로그인 실패: HTTP ${loggedIn.status()} ${await loggedIn.text()}`)
  const { accessToken } = await loggedIn.json()
  const changed = await request.patch('/api/users/password', {
    headers: { Authorization: `Bearer ${accessToken}` },
    data: { currentPassword, newPassword },
  })
  if (!changed.ok()) throw new Error(`비밀번호 복구 실패: HTTP ${changed.status()} ${await changed.text()}`)
}
