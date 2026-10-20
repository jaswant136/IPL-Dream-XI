import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Eye, EyeOff, Loader2, Sparkles, Timer, Trophy } from 'lucide-react'
import toast from 'react-hot-toast'
import { fetchGameModes, fetchTeams } from '../services/teamService'
import { startDraft } from '../services/draftService'
import PageHeader from '../components/common/PageHeader'
import ErrorState from '../components/common/ErrorState'
import LoadingSpinner from '../components/common/LoadingSpinner'
import SpinReel from '../components/draft/SpinReel'

const ICONS = { CLASSIC: Trophy, EXPERT: EyeOff, CHALLENGE: Timer, DREAM: Sparkles }

export default function PlayPage() {
  const navigate = useNavigate()
  const qc = useQueryClient()
  const modes = useQuery({ queryKey: ['game-modes'], queryFn: fetchGameModes })
  const teams = useQuery({ queryKey: ['teams'], queryFn: fetchTeams })
  const [mode, setMode] = useState('CLASSIC')
  const [phase, setPhase] = useState('idle') // idle | spinning | season | done
  const [started, setStarted] = useState(null)
  const startedRef = useRef(null)

  // The reel only DISPLAYS franchises the backend says are playable; it never invents options.
  const playable = useMemo(() => (teams.data ?? []).filter((t) => t.seasons.length > 0), [teams.data])
  const codes = useMemo(() => playable.map((t) => t.code), [playable])

  const start = useMutation({
    mutationFn: () => startDraft(mode, crypto.randomUUID()),
    onSuccess: (draft) => {
      startedRef.current = draft
      setStarted(draft)
      qc.setQueryData(['draft', draft.draftId], draft)
      qc.invalidateQueries({ queryKey: ['drafts'] })
      qc.invalidateQueries({ queryKey: ['statistics'] })
    },
    onError: (e) => {
      setPhase('idle')
      toast.error(e.message)
    },
  })

  const isDream = mode === 'DREAM'
  const spinning = phase === 'spinning' || phase === 'season' || start.isPending

  function spin() {
    if (spinning) return
    setStarted(null)
    startedRef.current = null
    setPhase('spinning')
    start.mutate()
  }

  const onTeamSettled = useCallback(() => setPhase('season'), [])
  const onSeasonSettled = useCallback(() => {
    const d = startedRef.current
    if (!d) return
    setPhase('done')
    toast.success(d.team ? `${d.team.name} · IPL ${d.season}` : 'Dream XI ready!')
    setTimeout(() => navigate(`/draft/${d.draftId}`), 700)
  }, [navigate])

  // Dream XI has no franchise reel: finish as soon as the server answers.
  useEffect(() => {
    if (isDream && started && phase === 'spinning') {
      const t = setTimeout(() => {
        setPhase('done')
        toast.success('Dream XI pool ready!')
        navigate(`/draft/${started.draftId}`)
      }, 900)
      return () => clearTimeout(t)
    }
    return undefined
  }, [isDream, started, phase, navigate])

  if (modes.isLoading || teams.isLoading) return <LoadingSpinner label="Loading game data…" />
  if (modes.isError || teams.isError) {
    return <ErrorState title="Could not load game data" message={(modes.error || teams.error).message}
      onRetry={() => { modes.refetch(); teams.refetch() }} />
  }

  const noData = !isDream && playable.length === 0
  const seasonCodes = started?.team ? (playable.find((t) => t.code === started.team.code)?.seasons ?? [started.season]).map(String) : []

  return (
    <div>
      <PageHeader title="Choose your mode" subtitle="The server picks the franchise and season for you." />
      <div role="radiogroup" aria-label="Game mode" className="grid gap-3 sm:grid-cols-2">
        {modes.data.map((m) => {
          const Icon = ICONS[m.mode]
          const active = mode === m.mode
          return (
            <button key={m.mode} type="button" role="radio" aria-checked={active} disabled={spinning}
              onClick={() => setMode(m.mode)}
              className={`panel p-5 text-left transition disabled:opacity-60 ${active ? 'border-gold ring-1 ring-gold' : 'hover:border-gold/50'}`}>
              <div className="flex items-center gap-3">
                <Icon className="h-6 w-6 text-gold" aria-hidden="true" />
                <h2 className="display text-2xl">{m.label}</h2>
                {m.ratingsHiddenWhileDrafting && <EyeOff className="h-4 w-4 text-mute" aria-label="Ratings hidden" />}
                {!m.ratingsHiddenWhileDrafting && <Eye className="h-4 w-4 text-mute" aria-label="Ratings visible" />}
              </div>
              <p className="mt-2 text-sm text-mute">{m.description}</p>
              <p className="mt-3 text-xs text-mute">
                Max {m.rules.maxOverseas} overseas
                {m.rules.minAllRounders ? ' · all-rounder required' : ''}
                {m.rules.maxTotalRating ? ` · rating budget ${m.rules.maxTotalRating}` : ''}
                {m.rules.maxFromOneFranchise ? ` · max ${m.rules.maxFromOneFranchise} per franchise` : ''}
              </p>
            </button>
          )
        })}
      </div>

      <div className="panel mt-6 flex flex-col gap-4 p-5 sm:flex-row sm:items-center sm:justify-between">
        <div className="text-left">
          <h2 className="display text-xl">Your custom 10-Team Draft</h2>
          <p className="mt-1 text-sm text-mute">
            Open your IPL Draft Club Dream XI builder to spin franchise-seasons, choose players, set batting positions, and view your squad prediction.
          </p>
        </div>
        <button
          type="button"
          className="btn-gold shrink-0 px-5 py-3"
          onClick={() => navigate('/franchise-draft')}
        >
          Open My Draft
        </button>
      </div>

      <div className="panel mt-8 p-6 text-center">
        {noData ? (
          <ErrorState title="No playable rosters yet"
            message="The database has no franchise-season with a complete roster. Import roster data (see the README), then reload." />
        ) : (
          <>
            {isDream ? (
              <p className="mx-auto max-w-md text-sm text-mute">
                Dream XI is a custom, multi-season team. You can pick from every roster in the database - but no more than 5 players from one franchise.
              </p>
            ) : (
              <div className="space-y-4">
                <div>
                  <p className="label text-center">Franchise</p>
                  <SpinReel codes={codes} target={started?.team?.code ?? null} spinning={phase === 'spinning'} onSettled={onTeamSettled} />
                </div>
                <div>
                  <p className="label text-center">Season</p>
                  {phase === 'season' || phase === 'done' ? (
                    <SpinReel label="Season" codes={seasonCodes} target={String(started?.season)} spinning={phase === 'season'} onSettled={onSeasonSettled} />
                  ) : (
                    <div className="mx-auto flex h-28 w-72 max-w-full items-center justify-center rounded-2xl border border-dashed border-edge text-mute">
                      {phase === 'spinning' ? 'Waiting for the franchise…' : 'IPL ????'}
                    </div>
                  )}
                </div>
              </div>
            )}
            <button type="button" className="btn-gold mt-6 px-8 py-3 text-base" disabled={spinning} onClick={spin}>
              {spinning ? (<><Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" /> Spinning…</>) : isDream ? 'Build Dream XI' : 'Spin the wheel'}
            </button>
            {start.isError && <p role="alert" className="mt-3 text-sm text-bad">{start.error.message}</p>}
          </>
        )}
      </div>
    </div>
  )
}
