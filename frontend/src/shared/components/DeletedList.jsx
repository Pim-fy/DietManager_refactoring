import { useState } from 'react'

import './DeletedList.css'

/**
 * 삭제한 기록 목록과 복구 버튼. 항목이 없으면 그리지 않는다.
 * preview를 주면 그 개수만 먼저 보여주고 더 보기·접기로 나머지를 펼친다.
 */
export default function DeletedList({ title, items, itemKey, renderLabel, restoreName, busy, onRestore, preview }) {
  const [expanded, setExpanded] = useState(false)
  if (!items.length) return null
  const hidden = preview ? items.length - preview : 0
  const shown = expanded || !preview ? items : items.slice(0, preview)
  return (
    <div className="undo-list">
      <h3>{title}</h3>
      {shown.map(item => (
        <p key={itemKey(item)}>
          <span>{renderLabel(item)}</span>
          <button className="text-button" disabled={busy} aria-label={restoreName(item)} onClick={() => onRestore(item)}>
            복구
          </button>
        </p>
      ))}
      {hidden > 0 ? (
        <button className="text-button more" type="button" aria-expanded={expanded} onClick={() => setExpanded(value => !value)}>
          {expanded ? '접기' : `더 보기 (남은 ${hidden}개)`}
        </button>
      ) : null}
    </div>
  )
}
