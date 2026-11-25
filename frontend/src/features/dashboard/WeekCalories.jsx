import AppLink from '../../shared/components/AppLink'
import EmptyState from '../../shared/components/EmptyState'
import ResourceState from '../../shared/components/ResourceState'
import SectionHeader from '../../shared/components/SectionHeader'
import StatList from '../../shared/components/StatList'
import { formatNutrition as format } from '../../shared/utils/nutrition'
import TrendChart from '../trends/TrendChart'
import { summarizeWeek } from './summarizeWeek'

function Summary({ days }) {
  const { recorded, withCalories, average } = summarizeWeek(days)
  if (!recorded.length) return <EmptyState size="compact">이번 주에는 아직 섭취 기록이 없습니다.</EmptyState>

  return (
    <>
      <StatList
        items={[
          { label: '기록한 날', value: recorded.length, unit: `/ ${days.length}일` },
          withCalories.length
            ? { label: '기록한 날 평균', value: format(Math.round(average), 0), unit: 'kcal' }
            : { label: '기록한 날 평균', note: '칼로리 정보가 있는 기록이 없습니다' },
        ]}
      />
      <TrendChart days={days} type="calories" title="하루 칼로리" unit="kcal" />
    </>
  )
}

/** 이번 주(월~일) 하루 칼로리 추이. resource는 대시보드에서 불러온 이번 주 통계다. */
export default function WeekCalories({ resource }) {
  return (
    <section className="dash-week" aria-labelledby="dash-week-title">
      <SectionHeader title="이번 주 칼로리 추이" titleId="dash-week-title">
        <AppLink to="/trends">기록 추이로</AppLink>
      </SectionHeader>
      <ResourceState resource={resource} loadingText="이번 주 기록을 불러오는 중…">
        {resource.data ? <Summary days={resource.data.days} /> : null}
      </ResourceState>
    </section>
  )
}
