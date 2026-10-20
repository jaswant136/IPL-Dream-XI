import { Check, Plus } from 'lucide-react'
import { ROLE_SHORT } from '../../utils/constants'
import RatingBadge from './RatingBadge'

export default function PlayerCard({ player, picked, disabled, blockedReason, ratingsHidden, onAdd }) {
  const unavailable = picked || disabled
  return (
    <li className={`flex items-center gap-3 rounded-xl border px-3 py-2.5 transition ${
      picked ? 'border-gold/30 bg-gold/5' : 'border-edge bg-ink/40 hover:border-edge/80'}`}>
      <span className="chip w-12 justify-center">{ROLE_SHORT[player.role]}</span>
      <div className="min-w-0 flex-1">
        <p className="truncate text-sm font-semibold">{player.name}</p>
        <p className="truncate text-xs text-mute">
          {player.country}
          {player.overseas && <span className="ml-2 rounded bg-bad/10 px-1.5 py-0.5 text-[10px] font-semibold uppercase text-bad">Overseas</span>}
        </p>
        {blockedReason && !picked && <p className="mt-0.5 text-[11px] text-bad/90">{blockedReason}</p>}
      </div>
      <RatingBadge rating={player.rating} hidden={ratingsHidden} />
      <button type="button" disabled={unavailable} onClick={() => onAdd(player)}
        aria-label={picked ? `${player.name} already drafted` : `Add ${player.name} to your XI`}
        className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-lg transition ${
          picked ? 'bg-gold/20 text-gold' : 'bg-gold text-ink hover:brightness-110 disabled:opacity-40'}`}>
        {picked ? <Check className="h-4 w-4" /> : <Plus className="h-4 w-4" />}
      </button>
    </li>
  )
}
