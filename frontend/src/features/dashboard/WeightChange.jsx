import { useMemo } from 'react'

import AppLink from '../../shared/components/AppLink'
import ResourceState from '../../shared/components/ResourceState'
import useResource from '../../shared/hooks/useResource'
import { formatNutrition as format } from '../../shared/utils/nutrition'
import { measurementApi } from '../measurements/api'

const DAY = 86400000

function daysBetween(from, to) {
  return Math.round((Date.parse(`${to}T00:00:00Z`) - Date.parse(`${from}T00:00:00Z`)) / DAY)
}

function shortDate(date) {
  const [, month, day] = date.split('-')
  return `${Number(month)}월 ${Number(day)}일`
}

function signed(value) {
  const rounded = Math.round(value * 10) / 10
  if (rounded > 0) return `+${rounded.toFixed(1)} kg`
  if (rounded < 0) return `−${Math.abs(rounded).toFixed(1)} kg`
  return '변화 없음'
}

function Summary({ measurements, date }) {
  if (!measurements.length) {
    return <p className="dash-empty">아직 신체 기록이 없습니다. 몸무게를 기록하면 여기서 변화를 볼 수 있습니다.</p>
  }
  // 목록은 측정일 최신순이다. 비교 대상은 최근 기록보다 7일 이상 앞선 기록 중 가장 최근 것.
  const [latest] = measurements
  const base = measurements.find(item => daysBetween(item.measuredDate, latest.measuredDate) >= 7)
  const stale = daysBetween(latest.measuredDate, date) > 14

  return (
    <>
      <p className="dash-figure">
        {format(latest.weight, 1)}
        <small>kg</small>
      </p>
      <p className="dash-note">
        {shortDate(latest.measuredDate)} 측정
        {base ? (
          <>
            {' · '}
            {shortDate(base.measuredDate)}보다 <strong>{signed(latest.weight - base.weight)}</strong>
          </>
        ) : null}
      </p>
      {stale ? <p className="dash-note">마지막 측정 후 2주가 지났습니다. 오늘 몸무게를 기록해 보세요.</p> : null}
    </>
  )
}

/** 최근 몸무게와 1주 전쯤 기록과의 차이. */
export default function WeightChange({ call, date }) {
  const api = useMemo(() => measurementApi(call), [call])
  const resource = useResource(api.list)

  return (
    <section className="dash-section" aria-labelledby="dash-weight-title">
      <div className="dash-head">
        <h2 id="dash-weight-title">몸무게 변화</h2>
        <AppLink to="/measurements">신체 기록으로</AppLink>
      </div>
      <ResourceState resource={resource} loadingText="신체 기록을 불러오는 중…">
        {resource.data ? <Summary measurements={resource.data} date={date} /> : null}
      </ResourceState>
    </section>
  )
}
