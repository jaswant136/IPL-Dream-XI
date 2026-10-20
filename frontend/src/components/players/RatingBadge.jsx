export default function RatingBadge({ rating, hidden = false }) {
  if (hidden || rating == null) {
    return <span className="chip" title="Ratings are revealed when you complete the draft" aria-label="Rating hidden">??</span>
  }
  const tone = rating >= 85 ? 'text-gold border-gold/50' : rating >= 70 ? 'text-good border-good/40' : 'text-mute border-edge'
  return <span className={`inline-flex min-w-9 justify-center rounded-lg border px-2 py-0.5 text-sm font-bold tabular-nums ${tone}`} aria-label={`Rating ${rating}`}>{rating}</span>
}
