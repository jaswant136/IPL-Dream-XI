import { ROLE_FILTERS } from '../../utils/constants'

export default function RoleFilter({ value, onChange, counts = {} }) {
  return (
    <div role="group" aria-label="Filter players by role" className="flex flex-wrap gap-1.5">
      {ROLE_FILTERS.map((f) => {
        const active = value === f.key
        return (
          <button key={f.key} type="button" aria-pressed={active} onClick={() => onChange(f.key)}
            className={`rounded-full border px-3 py-1 text-xs font-semibold transition ${
              active ? 'border-gold bg-gold text-ink' : 'border-edge text-mute hover:border-gold/60 hover:text-snow'}`}>
            {f.label}
            {counts[f.key] != null && <span className="ml-1.5 opacity-70">{counts[f.key]}</span>}
          </button>
        )
      })}
    </div>
  )
}
