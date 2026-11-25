import { useCallback, useState } from 'react'

import { clearSession, saveSession, SESSION_KEY } from '../../shared/api/client'
import { authApi } from './api'

export default function useAuth() {
  const [token, setToken] = useState(() => sessionStorage.getItem(SESSION_KEY) || '')
  const [notice, setNotice] = useState('')

  const logout = useCallback((message = '') => {
    authApi.logout().catch(() => {})
    clearSession()
    // 다음 로그인은 기본 화면에서 시작한다.
    window.history.replaceState(null, '', '/')
    setToken('')
    setNotice(message)
  }, [])

  function login(value) {
    saveSession(value)
    setToken(value)
    setNotice('')
  }

  return { token, notice, login, logout }
}
