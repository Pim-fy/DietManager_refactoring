/** 기록이 있는 날짜만 이은 선 그래프. type: calories | weight */
export default function TrendChart({ days, type, title, unit }) {
  const values = days.map(day => {
    if (type === 'weight') return day.weight
    return day.records > day.calories.missingCount ? day.calories.value : null
  })
  const known = values.filter(value => value != null)
  if (!known.length) {
    return (
      <div>
        <h3>{title}</h3>
        <p className="empty">이 기간의 기록이 없습니다.</p>
      </div>
    )
  }

  const min = type === 'weight' ? Math.min(...known) - 1 : 0
  const max = Math.max(...known) + (type === 'weight' ? 1 : 100)
  const points = values.flatMap((value, index) =>
    value == null
      ? []
      : [
          {
            x: 35 + (index / Math.max(1, days.length - 1)) * 590,
            y: 170 - ((value - min) / Math.max(1, max - min)) * 145,
          },
        ],
  )

  return (
    <div>
      <h3>
        {title} <span className="count">{unit}</span>
      </h3>
      <svg className="trend-chart" viewBox="0 0 650 200" role="img" aria-label={`${title}, 기록이 있는 날짜만 연결한 그래프`}>
        <line x1="35" y1="170" x2="625" y2="170" stroke="#c9d2dc" />
        <polyline points={points.map(point => `${point.x},${point.y}`).join(' ')} fill="none" stroke="#167d70" strokeWidth="3" />
        {points.map((point, index) => (
          <circle key={index} cx={point.x} cy={point.y} r="4" fill="#167d70" />
        ))}
        <text x="35" y="195">
          {days[0].date.slice(5)}
        </text>
        <text x="570" y="195">
          {days.at(-1).date.slice(5)}
        </text>
        <text x="35" y="24">
          최대 {Math.max(...known).toLocaleString()} {unit}
        </text>
      </svg>
    </div>
  )
}
