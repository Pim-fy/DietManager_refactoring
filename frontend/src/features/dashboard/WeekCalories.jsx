import { useCallback, useMemo } from 'react'

import AppLink from '../../shared/components/AppLink'
import ResourceState from '../../shared/components/ResourceState'
import useResource from '../../shared/hooks/useResource'
import { formatNutrition as format } from '../../shared/utils/nutrition'
import { statisticsApi } from '../trends/api'
import { dates } from '../trends/range'
import TrendChart from '../trends/TrendChart'

function Summary({ days }) {
  const recorded = days.filter(day => day.records > 0)
  if (!recorded.length) return <p className="dash-empty">이번 주에는 아직 섭취 기록이 없습니다.</p>
  const withCalories = recorded.filter(day => day.records > day.calories.missingCount)
  const average = withCalories.reduce((sum, day) => sum + day.calories.value, 0) / withCalories.length

  return (
    <>
      <dl className="latest">
        <div>
          <dt>기록한 날</dt>
          <dd>
            {recorded.length}
            <small> / {days.length}일</small>
          </dd>
        </div>
        <div>
          <dt>기록한 날 평균</dt>
          <dd>
            {withCalories.length ? (
              <>
                {format(Math.round(average), 0)}
                <small> kcal</small>
              </>
            ) : (
              <small>칼로리 정보가 있는 기록이 없습니다</small>
            )}
          </dd>
        </div>
      </dl>
      <TrendChart days={days} type="calories" title="하루 칼로리" unit="kcal" />
    </>
  )
}

/** 이번 주(월~일) 하루 칼로리 추이. */
export default function WeekCalories({ call, date }) {
  const api = useMemo(() => statisticsApi(call), [call])
  const { start, end } = dates(date, 'week')
  const load = useCallback(signal => api.range(start, end, signal), [api, start, end])
  const resource = useResource(load)

  return (
    <section className="dash-week" aria-labelledby="dash-week-title">
      <div className="dash-head">
        <h2 id="dash-week-title">이번 주 칼로리 추이</h2>
        <AppLink to="/trends">기록 추이로</AppLink>
      </div>
      <ResourceState resource={resource} loadingText="이번 주 기록을 불러오는 중…">
        {resource.data ? <Summary days={resource.data.days} /> : null}
      </ResourceState>
    </section>
  )
}
