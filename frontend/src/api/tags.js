import api from './axios.js'

export const tagsApi = {
  list: () => api.get('/tags'),
  create: (name) => api.post('/tags', { name }),
  delete: (id) => api.delete(`/tags/${id}`),
}
