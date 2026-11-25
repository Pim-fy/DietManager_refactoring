import './App.css'

import AuthPage from './features/auth/AuthPage'
import useAuth from './features/auth/useAuth'
import Workspace from './features/workspace/Workspace'

export default function App() {
  const { token, notice, login, logout } = useAuth()

  if (token) return <Workspace key={token} token={token} onLogout={logout} />

  return (
    <>
      <header className="app-header">
        <span className="brand">DietManager</span>
      </header>
      {notice ? (
        <p className="session-notice" role="status">
          {notice}
        </p>
      ) : null}
      <AuthPage onLogin={login} />
    </>
  )
}
