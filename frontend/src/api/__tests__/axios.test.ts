import { describe, it, expect, vi, beforeEach } from 'vitest'

// Mock router and element-plus before importing axios
vi.mock('@/router', () => ({
  default: { push: vi.fn() },
}))

vi.mock('element-plus', () => ({
  ElMessage: { error: vi.fn(), success: vi.fn(), info: vi.fn(), warning: vi.fn() },
}))

describe('Axios Instance Config', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('has correct baseURL and timeout', async () => {
    const { default: axiosInstance } = await import('../axios')
    expect(axiosInstance.defaults.baseURL).toBe('/api')
    expect(axiosInstance.defaults.timeout).toBe(30000)
  })

  it('has Content-Type header set to application/json', async () => {
    const { default: axiosInstance } = await import('../axios')
    expect(axiosInstance.defaults.headers['Content-Type']).toBe('application/json')
  })
})
