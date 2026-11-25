import AppLink from '../../shared/components/AppLink'
import { formatNutrition as format, nutrients } from '../../shared/utils/nutrition'
import ExcessWarning from './ExcessWarning'

function percent(value, target) {
  return Math.round((value / target) * 100)
}

function MacroRow({ label, unit, consumed, target }) {
  const value = consumed.value ?? 0
  const over = target != null && value > target
  return (
    <tr className={over ? 'over' : undefined}>
      <th scope="row">{label}</th>
      <td>
        {format(value, 1)} {unit}
      </td>
      <td className="target-cell">{target != null ? `${format(target, 0)} ${unit}` : '–'}</td>
      <td className="percent-cell">{target != null ? `${percent(value, target)}%` : ''}</td>
    </tr>
  )
}

/**
 * 하루 섭취 합계를 식품 영양정보 표시처럼 보여준다.
 * 권장량을 계산할 수 있으면 권장량 대비 비율을, 없으면 이유를 함께 보여준다.
 */
export default function DaySummary({ date, daily, target, targetError, onTargetRetry }) {
  const personal = target?.available ? target : null
  const calories = daily.calories.value ?? 0
  const missing = Math.max(...nutrients.map(([key]) => daily[key].missingCount || 0))
  const [, month, day] = date.split('-')

  return (
    <section className="day-label" aria-label="일별 영양 합계">
      <div className="day-label-head">
        <h2>하루 섭취</h2>
        <span>
          {Number(month)}월 {Number(day)}일
        </span>
      </div>
      <div className="day-label-calories">
        <p className="kcal">
          {format(calories, 0)}
          <small>kcal</small>
        </p>
        {personal ? (
          <>
            <p className="kcal-target">
              권장 {format(personal.calories, 0)} kcal 중 {percent(calories, personal.calories)}%
            </p>
            <div className={`meter${calories > personal.calories ? ' over' : ''}`} aria-hidden="true">
              <span style={{ width: `${Math.min(100, percent(calories, personal.calories))}%` }} />
            </div>
          </>
        ) : null}
      </div>
      <table className="macro-table">
        <thead>
          <tr>
            <th scope="col">영양성분</th>
            <th scope="col">섭취</th>
            <th scope="col">권장</th>
            <th scope="col">
              <span className="visually-hidden">권장 대비</span>
            </th>
          </tr>
        </thead>
        <tbody>
          {nutrients.slice(1).map(([key, label, unit]) => (
            <MacroRow key={key} label={label} unit={unit} consumed={daily[key]} target={personal?.[key]} />
          ))}
        </tbody>
      </table>
      <div className="day-label-foot">
        {!daily.items.length ? (
          <p>아직 기록이 없습니다. 끼니별로 먹은 음식을 추가하세요.</p>
        ) : target && !targetError ? (
          <ExcessWarning daily={daily} target={target} />
        ) : null}
        {missing ? <p>영양정보가 없는 기록 {missing}건은 합계에서 빠져 있어 실제 섭취량은 더 많을 수 있습니다.</p> : null}
        {targetError ? (
          <p>
            권장량을 불러오지 못했습니다.{' '}
            <button type="button" className="text-button" onClick={onTargetRetry}>
              다시 불러오기
            </button>
          </p>
        ) : target && !target.available ? (
          <p>
            {target.reason} 권장량은 <AppLink to="/measurements">신체 기록</AppLink>과 <AppLink to="/account">내 정보</AppLink>의 활동량으로 계산합니다.
          </p>
        ) : null}
      </div>
    </section>
  )
}
