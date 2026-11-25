import { useCallback, useMemo, useState } from 'react'

import useResource from '../../shared/hooks/useResource'
import { today } from '../../shared/utils/form'
import { intakeApi } from '../intakes/api'
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
 * 한 영역이 실패해도 다른 영역은 그대로 보인다. 계획을 섭취로 옮기면 오늘 섭취도 다시 불러온다.
 */
export default function DashboardPage({ call }) {
  const [date] = useState(today)
  const intakes = useMemo(() => intakeApi(call), [call])
  const loadIntake = useCallback(signal => intakes.daily(date, signal), [intakes, date])
  const intake = useResource(loadIntake)

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
          <TodayPlans call={call} date={date} onConsumed={intake.retry} />
          <WeightChange call={call} date={date} />
        </div>
        <WeekCalories call={call} date={date} />
      </div>
    </>
  )
}
