import { useEffect, useRef } from 'react'

/** 브라우저 기본 모달(dialog). Esc·닫기 버튼·바깥 클릭으로 onClose를 부른다. */
export default function Dialog({ open, title, onClose, children }) {
  const ref = useRef(null)

  useEffect(() => {
    const dialog = ref.current
    if (open && !dialog.open) dialog.showModal()
    if (!open && dialog.open) dialog.close()
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
      onClick={event => {
        if (event.target === ref.current) onClose()
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
