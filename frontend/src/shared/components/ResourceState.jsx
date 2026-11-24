/** useResource 결과의 불러오는 중·오류 상태를 공통으로 표시한다. */
export default function ResourceState({ resource, children }) {
  if (resource.error) {
    return (
      <div className="panel" role="alert">
        <p>{resource.error}</p>
        <button className="primary" onClick={resource.retry}>
          다시 불러오기
        </button>
      </div>
    )
  }
  if (!resource.data) return <p role="status">내 정보를 불러오는 중…</p>
  return children
}
