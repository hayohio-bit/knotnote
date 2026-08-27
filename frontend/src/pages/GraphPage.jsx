import { useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { notesApi } from '../api/notes.js'
import Navbar from '../components/Navbar.jsx'
import Spinner from '../components/Spinner.jsx'
import './GraphPage.css'

/* ────────────────────────────────────────────────────────────
 * 색상 팔레트 (태그 첫 번째 기준)
 * ──────────────────────────────────────────────────────────── */
const TAG_COLORS = [
  '#0d9488',
  '#2563eb',
  '#7c3aed',
  '#db2777',
  '#d97706',
  '#059669',
  '#0891b2',
  '#9333ea',
  '#e11d48',
  '#ca8a04',
]

function tagColor(tag) {
  if (!tag) return '#6b7280'
  let hash = 0
  for (let i = 0; i < tag.length; i++) hash = tag.charCodeAt(i) + ((hash << 5) - hash)
  return TAG_COLORS[Math.abs(hash) % TAG_COLORS.length]
}

/**
 * Knot Vitality Score → 노드 색상
 *   >= 0.7 : 초록 (건강)
 *   >= 0.4 : 노랑 (주의)
 *   < 0.4  : 빨강 (Knot Decay 위험)
 */
/** CSS 토큰 값을 읽는다 — 라이트/다크 팔레트를 캔버스에도 반영하기 위함 */
function cssVar(name, fallback) {
  const v = getComputedStyle(document.documentElement).getPropertyValue(name).trim()
  return v || fallback
}

/** --accent 를 알파가 있는 rgba 문자열로 변환 */
function accentRgba(alpha) {
  const hex = cssVar('--accent', '#0d9488').replace('#', '')
  const r = Number.parseInt(hex.slice(0, 2), 16)
  const g = Number.parseInt(hex.slice(2, 4), 16)
  const b = Number.parseInt(hex.slice(4, 6), 16)
  return `rgba(${r},${g},${b},${alpha})`
}

function vitalityColor(vitalityScore) {
  if (vitalityScore >= 0.7) return cssVar('--success', '#10b981') // 건강
  if (vitalityScore >= 0.4) return cssVar('--warning', '#f59e0b') // 주의
  return cssVar('--danger', '#ef4444') // Knot Decay 위험
}

/* ────────────────────────────────────────────────────────────
 * Force-directed 물리 엔진 (순수 JS)
 *  반발: Coulomb F = K_rep / r²
 *  인력: Hooke   F = K_att * (d - REST)
 * ──────────────────────────────────────────────────────────── */
const K_REP = 8000
const K_ATT = 0.03
const REST = 150
const DAMPING = 0.85
const GRAVITY = 0.03
const MAX_V = 8

function initPhysics(nodes, edges, W, H) {
  const n = nodes.length
  return nodes.map((node, i) => {
    const angle = (2 * Math.PI * i) / Math.max(n, 1)
    const r = Math.min(W, H) * 0.35
    return { ...node, x: W / 2 + r * Math.cos(angle), y: H / 2 + r * Math.sin(angle), vx: 0, vy: 0 }
  })
}

function stepPhysics(nodeArr, edgeArr, W, H) {
  const n = nodeArr.length
  const fx = new Array(n).fill(0)
  const fy = new Array(n).fill(0)

  for (let i = 0; i < n; i++) {
    for (let j = i + 1; j < n; j++) {
      const dx = nodeArr[j].x - nodeArr[i].x || 0.01
      const dy = nodeArr[j].y - nodeArr[i].y || 0.01
      const d2 = dx * dx + dy * dy
      const d = Math.sqrt(d2)
      const f = K_REP / d2
      fx[i] -= (f * dx) / d
      fy[i] -= (f * dy) / d
      fx[j] += (f * dx) / d
      fy[j] += (f * dy) / d
    }
  }

  const idxMap = {}
  nodeArr.forEach((nd, i) => {
    idxMap[nd.id] = i
  })
  edgeArr.forEach(({ source, target }) => {
    const i = idxMap[source],
      j = idxMap[target]
    if (i == null || j == null) return
    const dx = nodeArr[j].x - nodeArr[i].x
    const dy = nodeArr[j].y - nodeArr[i].y
    const d = Math.sqrt(dx * dx + dy * dy) || 1
    const f = K_ATT * (d - REST)
    fx[i] += (f * dx) / d
    fy[i] += (f * dy) / d
    fx[j] -= (f * dx) / d
    fy[j] -= (f * dy) / d
  })

  nodeArr.forEach((node, i) => {
    fx[i] += GRAVITY * (W / 2 - node.x)
    fy[i] += GRAVITY * (H / 2 - node.y)
  })

  return nodeArr.map((node, i) => {
    let vx = (node.vx + fx[i]) * DAMPING
    let vy = (node.vy + fy[i]) * DAMPING
    const sp = Math.sqrt(vx * vx + vy * vy)
    if (sp > MAX_V) {
      vx = (vx / sp) * MAX_V
      vy = (vy / sp) * MAX_V
    }
    return {
      ...node,
      x: Math.max(24, Math.min(W - 24, node.x + vx)),
      y: Math.max(24, Math.min(H - 24, node.y + vy)),
      vx,
      vy,
    }
  })
}

/* ────────────────────────────────────────────────────────────
 * 캔버스 렌더러
 *  - 노드: vitalityScore → 색상(초록/노랑/빨강)
 *  - 엣지: crystallized → 실선/점선, strength → 두께·투명도
 * ──────────────────────────────────────────────────────────── */
function drawGraph(ctx, nodeArr, edgeArr, hoveredId, W, H) {
  ctx.clearRect(0, 0, W, H)
  const rootStyle = getComputedStyle(document.documentElement)
  const idxMap = {}
  nodeArr.forEach((n, i) => {
    idxMap[n.id] = i
  })

  // ── 엣지 ──
  edgeArr.forEach((edge) => {
    const s = nodeArr[idxMap[edge.source]]
    const t = nodeArr[idxMap[edge.target]]
    if (!s || !t) return

    const strength = edge.strength ?? 0
    const lineWidth = 0.5 + strength * 4 // 0.5px ~ 4.5px
    const alpha = 0.3 + strength * 0.65 // 0.3 ~ 0.95 (다크 배경에서도 보이도록)

    ctx.save()
    ctx.beginPath()
    ctx.moveTo(s.x, s.y)
    ctx.lineTo(t.x, t.y)
    ctx.strokeStyle = accentRgba(alpha)
    ctx.lineWidth = lineWidth

    if (!edge.crystallized) {
      // 미확정(Crystallize 전): 점선
      ctx.setLineDash([5, 4])
    } else {
      ctx.setLineDash([])
    }

    ctx.stroke()
    ctx.restore()
  })

  // ── 노드 ──
  nodeArr.forEach((node) => {
    const r = Math.max(10, Math.min(22, 10 + node.degree * 2.5))
    const isHov = hoveredId === node.id

    // Vitality Score 기반 색상 (없으면 태그 기반)
    const vitality = node.vitalityScore ?? 0.5
    const useVitality = node.vitalityScore !== undefined && node.vitalityScore !== null
    const baseColor = useVitality ? vitalityColor(vitality) : tagColor(node.tags?.[0])

    if (isHov) {
      ctx.shadowColor = baseColor
      ctx.shadowBlur = 20
    }

    ctx.beginPath()
    ctx.arc(node.x, node.y, r, 0, Math.PI * 2)
    ctx.fillStyle = isHov ? baseColor : baseColor + 'cc'
    ctx.fill()
    ctx.strokeStyle = rootStyle.getPropertyValue('--card').trim() || '#fff'
    ctx.lineWidth = 2
    ctx.stroke()
    ctx.shadowBlur = 0

    // 레이블
    const maxChars = isHov ? 20 : 10
    const label = node.title.length > maxChars ? node.title.slice(0, maxChars) + '…' : node.title
    ctx.font = isHov ? 'bold 12px Pretendard, sans-serif' : '11px Pretendard, sans-serif'
    ctx.fillStyle = rootStyle.getPropertyValue('--text').trim() || '#1a2e26'
    ctx.textAlign = 'center'
    ctx.fillText(label, node.x, node.y + r + 14)
  })
}

/* ────────────────────────────────────────────────────────────
 * 메인 컴포넌트
 * ──────────────────────────────────────────────────────────── */
export default function GraphPage() {
  const navigate = useNavigate()
  const canvasRef = useRef(null)
  const nodesRef = useRef([])
  const edgesRef = useRef([])
  const rafRef = useRef(null)
  const draggingRef = useRef(null)
  const hoveredRef = useRef(null)
  const tickRef = useRef(0)
  const mouseDownPos = useRef(null)

  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [nodeCount, setNodeCount] = useState(0)
  const [edgeCount, setEdgeCount] = useState(0)
  const [hovered, setHovered] = useState(null)
  const [tooltip, setTooltip] = useState(null) // { x, y, node }

  const size = useCallback(() => {
    const el = canvasRef.current?.parentElement
    if (!el) return { W: 800, H: 600 }
    return { W: el.clientWidth, H: el.clientHeight }
  }, [])

  // ── 데이터 로드 ──
  useEffect(() => {
    notesApi
      .getGraph()
      .then((res) => {
        const { nodes, edges } = res.data.data
        const { W, H } = size()
        nodesRef.current = initPhysics(nodes, edges, W, H)
        edgesRef.current = edges
        setNodeCount(nodes.length)
        setEdgeCount(edges.length)
      })
      .catch(() => setError('그래프를 불러오지 못했습니다.'))
      .finally(() => setLoading(false))
  }, [size])

  // ── 애니메이션 루프 ──
  useEffect(() => {
    if (loading) return
    const canvas = canvasRef.current
    if (!canvas) return
    const ctx = canvas.getContext('2d')

    function loop() {
      const { W, H } = size()
      if (canvas.width !== W || canvas.height !== H) {
        canvas.width = W
        canvas.height = H
      }

      const shouldStep = tickRef.current < 200 || tickRef.current % 50 === 0
      if (shouldStep && nodesRef.current.length > 0) {
        nodesRef.current = stepPhysics(nodesRef.current, edgesRef.current, W, H)
      }
      tickRef.current++

      drawGraph(ctx, nodesRef.current, edgesRef.current, hoveredRef.current, W, H)
      rafRef.current = requestAnimationFrame(loop)
    }
    rafRef.current = requestAnimationFrame(loop)
    return () => cancelAnimationFrame(rafRef.current)
  }, [loading, size])

  // ── 히트 테스트 ──
  function hitNode(x, y) {
    for (const node of nodesRef.current) {
      const r = Math.max(10, Math.min(22, 10 + node.degree * 2.5))
      const dx = node.x - x,
        dy = node.y - y
      if (dx * dx + dy * dy < (r + 4) * (r + 4)) return node
    }
    return null
  }

  // ── 마우스 이벤트 ──
  function onMouseMove(e) {
    const rect = canvasRef.current.getBoundingClientRect()
    const x = e.clientX - rect.left
    const y = e.clientY - rect.top

    if (draggingRef.current) {
      draggingRef.current.x = x
      draggingRef.current.y = y
      draggingRef.current.vx = 0
      draggingRef.current.vy = 0
      return
    }

    const node = hitNode(x, y)
    hoveredRef.current = node?.id ?? null
    setHovered(node?.id ?? null)
    setTooltip(node ? { x: node.x, y: node.y, node } : null)
    canvasRef.current.style.cursor = node ? 'pointer' : 'default'
  }

  function onMouseDown(e) {
    const rect = canvasRef.current.getBoundingClientRect()
    const x = e.clientX - rect.left
    const y = e.clientY - rect.top
    mouseDownPos.current = { x, y }
    const node = hitNode(x, y)
    if (node) draggingRef.current = node
  }

  function onMouseUp(e) {
    if (!draggingRef.current) return
    const rect = canvasRef.current.getBoundingClientRect()
    const x = e.clientX - rect.left
    const y = e.clientY - rect.top
    const dx = x - (mouseDownPos.current?.x ?? x)
    const dy = y - (mouseDownPos.current?.y ?? y)
    // 드래그 거리 5px 미만이면 클릭으로 간주
    if (Math.abs(dx) < 5 && Math.abs(dy) < 5) {
      navigate(`/notes/${draggingRef.current.id}`)
    }
    draggingRef.current = null
    mouseDownPos.current = null
  }

  function onMouseLeave() {
    draggingRef.current = null
    hoveredRef.current = null
    setHovered(null)
    setTooltip(null)
  }

  // ── 범례 데이터 ──
  const legend = [
    { color: 'var(--success)', label: '활력 높음 (≥0.7)' },
    { color: 'var(--warning)', label: '주의 (0.4~0.7)' },
    { color: 'var(--danger)', label: 'Knot Decay 위험' },
  ]

  return (
    <div className="graph-page">
      <Navbar />

      <div className="graph-header">
        <div className="graph-header-left">
          <h2 className="graph-title">🗺️ 지식 그래프</h2>
          <span className="graph-meta">
            노트 {nodeCount}개 · 연결 {edgeCount}개
          </span>
        </div>
        <span className="graph-hint">클릭 → 메모 이동 &nbsp;·&nbsp; 드래그 → 위치 조정</span>
      </div>

      <div className="graph-canvas-wrap">
        {loading && <Spinner />}
        {error && <p className="graph-error">{error}</p>}
        {!loading && nodeCount === 0 && !error && (
          <div className="graph-empty">
            <p>연결된 메모가 없습니다.</p>
            <p>메모를 작성하고 링크를 연결해 보세요!</p>
          </div>
        )}

        <canvas
          ref={canvasRef}
          className="graph-canvas"
          onMouseMove={onMouseMove}
          onMouseDown={onMouseDown}
          onMouseUp={onMouseUp}
          onMouseLeave={onMouseLeave}
        />

        {/* 툴팁 */}
        {tooltip &&
          (() => {
            const { x, y, node } = tooltip
            const vitality = node.vitalityScore ?? 0.5
            return (
              <div className="graph-tooltip" style={{ left: x + 18, top: y - 10 }}>
                <strong>{node.title}</strong>
                <div className="graph-tooltip-vitality">
                  <span className="vitality-dot" style={{ background: vitalityColor(vitality) }} />
                  활력 {Math.round(vitality * 100)}%
                </div>
                {node.tags?.length > 0 && (
                  <div className="graph-tooltip-tags">
                    {node.tags.map((t) => (
                      <span key={t} className="graph-tooltip-tag">
                        #{t}
                      </span>
                    ))}
                  </div>
                )}
                <span className="graph-tooltip-degree">연결 {node.degree}개</span>
              </div>
            )
          })()}

        {/* 범례 */}
        {!loading && nodeCount > 0 && (
          <div className="graph-legend">
            <div className="graph-legend-title">Knot Vitality</div>
            {legend.map((l) => (
              <div key={l.label} className="graph-legend-item">
                <span className="graph-legend-dot" style={{ background: l.color }} />
                <span className="graph-legend-label">{l.label}</span>
              </div>
            ))}
            <div className="graph-legend-divider" />
            <div className="graph-legend-item">
              <span className="graph-legend-line solid" />
              <span className="graph-legend-label">확정된 연결</span>
            </div>
            <div className="graph-legend-item">
              <span className="graph-legend-line dashed" />
              <span className="graph-legend-label">미확정 연결</span>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
