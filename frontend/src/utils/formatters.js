export function formatDate(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' })
}
export function formatDuration(seconds) {
  if (seconds == null) return '—'
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return m > 0 ? `${m}m ${s}s` : `${s}s`
}
export function modeLabel(mode) {
  return { CLASSIC: 'Classic', EXPERT: 'Expert', CHALLENGE: 'Challenge', DREAM: 'Dream XI' }[mode] ?? mode
}
export function draftTitle(d) {
  if (!d) return ''
  if (!d.team) return 'Dream XI · multi-season'
  return `${d.team.name} · IPL ${d.season}`
}
