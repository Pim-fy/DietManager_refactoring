import { today } from '../utils/form'
import { shift } from './shiftDate'

/** 하루씩 이동하는 날짜 선택. future가 false면 오늘 이후로 갈 수 없다. */
export default function DateStepper({ label, value, onChange, disabled, future = false }) {
  const max = future ? undefined : today()
  return (
    <div className="date-stepper">
      <button type="button" className="step" disabled={disabled} aria-label="전날" onClick={() => onChange(shift(value, -1))}>
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
        aria-label="다음 날"
        onClick={() => onChange(shift(value, 1))}
      >
        ›
      </button>
    </div>
  )
}
