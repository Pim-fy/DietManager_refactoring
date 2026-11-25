import { useEffect, useMemo, useState } from 'react'

import { Field } from '../../shared/components/Forms'
import { statisticsApi } from './api'
import { dates } from './range'
import TrendChart from './TrendChart'
import useTrendsQuery from './useTrendsQuery'

const PAGE_SIZE = 14

/** '2026-11-23' → '11월 23일' */
function shortDate(value) {
  const [, month, day] = value.split('-')
  return `${Number(month)}월 ${Number(day)}일`
}
const NUTRIENT_KEYS = ['calories', 'carbohydrate', 'protein', 'fat']

function nutrientCell(day, key) {
  if (!day.records) return '–'
  const missing = day[key].missingCount ? ` (정보 없는 ${day[key].missingCount}건 제외)` : ''
  return `${day[key].value.toLocaleString('ko-KR', { maximumFractionDigits: key === 'calories' ? 0 : 1 })}${missing}`
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
              <th className="num">섭취 건수</th>
              <th className="num">칼로리 (kcal)</th>
              <th className="num">탄수화물 (g)</th>
              <th className="num">단백질 (g)</th>
              <th className="num">지방 (g)</th>
              <th className="num">체중 (kg)</th>
            </tr>
          </thead>
          <tbody>
            {days.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE).map(day => (
              <tr key={day.date}>
                <td>{day.date}</td>
                <td className="num">{day.records}</td>
                {NUTRIENT_KEYS.map(key => (
                  <td key={key} className={day.records ? 'num' : 'num muted'}>
                    {nutrientCell(day, key)}
                  </td>
                ))}
                <td className={day.weight == null ? 'num muted' : 'num'}>{day.weight ?? '–'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {days.length > PAGE_SIZE ? (
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
      ) : null}
    </section>
  )
}

export default function TrendsPage({ call }) {
  const api = useMemo(() => statisticsApi(call), [call])
  const { period, anchor, setPeriod, setAnchor } = useTrendsQuery()
  const [data, setData] = useState(null)
  const [error, setError] = useState('')
  const [retry, setRetry] = useState(0)
  // 날짜 표 페이지는 기간·기준 날짜가 바뀌면(뒤로가기 포함) 처음 페이지로 돌아간다.
  const pageKey = `${period}|${anchor}`
  const [pageState, setPageState] = useState({ key: pageKey, page: 0 })
  const page = pageState.key === pageKey ? pageState.page : 0
  const setPage = value => setPageState({ key: pageKey, page: value })
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
      <div className="page-heading heading-inline">
        <h1>기록 추이</h1>
        <p>
          {shortDate(range.start)} – {shortDate(range.end)}
        </p>
      </div>
      <div className="range-toolbar">
        <label className="field">
          <span>기간</span>
          <select
            value={period}
            onChange={event => setPeriod(event.target.value)}
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
            if (event.target.value) setAnchor(event.target.value)
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
        <p className="loading" role="status">
          기록을 불러오는 중…
        </p>
      ) : (
        <>
          <section className="panel">
            <h2>
              <span className="visually-hidden">기간별 그래프</span>
            </h2>
            <TrendChart days={current.days} type="calories" title="칼로리 추이" unit="kcal" />
            <TrendChart days={current.days} type="weight" title="체중 추이" unit="kg" />
            <p className="help">기록이 있는 날짜만 선으로 잇고, 기록이 없는 날은 0으로 치지 않습니다. 점에 마우스를 올리면 값이 보입니다.</p>
          </section>
          <DailyTable days={current.days} page={page} onPage={setPage} />
        </>
      )}
    </>
  )
}
