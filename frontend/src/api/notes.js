import api from './axios.js'

export const notesApi = {
  // ── 기본 CRUD ─────────────────────────────────────────────
  list: (page = 0, size = 20) => api.get('/notes', { params: { page, size } }),

  get: (id) => api.get(`/notes/${id}`),

  create: (title, content) => api.post('/notes', { title, content }),

  update: (id, title, content) => api.patch(`/notes/${id}`, { title, content }),

  delete: (id) => api.delete(`/notes/${id}`),

  // ── 링크 관리 ──────────────────────────────────────────────
  getLinks: (id) => api.get(`/notes/${id}/links`),

  addLink: (id, targetNoteId, intent = null) =>
    api.post(`/notes/${id}/links`, { targetNoteId, intent }),

  removeLink: (id, targetNoteId) => api.delete(`/notes/${id}/links/${targetNoteId}`),

  // ── 태그 ──────────────────────────────────────────────────
  addTag: (noteId, tagId) => api.post(`/notes/${noteId}/tags`, null, { params: { tagId } }),

  removeTag: (noteId, tagId) => api.delete(`/notes/${noteId}/tags/${tagId}`),

  // ── 지식 그래프 ────────────────────────────────────────────
  getGraph: () => api.get('/notes/graph'),

  // ── 스마트 연결 추천 ───────────────────────────────────────
  getRecommendations: (id, topN = 5) =>
    api.get(`/notes/${id}/recommendations`, { params: { topN } }),

  // ── Crystallize Mode ───────────────────────────────────────
  getPendingLinks: (noteId) => api.get(`/notes/${noteId}/links/pending`),

  crystallizeLink: (noteId, linkId, summary) =>
    api.patch(`/notes/${noteId}/links/${linkId}/crystallize`, { summary }),

  // ── Knot Decay ─────────────────────────────────────────────
  getDecayAlerts: () => api.get('/notes/decay-alerts'),

  // ── 버전 이력 (Phase 4) ────────────────────────────────────
  getVersions: (noteId) => api.get(`/notes/${noteId}/versions`),

  restoreVersion: (noteId, versionId) => api.post(`/notes/${noteId}/versions/${versionId}/restore`),

  // ── AI 태그 추천 (Phase 4) ─────────────────────────────────
  suggestTags: (noteId, topN = 5) =>
    api.get(`/notes/${noteId}/tag-suggestions`, { params: { topN } }),

  // ── Note Pinning (Phase 5) ─────────────────────────────────
  pin: (noteId) => api.post(`/notes/${noteId}/pin`),

  unpin: (noteId) => api.delete(`/notes/${noteId}/pin`),

  // ── Low-Vitality (Phase 5) ─────────────────────────────────
  getLowVitality: (threshold = 0.3) => api.get('/notes/low-vitality', { params: { threshold } }),

  // ── 벌크 작업 (Phase 5) ────────────────────────────────────
  bulkDelete: (noteIds) => api.post('/notes/bulk/delete', { noteIds }),

  bulkAddTag: (noteIds, tagId) => api.post('/notes/bulk/tag', { noteIds, tagId }),

  // ── 웹 클리핑 (Phase 9) ───────────────────────────────────
  clip: (url) => api.post('/notes/clip', { url }),

  // ── AI 요약 (Phase 9) ──────────────────────────────────────
  summarize: (noteId) => api.post(`/notes/${noteId}/summarize`),

  // ── 노트 공유 (Phase 9) ────────────────────────────────────
  share: (noteId, expiresInDays = null) =>
    api.post(`/notes/${noteId}/share`, null, {
      params: expiresInDays ? { expiresInDays } : {},
    }),

  unshare: (noteId) => api.delete(`/notes/${noteId}/share`),
}

// ── 공개 공유 노트 조회 (인증 불필요) ─────────────────────────
export const shareApi = {
  get: (shareToken) => api.get(`/share/${shareToken}`),
}

// ── 통계 / Graph Insights (Phase 3~4) ─────────────────────────
export const statsApi = {
  get: () => api.get('/stats'),
  getGraphInsights: () => api.get('/stats/graph-insights'),
}

// ── Activity Feed (Phase 5) ────────────────────────────────────
export const activityApi = {
  list: (limit = 30) => api.get('/activity', { params: { limit } }),
}

// ── 내보내기 (Phase 4) ─────────────────────────────────────────
export const exportApi = {
  download: (format = 'json') =>
    api.get('/export', {
      params: { format },
      responseType: 'blob',
    }),
}
