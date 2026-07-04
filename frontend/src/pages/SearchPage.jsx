import { useEffect, useRef, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { searchApi } from '../api/search.js'
import Navbar from '../components/Navbar.jsx'
import NoteCard from '../components/NoteCard.jsx'
import Spinner from '../components/Spinner.jsx'
import './SearchPage.css'

export default function SearchPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const initialQ = searchParams.get('q') || ''

  const [query, setQuery] = useState(initialQ)
  const [results, setResults] = useState([])
  const [totalElements, setTotalElements] = useState(0)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(false)
  const [searched, setSearched] = useState(false)
  const inputRef = useRef(null)

  const doSearch = async (q, p = 0) => {
    if (!q.trim()) return
    setLoading(true)
    setSearched(true)
    try {
      const { data } = await searchApi.keyword(q.trim(), p)
      setResults(data.data.content)
      setTotalElements(data.data.totalElements)
      setTotalPages(data.data.totalPages)
      setPage(p)
    } catch {
      setResults([])
    } finally {
      setLoading(false)
    }
  }

  // URL 쿼리로 첫 검색
  useEffect(() => {
    if (initialQ) doSearch(initialQ)
    inputRef.current?.focus()
  }, [])

  const handleSubmit = (e) => {
    e.preventDefault()
    setSearchParams(query ? { q: query } : {})
    doSearch(query)
  }

  return (
    <div className="search-page">
      <Navbar />

      <main className="container search-main">
        <h1 className="search-title">검색</h1>

        <form onSubmit={handleSubmit} className="search-form">
          <input
            ref={inputRef}
            className="input search-input"
            placeholder="검색어를 입력하세요..."
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
          <button type="submit" className="btn btn-primary">
            검색
          </button>
        </form>

        {loading ? (
          <Spinner />
        ) : searched ? (
          <>
            <div className="search-meta">
              {totalElements > 0
                ? `"${searchParams.get('q')}" 검색 결과 ${totalElements}건`
                : `"${searchParams.get('q')}"에 대한 결과가 없습니다.`}
            </div>

            {results.length > 0 ? (
              <>
                <div className="notes-grid">
                  {results.map((note) => (
                    <NoteCard key={note.id} note={note} />
                  ))}
                </div>

                {totalPages > 1 && (
                  <div className="pagination">
                    <button
                      className="btn btn-ghost"
                      disabled={page === 0}
                      onClick={() => doSearch(query, page - 1)}
                    >
                      ← 이전
                    </button>
                    <span className="page-info">
                      {page + 1} / {totalPages}
                    </span>
                    <button
                      className="btn btn-ghost"
                      disabled={page >= totalPages - 1}
                      onClick={() => doSearch(query, page + 1)}
                    >
                      다음 →
                    </button>
                  </div>
                )}
              </>
            ) : (
              <div className="search-empty">
                <div className="empty-icon">🔍</div>
                <p>검색어와 일치하는 메모가 없어요.</p>
                <p className="text-secondary" style={{ fontSize: 14, marginTop: 8 }}>
                  다른 키워드로 검색해보거나 새 메모를 작성해보세요.
                </p>
              </div>
            )}
          </>
        ) : (
          <div className="search-hint">
            <div className="empty-icon">💡</div>
            <p>제목 또는 내용으로 메모를 검색해보세요.</p>
          </div>
        )}
      </main>
    </div>
  )
}
