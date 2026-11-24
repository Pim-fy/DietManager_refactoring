export class ApiError extends Error {
  constructor(message, status, code) {
    super(message)
    this.status = status
    this.code = code
  }
}

export const SESSION_KEY = 'dietmanager.access-token.v1'

let renewal
let epoch = 0

export function clearSession() {
  epoch++
  sessionStorage.removeItem(SESSION_KEY)
}

export function saveSession(token) {
  epoch++
  sessionStorage.setItem(SESSION_KEY, token)
}

/** 동시에 여러 요청이 401을 받아도 갱신은 한 번만 보낸다. */
export function refreshSession() {
  const startedAt = epoch
  if (!renewal) {
    renewal = request('/users/refresh', { method: 'POST' })
      .then(data => {
        if (startedAt !== epoch) throw new ApiError('로그인이 종료되었습니다.', 401)
        sessionStorage.setItem(SESSION_KEY, data.accessToken)
        return data.accessToken
      })
      .finally(() => {
        renewal = null
      })
  }
  return renewal
}

/**
 * 모든 API 요청의 공통 진입점. path는 /api 뒤의 경로(예: /users/account, /intakes).
 * token을 주면 Authorization 헤더를 붙이고, 401이면 한 번 갱신한 뒤 다시 요청한다.
 */
export async function request(path, { token, method = 'GET', body, signal } = {}) {
  return send(false)

  async function send(retried) {
    let response
    try {
      const current = token ? sessionStorage.getItem(SESSION_KEY) || token : null
      response = await fetch(`/api${path}`, {
        method,
        signal,
        credentials: 'same-origin',
        headers: {
          ...(body ? { 'Content-Type': 'application/json' } : {}),
          ...(current ? { Authorization: `Bearer ${current}` } : {}),
        },
        ...(body ? { body: JSON.stringify(body) } : {}),
      })
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
