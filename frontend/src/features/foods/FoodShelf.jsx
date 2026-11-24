import { useEffect, useMemo, useState } from 'react'

import { Message, RetryButton } from '../../shared/components/Forms'
import { foodLabel } from '../../shared/utils/nutrition'
import { foodApi } from './api'

const SHELVES = [
  { key: 'favorites', title: '즐겨찾기', label: '즐겨찾기', empty: '검색한 식품을 즐겨찾기에 추가해보세요.' },
  { key: 'recent', title: '최근 섭취 식품', label: '최근 식품', empty: '섭취 기록을 저장하면 최근 식품에 표시됩니다.' },
]

function Shelf({ shelf, items, disabled, onSelect, onRemove }) {
  return (
    <div>
      <h3>{shelf.title}</h3>
      {items.length === 0 ? (
        <p className="help">{shelf.empty}</p>
      ) : (
        <ul className="saved-foods">
          {items.map(({ food }) => (
            <li key={food.foodCode}>
              <button className="outline" disabled={disabled} aria-label={`${shelf.label} ${food.foodName} 선택`} onClick={() => onSelect(food)}>
                {foodLabel(food.foodName)} · {food.servingSize}
              </button>
              {shelf.key === 'favorites' ? (
                <button className="outline danger" disabled={disabled} aria-label={`${food.foodName} 즐겨찾기 해제`} onClick={() => onRemove(food)}>
                  해제
                </button>
              ) : null}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

/** 즐겨찾기와 최근 섭취 식품. revision이 바뀌면 다시 불러온다. */
export default function FoodShelf({ call, onSelect, disabled, revision = 0 }) {
  const api = useMemo(() => foodApi(call), [call])
  const [data, setData] = useState(null)
  const [message, setMessage] = useState(null)
  const [retry, setRetry] = useState(0)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    const controller = new AbortController()
    Promise.all([api.favorites(controller.signal), api.recent(controller.signal)])
      .then(([favorites, recent]) => {
        if (!controller.signal.aborted) {
          setData({ favorites, recent })
          setMessage(null)
        }
      })
      .catch(error => {
        if (!controller.signal.aborted) setMessage({ text: error.message, error: true })
      })
    return () => controller.abort()
  }, [api, retry, revision])

  async function remove(food) {
    setBusy(true)
    try {
      await api.favorite(food.foodCode, false)
      setRetry(value => value + 1)
    } catch (error) {
      setMessage({ text: error.message, error: true })
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="panel food-shelf">
      <h2>내 식품</h2>
      <Message message={message} />
      {message?.error ? <RetryButton onRetry={() => setRetry(value => value + 1)} /> : null}
      {!data ? (
        <p role="status">저장한 식품을 불러오는 중…</p>
      ) : (
        SHELVES.map(shelf => (
          <Shelf key={shelf.key} shelf={shelf} items={data[shelf.key]} disabled={disabled || busy} onSelect={onSelect} onRemove={remove} />
        ))
      )}
    </section>
  )
}
