import { createBrowserRouter } from 'react-router-dom'
import Layout from '../components/layout/Layout'
import ProtectedRoute from '../components/layout/ProtectedRoute'
import HomePage from '../pages/HomePage'
import LoginPage from '../pages/LoginPage'
import RegisterPage from '../pages/RegisterPage'
import ForgotPasswordPage from '../pages/ForgotPasswordPage'
import DashboardPage from '../pages/DashboardPage'
import PlayPage from '../pages/PlayPage'
import FranchiseDraftPage from '../pages/FranchiseDraftPage'
import DraftPage from '../pages/DraftPage'
import DraftResultPage from '../pages/DraftResultPage'
import MyDraftsPage from '../pages/MyDraftsPage'
import LeaderboardPage from '../pages/LeaderboardPage'
import ProfilePage from '../pages/ProfilePage'
import SharedDraftPage from '../pages/SharedDraftPage'
import NotFoundPage from '../pages/NotFoundPage'

export const routes = [
  {
    element: <Layout />,
    children: [
      { path: '/', element: <HomePage /> },
      { path: '/login', element: <LoginPage /> },
      { path: '/register', element: <RegisterPage /> },
      { path: '/forgot-password', element: <ForgotPasswordPage /> },
      { path: '/leaderboard', element: <LeaderboardPage /> },
      { path: '/shared/:token', element: <SharedDraftPage /> },
      {
        element: <ProtectedRoute />,
        children: [
          { path: '/dashboard', element: <DashboardPage /> },
          { path: '/play', element: <PlayPage /> },
          { path: '/franchise-draft', element: <FranchiseDraftPage /> },
          { path: '/draft/:draftId', element: <DraftPage /> },
          { path: '/drafts', element: <MyDraftsPage /> },
          { path: '/drafts/:draftId/result', element: <DraftResultPage /> },
          { path: '/profile', element: <ProfilePage /> },
        ],
      },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
]

export const router = createBrowserRouter(routes)
