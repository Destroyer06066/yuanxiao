<template>
  <div class="role-manage">
    <!-- 左侧：角色列表 -->
    <div class="role-list-panel">
      <div class="panel-header">
        <span class="panel-title">角色列表</span>
        <el-button type="primary" size="small" @click="openCreateDialog">
          <el-icon><Plus /></el-icon>
          新增角色
        </el-button>
      </div>

      <!-- 预设角色 -->
      <div class="role-section-label">预设角色</div>
      <div
        v-for="role in presetRoles"
        :key="role.id"
        class="role-item preset"
        :class="{ active: selectedRole?.id === role.id }"
        @click="selectRole(role)"
      >
        <el-icon class="lock-icon"><Lock /></el-icon>
        <div class="role-info">
          <span class="role-name">{{ role.name }}</span>
          <el-tag size="small" :type="getPresetTagType(role.roleKey)">{{ role.roleKey }}</el-tag>
        </div>
        <span class="perm-count">{{ role.permissions.filter((p: any) => p.isExplicit).length }} 项权限</span>
      </div>

      <!-- 自定义角色 -->
      <div class="role-section-label">自定义角色</div>
      <el-empty v-if="customRoles.length === 0" description="暂无自定义角色" :image-size="60" />
      <div
        v-for="role in customRoles"
        :key="role.id"
        class="role-item custom"
        :class="{ active: selectedRole?.id === role.id }"
        @click="selectRole(role)"
      >
        <div class="role-info">
          <span class="role-name">{{ role.name }}</span>
        </div>
        <div class="role-actions" @click.stop>
          <el-button type="primary" link size="small" @click="openEditDialog(role)">编辑</el-button>
          <el-popconfirm title="确定删除该角色？" @confirm="handleDelete(role.id)">
            <template #reference>
              <el-button type="danger" link size="small">删除</el-button>
            </template>
          </el-popconfirm>
        </div>
      </div>
    </div>

    <!-- 右侧：角色详情 + 权限矩阵 -->
    <div class="role-detail-panel">
      <template v-if="selectedRole">
        <div class="detail-header">
          <div>
            <h3 class="detail-title">{{ selectedRole.name }}</h3>
            <span class="detail-key">{{ selectedRole.roleKey }}</span>
          </div>
          <el-tag v-if="selectedRole.isPreset" type="info">预设角色</el-tag>
          <el-tag v-else type="success">自定义角色</el-tag>
        </div>

        <div class="detail-desc">{{ selectedRole.description || '暂无描述' }}</div>

        <!-- 权限矩阵 -->
        <div class="permission-matrix">
          <div class="matrix-header">
            <span>权限分配</span>
            <template v-if="!selectedRole.isPreset">
              <el-button v-if="!isEditing" type="primary" size="small" @click="startEdit">
                编辑权限
              </el-button>
              <template v-else>
                <el-button size="small" @click="cancelEdit">取消</el-button>
                <el-button type="primary" size="small" @click="savePermissions" :loading="saving">
                  保存
                </el-button>
              </template>
            </template>
          </div>

          <el-collapse v-model="activeModules">
            <el-collapse-item
              v-for="mod in permissionModules"
              :key="mod.module"
              :title="mod.moduleLabel"
              :name="mod.module"
            >
              <div class="perm-list">
                <div
                  v-for="perm in mod.permissions"
                  :key="perm.id"
                  class="perm-row"
                >
                  <el-checkbox
                    v-model="perm.isExplicit"
                    :disabled="isCheckboxDisabled(perm)"
                  >
                    {{ perm.label }}
                  </el-checkbox>
                  <el-tag v-if="perm.isRestricted" size="small" type="warning">系统保留</el-tag>
                </div>
              </div>
            </el-collapse-item>
          </el-collapse>
        </div>
      </template>

      <el-empty v-else description="请从左侧选择一个角色" />
    </div>

    <!-- 新增/编辑角色弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新增角色' : '编辑角色'"
      width="500px"
    >
      <el-form :model="form" :rules="formRules" ref="formRef" label-width="100px">
        <el-form-item label="角色标识" prop="roleKey" v-if="dialogMode === 'create'">
          <el-input v-model="form.roleKey" placeholder="如 CUSTOM_001" />
        </el-form-item>
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="form.name" placeholder="角色展示名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="角色描述" />
        </el-form-item>
        <el-form-item label="继承模板" prop="presetKey" v-if="dialogMode === 'create'">
          <el-select v-model="form.presetKey" placeholder="选择继承的预设角色">
            <el-option label="运营管理员（全部权限）" value="OP_ADMIN" />
            <el-option label="院校管理员" value="SCHOOL_ADMIN" />
            <el-option label="院校工作人员（只读）" value="SCHOOL_STAFF" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" v-if="dialogMode === 'edit'">
          <el-radio-group v-model="form.status">
            <el-radio value="ACTIVE">启用</el-radio>
            <el-radio value="INACTIVE">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitting">
          {{ dialogMode === 'create' ? '创建' : '保存' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue'
import { Plus, Lock } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  getRoles, createRole, updateRole, deleteRole,
  updateRolePermissions
} from '@/api/role'
import type { Role } from '@/api/role'
import { getPermissionTree } from '@/api/permission'
import type { PermissionModule } from '@/api/permission'
import type { CreateRoleRequest, UpdateRoleRequest } from '@/api/role'

// 角色列表
const roles = ref<Role[]>([])
const selectedRole = ref<Role | null>(null)
const permissionModules = ref<PermissionModule[]>([])

// 编辑状态
const isEditing = ref(false)
const saving = ref(false)
const editingModules = ref<PermissionModule[]>([])

// 弹窗
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitting = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<CreateRoleRequest & UpdateRoleRequest>({
  roleKey: '',
  name: '',
  description: '',
  presetKey: 'SCHOOL_ADMIN',
  status: 'ACTIVE',
})
const formRules: FormRules = {
  roleKey: [{ required: true, message: '请输入角色标识', trigger: 'blur' }],
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  presetKey: [{ required: true, message: '请选择继承模板', trigger: 'change' }],
}

// 折叠
const activeModules = ref<string[]>([])

const presetRoles = computed(() => roles.value.filter(r => r.isPreset))
const customRoles = computed(() => roles.value.filter(r => !r.isPreset))

onMounted(async () => {
  await loadData()
})

async function loadData() {
  const [{ data: roleResult }, { data: permResult }] = await Promise.all([
    getRoles(),
    getPermissionTree(),
  ])
  roles.value = roleResult.data as Role[]
  permissionModules.value = permResult.data as PermissionModule[]

  if (roles.value.length > 0 && !selectedRole.value) {
    selectRole(roles.value[0])
  }
}

function selectRole(role: Role) {
  if (isEditing.value) {
    ElMessageBox.confirm('当前有未保存的修改，是否放弃？', '提示', { type: 'warning' })
      .then(() => { selectedRole.value = role; resetEdit() })
      .catch(() => {})
    return
  }
  selectedRole.value = role
}

function getPresetTagType(key: string): '' | 'primary' | 'success' | 'warning' {
  return key === 'OP_ADMIN' ? 'primary' : key === 'SCHOOL_ADMIN' ? 'success' : 'warning'
}

function isCheckboxDisabled(perm: { isRestricted?: boolean }) {
  if (!selectedRole.value?.isPreset) return false
  return true
}

function openCreateDialog() {
  dialogMode.value = 'create'
  Object.assign(form, { roleKey: '', name: '', description: '', presetKey: 'SCHOOL_ADMIN', status: 'ACTIVE' })
  dialogVisible.value = true
}

function openEditDialog(role: Role) {
  dialogMode.value = 'edit'
  Object.assign(form, { name: role.name, description: role.description || '', status: role.status })
  dialogVisible.value = true
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (dialogMode.value === 'create') {
      const res = await createRole(form)
      const newRole = res.data.data as Role
      roles.value.push(newRole)
      ElMessage.success('角色创建成功')
    } else if (selectedRole.value) {
      const res = await updateRole(selectedRole.value.id, form)
      const updatedRole = res.data.data as Role
      const idx = roles.value.findIndex(r => r.id === updatedRole.id)
      if (idx >= 0) roles.value[idx] = updatedRole
      if (selectedRole.value.id === updatedRole.id) selectedRole.value = updatedRole
      ElMessage.success('角色更新成功')
    }
    dialogVisible.value = false
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

async function handleDelete(id: string) {
  await deleteRole(id)
  roles.value = roles.value.filter(r => r.id !== id)
  if (selectedRole.value?.id === id) selectedRole.value = roles.value[0] || null
  ElMessage.success('删除成功')
}

function startEdit() {
  if (!selectedRole.value) return
  editingModules.value = JSON.parse(JSON.stringify(permissionModules.value))
  const permMap = new Map(
    selectedRole.value.permissions.map((p: any) => [`${p.module}:${p.action}`, p.isExplicit])
  )
  for (const mod of editingModules.value) {
    for (const p of mod.permissions) {
      p.isExplicit = permMap.get(`${p.module}:${p.action}`) ?? false
    }
  }
  isEditing.value = true
}

function cancelEdit() {
  resetEdit()
}

function resetEdit() {
  isEditing.value = false
  editingModules.value = []
}

async function savePermissions() {
  if (!selectedRole.value) return
  saving.value = true
  try {
    const permissionIds: string[] = []
    for (const mod of editingModules.value) {
      for (const p of mod.permissions) {
        if (p.isExplicit) permissionIds.push(p.id)
      }
    }
    const res = await updateRolePermissions(selectedRole.value.id, permissionIds)
    const updatedRole = res.data.data as Role
    const idx = roles.value.findIndex(r => r.id === updatedRole.id)
    if (idx >= 0) roles.value[idx] = updatedRole
    selectedRole.value = updatedRole
    const permSet = new Set(updatedRole.permissions.filter((p: any) => p.isExplicit).map((p: any) => `${p.module}:${p.action}`))
    for (const mod of permissionModules.value) {
      for (const p of mod.permissions) {
        p.isExplicit = permSet.has(`${p.module}:${p.action}`)
      }
    }
    ElMessage.success('权限保存成功')
    resetEdit()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.role-manage {
  display: flex;
  height: calc(100vh - 120px);
  gap: 16px;
  padding: 20px;
  overflow: hidden;
}

.role-list-panel {
  width: 300px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  overflow-y: auto;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.panel-title {
  font-size: 15px;
  font-weight: 600;
}

.role-section-label {
  font-size: 12px;
  color: #909399;
  margin: 8px 0 4px;
}

.role-item {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  margin-bottom: 4px;
  transition: background 0.2s;
}

.role-item:hover {
  background: #f5f7fa;
}

.role-item.active {
  background: #ecf5ff;
}

.role-item.preset {
  opacity: 0.85;
}

.lock-icon {
  color: #909399;
  margin-right: 8px;
  font-size: 14px;
}

.role-info {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.role-name {
  font-size: 14px;
  font-weight: 500;
}

.perm-count {
  font-size: 12px;
  color: #909399;
}

.role-actions {
  display: flex;
  gap: 4px;
}

.role-detail-panel {
  flex: 1;
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  overflow-y: auto;
}

.detail-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 12px;
}

.detail-title {
  margin: 0 0 4px;
  font-size: 18px;
  font-weight: 600;
}

.detail-key {
  font-size: 12px;
  color: #909399;
}

.detail-desc {
  color: #606266;
  font-size: 14px;
  margin-bottom: 20px;
  line-height: 1.6;
}

.permission-matrix {
  border-top: 1px solid #eee;
  padding-top: 16px;
}

.matrix-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  font-weight: 600;
  font-size: 14px;
}

.perm-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.perm-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
