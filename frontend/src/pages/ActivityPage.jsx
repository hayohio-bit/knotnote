import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { activityApi } from '../api/notes.js'
import Navbar from '../components/Navbar.jsx'
import Spinner from '../components/Spinner.jsx'
import './ActivityPage.css'

const TYPE_LABEL = {
  NOTE_CREATED: { label: '메모 생성', icon: '📝', color: 'green' },
  NOTE_UPDATED: { label: '메모 수정', icon: '✏️', color: 'blue' },
  NOTE_DELETED: { label: '메모 삭제', icon: '🗑', color: 'red' },
  NOTE_RESTORED: { label: '버전 복원', icon: '⏪', color: 'purple' },
  NOTE_PINNED: { label: '고정', icon: '📌', color: 'orange' },
  NOTE_UNPINNED: { label: '고정 해제', icon: '📌', color: 'gray' },
  LINK_CREATED: { label: '링크 연결', icon: '🔗', color: 'teal' },
  LINK_DELETED: { label: '링크 해제', icon: '🔗', color: 'red' },
  LINK_CRYSTALLIZED: { label: '매듭 확정', icon: '🔮', color: 'purple' },
  TAG_ADDED: { label: '태그 추가', icon: '🏷', color: 'blue' },
  TAG_REMOVED: { label: '태그 제거', icon: '🏷', color: 'gray' },
}

function fmtRelative(iso) {
  const diff = Date.now() - new Date(iso).getTime()
  const min = Math.floor(diff / 60000)
  const hour = Math.floor(diff / 3600000)
  const day = Math.floor(diff / 86400000)
  if (min < 1) return '방금 전'
  if (min < 60) return `${min}분 전`
  if (hour < 24) return `${hour}시간 전`
  if (day < 7) return `${day}일 전`
  return new Date(iso).toLocaleDateString('ko-KR', { month: 'short', day: 'numeric' })
}

export default function ActivityPage() {
  const navigate = useNavigate()
  const [activities, setActivities] = useState([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState(false)
  const [limit, setLimit] = useState(30)

  const load = async (lim) => {
    setLoading(true)
    setLoadError(false)
    try {
      const { data } = await activityApi.list(lim)
      setActivities(data.data ?? [])
    } catch {
      setLoadError(true)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load(limit)
  }, [limit])

  return (
    <div className="activity-page">
      <Navbar />
      <main className="container activity-main">
        <div className="activity-header">
          <h1 className="activity-title">활동 피드</h1>
          <p className="activity-sub">노트 생성·수정·링크·태그 등 최근 활동을 확인합니다.</p>
        </div>

        {loading ? (
          <Spinner />
        ) : loadError ? (
          <div className="activity-error" role="alert">
            <p>활동을 불러오지 못했어요</p>
            <button className="btn btn-secondary btn-sm" onClick={() => load(limit)}>
              재시도
            </button>
          </div>
        ) : (
          <>
            {activities.length === 0 ? (
              <div className="activity-empty">
                <span>활동 기록이 없습니다.</span>
              </div>
            ) : (
              <ul className="activity-list">
                {activities.map((a) => {
                  const meta = TYPE_LABEL[a.type] ?? { label: a.type, icon: '•', color: 'gray' }
                  return (
                    <li key={a.id} className={`activity-item color-${meta.color}`}>
                      <span className="activity-icon">{meta.icon}</span>
                      <div className="activity-body">
                        <span className="activity-type-label">{meta.label}</span>
                        {a.noteTitle && (
                          <button
                            className="activity-note-title"
                            onClick={() => a.noteId && navigate(`/notes/${a.noteId}`)}
                          >
                            {a.noteTitle}
                          </button>
                        )}
                        {a.detail && <span className="activity-detail">{a.detail}</span>}
                      </div>
                      <span className="activity-time">{fmtRelative(a.occurredAt)}</span>
                    </li>
                  )
                })}
              </ul>
            )}

            {activities.length >= limit && (
              <div className="activity-more">
                <button className="btn btn-ghost" onClick={() => setLimit((l) => l + 30)}>
                  더 보기
                </button>
              </div>
            )}
          </>
        )}
      </main>
    </div>
  )
}
