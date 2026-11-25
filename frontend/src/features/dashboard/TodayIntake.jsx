import AppLink from '../../shared/components/AppLink'
import ResourceState from '../../shared/components/ResourceState'
import { formatNutrition as format, meals } from '../../shared/utils/nutrition'
import DaySummary from '../nutrition/DaySummary'
import useRecommendation from '../nutrition/useRecommendation'

function mealCalories(items, meal) {
  const eaten = items.filter(item => item.mealType === meal)
  if (!eaten.length) return '–'
  return format(
    eaten.reduce((sum, item) => sum + (item.calories ?? 0), 0),
    0,
  )
}

/** 오늘 섭취 합계(섭취 기록 화면과 같은 표시)와 끼니별 칼로리. */
export default function TodayIntake({ call, date, resource }) {
  const recommendation = useRecommendation(call, date)
  const daily = resource.data

  if (!daily || resource.error) {
    return (
      <section className="dash-intake" aria-labelledby="dash-intake-title">
        <div className="dash-head">
          <h2 id="dash-intake-title">하루 섭취</h2>
        </div>
        <ResourceState resource={resource} loadingText="오늘 섭취 기록을 불러오는 중…" />
      </section>
    )
  }

  return (
    <div className="dash-intake">
      <DaySummary
        date={date}
        daily={daily}
        target={recommendation.target}
        targetError={recommendation.error}
        onTargetRetry={recommendation.retry}
      />
      <dl className="dash-meals" aria-label="끼니별 칼로리(kcal)">
        {Object.entries(meals).map(([meal, label]) => (
          <div key={meal}>
            <dt>{label}</dt>
            <dd>{mealCalories(daily.items, meal)}</dd>
          </div>
        ))}
      </dl>
      <AppLink className="dash-link" to="/intakes">
        섭취 기록으로
      </AppLink>
    </div>
  )
}
