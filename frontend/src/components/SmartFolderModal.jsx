import { useEffect, useState } from 'react'
import { smartFoldersApi } from '../api/smartFolders.js'
import './SmartFolderModal.css'

const DATE_OPTIONS = [
  { value: null, label: '전체' },
  { value: 7, label: '최근 7일' },
  { value: 30, label: '최근 30일' },
  { value: 90, label: '최근 90일' },
]

/**
 * 스마트 폴더 생성/편집 모달
 * Props:
 *  - allTags        : [{ id, name }]  — 전체 태그 목록
 *  - editingFolder  : SmartFolderResponse | null  — 수정 대상 (null = 생성 모드)
 *  - onClose        : () => void
 *  - onSaved        : (SmartFolderResponse) => void
 */
export default function SmartFolderModal({ allTags, editingFolder, onClose, onSaved }) {
  const isEdit = Boolean(editingFolder)

  const [name, setName] = useState(editingFolder?.name ?? '')
  const [selectedTagIds, setSelectedTagIds] = useState(editingFolder?.tagIds ?? [])
  const [tagMatchMode, setTagMatchMode] = useState(editingFolder?.tagMatchMode ?? 'ANY')
  const [createdWithin, setCreatedWithin] = useState(editingFolder?.createdWithinDays ?? null)
  const [keyword, setKeyword] = useState(editingFolder?.keyword ?? '')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  // ESC 닫기
  useEffect(() => {
    const handler = (e) => {
      if (e.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [onClose])

  const toggleTag = (tagId) => {
    setSelectedTagIds((prev) =>
      prev.includes(tagId) ? prev.filter((id) => id !== tagId) : [...prev, tagId],
    )
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!name.trim()) {
      setError('폴더 이름을 입력해주세요.')
      return
    }
    setSaving(true)
    setError('')
    const payload = {
      name: name.trim(),
      tagIds: selectedTagIds,
      tagMatchMode,
      createdWithinDays: createdWithin,
      keyword: keyword.trim() || null,
    }
    try {
      const { data } = isEdit
        ? await smartFoldersApi.update(editingFolder.id, payload)
        : await smartFoldersApi.create(payload)
      onSaved(data.data)
    } catch (err) {
      setError(err.response?.data?.message || '저장 실패')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div
      className="sf-modal-overlay"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose()
      }}
    >
      <div className="sf-modal">
        <div className="sf-modal-header">
          <h3>{isEdit ? '스마트 폴더 편집' : '새 스마트 폴더'}</h3>
          <button className="sf-modal-close" onClick={onClose}>
            ×
          </button>
        </div>

        <form className="sf-modal-body" onSubmit={handleSubmit}>
          {error && <div className="sf-error">{error}</div>}

          {/* 폴더 이름 */}
          <label className="sf-label">
            폴더 이름 <span className="sf-required">*</span>
          </label>
          <input
            className="sf-input"
            placeholder="예: 이번 주 업무"
            value={name}
            onChange={(e) => setName(e.target.value)}
            autoFocus
          />

          {/* 태그 필터 */}
          <label className="sf-label">태그 필터</label>
          {allTags.length === 0 ? (
            <p className="sf-hint">태그가 없습니다. 먼저 태그를 생성해 주세요.</p>
          ) : (
            <>
              <div className="sf-tag-grid">
                {allTags.map((tag) => (
                  <button
                    key={tag.id}
                    type="button"
                    className={`sf-tag-btn ${selectedTagIds.includes(tag.id) ? 'selected' : ''}`}
                    onClick={() => toggleTag(tag.id)}
                  >
                    #{tag.name}
                  </button>
                ))}
              </div>

              {selectedTagIds.length >= 2 && (
                <div className="sf-match-mode">
                  <span className="sf-match-label">조건:</span>
                  <label className="sf-radio">
                    <input
                      type="radio"
                      value="ANY"
                      checked={tagMatchMode === 'ANY'}
                      onChange={() => setTagMatchMode('ANY')}
                    />
                    하나라도 포함 (OR)
                  </label>
                  <label className="sf-radio">
                    <input
                      type="radio"
                      value="ALL"
                      checked={tagMatchMode === 'ALL'}
                      onChange={() => setTagMatchMode('ALL')}
                    />
                    모두 포함 (AND)
                  </label>
                </div>
              )}
            </>
          )}

          {/* 키워드 */}
          <label className="sf-label">키워드 (제목·내용 포함)</label>
          <input
            className="sf-input"
            placeholder="예: 회의록"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
          />

          {/* 기간 */}
          <label className="sf-label">생성 기간</label>
          <div className="sf-date-row">
            {DATE_OPTIONS.map((opt) => (
              <button
                key={String(opt.value)}
                type="button"
                className={`sf-date-btn ${createdWithin === opt.value ? 'selected' : ''}`}
                onClick={() => setCreatedWithin(opt.value)}
              >
                {opt.label}
              </button>
            ))}
          </div>

          {/* 미리보기 요약 */}
          <div className="sf-preview">
            <span className="sf-preview-label">필터 요약</span>
            <span className="sf-preview-text">
              {selectedTagIds.length > 0
                ? `태그 ${selectedTagIds.length}개 (${tagMatchMode === 'ALL' ? 'AND' : 'OR'})`
                : '태그 제한 없음'}
              {keyword.trim() ? ` · "${keyword.trim()}"` : ''}
              {createdWithin ? ` · 최근 ${createdWithin}일` : ''}
            </span>
          </div>

          <div className="sf-modal-footer">
            <button type="button" className="btn btn-ghost" onClick={onClose}>
              취소
            </button>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? '저장 중...' : isEdit ? '수정' : '만들기'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
