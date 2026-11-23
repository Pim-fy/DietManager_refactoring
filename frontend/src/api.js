export class ApiError extends Error {
  constructor(message, status, code) { super(message); this.status = status; this.code = code }
}
export const SESSION_KEY = 'dietmanager.access-token.v1'
let renewal
let epoch = 0
export function clearSession() { epoch++; sessionStorage.removeItem(SESSION_KEY) }
export function saveSession(token) { epoch++; sessionStorage.setItem(SESSION_KEY, token) }
export function refreshSession() {
  const startedAt = epoch
  if (!renewal) renewal = request('/refresh', { method: 'POST' }).then(data => {
    if (startedAt !== epoch) throw new ApiError('로그인이 종료되었습니다.', 401)
    sessionStorage.setItem(SESSION_KEY, data.accessToken)
    return data.accessToken
  }).finally(() => { renewal = null })
  return renewal
}
export async function request(path, { token, method = 'GET', body, signal, base = '/api/users' } = {}) {
  return send(false)
  async function send(retried) {
  let response
  try {
    const current = token ? sessionStorage.getItem(SESSION_KEY) || token : null
    response = await fetch(`${base}${path}`, { method, signal, credentials: 'same-origin', headers: {
      ...(body ? { 'Content-Type': 'application/json' } : {}),
      ...(current ? { Authorization: `Bearer ${current}` } : {}),
    }, ...(body ? { body: JSON.stringify(body) } : {}) })
  } catch (error) {
    if (error.name === 'AbortError') throw error
    throw new ApiError('서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.', 0)
  }
  if (response.status === 401 && token && !retried) {
    await refreshSession()
    if (signal?.aborted) throw new DOMException('Aborted', 'AbortError')
    return send(true)
  }
  if (response.status === 204) return null
  const data = await response.json().catch(() => null)
  if (!response.ok) throw new ApiError(data?.message || '요청을 처리하지 못했습니다.', response.status, data?.code)
  if (!data) throw new ApiError('서버 응답을 확인할 수 없습니다.', response.status)
  return data
  }
}
