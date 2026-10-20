import { Loader2 } from 'lucide-react'

export default function LoadingSpinner({ label = 'Loading…', className = '' }) {
  return (
    <div role="status" aria-live="polite" className={`flex items-center justify-center gap-3 py-12 text-mute ${className}`}>
      <Loader2 className="h-5 w-5 animate-spin text-gold" aria-hidden="true" />
      <span className="text-sm">{label}</span>
    </div>
  )
}
