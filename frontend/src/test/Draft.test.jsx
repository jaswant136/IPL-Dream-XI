import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import DraftProgress from '../components/draft/DraftProgress'
import PlayingXI from '../components/draft/PlayingXI'
import DraftResult from '../components/draft/DraftResult'
import { player, resultFixture } from './fixtures'

describe('DraftProgress', () => {
  it('reads "7 / 11 PLAYERS SELECTED"', () => {
    render(<DraftProgress count={7} total={11} />)
    expect(screen.getByText('7 / 11 PLAYERS SELECTED')).toBeInTheDocument()
    expect(screen.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '7')
  })
})

describe('PlayingXI', () => {
  const picks = [{ pickId: 'k1', pickNumber: 1, player: player({ id: 'a', name: 'Keeper One', role: 'WICKETKEEPER' }) }]

  it('renders filled and empty slots with progress and overseas count', () => {
    render(<PlayingXI picks={picks} squadSize={11} overseasCount={2} maxOverseas={4} onRemove={() => {}} />)
    expect(screen.getByText('1 / 11 PLAYERS SELECTED')).toBeInTheDocument()
    expect(screen.getByText('Keeper One')).toBeInTheDocument()
    expect(screen.getAllByText('Empty slot')).toHaveLength(10)
    expect(screen.getByText('2 / 4')).toBeInTheDocument()
  })

  it('removes a player', async () => {
    const onRemove = vi.fn()
    render(<PlayingXI picks={picks} squadSize={11} onRemove={onRemove} />)
    await userEvent.click(screen.getByRole('button', { name: 'Remove Keeper One from your XI' }))
    expect(onRemove).toHaveBeenCalledWith(expect.objectContaining({ pickId: 'k1' }))
  })

  it('has no remove buttons when read-only', () => {
    render(<PlayingXI picks={picks} squadSize={11} readOnly />)
    expect(screen.queryByRole('button', { name: /remove/i })).not.toBeInTheDocument()
  })
})

describe('DraftResult', () => {
  it('shows the server-calculated rating, breakdown and lineup', () => {
    render(<MemoryRouter><DraftResult result={resultFixture} /></MemoryRouter>)
    expect(screen.getByLabelText('Team rating 84')).toBeInTheDocument()
    expect(screen.getByText('Chennai Super Kings · IPL 2023')).toBeInTheDocument()
    expect(screen.getByText('Keeper One')).toBeInTheDocument()
    expect(screen.getByText('2:05')).toBeInTheDocument()
    expect(screen.getByText('Role balance')).toBeInTheDocument()
  })
})
