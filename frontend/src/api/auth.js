import axios from 'axios'

// 인증 API는 토큰 없이 직접 호출
const plain = axios.create({ baseURL: '/api', timeout: 10000 })

export const authApi = {
  signup: (email, password, nickname) =>
    plain.post('/auth/signup', { email, password, nickname }),

  login: (email, password) =>
    plain.post('/auth/login', { email, password }),

  refresh: (refreshToken) =>
    plain.post('/auth/refresh', { refreshToken }),
}
