import { useEffect, useMemo, useState } from 'react'

import { Message, RetryButton, SaveButton } from '../../shared/components/Forms'
import { nutrients } from '../../shared/utils/nutrition'
import { nutritionApi } from './api'

const ACTIVITIES = {
  SEDENTARY: '거의 활동하지 않음 (1.2)',
  LIGHT: '가벼운 활동 (1.375)',
  MODERATE: '보통 활동 (1.55)',
  ACTIVE: '활발한 활동 (1.725)',
  VERY_ACTIVE: '매우 활발한 활동 (1.9)',
}

function ActivityForm({ preference, busy, onSubmit }) {
  return (
    <form key={preference.activityLevel || 'unset'} className="activity-form" onSubmit={onSubmit}>
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
      <SaveButton busy={busy}>활동량 저장</SaveButton>
    </form>
  )
}

/** 권장량 한 항목과, 그날 섭취량이 있으면 대비 비율. */
function TargetItem({ label, unit, target, consumed }) {
  return (
    <div>
      <span>{label}</span>
      <strong>
        {target.toLocaleString()} {unit}
      </strong>
      {consumed ? (
        <>
          <progress aria-label={`${label} 권장량 대비 섭취`} max={target} value={Math.min(consumed.value, target)} />
          <p>
            {Math.round((consumed.value / target) * 100)}% 섭취 · 차이 {(target - consumed.value).toLocaleString('ko-KR', { maximumFractionDigits: 2 })} {unit}
          </p>
          {consumed.missingCount ? <p className="unit-warning">영양정보 없는 {consumed.missingCount}건 제외</p> : null}
        </>
      ) : null}
    </div>
  )
}

export default function NutritionTarget({ call, date, daily }) {
  const api = useMemo(() => nutritionApi(call), [call])
  const [data, setData] = useState(null)
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState(null)
  const [retry, setRetry] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    Promise.all([api.preferences(controller.signal), api.recommendation(date, controller.signal)])
      .then(([preference, target]) => {
        if (!controller.signal.aborted) {
          setData({ preference, target, date })
          setMessage(null)
        }
      })
      .catch(error => {
        if (!controller.signal.aborted) setMessage({ text: error.message, error: true })
      })
    return () => controller.abort()
  }, [api, date, retry])

  async function save(event) {
    event.preventDefault()
    setBusy(true)
    try {
      await api.save(new FormData(event.currentTarget).get('activityLevel'))
      setRetry(value => value + 1)
    } catch (error) {
      setMessage({ text: error.message, error: true })
    } finally {
      setBusy(false)
    }
  }

  const target = data?.date === date ? data.target : null

  return (
    <section className="panel">
      <h2>나의 권장 섭취량</h2>
      <Message message={message} />
      {message?.error ? <RetryButton onRetry={() => setRetry(value => value + 1)} /> : null}
      {data ? <ActivityForm preference={data.preference} busy={busy} onSubmit={save} /> : <p role="status">권장량을 불러오는 중…</p>}
      {target?.available ? (
        <>
          <p className="help">
            {target.measurementDate} 신체 기록 기준 · 안정 시 {target.restingCalories.toLocaleString()} kcal · 유지{' '}
            {target.maintenanceCalories.toLocaleString()} kcal
          </p>
          <div className="target-grid">
            {nutrients.map(([key, label, unit]) => (
              <TargetItem key={key} label={label} unit={unit} target={target[key]} consumed={daily?.date === date ? daily[key] : null} />
            ))}
          </div>
        </>
      ) : target ? (
        <p className="help">{target.reason}</p>
      ) : null}
      <p className="help">
        만 19세 이상 일반 성인의 참고 추정값입니다. Mifflin–St Jeor 공식, 목표별 ±300 kcal, 탄수화물·단백질·지방 50·20·30%를 적용합니다. 과거 날짜도 현재 프로필과
        활동량을 사용합니다.
      </p>
    </section>
  )
}
