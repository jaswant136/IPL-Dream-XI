import { draftTitle, formatDate } from '../../utils/formatters'
import RatingCard from './RatingCard'
import PlayingXI from './PlayingXI'

export default function DraftResult({ result, actions, hideChallengeCode = false }) {
  const picks = result.picks.map((p) => ({ ...p, hideTeam: Boolean(result.team) }))
  return (
    <div className="grid gap-6 lg:grid-cols-2">
      <div className="space-y-4">
        <div>
          <p className="chip mb-2 w-fit">{result.modeLabel}</p>
          <h2 className="display text-3xl">{draftTitle(result)}</h2>
          <p className="text-sm text-mute">
            Completed {formatDate(result.completedAt)}
            {!hideChallengeCode && result.challengeCode && <> · Challenge <span className="font-semibold text-snow">{result.challengeCode}</span></>}
          </p>
        </div>
        <RatingCard result={result} />
        {actions}
      </div>
      <PlayingXI picks={picks} squadSize={picks.length || 11} readOnly ratingsHidden={false} />
    </div>
  )
}
