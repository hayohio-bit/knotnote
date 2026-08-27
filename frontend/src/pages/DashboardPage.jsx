import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { exportApi, notesApi } from '../api/notes.js'
import { searchApi } from '../api/search.js'
import { smartFoldersApi } from '../api/smartFolders.js'
import { tagsApi } from '../api/tags.js'
import Navbar from '../components/Navbar.jsx'
import NoteCard from '../components/NoteCard.jsx'
import SmartFolderModal from '../components/SmartFolderModal.jsx'
import Spinner from '../components/Spinner.jsx'
import TemplateModal from '../components/TemplateModal.jsx'
import { toast } from '../lib/toast.js'
import './DashboardPage.css'

const VIEW_KEY = 'knotnote-view-mode'
const SORT_KEY = 'knotnote-sort'

const SORT_OPTIONS = [
  { value: 'newest', label: '최신순' },
  { value: 'oldest', label: '오래된순' },
  { value: 'title-asc', label: '제목 가나다순' },
  { value: 'title-desc', label: '제목 역순' },
]

const VIEW_OPTIONS = [
  { value: 'card', label: '카드' },
  { value: 'list', label: '리스트' },
  { value: 'feed', label: '피드' },
]

function sortNotes(notes, sort) {
  const arr = [...notes]
  switch (sort) {
    case 'oldest':
      return arr.sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt))
    case 'title-asc':
      return arr.sort((a, b) => a.title.localeCompare(b.title, 'ko'))
    case 'title-desc':
      return arr.sort((a, b) => b.title.localeCompare(a.title, 'ko'))
    default:
      return arr.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
  }
}

export default function DashboardPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()

  // 기본 상태
  const [notes, setNotes] = useState([])
  const [tags, setTags] = useState([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  // 검색
  const [query, setQuery] = useState('')
  const [debouncedQ, setDebouncedQ] = useState('')
  const debounceTimer = useRef(null)

  // 정렬·보기
  const [sort, setSort] = useState(() => localStorage.getItem(SORT_KEY) || 'newest')
  const [viewMode, setViewMode] = useState(() => localStorage.getItem(VIEW_KEY) || 'card')

  // 다중 태그 필터
  const [selectedTagIds, setSelectedTagIds] = useState(() => {
    const t = searchParams.get('tagId')
    return t ? [Number(t)] : []
  })
  const [tagMatchMode, setTagMatchMode] = useState('ANY')

  // 고정 메모 (사이드바, 전체 기준)
  const [pinnedNotes, setPinnedNotes] = useState([])

  // 스마트 폴더
  const [smartFolders, setSmartFolders] = useState([])
  const [activeSF, setActiveSF] = useState(null)
  const [sfLoading, setSfLoading] = useState(false)
  const [sfNotes, setSfNotes] = useState([])
  const [showSFModal, setShowSFModal] = useState(false)
  const [editingSF, setEditingSF] = useState(null)

  // ── 벌크 선택 (Phase 6-F) ──
  const [bulkMode, setBulkMode] = useState(false)
  const [selectedIds, setSelectedIds] = useState(new Set())
  const [bulkLoading, setBulkLoading] = useState(false)
  const [exporting, setExporting] = useState(false)
  const [showTagPicker, setShowTagPicker] = useState(false)
  const tagPickerRef = useRef(null)
  const tagPickerBtnRef = useRef(null)

  // ── 템플릿 모달 (Phase 7-C) ──
  const [showTemplateModal, setShowTemplateModal] = useState(false)

  // ── 웹 클리핑 모달 (Phase 9-F) ──
  const [showClipModal, setShowClipModal] = useState(false)
  const [clipUrl, setClipUrl] = useState('')
  const [clipLoading, setClipLoading] = useState(false)
  const [clipError, setClipError] = useState('')

  const isSearchMode = debouncedQ.trim().length > 0
  const isSFMode = Boolean(activeSF)

  // 검색 debounce
  const handleQueryChange = (val) => {
    setQuery(val)
    if (debounceTimer.current) clearTimeout(debounceTimer.current)
    debounceTimer.current = setTimeout(() => {
      setDebouncedQ(val)
      setPage(0)
    }, 300)
  }

  // 태그 토글
  const toggleTag = (tagId) => {
    setActiveSF(null)
    setSelectedTagIds((prev) =>
      prev.includes(tagId) ? prev.filter((id) => id !== tagId) : [...prev, tagId],
    )
    setPage(0)
  }

  const handleSort = (val) => {
    setSort(val)
    localStorage.setItem(SORT_KEY, val)
  }
  const handleView = (val) => {
    setViewMode(val)
    localStorage.setItem(VIEW_KEY, val)
  }

  // 스마트 폴더 로드
  const loadSmartFolders = useCallback(() => {
    smartFoldersApi
      .list()
      .then(({ data }) => setSmartFolders(data.data))
      .catch(() => {})
  }, [])
  useEffect(() => {
    loadSmartFolders()
  }, [loadSmartFolders])

  const loadSFNotes = async (sf) => {
    setSfLoading(true)
    try {
      const { data } = await smartFoldersApi.getNotes(sf.id)
      setSfNotes(data.data ?? [])
    } catch {
      setSfNotes([])
    } finally {
      setSfLoading(false)
    }
  }

  const selectSmartFolder = async (sf) => {
    if (activeSF?.id === sf.id) {
      setActiveSF(null)
      return
    }
    setActiveSF(sf)
    setSelectedTagIds([])
    setQuery('')
    setDebouncedQ('')
    await loadSFNotes(sf)
  }

  // 노트 로드
  const fetchNotes = useCallback(async () => {
    if (isSFMode) return
    setLoading(true)
    setError('')
    try {
      if (isSearchMode) {
        const { data } = await searchApi.search(debouncedQ, page)
        setNotes(data.data.content ?? [])
        setTotalPages(data.data.totalPages ?? 0)
      } else {
        // 태그 필터는 서버에서 처리한다 (페이지네이션과 어긋나지 않도록)
        const { data } = await notesApi.list(page, 20, selectedTagIds, tagMatchMode)
        setNotes(data.data.content ?? [])
        setTotalPages(data.data.totalPages ?? 0)
      }
    } catch {
      setError('메모를 불러오지 못했습니다.')
    } finally {
      setLoading(false)
    }
  }, [page, debouncedQ, isSearchMode, isSFMode, selectedTagIds, tagMatchMode])

  const loadTags = useCallback(() => {
    tagsApi
      .list()
      .then(({ data }) => setTags(data.data))
      .catch(() => {})
  }, [])

  const loadPinned = useCallback(() => {
    notesApi
      .listPinned()
      .then(({ data }) => setPinnedNotes(data.data ?? []))
      .catch(() => {})
  }, [])

  useEffect(() => {
    fetchNotes()
  }, [fetchNotes])
  useEffect(() => {
    loadTags()
    loadPinned()
  }, [loadTags, loadPinned])

  // 빠른 캡처 등 외부에서 노트가 생성되면 목록을 갱신한다
  useEffect(() => {
    const onNotesChanged = () => {
      fetchNotes()
      loadPinned()
    }
    window.addEventListener('knotnote:notes-changed', onNotesChanged)
    return () => window.removeEventListener('knotnote:notes-changed', onNotesChanged)
  }, [fetchNotes, loadPinned])

  useEffect(() => {
    const t = searchParams.get('tagId')
    if (t) {
      setSelectedTagIds([Number(t)])
      setActiveSF(null)
    }
  }, [searchParams])

  // 현재 표시 노트 (일반 목록의 태그 필터는 서버에서 처리되므로,
  // 클라이언트 필터는 검색 결과에만 적용한다)
  const sourceNotes = isSFMode ? sfNotes : notes
  const tagFilteredNotes =
    isSearchMode && selectedTagIds.length > 0
      ? sourceNotes.filter((n) => {
          const ids = (n.tags ?? []).map((t) => t.id)
          return tagMatchMode === 'ALL'
            ? selectedTagIds.every((id) => ids.includes(id))
            : selectedTagIds.some((id) => ids.includes(id))
        })
      : sourceNotes

  // 핀 노트 상단 정렬
  const sorted = sortNotes(tagFilteredNotes, sort)
  const displayNotes = [...sorted.filter((n) => n.isPinned), ...sorted.filter((n) => !n.isPinned)]

  const handleSFSaved = (savedSF) => {
    setShowSFModal(false)
    setEditingSF(null)
    loadSmartFolders()
    // 활성 폴더를 편집한 경우: 토글 해제가 아니라 새 조건으로 재조회한다
    if (activeSF?.id === savedSF.id) {
      setActiveSF(savedSF)
      loadSFNotes(savedSF)
    }
  }

  const handleSFDelete = async (sfId) => {
    if (!window.confirm('이 스마트 폴더를 삭제할까요?')) return
    try {
      await smartFoldersApi.delete(sfId)
    } catch {
      toast.error('폴더 삭제에 실패했어요')
      return
    }
    if (activeSF?.id === sfId) setActiveSF(null)
    loadSmartFolders()
  }

  // ── 벌크 선택 토글 ───────────────────────────────────────
  const toggleSelectNote = (noteId) => {
    setSelectedIds((prev) => {
      const next = new Set(prev)
      next.has(noteId) ? next.delete(noteId) : next.add(noteId)
      return next
    })
  }

  const toggleBulkMode = () => {
    setBulkMode((m) => !m)
    setSelectedIds(new Set())
    setShowTagPicker(false)
  }

  const selectAll = () => setSelectedIds(new Set(displayNotes.map((n) => n.id)))
  const clearSelect = () => setSelectedIds(new Set())

  // 벌크 삭제
  const handleBulkDelete = async () => {
    if (selectedIds.size === 0) return
    if (!window.confirm(`선택한 ${selectedIds.size}개 메모를 삭제할까요?`)) return
    setBulkLoading(true)
    try {
      await notesApi.bulkDelete([...selectedIds])
      setSelectedIds(new Set())
      setBulkMode(false)
      fetchNotes()
    } catch {
      toast.error('선택한 메모 삭제에 실패했어요')
    } finally {
      setBulkLoading(false)
    }
  }

  // 벌크 태그 부착: 태그 선택 팝오버에서 칩을 클릭하면 실행된다
  const handleBulkTag = async (tag) => {
    if (selectedIds.size === 0) return
    setBulkLoading(true)
    try {
      const count = selectedIds.size
      await notesApi.bulkAddTag([...selectedIds], tag.id)
      toast.success(`${count}개 메모에 #${tag.name} 태그를 부착했어요`)
      setShowTagPicker(false)
      setSelectedIds(new Set())
      setBulkMode(false)
      fetchNotes()
      loadTags()
    } catch {
      toast.error('태그 부착에 실패했어요')
    } finally {
      setBulkLoading(false)
    }
  }

  // 태그 선택 팝오버: 바깥 클릭으로 닫는다
  useEffect(() => {
    if (!showTagPicker) return
    const onPointerDown = (e) => {
      if (tagPickerRef.current?.contains(e.target)) return
      if (tagPickerBtnRef.current?.contains(e.target)) return
      setShowTagPicker(false)
    }
    document.addEventListener('mousedown', onPointerDown)
    return () => document.removeEventListener('mousedown', onPointerDown)
  }, [showTagPicker])

  // 웹 클리핑
  const handleClip = async () => {
    const trimmed = clipUrl.trim()
    if (!trimmed) return
    if (!/^https?:\/\//i.test(trimmed)) {
      setClipError('http:// 또는 https://로 시작하는 URL을 입력하세요.')
      return
    }
    setClipLoading(true)
    setClipError('')
    try {
      const { data } = await notesApi.clip(trimmed)
      setShowClipModal(false)
      setClipUrl('')
      navigate(`/notes/${data.data.id}`)
    } catch (e) {
      const msg = e?.response?.data?.message
      setClipError(msg || '클리핑에 실패했습니다. URL을 확인하거나 잠시 후 다시 시도하세요.')
    } finally {
      setClipLoading(false)
    }
  }

  // 내보내기
  const handleExport = async (format) => {
    setExporting(true)
    try {
      const { data } = await exportApi.download(format)
      const url = URL.createObjectURL(new Blob([data]))
      const link = document.createElement('a')
      link.href = url
      link.download = `knotnote-${new Date().toISOString().slice(0, 10)}.zip`
      link.click()
      URL.revokeObjectURL(url)
    } catch {
      toast.error('내보내기에 실패했어요')
    } finally {
      setExporting(false)
    }
  }

  return (
    <div className="dashboard-page">
      <Navbar />
      <div className="dashboard-body">
        {/* 스마트 폴더 사이드바 */}
        <aside className="sf-sidebar">
          <div className="sf-sidebar-header">
            <span className="sf-sidebar-title">📁 스마트 폴더</span>
            <button
              className="sf-add-btn"
              onClick={() => {
                setEditingSF(null)
                setShowSFModal(true)
              }}
              title="새 스마트 폴더"
            >
              +
            </button>
          </div>

          {smartFolders.length === 0 ? (
            <p className="sf-sidebar-empty">
              저장된 필터가 없습니다.
              <br />
              <button
                className="sf-create-link"
                onClick={() => {
                  setEditingSF(null)
                  setShowSFModal(true)
                }}
              >
                + 첫 스마트 폴더 만들기
              </button>
            </p>
          ) : (
            <ul className="sf-list">
              {smartFolders.map((sf) => (
                <li key={sf.id} className={`sf-item ${activeSF?.id === sf.id ? 'active' : ''}`}>
                  <button className="sf-name" onClick={() => selectSmartFolder(sf)}>
                    🗂 {sf.name}
                  </button>
                  <div className="sf-actions">
                    <button
                      className="sf-action-btn"
                      onClick={() => {
                        setEditingSF(sf)
                        setShowSFModal(true)
                      }}
                      title="편집"
                    >
                      ✎
                    </button>
                    <button
                      className="sf-action-btn sf-action-delete"
                      onClick={() => handleSFDelete(sf.id)}
                      title="삭제"
                    >
                      ×
                    </button>
                  </div>
                </li>
              ))}
            </ul>
          )}

          {/* 태그 목록 */}
          {tags.length > 0 && (
            <div className="sf-tags-section">
              <div className="sf-tags-header">
                <span className="sf-tags-title">🏷 태그 필터</span>
                {selectedTagIds.length >= 2 && (
                  <button
                    className="tag-mode-toggle"
                    onClick={() => setTagMatchMode((m) => (m === 'ANY' ? 'ALL' : 'ANY'))}
                    title="AND/OR 전환"
                  >
                    {tagMatchMode}
                  </button>
                )}
              </div>
              <div className="sf-tags-list">
                {tags.map((tag) => (
                  <button
                    key={tag.id}
                    className={`sf-tag-btn ${selectedTagIds.includes(tag.id) ? 'active' : ''}`}
                    onClick={() => toggleTag(tag.id)}
                  >
                    #{tag.name}
                    {tag.noteCount != null && <span className="sf-tag-count">{tag.noteCount}</span>}
                  </button>
                ))}
              </div>
            </div>
          )}

          {/* 고정 메모 */}
          {pinnedNotes.length > 0 && (
            <div className="sf-pinned-section">
              <span className="sf-tags-title">📌 고정 메모</span>
              <ul className="sf-pinned-list">
                {pinnedNotes.map((n) => (
                  <li key={n.id}>
                    <Link to={`/notes/${n.id}`} className="sf-pinned-link">
                      {n.title}
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </aside>

        {/* 메인 */}
        <main className="dashboard-main">
          {/* 툴바: 검색 + 정렬 + 보기 + 액션 한 줄 */}
          <div className="dashboard-toolbar">
            <div className="search-wrap">
              <input
                className="search-input"
                placeholder="메모 검색..."
                value={query}
                onChange={(e) => handleQueryChange(e.target.value)}
              />
            </div>

            <select className="select-sm" value={sort} onChange={(e) => handleSort(e.target.value)}>
              {SORT_OPTIONS.map((o) => (
                <option key={o.value} value={o.value}>
                  {o.label}
                </option>
              ))}
            </select>

            <div className="view-toggle">
              {VIEW_OPTIONS.map((o) => (
                <button
                  key={o.value}
                  className={`view-btn ${viewMode === o.value ? 'active' : ''}`}
                  onClick={() => handleView(o.value)}
                >
                  {o.label}
                </button>
              ))}
            </div>

            <div className="toolbar-actions">
              <button
                className="btn btn-ghost btn-sm"
                onClick={() => handleExport('markdown')}
                disabled={exporting}
                title="Markdown ZIP 내보내기"
              >
                {exporting ? '내보내는 중...' : '내보내기'}
              </button>

              <button
                className={`btn btn-ghost btn-sm ${bulkMode ? 'active' : ''}`}
                onClick={toggleBulkMode}
                title="다중 선택"
              >
                선택
              </button>

              <button
                className="btn btn-secondary btn-sm"
                onClick={() => {
                  setShowClipModal(true)
                  setClipUrl('')
                  setClipError('')
                }}
                title="URL에서 노트 생성"
              >
                웹 클리핑
              </button>

              <button
                className="btn btn-secondary btn-sm"
                onClick={() => setShowTemplateModal(true)}
                title="템플릿으로 새 메모"
              >
                템플릿
              </button>
              <Link to="/notes/new" className="btn btn-primary btn-sm">
                + 새 메모
              </Link>
            </div>
          </div>

          {/* 벌크 액션 바 */}
          {bulkMode && (
            <>
              <div className="bulk-action-bar">
                <span className="bulk-count">{selectedIds.size}개 선택됨</span>
                <button className="btn btn-ghost btn-sm" onClick={selectAll}>
                  전체 선택
                </button>
                <button className="btn btn-ghost btn-sm" onClick={clearSelect}>
                  선택 해제
                </button>
                <button
                  ref={tagPickerBtnRef}
                  className={`btn btn-ghost btn-sm ${showTagPicker ? 'active' : ''}`}
                  onClick={() => setShowTagPicker((v) => !v)}
                  disabled={selectedIds.size === 0 || bulkLoading}
                >
                  🏷 태그 부착
                </button>
                <button
                  className="btn btn-danger btn-sm"
                  onClick={handleBulkDelete}
                  disabled={selectedIds.size === 0 || bulkLoading}
                >
                  {bulkLoading ? '삭제 중...' : '🗑 삭제'}
                </button>
                <button className="btn btn-ghost btn-sm" onClick={toggleBulkMode}>
                  취소
                </button>
              </div>

              {/* 태그 선택 팝오버 */}
              {showTagPicker && (
                <div className="bulk-tag-picker" ref={tagPickerRef}>
                  <div className="bulk-tag-picker-header">
                    <span className="bulk-tag-picker-title">부착할 태그를 선택하세요</span>
                    <button
                      className="bulk-tag-picker-close"
                      onClick={() => setShowTagPicker(false)}
                      aria-label="태그 선택 닫기"
                    >
                      ✕
                    </button>
                  </div>
                  {tags.length === 0 ? (
                    <p className="bulk-tag-picker-empty">태그가 아직 없어요</p>
                  ) : (
                    <div className="bulk-tag-picker-list">
                      {tags.map((tag) => (
                        <button
                          key={tag.id}
                          className="bulk-tag-chip"
                          onClick={() => handleBulkTag(tag)}
                          disabled={bulkLoading}
                        >
                          #{tag.name}
                        </button>
                      ))}
                    </div>
                  )}
                </div>
              )}
            </>
          )}

          {/* 스마트폴더 활성 표시 */}
          {isSFMode && (
            <div className="sf-active-bar">
              <span>
                🗂 <strong>{activeSF.name}</strong>
              </span>
              <span className="sf-active-desc">
                {activeSF.tagIds?.length > 0
                  ? `태그 ${activeSF.tagIds.length}개 (${activeSF.tagMatchMode})`
                  : '태그 필터 없음'}
                {activeSF.keyword ? ` · "${activeSF.keyword}"` : ''}
                {activeSF.createdWithinDays ? ` · 최근 ${activeSF.createdWithinDays}일` : ''}
              </span>
              <button className="sf-active-close" onClick={() => setActiveSF(null)}>
                × 해제
              </button>
            </div>
          )}

          {/* 다중 태그 선택 표시 */}
          {!isSFMode && selectedTagIds.length > 0 && (
            <div className="active-tags-bar">
              <span>태그 필터:</span>
              {selectedTagIds.map((id) => {
                const tag = tags.find((t) => t.id === id)
                return tag ? (
                  <span key={id} className="active-tag-chip">
                    #{tag.name}
                    <button
                      onClick={() => toggleTag(id)}
                      aria-label={`#${tag.name} 태그 필터 제거`}
                    >
                      ×
                    </button>
                  </span>
                ) : null
              })}
              {selectedTagIds.length >= 2 && (
                <span className="active-mode-badge">{tagMatchMode === 'ALL' ? 'AND' : 'OR'}</span>
              )}
            </div>
          )}

          {/* 노트 목록 */}
          {(loading && !isSFMode) || (sfLoading && isSFMode) ? (
            <Spinner />
          ) : error ? (
            <p className="dashboard-error">{error}</p>
          ) : displayNotes.length === 0 ? (
            <div className="dashboard-empty">
              {isSFMode ? (
                <p>이 스마트 폴더 조건에 맞는 메모가 없습니다.</p>
              ) : isSearchMode ? (
                <p>"{debouncedQ}" 검색 결과가 없습니다.</p>
              ) : (
                <>
                  <span className="empty-icon">📝</span>
                  <p className="empty-title">아직 메모가 없습니다</p>
                  <p className="empty-desc">첫 메모를 쓰면 AI가 연관 노트를 연결해 드립니다.</p>
                  <Link to="/notes/new" className="btn btn-primary">
                    첫 메모 작성하기
                  </Link>
                </>
              )}
            </div>
          ) : (
            <>
              <div className={`notes-grid view-${viewMode}`}>
                {displayNotes.map((note) => (
                  <NoteCard
                    key={note.id}
                    note={note}
                    viewMode={viewMode}
                    bulkMode={bulkMode}
                    selected={selectedIds.has(note.id)}
                    onSelect={() => toggleSelectNote(note.id)}
                  />
                ))}
              </div>

              {!isSFMode && !isSearchMode && totalPages > 1 && (
                <div className="pagination">
                  <button
                    className="btn btn-ghost btn-sm"
                    disabled={page === 0}
                    onClick={() => setPage((p) => p - 1)}
                  >
                    ← 이전
                  </button>
                  <span className="pagination-info">
                    {page + 1} / {totalPages}
                  </span>
                  <button
                    className="btn btn-ghost btn-sm"
                    disabled={page >= totalPages - 1}
                    onClick={() => setPage((p) => p + 1)}
                  >
                    다음 →
                  </button>
                </div>
              )}
            </>
          )}
        </main>
      </div>

      {showSFModal && (
        <SmartFolderModal
          allTags={tags}
          editingFolder={editingSF}
          onClose={() => {
            setShowSFModal(false)
            setEditingSF(null)
          }}
          onSaved={handleSFSaved}
        />
      )}

      {showTemplateModal && <TemplateModal onClose={() => setShowTemplateModal(false)} />}

      {/* 웹 클리핑 모달 */}
      {showClipModal && (
        <div className="modal-backdrop" onClick={() => !clipLoading && setShowClipModal(false)}>
          <div className="clip-modal" onClick={(e) => e.stopPropagation()}>
            <div className="clip-modal-header">
              <h3>🌐 웹 클리핑</h3>
              <button
                className="modal-close-btn"
                onClick={() => setShowClipModal(false)}
                disabled={clipLoading}
                aria-label="웹 클리핑 모달 닫기"
              >
                ×
              </button>
            </div>
            <p className="clip-modal-desc">
              URL을 입력하면 제목과 본문을 자동으로 추출해 새 노트로 저장합니다.
            </p>
            <div className="clip-url-row">
              <input
                className="clip-url-input"
                type="url"
                placeholder="https://example.com/article"
                value={clipUrl}
                onChange={(e) => {
                  setClipUrl(e.target.value)
                  setClipError('')
                }}
                onKeyDown={(e) => e.key === 'Enter' && !clipLoading && handleClip()}
                autoFocus
                disabled={clipLoading}
              />
            </div>
            {clipError && <p className="clip-error">{clipError}</p>}
            <div className="clip-modal-actions">
              <button
                className="btn btn-ghost btn-sm"
                onClick={() => setShowClipModal(false)}
                disabled={clipLoading}
              >
                취소
              </button>
              <button
                className="btn btn-primary btn-sm"
                onClick={handleClip}
                disabled={clipLoading || !clipUrl.trim()}
              >
                {clipLoading ? '클리핑 중...' : '노트로 저장'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
