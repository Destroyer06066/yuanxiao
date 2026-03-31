import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getPermissionTree } from '@/api/permission'
import type { PermissionModule } from '@/api/permission'

export const usePermissionStore = defineStore('permission', () => {
  const modules = ref<PermissionModule[]>([])
  const loaded = ref(false)

  async function fetchModules() {
    if (loaded.value) return
    try {
      const { data } = await getPermissionTree()
      modules.value = data
      loaded.value = true
    } catch {
      // SCHOOL_ADMIN/STAFF 无权访问全量权限树，静默忽略
      loaded.value = true
    }
  }

  return { modules, loaded, fetchModules }
})
