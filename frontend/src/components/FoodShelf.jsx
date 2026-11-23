import { foodApi } from '../features/api'
import { useEffect, useMemo, useState } from 'react'
import { Message } from './Forms'
export default function FoodShelf({ call, onSelect, disabled, revision = 0 }) {
  const api = useMemo(() => foodApi(call), [call])
  const [data, setData] = useState(null)
  const [message, setMessage] = useState(null)
  const [retry, setRetry] = useState(0)
  const [busy, setBusy] = useState(false)
  useEffect(() => {
    const controller = new AbortController()
    Promise.all([api.favorites(controller.signal), api.recent(controller.signal)])
      .then(([favorites, recent]) => { if (!controller.signal.aborted) { setData({ favorites, recent }); setMessage(null) } })
      .catch(error => { if (!controller.signal.aborted) setMessage({ text: error.message, error: true }) })
    return () => controller.abort()
  }, [api, retry, revision])
  async function remove(food) {
    setBusy(true)
    try { await api.favorite(food.foodCode, false); setRetry(value => value + 1) }
    catch (error) { setMessage({ text: error.message, error: true }) }
    finally { setBusy(false) }
  }
  return <section className="panel food-shelf"><h2>내 식품</h2><Message message={message} />
    {message?.error ? <button className="outline" onClick={() => setRetry(value => value + 1)}>다시 불러오기</button> : null}
    {!data ? <p role="status">저장한 식품을 불러오는 중…</p> : ['favorites', 'recent'].map(key => <div key={key}><h3>{key === 'favorites' ? '즐겨찾기' : '최근 섭취 식품'}</h3>
      {data[key].length === 0 ? <p className="help">{key === 'favorites' ? '검색한 식품을 즐겨찾기에 추가해보세요.' : '섭취 기록을 저장하면 최근 식품에 표시됩니다.'}</p> : <ul className="saved-foods">{data[key].map(({ food }) => <li key={food.foodCode}>
        <button className="outline" disabled={disabled || busy} aria-label={`${key === 'favorites' ? '즐겨찾기' : '최근 식품'} ${food.foodName} 선택`} onClick={() => onSelect(food)}>{food.foodName.replaceAll('_', ' ')} · {food.servingSize}</button>
        {key === 'favorites' ? <button className="outline danger" disabled={disabled || busy} aria-label={`${food.foodName} 즐겨찾기 해제`} onClick={() => remove(food)}>해제</button> : null}
      </li>)}</ul>}</div>)}
  </section>
}
