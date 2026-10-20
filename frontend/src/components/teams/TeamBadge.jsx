/** Text-only franchise badge (no club logos are used - they are protected marks). */
export default function TeamBadge({ code, size = 'md' }) {
  const dims = size === 'lg' ? 'h-16 w-16 text-2xl' : size === 'sm' ? 'h-8 w-8 text-xs' : 'h-11 w-11 text-sm'
  return (
    <span aria-hidden="true"
      className={`display inline-flex ${dims} shrink-0 items-center justify-center rounded-xl border border-gold/40 bg-gold/10 text-gold`}>
      {code ?? 'XI'}
    </span>
  )
}
