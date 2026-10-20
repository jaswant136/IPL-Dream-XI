import { QueryClientProvider } from '@tanstack/react-query'
import { RouterProvider } from 'react-router-dom'
import { queryClient } from './app/queryClient'
import { router } from './app/router'
import { AuthProvider } from './contexts/AuthContext'
import ToastNotifications from './components/common/ToastNotifications'

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <RouterProvider router={router} />
        <ToastNotifications />
      </AuthProvider>
    </QueryClientProvider>
  )
}
