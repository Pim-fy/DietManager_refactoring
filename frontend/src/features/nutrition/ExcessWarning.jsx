import { findExcess } from './excess'

/** 화면의 다른 영양값과 같이 kcal은 정수, g은 소수 한 자리, 비율(%)은 소수 한 자리로 보여준다. */
function format(value, unit) {
  return value.toLocaleString('ko-KR', { maximumFractionDigits: unit === 'kcal' ? 0 : 1 })
}

/** 하루 섭취 합계가 권장량(또는 1일 기준치)을 넘은 영양소를 알려준다. */
export default function ExcessWarning({ daily, target }) {
  const { basis, items } = findExcess(daily, target)

  return (
    <div className="excess-warning" aria-label="섭취량 경고">
      {items.length ? (
        <ul role="alert">
          {items.map(item => (
            <li key={item.key}>
              {item.label}: {basis}({format(item.limit, item.unit)}
              {item.unit}) 대비 {format(item.over, item.unit)}
              {item.unit} 초과 ({format(item.percent)}%)
            </li>
          ))}
        </ul>
      ) : (
        <p role="status">{basis} 이내로 섭취했습니다.</p>
      )}
      {!target?.available ? <p>권장량이 없어 1일 영양성분 기준치(탄수화물·단백질·지방)로 비교합니다.</p> : null}
    </div>
  )
}
