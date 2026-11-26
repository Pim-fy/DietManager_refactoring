import './Pagination.css'

/** 이전 · 현재/전체 · 다음. page는 1부터 센다. 버튼 이름은 쓰는 곳마다 다르게 줄 수 있다. */
export default function Pagination({ page, total, disabled = false, onChange, previousLabel = '이전', nextLabel = '다음' }) {
  return (
    <div className="pagination">
      <button className="outline" disabled={disabled || page <= 1} onClick={() => onChange(page - 1)}>
        {previousLabel}
      </button>
      <span>
        {page} / {total}
      </span>
      <button className="outline" disabled={disabled || page >= total} onClick={() => onChange(page + 1)}>
        {nextLabel}
      </button>
    </div>
  )
}
