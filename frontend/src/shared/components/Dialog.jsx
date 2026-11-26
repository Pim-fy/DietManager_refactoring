import { useEffect, useRef } from 'react'

import './Dialog.css'

function withoutDialog(state) {
  const rest = { ...state }
  delete rest.dialog
  return Object.keys(rest).length ? rest : null
}

/**
 * 브라우저 기본 모달(dialog). Esc·닫기 버튼·바깥 클릭(누른 곳과 뗀 곳이 모두 바깥)으로 onClose를 부른다.
 * 열릴 때 같은 주소로 방문 기록을 한 단계 추가해, 뒤로가기는 창만 닫는다(화면은 그대로).
 * 코드로 닫힐 때는 추가한 단계를 history.back()으로 되돌려 뒤로가기를 한 번 더 누르지 않게 한다.
 */
export default function Dialog({ open, title, onClose, children }) {
  const ref = useRef(null)
  const pressedBackdrop = useRef(false)
  const onCloseRef = useRef(onClose)
  const openRef = useRef(open)

  useEffect(() => {
    onCloseRef.current = onClose
    openRef.current = open
  })

  // 새로고침 직후 남아 있는 창 표시를 지우고, 닫힌 창의 기록 단계로 앞으로가기 되면 그 단계를 건너뛴다.
  useEffect(() => {
    if (window.history.state?.dialog) window.history.replaceState(withoutDialog(window.history.state), '')
    function skipStaleStep() {
      if (window.history.state?.dialog && !openRef.current) window.history.back()
    }
    window.addEventListener('popstate', skipStaleStep)
    return () => window.removeEventListener('popstate', skipStaleStep)
  }, [])

  useEffect(() => {
    const dialog = ref.current
    if (!open) {
      if (dialog.open) dialog.close()
      return undefined
    }
    if (!dialog.open) dialog.showModal()
    window.history.pushState({ ...window.history.state, dialog: true }, '')
    // 브라우저 뒤로가기로 닫힌 경우: 이미 단계가 사라졌으므로 history.back()을 부르지 않는다.
    const onPopState = () => onCloseRef.current()
    window.addEventListener('popstate', onPopState)
    return () => {
      window.removeEventListener('popstate', onPopState) // 아래 back()이 일으키는 popstate는 처리하지 않는다.
      if (window.history.state?.dialog) window.history.back()
    }
  }, [open])

  return (
    <dialog
      ref={ref}
      className="sheet"
      aria-label={title}
      onCancel={event => {
        event.preventDefault()
        onClose()
      }}
      onPointerDown={event => {
        pressedBackdrop.current = event.target === ref.current
      }}
      onClick={event => {
        if (pressedBackdrop.current && event.target === ref.current) onClose()
        pressedBackdrop.current = false
      }}
    >
      {open ? (
        <div className="sheet-body">
          <div className="sheet-header">
            <h2>{title}</h2>
            <button type="button" className="text-button" onClick={onClose}>
              닫기
            </button>
          </div>
          {children}
        </div>
      ) : null}
    </dialog>
  )
}
