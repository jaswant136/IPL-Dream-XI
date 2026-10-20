import { Link, useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { fetchShared } from '../services/draftService'
import { draftTitle, formatDate } from '../utils/formatters'
import ErrorState from '../components/common/ErrorState'
import LoadingSpinner from '../components/common/LoadingSpinner'
import PlayingXI from '../components/draft/PlayingXI'
import RatingCard from '../components/draft/RatingCard'

export default function SharedDraftPage() {
  const { token } = useParams()
  const { data, isLoading, isError, error, refetch } = useQuery({ queryKey: ['shared', token], queryFn: () => fetchShared(token), retry: false })
  if (isLoading) return <LoadingSpinner label="Loading shared XI…" className="min-h-[40vh]" />
  if (isError) {
    return <ErrorState title={error.status === 404 ? 'This link is no longer available' : 'Could not load this XI'}
      message={error.status === 404 ? 'The owner may have stopped sharing it.' : error.message} onRetry={error.status === 404 ? undefined : refetch} />
  }
  const picks = data.picks.map((p) => ({ ...p, hideTeam: Boolean(data.team) }))
  return (
    <div className="grid gap-6 lg:grid-cols-2">
      <div className="space-y-4">
        <div>
          <p className="chip mb-2 w-fit">{data.modeLabel} · shared XI</p>
          <h1 className="display text-4xl">{draftTitle(data)}</h1>
          <p className="text-sm text-mute">Completed {formatDate(data.completedAt)}{data.challengeCode ? ` · ${data.challengeCode}` : ''}</p>
        </div>
        <RatingCard result={{ ...data, points: '—', completionSeconds: null }} />
        <Link to="/register" className="btn-gold">Build your own XI</Link>
      </div>
      <PlayingXI picks={picks} squadSize={picks.length} readOnly ratingsHidden={false} />
    </div>
  )
}
