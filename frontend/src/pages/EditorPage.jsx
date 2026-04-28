import { useState, useEffect, useCallback, useRef } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import Navbar from '../components/Navbar.jsx'
import TagBadge from '../components/TagBadge.jsx'
import Spinner from '../components/Spinner.jsx'
import SimpleEditor from '../components/SimpleEditor.jsx'
import MarkdownEditor from '../components/MarkdownEditor.jsx'
import CrystallizeModal from '../components/CrystallizeModal.jsx'
import { notesApi } from '../api/notes.js'
import { tagsApi } from '../api/tags.js'
import './EditorPage.css'

const MODE_KEY = 'knotnote-editor-mode'

function fmtDate(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  return d.toLocaleString('ko-KR', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

export default function EditorPage() {
  const { id } = useParams()
  const _navigate = useNavigate()
  const isEdit = Boolean(id)

  const [title, setTitle]                     = useState('')
  const [content, setContent]                 = useState('')
  const [noteTags, setNoteTags]               = useState([])
  const [allTags, setAllTags]                 = useState([])
  const [linkedNotes, setLinkedNotes]         = useState([])
  const [recommendations, setRecs]            = useState([])
  const [newTagName, setNewTagName]           = useState('')
  const [loading, setLoading]                 = useState(isEdit)
  const [recsLoading, setRecsLoading]         = useState(false)
  const [saving, setSaving]                   = useState(false)
  const [error, setError]                     = useState('')
  const [saved, setSaved]                     = useState(false)
  const [linkingId, setLinkingId]             = useState(null)

  // 핀
  const [isPinned, setIsPinned]               = useState(false)
  const [pinLoading, setPinLoading]           = useState(false)

  // 버전 이력
  const [versions, setVersions]               = useState([])
  const [versionsLoading, setVersionsLoading] = useState(false)
  const [showVersions, setShowVersions]       = useState(false)
  const [restoringId, setRestoringId]         = useState(null)

  // AI 태그 추천
  const [suggestedTags, setSuggestedTags]     = useState([])
  const [suggestLoading, setSuggestLoading]   = useState(false)

  // Crystallize Mode
  const [pendingLinks, setPendingLinks]       = useState([])
  const [showCrystallize, setShowCrystallize] = useState(false)

  // 미저장 감지
  const [isDirty, setIsDirty]       = useState(false)
  const [showUnsaved, setShowUnsaved] = useState(false)
  const savedTitle   = useRef('')
  const savedContent = useRef('')
  const pendingNav   = useRef(null)

  // 자동저장
  const [autoSaveStatus, setAutoSaveStatus] = useState('')
  const autoSaveTimer = useRef(null)

  // 에디터 모드
  const [editorMode, setEditorMode] = useState(
    () => localStorage.getItem(MODE_KEY) || 'simple'
  )
  const toggleMode = () => {
    const next = editorMode === 'simple' ? 'markdown' : 'simple'
    setEditorMode(next)
    localStorage.setItem(MODE_KEY, next)
  }

  const safeNavigate = useCallback((to, options) => {
    if (!isDirty) { _navigate(to, options); return }
    pendingNav.current = { to, options }
    setShowUnsaved(true)
  }, [isDirty, _navigate])

  // 메모 로드
  useEffect(() => {
    if (!isEdit) return
    ;(async () => {
      try {
        const { data } = await notesApi.get(id)
        const note = data.data
        setTitle(note.title)
        setContent(note.content ?? '')
        setNoteTags(note.tags ?? [])
        setLinkedNotes(note.linkedNotes ?? [])
        setIsPinned(note.isPinned ?? false)
        savedTitle.current   = note.title
        savedContent.current = note.content ?? ''
      } catch { setError('메모를 불러오지 못했습니다.') }
      finally  { setLoading(false) }
    })()
  }, [id, isEdit])

  // dirty 감지
  useEffect(() => {
    if (loading) return
    const dirty = isEdit
      ? (title !== savedTitle.current || content !== savedContent.current)
      : (title.trim() !== '' || content.trim() !== '')
    setIsDirty(dirty)
  }, [title, content, loading, isEdit])

  // 뒤로가기 차단
  useEffect(() => {
    if (!isDirty) return
    window.history.pushState(null, '', window.location.href)
    const handlePop = () => {
      window.history.pushState(null, '', window.location.href)
      pendingNav.current = { to: '/dashboard' }
      setShowUnsaved(true)
    }
    window.addEventListener('popstate', handlePop)
    return () => window.removeEventListener('popstate', handlePop)
  }, [isDirty])

  useEffect(() => {
    const h = (e) => { if (!isDirty) return; e.preventDefault(); e.returnValue = '' }
    window.addEventListener('beforeunload', h)
    return () => window.removeEventListener('beforeunload', h)
  }, [isDirty])

  // 자동저장 1.5초 debounce
  useEffect(() => {
    if (!isEdit || !isDirty) return
    if (autoSaveTimer.current) clearTimeout(autoSaveTimer.current)
    autoSaveTimer.current = setTimeout(async () => {
      if (!title.trim()) return
      setAutoSaveStatus('saving')
      try {
        await notesApi.update(id, title, content)
        savedTitle.current   = title
        savedContent.current = content
        setIsDirty(false)
        setAutoSaveStatus('saved')
        setTimeout(() => setAutoSaveStatus(''), 2000)
      } catch { setAutoSaveStatus('') }
    }, 1500)
    return () => clearTimeout(autoSaveTimer.current)
  }, [title, content, isEdit, isDirty, id])

  // 미확정 링크 로드
  const loadPendingLinks = useCallback(async () => {
    if (!isEdit || !id) return
    try {
      const { data } = await notesApi.getPendingLinks(id)
      setPendingLinks(data.data ?? [])
    } catch {}
  }, [id, isEdit])

  // 추천 로드
  const loadRecommendations = useCallback(async () => {
    if (!isEdit || !id) return
    setRecsLoading(true)
    try {
      const { data } = await notesApi.getRecommendations(id, 5)
      setRecs(data.data ?? [])
    } catch {}
    finally { setRecsLoading(false) }
  }, [id, isEdit])

  useEffect(() => {
    loadRecommendations()
    loadPendingLinks()
    tagsApi.list().then(({ data }) => setAllTags(data.data)).catch(() => {})
  }, [loadRecommendations, loadPendingLinks])

  // 버전 이력
  const loadVersions = useCallback(async () => {
    if (!isEdit || !id) return
    setVersionsLoading(true)
    try {
      const { data } = await notesApi.getVersions(id)
      setVersions(data.data ?? [])
    } catch {}
    finally { setVersionsLoading(false) }
  }, [id, isEdit])

  const handleToggleVersions = () => {
    if (!showVersions && versions.length === 0) loadVersions()
    setShowVersions(v => !v)
  }

  const handleRestoreVersion = async (versionId) => {
    if (!window.confirm('이 버전으로 복원할까요? 현재 내용은 새 버전으로 저장됩니다.')) return
    setRestoringId(versionId)
    try {
      const { data } = await notesApi.restoreVersion(id, versionId)
      const note = data.data
      setTitle(note.title)
      setContent(note.content ?? '')
      savedTitle.current   = note.title
      savedContent.current = note.content ?? ''
      setIsDirty(false)
      setVersions([])
      setShowVersions(false)
    } catch { alert('복원 실패') }
    finally { setRestoringId(null) }
  }

  // AI 태그 추천
  const handleSuggestTags = async () => {
    setSuggestLoading(true)
    setSuggestedTags([])
    try {
      const { data } = await notesApi.suggestTags(id, 5)
      setSuggestedTags(data.data ?? [])
    } catch {}
    finally { setSuggestLoading(false) }
  }

  // 핀 토글
  const handleTogglePin = async () => {
    if (pinLoading) return
    setPinLoading(true)
    try {
      if (isPinned) { await notesApi.unpin(id); setIsPinned(false) }
      else          { await notesApi.pin(id);   setIsPinned(true)  }
    } catch { alert('핀 변경 실패') }
    finally { setPinLoading(false) }
  }

  // 수동 저장
  const handleSave = async () => {
    if (!title.trim()) { setError('제목을 입력해주세요.'); return }
    setSaving(true); setError('')
    if (autoSaveTimer.current) clearTimeout(autoSaveTimer.current)
    try {
      if (isEdit) {
        await notesApi.update(id, title, content)
        savedTitle.current   = title
        savedContent.current = content
        setIsDirty(false)
        setSaved(true); setAutoSaveStatus('')
        setTimeout(() => setSaved(false), 2000)
        loadRecommendations(); loadPendingLinks()
      } else {
        const { data } = await notesApi.create(title, content)
        setIsDirty(false)
        _navigate(`/notes/${data.data.id}`, { replace: true })
      }
    } catch (err) { setError(err.response?.data?.message || '저장 중 오류') }
    finally { setSaving(false) }
  }

  const handleUnsavedSave = async () => {
    if (!title.trim()) { setError('제목을 입력해주세요.'); setShowUnsaved(false); return }
    setSaving(true); setError('')
    try {
      if (isEdit) await notesApi.update(id, title, content)
      else        await notesApi.create(title, content)
      setIsDirty(false); setShowUnsaved(false); proceedNavigation()
    } catch (err) { setError(err.response?.data?.message || '저장 오류'); setShowUnsaved(false) }
    finally { setSaving(false) }
  }

  const handleUnsavedDiscard = () => { setIsDirty(false); setShowUnsaved(false); proceedNavigation() }

  const proceedNavigation = () => {
    const nav = pendingNav.current; pendingNav.current = null
    if (!nav) return
    if (nav.delta !== undefined) _navigate(nav.delta)
    else _navigate(nav.to, nav.options)
  }

  // 태그
  const handleAddTag = async (tagId) => {
    if (noteTags.some(t => t.id === tagId)) return
    try {
      await notesApi.addTag(id, tagId)
      const tag = allTags.find(t => t.id === tagId)
      if (tag) setNoteTags(prev => [...prev, tag])
      setSuggestedTags(prev => prev.filter(s => s.tagId !== tagId))
    } catch (err) { alert(err.response?.data?.message || '태그 연결 실패') }
  }

  const handleRemoveTag = async (tagId) => {
    try {
      await notesApi.removeTag(id, tagId)
      setNoteTags(prev => prev.filter(t => t.id !== tagId))
    } catch { alert('태그 해제 실패') }
  }

  const handleCreateTag = async () => {
    const name = newTagName.trim(); if (!name) return
    try {
      const { data } = await tagsApi.create(name)
      const tag = data.data
      setAllTags(prev => [...prev, tag]); setNewTagName('')
      if (isEdit) handleAddTag(tag.id)
    } catch (err) { alert(err.response?.data?.message || '태그 생성 실패') }
  }

  const handleUnlink = async (targetId) => {
    try {
      await notesApi.removeLink(id, targetId)
      setLinkedNotes(prev => prev.filter(n => n.id !== targetId))
      loadRecommendations(); loadPendingLinks()
    } catch { alert('링크 해제 실패') }
  }

  const handleLinkRecommendation = async (recId) => {
    setLinkingId(recId)
    try {
      await notesApi.addLink(id, recId)
      const rec = recommendations.find(r => r.id === recId)
      if (rec) setLinkedNotes(prev => [...prev, { id: rec.id, title: rec.title, tags: rec.tags }])
      setRecs(prev => prev.filter(r => r.id !== recId)); loadPendingLinks()
    } catch (err) { alert(err.response?.data?.message || '연결 실패') }
    finally { setLinkingId(null) }
  }

  const handleDelete = async () => {
    if (!window.confirm('이 메모를 삭제할까요?')) return
    try { await notesApi.delete(id); setIsDirty(false); _navigate('/dashboard') }
    catch { alert('삭제 실패') }
  }

  if (loading) return <><Navbar /><Spinner /></>

  return (
    <div className="editor-page">
      <Navbar onNavigate={safeNavigate} />

      <main className="container editor-main">
        {/* 툴바 */}
        <div className="editor-toolbar">
          <button className="btn btn-ghost" onClick={() => safeNavigate('/dashboard')}>← 뒤로</button>
          <div className="toolbar-right">
            {autoSaveStatus === 'saving' && <span className="save-indicator autosave">⟳ 저장 중...</span>}
            {autoSaveStatus === 'saved'  && <span className="save-indicator">✓ 자동저장됨</span>}
            {saved && !autoSaveStatus    && <span className="save-indicator">✓ 저장됨</span>}

            {isEdit && pendingLinks.length > 0 && (
              <button className="btn btn-crystallize" onClick={() => setShowCrystallize(true)} title="미확정 연결 확정">
                🔮 매듭 확정 <span className="crystallize-badge">{pendingLinks.length}</span>
              </button>
            )}

            {isEdit && (
              <button
                className={`btn btn-ghost btn-sm pin-btn ${isPinned ? 'pinned' : ''}`}
                onClick={handleTogglePin} disabled={pinLoading}
                title={isPinned ? '고정 해제' : '상단 고정'}
              >
                {isPinned ? '📌 고정됨' : '📌 고정'}
              </button>
            )}

            <div className="editor-mode-toggle">
              <button type="button" className={`mode-btn ${editorMode === 'simple' ? 'active' : ''}`}
                onClick={() => editorMode !== 'simple' && toggleMode()}>서식</button>
              <button type="button" className={`mode-btn ${editorMode === 'markdown' ? 'active' : ''}`}
                onClick={() => editorMode !== 'markdown' && toggleMode()}>MD</button>
            </div>

            {isEdit && (
              <button className="btn btn-ghost btn-sm" onClick={() => safeNavigate('/graph')} title="지식 그래프">🗺️</button>
            )}
            {isEdit && <button className="btn btn-danger" onClick={handleDelete}>삭제</button>}
            <button className="btn btn-primary" onClick={handleSave} disabled={saving}>
              {saving ? '저장 중...' : isEdit ? '저장' : '작성 완료'}
            </button>
          </div>
        </div>

        {error && <div className="editor-error">{error}</div>}

        <div className="editor-layout">
          {/* 편집 영역 */}
          <div className="editor-area">
            {isPinned && <div className="pin-indicator-bar">📌 고정된 메모</div>}
            <input
              className="editor-title-input"
              placeholder="제목을 입력하세요"
              value={title}
              onChange={e => setTitle(e.target.value)}
            />
            {editorMode === 'simple'
              ? <SimpleEditor value={content} onChange={setContent} />
              : <MarkdownEditor value={content} onChange={setContent} />
            }
          </div>

          {/* 사이드바 */}
          {isEdit && (
            <aside className="editor-sidebar">

              {/* 태그 */}
              <div className="sidebar-section">
                <div className="sidebar-title-row">
                  <h3 className="sidebar-title">태그</h3>
                  <button className="sidebar-action-btn" onClick={handleSuggestTags} disabled={suggestLoading}>
                    {suggestLoading ? '...' : '✨ AI 추천'}
                  </button>
                </div>
                <div className="tag-list">
                  {noteTags.map(tag => (
                    <TagBadge key={tag.id} name={tag.name} onRemove={() => handleRemoveTag(tag.id)} />
                  ))}
                </div>
                {suggestedTags.length > 0 && (
                  <div className="suggested-tags">
                    <p className="suggested-tags-label">AI 추천:</p>
                    {suggestedTags.map(s => (
                      <button key={s.tagId} className="suggested-tag-btn"
                        onClick={() => handleAddTag(s.tagId)}
                        title={`신뢰도 ${Math.round(s.confidence * 100)}%`}>
                        + #{s.tagName}
                        <span className="tag-confidence">{Math.round(s.confidence * 100)}%</span>
                      </button>
                    ))}
                  </div>
                )}
                <div className="tag-add-row">
                  <input className="input input-sm" placeholder="새 태그"
                    value={newTagName} onChange={e => setNewTagName(e.target.value)}
                    onKeyDown={e => e.key === 'Enter' && handleCreateTag()} />
                  <button className="btn btn-ghost btn-sm" onClick={handleCreateTag}>추가</button>
                </div>
                {allTags.filter(t => !noteTags.some(nt => nt.id === t.id)).length > 0 && (
                  <div className="tag-pick-list">
                    {allTags.filter(t => !noteTags.some(nt => nt.id === t.id)).map(tag => (
                      <button key={tag.id} className="tag-pick-btn" onClick={() => handleAddTag(tag.id)}>
                        + #{tag.name}
                      </button>
                    ))}
                  </div>
                )}
              </div>

              {/* 버전 이력 */}
              <div className="sidebar-section">
                <div className="sidebar-title-row">
                  <h3 className="sidebar-title">
                    버전 이력
                    {versions.length > 0 && <span className="sidebar-count">{versions.length}</span>}
                  </h3>
                  <button className="sidebar-action-btn" onClick={handleToggleVersions}>
                    {showVersions ? '접기' : '보기'}
                  </button>
                </div>
                {showVersions && (
                  versionsLoading ? <p className="sidebar-empty">로드 중...</p>
                  : versions.length === 0 ? <p className="sidebar-empty">저장된 버전이 없습니다.</p>
                  : (
                    <ul className="version-list">
                      {versions.map(v => (
                        <li key={v.versionId} className="version-item">
                          <div className="version-info">
                            <span className="version-num">v{v.versionNumber}</span>
                            <span className="version-title-text">{v.title}</span>
                            <span className="version-date">{fmtDate(v.savedAt)}</span>
                          </div>
                          <button className="btn btn-ghost btn-xs"
                            onClick={() => handleRestoreVersion(v.versionId)}
                            disabled={restoringId === v.versionId}>
                            {restoringId === v.versionId ? '...' : '복원'}
                          </button>
                        </li>
                      ))}
                    </ul>
                  )
                )}
              </div>

              {/* 연결된 메모 */}
              <div className="sidebar-section">
                <h3 className="sidebar-title">
                  연결된 메모 <span className="sidebar-count">{linkedNotes.length}</span>
                  {pendingLinks.length > 0 && (
                    <span className="sidebar-count pending-count">점선 {pendingLinks.length}</span>
                  )}
                </h3>
                {linkedNotes.length === 0
                  ? <p className="sidebar-empty">연결된 메모가 없습니다.</p>
                  : (
                    <ul className="linked-notes-list">
                      {linkedNotes.map(note => {
                        const isPending = pendingLinks.some(l => l.fromNoteId === note.id || l.toNoteId === note.id)
                        return (
                          <li key={note.id} className="linked-note-item">
                            <button className="linked-note-title"
                              onClick={() => safeNavigate(`/notes/${note.id}`)}>
                              {isPending ? '⋯' : '🔗'} {note.title}
                            </button>
                            <button className="linked-note-remove"
                              onClick={() => handleUnlink(note.id)} title="링크 해제">×</button>
                          </li>
                        )
                      })}
                    </ul>
                  )
                }
              </div>

              {/* 스마트 추천 */}
              <div className="sidebar-section">
                <h3 className="sidebar-title">
                  💡 연결 추천
                  {recsLoading && <span className="recs-loading"> 로드 중…</span>}
                </h3>
                {!recsLoading && recommendations.length === 0 && (
                  <p className="sidebar-empty">추천할 메모가 없습니다.</p>
                )}
                {recommendations.length > 0 && (
                  <ul className="rec-list">
                    {recommendations.map(rec => (
                      <li key={rec.id} className="rec-item">
                        <div className="rec-info">
                          <button className="rec-title" onClick={() => safeNavigate(`/notes/${rec.id}`)}>
                            {rec.title}
                          </button>
                          {rec.scoreReason && <span className="rec-reason">{rec.scoreReason}</span>}
                          {rec.tags?.length > 0 && (
                            <div className="rec-tags">
                              {rec.tags.slice(0, 3).map(t => (
                                <span key={t.id} className="rec-tag">#{t.name}</span>
                              ))}
                            </div>
                          )}
                        </div>
                        <button className="btn btn-ghost btn-sm rec-link-btn"
                          onClick={() => handleLinkRecommendation(rec.id)}
                          disabled={linkingId === rec.id}>
                          {linkingId === rec.id ? '…' : '+ 연결'}
                        </button>
                      </li>
                    ))}
                  </ul>
                )}
              </div>

            </aside>
          )}
        </div>
      </main>

      {showCrystallize && pendingLinks.length > 0 && (
        <CrystallizeModal
          noteId={Number(id)}
          pendingLinks={pendingLinks}
          onComplete={() => { setShowCrystallize(false); loadPendingLinks(); loadRecommendations() }}
          onClose={() => setShowCrystallize(false)}
        />
      )}

      {showUnsaved && (
        <div className="unsaved-backdrop" onClick={() => setShowUnsaved(false)}>
          <div className="unsaved-modal" onClick={e => e.stopPropagation()}>
            <div className="unsaved-icon">📝</div>
            <h2 className="unsaved-title">저장하지 않은 내용이 있어요</h2>
            <p className="unsaved-desc">작성 중인 내용이 있습니다.<br />저장하고 이동하시겠어요?</p>
            <div className="unsaved-actions">
              <button className="btn btn-primary unsaved-btn-save" onClick={handleUnsavedSave} disabled={saving}>
                {saving ? '저장 중...' : '저장하고 나가기'}
              </button>
              <button className="btn btn-danger unsaved-btn-discard" onClick={handleUnsavedDiscard}>저장 없이 나가기</button>
              <button className="btn btn-ghost unsaved-btn-cancel"
                onClick={() => { setShowUnsaved(false); pendingNav.current = null }}>계속 작성하기</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
