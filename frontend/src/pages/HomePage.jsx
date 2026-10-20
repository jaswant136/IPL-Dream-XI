import { Link } from 'react-router-dom'
import { motion } from 'framer-motion'
import { useQuery } from '@tanstack/react-query'
import { Eye, EyeOff, Gauge, RotateCw, ShieldCheck, Sparkles, Timer, Trophy, Users } from 'lucide-react'
import { fetchGameModes, fetchTeams } from '../services/teamService'
import { fetchLeaderboard } from '../services/leaderboardService'
import { useAuth } from '../contexts/AuthContext'

const MODE_ICON = { CLASSIC: Trophy, EXPERT: EyeOff, CHALLENGE: Timer, DREAM: Sparkles }

function PitchArt() {
  return (
    <svg viewBox="0 0 420 420" className="mx-auto w-full max-w-sm" role="img" aria-label="Illustration of a cricket pitch with a ball and stumps">
      <defs>
        <radialGradient id="glow" cx="50%" cy="50%" r="50%"><stop offset="0" stopColor="#F5C84B" stopOpacity=".22" /><stop offset="1" stopColor="#F5C84B" stopOpacity="0" /></radialGradient>
      </defs>
      <circle cx="210" cy="210" r="200" fill="url(#glow)" />
      <ellipse cx="210" cy="230" rx="190" ry="120" fill="none" stroke="#292A30" strokeWidth="2" />
      <ellipse cx="210" cy="230" rx="130" ry="80" fill="none" stroke="#292A30" strokeWidth="2" strokeDasharray="6 8" />
      <rect x="185" y="140" width="50" height="180" rx="6" fill="#17181C" stroke="#292A30" strokeWidth="2" />
      <line x1="185" y1="165" x2="235" y2="165" stroke="#F5C84B" strokeWidth="2" />
      <line x1="185" y1="295" x2="235" y2="295" stroke="#F5C84B" strokeWidth="2" />
      {[198, 210, 222].map((x) => (<g key={x}><line x1={x} y1="118" x2={x} y2="162" stroke="#F3F3F4" strokeWidth="3" strokeLinecap="round" /></g>))}
      <rect x="196" y="116" width="28" height="4" rx="2" fill="#F5C84B" />
      {[198, 210, 222].map((x) => (<line key={`b${x}`} x1={x} y1="298" x2={x} y2="342" stroke="#F3F3F4" strokeWidth="3" strokeLinecap="round" />))}
      <rect x="196" y="296" width="28" height="4" rx="2" fill="#F5C84B" />
      <motion.g animate={{ y: [0, -14, 0] }} transition={{ duration: 3, repeat: Infinity, ease: 'easeInOut' }}>
        <circle cx="300" cy="120" r="26" fill="#F5C84B" />
        <path d="M284 104c14 10 14 26 0 36M316 104c-14 10-14 26 0 36" stroke="#101114" strokeWidth="3" fill="none" strokeLinecap="round" />
      </motion.g>
    </svg>
  )
}

export default function HomePage() {
  const { currentUser } = useAuth()
  const modes = useQuery({ queryKey: ['game-modes'], queryFn: fetchGameModes })
  const teams = useQuery({ queryKey: ['teams'], queryFn: fetchTeams })
  const board = useQuery({ queryKey: ['leaderboard', 'home'], queryFn: () => fetchLeaderboard({ size: 5 }) })

  const playable = (teams.data ?? []).filter((t) => t.seasons.length > 0)
  const seasonSet = new Set(playable.flatMap((t) => t.seasons))
  const stats = [
    { label: 'Franchises ready', value: teams.data ? playable.length : '—', icon: Users },
    { label: 'Seasons playable', value: teams.data ? seasonSet.size : '—', icon: RotateCw },
    { label: 'Game modes', value: modes.data ? modes.data.length : '—', icon: Gauge },
    { label: 'Ranked players', value: board.data ? board.data.totalElements : '—', icon: Trophy },
  ]

  return (
    <div className="space-y-20">
      <section className="grid items-center gap-10 md:grid-cols-2">
        <div>
          <p className="chip mb-4 w-fit border-gold/40 text-gold">Unofficial fan game</p>
          <h1 className="display text-6xl leading-[.95] sm:text-7xl">Draft your <span className="text-gold">dream XI.</span></h1>
          <p className="mt-5 max-w-md text-lg text-mute">Spin for a franchise. Build your lineup. Prove your cricket IQ.</p>
          <div className="mt-8 flex flex-wrap gap-3">
            <Link to={currentUser ? '/play' : '/register'} className="btn-gold px-6 py-3 text-base">Start drafting</Link>
            <a href="#how-it-works" className="btn-ghost px-6 py-3 text-base">How it works</a>
          </div>
        </div>
        <PitchArt />
      </section>

      <section aria-label="Game statistics" className="grid grid-cols-2 gap-3 md:grid-cols-4">
        {stats.map((s) => (
          <div key={s.label} className="panel p-4">
            <s.icon className="h-5 w-5 text-gold" aria-hidden="true" />
            <p className="display mt-2 text-4xl">{s.value}</p>
            <p className="text-xs text-mute">{s.label}</p>
          </div>
        ))}
      </section>

      <section>
        <h2 className="display text-3xl">Game modes</h2>
        {modes.isError ? (
          <p className="mt-3 text-sm text-bad" role="alert">Could not load game modes: {modes.error.message}</p>
        ) : (
          <div className="mt-4 grid gap-3 sm:grid-cols-2">
            {(modes.data ?? Array.from({ length: 4 })).map((m, i) => {
              const Icon = m ? MODE_ICON[m.mode] : Trophy
              return (
                <div key={m?.mode ?? i} className="panel p-5">
                  <Icon className="h-6 w-6 text-gold" aria-hidden="true" />
                  <h3 className="display mt-3 text-2xl">{m?.label ?? '…'}</h3>
                  <p className="mt-1 text-sm text-mute">{m?.description ?? 'Loading…'}</p>
                  {m && <p className="mt-3 text-xs text-mute">Max {m.rules.maxOverseas} overseas{m.rules.minAllRounders ? ' · all-rounder required' : ''}{m.rules.maxTotalRating ? ` · budget ${m.rules.maxTotalRating}` : ''}</p>}
                </div>
              )
            })}
          </div>
        )}
      </section>

      <section id="how-it-works" className="scroll-mt-24">
        <h2 className="display text-3xl">How it works</h2>
        <ol className="mt-4 grid gap-3 md:grid-cols-4">
          {[
            ['Spin', 'The server picks a real franchise and season that has a full roster.'],
            ['Draft', 'Pick 11 players from that exact squad. Filter by role, swap anyone.'],
            ['Obey the rules', '1 keeper, a batter, 3 bowlers, max 4 overseas - enforced server-side.'],
            ['Score', 'Your XI is rated by a transparent formula and saved to your account.'],
          ].map(([t, d], i) => (
            <li key={t} className="panel p-5">
              <span className="display text-4xl text-gold">{i + 1}</span>
              <h3 className="display mt-1 text-xl">{t}</h3>
              <p className="mt-1 text-sm text-mute">{d}</p>
            </li>
          ))}
        </ol>
      </section>

      <section className="grid gap-6 md:grid-cols-2">
        <div>
          <h2 className="display text-3xl">Community leaders</h2>
          <div className="panel mt-4 divide-y divide-edge">
            {board.isLoading && <p className="p-4 text-sm text-mute">Loading…</p>}
            {board.isError && <p className="p-4 text-sm text-bad" role="alert">Could not load the leaderboard.</p>}
            {board.data?.content.length === 0 && <p className="p-4 text-sm text-mute">No completed drafts yet - be the first on the board.</p>}
            {board.data?.content.map((e) => (
              <div key={e.rank} className="flex items-center gap-3 p-4">
                <span className="display w-8 text-2xl text-gold">{e.rank}</span>
                <span className="flex-1 truncate font-semibold">{e.displayName}</span>
                <span className="text-sm text-mute">best {e.highestRating}</span>
              </div>
            ))}
          </div>
        </div>
        <div>
          <h2 className="display text-3xl">Fair play</h2>
          <ul className="mt-4 space-y-3 text-sm text-mute">
            <li className="flex gap-3"><ShieldCheck className="h-5 w-5 shrink-0 text-gold" aria-hidden="true" />Ratings and results are computed on the server - nothing the browser sends can change your score.</li>
            <li className="flex gap-3"><Eye className="h-5 w-5 shrink-0 text-gold" aria-hidden="true" />Expert mode hides ratings until the draft is complete.</li>
            <li className="flex gap-3"><RotateCw className="h-5 w-5 shrink-0 text-gold" aria-hidden="true" />Only seasons with real roster data are ever offered.</li>
          </ul>
        </div>
      </section>
    </div>
  )
}
