import { navigate } from '../hooks/usePathRoute'

/** 화면 이동 링크. 일반 클릭은 새로고침 없이 이동하고, 새 탭 열기 등은 브라우저에 맡긴다. */
export default function AppLink({ to, onClick, children, ...rest }) {
  function handleClick(event) {
    onClick?.(event)
    if (event.defaultPrevented || event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return
    event.preventDefault()
    navigate(to)
  }

  return (
    <a href={to} onClick={handleClick} {...rest}>
      {children}
    </a>
  )
}
