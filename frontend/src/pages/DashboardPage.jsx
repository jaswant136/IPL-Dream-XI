import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { ClipboardList, Play } from 'lucide-react'
import { useAuth } from '../contexts/AuthContext'
import { fetchDrafts, fetchStatistics } from '../services/draftService'
import { draftTitle, formatDate } from '../utils/formatters'
import PageHeader from '../components/common/PageHeader'
import EmptyState from '../components/common/EmptyState'
import ErrorState from '../components/common/ErrorState'
import LoadingSpinner from '../components/common/LoadingSpinner'

function Stat({ label, value }) {
  return (
    <div className="panel p-4">
      <p className="text-xs uppercase tracking-wider text-mute">{label}</p>
      <p className="display mt-1 text-4xl">{value}</p>
    </div>
  )
}

export default function DashboardPage() {
  const { currentUser } = useAuth()
  const stats = useQuery({ queryKey: ['statistics'], queryFn: fetchStatistics })
  const recent = useQuery({ queryKey: ['drafts', 'recent'], queryFn: () => fetchDrafts({ size: 5 }) })

  if (stats.isLoading) return <LoadingSpinner label="Loading your dashboard…" />
  if (stats.isError) return <ErrorState message={stats.error.message} onRetry={stats.refetch} />

  const s = stats.data
  const name = currentUser?.displayName || currentUser?.email?.split('@')[0] || 'captain'
  const verifyBanner = currentUser && !currentUser.emailVerified && currentUser.providerData?.[0]?.providerId === 'password'

  return (
    <div className="space-y-8">
      <PageHeader title={`Welcome, ${name}`} subtitle="Your draft HQ."
        actions={<Link to="/play" className="btn-gold"><Play className="h-4 w-4" aria-hidden="true" />Start a new draft</Link>} />
      {verifyBanner && (
        <p role="status" className="rounded-xl border border-gold/40 bg-gold/10 px-4 py-3 text-sm text-gold">
          Please verify your email address - we sent a link when you registered.
        </p>
      )}
      <section aria-label="Your statistics" className="grid grid-cols-2 gap-3 md:grid-cols-5">
        <Stat label="Drafts played" value={s.totalDrafts} />
        <Stat label="Completed" value={s.completedDrafts} />
        <Stat label="Avg rating" value={s.completedDrafts ? s.averageRating.toFixed(1) : '—'} />
        <Stat label="Best rating" value={s.completedDrafts ? s.highestRating : '—'} />
        <Stat label="Leaderboard" value={s.leaderboardPosition ? `#${s.leaderboardPosition}` : '—'} />
      </section>

      {s.bestDraft && (
        <section aria-label="Best lineup">
          <h2 className="display mb-3 text-2xl">Best-rated lineup</h2>
          <Link to={`/drafts/${s.bestDraft.draftId}/result`} className="panel block p-5 transition hover:border-gold/50">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div>
                <p className="display text-2xl">{draftTitle(s.bestDraft)}</p>
                <p className="text-xs text-mute">{s.bestDraft.modeLabel} · {formatDate(s.bestDraft.completedAt)}</p>
              </div>
              <p className="display text-5xl text-gold">{s.bestDraft.teamRating}</p>
            </div>
            <p className="mt-3 text-xs text-mute">{s.bestDraft.playerNames.join(' · ')}</p>
          </Link>
        </section>
      )}

      <section aria-label="Recent drafts">
        <div className="mb-3 flex items-center justify-between">
          <h2 className="display text-2xl">Recent drafts</h2>
          <Link to="/drafts" className="text-sm text-gold hover:underline">View all</Link>
        </div>
        {recent.isLoading && <LoadingSpinner />}
        {recent.isError && <ErrorState message={recent.error.message} onRetry={recent.refetch} />}
        {recent.data?.content.length === 0 && (
          <EmptyState icon={ClipboardList} title="No drafts yet" message="Spin the wheel and build your first XI."
            action={<Link to="/play" className="btn-gold">Start drafting</Link>} />
        )}
        {recent.data?.content.length > 0 && (
          <ul className="panel divide-y divide-edge">
            {recent.data.content.map((d) => (
              <li key={d.draftId}>
                <Link to={d.status === 'COMPLETED' ? `/drafts/${d.draftId}/result` : `/draft/${d.draftId}`}
                  className="flex items-center gap-3 p-4 transition hover:bg-ink/60">
                  <div className="min-w-0 flex-1">
                    <p className="truncate font-semibold">{draftTitle(d)}</p>
                    <p className="text-xs text-mute">{d.modeLabel} · {formatDate(d.startedAt)} · {d.pickCount}/11</p>
                  </div>
                  <span className={`chip ${d.status === 'COMPLETED' ? 'border-good/40 text-good' : 'border-gold/40 text-gold'}`}>
                    {d.status === 'COMPLETED' ? `Rating ${d.teamRating}` : 'In progress'}
                  </span>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  )
}
