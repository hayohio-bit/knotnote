import { useEffect, useRef, useState } from 'react'
import './ConfirmHost.css'

/**
 * confirmDialog() (lib/confirm.js) 가 발행한 'knotnote:confirm' 이벤트를 받아
 * 확인 모달을 렌더링한다. 한 번에 하나만 표시한다.
 */
export default function ConfirmHost() {
  const [pending, setPending] = useState(null)
  const confirmBtnRef = useRef(null)

  const close = (result) => {
    pending?.resolve(result)
    setPending(null)
  }

  useEffect(() => {
    const onConfirm = (e) => {
      setPending((prev) => {
        // 이미 열린 모달이 있으면 취소로 정리하고 새 요청을 표시한다
        prev?.resolve(false)
        return e.detail
      })
    }
    window.addEventListener('knotnote:confirm', onConfirm)
    return () => window.removeEventListener('knotnote:confirm', onConfirm)
  }, [])

  useEffect(() => {
    if (!pending) return
    confirmBtnRef.current?.focus()
    const handler = (e) => {
      if (e.key === 'Escape') {
        pending.resolve(false)
        setPending(null)
      }
    }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [pending])

  if (!pending) return null

  return (
    <div
      className="confirm-overlay"
      onClick={(e) => {
        if (e.target === e.currentTarget) close(false)
      }}
    >
      <div className="confirm-modal" role="alertdialog" aria-modal="true">
        <p className="confirm-message">{pending.message}</p>
        <div className="confirm-actions">
          <button type="button" className="btn btn-ghost" onClick={() => close(false)}>
            취소
          </button>
          <button
            type="button"
            ref={confirmBtnRef}
            className={`btn ${pending.danger ? 'confirm-btn-danger' : 'btn-primary'}`}
            onClick={() => close(true)}
          >
            {pending.confirmLabel}
          </button>
        </div>
      </div>
    </div>
  )
}
