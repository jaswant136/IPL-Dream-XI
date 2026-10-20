import { get } from './apiClient'

export const fetchLeaderboard = ({ page = 0, size = 20, sort = 'points' } = {}) =>
  get('/leaderboard', { params: { page, size, sort } })
