import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { authErrorMessage } from '../services/authService'
import { resetSchema } from '../utils/schemas'
import AuthCard from '../components/common/AuthCard'
import FormField from '../components/common/FormField'

export default function ForgotPasswordPage() {
  const { resetPassword } = useAuth()
  const [sent, setSent] = useState(false)
  const [formError, setFormError] = useState('')
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm({ resolver: zodResolver(resetSchema) })

  async function onSubmit({ email }) {
    setFormError('')
    try {
      await resetPassword(email)
      setSent(true)
    } catch (e) {
      setFormError(authErrorMessage(e))
    }
  }

  return (
    <AuthCard title="Reset password" subtitle="We will email you a reset link."
      footer={<Link className="text-gold hover:underline" to="/login">Back to sign in</Link>}>
      {sent ? (
        <p role="status" className="rounded-lg border border-good/40 bg-good/10 px-3 py-3 text-sm text-good">
          If an account exists for that address, a reset link is on its way.
        </p>
      ) : (
        <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">
          <FormField id="email" label="Email" error={errors.email?.message}>
            <input id="email" type="email" autoComplete="email" className="input" {...register('email')} />
          </FormField>
          {formError && <p role="alert" className="rounded-lg border border-bad/40 bg-bad/10 px-3 py-2 text-sm text-bad">{formError}</p>}
          <button type="submit" className="btn-gold w-full" disabled={isSubmitting}>{isSubmitting ? 'Sending…' : 'Send reset link'}</button>
        </form>
      )}
    </AuthCard>
  )
}
