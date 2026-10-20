import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import toast from 'react-hot-toast'
import { useAuth } from '../contexts/AuthContext'
import { authErrorMessage } from '../services/authService'
import { loginSchema } from '../utils/schemas'
import AuthCard from '../components/common/AuthCard'
import FormField from '../components/common/FormField'

export default function LoginPage() {
  const { login, loginWithGoogle, currentUser } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const redirectTo = location.state?.from || '/dashboard'
  const [formError, setFormError] = useState('')
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm({ resolver: zodResolver(loginSchema) })

  if (currentUser) return <Navigate to={redirectTo} replace />

  async function onSubmit(values) {
    setFormError('')
    try {
      await login(values)
      toast.success('Welcome back!')
      navigate(redirectTo, { replace: true })
    } catch (e) {
      setFormError(authErrorMessage(e))
    }
  }

  async function onGoogle() {
    setFormError('')
    try {
      await loginWithGoogle()
      navigate(redirectTo, { replace: true })
    } catch (e) {
      setFormError(authErrorMessage(e))
    }
  }

  return (
    <AuthCard title="Sign in" subtitle="Pick up your drafts where you left off."
      footer={<>New here? <Link className="text-gold hover:underline" to="/register">Create an account</Link></>}>
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">
        <FormField id="email" label="Email" error={errors.email?.message}>
          <input id="email" type="email" autoComplete="email" className="input" aria-invalid={!!errors.email} {...register('email')} />
        </FormField>
        <FormField id="password" label="Password" error={errors.password?.message}>
          <input id="password" type="password" autoComplete="current-password" className="input" aria-invalid={!!errors.password} {...register('password')} />
        </FormField>
        {formError && <p role="alert" className="rounded-lg border border-bad/40 bg-bad/10 px-3 py-2 text-sm text-bad">{formError}</p>}
        <button type="submit" className="btn-gold w-full" disabled={isSubmitting}>{isSubmitting ? 'Signing in…' : 'Sign in'}</button>
        <div className="text-right"><Link to="/forgot-password" className="text-xs text-mute hover:text-gold">Forgot password?</Link></div>
      </form>
      <div className="my-5 flex items-center gap-3 text-xs text-mute"><span className="h-px flex-1 bg-edge" />or<span className="h-px flex-1 bg-edge" /></div>
      <button type="button" className="btn-ghost w-full" onClick={onGoogle}>Continue with Google</button>
    </AuthCard>
  )
}
