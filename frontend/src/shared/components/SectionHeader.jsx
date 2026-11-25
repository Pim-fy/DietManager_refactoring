import './SectionHeader.css'

/** 구역 제목(h2)과 보조 정보(meta), 오른쪽 링크·버튼(children). divided는 끼니 목록처럼 굵은 밑줄로 나누는 큰 제목이다. */
export default function SectionHeader({ title, titleId, meta, divided = false, children }) {
  return (
    <div className={divided ? 'section-head divided' : 'section-head'}>
      <h2 id={titleId}>{title}</h2>
      {meta != null ? <span className="section-meta">{meta}</span> : null}
      {children != null ? <div className="section-action">{children}</div> : null}
    </div>
  )
}
