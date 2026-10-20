import { AlertTriangle } from 'lucide-react'
import { firebaseConfigured } from '../../firebase/firebase'

export default function AuthCard({ title, subtitle, children, footer }) {
  return (
    <div className="mx-auto w-full max-w-md">
      <div className="panel p-6 sm:p-8">
        <h1 className="display text-4xl">{title}</h1>
        {subtitle && <p className="mt-1 text-sm text-mute">{subtitle}</p>}
        {!firebaseConfigured && (
          <div role="alert" className="mt-4 flex gap-2 rounded-xl border border-gold/40 bg-gold/10 p-3 text-xs text-gold">
            <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
            <span>Firebase is not configured. Copy <code>frontend/.env.example</code> to <code>frontend/.env</code> and fill in your Firebase web app values, then restart the dev server.</span>
          </div>
        )}
        <div className="mt-6">{children}</div>
      </div>
      {footer && <p className="mt-4 text-center text-sm text-mute">{footer}</p>}
    </div>
  )
}
