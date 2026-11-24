import { useState } from 'react'

import { Field, Message, ProfileFields, SaveButton } from '../../shared/components/Forms'
import { formValues } from '../../shared/utils/form'

/** 섹션 하나를 저장하는 폼. section: account | profile | password */
function EditSection({ title, section, api, onSaved, children, password = false }) {
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState(null)

  async function submit(event) {
    event.preventDefault()
    if (busy) return
    const form = event.currentTarget
    const body = formValues(event)
    if (password && body.newPassword !== body.confirmPassword) {
      setMessage({ text: '새 비밀번호가 일치하지 않습니다.', error: true })
      return
    }
    delete body.confirmPassword
    setBusy(true)
    setMessage(null)
    try {
      const result = await api.update(section, body)
      onSaved?.(result)
      if (password) form.reset()
      setMessage({ text: result.message || '변경사항을 저장했습니다.' })
    } catch (error) {
      setMessage({ text: error.message, error: true })
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="panel">
      <h2>{title}</h2>
      <form onSubmit={submit}>
        <fieldset disabled={busy}>{children}</fieldset>
        <Message message={message} />
        <SaveButton busy={busy} />
      </form>
    </section>
  )
}

export default function AccountPage({ account, profile, api, onAccount, onProfile, onPassword }) {
  return (
    <>
      <div className="page-heading">
        <h1>내 정보</h1>
        <p>계정 정보와 프로필을 관리하세요.</p>
      </div>
      <div className="account-layout">
        <EditSection title="계정 정보" section="account" api={api} onSaved={onAccount}>
          <Field label="이메일" name="email" type="email" maxLength={255} defaultValue={account.email} autoComplete="username" />
          <Field label="이름" name="userName" maxLength={30} defaultValue={account.userName} autoComplete="name" />
        </EditSection>
        <EditSection title="프로필 정보" section="profile" api={api} onSaved={onProfile}>
          <ProfileFields profile={profile} />
        </EditSection>
        <EditSection title="비밀번호 변경" section="password" api={api} onSaved={onPassword} password>
          <Field label="현재 비밀번호" name="currentPassword" type="password" autoComplete="current-password" />
          <Field label="새 비밀번호" name="newPassword" type="password" minLength={8} maxLength={64} autoComplete="new-password" />
          <Field label="새 비밀번호 확인" name="confirmPassword" type="password" minLength={8} maxLength={64} autoComplete="new-password" />
        </EditSection>
      </div>
    </>
  )
}
