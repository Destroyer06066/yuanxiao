import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { usePermission } from '../usePermission'
import { useAuthStore } from '@/stores/auth'
import { usePermissionStore } from '@/stores/permission'

vi.mock('@/api/auth', () => ({
  login: vi.fn(),
  getCurrentUser: vi.fn(),
  logout: vi.fn(),
}))

vi.mock('@/api/permission', () => ({
  getPermissionTree: vi.fn(),
}))

vi.mock('@/router', () => ({
  default: { push: vi.fn() },
}))

describe('usePermission', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('OP_ADMIN: can() always true', () => {
    const authStore = useAuthStore()
    authStore.userInfo = {
      accountId: '1',
      username: 'admin',
      role: 'OP_ADMIN',
      schoolId: null,
      realName: '管理员',
      requirePasswordChange: false,
      accessToken: 'token',
    }

    const { can } = usePermission()
    expect(can('major:create').value).toBe(true)
    expect(can('student:read').value).toBe(true)
    expect(can('anything:whatever').value).toBe(true)
  })

  it('SCHOOL_ADMIN with matching permission: can() returns true', () => {
    const authStore = useAuthStore()
    authStore.userInfo = {
      accountId: '2',
      username: 'school',
      role: 'SCHOOL_ADMIN',
      schoolId: 'school-1',
      realName: '校管理',
      requirePasswordChange: false,
      accessToken: 'token',
    }

    const permStore = usePermissionStore()
    permStore.loaded = true
    permStore.modules = [
      {
        module: 'major',
        moduleLabel: '专业管理',
        permissions: [
          { id: '1', module: 'major', action: 'create', label: '创建', isExplicit: true, isRestricted: false },
        ],
      },
    ] as any

    const { can } = usePermission()
    expect(can('major:create').value).toBe(true)
  })

  it('SCHOOL_ADMIN without permission: can() returns false', () => {
    const authStore = useAuthStore()
    authStore.userInfo = {
      accountId: '2',
      username: 'school',
      role: 'SCHOOL_ADMIN',
      schoolId: 'school-1',
      realName: '校管理',
      requirePasswordChange: false,
      accessToken: 'token',
    }

    const permStore = usePermissionStore()
    permStore.loaded = true
    permStore.modules = [
      {
        module: 'major',
        moduleLabel: '专业管理',
        permissions: [
          { id: '1', module: 'major', action: 'create', label: '创建', isExplicit: true, isRestricted: false },
        ],
      },
    ] as any

    const { can } = usePermission()
    expect(can('student:delete').value).toBe(false)
  })

  it('isOpAdmin/isSchoolAdmin/isSchoolStaff computed refs', () => {
    const authStore = useAuthStore()
    const { isOpAdmin, isSchoolAdmin, isSchoolStaff } = usePermission()

    // No role
    expect(isOpAdmin.value).toBe(false)
    expect(isSchoolAdmin.value).toBe(false)
    expect(isSchoolStaff.value).toBe(false)

    // OP_ADMIN
    authStore.userInfo = {
      accountId: '1', username: 'admin', role: 'OP_ADMIN',
      schoolId: null, realName: '管理员', requirePasswordChange: false, accessToken: 'token',
    }
    expect(isOpAdmin.value).toBe(true)
    expect(isSchoolAdmin.value).toBe(false)

    // SCHOOL_STAFF
    authStore.userInfo = {
      accountId: '3', username: 'staff', role: 'SCHOOL_STAFF',
      schoolId: 'school-1', realName: '教师', requirePasswordChange: false, accessToken: 'token',
    }
    expect(isSchoolStaff.value).toBe(true)
    expect(isOpAdmin.value).toBe(false)
  })
})
