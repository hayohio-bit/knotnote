import { useState } from 'react'
import { notesApi } from '../api/notes.js'
import './CrystallizeModal.css'

/**
 * Crystallize Mode 모달
 * - 미확정(점선) 링크에 대해 사용자가 관계 요약을 직접 입력
 * - AI 자동완성 없음: 사용자가 직접 이해하고 써야 확정됨
 */
export default function CrystallizeModal({ noteId, pendingLinks, onComplete, onClose }) {
  const [summaries, setSummaries] = useState({})
  const [completed, setCompleted] = useState(new Set())
  const [loading, setLoading] = useState({})
  const [errors, setErrors] = useState({})

  const handleCrystallize = async (link) => {
    const summary = summaries[link.linkId] || ''
    if (summary.trim().length < 5) {
      setErrors(prev => ({ ...prev, [link.linkId]: '최소 5자 이상 입력해주세요' }))
      return
    }
    setErrors(prev => ({ ...prev, [link.linkId]: null }))
    setLoading(prev => ({ ...prev, [link.linkId]: true }))

    try {
      await notesApi.crystallizeLink(noteId, link.linkId, summary.trim())
      const newCompleted = new Set([...completed, link.linkId])
      setCompleted(newCompleted)

      // 모두 완료되면 500ms 후 닫기
      if (newCompleted.size >= pendingLinks.length) {
        setTimeout(() => onComplete(), 500)
      }
    } catch (e) {
      setErrors(prev => ({ ...prev, [link.linkId]: '저장에 실패했습니다. 다시 시도해주세요.' }))
    } finally {
      setLoading(prev => ({ ...prev, [link.linkId]: false }))
    }
  }

  const handleKeyDown = (e, link) => {
    if (e.key === 'Enter') handleCrystallize(link)
  }

  const completedCount = completed.size
  const totalCount = pendingLinks.length

  return (
    <div className="crystallize-backdrop" onClick={onClose}>
      <div className="crystallize-modal" onClick={e => e.stopPropagation()}>
        {/* 헤더 */}
        <div className="crystallize-header">
          <div className="crystallize-title-row">
            <span className="crystallize-icon">🔮</span>
            <h2 className="crystallize-title">매듭을 단단하게</h2>
          </div>
          <div className="crystallize-progress-badge">
            {completedCount} / {totalCount} 완료
          </div>
        </div>

        <p className="crystallize-desc">
          아래 연결들의 관계를 한 문장으로 설명해주세요.
          <br />
          <em className="crystallize-notice">AI는 이 과정을 대신하지 않습니다.</em>
        </p>

        {/* 진행 바 */}
        <div className="crystallize-progress-bar">
          <div
            className="crystallize-progress-fill"
            style={{ width: `${totalCount > 0 ? (completedCount / totalCount) * 100 : 0}%` }}
          />
        </div>

        {/* 링크 목록 */}
        <div className="crystallize-list">
          {pendingLinks.map(link => {
            const isDone = completed.has(link.linkId)
            const isLoading = loading[link.linkId]
            const error = errors[link.linkId]

            return (
              <div
                key={link.linkId}
                className={`crystallize-item ${isDone ? 'done' : 'pending'}`}
              >
                {/* 연결 쌍 */}
                <div className="link-pair">
                  <span className="link-note-name">{link.fromTitle}</span>
                  <span className="link-arrow">↔</span>
                  <span className="link-note-name">{link.toTitle}</span>
                </div>

                {/* 입력 영역 또는 완료 표시 */}
                {!isDone ? (
                  <div className="crystallize-input-row">
                    <input
                      type="text"
                      placeholder="이 둘의 관계는..."
                      value={summaries[link.linkId] || ''}
                      onChange={e =>
                        setSummaries(prev => ({ ...prev, [link.linkId]: e.target.value }))
                      }
                      onKeyDown={e => handleKeyDown(e, link)}
                      autoComplete="off"
                      className="crystallize-input"
                      disabled={isLoading}
                    />
                    <button
                      onClick={() => handleCrystallize(link)}
                      disabled={!summaries[link.linkId]?.trim() || isLoading}
                      className="crystallize-confirm-btn"
                    >
                      {isLoading ? '저장 중...' : '확정'}
                    </button>
                  </div>
                ) : (
                  <p className="crystallize-done-text">
                    ✓ {summaries[link.linkId]}
                  </p>
                )}

                {error && <p className="crystallize-error">{error}</p>}
              </div>
            )
          })}
        </div>

        {/* 닫기 */}
        <button className="crystallize-close-btn" onClick={onClose}>
          나중에 하기
        </button>
      </div>
    </div>
  )
}
