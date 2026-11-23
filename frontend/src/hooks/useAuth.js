import { useCallback, useState } from 'react'
import { request, SESSION_KEY, clearSession, saveSession } from '../api'
export default function useAuth() {
  const [token, setToken] = useState(() => sessionStorage.getItem(SESSION_KEY) || '')
  const [notice, setNotice] = useState('')
  const logout = useCallback((message = '') => {
    request('/logout', { method: 'POST' }).catch(() => {})
    clearSession(); setToken(''); setNotice(message)
  }, [])
  function login(value) { saveSession(value); setToken(value); setNotice('') }
  return { token, notice, login, logout }
}
