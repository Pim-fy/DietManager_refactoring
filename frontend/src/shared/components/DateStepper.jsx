import { today } from '../utils/form'
import { shift, shiftMonth } from './shiftDate'
import './DateStepper.css'

// 이동 단위별 이동 함수와 버튼 이름
const UNITS = {
  day: { move: (value, sign) => shift(value, sign), back: '전날', next: '다음 날' },
  week: { move: (value, sign) => shift(value, sign * 7), back: '이전 주', next: '다음 주' },
  month: { move: (value, sign) => shiftMonth(value, sign), back: '이전 달', next: '다음 달' },
}

/** 날짜 선택. unit(day·week·month) 단위로 이동한다. future가 false면 오늘 이후로 갈 수 없다. */
export default function DateStepper({ label, value, onChange, disabled, future = false, unit = 'day' }) {
  const max = future ? undefined : today()
  const { move, back, next } = UNITS[unit]
  const nextValue = move(value, 1)
  return (
    <div className="date-stepper">
      <button type="button" className="step" disabled={disabled} aria-label={back} onClick={() => onChange(move(value, -1))}>
        ‹
      </button>
      <label>
        <span className="visually-hidden">{label}</span>
        <input type="date" value={value} max={max} disabled={disabled} onChange={event => onChange(event.target.value)} />
      </label>
      <button
        type="button"
        className="step"
        disabled={disabled || (max && value >= max)}
        aria-label={next}
        onClick={() => onChange(nextValue)}
      >
        ›
      </button>
    </div>
  )
}
