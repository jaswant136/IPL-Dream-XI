import { X } from 'lucide-react'
import { ROLE_SHORT } from '../../utils/constants'
import RatingBadge from '../players/RatingBadge'

export default function PlayerSlot({ number, pick, ratingsHidden, onRemove, readOnly, busy }) {
  if (!pick) {
    return (
      <li className="flex items-center gap-3 rounded-xl border border-dashed border-edge px-3 py-2.5 text-mute" aria-label={`Slot ${number} empty`}>
        <span className="display w-6 text-lg">{number}</span>
        <span className="text-sm">Empty slot</span>
      </li>
    )
  }
  const { player } = pick
  return (
    <li className="flex items-center gap-3 rounded-xl border border-edge bg-ink/40 px-3 py-2.5">
      <span className="display w-6 text-lg text-gold">{pick.pickNumber}</span>
      <span className="chip w-12 justify-center">{ROLE_SHORT[player.role]}</span>
      <div className="min-w-0 flex-1">
        <p className="truncate text-sm font-semibold">{player.name}</p>
        <p className="truncate text-xs text-mute">{player.country}{player.overseas ? ' · Overseas' : ''}{player.teamCode && !pick.hideTeam ? ` · ${player.teamCode} ${player.season}` : ''}</p>
      </div>
      <RatingBadge rating={player.rating} hidden={ratingsHidden} />
      {!readOnly && (
        <button type="button" disabled={busy} onClick={() => onRemove(pick)} aria-label={`Remove ${player.name} from your XI`}
          className="flex h-8 w-8 items-center justify-center rounded-lg text-mute transition hover:bg-bad/10 hover:text-bad disabled:opacity-40">
          <X className="h-4 w-4" />
        </button>
      )}
    </li>
  )
}
