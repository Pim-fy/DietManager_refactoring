import { useEffect, useMemo, useRef, useState } from 'react'

import { Field, Message } from '../../shared/components/Forms'
import { foodLabel, formatNutrition as nutrient } from '../../shared/utils/nutrition'
import { foodApi } from './api'

const PAGE_SIZE = 10

function FoodResult({ food, disabled, onSelect, onFavorite }) {
  const usable = food.baseAmount > 0 && ['g', 'ml'].includes(food.baseUnit)
  return (
    <li>
      <div>
        <strong>{foodLabel(food.foodName)}</strong>
        <p>
          {food.makerName || '식약처 식품영양성분 DB'} · 기준 {food.servingSize || '정보 없음'}
        </p>
        <p className="food-macros">
          {nutrient(food.calories)} kcal · 탄수화물 {nutrient(food.carbohydrate)} g · 단백질 {nutrient(food.protein)} g · 지방 {nutrient(food.fat)} g
        </p>
        {!usable ? <p className="unit-warning">기준량 단위를 확인할 수 없어 기록할 수 없습니다.</p> : null}
      </div>
      <div className="row-actions">
        <button className="outline" disabled={disabled || !usable} onClick={() => onSelect(food)} aria-label={`${food.foodName} 선택`}>
          선택
        </button>
        <button className="outline" disabled={disabled || !usable} onClick={() => onFavorite(food)} aria-label={`${food.foodName} 즐겨찾기 추가`}>
          즐겨찾기
        </button>
      </div>
    </li>
  )
}

export default function FoodSearch({ call, onSelect, onFavorite, disabled }) {
  const api = useMemo(() => foodApi(call), [call])
  const [query, setQuery] = useState('')
  const [result, setResult] = useState(null)
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState(null)
  const [usage, setUsage] = useState(null)
  const pending = useRef(null)

  useEffect(() => () => pending.current?.abort(), [])

  useEffect(() => {
    const controller = new AbortController()
    api
      .usage(controller.signal)
      .then(value => {
        if (!controller.signal.aborted) setUsage(value)
      })
      .catch(() => {})
    return () => controller.abort()
  }, [api])

  async function favorite(food) {
    setBusy(true)
    try {
      await api.favorite(food.foodCode, true)
      setMessage({ text: '즐겨찾기에 추가했습니다.' })
      onFavorite?.()
    } catch (error) {
      setMessage({ text: error.message, error: true })
    } finally {
      setBusy(false)
    }
  }

  async function search(keyword, page = 1) {
    pending.current?.abort()
    const controller = new AbortController()
    pending.current = controller
    setBusy(true)
    setMessage(null)
    setQuery(keyword)
    try {
      const params = new URLSearchParams({ keyword, page, size: PAGE_SIZE })
      const data = await api.search(params, controller.signal)
      if (!controller.signal.aborted) {
        setResult(data)
        setQuery(keyword)
      }
    } catch (error) {
      if (!controller.signal.aborted) setMessage({ text: error.message, error: true })
    } finally {
      if (!controller.signal.aborted) {
        setBusy(false)
        api
          .usage(controller.signal)
          .then(value => {
            if (!controller.signal.aborted) setUsage(value)
          })
          .catch(() => {})
      }
    }
  }

  function submit(event) {
    event.preventDefault()
    const keyword = new FormData(event.currentTarget).get('keyword').trim()
    if (!keyword) {
      setMessage({ text: '검색어를 입력해주세요.', error: true })
      return
    }
    setResult(null)
    search(keyword)
  }

  const locked = busy || disabled

  return (
    <section className="panel">
      <h2>식품 찾기</h2>
      <form className="search-form" onSubmit={submit}>
        <Field label="식품명" name="keyword" maxLength={100} placeholder="예: 밥, 닭가슴살" />
        <button className="primary" disabled={locked}>
          {busy ? '검색 중…' : '검색'}
        </button>
      </form>
      <Message message={message} />
      {message?.error && query ? (
        <button className="outline" disabled={locked} onClick={() => search(query)}>
          검색 다시 시도
        </button>
      ) : null}
      {usage ? (
        <p className="help">
          오늘 외부 API 호출 {usage.calls.toLocaleString()} / {usage.dayLimit.toLocaleString()}회 · 분당 최대 {usage.minuteLimit}회 (검색 캐시는 호출 제외)
        </p>
      ) : null}
      {result ? (
        <>
          <p className="search-count" role="status">
            검색 결과 {result.totalCount.toLocaleString()}건 · {result.page}페이지
          </p>
          <ul className="food-results">
            {result.items.map(food => (
              <FoodResult
                key={food.foodCode}
                food={food}
                disabled={locked}
                onSelect={value => {
                  onSelect(value)
                  setResult(null)
                }}
                onFavorite={favorite}
              />
            ))}
          </ul>
          {result.items.length === 0 ? (
            <p className="empty">
              검색 결과가 없습니다.
              <br />
              <span>다른 식품명으로 검색해보세요.</span>
            </p>
          ) : null}
          <div className="pagination">
            <button className="outline" disabled={locked || result.page <= 1} onClick={() => search(query, result.page - 1)}>
              이전
            </button>
            <span>
              {result.page} / {Math.max(1, Math.ceil(result.totalCount / result.size))}
            </span>
            <button className="outline" disabled={locked || result.page * result.size >= result.totalCount} onClick={() => search(query, result.page + 1)}>
              다음
            </button>
          </div>
        </>
      ) : (
        <p className="help">식품을 검색하고 실제로 먹은 양을 입력하세요. 영양값은 식품별 기준량에 따라 계산합니다.</p>
      )}
    </section>
  )
}
