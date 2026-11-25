import { useState } from 'react'

import FoodSearch from './FoodSearch'
import FoodShelf from './FoodShelf'

/**
 * 식품 고르기 탭(식품 검색 · 내 식품, 필요하면 직접 입력).
 * manual을 주면 '직접 입력' 탭에 그 내용을 보여준다.
 */
export default function FoodPicker({ call, disabled, revision, onSelect, onFavorite, manual }) {
  const [tab, setTab] = useState('search')
  const tabs = [
    ['search', '식품 검색'],
    ['shelf', '내 식품'],
    ...(manual ? [['manual', '직접 입력']] : []),
  ]

  return (
    <>
      <div className="tabs" role="tablist" aria-label="식품 고르기">
        {tabs.map(([key, label]) => (
          <button key={key} type="button" role="tab" aria-selected={tab === key} onClick={() => setTab(key)}>
            {label}
          </button>
        ))}
      </div>
      <div role="tabpanel" className="tab-panel">
        {tab === 'search' ? <FoodSearch call={call} disabled={disabled} onFavorite={onFavorite} onSelect={onSelect} /> : null}
        {tab === 'shelf' ? <FoodShelf call={call} disabled={disabled} revision={revision} onSelect={onSelect} /> : null}
        {tab === 'manual' ? manual : null}
      </div>
    </>
  )
}
