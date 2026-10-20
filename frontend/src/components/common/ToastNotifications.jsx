import { Toaster } from 'react-hot-toast'

export default function ToastNotifications() {
  return (
    <Toaster
      position="top-center"
      toastOptions={{
        style: { background: '#17181C', color: '#F3F3F4', border: '1px solid #292A30', fontSize: '14px' },
        success: { iconTheme: { primary: '#4ADE80', secondary: '#101114' } },
        error: { iconTheme: { primary: '#FB7185', secondary: '#101114' } },
      }}
    />
  )
}
