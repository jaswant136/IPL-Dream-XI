import DraftProgress from './DraftProgress'
import PlayerSlot from './PlayerSlot'

export default function PlayingXI({ picks, squadSize = 11, ratingsHidden, onRemove, busy, overseasCount, maxOverseas, readOnly = false }) {
  const byNumber = new Map(picks.map((p) => [p.pickNumber, p]))
  return (
    <section aria-label="Your playing XI" className="space-y-4">
      <DraftProgress count={picks.length} total={squadSize} />
      {maxOverseas != null && (
        <p className="text-xs text-mute">
          Overseas: <span className={overseasCount >= maxOverseas ? 'font-semibold text-bad' : 'font-semibold text-snow'}>{overseasCount} / {maxOverseas}</span>
        </p>
      )}
      <ol className="space-y-2">
        {Array.from({ length: squadSize }, (_, i) => i + 1).map((n) => (
          <PlayerSlot key={n} number={n} pick={byNumber.get(n)} ratingsHidden={ratingsHidden} onRemove={onRemove} busy={busy} readOnly={readOnly} />
        ))}
      </ol>
    </section>
  )
}
