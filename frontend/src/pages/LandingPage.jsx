import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../store/AuthContext.jsx'
import './LandingPage.css'

const FEATURES = [
  {
    icon: '🔗',
    title: '양방향 링크',
    desc: '메모와 메모를 연결해 지식 그래프를 만들어요. 아이디어가 자연스럽게 연결됩니다.',
  },
  {
    icon: '🤖',
    title: 'AI 시맨틱 검색',
    desc: '키워드가 기억 안 나도 괜찮아요. AI가 의미 기반으로 관련 메모를 찾아줍니다.',
  },
  {
    icon: '🏷️',
    title: '스마트 태그',
    desc: '태그로 메모를 분류하고, 필터링으로 원하는 내용만 빠르게 모아보세요.',
  },
  {
    icon: '🔒',
    title: 'JWT 보안 인증',
    desc: '내 메모는 나만 볼 수 있어요. 안전한 토큰 인증으로 개인 정보를 보호합니다.',
  },
]

const STATS = [
  { value: '5',   label: '핵심 도메인' },
  { value: '20+', label: 'REST 엔드포인트' },
  { value: '7',   label: 'DB 테이블' },
  { value: '40',  label: '통합 테스트' },
]

export default function LandingPage() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/')
  }

  return (
    <div className="landing">

      {/* ── 네비 ── */}
      <header className="landing-nav">
        <div className="container landing-nav-inner">
          <span className="landing-logo">🪢 KnotNote</span>
          <div className="landing-nav-actions">
            {user ? (
              <>
                <Link to="/dashboard" className="btn btn-ghost">대시보드</Link>
                <button onClick={handleLogout} className="btn btn-primary">로그아웃</button>
              </>
            ) : (
              <>
                <Link to="/login"  className="btn btn-ghost">로그인</Link>
                <Link to="/signup" className="btn btn-primary">무료로 시작</Link>
              </>
            )}
          </div>
        </div>
      </header>

      {/* ── Hero ── */}
      <section className="hero">
        <div className="container hero-container">

          {/* 텍스트 영역 */}
          <div className="hero-text">
            <div className="hero-badge">✨ 2026 Beta</div>
            <h1 className="hero-title">
              메모를 연결하면<br />
              <span className="text-accent">인사이트가 보인다</span>
            </h1>
            <p className="hero-desc">
              AI가 메모를 분석하고 연관 노트를 추천해드려요.<br />
              복잡한 생각도 KnotNote와 함께라면 명확해집니다.
            </p>
            <div className="hero-cta">
              <Link to="/signup" className="btn btn-primary btn-lg">
                지금 무료로 시작 →
              </Link>
              <Link to="/login" className="btn btn-ghost btn-lg">
                기존 계정 로그인
              </Link>
            </div>
          </div>

          {/* 장식 카드 */}
          <div className="hero-visual">
            <div className="hero-card neu-card">
              <div className="hero-card-title">📝 오늘의 메모</div>
              <div className="hero-card-body">React 상태관리 패턴 정리...</div>
              <div className="hero-card-links">
                <span className="hero-link-badge">🔗 Redux 정리</span>
                <span className="hero-link-badge">🔗 Zustand 비교</span>
              </div>
            </div>
          </div>

        </div>
      </section>

      {/* ── Stats ── */}
      <section className="stats">
        <div className="container stats-grid">
          {STATS.map((s) => (
            <div key={s.label} className="stat-item neu-card">
              <div className="stat-value">{s.value}</div>
              <div className="stat-label">{s.label}</div>
            </div>
          ))}
        </div>
      </section>

      {/* ── Features ── */}
      <section className="features">
        <div className="container">
          <h2 className="section-title">왜 KnotNote인가요?</h2>
          <p className="section-desc">단순한 메모 앱을 넘어 지식을 연결하는 도구입니다</p>
          <div className="features-grid">
            {FEATURES.map((f) => (
              <div key={f.title} className="feature-card neu-card">
                <div className="feature-icon">{f.icon}</div>
                <h3 className="feature-title">{f.title}</h3>
                <p className="feature-desc">{f.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── CTA ── */}
      <section className="cta-section">
        <div className="container">
          <div className="cta-inner">
            <h2>지금 바로 시작해보세요</h2>
            <p>회원가입은 이메일 하나로 충분합니다</p>
            <Link to="/signup" className="btn btn-primary btn-lg">
              무료로 시작하기 →
            </Link>
          </div>
        </div>
      </section>

      {/* ── Footer ── */}
      <footer className="landing-footer">
        <div className="container">
          <span>© 2026 KnotNote · Made with ☕ by Serena</span>
        </div>
      </footer>

    </div>
  )
}
