import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { usePermissionStore } from '@/stores/permission'

export function usePermission() {
  const auth = useAuthStore()
  const permStore = usePermissionStore()

  /**
   * can('major:create') — 返回 ComputedRef<boolean>
   * 直接依赖 permStore.modules.length，确保模块数据加载后自动重新计算。
   * 模板中使用 v-if="can('major:create')" 时 Vue 自动解包为 boolean。
   */
  function can(permission: string) {
    return computed(() => {
      if (!auth.role) return false
      if (auth.role === 'OP_ADMIN') return true

      // 触发懒加载权限模块（内部有幂等检查，不会重复请求）
      if (!permStore.loaded) {
        permStore.fetchModules()
      }

      // 直接引用 permStore.modules 以建立响应式依赖
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
    })
  }

  function canAny(...permissions: string[]) {
    return computed(() => permissions.some(p => can(p).value))
  }

  const isOpAdmin = computed(() => auth.role === 'OP_ADMIN')
  const isSchoolAdmin = computed(() => auth.role === 'SCHOOL_ADMIN')
  const isSchoolStaff = computed(() => auth.role === 'SCHOOL_STAFF')

  return { can, canAny, isOpAdmin, isSchoolAdmin, isSchoolStaff }
}
