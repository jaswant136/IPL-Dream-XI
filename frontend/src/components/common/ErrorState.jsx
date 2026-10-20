import { AlertTriangle, RefreshCw } from 'lucide-react'

export default function ErrorState({ title = 'Something went wrong', message, onRetry, retryLabel = 'Retry' }) {
  return (
    <div role="alert" className="panel flex flex-col items-center gap-3 px-6 py-10 text-center">
      <AlertTriangle className="h-8 w-8 text-bad" aria-hidden="true" />
      <h3 className="display text-xl">{title}</h3>
      {message && <p className="max-w-md text-sm text-mute">{message}</p>}
      {onRetry && (
        <button type="button" className="btn-gold mt-2" onClick={onRetry}>
          <RefreshCw className="h-4 w-4" aria-hidden="true" /> {retryLabel}
        </button>
      )}
    </div>
  )
}
