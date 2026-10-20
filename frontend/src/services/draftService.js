import { del, get, post, put } from './apiClient'

export const startDraft = (mode, idempotencyKey) =>
  post('/drafts/start', { mode }, idempotencyKey ? { headers: { 'Idempotency-Key': idempotencyKey } } : undefined)
export const fetchDrafts = ({ page = 0, size = 10, status } = {}) =>
  get('/drafts', { params: { page, size, ...(status ? { status } : {}) } })
export const fetchDraft = (id) => get(`/drafts/${id}`)
export const addPick = (id, playerSeasonId) => post(`/drafts/${id}/picks`, { playerSeasonId })
export const removePick = (id, pickId) => del(`/drafts/${id}/picks/${pickId}`)
export const completeDraft = (id) => post(`/drafts/${id}/complete`)
export const fetchResult = (id) => get(`/drafts/${id}/result`)
export const deleteDraft = (id) => del(`/drafts/${id}`)
export const shareDraft = (id) => post(`/drafts/${id}/share`)
export const revokeShare = (id) => del(`/drafts/${id}/share`)
export const fetchShared = (token) => get(`/shared/${encodeURIComponent(token)}`)
export const fetchMe = () => get('/me')
export const updateProfile = (displayName) => put('/me/profile', { displayName })
export const fetchStatistics = () => get('/me/statistics')
