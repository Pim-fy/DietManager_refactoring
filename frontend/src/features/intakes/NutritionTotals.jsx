import { formatNutrition as format, nutrients } from '../../shared/utils/nutrition'

/** 하루 영양 합계. 영양값이 없는 기록은 합계에서 빠지고 건수를 따로 보여준다. */
export default function NutritionTotals({ daily }) {
  return (
    <div className="nutrition-totals" aria-label="일별 영양 합계">
      {nutrients.map(([key, label, unit]) => (
        <div key={key}>
          <span>{label}</span>
          <strong>
            {format(daily[key].value)} <small>{unit}</small>
          </strong>
          {daily[key].missingCount > 0 ? <p className="unit-warning">정보 없는 {daily[key].missingCount}건 제외한 합계</p> : null}
        </div>
      ))}
    </div>
  )
}
