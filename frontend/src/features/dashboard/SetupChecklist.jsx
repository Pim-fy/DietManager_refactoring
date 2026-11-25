import { useMemo } from 'react'

import AppLink from '../../shared/components/AppLink'
import useResource from '../../shared/hooks/useResource'
import { measurementApi } from '../measurements/api'
import { nutritionApi } from '../nutrition/api'

function Step({ number, done, to, title, hint }) {
  return (
    <li className={done ? 'setup-step done' : 'setup-step'}>
      <span className="setup-mark" aria-hidden="true">{done ? '✓' : number}</span>
      <span className="setup-text">
        {done ? <strong>{title}</strong> : <AppLink to={to}>{title}</AppLink>}
        <span>{done ? '완료했습니다.' : hint}</span>
      </span>
    </li>
  )
}

/**
 * 권장 섭취량 계산에 필요한 신체 기록과 활동량 안내. 둘 다 끝났거나 아직 불러오는 중·조회 실패면 아무것도 그리지 않는다.
 */
export default function SetupChecklist({ call }) {
  const measurements = useMemo(() => measurementApi(call), [call])
  const nutrition = useMemo(() => nutritionApi(call), [call])
  const body = useResource(measurements.list)
  const preferences = useResource(nutrition.preferences)

  if (!body.data || !preferences.data || body.error || preferences.error) return null
  const hasBody = body.data.length > 0
  const hasActivity = Boolean(preferences.data.activityLevel)
  if (hasBody && hasActivity) return null

  return (
    <section className="setup" aria-labelledby="setup-title">
      <h2 id="setup-title">권장 섭취량을 계산하려면 두 가지가 필요합니다</h2>
      <ol className="setup-list">
        <Step number={1} done={hasBody} to="/measurements" title="신체 기록 입력하기" hint="키와 몸무게를 입력하면 권장 섭취량을 계산합니다" />
        <Step number={2} done={hasActivity} to="/account" title="활동량 설정하기" hint="내 정보에서 평소 활동량을 고르세요" />
      </ol>
    </section>
  )
}
