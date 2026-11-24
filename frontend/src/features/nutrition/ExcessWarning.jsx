import { findExcess } from './excess'

function format(value) {
  return value.toLocaleString('ko-KR', { maximumFractionDigits: 1 })
}

/** 하루 섭취 합계가 권장량(또는 1일 기준치)을 넘은 영양소를 알려준다. */
export default function ExcessWarning({ daily, target }) {
  const { basis, items } = findExcess(daily, target)
  const missing = Math.max(...['calories', 'carbohydrate', 'protein', 'fat'].map(key => daily[key]?.missingCount || 0))

  return (
    <div className="excess-warning" aria-label="섭취량 경고">
      <h3>섭취량 경고</h3>
      {items.length ? (
        <ul className="message error" role="alert">
          {items.map(item => (
            <li key={item.key}>
              {item.label}: {basis}({format(item.limit)}
              {item.unit}) 대비 {format(item.over)}
              {item.unit} 초과 ({format(item.percent)}%)
            </li>
          ))}
        </ul>
      ) : (
        <p className="message success" role="status">
          {basis} 이내로 섭취했습니다.
        </p>
      )}
      {!target?.available ? <p className="help">권장량을 계산할 수 없어 1일 영양성분 기준치(탄수화물·단백질·지방)로 비교합니다.</p> : null}
      {missing ? <p className="unit-warning">영양정보가 없는 기록은 합계에서 빠져 있어 실제 섭취량은 더 많을 수 있습니다.</p> : null}
    </div>
  )
}
