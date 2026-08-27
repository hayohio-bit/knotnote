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
    icon: '🕸️',
    title: '지식 그래프',
    desc: '연결된 메모를 한눈에 보는 그래프 뷰. 방치된 지식은 Vitality 점수로 알려드려요.',
  },
  {
    icon: '🤖',
    title: 'AI 시맨틱 검색',
    desc: '키워드가 기억 안 나도 괜찮아요. AI가 의미 기반으로 관련 메모를 찾아줍니다.',
  },
  {
    icon: '✨',
    title: 'AI 요약 · 웹 클리핑',
    desc: '긴 메모는 AI가 요약하고, URL 하나로 웹의 글을 내 노트로 가져옵니다.',
  },
]

const HOW_STEPS = [
  {
    step: '1',
    title: '메모를 쓰면',
    desc: '떠오르는 대로 기록하세요. 태그와 템플릿이 정리를 도와줍니다.',
  },
  {
    step: '2',
    title: 'AI가 연결하고',
    desc: 'AI가 의미가 닿는 노트를 찾아 연결을 추천합니다.',
  },
  {
    step: '3',
    title: '인사이트가 보입니다',
    desc: '지식 그래프에서 흩어진 생각이 하나의 맥락으로 이어집니다.',
  },
]

const TECH_STACK = [
  'Java 17',
  'Spring Boot 3',
  'React 18 + Vite',
  'MySQL 8',
  'JWT 인증',
  'Gemini API',
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
                <Link to="/dashboard" className="btn btn-ghost">
                  대시보드
                </Link>
                <button onClick={handleLogout} className="btn btn-primary">
                  로그아웃
                </button>
              </>
            ) : (
              <>
                <Link to="/login" className="btn btn-ghost">
                  로그인
                </Link>
                <Link to="/signup" className="btn btn-primary">
                  무료로 시작
                </Link>
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
              메모를 연결하면
              <br />
              <span className="text-accent">인사이트가 보인다</span>
            </h1>
            <p className="hero-desc">
              AI가 메모를 분석하고 연관 노트를 추천해드려요.
              <br />
              복잡한 생각도 KnotNote와 함께라면 명확해집니다.
            </p>
            <div className="hero-cta">
              {user ? (
                <Link to="/dashboard" className="btn btn-primary btn-lg">
                  대시보드로 가기 →
                </Link>
              ) : (
                <>
                  <Link to="/signup" className="btn btn-primary btn-lg">
                    지금 무료로 시작 →
                  </Link>
                  <Link to="/login" className="btn btn-ghost btn-lg">
                    기존 계정 로그인
                  </Link>
                </>
              )}
            </div>
          </div>

          {/* 미니 지식 그래프 목업 */}
          <div className="hero-visual">
            <div className="hero-graph">
              <svg className="hero-graph-lines" viewBox="0 0 380 330" aria-hidden="true">
                <line x1="120" y1="70" x2="270" y2="185" />
                <line x1="110" y1="70" x2="115" y2="265" />
                <line className="dashed" x1="270" y1="195" x2="130" y2="265" />
                <line className="faint" x1="130" y1="60" x2="330" y2="100" />
                <line className="faint" x1="290" y1="205" x2="255" y2="302" />
                <line className="faint" x1="333" y1="101" x2="290" y2="185" />
              </svg>
              <div className="hero-node hero-node-main">
                <div className="hero-node-title">📝 React 상태관리 정리</div>
                <div className="hero-node-tags">
                  <span>#react</span>
                  <span>#패턴</span>
                </div>
              </div>
              <div className="hero-node hero-node-b">Redux vs Zustand</div>
              <div className="hero-node hero-node-c">서버 상태와 캐싱</div>
              <span className="hero-dot hero-dot-a" />
              <span className="hero-dot hero-dot-b" />
              <div className="hero-ai-chip">✨ AI 추천 연결</div>
            </div>
          </div>
        </div>
      </section>

      {/* ── 사용 흐름 ── */}
      <section className="how">
        <div className="container how-grid">
          {HOW_STEPS.map((s) => (
            <div key={s.step} className="how-item neu-card">
              <div className="how-step">{s.step}</div>
              <div className="how-title">{s.title}</div>
              <div className="how-desc">{s.desc}</div>
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
            {user ? (
              <>
                <h2>이어서 기록해보세요</h2>
                <p>작성한 메모가 대시보드에서 기다리고 있습니다</p>
                <Link to="/dashboard" className="btn btn-primary btn-lg">
                  대시보드로 가기 →
                </Link>
              </>
            ) : (
              <>
                <h2>지금 바로 시작해보세요</h2>
                <p>회원가입은 이메일 하나로 충분합니다</p>
                <Link to="/signup" className="btn btn-primary btn-lg">
                  무료로 시작하기 →
                </Link>
              </>
            )}
          </div>
        </div>
      </section>

      {/* ── 기술 스택 ── */}
      <section className="tech-strip">
        <div className="container">
          <p className="tech-strip-label">Built with</p>
          <div className="tech-badges">
            {TECH_STACK.map((t) => (
              <span key={t} className="tech-badge">
                {t}
              </span>
            ))}
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
