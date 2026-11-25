import { formatNutrition as format } from '../utils/nutrition'
import './RecordItem.css'

/**
 * 기록 한 줄(li). 왼쪽에 이름(title)과 부가 정보(children), 오른쪽에 칼로리(kcal)와 동작 버튼(actions)을 둔다.
 * kcal을 넘기지 않으면 칼로리 칸을 그리지 않고, null이면 '칼로리 정보 없음'을 보여준다. compact는 대시보드처럼 한 줄로 짧게 보여준다.
 */
export default function RecordItem({ title, children, kcal, actions, deleted = false, compact = false }) {
  const className = ['record-item', compact && 'compact', deleted && 'is-deleted'].filter(Boolean).join(' ')
  return (
    <li className={className}>
      <div className="item-main">
        <strong>{title}</strong>
        {children}
      </div>
      <div className="item-side">
        {kcal !== undefined ? (
          <p className="item-kcal">
            {kcal == null ? (
              <small>칼로리 정보 없음</small>
            ) : (
              <>
                {format(kcal, 0)}
                <small> kcal</small>
              </>
            )}
          </p>
        ) : null}
        <div className="row-actions">{actions}</div>
      </div>
    </li>
  )
}
