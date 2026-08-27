import { memo, useCallback, useLayoutEffect, useRef, useState } from 'react'
import './SimpleEditor.css'

// ── 블록 타입 서식 버튼 ───────────────────────────────────────
const BLOCK_FORMATS = [
  { type: 'h1', label: '제목' },
  { type: 'h2', label: '부제목' },
  { type: 'h3', label: '소제목' },
  { type: 'p', label: '본문' },
  { type: 'ul', label: '• 목록' },
  { type: 'ol', label: '1. 번호' },
  { type: 'checkbox', label: '☑ 체크' },
]

// 인라인 서식 버튼 (마크다운 마커 삽입)
const INLINE_FORMATS = [
  { syntax: '**', label: 'B', tip: '굵게 (Ctrl+B)', style: { fontWeight: 700 } },
  { syntax: '*', label: 'I', tip: '기울임 (Ctrl+I)', style: { fontStyle: 'italic' } },
]

let _nextId = 1
const uid = () => _nextId++

// ── 마크다운 ↔ 블록 변환 ─────────────────────────────────────
// 코드 펜스(```)로 열린 구간은 내부를 해석하지 않고 하나의 code 블록으로 묶는다.
// 그러지 않으면 코드 안의 "2. foo" 같은 줄이 목록으로 오인되어 원문이 변형된다.
const FENCE_RE = /^```(.*)$/

export function mdToBlocks(md) {
  if (!md) return [{ id: uid(), type: 'p', text: '' }]
  const lines = md.split('\n')
  const blocks = []

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i]

    const fence = line.match(FENCE_RE)
    if (fence) {
      const lang = fence[1]
      const body = []
      let closed = false
      i++
      while (i < lines.length) {
        if (lines[i] === '```') {
          closed = true
          break
        }
        body.push(lines[i])
        i++
      }
      blocks.push({
        id: uid(),
        type: 'code',
        text: body.join('\n'),
        lang,
        empty: body.length === 0,
        unclosed: !closed,
      })
      continue
    }

    if (line === '---') {
      blocks.push({ id: uid(), type: 'hr', text: '' })
      continue
    }
    if (line.startsWith('### ')) {
      blocks.push({ id: uid(), type: 'h3', text: line.slice(4) })
      continue
    }
    if (line.startsWith('## ')) {
      blocks.push({ id: uid(), type: 'h2', text: line.slice(3) })
      continue
    }
    if (line.startsWith('# ')) {
      blocks.push({ id: uid(), type: 'h1', text: line.slice(2) })
      continue
    }

    const cbMatch = line.match(/^- \[([ x])\] (.*)/)
    if (cbMatch) {
      blocks.push({ id: uid(), type: 'checkbox', text: cbMatch[2], checked: cbMatch[1] === 'x' })
      continue
    }
    if (line.startsWith('- ')) {
      blocks.push({ id: uid(), type: 'ul', text: line.slice(2) })
      continue
    }
    const olMatch = line.match(/^(\d+)\. (.*)/)
    if (olMatch) {
      blocks.push({ id: uid(), type: 'ol', text: olMatch[2], num: Number(olMatch[1]) })
      continue
    }
    blocks.push({ id: uid(), type: 'p', text: line })
  }

  return blocks.length ? blocks : [{ id: uid(), type: 'p', text: '' }]
}

export function blocksToMd(blocks) {
  // 순서 목록은 원문 번호를 유지한다. 새로 만들어져 번호가 없는 항목만
  // 직전 항목의 번호에서 이어 붙인다.
  let lastOlNum = 0
  const out = []

  for (const b of blocks) {
    if (b.type !== 'ol') lastOlNum = 0

    if (b.type === 'code') {
      const fence = '```' + (b.lang ?? '')
      if (b.empty && !b.text) out.push(b.unclosed ? fence : fence + '\n```')
      else if (b.unclosed) out.push(fence + '\n' + b.text)
      else out.push(fence + '\n' + b.text + '\n```')
    } else if (b.type === 'hr') out.push('---')
    else if (b.type === 'h1') out.push(`# ${b.text}`)
    else if (b.type === 'h2') out.push(`## ${b.text}`)
    else if (b.type === 'h3') out.push(`### ${b.text}`)
    else if (b.type === 'ul') out.push(`- ${b.text}`)
    else if (b.type === 'ol') {
      const num = b.num ?? lastOlNum + 1
      lastOlNum = num
      out.push(`${num}. ${b.text}`)
    } else if (b.type === 'checkbox') out.push(`- [${b.checked ? 'x' : ' '}] ${b.text}`)
    else out.push(b.text)
  }

  return out.join('\n')
}

// code 블록은 textarea 로 그린다. contentEditable 은 끝의 개행을 렌더링하지
// 않아 커서가 어긋나기 때문이다. 두 종류의 요소에서 텍스트를 같은 방법으로 읽는다.
const isTextarea = (el) => el?.tagName === 'TEXTAREA'
const readText = (el) => (el ? (isTextarea(el) ? el.value : el.textContent) : null)
// textarea 는 내용 높이만큼 늘려 스크롤바 없이 전체 코드를 보여 준다.
const autoGrow = (el) => {
  el.style.height = 'auto'
  el.style.height = `${el.scrollHeight}px`
}

// ── 커서 위치 계산 ────────────────────────────────────────────
function getCaretOffset(el) {
  const sel = window.getSelection()
  if (!sel || !sel.rangeCount) return 0
  const range = sel.getRangeAt(0).cloneRange()
  const temp = document.createRange()
  temp.selectNodeContents(el)
  temp.setEnd(range.startContainer, range.startOffset)
  return temp.toString().length
}

function getSelectionOffsets(el) {
  const sel = window.getSelection()
  if (!sel || !sel.rangeCount) return { start: 0, end: 0 }
  const range = sel.getRangeAt(0)
  const measure = (container, offset) => {
    const walk = document.createTreeWalker(el, NodeFilter.SHOW_TEXT)
    let total = 0,
      node = walk.nextNode()
    while (node) {
      if (node === container) return total + offset
      total += node.textContent.length
      node = walk.nextNode()
    }
    return total
  }
  return {
    start: measure(range.startContainer, range.startOffset),
    end: measure(range.endContainer, range.endOffset),
  }
}

function setCaretOffset(el, offset) {
  if (!el) return
  try {
    const walk = document.createTreeWalker(el, NodeFilter.SHOW_TEXT)
    let rem = offset,
      node = walk.nextNode()
    const range = document.createRange()
    while (node) {
      if (rem <= node.textContent.length) {
        range.setStart(node, rem)
        range.collapse(true)
        const sel = window.getSelection()
        sel.removeAllRanges()
        sel.addRange(range)
        return
      }
      rem -= node.textContent.length
      node = walk.nextNode()
    }
    range.selectNodeContents(el)
    range.collapse(false)
    const sel = window.getSelection()
    sel.removeAllRanges()
    sel.addRange(range)
  } catch (_) {}
}

// ── 블록 DOM 컴포넌트 ─────────────────────────────────────────
const BlockEl = memo(function BlockEl({
  id,
  type,
  text,
  checked,
  olIndex,
  lang,
  onInput,
  onKeyDown,
  onFocus,
  onRef,
  onToggleCheck,
}) {
  const elRef = useRef(null)
  const composing = useRef(false)

  const refCb = useCallback(
    (el) => {
      elRef.current = el
      onRef(id, el)
    },
    [id, onRef],
  )

  // DOM 동기화 (포커스 중이 아닐 때만)
  useLayoutEffect(() => {
    const el = elRef.current
    if (!el) return
    if (isTextarea(el)) {
      if (el.value !== text) el.value = text
      autoGrow(el)
      return
    }
    if (el !== document.activeElement && el.textContent !== text) {
      el.textContent = text
    }
  })

  const handleInput = useCallback(
    (e) => {
      const el = e.currentTarget
      if (isTextarea(el)) autoGrow(el)
      if (!composing.current) onInput(id, readText(el))
    },
    [id, onInput],
  )
  const handleCompositionEnd = useCallback(
    (e) => {
      composing.current = false
      onInput(id, readText(e.currentTarget))
    },
    [id, onInput],
  )
  const handleKeyDownCb = useCallback(
    (e) => {
      onKeyDown(e, id)
    },
    [id, onKeyDown],
  )
  const handleFocusCb = useCallback(() => {
    onFocus(id)
  }, [id, onFocus])

  // ── code 블록 ──
  // 코드 펜스 구간 전체를 textarea 하나가 담는다. 개행과 커서 이동을 브라우저가
  // 그대로 처리하므로 원문이 변형되지 않는다.
  if (type === 'code') {
    return (
      <div className="se-code-wrap">
        {lang ? <span className="se-code-lang">{lang}</span> : null}
        <textarea
          ref={refCb}
          className="se-block se-code"
          rows={1}
          spellCheck={false}
          placeholder="코드를 입력하세요..."
          onCompositionStart={() => {
            composing.current = true
          }}
          onCompositionEnd={handleCompositionEnd}
          onInput={handleInput}
          onKeyDown={handleKeyDownCb}
          onFocus={handleFocusCb}
        />
      </div>
    )
  }

  // ── hr 블록 ──
  if (type === 'hr') {
    return (
      <div
        ref={refCb}
        className="se-block se-hr"
        tabIndex={0}
        onKeyDown={handleKeyDownCb}
        onFocus={handleFocusCb}
      >
        <hr />
      </div>
    )
  }

  // ── checkbox 블록 ──
  if (type === 'checkbox') {
    return (
      <div className="se-checkbox-wrap">
        <input
          type="checkbox"
          className="se-cb-input"
          checked={!!checked}
          onChange={() => onToggleCheck(id)}
        />
        <span
          ref={refCb}
          className={`se-block se-checkbox${checked ? ' se-checked' : ''}`}
          contentEditable
          suppressContentEditableWarning
          data-placeholder="할 일..."
          onCompositionStart={() => {
            composing.current = true
          }}
          onCompositionEnd={handleCompositionEnd}
          onInput={handleInput}
          onKeyDown={handleKeyDownCb}
          onFocus={handleFocusCb}
        />
      </div>
    )
  }

  // ── ul 블록 ──
  if (type === 'ul') {
    return (
      <div className="se-ul-wrap">
        <span className="se-ul-bullet">•</span>
        <div
          ref={refCb}
          className="se-block se-ul"
          contentEditable
          suppressContentEditableWarning
          data-placeholder="목록 항목..."
          onCompositionStart={() => {
            composing.current = true
          }}
          onCompositionEnd={handleCompositionEnd}
          onInput={handleInput}
          onKeyDown={handleKeyDownCb}
          onFocus={handleFocusCb}
        />
      </div>
    )
  }

  // ── ol 블록 ──
  if (type === 'ol') {
    return (
      <div className="se-ol-wrap">
        <span className="se-ol-num">{olIndex}.</span>
        <div
          ref={refCb}
          className="se-block se-ol"
          contentEditable
          suppressContentEditableWarning
          data-placeholder="항목..."
          onCompositionStart={() => {
            composing.current = true
          }}
          onCompositionEnd={handleCompositionEnd}
          onInput={handleInput}
          onKeyDown={handleKeyDownCb}
          onFocus={handleFocusCb}
        />
      </div>
    )
  }

  // ── h1 / h2 / h3 / p 블록 ──
  const Tag = type
  return (
    <Tag
      ref={refCb}
      className={`se-block se-${type}`}
      contentEditable
      suppressContentEditableWarning
      data-placeholder={
        type === 'h1'
          ? '제목'
          : type === 'h2'
            ? '부제목'
            : type === 'h3'
              ? '소제목'
              : '내용을 입력하세요...'
      }
      onCompositionStart={() => {
        composing.current = true
      }}
      onCompositionEnd={handleCompositionEnd}
      onInput={handleInput}
      onKeyDown={handleKeyDownCb}
      onFocus={handleFocusCb}
    />
  )
})

// ── 히스토리 훅 ──────────────────────────────────────────────
const MAX_HISTORY = 100
const DEBOUNCE_MS = 500

function useHistory(initialMd) {
  const stack = useRef([initialMd])
  const index = useRef(0)
  const debounce = useRef(null)
  const restoring = useRef(false)

  const pushNow = useCallback((md) => {
    if (restoring.current) return
    const trimmed = stack.current.slice(0, index.current + 1)
    if (trimmed[trimmed.length - 1] === md) return
    trimmed.push(md)
    if (trimmed.length > MAX_HISTORY) trimmed.shift()
    stack.current = trimmed
    index.current = trimmed.length - 1
  }, [])

  const push = useCallback(
    (md, immediate = false) => {
      if (restoring.current) return
      clearTimeout(debounce.current)
      if (immediate) pushNow(md)
      else debounce.current = setTimeout(() => pushNow(md), DEBOUNCE_MS)
    },
    [pushNow],
  )

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

// ── SimpleEditor 메인 ─────────────────────────────────────────
export default function SimpleEditor({ value, onChange }) {
  const [blocks, setBlocks] = useState(() => mdToBlocks(value))
  const [activeId, setActiveId] = useState(null)
  const refs = useRef({})
  const pendingFocus = useRef(null)
  const hist = useHistory(value ?? '')

  const [canUndo, setCanUndo] = useState(false)
  const [canRedo, setCanRedo] = useState(false)
  const refreshUR = useCallback(() => {
    setCanUndo(hist.canUndo())
    setCanRedo(hist.canRedo())
  }, [hist])

  // 포커스 복원
  useLayoutEffect(() => {
    if (!pendingFocus.current) return
    const { id, offset } = pendingFocus.current
    pendingFocus.current = null
    const el = refs.current[id]
    if (el) {
      el.focus()
      setCaretOffset(el, offset)
    }
  })

  const handleRef = useCallback((id, el) => {
    if (el) refs.current[id] = el
    else delete refs.current[id]
  }, [])

  // 블록 변경 → md 변환 + 상위 알림 + 히스토리
  const applyBlocks = useCallback(
    (newBlocks, { immediate = false } = {}) => {
      const md = blocksToMd(newBlocks)
      setBlocks(newBlocks)
      onChange(md)
      hist.push(md, immediate)
      setTimeout(refreshUR, 10)
    },
    [onChange, hist, refreshUR],
  )

  // 히스토리 복원
  const restoreFromMd = useCallback(
    (md) => {
      if (md === null) return
      hist.restoring.current = true
      const newBlocks = mdToBlocks(md)
      setBlocks(newBlocks)
      onChange(md)
      pendingFocus.current = { id: newBlocks[0]?.id, offset: 0 }
      setTimeout(() => {
        hist.restoring.current = false
        refreshUR()
      }, 20)
    },
    [hist, onChange, refreshUR],
  )

  const handleUndo = useCallback(() => restoreFromMd(hist.undo()), [hist, restoreFromMd])
  const handleRedo = useCallback(() => restoreFromMd(hist.redo()), [hist, restoreFromMd])

  // 타이핑
  const handleInput = useCallback(
    (id, text) => {
      setBlocks((prev) => {
        const next = prev.map((b) => (b.id === id ? { ...b, text } : b))
        const md = blocksToMd(next)
        onChange(md)
        hist.push(md, false)
        setTimeout(refreshUR, 10)
        return next
      })
    },
    [onChange, hist, refreshUR],
  )

  const handleFocus = useCallback((id) => setActiveId(id), [])

  // ── 키보드 처리 ──────────────────────────────────────────────
  const handleKeyDown = useCallback(
    (e, id) => {
      const el = refs.current[id]
      if (!el) return
      const ctrl = e.ctrlKey || e.metaKey

      // 실행취소 / 다시실행
      if (ctrl && e.key === 'z' && !e.shiftKey) {
        e.preventDefault()
        handleUndo()
        return
      }
      if ((ctrl && e.key === 'y') || (ctrl && e.shiftKey && (e.key === 'z' || e.key === 'Z'))) {
        e.preventDefault()
        handleRedo()
        return
      }

      // 인라인 서식 단축키
      if (ctrl && e.key === 'b') {
        e.preventDefault()
        handleInlineFormat('**')
        return
      }
      if (ctrl && e.key === 'i') {
        e.preventDefault()
        handleInlineFormat('*')
        return
      }

      const blockType = blocks.find((b) => b.id === id)?.type ?? 'p'

      // code 블록은 textarea 라 Enter 와 Backspace 를 브라우저에 맡긴다. 다만 맨 앞에서
      // Backspace 를 누르면 앞 블록과 병합되어 코드가 본문으로 흡수되므로 막는다.
      if (blockType === 'code') {
        if (e.key === 'Backspace' && el.selectionStart === 0 && el.value !== '') {
          e.preventDefault()
        }
        return
      }

      // hr 블록: Enter → 다음 p 블록 생성, Backspace → 삭제
      if (blockType === 'hr') {
        if (e.key === 'Enter' || e.key === 'Backspace') {
          e.preventDefault()
          setBlocks((prev) => {
            const latest = prev.map((b) => ({
              ...b,
              text: readText(refs.current[b.id]) ?? b.text,
            }))
            hist.push(blocksToMd(latest), true)
            const idx = latest.findIndex((b) => b.id === id)
            let updated
            if (e.key === 'Enter') {
              const newId = uid()
              updated = [
                ...latest.slice(0, idx + 1),
                { id: newId, type: 'p', text: '' },
                ...latest.slice(idx + 1),
              ]
              const md = blocksToMd(updated)
              onChange(md)
              hist.push(md, true)
              setTimeout(refreshUR, 10)
              pendingFocus.current = { id: newId, offset: 0 }
            } else {
              if (latest.length === 1) return prev
              updated = [...latest.slice(0, idx), ...latest.slice(idx + 1)]
              const md = blocksToMd(updated)
              onChange(md)
              hist.push(md, true)
              setTimeout(refreshUR, 10)
              const focusId = (latest[idx - 1] ?? latest[idx + 1])?.id
              if (focusId) pendingFocus.current = { id: focusId, offset: 0 }
            }
            return updated
          })
        }
        return
      }

      // Enter: 새 블록 추가
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault()
        const offset = getCaretOffset(el)
        const text = el.textContent
        const before = text.slice(0, offset)
        const after = text.slice(offset)

        // 목록 계열 빈 블록 → 본문으로
        const listTypes = ['ul', 'ol', 'checkbox']
        const newType = listTypes.includes(blockType) ? (before === '' ? 'p' : blockType) : 'p'

        setBlocks((prev) => {
          const latest = prev.map((b) => ({
            ...b,
            text: readText(refs.current[b.id]) ?? b.text,
          }))
          hist.push(blocksToMd(latest), true)
          const idx = latest.findIndex((b) => b.id === id)
          const newId = uid()
          const newBlock = { id: newId, type: newType, text: after }
          if (newType === 'checkbox') newBlock.checked = false

          // 빈 목록 블록 Enter → 본문으로 전환 (현재 블록 type 변경)
          const currentBlock =
            newType === 'p' && listTypes.includes(blockType)
              ? { ...latest[idx], type: 'p', text: before }
              : { ...latest[idx], text: before }

          const updated = [
            ...latest.slice(0, idx),
            currentBlock,
            { ...newBlock },
            ...latest.slice(idx + 1),
          ]
          const md = blocksToMd(updated)
          onChange(md)
          hist.push(md, true)
          setTimeout(refreshUR, 10)
          pendingFocus.current = { id: newId, offset: 0 }
          return updated
        })
      }

      // Backspace: 줄 앞 → 이전 블록과 병합
      else if (e.key === 'Backspace') {
        const offset = getCaretOffset(el)
        if (offset === 0) {
          e.preventDefault()
          setBlocks((prev) => {
            if (prev.length === 1) return prev
            const latest = prev.map((b) => ({
              ...b,
              text: readText(refs.current[b.id]) ?? b.text,
            }))
            hist.push(blocksToMd(latest), true)
            const idx = latest.findIndex((b) => b.id === id)
            if (idx === 0) return prev
            const prevBlock = latest[idx - 1]
            const prevLen = prevBlock.text.length
            const merged = { ...prevBlock, text: prevBlock.text + el.textContent }
            const updated = [...latest.slice(0, idx - 1), merged, ...latest.slice(idx + 1)]
            const md = blocksToMd(updated)
            onChange(md)
            hist.push(md, true)
            setTimeout(refreshUR, 10)
            pendingFocus.current = { id: prevBlock.id, offset: prevLen }
            return updated
          })
        }
      }
    },
    [blocks, onChange, hist, handleUndo, handleRedo, refreshUR, handleInput],
  )

  // ── 블록 서식 변경 ───────────────────────────────────────────
  const handleFormat = useCallback(
    (type) => {
      if (!activeId) return
      // code 블록을 다른 타입으로 바꾸면 여러 줄이 한 줄짜리 블록으로 뭉개진다.
      if (blocks.find((b) => b.id === activeId)?.type === 'code') return
      const el = refs.current[activeId]
      const offset = el ? getCaretOffset(el) : 0
      setBlocks((prev) => {
        const latest = prev.map((b) => ({
          ...b,
          text: readText(refs.current[b.id]) ?? b.text,
        }))
        hist.push(blocksToMd(latest), true)
        const updated = latest.map((b) => {
          if (b.id !== activeId) return b
          const base = { ...b, type }
          if (type === 'checkbox' && base.checked === undefined) base.checked = false
          return base
        })
        const md = blocksToMd(updated)
        onChange(md)
        hist.push(md, true)
        setTimeout(refreshUR, 10)
        pendingFocus.current = { id: activeId, offset }
        return updated
      })
    },
    [activeId, blocks, onChange, hist, refreshUR],
  )

  // ── 구분선 삽입 ──────────────────────────────────────────────
  const handleInsertHr = useCallback(() => {
    const targetId = activeId
    setBlocks((prev) => {
      const latest = prev.map((b) => ({
        ...b,
        text: readText(refs.current[b.id]) ?? b.text,
      }))
      hist.push(blocksToMd(latest), true)
      const idx = targetId ? latest.findIndex((b) => b.id === targetId) : latest.length - 1
      const hrId = uid()
      const nextId = uid()
      const updated = [
        ...latest.slice(0, idx + 1),
        { id: hrId, type: 'hr', text: '' },
        { id: nextId, type: 'p', text: '' },
        ...latest.slice(idx + 1),
      ]
      const md = blocksToMd(updated)
      onChange(md)
      hist.push(md, true)
      setTimeout(refreshUR, 10)
      pendingFocus.current = { id: nextId, offset: 0 }
      return updated
    })
  }, [activeId, onChange, hist, refreshUR])

  // ── 인라인 서식 삽입 ─────────────────────────────────────────
  const handleInlineFormat = useCallback(
    (syntax) => {
      if (!activeId) return
      const el = refs.current[activeId]
      if (!el) return
      const { start, end } = getSelectionOffsets(el)
      const text = el.textContent
      const selected = text.slice(start, end)
      const inner = selected || '텍스트'
      const newText = text.slice(0, start) + syntax + inner + syntax + text.slice(end)
      const newEnd = start + syntax.length + inner.length

      // DOM 즉시 업데이트 (포커스 중이어도)
      el.textContent = newText
      setCaretOffset(el, newEnd)

      setBlocks((prev) => {
        const latest = prev.map((b) =>
          b.id === activeId
            ? { ...b, text: newText }
            : { ...b, text: readText(refs.current[b.id]) ?? b.text },
        )
        const md = blocksToMd(latest)
        onChange(md)
        hist.push(md, true)
        setTimeout(refreshUR, 10)
        return latest
      })
    },
    [activeId, onChange, hist, refreshUR],
  )

  // ── 체크박스 토글 ────────────────────────────────────────────
  const handleToggleCheck = useCallback(
    (id) => {
      setBlocks((prev) => {
        const updated = prev.map((b) => (b.id === id ? { ...b, checked: !b.checked } : b))
        const md = blocksToMd(updated)
        onChange(md)
        hist.push(md, true)
        setTimeout(refreshUR, 10)
        return updated
      })
    },
    [onChange, hist, refreshUR],
  )

  const activeType = blocks.find((b) => b.id === activeId)?.type ?? 'p'

  // ol 인덱스 계산
  let olCounter = 0
  const olIndexMap = {}
  blocks.forEach((b) => {
    if (b.type === 'ol') {
      olCounter++
      olIndexMap[b.id] = olCounter
    } else olCounter = 0
  })

  return (
    <div className="se-editor">
      {/* ── 툴바 ── */}
      <div className="se-toolbar">
        {/* 실행취소 / 다시실행 */}
        <div className="se-undo-group">
          <button
            type="button"
            className="se-undo-btn"
            onMouseDown={(e) => {
              e.preventDefault()
              handleUndo()
            }}
            disabled={!canUndo}
            title="실행취소 (Ctrl+Z)"
          >
            ↩
          </button>
          <button
            type="button"
            className="se-undo-btn"
            onMouseDown={(e) => {
              e.preventDefault()
              handleRedo()
            }}
            disabled={!canRedo}
            title="다시실행 (Ctrl+Y)"
          >
            ↪
          </button>
        </div>

        <span className="se-toolbar-divider" />

        {/* 블록 서식 */}
        {BLOCK_FORMATS.map((fmt) => (
          <button
            key={fmt.type}
            type="button"
            className={`se-fmt-btn${activeType === fmt.type ? ' active' : ''}`}
            onMouseDown={(e) => {
              e.preventDefault()
              handleFormat(fmt.type)
            }}
          >
            {fmt.label}
          </button>
        ))}

        <span className="se-toolbar-divider" />

        {/* 인라인 서식 */}
        {INLINE_FORMATS.map((fmt) => (
          <button
            key={fmt.syntax}
            type="button"
            className="se-fmt-btn se-inline-btn"
            title={fmt.tip}
            style={fmt.style}
            onMouseDown={(e) => {
              e.preventDefault()
              handleInlineFormat(fmt.syntax)
            }}
          >
            {fmt.label}
          </button>
        ))}

        <span className="se-toolbar-divider" />

        {/* 구분선 삽입 */}
        <button
          type="button"
          className="se-fmt-btn"
          title="구분선 삽입"
          onMouseDown={(e) => {
            e.preventDefault()
            handleInsertHr()
          }}
        >
          ─ 구분선
        </button>
      </div>

      {/* ── 블록 목록 ── */}
      <div className="se-content">
        {blocks.map((block) => (
          <BlockEl
            key={`${block.id}-${block.type}`}
            id={block.id}
            type={block.type}
            text={block.text}
            checked={block.checked}
            olIndex={olIndexMap[block.id] ?? 1}
            lang={block.lang}
            onInput={handleInput}
            onKeyDown={handleKeyDown}
            onFocus={handleFocus}
            onRef={handleRef}
            onToggleCheck={handleToggleCheck}
          />
        ))}
      </div>
    </div>
  )
}
