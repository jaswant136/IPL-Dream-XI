import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ClipboardList, Trash2 } from 'lucide-react'
import toast from 'react-hot-toast'
import { deleteDraft, fetchDrafts } from '../services/draftService'
import { draftTitle, formatDate } from '../utils/formatters'
import PageHeader from '../components/common/PageHeader'
import EmptyState from '../components/common/EmptyState'
import ErrorState from '../components/common/ErrorState'
import LoadingSpinner from '../components/common/LoadingSpinner'
import ConfirmationDialog from '../components/common/ConfirmationDialog'
import TeamBadge from '../components/teams/TeamBadge'
import { useAuth } from '../contexts/AuthContext'

const FILTERS = [['', 'All'], ['IN_PROGRESS', 'In progress'], ['COMPLETED', 'Completed']]
const LOCAL_DRAFT_KEY = 'ipl-draft-club-franchise-draft-v1'

function readLocalFranchiseDraft(userId) {
  try {
    const raw = localStorage.getItem(`${LOCAL_DRAFT_KEY}:${userId || 'guest'}`)
    if (!raw) return null
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed.picks) || parsed.picks.length === 0) return null
    return { ...parsed, picks: parsed.picks, updatedAt: parsed.updatedAt || parsed.picks.at(-1)?.pickedAt || null }
  } catch { return null }
}

export default function MyDraftsPage() {
  const { currentUser } = useAuth()
  const [localDraft, setLocalDraft] = useState(() => readLocalFranchiseDraft(currentUser?.uid))
  useEffect(() => { setLocalDraft(readLocalFranchiseDraft(currentUser?.uid)) }, [currentUser?.uid])
  const qc = useQueryClient()
  const [page, setPage] = useState(0)
  const [status, setStatus] = useState('')
  const showLocalDraft = !!localDraft && (!status || status === 'IN_PROGRESS')
  const localPlayerNames = (localDraft?.picks || []).map((pick) => pick.player?.name || pick.player?.fullName || 'Unknown player')
  const [toDelete, setToDelete] = useState(null)

  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['drafts', { page, status }],
    queryFn: () => fetchDrafts({ page, size: 8, status: status || undefined }),
    placeholderData: keepPreviousData,
  })
  const del = useMutation({
    mutationFn: (id) => deleteDraft(id),
    onSuccess: () => {
      toast.success('Draft deleted')
      setToDelete(null)
      qc.invalidateQueries({ queryKey: ['drafts'] })
      qc.invalidateQueries({ queryKey: ['statistics'] })
    },
    onError: (e) => { toast.error(e.message); setToDelete(null) },
  })

  return (
    <div>
      <PageHeader title="My drafts" subtitle="Resume unfinished XIs or revisit completed ones." />
      <div role="group" aria-label="Filter by status" className="mb-4 flex gap-2">
        {FILTERS.map(([key, label]) => (
          <button key={key} type="button" aria-pressed={status === key} onClick={() => { setStatus(key); setPage(0) }}
            className={`rounded-full border px-3 py-1 text-xs font-semibold ${status === key ? 'border-gold bg-gold text-ink' : 'border-edge text-mute hover:text-snow'}`}>{label}</button>
        ))}
      </div>

      {isLoading && <LoadingSpinner />}
      {isError && <ErrorState message={error.message} onRetry={refetch} />}
      {showLocalDraft && (
        <ul className="mb-3 space-y-3">
          <li className="panel flex flex-wrap items-center gap-4 p-4">
            <TeamBadge code="XI" />
            <div className="min-w-0 flex-1">
              <p className="truncate font-semibold">10-Team IPL Draft</p>
              <p className="text-xs text-mute">Saved in this browser · {localDraft.picks.length} picks across 10 franchises</p>
              <p className="mt-1 truncate text-xs text-mute">{localPlayerNames.join(', ')}</p>
            </div>
            <span className="chip border-gold/40 text-gold">In progress</span>
            <Link to="/franchise-draft" className="btn-ghost">Resume</Link>
          </li>
        </ul>
      )}
      {data && data.content.length === 0 && !showLocalDraft && (
        <EmptyState icon={ClipboardList} title="Nothing here yet" message="Your drafts will appear here."
          action={<Link to="/play" className="btn-gold">Start drafting</Link>} />
      )}
      {data && data.content.length > 0 && (
        <ul className="space-y-3">
          {data.content.map((d) => {
            const done = d.status === 'COMPLETED'
            return (
              <li key={d.draftId} className="panel flex flex-wrap items-center gap-4 p-4">
                <TeamBadge code={d.team?.code ?? 'XI'} />
                <div className="min-w-0 flex-1">
                  <p className="truncate font-semibold">{draftTitle(d)}</p>
                  <p className="text-xs text-mute">{d.modeLabel} · {formatDate(d.startedAt)}{d.shared ? ' · shared' : ''}</p>
                  <p className="mt-1 truncate text-xs text-mute">{d.playerNames.length ? d.playerNames.join(', ') : 'No players picked yet'}</p>
                </div>
                {done ? <span className="display text-3xl text-gold" aria-label={`Rating ${d.teamRating}`}>{d.teamRating}</span>
                  : <span className="chip border-gold/40 text-gold">{d.pickCount}/11</span>}
                <div className="flex gap-2">
                  <Link to={done ? `/drafts/${d.draftId}/result` : `/draft/${d.draftId}`} className="btn-ghost">{done ? 'View' : 'Resume'}</Link>
                  {!done && (
                    <button type="button" className="btn-danger px-3" aria-label={`Delete draft ${draftTitle(d)}`} onClick={() => setToDelete(d)}><Trash2 className="h-4 w-4" /></button>
                  )}
                </div>
              </li>
            )
          })}
        </ul>
      )}
      {data && data.totalPages > 1 && (
        <nav aria-label="Pagination" className="mt-6 flex items-center justify-center gap-3">
          <button type="button" className="btn-ghost" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>Previous</button>
          <span className="text-sm text-mute">Page {page + 1} of {data.totalPages}</span>
          <button type="button" className="btn-ghost" disabled={page + 1 >= data.totalPages} onClick={() => setPage((p) => p + 1)}>Next</button>
        </nav>
      )}
      <ConfirmationDialog open={!!toDelete} danger title="Delete this draft?" message="This in-progress draft will be permanently removed."
        confirmLabel={del.isPending ? 'Deleting…' : 'Delete'} onConfirm={() => del.mutate(toDelete.draftId)} onCancel={() => setToDelete(null)} />
    </div>
  )
}
