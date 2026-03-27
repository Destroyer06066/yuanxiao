import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { usePermissionStore } from '@/stores/permission'

export function usePermission() {
  const auth = useAuthStore()
  const permStore = usePermissionStore()

  function can(permission: string): boolean {
    if (!auth.role) return false
    if (auth.role === 'OP_ADMIN') return true

    const modules = permStore.modules
    if (!modules.length) return false

    for (const mod of modules) {
      for (const p of mod.permissions) {
        if (`${p.module}:${p.action}` === permission && p.isExplicit) {
          return true
        }
      }
    }
    return false
  }

  function canAny(...permissions: string[]): boolean {
    return permissions.some(p => can(p))
  }

  const isOpAdmin = computed(() => auth.role === 'OP_ADMIN')
  const isSchoolAdmin = computed(() => auth.role === 'SCHOOL_ADMIN')
  const isSchoolStaff = computed(() => auth.role === 'SCHOOL_STAFF')

  return { can, canAny, isOpAdmin, isSchoolAdmin, isSchoolStaff }
}
