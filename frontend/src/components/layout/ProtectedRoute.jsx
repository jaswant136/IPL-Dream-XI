import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'
import LoadingSpinner from '../common/LoadingSpinner'

export default function ProtectedRoute() {
  const { currentUser, loading } = useAuth()
  const location = useLocation()
  if (loading) return <LoadingSpinner label="Checking your session…" className="min-h-[50vh]" />
  if (!currentUser) return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />
  return <Outlet />
}
