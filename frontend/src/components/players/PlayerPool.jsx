import { useMemo, useState } from 'react'
import { Search, Users } from 'lucide-react'
import { filterPlayers, roleCounts } from '../../services/playerService'
import EmptyState from '../common/EmptyState'
import ErrorState from '../common/ErrorState'
import { PlayerListSkeleton } from '../common/Skeleton'
import PlayerCard from './PlayerCard'
import RoleFilter from './RoleFilter'

/** Never renders a silent blank list: loading, error and empty are each an explicit visible state. */
export default function PlayerPool({ players, loading, error, onRetry, pickedIds, ratingsHidden, onAdd, busy, blockReason, rules }) {
  const [role, setRole] = useState('ALL')
  const [query, setQuery] = useState('')
  const [hidePicked, setHidePicked] = useState(false)

  const counts = useMemo(() => roleCounts(players ?? []), [players])
  const visible = useMemo(
    () => filterPlayers(players ?? [], { role, query, hidePicked, pickedIds }),
    [players, role, query, hidePicked, pickedIds],
  )

  if (loading) return <PlayerListSkeleton />
  if (error) return <ErrorState title="Could not load the player pool" message={error} onRetry={onRetry} />
  if (!players || players.length === 0) {
    // Diagnostic breadcrumb for developers; contains no secrets.
    console.error('[IPL Draft Club] The server returned an empty player pool for this draft.')
    return (
      <ErrorState title="No players available"
        message="The server returned an empty player pool for this draft. This should not happen - please retry, or start a new draft."
        onRetry={onRetry} />
    )
  }

  return (
    <section aria-label="Player pool" className="space-y-3">
      <div className="relative">
        <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-mute" aria-hidden="true" />
        <input className="input pl-9" placeholder="Search by name or country" value={query}
          onChange={(e) => setQuery(e.target.value)} aria-label="Search players" />
      </div>
      <RoleFilter value={role} onChange={setRole} counts={counts} />
      <label className="flex items-center gap-2 text-xs text-mute">
        <input type="checkbox" checked={hidePicked} onChange={(e) => setHidePicked(e.target.checked)} className="accent-[#F5C84B]" />
        Hide drafted players
      </label>
      {visible.length === 0 ? (
        <EmptyState icon={Users} title="No matching players" message="Try a different role filter or search term." />
      ) : (
        <ul className="max-h-[62vh] space-y-2 overflow-y-auto pr-1" aria-label={`${visible.length} players`}>
          {visible.map((p) => (
            <PlayerCard key={p.id} player={p} picked={pickedIds.has(p.id)} disabled={busy}
              blockedReason={blockReason?.(p)} ratingsHidden={ratingsHidden} onAdd={onAdd} />
          ))}
        </ul>
      )}
      {rules && <p className="text-[11px] text-mute">Max {rules.maxOverseas} overseas · at least {rules.minWicketkeepers} WK, {rules.minBatters} batter, {rules.minBowlers} bowlers{rules.minAllRounders ? `, ${rules.minAllRounders} all-rounder` : ''}.</p>}
    </section>
  )
}
