import './TagBadge.css'

export default function TagBadge({ name, onRemove }) {
  return (
    <span className="tag-badge">
      #{name}
      {onRemove && (
        <button
          className="tag-badge-remove"
          onClick={(e) => {
            e.preventDefault()
            e.stopPropagation()
            onRemove()
          }}
          aria-label={`${name} 태그 제거`}
        >
          ×
        </button>
      )}
    </span>
  )
}
