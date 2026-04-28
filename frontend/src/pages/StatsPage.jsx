import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import Navbar from '../components/Navbar.jsx'
import Spinner from '../components/Spinner.jsx'
import { statsApi, exportApi } from '../api/notes.js'
import './StatsPage.css'

function StatCard({ label, value, sub, accent }) {
  return (
    <div className={`stat-card ${accent ? 'accent' : ''}`}>
      <span className="stat-value">{value ?? '-'}</span>
      <span className="stat-label">{label}</span>
      {sub && <span className="stat-sub">{sub}</span>}
    </div>
  )
}

function ProgressBar({ value, max, color }) {
  const pct = max > 0 ? Math.round((value / max) * 100) : 0
  return (
    <div className="progress-bar-wrap">
      <div className="progress-bar-bg">
        <div className="progress-bar-fill" style={{ width: `${pct}%`, background: color }} />
      </div>
      <span className="progress-bar-pct">{pct}%</span>
    </div>
  )
}

export default function StatsPage() {
  const navigate = useNavigate()
  const [stats, setStats]       = useState(null)
  const [insights, setInsights] = useState(null)
  const [loading, setLoading]   = useState(true)
  const [exporting, setExporting] = useState(false)

  useEffect(() => {
    Promise.all([statsApi.get(), statsApi.getGraphInsights()])
      .then(([s, g]) => {
        setStats(s.data.data)
        setInsights(g.data.data)
      })
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [])

  const handleExport = async (format) => {
    setExporting(true)
    try {
      const { data, headers } = await exportApi.download(format)
      const url  = URL.createObjectURL(new Blob([data]))
      const link = document.createElement('a')
      link.href  = url
      link.download = `knotnote-export-${new Date().toISOString().slice(0,10)}.zip`
      link.click()
      URL.revokeObjectURL(url)
    } catch { alert('내보내기 실패') }
    finally { setExporting(false) }
  }

  if (loading) return <><Navbar /><Spinner /></>

  const vd = stats?.vitalityDistribution ?? {}

  return (
    <div className="stats-page">
      <Navbar />
      <main className="container stats-main">

        <div className="stats-header">
          <h1 className="stats-title">📊 통계 대시보드</h1>
          <div className="stats-export-btns">
            <button className="btn btn-ghost btn-sm" onClick={() => handleExport('json')} disabled={exporting}>
              {exporting ? '내보내는 중...' : '⬇ JSON 내보내기'}
            </button>
            <button className="btn btn-ghost btn-sm" onClick={() => handleExport('markdown')} disabled={exporting}>
              {exporting ? '...' : '⬇ Markdown 내보내기'}
            </button>
          </div>
        </div>

        {/* 핵심 지표 카드 */}
        <section className="stat-cards-grid">
          <StatCard label="전체 노트"    value={stats?.totalNotes}      sub={`최근 7일 +${stats?.recentNoteCount ?? 0}`} accent />
          <StatCard label="전체 링크"    value={stats?.totalLinks}      sub={`매듭 확정 ${stats?.crystallizedLinks ?? 0}`} />
          <StatCard label="전체 태그"    value={stats?.totalTags} />
          <StatCard label="평균 Vitality" value={stats?.avgVitalityScore != null ? stats.avgVitalityScore.toFixed(2) : '-'} />
          <StatCard label="매듭 확정률"  value={stats?.crystallizationRate != null ? `${(stats.crystallizationRate * 100).toFixed(1)}%` : '-'} />
        </section>

        {/* Vitality 분포 */}
        {vd && Object.keys(vd).length > 0 && (
          <section className="stats-section">
            <h2 className="stats-section-title">Vitality 분포</h2>
            <div className="vitality-bars">
              {[
                { key: 'veryLow',  label: '매우 낮음 (0~0.2)',  color: '#ef4444' },
                { key: 'low',      label: '낮음 (0.2~0.4)',     color: '#f59e0b' },
                { key: 'medium',   label: '보통 (0.4~0.6)',     color: '#3b82f6' },
                { key: 'high',     label: '높음 (0.6~0.8)',     color: '#10b981' },
                { key: 'veryHigh', label: '매우 높음 (0.8~1)',  color: '#6366f1' },
              ].map(({ key, label, color }) => (
                <div key={key} className="vitality-bar-row">
                  <span className="vitality-bar-label">{label}</span>
                  <ProgressBar value={vd[key] ?? 0} max={stats?.totalNotes ?? 1} color={color} />
                  <span className="vitality-bar-count">{vd[key] ?? 0}개</span>
                </div>
              ))}
            </div>
          </section>
        )}

        {/* 허브 노트 */}
        {stats?.mostConnectedNote && (
          <section className="stats-section">
            <h2 className="stats-section-title">가장 연결된 노트</h2>
            <button
              className="hub-note-card"
              onClick={() => navigate(`/notes/${stats.mostConnectedNote.id}`)}
            >
              <span className="hub-note-title">{stats.mostConnectedNote.title}</span>
              <span className="hub-note-meta">링크 {stats.mostConnectedNote.linkCount ?? 0}개</span>
            </button>
          </section>
        )}

        {/* 인기 태그 */}
        {stats?.topTags?.length > 0 && (
          <section className="stats-section">
            <h2 className="stats-section-title">인기 태그 Top 5</h2>
            <div className="top-tags-list">
              {stats.topTags.map((t, i) => (
                <div key={t.tagName} className="top-tag-item">
                  <span className="top-tag-rank">#{i + 1}</span>
                  <span className="top-tag-name">{t.tagName}</span>
                  <span className="top-tag-count">{t.noteCount}개</span>
                </div>
              ))}
            </div>
          </section>
        )}

        {/* Graph Insights */}
        {insights && (
          <section className="stats-section">
            <h2 className="stats-section-title">🗺️ Graph Insights</h2>
            <div className="insights-grid">
              <div className="insight-card">
                <span className="insight-value">{insights.clusterCount ?? '-'}</span>
                <span className="insight-label">클러스터 수</span>
              </div>
              <div className="insight-card">
                <span className="insight-value">
                  {insights.connectivityRate != null ? `${(insights.connectivityRate * 100).toFixed(1)}%` : '-'}
                </span>
                <span className="insight-label">연결률</span>
              </div>
              <div className="insight-card">
                <span className="insight-value">{insights.orphanNotes?.length ?? 0}</span>
                <span className="insight-label">고립 노트</span>
              </div>
              <div className="insight-card">
                <span className="insight-value">{insights.weakLinks?.length ?? 0}</span>
                <span className="insight-label">약한 링크</span>
              </div>
            </div>

            {/* 허브 노트 */}
            {insights.hubNotes?.length > 0 && (
              <div className="insights-sub">
                <h3 className="insights-sub-title">허브 노트 (연결 많은 순)</h3>
                <ul className="hub-list">
                  {insights.hubNotes.map(n => (
                    <li key={n.noteId} className="hub-item">
                      <button className="hub-item-title" onClick={() => navigate(`/notes/${n.noteId}`)}>
                        {n.noteTitle}
                      </button>
                      <span className="hub-item-meta">링크 {n.degree}개 · 확정 {n.crystallizedCount}개</span>
                    </li>
                  ))}
                </ul>
              </div>
            )}

            {/* 고립 노트 */}
            {insights.orphanNotes?.length > 0 && (
              <div className="insights-sub">
                <h3 className="insights-sub-title">고립 노트 (링크 없음)</h3>
                <ul className="orphan-list">
                  {insights.orphanNotes.slice(0, 5).map(n => (
                    <li key={n.noteId}>
                      <button className="orphan-title" onClick={() => navigate(`/notes/${n.noteId}`)}>
                        {n.noteTitle}
                      </button>
                    </li>
                  ))}
                  {insights.orphanNotes.length > 5 && (
                    <li className="orphan-more">+{insights.orphanNotes.length - 5}개 더</li>
                  )}
                </ul>
              </div>
            )}
          </section>
        )}

      </main>
    </div>
  )
}
