export default function PageHeader({ title, subtitle, actions }) {
  return (
    <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
      <div>
        <h1 className="display text-4xl sm:text-5xl">{title}</h1>
        {subtitle && <p className="mt-1 text-sm text-mute">{subtitle}</p>}
      </div>
      {actions}
    </div>
  )
}
