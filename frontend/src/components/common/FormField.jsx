export default function FormField({ id, label, error, children }) {
  return (
    <div>
      <label htmlFor={id} className="label">{label}</label>
      {children}
      {error && <p id={`${id}-error`} role="alert" className="mt-1 text-xs text-bad">{error}</p>}
    </div>
  )
}
