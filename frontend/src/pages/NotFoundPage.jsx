import { Link } from 'react-router-dom'

export default function NotFoundPage() {
  return (
    <div className="flex flex-col items-center gap-4 py-24 text-center">
      <p className="display text-8xl text-gold">404</p>
      <h1 className="display text-3xl">Bowled out - page not found</h1>
      <p className="text-sm text-mute">That page does not exist or has moved.</p>
      <Link to="/" className="btn-gold">Back to home</Link>
    </div>
  )
}
