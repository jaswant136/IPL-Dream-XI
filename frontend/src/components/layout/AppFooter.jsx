import { DISCLAIMER } from '../../utils/constants'
import { BrandMark } from './AppHeader'

export default function AppFooter() {
  return (
    <footer className="mt-20 border-t border-edge">
      <div className="mx-auto flex max-w-6xl flex-col gap-4 px-4 py-10 sm:flex-row sm:items-start sm:justify-between">
        <BrandMark />
        <p className="max-w-xl text-xs leading-relaxed text-mute">{DISCLAIMER}</p>
      </div>
    </footer>
  )
}
