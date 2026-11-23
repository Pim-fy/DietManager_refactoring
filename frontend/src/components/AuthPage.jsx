import { formValues } from '../form-utils'
import { useState } from 'react'
import { request } from '../api'
import { Field, ProfileFields, SaveButton, Message } from './Forms'
export default function AuthPage({ onLogin }) {
  const [register, setRegister] = useState(false)
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState(null)
  async function submit(event) {
    event.preventDefault()
    if (busy) return
    const body = formValues(event)
    setBusy(true); setMessage(null)
    try {
      if (register) {
        const response = await request('/register', { method: 'POST', body })
        setRegister(false); setMessage({ text: `${response.message} 로그인해주세요.` })
      } else {
        const response = await request('/login', { method: 'POST', body })
        onLogin(response.accessToken)
      }
    } catch (error) { setMessage({ text: error.message, error: true }) }
    finally { setBusy(false) }
  }
  function switchMode(value) { setRegister(value); setMessage(null) }
  return <main className="auth-page">
    <div className="auth-intro"><h1>나의 건강 기록을<br />차근차근.</h1><p>계정과 신체 기록을 한곳에서 관리하세요.</p></div>
    <section className="panel auth-panel" aria-label={register ? '회원가입' : '로그인'}>
      <div className="tabs"><button type="button" aria-pressed={!register} disabled={busy} onClick={() => switchMode(false)}>로그인</button>
        <button type="button" aria-pressed={register} disabled={busy} onClick={() => switchMode(true)}>회원가입</button></div>
      <h2>{register ? '회원가입' : '로그인'}</h2>
      <form key={register ? 'register' : 'login'} onSubmit={submit}>
        <fieldset disabled={busy}>
          <Field label="이메일" name="email" type="email" maxLength={255} autoComplete="username" />
          <Field label="비밀번호" name="password" type="password" minLength={register ? 8 : undefined} maxLength={register ? 64 : undefined} autoComplete={register ? 'new-password' : 'current-password'} />
          {register ? <><Field label="이름" name="userName" maxLength={30} autoComplete="name" /><ProfileFields /></> : null}
        </fieldset><Message message={message} /><SaveButton busy={busy}>{register ? '계정 만들기' : '로그인'}</SaveButton>
      </form>
    </section>
  </main>
}

