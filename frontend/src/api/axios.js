import axios from 'axios'
import { tokenStorage } from '../lib/token.js'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

// ── 요청 인터셉터: 액세스 토큰 자동 첨부 ──────────────────────
api.interceptors.request.use((config) => {
  const token = tokenStorage.getAccess()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// ── 응답 인터셉터: 401 시 토큰 자동 갱신 ─────────────────────
let isRefreshing = false
let failedQueue = []

const isPublicPath = () => {
  const path = window.location.pathname
  return path.startsWith('/shared/') || path === '/login'
}

const redirectToLogin = () => {
  if (!isPublicPath()) {
    window.location.href = '/login'
  }
}

const processQueue = (error, token = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error)
    } else {
      prom.resolve(token)
    }
  })
  failedQueue = []
}

api.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config

    if (error.response?.status !== 401 || original._retry) {
      return Promise.reject(error)
    }

    if (isRefreshing) {
      return new Promise((resolve, reject) => {
        failedQueue.push({ resolve, reject })
      })
        .then((token) => {
          original._retry = true
          original.headers.Authorization = `Bearer ${token}`
          return api(original)
        })
        .catch((err) => Promise.reject(err))
    }

    original._retry = true
    isRefreshing = true

    const refreshToken = tokenStorage.getRefresh()
    if (!refreshToken) {
      isRefreshing = false
      processQueue(error, null)
      tokenStorage.clear()
      redirectToLogin()
      return Promise.reject(error)
    }

    try {
      const { data } = await axios.post('/api/auth/refresh', {
        refreshToken,
      })
      const newAccess = data.data.accessToken
      tokenStorage.setAccess(newAccess)
      // 백엔드가 refresh 시 refreshToken을 회전시키므로 새 토큰도 반드시 저장한다
      if (data.data.refreshToken) {
        tokenStorage.setRefresh(data.data.refreshToken)
      }
      processQueue(null, newAccess)
      original.headers.Authorization = `Bearer ${newAccess}`
      return api(original)
    } catch (refreshError) {
      processQueue(refreshError, null)
      tokenStorage.clear()
      redirectToLogin()
      return Promise.reject(refreshError)
    } finally {
      isRefreshing = false
    }
  },
)

export default api
