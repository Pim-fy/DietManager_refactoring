import { dateString } from '../form-utils'
export function Field({ label, name, type = 'text', ...props }) {
  return <label className="field"><span>{label}</span><input name={name} type={type} required {...props} /></label>
}
export function ProfileFields({ profile = {} }) {
  const date = new Date(); date.setDate(date.getDate() - 1)
  return <>
    <label className="field"><span>성별</span><select name="gender" defaultValue={profile.gender || ''} required>
      <option value="" disabled>선택하세요</option><option value="MALE">남성</option><option value="FEMALE">여성</option>
    </select></label>
    <Field label="생년월일" name="birthDate" type="date" max={dateString(date)} defaultValue={profile.birthDate || ''} />
    <label className="field"><span>목표</span><select name="goalType" defaultValue={profile.goalType || ''} required>
      <option value="" disabled>선택하세요</option><option value="LOSE_WEIGHT">체중 감량</option>
      <option value="MAINTAIN_WEIGHT">체중 유지</option><option value="GAIN_WEIGHT">체중 증가</option>
    </select></label>
  </>
}
export function SaveButton({ busy, children = '변경사항 저장' }) {
  return <button className="primary" disabled={busy} type="submit">{busy ? '처리 중…' : children}</button>
}
export function Message({ message }) {
  return message ? <p className={`message ${message.error ? 'error' : 'success'}`} role={message.error ? 'alert' : 'status'}>{message.text}</p> : null
}

