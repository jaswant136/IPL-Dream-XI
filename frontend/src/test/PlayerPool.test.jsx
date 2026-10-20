import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import PlayerPool from '../components/players/PlayerPool'
import { player } from './fixtures'

const base = { pickedIds: new Set(), ratingsHidden: false, onAdd: vi.fn(), busy: false, onRetry: vi.fn() }
const pool = [
  player({ id: '1', name: 'Alpha Keeper', role: 'WICKETKEEPER' }),
  player({ id: '2', name: 'Beta Bowler', role: 'BOWLER', country: 'Sri Lanka', overseas: true }),
  player({ id: '3', name: 'Gamma Bat', role: 'BATTER' }),
]

describe('PlayerPool states', () => {
  it('shows skeleton loaders while loading', () => {
    render(<PlayerPool {...base} players={undefined} loading />)
    expect(screen.getByRole('status', { name: 'Loading players' })).toBeInTheDocument()
  })

  it('shows a visible error with Retry instead of a blank list', async () => {
    const onRetry = vi.fn()
    render(<PlayerPool {...base} players={undefined} error="Network down" onRetry={onRetry} />)
    expect(screen.getByRole('alert')).toHaveTextContent('Network down')
    await userEvent.click(screen.getByRole('button', { name: /retry/i }))
    expect(onRetry).toHaveBeenCalled()
  })

  it('never silently renders an empty pool', () => {
    const spy = vi.spyOn(console, 'error').mockImplementation(() => {})
    render(<PlayerPool {...base} players={[]} />)
    expect(screen.getByRole('alert')).toHaveTextContent('No players available')
    expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument()
    expect(spy).toHaveBeenCalled()
    spy.mockRestore()
  })

  it('filters by role', async () => {
    render(<PlayerPool {...base} players={pool} />)
    await userEvent.click(screen.getByRole('button', { name: /^Bowlers/ }))
    const list = screen.getByRole('list', { name: /players/ })
    expect(within(list).getByText('Beta Bowler')).toBeInTheDocument()
    expect(within(list).queryByText('Alpha Keeper')).not.toBeInTheDocument()
  })

  it('adds a player through the Add button', async () => {
    const onAdd = vi.fn()
    render(<PlayerPool {...base} players={pool} onAdd={onAdd} />)
    await userEvent.click(screen.getByRole('button', { name: 'Add Alpha Keeper to your XI' }))
    expect(onAdd).toHaveBeenCalledWith(expect.objectContaining({ id: '1' }))
  })

  it('marks drafted players and disables their button', () => {
    render(<PlayerPool {...base} players={pool} pickedIds={new Set(['1'])} />)
    expect(screen.getByRole('button', { name: 'Alpha Keeper already drafted' })).toBeDisabled()
  })

  it('shows the overseas restriction message', () => {
    render(<PlayerPool {...base} players={pool} blockReason={(p) => (p.overseas ? 'Overseas limit (4) reached' : null)} />)
    expect(screen.getByText('Overseas limit (4) reached')).toBeInTheDocument()
  })

  it('hides ratings in expert mode', () => {
    render(<PlayerPool {...base} players={pool.map((p) => ({ ...p, rating: null }))} ratingsHidden />)
    expect(screen.getAllByLabelText('Rating hidden')).toHaveLength(3)
  })
})
