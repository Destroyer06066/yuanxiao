import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { usePermissionStore } from '../permission'

vi.mock('@/api/permission', () => ({
  getPermissionTree: vi.fn(),
}))

describe('Permission Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('initial state: empty modules, loaded=false', () => {
    const store = usePermissionStore()
    expect(store.modules).toEqual([])
    expect(store.loaded).toBe(false)
  })

  it('fetchModules loads data and sets loaded=true', async () => {
    const { getPermissionTree } = await import('@/api/permission')
    const mockModules = [
      {
        module: 'major',
        moduleLabel: '专业管理',
        permissions: [
          { id: '1', module: 'major', action: 'create', label: '创建', isExplicit: true, isRestricted: false },
        ],
      },
    ]
    vi.mocked(getPermissionTree).mockResolvedValue({ data: mockModules } as any)

    const store = usePermissionStore()
    await store.fetchModules()

    expect(store.modules).toEqual(mockModules)
    expect(store.loaded).toBe(true)
    expect(getPermissionTree).toHaveBeenCalledTimes(1)
  })

  it('idempotent: second call does not re-fetch', async () => {
    const { getPermissionTree } = await import('@/api/permission')
    vi.mocked(getPermissionTree).mockResolvedValue({ data: [] } as any)

    const store = usePermissionStore()
    await store.fetchModules()
    await store.fetchModules()

    expect(getPermissionTree).toHaveBeenCalledTimes(1)
  })
})
