import { useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { notesApi } from '../api/notes.js'
import './QuickCapture.css'

export default function QuickCapture() {
  const [open, setOpen] = useState(false)
  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const titleRef = useRef(null)
  const navigate = useNavigate()

  // Ctrl+K (또는 Cmd+K) 글로벌 단축키
  useEffect(() => {
    const handleKey = (e) => {
      if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
        e.preventDefault()
        setOpen((prev) => !prev)
      }
      if (e.key === 'Escape') {
        setOpen(false)
      }
    }
    window.addEventListener('keydown', handleKey)
    return () => window.removeEventListener('keydown', handleKey)
  }, [])

  // 열릴 때 제목 입력에 포커스
  useEffect(() => {
    if (open) {
      setTitle('')
      setContent('')
      setError('')
      setTimeout(() => titleRef.current?.focus(), 60)
    }
  }, [open])

  const handleSave = useCallback(
    async (andOpen = false) => {
      if (!title.trim()) {
        setError('제목을 입력해 주세요.')
        titleRef.current?.focus()
        return
      }
      setSaving(true)
      try {
        const res = await notesApi.create(title.trim(), content.trim())
        const newId = res.data?.data?.id
        setOpen(false)
        // 대시보드 등 열려 있는 목록이 새 노트를 반영하도록 알린다
        window.dispatchEvent(new CustomEvent('knotnote:notes-changed'))
        if (andOpen && newId) {
          navigate(`/notes/${newId}`)
        }
      } catch {
        setError('저장에 실패했습니다. 다시 시도해 주세요.')
      } finally {
        setSaving(false)
      }
    },
    [title, content, navigate],
  )

  // Ctrl+Enter → 저장하고 에디터로
  const handleKeyDown = (e) => {
    if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
      e.preventDefault()
      handleSave(true)
    }
  }

  if (!open) return null

  return (
    <div className="qc-overlay" onClick={() => setOpen(false)}>
      <div className="qc-modal" onClick={(e) => e.stopPropagation()} onKeyDown={handleKeyDown}>
        <div className="qc-header">
          <span className="qc-title-label">⚡ 빠른 메모</span>
          <span className="qc-hint">Esc 닫기 &nbsp;·&nbsp; Ctrl+Enter 저장 후 열기</span>
        </div>

        <input
          ref={titleRef}
          className="qc-title-input"
          placeholder="제목"
          value={title}
          onChange={(e) => {
            setTitle(e.target.value)
            setError('')
          }}
        />

        <textarea
          className="qc-content-input"
          placeholder="내용 (선택 사항)"
          value={content}
          rows={5}
          onChange={(e) => setContent(e.target.value)}
        />

        {error && <p className="qc-error">{error}</p>}

        <div className="qc-actions">
          <button className="btn btn-ghost btn-sm" onClick={() => setOpen(false)}>
            취소
          </button>
          <button
            className="btn btn-secondary btn-sm"
            onClick={() => handleSave(false)}
            disabled={saving}
          >
            저장
          </button>
          <button
            className="btn btn-primary btn-sm"
            onClick={() => handleSave(true)}
            disabled={saving}
          >
            {saving ? '저장 중…' : '저장 후 열기'}
          </button>
        </div>
      </div>
    </div>
  )
}
