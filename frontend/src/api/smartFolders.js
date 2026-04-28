import api from './axios.js'

export const smartFoldersApi = {
  list: () => api.get('/smart-folders'),

  create: (payload) => api.post('/smart-folders', payload),

  update: (id, payload) => api.patch(`/smart-folders/${id}`, payload),

  delete: (id) => api.delete(`/smart-folders/${id}`),

  getNotes: (id) => api.get(`/smart-folders/${id}/notes`),
}
