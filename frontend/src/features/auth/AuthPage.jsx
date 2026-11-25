import { useState } from 'react'

import { Field, Message, ProfileFields, SaveButton } from '../../shared/components/Forms'
import { formValues } from '../../shared/utils/form'
import { authApi } from './api'
import './auth.css'

export default function AuthPage({ onLogin }) {
  const [register, setRegister] = useState(false)
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState(null)

  async function submit(event) {
    event.preventDefault()
    if (busy) return
    const body = formValues(event)
    setBusy(true)
    setMessage(null)
    try {
      if (register) {
        const response = await authApi.register(body)
        setRegister(false)
        setMessage({ text: `${response.message} 로그인해주세요.` })
      } else {
        const response = await authApi.login(body)
        onLogin(response.accessToken)
      }
    } catch (error) {
      setMessage({ text: error.message, error: true })
    } finally {
      setBusy(false)
    }
  }

  function switchMode(value) {
    setRegister(value)
    setMessage(null)
  }

  return (
    <main className="auth-page">
      <div className="auth-intro">
        <h1>
          오늘 먹은 것을
          <br />
          영양정보로 확인하세요.
        </h1>
        <p>끼니별로 기록하면 칼로리와 탄수화물·단백질·지방을 권장량과 비교해 보여줍니다.</p>
      </div>
      <section className="panel auth-panel" aria-label={register ? '회원가입' : '로그인'}>
        <div className="tabs">
          <button type="button" aria-pressed={!register} disabled={busy} onClick={() => switchMode(false)}>
            로그인
          </button>
          <button type="button" aria-pressed={register} disabled={busy} onClick={() => switchMode(true)}>
            회원가입
          </button>
        </div>
        <h2>{register ? '회원가입' : '로그인'}</h2>
        <form key={register ? 'register' : 'login'} onSubmit={submit}>
          <fieldset disabled={busy}>
            <Field label="이메일" name="email" type="email" maxLength={255} autoComplete="username" />
            <Field
              label="비밀번호"
              name="password"
              type="password"
              minLength={register ? 8 : undefined}
              maxLength={register ? 64 : undefined}
              autoComplete={register ? 'new-password' : 'current-password'}
            />
            {register ? (
              <>
                <Field label="이름" name="userName" maxLength={30} autoComplete="name" />
                <ProfileFields />
              </>
            ) : null}
          </fieldset>
          <Message message={message} />
          <SaveButton busy={busy}>{register ? '계정 만들기' : '로그인'}</SaveButton>
        </form>
      </section>
    </main>
  )
}
