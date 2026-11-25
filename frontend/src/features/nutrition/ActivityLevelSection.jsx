import { useEffect, useMemo, useState } from 'react'

import { Message, RetryButton, SaveButton } from '../../shared/components/Forms'
import { nutritionApi } from './api'

const ACTIVITIES = {
  SEDENTARY: '거의 활동하지 않음 (1.2)',
  LIGHT: '가벼운 활동 (1.375)',
  MODERATE: '보통 활동 (1.55)',
  ACTIVE: '활발한 활동 (1.725)',
  VERY_ACTIVE: '매우 활발한 활동 (1.9)',
}

/** 권장 섭취량 계산에 쓰는 활동량 설정. */
export default function ActivityLevelSection({ call }) {
  const api = useMemo(() => nutritionApi(call), [call])
  const [preference, setPreference] = useState(null)
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState(null)
  const [retry, setRetry] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    api
      .preferences(controller.signal)
      .then(value => {
        if (!controller.signal.aborted) {
          setPreference(value)
          setMessage(null)
        }
      })
      .catch(error => {
        if (!controller.signal.aborted) setMessage({ text: error.message, error: true })
      })
    return () => controller.abort()
  }, [api, retry])

  async function save(event) {
    event.preventDefault()
    const activityLevel = new FormData(event.currentTarget).get('activityLevel')
    setBusy(true)
    setMessage(null)
    try {
      await api.save(activityLevel)
      setPreference(previous => ({ ...previous, activityLevel }))
      setMessage({ text: '활동량을 저장했습니다.' })
    } catch (error) {
      setMessage({ text: error.message, error: true })
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="panel">
      <h2>활동량</h2>
      <p className="help">하루 권장 섭취량을 계산할 때 씁니다. 지난 날짜는 그날 적용 중이던 활동량으로 계산합니다.</p>
      {!preference ? (
        message?.error ? (
          <>
            <Message message={message} />
            <RetryButton onRetry={() => setRetry(value => value + 1)} />
          </>
        ) : (
          <p className="loading" role="status">
            활동량을 불러오는 중…
          </p>
        )
      ) : (
        <form key={preference.activityLevel || 'unset'} onSubmit={save}>
          <label className="field">
            <span>활동량</span>
            <select name="activityLevel" required defaultValue={preference.activityLevel || ''} disabled={busy}>
              <option value="" disabled>
                활동량을 선택하세요
              </option>
              {Object.entries(ACTIVITIES).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </label>
          <Message message={message} />
          <SaveButton busy={busy}>활동량 저장</SaveButton>
        </form>
      )}
    </section>
  )
}
