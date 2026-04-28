import { useRef, useState, useCallback } from 'react'
import './MarkdownEditor.css'

// ── 간단한 마크다운 → HTML 변환기 ────────────────────────────
function escapeHtml(s) {
  return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
}

function inlineMd(s) {
  const parts = []
  let last = 0
  const codeRe = /`([^`]+)`/g
  let m
  while ((m = codeRe.exec(s)) !== null) {
    parts.push(escapeHtml(s.slice(last, m.index)))
    parts.push(`<code>${escapeHtml(m[1])}</code>`)
    last = m.index + m[0].length
  }
  parts.push(escapeHtml(s.slice(last)))
  let r = parts.join('')
  r = r.replace(/\*\*\*(.+?)\*\*\*/g, '<strong><em>$1</em></strong>')
  r = r.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
  r = r.replace(/\*(.+?)\*/g, '<em>$1</em>')
  r = r.replace(/_(.+?)_/g, '<em>$1</em>')
  return r
}

function mdToHtml(md) {
  const lines = md.split('\n')
  const out = []
  let i = 0
  while (i < lines.length) {
    const line = lines[i]
    if (/^---+$/.test(line.trim())) { out.push('<hr />'); i++; continue }
    const hm = line.match(/^(#{1,3})\s+(.*)/)
    if (hm) { out.push(`<h${hm[1].length}>${inlineMd(hm[2])}</h${hm[1].length}>`); i++; continue }
    if (line.startsWith('> ')) { out.push(`<blockquote>${inlineMd(line.slice(2))}</blockquote>`); i++; continue }
    const cbm = line.match(/^- \[([ x])\] (.*)/)
    if (cbm) { out.push(`<p class="md-check"><input type="checkbox"${cbm[1]==='x'?' checked':''} disabled /> ${inlineMd(cbm[2])}</p>`); i++; continue }
    if (/^[-*] /.test(line)) {
      const items = []
      while (i < lines.length && /^[-*] /.test(lines[i])) { items.push(`<li>${inlineMd(lines[i].replace(/^[-*] /,''))}</li>`); i++ }
      out.push(`<ul>${items.join('')}</ul>`); continue
    }
    if (/^\d+\. /.test(line)) {
      const items = []
      while (i < lines.length && /^\d+\. /.test(lines[i])) { items.push(`<li>${inlineMd(lines[i].replace(/^\d+\. /,''))}</li>`); i++ }
      out.push(`<ol>${items.join('')}</ol>`); continue
    }
    if (line.trim() === '') { out.push('<br />'); i++; continue }
    out.push(`<p>${inlineMd(line)}</p>`); i++
  }
  return out.join('\n')
}

// ── 툴바 버튼 정의 ────────────────────────────────────────────
const TOOLBAR = [
  { label: '제목',    syntax: '# ',      block: true,  tip: '큰 제목 (H1)' },
  { label: '부제목',  syntax: '## ',     block: true,  tip: '중간 제목 (H2)' },
  { label: '소제목',  syntax: '### ',    block: true,  tip: '작은 제목 (H3)' },
  { type: 'sep' },
  { label: 'B',       syntax: '**',      wrap: true,   tip: '굵게 (Bold)',     style: { fontWeight: 700 } },
  { label: 'I',       syntax: '*',       wrap: true,   tip: '기울임 (Italic)', style: { fontStyle: 'italic' } },
  { type: 'sep' },
  { label: '목록',    syntax: '- ',      block: true,  tip: '글머리 기호' },
  { label: '번호',    syntax: '1. ',     block: true,  tip: '번호 목록' },
  { label: '체크',    syntax: '- [ ] ',  block: true,  tip: '체크박스' },
  { type: 'sep' },
  { label: '코드',    syntax: '`',       wrap: true,   tip: '인라인 코드' },
  { label: '구분선',  syntax: '\n---\n', block: false, tip: '수평선' },
  { label: '인용',    syntax: '> ',      block: true,  tip: '인용문' },
]

// ── 히스토리 훅 ──────────────────────────────────────────────
const MAX_HISTORY = 100
const DEBOUNCE_MS = 500

function useHistory(initialValue) {
  const stack     = useRef([initialValue])
  const index     = useRef(0)
  const debounce  = useRef(null)
  const restoring = useRef(false)

  const pushNow = useCallback((text) => {
    if (restoring.current) return
    const trimmed = stack.current.slice(0, index.current + 1)
    if (trimmed[trimmed.length - 1] === text) return
    trimmed.push(text)
    if (trimmed.length > MAX_HISTORY) trimmed.shift()
    stack.current = trimmed
    index.current = trimmed.length - 1
  }, [])

  const push = useCallback((text, immediate) => {
    if (restoring.current) return
    clearTimeout(debounce.current)
    if (immediate) {
      pushNow(text)
    } else {
      debounce.current = setTimeout(() => pushNow(text), DEBOUNCE_MS)
    }
  }, [pushNow])

  const undo = useCallback(() => {
    clearTimeout(debounce.current)
    if (index.current <= 0) return null
    index.current -= 1
    return stack.current[index.current]
  }, [])

  const redo = useCallback(() => {
    clearTimeout(debounce.current)
    if (index.current >= stack.current.length - 1) return null
    index.current += 1
    return stack.current[index.current]
  }, [])

  const canUndo = () => index.current > 0
  const canRedo = () => index.current < stack.current.length - 1

  return { push, undo, redo, canUndo, canRedo, restoring }
}

// ── 컴포넌트 ─────────────────────────────────────────────────
export default function MarkdownEditor({ value, onChange }) {
  const [preview,  setPreview]  = useState(false)
  const [undoable, setUndoable] = useState(false)
  const [redoable, setRedoable] = useState(false)
  const textareaRef = useRef(null)
  const hist = useHistory(value)

  // 버튼 활성화 상태 갱신
  const refreshUR = useCallback(() => {
    setTimeout(() => {
      setUndoable(hist.canUndo())
      setRedoable(hist.canRedo())
    }, 10)
  }, [hist])

  const handleUndo = useCallback(() => {
    const prev = hist.undo()
    if (prev === null) return
    hist.restoring.current = true
    onChange(prev)
    refreshUR()
    requestAnimationFrame(() => {
      hist.restoring.current = false
      textareaRef.current && textareaRef.current.focus()
    })
  }, [hist, onChange, refreshUR])

  const handleRedo = useCallback(() => {
    const next = hist.redo()
    if (next === null) return
    hist.restoring.current = true
    onChange(next)
    refreshUR()
    requestAnimationFrame(() => {
      hist.restoring.current = false
      textareaRef.current && textareaRef.current.focus()
    })
  }, [hist, onChange, refreshUR])

  // textarea onChange: 디바운스 푸시
  const handleChange = useCallback((e) => {
    const newText = e.target.value
    onChange(newText)
    hist.push(newText, false)
    refreshUR()
  }, [hist, onChange, refreshUR])

  // 키보드 단축키 (Ctrl+Z / Ctrl+Y / Ctrl+Shift+Z)
  const handleKeyDown = useCallback((e) => {
    const ctrl = e.ctrlKey || e.metaKey
    if (ctrl && e.key === 'z' && !e.shiftKey) {
      e.preventDefault(); handleUndo(); return
    }
    if ((ctrl && e.key === 'y') || (ctrl && e.shiftKey && (e.key === 'z' || e.key === 'Z'))) {
      e.preventDefault(); handleRedo(); return
    }
  }, [handleUndo, handleRedo])

  // 툴바 버튼: 커서 위치에 마크다운 삽입 + 즉시 히스토리 저장
  const insertSyntax = useCallback(({ syntax, wrap, block }) => {
    const ta = textareaRef.current
    if (!ta) return
    const start    = ta.selectionStart
    const end      = ta.selectionEnd
    const selected = value.slice(start, end)
    const before   = value.slice(0, start)
    const after    = value.slice(end)
    let newText, newCursorStart, newCursorEnd

    if (wrap) {
      const inner   = selected || '텍스트'
      const wrapped = syntax + inner + syntax
      newText        = before + wrapped + after
      newCursorStart = start + syntax.length
      newCursorEnd   = newCursorStart + inner.length
    } else if (block) {
      const lineStart = before.lastIndexOf('\n') + 1
      newText        = value.slice(0, lineStart) + syntax + value.slice(lineStart)
      newCursorStart = start + syntax.length
      newCursorEnd   = end   + syntax.length
    } else {
      newText        = before + syntax + after
      newCursorStart = start + syntax.length
      newCursorEnd   = newCursorStart
    }

    hist.push(value, true)    // 변경 전 상태 저장
    onChange(newText)
    hist.push(newText, true)  // 변경 후 상태 저장
    refreshUR()

    requestAnimationFrame(() => {
      ta.focus()
      ta.setSelectionRange(newCursorStart, newCursorEnd)
    })
  }, [value, onChange, hist, refreshUR])

  return (
    <div className="md-editor">
      <div className="md-toolbar">
        <div className="md-toolbar-left">

          {/* ↩ ↪ 실행취소 / 다시실행 */}
          <button
            type="button"
            className="md-undo-btn"
            onMouseDown={(e) => { e.preventDefault(); handleUndo() }}
            disabled={!undoable}
            title="실행취소 (Ctrl+Z)"
          >&#x21A9;</button>
          <button
            type="button"
            className="md-undo-btn"
            onMouseDown={(e) => { e.preventDefault(); handleRedo() }}
            disabled={!redoable}
            title="다시실행 (Ctrl+Y)"
          >&#x21AA;</button>
          <span className="md-sep" />

          {TOOLBAR.map((btn, i) =>
            btn.type === 'sep' ? (
              <span key={i} className="md-sep" />
            ) : (
              <button
                key={i}
                type="button"
                className="md-btn"
                title={btn.tip}
                style={btn.style}
                onMouseDown={(e) => { e.preventDefault(); insertSyntax(btn) }}
              >
                {btn.label}
              </button>
            )
          )}
        </div>

        <button
          type="button"
          className={'md-preview-toggle' + (preview ? ' active' : '')}
          onClick={() => setPreview((p) => !p)}
        >
          {preview ? '\u270F\uFE0F 편집' : '\uD83D\uDC41 미리보기'}
        </button>
      </div>

      {preview ? (
        <div className="md-preview">
          {value.trim() ? (
            <div dangerouslySetInnerHTML={{ __html: mdToHtml(value) }} />
          ) : (
            <p className="md-preview-empty">내용이 없습니다.</p>
          )}
        </div>
      ) : (
        <textarea
          ref={textareaRef}
          className="md-textarea"
          placeholder="내용을 입력하세요..."
          value={value}
          onChange={handleChange}
          onKeyDown={handleKeyDown}
        />
      )}
    </div>
  )
}
