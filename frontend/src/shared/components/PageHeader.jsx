import './PageHeader.css'

/** 페이지 제목(h1). meta는 제목 옆 보조 정보, tools는 오른쪽 도구 영역에 놓는다. */
export default function PageHeader({ title, meta, tools }) {
  const className = ['page-heading', meta != null && 'heading-inline', tools != null && 'with-tools'].filter(Boolean).join(' ')
  return (
    <div className={className}>
      <h1>{title}</h1>
      {meta != null ? <p>{meta}</p> : null}
      {tools != null ? <div className="page-tools">{tools}</div> : null}
    </div>
  )
}
