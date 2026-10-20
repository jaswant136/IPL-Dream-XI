import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import { useAuth } from '../contexts/AuthContext'
import { fetchMe, updateProfile } from '../services/draftService'
import { formatDate } from '../utils/formatters'
import { profileSchema } from '../utils/schemas'
import PageHeader from '../components/common/PageHeader'
import ErrorState from '../components/common/ErrorState'
import LoadingSpinner from '../components/common/LoadingSpinner'
import FormField from '../components/common/FormField'

export default function ProfilePage() {
  const { currentUser, resendVerification } = useAuth()
  const qc = useQueryClient()
  const me = useQuery({ queryKey: ['me'], queryFn: fetchMe })
  const { register, handleSubmit, reset, formState: { errors, isDirty } } = useForm({ resolver: zodResolver(profileSchema), defaultValues: { displayName: '' } })

  useEffect(() => { if (me.data) reset({ displayName: me.data.displayName }) }, [me.data, reset])

  const save = useMutation({
    mutationFn: ({ displayName }) => updateProfile(displayName),
    onSuccess: (data) => {
      qc.setQueryData(['me'], data)
      qc.invalidateQueries({ queryKey: ['leaderboard'] })
      toast.success('Profile updated')
    },
    onError: (e) => toast.error(e.message),
  })

  if (me.isLoading) return <LoadingSpinner />
  if (me.isError) return <ErrorState message={me.error.message} onRetry={me.refetch} />

  async function onResend() {
    try { await resendVerification(); toast.success('Verification email sent') } catch (e) { toast.error(e.message) }
  }

  return (
    <div className="mx-auto max-w-lg">
      <PageHeader title="Profile" subtitle={`Member since ${formatDate(me.data.createdAt)}`} />
      <div className="panel space-y-5 p-6">
        <div>
          <p className="label">Email</p>
          <p className="text-sm">{me.data.email ?? currentUser?.email ?? '—'}</p>
          <p className="mt-1 text-xs text-mute">Only you can see this. It is never shown on the leaderboard or shared pages.</p>
          {!me.data.emailVerified && currentUser?.providerData?.[0]?.providerId === 'password' && (
            <button type="button" className="mt-2 text-xs text-gold hover:underline" onClick={onResend}>Email not verified - resend verification</button>
          )}
        </div>
        <form onSubmit={handleSubmit((v) => save.mutate(v))} noValidate className="space-y-4">
          <FormField id="displayName" label="Display name (public)" error={errors.displayName?.message}>
            <input id="displayName" className="input" {...register('displayName')} />
          </FormField>
          <button type="submit" className="btn-gold" disabled={!isDirty || save.isPending}>{save.isPending ? 'Saving…' : 'Save changes'}</button>
        </form>
      </div>
    </div>
  )
}
