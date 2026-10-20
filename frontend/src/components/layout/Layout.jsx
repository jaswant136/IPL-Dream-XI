import { Outlet } from 'react-router-dom'
import AppHeader from './AppHeader'
import AppFooter from './AppFooter'

export default function Layout() {
  return (
    <div className="flex min-h-screen flex-col">
      <a href="#main" className="sr-only focus:not-sr-only focus:absolute focus:left-3 focus:top-3 focus:z-50 focus:rounded-lg focus:bg-gold focus:px-3 focus:py-2 focus:text-ink">
        Skip to content
      </a>
      <AppHeader />
      <main id="main" className="mx-auto w-full max-w-6xl flex-1 px-4 py-8">
        <Outlet />
      </main>
      <AppFooter />
    </div>
  )
}
