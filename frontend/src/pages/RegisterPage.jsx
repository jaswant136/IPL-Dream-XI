import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import toast from 'react-hot-toast'
import { useAuth } from '../contexts/AuthContext'
import { authErrorMessage } from '../services/authService'
import { registerSchema } from '../utils/schemas'
import AuthCard from '../components/common/AuthCard'
import FormField from '../components/common/FormField'

export default function RegisterPage() {
  const { register: registerUser, loginWithGoogle, currentUser } = useAuth()
  const navigate = useNavigate()
  const [formError, setFormError] = useState('')
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm({ resolver: zodResolver(registerSchema) })

  if (currentUser) return <Navigate to="/dashboard" replace />

  async function onSubmit({ displayName, email, password }) {
    setFormError('')
    try {
      await registerUser({ displayName, email, password })
      toast.success('Account created. We sent a verification email - check your inbox.')
      navigate('/dashboard', { replace: true })
    } catch (e) {
      setFormError(authErrorMessage(e))
    }
  }

  async function onGoogle() {
    setFormError('')
    try {
      await loginWithGoogle()
      navigate('/dashboard', { replace: true })
    } catch (e) {
      setFormError(authErrorMessage(e))
    }
  }

  return (
    <AuthCard title="Create account" subtitle="Free. Draft as many XIs as you like."
      footer={<>Already registered? <Link className="text-gold hover:underline" to="/login">Sign in</Link></>}>
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">
        <FormField id="displayName" label="Display name (shown on leaderboard)" error={errors.displayName?.message}>
          <input id="displayName" autoComplete="nickname" className="input" aria-invalid={!!errors.displayName} {...register('displayName')} />
        </FormField>
        <FormField id="email" label="Email" error={errors.email?.message}>
          <input id="email" type="email" autoComplete="email" className="input" aria-invalid={!!errors.email} {...register('email')} />
        </FormField>
        <FormField id="password" label="Password" error={errors.password?.message}>
          <input id="password" type="password" autoComplete="new-password" className="input" aria-invalid={!!errors.password} {...register('password')} />
        </FormField>
        <FormField id="confirm" label="Confirm password" error={errors.confirm?.message}>
          <input id="confirm" type="password" autoComplete="new-password" className="input" aria-invalid={!!errors.confirm} {...register('confirm')} />
        </FormField>
        {formError && <p role="alert" className="rounded-lg border border-bad/40 bg-bad/10 px-3 py-2 text-sm text-bad">{formError}</p>}
        <button type="submit" className="btn-gold w-full" disabled={isSubmitting}>{isSubmitting ? 'Creating…' : 'Create account'}</button>
      </form>
      <div className="my-5 flex items-center gap-3 text-xs text-mute"><span className="h-px flex-1 bg-edge" />or<span className="h-px flex-1 bg-edge" /></div>
      <button type="button" className="btn-ghost w-full" onClick={onGoogle}>Continue with Google</button>
    </AuthCard>
  )
}
