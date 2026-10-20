import { useEffect, useRef } from 'react'

export default function ConfirmationDialog({ open, title, message, confirmLabel = 'Confirm', danger = false, onConfirm, onCancel }) {
  const cancelRef = useRef(null)
  useEffect(() => {
    if (!open) return undefined
    cancelRef.current?.focus()
    const onKey = (e) => e.key === 'Escape' && onCancel()
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [open, onCancel])
  if (!open) return null
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4" onClick={onCancel}>
      <div role="alertdialog" aria-modal="true" aria-labelledby="confirm-title" aria-describedby="confirm-msg"
        className="panel w-full max-w-sm p-6" onClick={(e) => e.stopPropagation()}>
        <h2 id="confirm-title" className="display text-2xl">{title}</h2>
        <p id="confirm-msg" className="mt-2 text-sm text-mute">{message}</p>
        <div className="mt-6 flex justify-end gap-2">
          <button ref={cancelRef} type="button" className="btn-ghost" onClick={onCancel}>Cancel</button>
          <button type="button" className={danger ? 'btn-danger' : 'btn-gold'} onClick={onConfirm}>{confirmLabel}</button>
        </div>
      </div>
    </div>
  )
}
