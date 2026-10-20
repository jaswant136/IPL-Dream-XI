import { describe, expect, it } from 'vitest'
import { normalizeError } from '../services/apiClient'

describe('normalizeError', () => {
  it('maps API error bodies', () => {
    const e = normalizeError({ response: { status: 422, data: { message: 'Overseas limit reached', details: ['x'] } } })
    expect(e.status).toBe(422)
    expect(e.message).toBe('Overseas limit reached')
    expect(e.details).toEqual(['x'])
  })
  it('flags network failures with a helpful message', () => {
    const e = normalizeError({ request: {}, message: 'Network Error' })
    expect(e.network).toBe(true)
    expect(e.message).toMatch(/Cannot reach the server/)
  })
})
