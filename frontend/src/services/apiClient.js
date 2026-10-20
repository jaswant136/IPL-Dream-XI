import axios from 'axios'
import { auth } from '../firebase/firebase'

const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1'

export const apiClient = axios.create({ baseURL, timeout: 20000 })

/** Normalised error thrown by every API call: { status, message, details, network } */
export class ApiError extends Error {
  constructor({ status = 0, message, details = [], network = false }) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.details = details
    this.network = network
  }
}

export function normalizeError(error) {
  if (axios.isCancel?.(error) || error?.code === 'ERR_CANCELED') {
    return new ApiError({ status: 0, message: 'Request cancelled.' })
  }
  if (error?.response) {
    const data = error.response.data
    return new ApiError({
      status: error.response.status,
      message: data?.message || `Request failed (${error.response.status}).`,
      details: data?.details ?? [],
    })
  }
  if (error?.request || error?.code === 'ECONNABORTED') {
    return new ApiError({
      network: true,
      message: 'Cannot reach the server. Check your connection and that the backend is running.',
    })
  }
  return new ApiError({ message: error?.message || 'Something went wrong.' })
}

// Attach a fresh Firebase ID token (the SDK refreshes it automatically when it is close to expiry).
apiClient.interceptors.request.use(async (config) => {
  const user = auth?.currentUser
  if (user) {
    const token = await user.getIdToken()
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

let onUnauthorized = null
export function setUnauthorizedHandler(fn) {
  onUnauthorized = fn
}

apiClient.interceptors.response.use(
  (r) => r,
  async (error) => {
    const original = error.config
    // One forced-refresh retry on 401, flagged so it can never loop.
    if (error.response?.status === 401 && original && !original._retried && auth?.currentUser) {
      original._retried = true
      try {
        const token = await auth.currentUser.getIdToken(true)
        original.headers.Authorization = `Bearer ${token}`
        return await apiClient(original)
      } catch (retryError) {
        if (retryError?.response?.status === 401) onUnauthorized?.()
        throw normalizeError(retryError)
      }
    }
    throw normalizeError(error)
  },
)

export const get = (url, config) => apiClient.get(url, config).then((r) => r.data)
export const post = (url, body, config) => apiClient.post(url, body, config).then((r) => r.data)
export const put = (url, body, config) => apiClient.put(url, body, config).then((r) => r.data)
export const del = (url, config) => apiClient.delete(url, config).then((r) => r.data)
