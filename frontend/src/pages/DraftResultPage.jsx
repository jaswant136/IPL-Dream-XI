import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Copy, Link2Off, Share2 } from 'lucide-react'
import toast from 'react-hot-toast'
import { fetchResult, revokeShare, shareDraft } from '../services/draftService'
import DraftResult from '../components/draft/DraftResult'
import ErrorState from '../components/common/ErrorState'
import LoadingSpinner from '../components/common/LoadingSpinner'

export default function DraftResultPage() {
  const { draftId } = useParams()
  const qc = useQueryClient()
  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['draft-result', draftId],
    queryFn: () => fetchResult(draftId),
  })
  const [shareToken, setShareToken] = useState(null)
  const token = shareToken === null ? data?.shareToken : shareToken

  const share = useMutation({
    mutationFn: () => shareDraft(draftId),
    onSuccess: (r) => {
      setShareToken(r.shareToken)
      qc.invalidateQueries({ queryKey: ['drafts'] })
      toast.success('Share link created')
    },
    onError: (e) => toast.error(e.message),
  })
  const revoke = useMutation({
    mutationFn: () => revokeShare(draftId),
    onSuccess: () => {
      setShareToken('')
      qc.invalidateQueries({ queryKey: ['drafts'] })
      toast.success('Share link revoked - it no longer works')
    },
    onError: (e) => toast.error(e.message),
  })

  if (isLoading) return <LoadingSpinner label="Loading result…" className="min-h-[40vh]" />
  if (isError) return <ErrorState title="Could not load the result" message={error.message} onRetry={error.status === 409 || error.status === 404 ? undefined : refetch} />

  const url = token ? `${window.location.origin}/shared/${token}` : null
  async function copy() {
    try {
      await navigator.clipboard.writeText(url)
      toast.success('Link copied')
    } catch {
      toast.error('Could not copy - select the link and copy it manually.')
    }
  }

  const actions = (
    <div className="panel space-y-3 p-4">
      <div className="flex flex-wrap gap-2">
        <Link to="/play" className="btn-gold">Draft again</Link>
        <Link to="/drafts" className="btn-ghost">My drafts</Link>
        {!url && <button type="button" className="btn-ghost" disabled={share.isPending} onClick={() => share.mutate()}><Share2 className="h-4 w-4" aria-hidden="true" />Share XI</button>}
      </div>
      {url && (
        <div className="space-y-2">
          <label htmlFor="share-url" className="label">Public link</label>
          <div className="flex gap-2">
            <input id="share-url" readOnly value={url} className="input" onFocus={(e) => e.target.select()} />
            <button type="button" className="btn-ghost" onClick={copy} aria-label="Copy share link"><Copy className="h-4 w-4" /></button>
            <button type="button" className="btn-danger" disabled={revoke.isPending} onClick={() => revoke.mutate()} aria-label="Revoke share link"><Link2Off className="h-4 w-4" /></button>
          </div>
          <p className="text-xs text-mute">Anyone with this link can view this XI (no personal details). Revoke to disable it.</p>
        </div>
      )}
    </div>
  )

  return <DraftResult result={data} actions={actions} />
}
