import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { authApi } from '../api/auth.js'
import { AuthProvider } from '../store/AuthContext.jsx'
import LoginPage from './LoginPage.jsx'

vi.mock('../api/auth.js', () => ({
  authApi: {
    login: vi.fn(),
    signup: vi.fn(),
    refresh: vi.fn(),
  },
}))

function renderLogin() {
  return render(
    <AuthProvider>
      <MemoryRouter initialEntries={['/login']}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/dashboard" element={<div>대시보드 화면</div>} />
        </Routes>
      </MemoryRouter>
    </AuthProvider>,
  )
}

async function submitLogin(email, password) {
  await userEvent.type(screen.getByLabelText('이메일'), email)
  await userEvent.type(screen.getByLabelText('비밀번호'), password)
  await userEvent.click(screen.getByRole('button', { name: '로그인' }))
}

describe('LoginPage', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('로그인 성공 시 토큰을 저장하고 대시보드로 이동한다', async () => {
    authApi.login.mockResolvedValue({
      data: { data: { accessToken: 'access-1', refreshToken: 'refresh-1' } },
    })
    renderLogin()
    await submitLogin('user@test.local', 'pw1234!')

    expect(authApi.login).toHaveBeenCalledWith('user@test.local', 'pw1234!')
    expect(localStorage.getItem('kn_access')).toBe('access-1')
    expect(localStorage.getItem('kn_refresh')).toBe('refresh-1')
    expect(await screen.findByText('대시보드 화면')).toBeInTheDocument()
  })

  it('로그인 실패 시 서버 메시지를 표시하고 이동하지 않는다', async () => {
    authApi.login.mockRejectedValue({
      response: { data: { message: '계정이 잠겼습니다.' } },
    })
    renderLogin()
    await submitLogin('user@test.local', 'wrong')

    expect(await screen.findByText('계정이 잠겼습니다.')).toBeInTheDocument()
    expect(localStorage.getItem('kn_access')).toBeNull()
    expect(screen.queryByText('대시보드 화면')).not.toBeInTheDocument()
  })

  it('서버 메시지가 없으면 기본 오류 문구를 표시한다', async () => {
    authApi.login.mockRejectedValue(new Error('network'))
    renderLogin()
    await submitLogin('user@test.local', 'pw1234!')

    expect(await screen.findByText('이메일 또는 비밀번호가 올바르지 않습니다.')).toBeInTheDocument()
  })
})
