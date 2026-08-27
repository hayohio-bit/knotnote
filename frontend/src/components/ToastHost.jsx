import { useEffect, useState } from 'react'
import './ToastHost.css'

const DURATION = 3500

export default function ToastHost() {
  const [toasts, setToasts] = useState([])

  useEffect(() => {
    const onToast = (e) => {
      const t = e.detail
      setToasts((prev) => [...prev, t])
      setTimeout(() => {
        setToasts((prev) => prev.filter((x) => x.id !== t.id))
      }, DURATION)
    }
    window.addEventListener('knotnote:toast', onToast)
    return () => window.removeEventListener('knotnote:toast', onToast)
  }, [])

  if (toasts.length === 0) return null

  return (
    <div className="toast-host" role="status" aria-live="polite">
      {toasts.map((t) => (
        <div key={t.id} className={`toast toast-${t.type}`}>
          {t.message}
        </div>
      ))}
    </div>
  )
}
