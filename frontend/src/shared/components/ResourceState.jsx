/** useResource 결과의 불러오는 중·오류 상태를 공통으로 표시한다. */
export default function ResourceState({ resource, loadingText = '불러오는 중…', children }) {
  if (resource.error) {
    return (
      <div className="message error" role="alert">
        <p>{resource.error}</p>
        <button className="outline" onClick={resource.retry}>
          다시 불러오기
        </button>
      </div>
    )
  }
  if (!resource.data) return <p className="loading" role="status">{loadingText}</p>
  return children
}
