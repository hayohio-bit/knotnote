import { createContext, useCallback, useContext, useState } from 'react'
import { authApi } from '../api/auth.js'
import { tokenStorage } from '../lib/token.js'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    // 새로고침 후에도 로그인 상태 유지 (토큰이 있으면 로그인 상태로 간주)
    return tokenStorage.getAccess() ? { loggedIn: true } : null
  })

  const login = useCallback(async (email, password) => {
    const { data } = await authApi.login(email, password)
    const { accessToken, refreshToken } = data.data
    tokenStorage.setAccess(accessToken)
    tokenStorage.setRefresh(refreshToken)
    setUser({ loggedIn: true, email })
    return data
  }, [])

  const signup = useCallback(async (email, password, nickname) => {
    const { data } = await authApi.signup(email, password, nickname)
    return data
  }, [])

  const logout = useCallback(() => {
    tokenStorage.clear()
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
