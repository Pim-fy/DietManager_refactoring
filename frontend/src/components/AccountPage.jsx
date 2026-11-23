import { userApi } from '../features/api'
import { formValues } from '../form-utils'
import { useMemo, useState } from 'react'
import { Field, ProfileFields, SaveButton, Message } from './Forms'
function EditSection({ title, path, call, onSaved, children, password = false }) {
  const api = useMemo(() => userApi(call), [call])
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState(null)
  async function submit(event) {
    event.preventDefault()
    if (busy) return
    const form = event.currentTarget
    const body = formValues(event)
    if (password && body.newPassword !== body.confirmPassword) {
      setMessage({ text: '새 비밀번호가 일치하지 않습니다.', error: true }); return
    }
    delete body.confirmPassword
    setBusy(true); setMessage(null)
    try {
      const result = await api.update(path.slice(1), body)
      onSaved?.(result)
      if (password) form.reset()
      setMessage({ text: result.message || '변경사항을 저장했습니다.' })
    } catch (error) { setMessage({ text: error.message, error: true }) }
    finally { setBusy(false) }
  }
  return <section className="panel"><h2>{title}</h2><form onSubmit={submit}>
    <fieldset disabled={busy}>{children}</fieldset><Message message={message} /><SaveButton busy={busy} />
  </form></section>
}
export default function AccountPage({ account, profile, call, onAccount, onProfile, onPassword }) {
  return <><div className="page-heading"><h1>내 정보</h1><p>계정 정보와 프로필을 관리하세요.</p></div>
    <div className="account-layout">
      <EditSection title="계정 정보" path="/account" call={call} onSaved={onAccount}>
        <Field label="이메일" name="email" type="email" maxLength={255} defaultValue={account.email} autoComplete="username" />
        <Field label="이름" name="userName" maxLength={30} defaultValue={account.userName} autoComplete="name" />
      </EditSection>
      <EditSection title="프로필 정보" path="/profile" call={call} onSaved={onProfile}><ProfileFields profile={profile} /></EditSection>
      <EditSection title="비밀번호 변경" path="/password" call={call} onSaved={onPassword} password>
        <Field label="현재 비밀번호" name="currentPassword" type="password" autoComplete="current-password" />
        <Field label="새 비밀번호" name="newPassword" type="password" minLength={8} maxLength={64} autoComplete="new-password" />
        <Field label="새 비밀번호 확인" name="confirmPassword" type="password" minLength={8} maxLength={64} autoComplete="new-password" />
      </EditSection>
    </div>
  </>
}

