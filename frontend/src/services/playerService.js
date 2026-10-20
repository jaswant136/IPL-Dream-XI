import { ROLE_LABELS } from '../utils/constants'

/** Pure client-side helpers over players the server already returned (filtering is a view concern only). */
export function filterPlayers(players, { role = 'ALL', query = '', hidePicked = false, pickedIds = new Set() } = {}) {
  const q = query.trim().toLowerCase()
  return players.filter((p) => {
    if (role !== 'ALL' && p.role !== role) return false
    if (q && !p.name.toLowerCase().includes(q) && !p.country.toLowerCase().includes(q)) return false
    if (hidePicked && pickedIds.has(p.id)) return false
    return true
  })
}

export function roleCounts(players) {
  const counts = { ALL: players.length }
  Object.keys(ROLE_LABELS).forEach((r) => (counts[r] = players.filter((p) => p.role === r).length))
  return counts
}
