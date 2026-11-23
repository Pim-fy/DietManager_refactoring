import AuthPage from './components/AuthPage'
import Workspace from './components/Workspace'
import useAuth from './hooks/useAuth'
import './App.css'
export default function App() {
  const { token, notice, login, logout } = useAuth()
  return token ? <Workspace key={token} token={token} onLogout={logout} /> : <>
    <header className="public-header"><span className="brand">DietManager</span></header>
    {notice ? <p className="session-notice" role="status">{notice}</p> : null}
    <AuthPage onLogin={login} />
  </>
}
