import { useEffect, useRef, useState } from 'react'

import { ticks } from './ticks'

const HEIGHT = 240
const LEFT = 52
const RIGHT = 16
const TOP = 16
const BOTTOM = 208
const WEEKDAYS = ['일', '월', '화', '수', '목', '금', '토']

function dateLabel(date, weekly) {
  const day = new Date(`${date}T12:00:00`)
  return weekly ? `${WEEKDAYS[day.getDay()]} ${day.getDate()}` : `${day.getMonth() + 1}.${day.getDate()}`
}

/** 요소의 실제 너비. 그래프를 화면 픽셀 그대로 그려 글자 크기가 화면 폭에 따라 줄지 않게 한다. */
function useWidth() {
  const ref = useRef(null)
  const [width, setWidth] = useState(0)
  useEffect(() => {
    const observer = new ResizeObserver(([entry]) => setWidth(Math.round(entry.contentRect.width)))
    observer.observe(ref.current)
    return () => observer.disconnect()
  }, [])
  return [ref, width]
}

/** 기록이 있는 날짜만 이은 선 그래프. type: calories | weight */
export default function TrendChart({ days, type, title, unit }) {
  const [ref, measured] = useWidth()
  const values = days.map(day => {
    if (type === 'weight') return day.weight
    return day.records > day.calories.missingCount ? day.calories.value : null
  })
  const known = values.filter(value => value != null)
  if (!known.length) {
    return (
      <div className="trend" ref={ref}>
        <h3>{title}</h3>
        <p className="empty">이 기간의 기록이 없습니다.</p>
      </div>
    )
  }

  const scale = type === 'weight' ? ticks(Math.min(...known) - 1, Math.max(...known) + 1) : ticks(0, Math.max(...known, 100))
  const min = scale[0]
  const max = scale.at(-1)
  const WIDTH = measured || 900
  const weekly = days.length <= 7
  const x = index => LEFT + (index / Math.max(1, days.length - 1)) * (WIDTH - LEFT - RIGHT)
  const y = value => BOTTOM - ((value - min) / (max - min || 1)) * (BOTTOM - TOP)
  const points = values.flatMap((value, index) => (value == null ? [] : [{ x: x(index), y: y(value), value, date: days[index].date }]))
  const labelEvery = weekly ? 1 : WIDTH < 500 ? 10 : 7

  return (
    <div className="trend" ref={ref}>
      <h3>
        {title} <span className="count">{unit}</span>
      </h3>
      <svg className="trend-chart" viewBox={`0 0 ${WIDTH} ${HEIGHT}`} role="img" aria-label={`${title}, 기록이 있는 날짜만 연결한 그래프`}>
        {scale.map(value => (
          <g key={value}>
            <line className="grid" x1={LEFT} x2={WIDTH - RIGHT} y1={y(value)} y2={y(value)} />
            <text className="axis" x={LEFT - 8} y={y(value) + 4} textAnchor="end">
              {value.toLocaleString()}
            </text>
          </g>
        ))}
        {days.map((day, index) =>
          index % labelEvery === 0 ? (
            <text key={day.date} className="axis" x={x(index)} y={HEIGHT - 8} textAnchor="middle">
              {dateLabel(day.date, weekly)}
            </text>
          ) : null,
        )}
        <polyline className="line" points={points.map(point => `${point.x},${point.y}`).join(' ')} />
        {points.map(point => (
          <circle key={point.date} className="dot" cx={point.x} cy={point.y} r="4.5">
            <title>
              {point.date} · {point.value.toLocaleString()} {unit}
            </title>
          </circle>
        ))}
      </svg>
    </div>
  )
}
