import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import ProtectedRoute from '../components/layout/ProtectedRoute'

let state = { currentUser: null, loading: false }
vi.mock('../contexts/AuthContext', () => ({ useAuth: () => state }))

const setup = () => render(
  <MemoryRouter initialEntries={['/secret']}>
    <Routes>
      <Route element={<ProtectedRoute />}><Route path="/secret" element={<p>secret area</p>} /></Route>
      <Route path="/login" element={<p>login page</p>} />
    </Routes>
  </MemoryRouter>,
)

describe('ProtectedRoute', () => {
  it('redirects anonymous users to /login', () => {
    state = { currentUser: null, loading: false }
    setup()
    expect(screen.getByText('login page')).toBeInTheDocument()
  })
  it('shows a loading state while the session is restored', () => {
    state = { currentUser: null, loading: true }
    setup()
    expect(screen.getByRole('status')).toHaveTextContent('Checking your session')
  })
  it('renders the page for a signed-in user', () => {
    state = { currentUser: { uid: 'u' }, loading: false }
    setup()
    expect(screen.getByText('secret area')).toBeInTheDocument()
  })
})
