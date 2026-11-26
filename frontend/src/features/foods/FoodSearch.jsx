import { useEffect, useMemo, useRef, useState } from 'react'

import EmptyState from '../../shared/components/EmptyState'
import { Field, Message } from '../../shared/components/Forms'
import Pagination from '../../shared/components/Pagination'
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
          {food.makerName || '식약처 식품영양성분 DB'}, {food.servingSize || '기준량 정보 없음'} 기준
        </p>
        <p className="food-macros">
          <span>{nutrient(food.calories, 0)} kcal</span>
          <span>탄수화물 {nutrient(food.carbohydrate, 1)} g</span>
          <span>단백질 {nutrient(food.protein, 1)} g</span>
          <span>지방 {nutrient(food.fat, 1)} g</span>
        </p>
        {!usable ? <p className="unit-warning">기준량 단위를 확인할 수 없어 기록할 수 없습니다.</p> : null}
      </div>
      <div className="row-actions">
        <button className="text-button" disabled={disabled || !usable} onClick={() => onFavorite(food)} aria-label={`${food.foodName} 즐겨찾기 추가`}>
          즐겨찾기
        </button>
        <button className="outline" disabled={disabled || !usable} onClick={() => onSelect(food)} aria-label={`${food.foodName} 선택`}>
          선택
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
  const remaining = usage ? usage.dayLimit - usage.calls : null

  return (
    <div className="food-search">
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
      {remaining != null && remaining <= usage.dayLimit * 0.1 ? (
        <p className="unit-warning">오늘 새로 검색할 수 있는 횟수가 {Math.max(0, remaining).toLocaleString()}회 남았습니다. 이미 검색한 식품은 횟수와 상관없이 다시 볼 수 있습니다.</p>
      ) : null}
      {result ? (
        <>
          <p className="search-count" role="status">
            검색 결과 {result.totalCount.toLocaleString()}건
          </p>
          <ul className="food-results">
            {result.items.map(food => (
              <FoodResult
                key={food.foodCode}
                food={food}
                disabled={locked}
                onSelect={onSelect}
                onFavorite={favorite}
              />
            ))}
          </ul>
          {result.items.length === 0 ? (
            <EmptyState>
              검색 결과가 없습니다.
              <br />
              <span>다른 식품명으로 검색해보세요.</span>
            </EmptyState>
          ) : null}
          <Pagination
            page={result.page}
            total={Math.max(1, Math.ceil(result.totalCount / result.size))}
            disabled={locked}
            onChange={value => search(query, value)}
          />
        </>
      ) : (
        <p className="help">식품을 고른 뒤 실제로 먹은 양을 입력하면 영양값을 계산합니다.</p>
      )}
    </div>
  )
}
