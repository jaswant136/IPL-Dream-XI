import { useMemo, useState } from 'react'
import { Navigate, useNavigate, useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { CheckCircle2, RotateCcw } from 'lucide-react'
import toast from 'react-hot-toast'
import { fetchDraft } from '../services/draftService'
import { useDraftActions } from '../hooks/useDraftActions'
import { draftTitle } from '../utils/formatters'
import ErrorState from '../components/common/ErrorState'
import ConfirmationDialog from '../components/common/ConfirmationDialog'
import LoadingSpinner from '../components/common/LoadingSpinner'
import TeamBadge from '../components/teams/TeamBadge'
import PlayerPool from '../components/players/PlayerPool'
import PlayingXI from '../components/draft/PlayingXI'

export default function DraftPage() {
  const { draftId } = useParams()
  const navigate = useNavigate()
  const { data: draft, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['draft', draftId],
    queryFn: () => fetchDraft(draftId),
  })
  const { add, remove, complete, abandon, busy } = useDraftActions(draftId)
  const [confirmAbandon, setConfirmAbandon] = useState(false)

  const pickedIds = useMemo(() => new Set((draft?.picks ?? []).map((p) => p.player.id)), [draft])
  const pickedPlayerIds = useMemo(() => new Set((draft?.picks ?? []).map((p) => p.player.playerId)), [draft])

  if (isLoading) return <LoadingSpinner label="Loading your draft…" className="min-h-[40vh]" />
  if (isError) {
    return <ErrorState title={error.status === 404 ? 'Draft not found' : 'Could not load this draft'} message={error.message}
      onRetry={error.status === 404 ? undefined : refetch} />
  }
  if (draft.status === 'COMPLETED') return <Navigate to={`/drafts/${draftId}/result`} replace />

  const rules = draft.rules
  const canAbandon = draft.mode !== 'CHALLENGE' // challenges are locked once started

  // Client-side hints only. The backend re-validates every pick and is the authority.
  function blockReason(player) {
    if (pickedPlayerIds.has(player.playerId) && !pickedIds.has(player.id)) return 'Already drafted from another season'
    if (player.overseas && draft.overseasCount >= rules.maxOverseas) return `Overseas limit (${rules.maxOverseas}) reached`
    if (draft.pickCount >= draft.squadSize) return 'XI is full'
    return null
  }

  function onAdd(player) {
    const reason = blockReason(player)
    if (reason) {
      toast.error(reason)
      return
    }
    add.mutate(player.id, { onSuccess: () => toast.success(`${player.name} added`) })
  }

  function onComplete() {
    complete.mutate(undefined, {
      onSuccess: () => {
        toast.success('Draft complete!')
        navigate(`/drafts/${draftId}/result`)
      },
    })
  }

  function onAbandon() {
    abandon.mutate(undefined, {
      onSuccess: () => {
        setConfirmAbandon(false)
        navigate('/play')
      },
    })
  }

  return (
    <div>
      <div className="mb-6 flex flex-wrap items-center gap-4">
        <TeamBadge code={draft.team?.code ?? 'XI'} size="lg" />
        <div className="min-w-0 flex-1">
          <p className="chip mb-1 w-fit">{draft.modeLabel}{draft.challengeCode ? ` · ${draft.challengeCode}` : ''}</p>
          <h1 className="display text-3xl sm:text-4xl">{draftTitle(draft)}</h1>
          {draft.ratingsHidden && <p className="text-xs text-mute">Expert mode: ratings are hidden until you complete your XI.</p>}
        </div>
        {canAbandon && (
          <button type="button" className="btn-ghost" disabled={busy} onClick={() => setConfirmAbandon(true)}>
            <RotateCcw className="h-4 w-4" aria-hidden="true" /> New spin
          </button>
        )}
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <div>
          <h2 className="display mb-3 text-2xl">Player pool</h2>
          <PlayerPool players={draft.players} pickedIds={pickedIds} ratingsHidden={draft.ratingsHidden}
            onAdd={onAdd} busy={busy} blockReason={blockReason} rules={rules} onRetry={refetch} />
        </div>
        <div className="lg:sticky lg:top-24 lg:self-start">
          <h2 className="display mb-3 text-2xl">Your XI</h2>
          <div className="panel p-4">
            <PlayingXI picks={draft.picks.map((p) => ({ ...p, hideTeam: !draft.multiSeason }))} squadSize={draft.squadSize}
              ratingsHidden={draft.ratingsHidden} onRemove={(p) => remove.mutate(p.pickId)} busy={busy}
              overseasCount={draft.overseasCount} maxOverseas={rules.maxOverseas} />
            {draft.issues.length > 0 && draft.pickCount > 0 && (
              <ul className="mt-4 space-y-1 text-xs text-mute" aria-label="What is still needed">
                {draft.issues.map((i) => <li key={i}>• {i}</li>)}
              </ul>
            )}
            <button type="button" className="btn-gold mt-5 w-full py-3" disabled={!draft.canComplete || busy} onClick={onComplete}>
              <CheckCircle2 className="h-4 w-4" aria-hidden="true" /> {complete.isPending ? 'Scoring…' : 'Lock in my XI'}
            </button>
          </div>
        </div>
      </div>

      <ConfirmationDialog open={confirmAbandon} danger title="Start over?"
        message="This discards your current draft and takes you back to the wheel for a new spin."
        confirmLabel="Discard & respin" onConfirm={onAbandon} onCancel={() => setConfirmAbandon(false)} />
    </div>
  )
}
