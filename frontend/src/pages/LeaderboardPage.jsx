import { useState } from 'react'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { Trophy } from 'lucide-react'
import { fetchLeaderboard } from '../services/leaderboardService'
import PageHeader from '../components/common/PageHeader'
import EmptyState from '../components/common/EmptyState'
import ErrorState from '../components/common/ErrorState'
import LoadingSpinner from '../components/common/LoadingSpinner'

const SORTS = [['points', 'Points'], ['average', 'Average'], ['highest', 'Best XI'], ['completed', 'Drafts']]

export default function LeaderboardPage() {
  const [page, setPage] = useState(0)
  const [sort, setSort] = useState('points')
  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['leaderboard', { page, sort }],
    queryFn: () => fetchLeaderboard({ page, size: 20, sort }),
    placeholderData: keepPreviousData,
  })

  return (
    <div>
      <PageHeader title="Leaderboard" subtitle="Ranked from completed drafts scored by the server. Ties break on best XI, then drafts played." />
      <div role="group" aria-label="Sort leaderboard" className="mb-4 flex flex-wrap gap-2">
        {SORTS.map(([key, label]) => (
          <button key={key} type="button" aria-pressed={sort === key} onClick={() => { setSort(key); setPage(0) }}
            className={`rounded-full border px-3 py-1 text-xs font-semibold ${sort === key ? 'border-gold bg-gold text-ink' : 'border-edge text-mute hover:text-snow'}`}>{label}</button>
        ))}
      </div>
      {isLoading && <LoadingSpinner />}
      {isError && <ErrorState message={error.message} onRetry={refetch} />}
      {data && data.content.length === 0 && <EmptyState icon={Trophy} title="No ranked players yet" message="Complete a draft to appear on the board." />}
      {data && data.content.length > 0 && (
        <div className="panel overflow-x-auto">
          <table className="w-full min-w-[520px] text-left text-sm">
            <caption className="sr-only">Leaderboard</caption>
            <thead className="border-b border-edge text-xs uppercase tracking-wider text-mute">
              <tr><th className="p-3">Rank</th><th className="p-3">Player</th><th className="p-3 text-right">Drafts</th><th className="p-3 text-right">Avg</th><th className="p-3 text-right">Best</th><th className="p-3 text-right">Points</th></tr>
            </thead>
            <tbody className="divide-y divide-edge">
              {data.content.map((e) => (
                <tr key={e.rank}>
                  <td className="display p-3 text-2xl text-gold">{e.rank}</td>
                  <td className="p-3 font-semibold">{e.displayName}</td>
                  <td className="p-3 text-right tabular-nums">{e.completedDrafts}</td>
                  <td className="p-3 text-right tabular-nums">{e.averageRating.toFixed(1)}</td>
                  <td className="p-3 text-right tabular-nums">{e.highestRating}</td>
                  <td className="p-3 text-right font-bold tabular-nums">{e.points}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {data && data.totalPages > 1 && (
        <nav aria-label="Pagination" className="mt-6 flex items-center justify-center gap-3">
          <button type="button" className="btn-ghost" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>Previous</button>
          <span className="text-sm text-mute">Page {page + 1} of {data.totalPages}</span>
          <button type="button" className="btn-ghost" disabled={page + 1 >= data.totalPages} onClick={() => setPage((p) => p + 1)}>Next</button>
        </nav>
      )}
    </div>
  )
}
