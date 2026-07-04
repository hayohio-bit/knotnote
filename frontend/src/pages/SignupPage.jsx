import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../store/AuthContext.jsx'
import './AuthPage.css'

export default function SignupPage() {
  const { signup } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ email: '', password: '', nickname: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleChange = (e) => setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }))

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')

    if (form.password.length < 8) {
      setError('비밀번호는 8자 이상이어야 합니다.')
      return
    }

    setLoading(true)
    try {
      await signup(form.email, form.password, form.nickname)
      navigate('/login', { state: { message: '회원가입 완료! 로그인해주세요.' } })
    } catch (err) {
      setError(err.response?.data?.message || '회원가입 중 오류가 발생했습니다.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card neu-card">
        <div className="auth-logo">🪢 KnotNote</div>
        <h1 className="auth-title">회원가입</h1>
        <p className="auth-subtitle">지금 무료로 시작하세요.</p>

        <form onSubmit={handleSubmit} className="auth-form">
          <div className="form-group">
            <label htmlFor="nickname">닉네임</label>
            <input
              id="nickname"
              name="nickname"
              type="text"
              className="input"
              placeholder="닉네임 입력"
              value={form.nickname}
              onChange={handleChange}
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="email">이메일</label>
            <input
              id="email"
              name="email"
              type="email"
              className="input"
              placeholder="your@email.com"
              value={form.email}
              onChange={handleChange}
              required
              autoComplete="email"
            />
          </div>

          <div className="form-group">
            <label htmlFor="password">
              비밀번호 <span className="text-secondary">(8자 이상)</span>
            </label>
            <input
              id="password"
              name="password"
              type="password"
              className="input"
              placeholder="비밀번호 입력 (8자 이상, 숫자·특수문자 포함)"
              value={form.password}
              onChange={handleChange}
              required
              autoComplete="new-password"
            />
          </div>

          {error && <div className="auth-error">{error}</div>}

          <button type="submit" className="btn btn-primary auth-submit" disabled={loading}>
            {loading ? '가입 중...' : '회원가입'}
          </button>
        </form>

        <p className="auth-footer">
          이미 계정이 있으신가요?{' '}
          <Link to="/login" className="text-accent">
            로그인
          </Link>
        </p>
      </div>
    </div>
  )
}
