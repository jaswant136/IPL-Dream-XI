import { useMutation, useQueryClient } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import { addPick, completeDraft, deleteDraft, removePick } from '../services/draftService'

/** Merges a pick-mutation response into the cached draft while keeping the (large) player pool already loaded. */
function mergeDraft(old, next) {
  return { ...old, ...next, players: old?.players ?? next.players }
}

export function useDraftActions(draftId) {
  const qc = useQueryClient()
  const key = ['draft', draftId]

  const add = useMutation({
    mutationFn: (playerSeasonId) => addPick(draftId, playerSeasonId),
    onSuccess: (next) => qc.setQueryData(key, (old) => mergeDraft(old, next)),
    onError: (e) => toast.error(e.message),
  })
  const remove = useMutation({
    mutationFn: (pickId) => removePick(draftId, pickId),
    onSuccess: (next) => qc.setQueryData(key, (old) => mergeDraft(old, next)),
    onError: (e) => toast.error(e.message),
  })
  const complete = useMutation({
    mutationFn: () => completeDraft(draftId),
    onSuccess: (result) => {
      qc.setQueryData(['draft-result', draftId], result)
      qc.invalidateQueries({ queryKey: ['draft', draftId] })
      qc.invalidateQueries({ queryKey: ['drafts'] })
      qc.invalidateQueries({ queryKey: ['statistics'] })
      qc.invalidateQueries({ queryKey: ['leaderboard'] })
    },
    onError: (e) => toast.error(e.message),
  })
  const abandon = useMutation({
    mutationFn: () => deleteDraft(draftId),
    onSuccess: () => {
      qc.removeQueries({ queryKey: key })
      qc.invalidateQueries({ queryKey: ['drafts'] })
      qc.invalidateQueries({ queryKey: ['statistics'] })
    },
    onError: (e) => toast.error(e.message),
  })

  return { add, remove, complete, abandon, busy: add.isPending || remove.isPending || complete.isPending || abandon.isPending }
}
