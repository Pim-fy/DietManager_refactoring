import { statisticsApi } from '../features/api'
import { useEffect, useMemo, useState } from 'react'
import { Field } from './Forms'
import { dateString, today } from '../form-utils'
function dates(anchor, period) {
  const start = new Date(`${anchor}T12:00:00`)
  const end = new Date(start)
  if (period === 'week') { start.setDate(start.getDate() - (start.getDay() + 6) % 7); end.setTime(start.getTime()); end.setDate(end.getDate() + 6) }
  else { start.setDate(1); end.setMonth(end.getMonth() + 1, 0) }
  return { start: dateString(start), end: dateString(end) }
}
function Chart({ days, type, title, unit }) {
  const values = days.map(day => type === 'weight' ? day.weight : day.records > day.calories.missingCount ? day.calories.value : null)
  const known = values.filter(value => value != null)
  if (!known.length) return <div><h3>{title}</h3><p className="empty">이 기간의 기록이 없습니다.</p></div>
  const min = type === 'weight' ? Math.min(...known) - 1 : 0
  const max = Math.max(...known) + (type === 'weight' ? 1 : 100)
  const points = values.flatMap((value, index) => value == null ? [] : [{ x: 35 + index / Math.max(1, days.length - 1) * 590, y: 170 - (value - min) / Math.max(1, max - min) * 145 }])
  return <div><h3>{title} <span className="count">{unit}</span></h3><svg className="trend-chart" viewBox="0 0 650 200" role="img" aria-label={`${title}, 기록이 있는 날짜만 연결한 그래프`}>
    <line x1="35" y1="170" x2="625" y2="170" stroke="#c9d2dc" /><polyline points={points.map(point => `${point.x},${point.y}`).join(' ')} fill="none" stroke="#167d70" strokeWidth="3" />
    {points.map((point, index) => <circle key={index} cx={point.x} cy={point.y} r="4" fill="#167d70" />)}
    <text x="35" y="195">{days[0].date.slice(5)}</text><text x="570" y="195">{days.at(-1).date.slice(5)}</text>
    <text x="35" y="24">최대 {Math.max(...known).toLocaleString()} {unit}</text>
  </svg></div>
}
export default function TrendsPage({ call }) {
  const api = useMemo(() => statisticsApi(call), [call])
  const [anchor, setAnchor] = useState(today)
  const [period, setPeriod] = useState('week')
  const [data, setData] = useState(null)
  const [error, setError] = useState('')
  const [retry, setRetry] = useState(0)
  const [page, setPage] = useState(0)
  const range = dates(anchor, period)
  useEffect(() => {
    const controller = new AbortController()
    api.range(range.start, range.end, controller.signal)
      .then(value => { if (!controller.signal.aborted) { setData(value); setError('') } })
      .catch(failure => { if (!controller.signal.aborted) setError(failure.message) })
    return () => controller.abort()
  }, [api, range.start, range.end, retry]) // range는 날짜 문자열로 비교한다.
  const current = data?.start === range.start && data?.end === range.end ? data : null
  return <><div className="page-heading"><h1>기록 추이</h1><p>주·월별 영양 섭취와 체중 변화를 확인하세요.</p></div>
    <div className="range-toolbar"><label className="field"><span>기간</span><select value={period} onChange={event => { setPeriod(event.target.value); setPage(0) }}><option value="week">주간</option><option value="month">월간</option></select></label>
      <Field label="기준 날짜" type="date" value={anchor} onChange={event => { if (event.target.value) { setAnchor(event.target.value); setPage(0) } }} /></div>
    {error ? <div className="message error" role="alert">{error}<button className="outline" onClick={() => setRetry(value => value + 1)}>다시 불러오기</button></div> : !current ? <p role="status">기록을 불러오는 중…</p> : <>
      <section className="panel"><h2>{current.start} ~ {current.end}</h2><Chart days={current.days} type="calories" title="칼로리 추이" unit="kcal" /><Chart days={current.days} type="weight" title="체중 추이" unit="kg" />
        <p className="help">기록이 있는 날짜만 연결합니다. 기록이 없는 날은 0으로 추정하지 않습니다.</p></section>
      <section className="panel"><h2>날짜별 기록</h2><div className="table-wrap"><table><thead><tr><th>날짜</th><th>섭취 건수</th><th>칼로리</th><th>탄수화물</th><th>단백질</th><th>지방</th><th>체중</th></tr></thead>
        <tbody>{current.days.slice(page * 14, (page + 1) * 14).map(day => <tr key={day.date}><td>{day.date}</td><td>{day.records}</td>{['calories', 'carbohydrate', 'protein', 'fat'].map(key => <td key={key}>{day.records ? `${day[key].value.toLocaleString()}${day[key].missingCount ? ` (누락 ${day[key].missingCount}건)` : ''}` : '기록 없음'}</td>)}<td>{day.weight ?? '기록 없음'}</td></tr>)}</tbody></table></div>
        <div className="pagination"><button className="outline" disabled={!page} onClick={() => setPage(value => value - 1)}>이전 날짜</button><span>{page + 1} / {Math.ceil(current.days.length / 14)}</span><button className="outline" disabled={(page + 1) * 14 >= current.days.length} onClick={() => setPage(value => value + 1)}>다음 날짜</button></div></section>
    </>}</>
}
