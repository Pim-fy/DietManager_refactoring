import './EmptyState.css'

/** 빈 상태 문구. action(버튼·링크)을 주면 문구 아래에 둔다. size="compact"는 목록 안의 짧은 빈 줄이다. */
export default function EmptyState({ children, action, size }) {
  const className = size === 'compact' ? 'empty compact' : 'empty'
  if (action == null) return <p className={className}>{children}</p>
  return (
    <div className={className}>
      <p>{children}</p>
      {action}
    </div>
  )
}
