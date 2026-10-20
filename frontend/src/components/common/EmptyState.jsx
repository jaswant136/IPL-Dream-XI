export default function EmptyState({ icon: Icon, title, message, action }) {
  return (
    <div className="panel flex flex-col items-center gap-3 px-6 py-12 text-center">
      {Icon && <Icon className="h-9 w-9 text-mute" aria-hidden="true" />}
      <h3 className="display text-xl">{title}</h3>
      {message && <p className="max-w-md text-sm text-mute">{message}</p>}
      {action}
    </div>
  )
}
