import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import LoginPage from '../pages/LoginPage'

const login = vi.fn().mockResolvedValue({})
vi.mock('../contexts/AuthContext', () => ({
  useAuth: () => ({ login, loginWithGoogle: vi.fn(), currentUser: null }),
}))
vi.mock('react-hot-toast', () => ({ default: { success: vi.fn(), error: vi.fn() } }))

const setup = () => render(<MemoryRouter><LoginPage /></MemoryRouter>)

describe('LoginPage validation', () => {
  it('shows required-field errors and does not call login', async () => {
    setup()
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }))
    expect(await screen.findByText('Email is required')).toBeInTheDocument()
    expect(screen.getByText('Password is required')).toBeInTheDocument()
    expect(login).not.toHaveBeenCalled()
  })

  it('rejects a malformed email', async () => {
    setup()
    await userEvent.type(screen.getByLabelText('Email'), 'not-an-email')
    await userEvent.type(screen.getByLabelText('Password'), 'secret123')
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }))
    expect(await screen.findByText('Enter a valid email address')).toBeInTheDocument()
    expect(login).not.toHaveBeenCalled()
  })

  it('submits valid credentials', async () => {
    setup()
    await userEvent.type(screen.getByLabelText('Email'), 'a@b.co')
    await userEvent.type(screen.getByLabelText('Password'), 'secret123')
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }))
    await vi.waitFor(() => expect(login).toHaveBeenCalledWith({ email: 'a@b.co', password: 'secret123' }))
  })
})
