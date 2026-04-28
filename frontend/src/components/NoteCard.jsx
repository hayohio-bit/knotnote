import { Link, useNavigate } from 'react-router-dom'
import TagBadge from './TagBadge.jsx'
import './NoteCard.css'

function stripMarkdown(text) {
  return (text || '')
    .replace(/^#{1,6}\s+/gm, '')
    .replace(/\*\*(.+?)\*\*/g, '$1')
    .replace(/\*(.+?)\*/g, '$1')
    .replace(/`(.+?)`/g, '$1')
    .replace(/^[-*]\s+/gm, '')
    .replace(/^\d+\.\s+/gm, '')
    .replace(/^>\s+/gm, '')
    .trim()
}

function feedPreview(text, maxChars = 800) {
  const stripped = (text || '')
    .replace(/^#{1,6}\s+/gm, '')
    .replace(/\*\*(.+?)\*\*/g, '$1')
    .replace(/\*(.+?)\*/g, '$1')
    .replace(/`(.+?)`/g, '$1')
    .replace(/^[-*]\s+/gm, '• ')
    .replace(/^\d+\.\s+/gm, '')
    .replace(/^>\s+/gm, '')
    .replace(/\n{3,}/g, '\n\n')
    .trim()
  if (stripped.length <= maxChars) return { text: stripped, truncated: false }
  const cut = stripped.slice(0, maxChars).replace(/\s+\S*$/, '')
  return { text: cut, truncated: true }
}

function formatDate(iso) {
  return new Date(iso).toLocaleDateString('ko-KR', {
    year: 'numeric', month: 'short', day: 'numeric',
  })
}

// ── 리스트 모드 ───────────────────────────────────────────────
function ListItem({ note, bulkMode, selected, onSelect }) {
  const navigate = useNavigate()

  const handleClick = (e) => {
    if (bulkMode) {
      e.preventDefault()
      onSelect?.(note.id)
    }
  }

  return (
    <Link
      to={`/notes/${note.id}`}
      className={`note-list-item ${bulkMode ? 'bulk-selectable' : ''} ${selected ? 'bulk-selected' : ''}`}
      onClick={handleClick}
    >
      {bulkMode && (
        <span className="bulk-checkbox" aria-checked={selected}>
          {selected ? '☑' : '☐'}
        </span>
      )}
      {note.isPinned && <span className="note-pin-badge" title="고정됨">📌</span>}
      <span className="note-list-title">{note.title}</span>
      <div className="note-list-right">
        {note.tags?.slice(0, 3).map((tag) => (
          <TagBadge key={tag.id} name={tag.name} />
        ))}
        <span className="note-list-date">{formatDate(note.updatedAt || note.createdAt)}</span>
      </div>
    </Link>
  )
}

// ── 피드 모드 ─────────────────────────────────────────────────
function FeedItem({ note, bulkMode, selected, onSelect }) {
  const { text: preview, truncated } = feedPreview(note.preview)

  const handleClick = (e) => {
    if (bulkMode) {
      e.preventDefault()
      onSelect?.(note.id)
    }
  }

  return (
    <Link
      to={`/notes/${note.id}`}
      className={`note-feed-item ${bulkMode ? 'bulk-selectable' : ''} ${selected ? 'bulk-selected' : ''}`}
      onClick={handleClick}
    >
      {bulkMode && (
        <span className="bulk-checkbox feed-checkbox" aria-checked={selected}>
          {selected ? '☑' : '☐'}
        </span>
      )}
      <div className="note-feed-meta">
        {note.isPinned && <span className="note-pin-badge" title="고정됨">📌</span>}
        <span className="note-feed-date">{formatDate(note.updatedAt || note.createdAt)}</span>
        {note.tags?.slice(0, 4).map((tag) => (
          <TagBadge key={tag.id} name={tag.name} />
        ))}
      </div>
      <h3 className="note-feed-title">{note.title}</h3>
      {preview && (
        <div className="note-feed-preview">
          {preview.split('\n\n').map((para, i) => (
            <p key={i}>{para}</p>
          ))}
          {truncated && <span className="note-feed-more">계속 읽기 →</span>}
        </div>
      )}
    </Link>
  )
}

// ── 카드 모드 (기본) ──────────────────────────────────────────
function CardItem({ note, bulkMode, selected, onSelect }) {
  const preview = stripMarkdown(note.preview).slice(0, 120)

  const handleClick = (e) => {
    if (bulkMode) {
      e.preventDefault()
      onSelect?.(note.id)
    }
  }

  return (
    <Link
      to={`/notes/${note.id}`}
      className={`note-card ${bulkMode ? 'bulk-selectable' : ''} ${selected ? 'bulk-selected' : ''}`}
      onClick={handleClick}
    >
      {bulkMode && (
        <span className="bulk-checkbox card-checkbox" aria-checked={selected}>
          {selected ? '☑' : '☐'}
        </span>
      )}
      <div className="note-card-header">
        <h3 className="note-card-title">
          {note.isPinned && <span className="note-pin-badge" title="고정됨">📌 </span>}
          {note.title}
        </h3>
        <span className="note-card-date">{formatDate(note.updatedAt || note.createdAt)}</span>
      </div>
      {preview && <p className="note-card-preview">{preview}</p>}
      {note.tags?.length > 0 && (
        <div className="note-card-tags">
          {note.tags.map((tag) => (
            <TagBadge key={tag.id} name={tag.name} />
          ))}
        </div>
      )}
    </Link>
  )
}

// ── 공통 export ───────────────────────────────────────────────
export default function NoteCard({ note, viewMode = 'card', bulkMode = false, selected = false, onSelect }) {
  const props = { note, bulkMode, selected, onSelect }
  if (viewMode === 'list') return <ListItem {...props} />
  if (viewMode === 'feed') return <FeedItem {...props} />
  return <CardItem {...props} />
}
