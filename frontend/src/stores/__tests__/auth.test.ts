import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '../auth'

// Mock dependencies
vi.mock('@/api/auth', () => ({
  login: vi.fn(),
  getCurrentUser: vi.fn(),
  logout: vi.fn(),
}))

vi.mock('@/router', () => ({
  default: { push: vi.fn() },
}))

vi.mock('@/stores/permission', () => ({
  usePermissionStore: () => ({
    fetchModules: vi.fn(),
  }),
}))

describe('Auth Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('initial state: no token when localStorage is empty', () => {
    const store = useAuthStore()
    expect(store.token).toBe('')
    expect(store.userInfo).toBeNull()
    expect(store.role).toBe('')
    expect(store.isOpAdmin).toBe(false)
  })

  it('reads token from localStorage on init', () => {
    localStorage.setItem('access_token', 'test-token-123')
    const store = useAuthStore()
    expect(store.token).toBe('test-token-123')
  })

  it('loginAction: calls API and stores token on success', async () => {
    const { login } = await import('@/api/auth')
    const mockResponse = {
      data: {
        data: {
          accessToken: 'new-token',
          role: 'SCHOOL_ADMIN',
          schoolId: 'school-1',
          realName: '张三',
          requirePasswordChange: false,
        },
      },
    }
    vi.mocked(login).mockResolvedValue(mockResponse as any)

    const store = useAuthStore()
    const result = await store.loginAction({ username: 'admin', password: '123456' })

    expect(login).toHaveBeenCalledWith({ username: 'admin', password: '123456' })
    expect(store.token).toBe('new-token')
    expect(localStorage.getItem('access_token')).toBe('new-token')
    expect(result.accessToken).toBe('new-token')
  })

  it('logoutAction: clears token and userInfo', async () => {
    const { logout } = await import('@/api/auth')
    vi.mocked(logout).mockResolvedValue({} as any)

    const store = useAuthStore()
    store.token = 'existing-token'
    store.userInfo = {
      accountId: '1',
      username: 'admin',
      role: 'OP_ADMIN',
      schoolId: null,
      realName: '管理员',
      requirePasswordChange: false,
      accessToken: 'existing-token',
    }
    localStorage.setItem('access_token', 'existing-token')

    await store.logoutAction()

    expect(store.token).toBe('')
    expect(store.userInfo).toBeNull()
    expect(localStorage.getItem('access_token')).toBeNull()
  })

  it('role/isOpAdmin computed properties derive from userInfo', () => {
    const store = useAuthStore()

    // No userInfo
    expect(store.role).toBe('')
    expect(store.isOpAdmin).toBe(false)

    // Set OP_ADMIN
    store.userInfo = {
      accountId: '1',
      username: 'admin',
      role: 'OP_ADMIN',
      schoolId: null,
      realName: '管理员',
      requirePasswordChange: false,
      accessToken: 'token',
    }
    expect(store.role).toBe('OP_ADMIN')
    expect(store.isOpAdmin).toBe(true)
    expect(store.realName).toBe('管理员')
    expect(store.accountId).toBe('1')

    // Set SCHOOL_ADMIN
    store.userInfo = {
      accountId: '2',
      username: 'school',
      role: 'SCHOOL_ADMIN',
      schoolId: 'school-1',
      realName: '校管理',
      requirePasswordChange: false,
      accessToken: 'token',
    }
    expect(store.role).toBe('SCHOOL_ADMIN')
    expect(store.isOpAdmin).toBe(false)
    expect(store.schoolId).toBe('school-1')
  })
})
