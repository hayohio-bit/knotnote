import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { shareApi } from '../api/notes.js'
import MarkdownPreview from '../components/MarkdownPreview.jsx'
import Spinner from '../components/Spinner.jsx'
import './SharedNotePage.css'

function fmtDate(iso) {
  if (!iso) return ''
  return new Date(iso).toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  })
}

export default function SharedNotePage() {
  const { shareToken } = useParams()
  const [note, setNote] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    ;(async () => {
      try {
        const { data } = await shareApi.get(shareToken)
        setNote(data.data)
      } catch (err) {
        const status = err.response?.status
        if (status === 404 || status === 410) {
          setError('링크가 만료되었거나 존재하지 않습니다.')
        } else {
          setError('노트를 불러오지 못했습니다.')
        }
      } finally {
        setLoading(false)
      }
    })()
  }, [shareToken])

  if (loading)
    return (
      <div className="shared-page">
        <Spinner />
      </div>
    )

  if (error)
    return (
      <div className="shared-page">
        <div className="shared-error">
          <div className="shared-error-icon">🔗</div>
          <h1 className="shared-error-title">링크를 사용할 수 없습니다</h1>
          <p className="shared-error-desc">{error}</p>
          <Link to="/" className="btn btn-primary">
            KnotNote 홈으로
          </Link>
        </div>
      </div>
    )

  return (
    <div className="shared-page">
      <header className="shared-header">
        <Link to="/" className="shared-logo">
          KnotNote
        </Link>
        <span className="shared-badge">읽기 전용</span>
      </header>

      <main className="shared-main">
        <article className="shared-article">
          <h1 className="shared-title">{note.title}</h1>

          <div className="shared-meta">
            <span>{fmtDate(note.updatedAt)}</span>
            {note.tags?.length > 0 && (
              <div className="shared-tags">
                {note.tags.map((t) => (
                  <span key={t.id} className="shared-tag">
                    #{t.name}
                  </span>
                ))}
              </div>
            )}
          </div>

          {note.aiSummary && (
            <div className="shared-ai-summary">
              <span className="shared-ai-label">✨ AI 요약</span>
              <p>{note.aiSummary}</p>
            </div>
          )}

          <div className="shared-content">
            <MarkdownPreview source={note.content} />
          </div>
        </article>
      </main>

      <footer className="shared-footer">
        <p>
          <Link to="/">KnotNote</Link>로 지식 그래프를 만들어보세요
        </p>
      </footer>
    </div>
  )
}
