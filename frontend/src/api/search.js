import api from './axios.js'

export const searchApi = {
  keyword: (q, page = 0, size = 20) => api.get('/search', { params: { q, page, size } }),
}
