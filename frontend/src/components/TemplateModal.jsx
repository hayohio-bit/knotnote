import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { notesApi } from '../api/notes.js'
import './TemplateModal.css'

// ── 내장 템플릿 목록 ───────────────────────────────────────────
const TEMPLATES = [
  {
    id: 'daily',
    icon: '📅',
    label: '데일리 노트',
    titleFn: () => {
      const d = new Date()
      return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    },
    content: `## 오늘의 목표
- 

## 한 일
- 

## 배운 것
- 

## 내일 할 일
- 

---
*KnotNote 데일리 노트*`,
  },
  {
    id: 'meeting',
    icon: '🤝',
    label: '회의록',
    titleFn: () => {
      const d = new Date()
      return `회의록 ${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    },
    content: `## 참석자
- 

## 안건
1. 

## 논의 내용


## 결정 사항
- 

## 액션 아이템
- [ ] 담당: `,
  },
  {
    id: 'idea',
    icon: '💡',
    label: '아이디어',
    titleFn: () => '새 아이디어',
    content: `## 핵심 아이디어


## 배경 / 문제


## 해결 방법


## 다음 단계
- `,
  },
  {
    id: 'book',
    icon: '📚',
    label: '독서 노트',
    titleFn: () => '독서 노트',
    content: `## 책 정보
- **제목**: 
- **저자**: 
- **읽은 날**: 

## 핵심 요약


## 인상 깊은 구절
> 

## 적용할 점
- `,
  },
  {
    id: 'blank',
    icon: '📝',
    label: '빈 노트',
    titleFn: () => '',
    content: '',
  },
]

export default function TemplateModal({ onClose }) {
  const [saving, setSaving] = useState(null) // template id currently saving
  const navigate = useNavigate()

  const handleSelect = async (tpl) => {
    setSaving(tpl.id)
    try {
      const title = tpl.titleFn()
      const res = await notesApi.create(title, tpl.content)
      const newId = res.data?.data?.id
      onClose()
      if (newId) navigate(`/notes/${newId}`)
      else navigate('/notes/new')
    } catch {
      setSaving(null)
    }
  }

  return (
    <div className="tpl-overlay" onClick={onClose}>
      <div className="tpl-modal" onClick={(e) => e.stopPropagation()}>
        <div className="tpl-header">
          <h3 className="tpl-heading">📋 템플릿 선택</h3>
          <button className="tpl-close" onClick={onClose} aria-label="닫기">
            ✕
          </button>
        </div>
        <div className="tpl-grid">
          {TEMPLATES.map((tpl) => (
            <button
              key={tpl.id}
              className="tpl-item"
              onClick={() => handleSelect(tpl)}
              disabled={saving !== null}
            >
              <span className="tpl-icon">{tpl.icon}</span>
              <span className="tpl-label">{tpl.label}</span>
              {saving === tpl.id && <span className="tpl-spinner">…</span>}
            </button>
          ))}
        </div>
      </div>
    </div>
  )
}
