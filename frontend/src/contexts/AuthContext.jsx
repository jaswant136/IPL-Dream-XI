import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { onAuthStateChanged } from 'firebase/auth'
import { auth, firebaseConfigured } from '../firebase/firebase'
import * as authService from '../services/authService'
import { setUnauthorizedHandler } from '../services/apiClient'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [currentUser, setCurrentUser] = useState(null)
  const [loading, setLoading] = useState(firebaseConfigured)

  useEffect(() => {
    if (!auth) return undefined
    // Firebase restores the persisted session, then fires this once with the user (or null).
    return onAuthStateChanged(auth, (user) => {
      setCurrentUser(user)
      setLoading(false)
    })
  }, [])

  const logout = useCallback(() => authService.logout(), [])
  useEffect(() => {
    setUnauthorizedHandler(() => {
      if (auth?.currentUser) authService.logout().catch(() => {})
    })
  }, [])

  const value = useMemo(
    () => ({
      currentUser,
      loading,
      firebaseConfigured,
      login: authService.loginWithEmail,
      register: authService.registerWithEmail,
      loginWithGoogle: authService.loginWithGoogle,
      logout,
      resetPassword: authService.resetPassword,
      resendVerification: authService.resendVerification,
    }),
    [currentUser, loading, logout],
  )
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>')
  return ctx
}
