export function ScoreBar({ label, value }) {
  return (
    <div>
      <div className="flex justify-between text-xs"><span className="text-mute">{label}</span><span className="font-semibold tabular-nums">{value}</span></div>
      <div className="mt-1 h-1.5 overflow-hidden rounded-full bg-edge">
        <div className="h-full rounded-full bg-gold" style={{ width: `${Math.max(0, Math.min(100, value))}%` }} />
      </div>
    </div>
  )
}

export default function RatingCard({ result }) {
  return (
    <div className="panel p-6">
      <div className="flex items-center gap-5">
        <div className="flex h-28 w-28 shrink-0 flex-col items-center justify-center rounded-full border-4 border-gold">
          <span className="display text-5xl leading-none text-gold" aria-label={`Team rating ${result.teamRating}`}>{result.teamRating}</span>
          <span className="mt-1 text-[10px] uppercase tracking-widest text-mute">Team rating</span>
        </div>
        <div className="grid flex-1 grid-cols-1 gap-3">
          <ScoreBar label="Batting" value={result.battingScore} />
          <ScoreBar label="Bowling" value={result.bowlingScore} />
          <ScoreBar label="All-round depth" value={result.allRoundScore} />
          <ScoreBar label="Wicketkeeping" value={result.wicketkeepingScore} />
          <ScoreBar label="Role balance" value={result.roleBalanceScore} />
        </div>
      </div>
      <dl className="mt-5 grid grid-cols-3 gap-3 border-t border-edge pt-4 text-center">
        <div><dt className="text-[10px] uppercase tracking-widest text-mute">Overseas</dt><dd className="display text-2xl">{result.overseasPlayers}</dd></div>
        <div><dt className="text-[10px] uppercase tracking-widest text-mute">Points</dt><dd className="display text-2xl">{result.points}</dd></div>
        <div><dt className="text-[10px] uppercase tracking-widest text-mute">Time</dt><dd className="display text-2xl">{result.completionSeconds != null ? `${Math.floor(result.completionSeconds / 60)}:${String(result.completionSeconds % 60).padStart(2, '0')}` : '—'}</dd></div>
      </dl>
      <p className="mt-4 text-[11px] leading-relaxed text-mute">
        Scores are a transparent rating-weighted index computed by the server (not a match prediction). Ratings are illustrative game values.
      </p>
    </div>
  )
}
