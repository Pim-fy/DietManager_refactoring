import { useEffect, useRef, useState } from 'react'

import { calorieTicks, recommendedValues, stepSegments } from './recommendationLine'
import { ticks } from './ticks'

const HEIGHT = 240
const MIN_LEFT = 52
const AXIS_CHAR_WIDTH = 7.5
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

/** 기록이 있는 날짜만 이은 선 그래프. type: calories | weight. 칼로리는 날짜별 권장량을 계단형 점선으로 함께 그린다. */
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

  // 권장량 필드가 없는(구버전) 응답이면 모두 null이라 기준선을 그리지 않는다.
  const recommended = type === 'calories' ? recommendedValues(days) : days.map(() => null)
  const hasRecommended = recommended.some(value => value != null)
  const scale = type === 'weight' ? ticks(Math.min(...known) - 1, Math.max(...known) + 1) : calorieTicks(known, recommended)
  // 세로축 숫자가 길어져도(예: 150,000) 잘리지 않게 가장 긴 눈금 글자에 맞춰 왼쪽 여백을 넓힌다.
  const LEFT = Math.max(MIN_LEFT, Math.ceil(Math.max(...scale.map(value => value.toLocaleString().length)) * AXIS_CHAR_WIDTH) + 14)
  const min = scale[0]
  const max = scale.at(-1)
  const WIDTH = measured || 900
  const weekly = days.length <= 7
  const x = index => LEFT + (index / Math.max(1, days.length - 1)) * (WIDTH - LEFT - RIGHT)
  const y = value => BOTTOM - ((value - min) / (max - min || 1)) * (BOTTOM - TOP)
  const points = values.flatMap((value, index) =>
    value == null ? [] : [{ x: x(index), y: y(value), value, date: days[index].date, recommended: recommended[index] }],
  )
  // 기준선은 날짜 사이 경계(반 칸 위치)에서 꺾이므로 x에 소수 위치를 넘긴다.
  const targetLines = stepSegments(recommended).map(segment => segment.map(point => `${x(point.position)},${y(point.value)}`).join(' '))
  const labelEvery = weekly ? 1 : WIDTH < 500 ? 10 : 7

  return (
    <div className="trend" ref={ref}>
      <h3>
        {title} <span className="count">{unit}</span>
        {hasRecommended ? (
          <span className="trend-legend">
            <span className="legend-intake">섭취</span>
            <span className="legend-target">권장량</span>
          </span>
        ) : null}
      </h3>
      <svg className="trend-chart" viewBox={`0 0 ${WIDTH} ${HEIGHT}`} role="img" aria-label={`${title}, 기록이 있는 날짜만 연결한 그래프${hasRecommended ? ', 점선은 날짜별 권장 칼로리' : ''}`}>
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
        {targetLines.map(line => (
          <polyline key={line} className="target-line" points={line}>
            <title>권장 칼로리</title>
          </polyline>
        ))}
        <polyline className="line" points={points.map(point => `${point.x},${point.y}`).join(' ')} />
        {points.map(point => (
          <circle key={point.date} className="dot" cx={point.x} cy={point.y} r="4.5">
            <title>
              {point.date} · {point.value.toLocaleString()} {unit}
              {point.recommended != null ? ` · 권장 ${Math.round(point.recommended).toLocaleString()} ${unit}` : ''}
            </title>
          </circle>
        ))}
      </svg>
    </div>
  )
}
