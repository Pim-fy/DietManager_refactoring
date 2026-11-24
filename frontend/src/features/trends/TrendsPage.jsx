import { useEffect, useMemo, useState } from 'react'

import { Field } from '../../shared/components/Forms'
import { dateString, today } from '../../shared/utils/form'
import { statisticsApi } from './api'
import TrendChart from './TrendChart'

const PAGE_SIZE = 14
const NUTRIENT_KEYS = ['calories', 'carbohydrate', 'protein', 'fat']

/** 기준 날짜가 속한 주(월~일) 또는 달의 시작·끝 날짜. */
function dates(anchor, period) {
  const start = new Date(`${anchor}T12:00:00`)
  const end = new Date(start)
  if (period === 'week') {
    start.setDate(start.getDate() - ((start.getDay() + 6) % 7))
    end.setTime(start.getTime())
    end.setDate(end.getDate() + 6)
  } else {
    start.setDate(1)
    end.setMonth(end.getMonth() + 1, 0)
  }
  return { start: dateString(start), end: dateString(end) }
}

function nutrientCell(day, key) {
  if (!day.records) return '기록 없음'
  const missing = day[key].missingCount ? ` (누락 ${day[key].missingCount}건)` : ''
  return `${day[key].value.toLocaleString()}${missing}`
}

function DailyTable({ days, page, onPage }) {
  return (
    <section className="panel">
      <h2>날짜별 기록</h2>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>날짜</th>
              <th>섭취 건수</th>
              <th>칼로리</th>
              <th>탄수화물</th>
              <th>단백질</th>
              <th>지방</th>
              <th>체중</th>
            </tr>
          </thead>
          <tbody>
            {days.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE).map(day => (
              <tr key={day.date}>
                <td>{day.date}</td>
                <td>{day.records}</td>
                {NUTRIENT_KEYS.map(key => (
                  <td key={key}>{nutrientCell(day, key)}</td>
                ))}
                <td>{day.weight ?? '기록 없음'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div className="pagination">
        <button className="outline" disabled={!page} onClick={() => onPage(page - 1)}>
          이전 날짜
        </button>
        <span>
          {page + 1} / {Math.ceil(days.length / PAGE_SIZE)}
        </span>
        <button className="outline" disabled={(page + 1) * PAGE_SIZE >= days.length} onClick={() => onPage(page + 1)}>
          다음 날짜
        </button>
      </div>
    </section>
  )
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
    api
      .range(range.start, range.end, controller.signal)
      .then(value => {
        if (!controller.signal.aborted) {
          setData(value)
          setError('')
        }
      })
      .catch(failure => {
        if (!controller.signal.aborted) setError(failure.message)
      })
    return () => controller.abort()
  }, [api, range.start, range.end, retry]) // range는 날짜 문자열로 비교한다.

  const current = data?.start === range.start && data?.end === range.end ? data : null

  return (
    <>
      <div className="page-heading">
        <h1>기록 추이</h1>
        <p>주·월별 영양 섭취와 체중 변화를 확인하세요.</p>
      </div>
      <div className="range-toolbar">
        <label className="field">
          <span>기간</span>
          <select
            value={period}
            onChange={event => {
              setPeriod(event.target.value)
              setPage(0)
            }}
          >
            <option value="week">주간</option>
            <option value="month">월간</option>
          </select>
        </label>
        <Field
          label="기준 날짜"
          type="date"
          value={anchor}
          onChange={event => {
            if (event.target.value) {
              setAnchor(event.target.value)
              setPage(0)
            }
          }}
        />
      </div>
      {error ? (
        <div className="message error" role="alert">
          {error}
          <button className="outline" onClick={() => setRetry(value => value + 1)}>
            다시 불러오기
          </button>
        </div>
      ) : !current ? (
        <p role="status">기록을 불러오는 중…</p>
      ) : (
        <>
          <section className="panel">
            <h2>
              {current.start} ~ {current.end}
            </h2>
            <TrendChart days={current.days} type="calories" title="칼로리 추이" unit="kcal" />
            <TrendChart days={current.days} type="weight" title="체중 추이" unit="kg" />
            <p className="help">기록이 있는 날짜만 연결합니다. 기록이 없는 날은 0으로 추정하지 않습니다.</p>
          </section>
          <DailyTable days={current.days} page={page} onPage={setPage} />
        </>
      )}
    </>
  )
}
