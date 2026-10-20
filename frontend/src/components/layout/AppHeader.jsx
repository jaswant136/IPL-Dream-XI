import { useState } from 'react'
import { Link, NavLink, useNavigate } from 'react-router-dom'
import { LogOut, Menu, User, X } from 'lucide-react'
import toast from 'react-hot-toast'
import { useAuth } from '../../contexts/AuthContext'

const links = [
  { to: '/', label: 'Home', end: true },
  { to: '/play', label: 'Play' },
  { to: '/franchise-draft', label: '10-Team Draft' },
  { to: '/leaderboard', label: 'Leaderboard' },
  { to: '/drafts', label: 'My Drafts' },
]

export function BrandMark() {
  return (
    <Link to="/" className="flex items-center gap-2.5" aria-label="IPL Draft Club home">
      <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-gold">
        <svg viewBox="0 0 32 32" className="h-5 w-5" aria-hidden="true">
          <circle cx="16" cy="16" r="9" fill="#101114" />
          <path d="M10 11c4 3 4 7 0 10M22 11c-4 3-4 7 0 10" stroke="#F5C84B" strokeWidth="1.6" fill="none" />
        </svg>
      </span>
      <span className="display text-xl leading-none">IPL Draft <span className="text-gold">Club</span></span>
    </Link>
  )
}

export default function AppHeader() {
  const { currentUser, logout } = useAuth()
  const [open, setOpen] = useState(false)
  const navigate = useNavigate()

  const linkClass = ({ isActive }) =>
    `rounded-lg px-3 py-2 text-sm font-medium transition ${isActive ? 'text-gold' : 'text-mute hover:text-snow'}`

  async function onLogout() {
    try {
      await logout()
      setOpen(false)
      navigate('/')
      toast.success('Signed out.')
    } catch {
      toast.error('Could not sign out. Try again.')
    }
  }

  return (
    <header className="sticky top-0 z-40 border-b border-edge bg-ink/90 backdrop-blur">
      <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-4">
        <BrandMark />
        <nav aria-label="Main" className="hidden items-center gap-1 md:flex">
          {links.map((l) => (
            <NavLink key={l.to} to={l.to} end={l.end} className={linkClass}>{l.label}</NavLink>
          ))}
        </nav>
        <div className="hidden items-center gap-2 md:flex">
          {currentUser ? (
            <>
              <Link to="/profile" className="btn-ghost"><User className="h-4 w-4" aria-hidden="true" />{currentUser.displayName || 'Profile'}</Link>
              <button type="button" className="btn-ghost" onClick={onLogout} aria-label="Sign out"><LogOut className="h-4 w-4" aria-hidden="true" /></button>
            </>
          ) : (
            <>
              <Link to="/login" className="btn-ghost">Sign in</Link>
              <Link to="/register" className="btn-gold">Create account</Link>
            </>
          )}
        </div>
        <button type="button" className="btn-ghost px-2.5 md:hidden" aria-label={open ? 'Close menu' : 'Open menu'}
          aria-expanded={open} onClick={() => setOpen((v) => !v)}>
          {open ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
        </button>
      </div>
      {open && (
        <div className="border-t border-edge bg-ink px-4 py-3 md:hidden">
          <nav aria-label="Mobile" className="flex flex-col">
            {links.map((l) => (
              <NavLink key={l.to} to={l.to} end={l.end} className={linkClass} onClick={() => setOpen(false)}>{l.label}</NavLink>
            ))}
            {currentUser ? (
              <>
                <NavLink to="/profile" className={linkClass} onClick={() => setOpen(false)}>Profile</NavLink>
                <button type="button" className="rounded-lg px-3 py-2 text-left text-sm font-medium text-bad" onClick={onLogout}>Sign out</button>
              </>
            ) : (
              <>
                <NavLink to="/login" className={linkClass} onClick={() => setOpen(false)}>Sign in</NavLink>
                <NavLink to="/register" className={linkClass} onClick={() => setOpen(false)}>Create account</NavLink>
              </>
            )}
          </nav>
        </div>
      )}
    </header>
  )
}
