import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getPermissionTree } from '@/api/permission'
import type { PermissionModule } from '@/api/permission'

export const usePermissionStore = defineStore('permission', () => {
  const modules = ref<PermissionModule[]>([])
  const loaded = ref(false)

  async function fetchModules() {
    if (loaded.value) return
    const { data } = await getPermissionTree()
    modules.value = data
    loaded.value = true
  }

  return { modules, loaded, fetchModules }
})
