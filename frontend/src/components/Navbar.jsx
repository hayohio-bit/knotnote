import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../store/AuthContext.jsx'
import './Navbar.css'

export default function Navbar({ onNavigate }) {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const { pathname } = useLocation()

  const go = (to, e) => {
    if (e) e.preventDefault()
    if (onNavigate) onNavigate(to)
    else navigate(to)
  }

  const handleLogout = () => {
    logout()
    navigate('/')
  }

  const isActive = (path) => (pathname === path ? 'active' : '')

  return (
    <header className="navbar">
      <div className="container navbar-inner">
        <Link to="/" className="navbar-logo" onClick={(e) => go('/', e)}>
          🪢 KnotNote
        </Link>

        <nav className="navbar-links">
          {user ? (
            <>
              <Link
                to="/dashboard"
                className={`nav-link ${isActive('/dashboard')}`}
                onClick={(e) => go('/dashboard', e)}
              >
                내 메모
              </Link>
              <Link
                to="/graph"
                className={`nav-link ${isActive('/graph')}`}
                title="지식 그래프"
                onClick={(e) => go('/graph', e)}
              >
                🗺️ 그래프
              </Link>
              <Link
                to="/activity"
                className={`nav-link ${isActive('/activity')}`}
                title="활동 피드"
                onClick={(e) => go('/activity', e)}
              >
                📋 활동
              </Link>
              <Link
                to="/stats"
                className={`nav-link ${isActive('/stats')}`}
                title="통계 &amp; 내보내기"
                onClick={(e) => go('/stats', e)}
              >
                📊 통계
              </Link>
              <div className="navbar-divider" />
              <button onClick={handleLogout} className="btn btn-ghost btn-sm">
                로그아웃
              </button>
            </>
          ) : (
            <>
              <Link to="/login" className="nav-link" onClick={(e) => go('/login', e)}>
                로그인
              </Link>
              <Link
                to="/signup"
                className="btn btn-primary btn-sm"
                onClick={(e) => go('/signup', e)}
              >
                시작하기
              </Link>
            </>
          )}
        </nav>
      </div>
    </header>
  )
}
