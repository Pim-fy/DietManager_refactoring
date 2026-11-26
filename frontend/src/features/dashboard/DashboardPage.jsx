import { useCallback, useEffect, useMemo, useState } from 'react'

import useResource from '../../shared/hooks/useResource'
import { today } from '../../shared/utils/form'
import { intakeApi } from '../intakes/api'
import { statisticsApi } from '../trends/api'
import { dates } from '../trends/range'
import SetupChecklist from './SetupChecklist'
import TodayIntake from './TodayIntake'
import TodayPlans from './TodayPlans'
import WeekCalories from './WeekCalories'
import WeightChange from './WeightChange'

function longDate(date) {
  return new Date(`${date}T12:00:00`).toLocaleDateString('ko-KR', { month: 'long', day: 'numeric', weekday: 'long' })
}

/**
 * 로그인 후 첫 화면. 오늘 섭취·오늘 계획·몸무게 변화·이번 주 칼로리를 영역마다 따로 불러온다.
 * 한 영역이 실패해도 다른 영역은 그대로 보인다. 계획을 섭취로 옮기면 오늘 섭취와 이번 주 칼로리도 다시 불러온다.
 */
export default function DashboardPage({ call }) {
  const [date, setDate] = useState(today)
  useEffect(() => {
    // 자정이 지난 뒤 창이 다시 보이거나 포커스되면 오늘 날짜로 바꾼다.
    const refreshDate = () => {
      if (document.visibilityState === 'visible') setDate(today())
    }
    document.addEventListener('visibilitychange', refreshDate)
    window.addEventListener('focus', refreshDate)
    return () => {
      document.removeEventListener('visibilitychange', refreshDate)
      window.removeEventListener('focus', refreshDate)
    }
  }, [])
  const intakes = useMemo(() => intakeApi(call), [call])
  const loadIntake = useCallback(signal => intakes.daily(date, signal), [intakes, date])
  const intake = useResource(loadIntake)
  const statistics = useMemo(() => statisticsApi(call), [call])
  const { start, end } = dates(date, 'week')
  const loadWeek = useCallback(signal => statistics.range(start, end, signal), [statistics, start, end])
  const week = useResource(loadWeek)
  const refreshAfterConsume = () => {
    intake.retry()
    week.retry()
  }

  return (
    <>
      <div className="page-heading heading-inline">
        <h1>대시보드</h1>
        <p>{longDate(date)}</p>
      </div>
      <SetupChecklist call={call} />
      <div className="dashboard">
        <TodayIntake call={call} date={date} resource={intake} />
        <div className="dash-side">
          <TodayPlans call={call} date={date} onConsumed={refreshAfterConsume} />
          <WeightChange call={call} date={date} />
        </div>
        <WeekCalories resource={week} />
      </div>
    </>
  )
}
