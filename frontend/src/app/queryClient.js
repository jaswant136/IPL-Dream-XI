import { QueryClient } from '@tanstack/react-query'

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 15_000,
      refetchOnWindowFocus: false,
      // Do not hammer the API on client errors (4xx); retry transient/network failures once.
      retry: (count, error) => (error?.status >= 400 && error?.status < 500 ? false : count < 1),
    },
  },
})
