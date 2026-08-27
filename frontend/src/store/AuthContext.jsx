import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import { authApi } from '../api/auth.js'
import { tokenStorage } from '../lib/token.js'

const AuthContext = createContext(null)

const EMAIL_KEY = 'kn_email'

const readUser = () => {
  if (!tokenStorage.getAccess()) return null
  return { loggedIn: true, email: localStorage.getItem(EMAIL_KEY) || null }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    // 새로고침 후에도 로그인 상태 유지 (토큰이 있으면 로그인 상태로 간주)
    return readUser()
  })

  // 다른 탭에서 로그인·로그아웃하면 storage 이벤트로 상태를 동기화한다
  useEffect(() => {
    const handleStorage = () => {
      setUser(readUser())
    }
    window.addEventListener('storage', handleStorage)
    return () => window.removeEventListener('storage', handleStorage)
  }, [])

  const login = useCallback(async (email, password) => {
    const { data } = await authApi.login(email, password)
    const { accessToken, refreshToken } = data.data
    tokenStorage.setAccess(accessToken)
    tokenStorage.setRefresh(refreshToken)
    localStorage.setItem(EMAIL_KEY, email)
    setUser({ loggedIn: true, email })
    return data
  }, [])

  const signup = useCallback(async (email, password, nickname) => {
    const { data } = await authApi.signup(email, password, nickname)
    return data
  }, [])

  const logout = useCallback(() => {
    tokenStorage.clear()
    localStorage.removeItem(EMAIL_KEY)
    setUser(null)
  }, [])

  return (
    <AuthContext.Provider value={{ user, login, signup, logout }}>{children}</AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
