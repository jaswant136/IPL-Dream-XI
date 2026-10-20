export function Skeleton({ className = '' }) {
  return <div aria-hidden="true" className={`animate-pulse rounded-lg bg-edge/60 ${className}`} />
}

export function PlayerListSkeleton({ rows = 8 }) {
  return (
    <div role="status" aria-label="Loading players" className="space-y-2">
      {Array.from({ length: rows }).map((_, i) => (
        <Skeleton key={i} className="h-14 w-full" />
      ))}
    </div>
  )
}
