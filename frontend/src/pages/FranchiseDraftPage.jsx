import { useEffect, useMemo, useState } from 'react'
import { RotateCcw, Trophy, Users, Search, Star, ShieldCheck, Dices, LockKeyhole, ArrowUp, ArrowDown } from 'lucide-react'
import toast from 'react-hot-toast'
import { useQuery } from '@tanstack/react-query'
import { fetchTeams, fetchRoster } from '../services/teamService'
import { useAuth } from '../contexts/AuthContext'
import PageHeader from '../components/common/PageHeader'
import LoadingSpinner from '../components/common/LoadingSpinner'
import ErrorState from '../components/common/ErrorState'

const STORAGE_KEY = 'ipl-draft-club-dream-xi-v2'
const XI_SIZE = 11
const POSITIONS = [
  { code: 'WK', label: 'Wicketkeeper' },
  { code: 'BAT', label: 'Batter' },
  { code: 'AR', label: 'All-rounder' },
  { code: 'BOWL', label: 'Bowler' },
]

function getName(player) {
  return String(player?.name ?? player?.playerName ?? player?.fullName ?? 'Unknown player').trim()
}
function identity(player) {
  const id = player?.playerId ?? player?.id
  if (id != null) return `id:${String(id)}`
  const name = getName(player).normalize('NFKC').toLocaleLowerCase().replace(/\s+/g, ' ').trim()
  return name && name !== 'unknown player' ? `name:${name}` : null
}
function getRole(player) {
  // IPL datasets use different field names and labels for player roles.
  const fields = [
    player?.role, player?.playerRole, player?.playingRole, player?.type,
    player?.specialism, player?.specialization, player?.playerType,
    player?.category, player?.player_category, player?.player_type,
  ]
  return fields.filter(value => value != null).map(String).join(' ').toLowerCase().replace(/[_-]+/g, ' ').replace(/\s+/g, ' ').trim()
}
function roleGroup(player) {
  const role = getRole(player)
  if (/\b(wicket ?keeper|wk|keeper)\b/.test(role)) return 'Wicketkeeper'
  // Check all-rounder before batter/bowler because labels often say e.g. "Batting Allrounder".
  if (/all ?round(er)?|\ball ?round\b|\bar\b/.test(role)) return 'All-rounder'
  if (/\b(bowler|bowling|fast bowler|spinner)\b/.test(role)) return 'Bowler'
  if (/\b(batter|batsman|batswoman|batting|opener)\b/.test(role)) return 'Batter'
  return 'Unspecified'
}
function numberFrom(player, keys) {
  for (const key of keys) {
    const value = Number(player?.[key])
    if (Number.isFinite(value) && player?.[key] != null && player?.[key] !== '') return value
  }
  return null
}
function estimatePlayerScore(player) {
  const runs = numberFrom(player, ['runs', 'totalRuns', 'runsScored'])
  const wickets = numberFrom(player, ['wickets', 'totalWickets', 'wicketsTaken'])
  const average = numberFrom(player, ['battingAverage', 'average', 'batAvg'])
  const strikeRate = numberFrom(player, ['strikeRate', 'battingStrikeRate', 'sr'])
  const economy = numberFrom(player, ['economy', 'economyRate', 'bowlingEconomy'])
  const role = roleGroup(player)
  const hasBattingData = runs != null || average != null || strikeRate != null
  const hasBowlingData = wickets != null || economy != null
  let evidence = 0
  const battingScore = () => {
    let value = 55
    if (runs != null) { value += Math.min(18, Math.log10(Math.max(1, runs)) * 5); evidence++ }
    if (average != null) { value += Math.max(-10, Math.min(12, (average - 20) * 0.45)); evidence++ }
    if (strikeRate != null) { value += Math.max(-7, Math.min(10, (strikeRate - 110) * 0.15)); evidence++ }
    return value
  }
  const bowlingScore = () => {
    let value = 55
    if (wickets != null) { value += Math.min(22, Math.log10(Math.max(1, wickets)) * 7); evidence++ }
    if (economy != null) { value += Math.max(-10, Math.min(12, (8.5 - economy) * 2)); evidence++ }
    return value
  }
  let score
  if (role === 'Bowler') score = hasBowlingData ? bowlingScore() : (hasBattingData ? battingScore() : 60)
  else if (role === 'All-rounder') {
    const bat = hasBattingData ? battingScore() : null
    const bowl = hasBowlingData ? bowlingScore() : null
    score = bat != null && bowl != null ? (bat + bowl) / 2 : (bat ?? bowl ?? 60)
  } else if (role === 'Wicketkeeper') score = hasBattingData ? battingScore() + 2 : 60
  else score = hasBattingData ? battingScore() : (hasBowlingData ? bowlingScore() : 60)
  return { score: Math.round(Math.max(40, Math.min(98, score))), evidence, hasStats: evidence > 0 }
}
function loadSavedState(key) {
  try {
    const parsed = JSON.parse(localStorage.getItem(key) || '{}')
    return {
      players: Array.isArray(parsed.players) ? parsed.players.slice(0, XI_SIZE) : [],
      captain: parsed.captain || '',
      viceCaptain: parsed.viceCaptain || '',
      battingOrder: Array.isArray(parsed.battingOrder) ? parsed.battingOrder : [],
      spinHistory: Array.isArray(parsed.spinHistory) ? parsed.spinHistory : [],
      currentSpin: parsed.currentSpin || null,
    }
  } catch {
    return { players: [], captain: '', viceCaptain: '', battingOrder: [], spinHistory: [], currentSpin: null }
  }
}
function weightedChoice(items, weightFn) {
  const weighted = items.map(item => ({ item, weight: Math.max(0.01, weightFn(item)) }))
  const total = weighted.reduce((sum, entry) => sum + entry.weight, 0)
  let roll = Math.random() * total
  for (const entry of weighted) {
    roll -= entry.weight
    if (roll <= 0) return entry.item
  }
  return weighted[weighted.length - 1]?.item ?? null
}

export default function DreamXIPage() {
  const { currentUser } = useAuth()
  const storageKey = `${STORAGE_KEY}-${currentUser?.uid || 'guest'}`
  const teamsQuery = useQuery({ queryKey: ['teams'], queryFn: fetchTeams })
  const teams = useMemo(() => Array.isArray(teamsQuery.data) ? teamsQuery.data : [], [teamsQuery.data])
  const [state, setState] = useState(() => loadSavedState(storageKey))
  const [rosters, setRosters] = useState([])
  const [rosterLoading, setRosterLoading] = useState(true)
  const [rosterError, setRosterError] = useState(null)
  const [search, setSearch] = useState('')
  const [roleFilter, setRoleFilter] = useState('All')
  const [positionChoice, setPositionChoice] = useState('')
  const [battingPositionChoice, setBattingPositionChoice] = useState('')
  const [spinning, setSpinning] = useState(false)
  const [captain, setCaptain] = useState('')
  const [viceCaptain, setViceCaptain] = useState('')

  useEffect(() => {
    const saved = loadSavedState(storageKey)
    setState(saved)
    setCaptain(saved.captain)
    setViceCaptain(saved.viceCaptain)
  }, [storageKey])
  useEffect(() => {
    try { localStorage.setItem(storageKey, JSON.stringify({ ...state, captain, viceCaptain })) }
    catch { toast.error('Could not save your Dream XI in browser storage.') }
  }, [state, captain, viceCaptain, storageKey])
  useEffect(() => {
    let cancelled = false
    async function loadAllRosters() {
      if (teamsQuery.isPending && !teamsQuery.data) return
      if (teamsQuery.isError) { setRosterError(teamsQuery.error); setRosterLoading(false); return }
      setRosterLoading(true); setRosterError(null)
      try {
        const records = await Promise.all(teams.flatMap(team => (team.seasons || []).map(async season => {
          const response = await fetchRoster(team.code, season)
          const players = Array.isArray(response) ? response : (response?.players || [])
          return players.map(player => ({ ...player, sourceTeamCode: team.code, sourceTeamName: team.name, sourceSeason: season }))
        })))
        if (!cancelled) {
          const unique = new Map()
          records.flat().forEach(player => {
            const key = `${identity(player)}|${player.sourceSeason}|${player.sourceTeamCode}`
            if (identity(player) && !unique.has(key)) unique.set(key, player)
          })
          setRosters([...unique.values()])
        }
      } catch (error) {
        if (!cancelled) { setRosterError(error); setRosters([]) }
      } finally { if (!cancelled) setRosterLoading(false) }
    }
    loadAllRosters()
    return () => { cancelled = true }
  }, [teams, teamsQuery.data, teamsQuery.error, teamsQuery.isError, teamsQuery.isPending])

  const selectedKeys = useMemo(() => new Set(state.players.map(identity).filter(Boolean)), [state.players])
  const selectedNames = useMemo(() => new Set(state.players.map(p => getName(p).toLowerCase())), [state.players])
  const franchises = useMemo(() => {
    const map = new Map()
    for (const p of rosters) {
      const key = `${p.sourceTeamCode}|${p.sourceSeason}`
      if (!map.has(key)) map.set(key, { key, code: p.sourceTeamCode, name: p.sourceTeamName, season: String(p.sourceSeason) })
    }
    return [...map.values()]
  }, [rosters])
  const currentSquad = useMemo(() => state.currentSpin
    ? rosters.filter(p => p.sourceTeamCode === state.currentSpin.code && String(p.sourceSeason) === String(state.currentSpin.season))
    : [], [rosters, state.currentSpin])
  const filteredPlayers = useMemo(() => currentSquad.filter(player => {
    const text = `${getName(player)} ${player.sourceTeamName || ''} ${player.sourceTeamCode || ''}`.toLowerCase()
    return text.includes(search.toLowerCase()) && (roleFilter === 'All' || roleGroup(player) === roleFilter)
  }).sort((a, b) => getName(a).localeCompare(getName(b))), [currentSquad, search, roleFilter])
  const availablePositions = POSITIONS // Position types can repeat; each of the 11 player slots is independently locked.
  const selectedStats = useMemo(() => {
    const ratings = state.players.map(estimatePlayerScore)
    const scores = ratings.map(x => x.score)
    const overall = scores.length ? Math.round(scores.reduce((sum, n) => sum + n, 0) / scores.length) : 0
    const playersWithStats = ratings.filter(x => x.hasStats).length
    const counts = state.players.reduce((acc, p) => { const role = roleGroup(p); acc[role] = (acc[role] || 0) + 1; return acc }, {})
    const keeperCount = counts.Wicketkeeper || 0
    const bowlers = counts.Bowler || 0
    const allRounders = counts['All-rounder'] || 0
    const batters = counts.Batter || 0
    const completeness = state.players.length === XI_SIZE
    const hasKeeper = keeperCount >= 1
    const hasBowling = bowlers + allRounders >= 5
    const hasBatting = batters + allRounders >= 5
    const balanceBonus = (hasKeeper ? 4 : 0) + (hasBowling ? 4 : 0) + (hasBatting ? 3 : 0)
    const rating = state.players.length ? Math.min(100, Math.round(overall * 0.88 + balanceBonus)) : 0
    const balanceChecks = [hasKeeper, hasBowling, hasBatting].filter(Boolean).length
    const titleChance = completeness ? Math.max(2, Math.min(35, Math.round((rating - 55) * 0.65 + (hasKeeper ? 3 : -5) + (hasBowling ? 4 : -4) + (hasBatting ? 2 : -3)))) : 0
    // Transparent, illustrative outcome model based on this XI's rating and role balance.
    // It is not a real-world IPL forecast or a statistical simulation.
    const strength = Math.max(0, Math.min(1, (rating - 55) / 40))
    const finishWeights = completeness ? [
      titleChance,
      8 + strength * 8,
      8 + strength * 6,
      18 + strength * 12 + balanceChecks * 2,
      66 - titleChance - (8 + strength * 8) - (8 + strength * 6) - (18 + strength * 12 + balanceChecks * 2),
    ].map(value => Math.max(1, value)) : []
    const finishTotal = finishWeights.reduce((sum, value) => sum + value, 0)
    const finishProbabilities = finishWeights.map(value => Math.round(value / finishTotal * 100))
    // Keep the displayed distribution at exactly 100% after rounding.
    if (finishProbabilities.length) finishProbabilities[4] += 100 - finishProbabilities.reduce((sum, value) => sum + value, 0)
    const finishes = [
      { label: 'Champion', range: '1st', probability: finishProbabilities[0] || 0 },
      { label: 'Runner-up', range: '2nd', probability: finishProbabilities[1] || 0 },
      { label: 'Third place', range: '3rd', probability: finishProbabilities[2] || 0 },
      { label: 'Playoffs', range: 'Top 4', probability: finishProbabilities[3] || 0 },
      { label: 'League stage exit', range: '5th–10th', probability: finishProbabilities[4] || 0 },
    ]
    const likelyFinish = completeness ? finishes.reduce((best, item) => item.probability > best.probability ? item : best, finishes[0]) : null
    return { rating, titleChance, counts, keeperCount, bowlers, allRounders, batters, completeness, playersWithStats, balanceChecks, finishes, likelyFinish }
  }, [state.players])

  function spin() {
    // Every spin must be followed by exactly one locked player pick.
    if (state.currentSpin) { toast.error('Choose and lock one player from the current squad before spinning again.'); return }
    if (spinning || state.players.length >= XI_SIZE || !franchises.length) return
    setSpinning(true)
    const usedCodes = new Set(state.players.map(p => p.sourceTeamCode))
    const lastCode = state.spinHistory[state.spinHistory.length - 1]?.code
    // New franchises get a substantial boost; used franchises remain possible but less likely.
    const chosen = weightedChoice(franchises, franchise => {
      let weight = usedCodes.has(franchise.code) ? 0.38 : 2.8
      if (franchise.code === lastCode) weight *= 0.22
      return weight
    })
    window.setTimeout(() => {
      if (!chosen) { setSpinning(false); toast.error('No franchise-season combinations found.'); return }
      setState(prev => ({ ...prev, currentSpin: chosen, spinHistory: [...prev.spinHistory, chosen] }))
      setSearch(''); setRoleFilter('All'); setPositionChoice(''); setBattingPositionChoice(''); setSpinning(false)
      toast.success(`Spin result: ${chosen.code} · IPL ${chosen.season}`)
    }, 650)
  }
  function addPlayer(player) {
    if (state.players.length >= XI_SIZE) return toast.error('Your Dream XI is already complete.')
    const key = identity(player)
    if (!key || selectedKeys.has(key) || selectedNames.has(getName(player).toLowerCase())) return toast.error('This player is already in your XI.')
    if (!state.currentSpin || String(player.sourceTeamCode).trim().toUpperCase() !== String(state.currentSpin.code).trim().toUpperCase() || String(player.sourceSeason) !== String(state.currentSpin.season)) return toast.error('Choose a player from the spun franchise and season.')
    if (!positionChoice || !availablePositions.some(pos => pos.code === positionChoice)) return toast.error('Choose a cricket role first.')
    const battingPosition = Number(battingPositionChoice)
    if (!Number.isInteger(battingPosition) || battingPosition < 1 || battingPosition > XI_SIZE) return toast.error('Choose a batting position from 1 to 11.')
    if (state.players.some(p => Number(p.battingOrder) === battingPosition)) return toast.error(`Batting position ${battingPosition} is already occupied. Choose an open number or move a player first.`)
    const assigned = { ...player, dreamPosition: positionChoice, battingOrder: battingPosition, dreamPick: state.players.length + 1, locked: true }
    setState(prev => {
      // Guard against rapid double-clicks using the latest state.
      if (prev.players.length >= XI_SIZE || prev.players.some(p => identity(p) === key || getName(p).toLowerCase() === getName(player).toLowerCase())) return prev
      // Batting numbers are fixed slots, not insertion indexes. Keep existing
      // players at their chosen numbers when a different open slot is selected.
      if (prev.players.some(p => Number(p.battingOrder) === battingPosition)) return prev
      return { ...prev, players: [...prev.players, assigned], currentSpin: null }
    })
    setPositionChoice(''); setBattingPositionChoice(''); setSearch('')
    toast.success(`${getName(player)} locked as ${positionChoice} at batting position ${battingPosition}`)
  }
  function movePlayer(battingPosition, direction) {
    setState(prev => {
      const players = [...prev.players]
      const currentIndex = players.findIndex(player => Number(player.battingOrder || 0) === battingPosition)
      const targetIndex = players.findIndex(player => Number(player.battingOrder || 0) === battingPosition + direction)
      if (currentIndex < 0 || targetIndex < 0) return prev
      const currentOrder = players[currentIndex].battingOrder
      players[currentIndex] = { ...players[currentIndex], battingOrder: players[targetIndex].battingOrder }
      players[targetIndex] = { ...players[targetIndex], battingOrder: currentOrder }
      return { ...prev, players }
    })
  }

  function resetXI() {
    if (!window.confirm('Start a fresh Dream XI? This clears all locked picks and spin history.')) return
    setState({ players: [], captain: '', viceCaptain: '', battingOrder: [], spinHistory: [], currentSpin: null })
    setCaptain(''); setViceCaptain(''); setSearch(''); setPositionChoice(''); setBattingPositionChoice('')
    toast.success('Fresh Dream XI started.')
  }

  if (teamsQuery.isPending && !teamsQuery.data) return <LoadingSpinner />
  if (teamsQuery.isError && !teams.length) return <ErrorState title="Could not load IPL franchises" message="Please check the backend connection and try again." />

  return <div className="space-y-6">
    <PageHeader title="My Dream IPL XI" subtitle="Spin a franchise and season, pick one player, assign a cricket slot, and lock them in. Build your XI one spin at a time." actions={<button className="btn-ghost" onClick={resetXI}><RotateCcw className="h-4 w-4" /> New Dream XI</button>} />

    <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
      <div className="card flex items-center gap-3"><Users className="h-5 w-5 text-gold" /><div><p className="text-sm text-muted">Players locked</p><p className="text-2xl font-bold">{state.players.length} / 11</p></div></div>
      <div className="card flex items-center gap-3"><Star className="h-5 w-5 text-gold" /><div><p className="text-sm text-muted">Team rating</p><p className="text-2xl font-bold">{selectedStats.rating} / 100</p><p className="text-xs text-muted">Stats found: {selectedStats.playersWithStats}/{state.players.length}</p></div></div>
      <div className="card flex items-center gap-3"><Trophy className="h-5 w-5 text-gold" /><div><p className="text-sm text-muted">Estimated title chance</p><p className="text-2xl font-bold">{selectedStats.completeness ? `${selectedStats.titleChance}%` : '—'}</p></div></div>
    </div>

    {rosterError && <ErrorState title="Could not load player rosters" message="Check the backend and roster API, then refresh this page." />}
    <div className="grid gap-6 xl:grid-cols-[minmax(0,0.95fr)_minmax(0,1.05fr)]">
      <section className="card space-y-4">
        <div><h2 className="text-xl font-bold">Spin &amp; pick</h2><p className="mt-1 text-sm text-muted">Each spin reveals one franchise-season squad. You must lock one player before the next spin. Unrepresented franchises are more likely to appear; back-to-back repeats are discouraged.</p></div>
        <div className="rounded-2xl border border-gold/30 bg-gold/5 p-5 text-center">
          <Dices className="mx-auto h-9 w-9 text-gold" />
          <p className="mt-3 text-xs uppercase tracking-[0.2em] text-muted">Spin result</p>
          <p className="mt-1 text-2xl font-bold">{state.currentSpin ? `${state.currentSpin.code} · IPL ${state.currentSpin.season}` : state.players.length === XI_SIZE ? 'Dream XI complete!' : 'Ready to spin?'}</p>
          {state.currentSpin && <p className="mt-1 text-sm text-muted">{state.currentSpin.name}</p>}
          <button className="btn-gold mt-4 w-full justify-center" onClick={spin} disabled={spinning || !!state.currentSpin || state.players.length >= XI_SIZE || rosterLoading || !franchises.length}>
            <Dices className={`h-4 w-4 ${spinning ? 'animate-spin' : ''}`} /> {spinning ? 'Spinning…' : state.players.length >= XI_SIZE ? 'XI complete' : state.currentSpin ? 'Lock one player to continue' : 'Spin franchise + season'}
          </button>
          {state.currentSpin && <p className="mt-3 text-xs text-muted">Compulsory: choose a player, select their role and batting position, then lock the pick to unlock the next spin.</p>}
        </div>

        {state.currentSpin && state.players.length < XI_SIZE && <>
          <div className="grid gap-3 sm:grid-cols-[1fr_170px]">
            <div className="relative"><Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted" /><input className="input w-full pl-9" value={search} onChange={e => setSearch(e.target.value)} placeholder="Search this squad…" /></div>
            <select className="input w-full" value={roleFilter} onChange={e => setRoleFilter(e.target.value)}>{['All', 'Batter', 'Bowler', 'All-rounder', 'Wicketkeeper', 'Unspecified'].map(role => <option key={role} value={role}>{role === 'All' ? 'All roles' : role}</option>)}</select>
          </div>
          <div>
            <p className="mb-2 text-sm text-muted">1. Choose the player's cricket role</p>
            <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">{availablePositions.map(pos => <button type="button" key={pos.code} onClick={() => setPositionChoice(pos.code)} className={`rounded-xl border p-3 text-left transition ${positionChoice === pos.code ? 'border-gold bg-gold/10' : 'border-white/10 hover:border-white/30'}`}><span className="block font-bold">{pos.code}</span><span className="mt-1 block text-xs text-muted">{pos.label}</span></button>)}</div>
            <p className="mb-2 mt-4 text-sm text-muted">2. Choose batting position (1–11)</p>
            <div className="grid grid-cols-6 gap-2 sm:grid-cols-11">{Array.from({ length: XI_SIZE }, (_, index) => { const number = index + 1; const occupied = state.players.some(p => Number(p.battingOrder || 0) === number); return <button type="button" key={number} onClick={() => setBattingPositionChoice(String(number))} className={`rounded-lg border p-2 text-sm font-bold transition ${Number(battingPositionChoice) === number ? 'border-gold bg-gold/10 text-gold' : 'border-white/10 hover:border-white/30'} ${occupied ? 'border-dashed' : ''}`} title={occupied ? `No. ${number} is occupied; choose an open position` : `Bat at No. ${number}`}>{number}{occupied ? ' ↕' : ''}</button> })}</div>
            {battingPositionChoice && <p className="mt-2 text-xs text-muted">Selected batting position: No. {battingPositionChoice}. Existing players keep their assigned numbers; choose an open slot.</p>}
          </div>
          {rosterLoading ? <div className="py-8"><LoadingSpinner /></div> : filteredPlayers.length === 0 ? <p className="py-8 text-center text-sm text-muted">No players match this squad search.</p> : <div className="max-h-[560px] space-y-2 overflow-y-auto pr-1">
            {filteredPlayers.map(player => {
              const key = identity(player)
              const alreadySelected = selectedKeys.has(key) || selectedNames.has(getName(player).toLowerCase())
              const rating = estimatePlayerScore(player)
              return <div key={`${key}-${player.sourceSeason}-${player.sourceTeamCode}`} className="flex items-center justify-between gap-3 rounded-xl border border-white/10 p-3">
                <div className="min-w-0"><p className="truncate font-semibold">{getName(player)}</p><p className="text-xs text-muted">{roleGroup(player)} · {player.sourceTeamCode} · IPL {player.sourceSeason}</p><p className="mt-1 text-xs text-gold">Estimated score: {rating.score}/100{rating.hasStats ? '' : ' · limited stats'}</p></div>
                <button className={alreadySelected || !positionChoice ? 'btn-ghost shrink-0 opacity-50' : 'btn-gold shrink-0'} onClick={() => addPlayer(player)} disabled={alreadySelected || !positionChoice || !battingPositionChoice}>{alreadySelected ? 'Already picked' : !positionChoice || !battingPositionChoice ? 'Choose role + batting position' : `Lock at No. ${battingPositionChoice}`}</button>
              </div>
            })}
          </div>}
        </>}
        {!state.currentSpin && state.players.length < XI_SIZE && <div className="rounded-xl border border-dashed border-white/20 p-6 text-center text-sm text-muted">Spin the wheel to reveal a franchise-season squad and start your next pick.</div>}
        <div className="rounded-xl bg-white/5 p-3"><p className="text-sm font-semibold">Spin history</p><div className="mt-2 flex flex-wrap gap-2">{state.spinHistory.length ? state.spinHistory.map((spinItem, index) => <span key={`${spinItem.key}-${index}`} className="rounded-full border border-white/10 px-3 py-1 text-xs">{index + 1}. {spinItem.code} · {spinItem.season}</span>) : <span className="text-xs text-muted">No spins yet</span>}</div></div>
      </section>

      <section className="card space-y-4">
        <div><div className="flex items-center justify-between gap-3"><h2 className="text-xl font-bold">Your locked Dream XI</h2><span className="text-sm text-muted">{Math.round(state.players.length / XI_SIZE * 100)}%</span></div><div className="mt-3 h-2 overflow-hidden rounded-full bg-edge"><div className="h-full rounded-full bg-gold transition-all" style={{ width: `${state.players.length / XI_SIZE * 100}%` }} /></div><p className="mt-2 text-sm text-muted">{state.players.length} of 11 slots filled. Choose a batting position before locking each player; arrows can still adjust the order later.</p></div>
        <div className="space-y-2">{Array.from({ length: XI_SIZE }, (_, index) => {
          // Render each numbered batting slot from the saved battingOrder,
          // not from the order in which players were drafted.
          const player = state.players.find(p => Number(p.battingOrder || 0) === index + 1) || null
          const pos = player ? POSITIONS.find(p => p.code === player.dreamPosition) : null
          return <div key={index} className={`flex items-center gap-3 rounded-xl border p-3 ${player ? 'border-gold/30 bg-gold/5' : 'border-dashed border-white/15'}`}>
            <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-white/5 text-sm font-bold text-gold">{String(index + 1).padStart(2, '0')}</div>
            <div className="min-w-0 flex-1">{player ? <><p className="truncate font-semibold">{getName(player)}</p><p className="text-xs text-muted">{player.sourceTeamCode} · IPL {player.sourceSeason} · {roleGroup(player)} · Batting No. {player.battingOrder || index + 1}</p></> : <><p className="font-medium text-muted">Open slot {index + 1}</p><p className="text-xs text-muted">Waiting for next spin and pick</p></>}</div>
            {player ? <div className="flex shrink-0 items-center gap-2">
              <div className="text-right"><span className="inline-flex items-center gap-1 rounded-lg border border-gold/30 px-2 py-1 text-xs font-bold text-gold">{pos?.code || player.dreamPosition} <LockKeyhole className="h-3 w-3" /></span><p className="mt-1 text-xs text-muted">{estimatePlayerScore(player).score}/100</p></div>
              <div className="flex flex-col gap-1">
                <button type="button" aria-label={`Move ${getName(player)} up in batting order`} title="Move up in batting order" onClick={() => movePlayer(index + 1, -1)} disabled={index === 0} className="rounded-md border border-white/10 p-1.5 hover:border-gold/50 disabled:cursor-not-allowed disabled:opacity-25"><ArrowUp className="h-3.5 w-3.5" /></button>
                <button type="button" aria-label={`Move ${getName(player)} down in batting order`} title="Move down in batting order" onClick={() => movePlayer(index + 1, 1)} disabled={index === state.players.length - 1} className="rounded-md border border-white/10 p-1.5 hover:border-gold/50 disabled:cursor-not-allowed disabled:opacity-25"><ArrowDown className="h-3.5 w-3.5" /></button>
              </div>
            </div> : <span className="shrink-0 text-xs text-muted">—</span>}
          </div>
        })}</div>

        <div className="grid gap-4 sm:grid-cols-2">
          <label className="block text-sm font-medium">Captain<select className="input mt-2 w-full" value={captain} onChange={e => setCaptain(e.target.value)} disabled={!state.players.length}><option value="">Choose captain</option>{state.players.map(p => <option key={identity(p)} value={identity(p)}>{getName(p)}</option>)}</select></label>
          <label className="block text-sm font-medium">Vice-captain<select className="input mt-2 w-full" value={viceCaptain} onChange={e => setViceCaptain(e.target.value)} disabled={!state.players.length}><option value="">Choose vice-captain</option>{state.players.filter(p => identity(p) !== captain).map(p => <option key={identity(p)} value={identity(p)}>{getName(p)}</option>)}</select></label>
        </div>
        <div className="rounded-xl border border-gold/25 bg-gold/5 p-4">
          <div className="mb-3 flex items-center gap-2"><Trophy className="h-5 w-5 text-gold" /><h3 className="font-bold">Dream XI season prediction</h3></div>
          {selectedStats.completeness ? <>
            <div className="mb-4 grid gap-3 sm:grid-cols-3">
              <div className="rounded-xl bg-black/20 p-3"><p className="text-xs text-muted">Most likely outcome</p><p className="mt-1 text-lg font-bold text-gold">{selectedStats.likelyFinish?.label}</p><p className="text-xs text-muted">{selectedStats.likelyFinish?.probability}% model estimate</p></div>
              <div className="rounded-xl bg-black/20 p-3"><p className="text-xs text-muted">Estimated title chance</p><p className="mt-1 text-lg font-bold">{selectedStats.titleChance}%</p><p className="text-xs text-muted">Winning the IPL</p></div>
              <div className="rounded-xl bg-black/20 p-3"><p className="text-xs text-muted">Squad strength</p><p className="mt-1 text-lg font-bold">{selectedStats.rating}/100</p><p className="text-xs text-muted">{selectedStats.balanceChecks}/3 balance checks</p></div>
            </div>
            <div className="overflow-x-auto"><table className="w-full text-left text-sm"><thead><tr className="border-b border-white/10 text-muted"><th className="py-2 pr-3 font-medium">Predicted finish</th><th className="py-2 pr-3 font-medium">Position</th><th className="py-2 text-right font-medium">Estimated chance</th></tr></thead><tbody>{selectedStats.finishes.map(item => <tr key={item.label} className="border-b border-white/5 last:border-0"><td className="py-2 pr-3 font-medium">{item.label}</td><td className="py-2 pr-3 text-muted">{item.range}</td><td className="py-2 text-right font-semibold">{item.probability}%</td></tr>)}</tbody></table></div>
            <p className="mt-3 text-xs text-muted">This is an illustrative estimate based on your XI's rating and role balance, not a real IPL simulation or guarantee. Actual results depend on opponents, form, conditions, and match outcomes.</p>
          </> : <p className="text-sm text-muted">Complete all 11 picks to generate your likely finish, title chance, and predicted position table.</p>}
        </div>

        <div className="rounded-xl bg-white/5 p-4"><div className="mb-3 flex items-center gap-2"><ShieldCheck className="h-5 w-5 text-gold" /><h3 className="font-bold">Team balance</h3></div><div className="grid grid-cols-2 gap-3 text-sm sm:grid-cols-4">{['Batter', 'Bowler', 'All-rounder', 'Wicketkeeper'].map(role => <div key={role}><p className="text-muted">{role}</p><p className="text-xl font-bold">{selectedStats.counts[role] || 0}</p></div>)}</div>{state.players.length === XI_SIZE && <div className="mt-3 space-y-1 text-sm"><p className={selectedStats.balanceChecks === 3 ? 'text-emerald-400' : 'text-amber-300'}>{selectedStats.balanceChecks}/3 balance checks passed: {selectedStats.keeperCount >= 1 ? 'wicketkeeper included' : 'add a wicketkeeper'} · {selectedStats.bowlers + selectedStats.allRounders >= 5 ? 'bowling depth looks adequate' : 'consider more bowling options'} · {selectedStats.batters + selectedStats.allRounders >= 5 ? 'batting depth looks adequate' : 'consider more batting options'}.</p><p className="text-muted">Estimated title chance: {selectedStats.titleChance}%. This is a rough squad-balance heuristic, not a statistical simulation. Stats found for {selectedStats.playersWithStats}/11 players.</p></div>}{state.players.length < XI_SIZE && <p className="mt-3 text-sm text-muted">{XI_SIZE - state.players.length} more locked pick(s) needed to complete the XI.</p>}</div>
      </section>
    </div>
    <p className="text-center text-xs text-muted">Your Dream XI is saved in browser storage for this profile. Ratings and title chances are preliminary estimates. Use the up/down arrows to set batting order. Player roles remain locked; start a new Dream XI only if you want to clear your picks and spin history.</p>
  </div>
}
