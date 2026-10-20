export default function DraftProgress({ count, total = 11 }) {
  const pct = Math.min(100, Math.round((count / total) * 100))
  return (
    <div>
      <div className="flex items-baseline justify-between">
        <p className="display text-lg tracking-wide" aria-live="polite">{count} / {total} PLAYERS SELECTED</p>
        <span className="text-xs text-mute">{pct}%</span>
      </div>
      <div className="mt-2 h-2 overflow-hidden rounded-full bg-edge" role="progressbar" aria-valuemin={0} aria-valuemax={total} aria-valuenow={count} aria-label="Draft progress">
        <div className="h-full rounded-full bg-gold transition-all duration-300" style={{ width: `${pct}%` }} />
      </div>
    </div>
  )
}
