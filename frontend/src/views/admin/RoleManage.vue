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
            <template v-if="!isEditing">
              <el-button type="primary" size="small" @click="startEdit">
                编辑权限
              </el-button>
            </template>
            <template v-else>
              <el-button size="small" @click="cancelEdit">取消</el-button>
              <el-button type="primary" size="small" @click="savePermissions" :loading="saving">
                保存
              </el-button>
            </template>
          </div>

          <!-- 三级权限矩阵：分组 → 菜单项 → 按钮权限 -->
          <div class="perm-matrix-3level">
            <div
              v-for="group in permissionDomains"
              :key="group.label"
              class="perm-group"
            >
              <div class="group-header">
                <el-icon><FolderOpened /></el-icon>
                {{ group.label }}
              </div>
              <div class="group-items">
                <div
                  v-for="menuItem in group.items"
                  :key="menuItem.item"
                  class="menu-item-row"
                >
                  <div class="menu-item-label">
                    <el-icon><Document /></el-icon>
                    {{ menuItem.item }}
                  </div>
                  <div class="menu-item-perms">
                    <div
                      v-for="perm in menuItem.perms"
                      :key="perm.id || perm.action"
                      class="perm-cell"
                    >
                      <el-checkbox
                        :model-value="perm.isExplicit"
                        :disabled="isCheckboxDisabled(perm)"
                        @update:model-value="(val: boolean) => togglePerm(menuItem.module, perm.action, val)"
                      >
                        {{ perm.label }}
                      </el-checkbox>
                      <el-tag v-if="perm.isRestricted" size="small" type="warning">系统保留</el-tag>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
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
import { Plus, Lock, FolderOpened, Document } from '@element-plus/icons-vue'
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

// 第一级 → 第二级 → 第三级映射（和 Layout.vue 侧边栏完全对齐）
// 格式: { group: '侧边栏分组', item: '侧边栏菜单项', module: '数据库模块', actions: '该菜单项对应的所有 action 权限' }
const MENU_ITEMS: Array<{
  group: string
  item: string
  module: string
  actions: string[]
}> = [
  // ========== OP_ADMIN 菜单 ==========
  // 招生管理
  { group: '招生管理', item: '考生列表', module: 'account', actions: ['read'] },
  { group: '招生管理', item: '录取轮次', module: 'supplement', actions: ['read'] },
  { group: '招生管理', item: '补录管理', module: 'supplement', actions: ['read'] },
  { group: '招生管理', item: '成绩核验', module: 'verification', actions: ['read', 'create'] },
  // 数据统计
  { group: '数据统计', item: '数据统计', module: 'report', actions: ['read'] },
  { group: '数据统计', item: '考生统计', module: 'report', actions: ['read'] },
  // 院校管理
  { group: '院校管理', item: '院校列表', module: 'school', actions: ['read', 'create', 'edit', 'disable'] },
  // 站内通知
  { group: '站内通知', item: '站内通知', module: 'notification', actions: ['read'] },
  // 系统
  { group: '系统', item: '账号管理', module: 'account', actions: ['read', 'create', 'edit', 'disable'] },
  { group: '系统', item: '角色管理', module: 'role', actions: ['read', 'create', 'edit', 'disable'] },
  { group: '系统', item: '操作日志', module: 'audit', actions: ['read'] },
  { group: '系统', item: '系统参数', module: 'system', actions: ['read'] },

  // ========== SCHOOL_ADMIN/STAFF 菜单 ==========
  // 招生管理
  { group: '招生管理', item: '报到管理', module: 'checkin', actions: ['read', 'material', 'confirm'] },
  // 配置管理
  { group: '配置管理', item: '专业配置', module: 'major', actions: ['read', 'create', 'edit', 'disable'] },
  { group: '配置管理', item: '名额管理', module: 'quota', actions: ['read', 'create', 'edit'] },
  { group: '配置管理', item: '招生简章', module: 'brochure', actions: ['read', 'create', 'edit'] },
]

// 第二级 → 第三级权限
// 格式: item → { action: string, label: string, restricted?: boolean }
type PermissionItem = {
  action: string
  label: string
  isRestricted?: boolean
}

const ITEM_PERMISSIONS: Record<string, PermissionItem[]> = {
  // 招生管理
  '考生列表': [
    { action: 'read', label: '查看' },
  ],
  '报到管理': [
    { action: 'read', label: '查看报到' },
    { action: 'material', label: '材料收件登记' },
    { action: 'confirm', label: '确认报到' },
  ],
  '录取轮次': [
    { action: 'read', label: '查看' },
  ],
  '补录管理': [
    { action: 'read', label: '查看' },
  ],
  '成绩核验': [
    { action: 'read', label: '查看核验' },
    { action: 'create', label: '提交核验' },
  ],
  // 数据统计
  '数据统计': [
    { action: 'read', label: '查看报表' },
  ],
  '考生统计': [
    { action: 'read', label: '查看' },
  ],
  // 院校管理
  '院校列表': [
    { action: 'read', label: '查看院校' },
    { action: 'create', label: '新增院校' },
    { action: 'edit', label: '编辑院校' },
    { action: 'disable', label: '停用/启用院校' },
  ],
  // 站内通知
  '站内通知': [
    { action: 'read', label: '查看通知' },
  ],
  // 配置管理
  '专业配置': [
    { action: 'read', label: '查看专业' },
    { action: 'create', label: '新增专业' },
    { action: 'edit', label: '编辑专业' },
    { action: 'disable', label: '停用/启用专业' },
  ],
  '名额管理': [
    { action: 'read', label: '查看名额' },
    { action: 'create', label: '新增名额' },
    { action: 'edit', label: '编辑名额' },
  ],
  '招生简章': [
    { action: 'read', label: '查看简章' },
    { action: 'create', label: '新增简章' },
    { action: 'edit', label: '编辑简章' },
  ],
  // 系统
  '账号管理': [
    { action: 'read', label: '查看账号' },
    { action: 'create', label: '新增账号' },
    { action: 'edit', label: '编辑账号' },
    { action: 'disable', label: '禁用/启用账号' },
  ],
  '角色管理': [
    { action: 'read', label: '查看角色' },
    { action: 'create', label: '新增角色' },
    { action: 'edit', label: '编辑角色' },
    { action: 'disable', label: '停用/启用角色' },
  ],
  '操作日志': [
    { action: 'read', label: '查看日志' },
  ],
  '系统参数': [
    { action: 'read', label: '查看参数' },
  ],
}

const RESTRICTED_SET = new Set(['account:create', 'account:edit', 'account:disable'])

// 三级结构
type PermRow = {
  action: string
  label: string
  id: string
  isRestricted: boolean
  isExplicit: boolean
}
type MenuItemRow = {
  item: string
  module: string
  perms: PermRow[]
}
type GroupRow = {
  label: string
  items: MenuItemRow[]
}

// 从 selectedRole.permissions 读取 isExplicit 状态
const rolePermMap = computed(() => {
  const m = new Map<string, any>()
  if (!selectedRole.value) return m
  for (const p of selectedRole.value.permissions) {
    m.set(`${p.module}:${p.action}`, p)
  }
  return m
})

// 从 permissionModules 构建三级结构（以 selectedRole 的权限为准）
const permissionDomains = computed((): GroupRow[] => {
  if (!permissionModules.value.length) return []

  // 建立 module:action → PermissionDTO 的索引（全局权限元数据：id、label 等）
  const permMetaMap = new Map<string, any>()
  for (const mod of permissionModules.value) {
    for (const p of mod.permissions) {
      permMetaMap.set(`${p.module}:${p.action}`, p)
    }
  }

  const groupMap = new Map<string, GroupRow>()
  for (const entry of MENU_ITEMS) {
    const groupKey = entry.group
    const itemKey = entry.item
    const moduleKey = entry.module
    const actions = entry.actions || []

    const perms: PermRow[] = actions.map(action => {
      const key = `${moduleKey}:${action}`
      const meta = permMetaMap.get(key)

      // 编辑模式：读 editingModules
      if (editingModules.value.length > 0) {
        const editing = editingModules.value
          .flatMap((m: any) => m.permissions)
          .find((p: any) => p.module === moduleKey && p.action === action)
        return {
          action,
          label: ITEM_PERMISSIONS[itemKey]?.find(p => p.action === action)?.label || meta?.label || action,
          id: meta?.id || '',
          isRestricted: RESTRICTED_SET.has(key),
          isExplicit: editing ? (editing as any).isExplicit : false,
        }
      }

      // 非编辑模式：读 selectedRole.permissions
      const rolePerm = rolePermMap.value.get(key)
      return {
        action,
        label: ITEM_PERMISSIONS[itemKey]?.find(p => p.action === action)?.label || meta?.label || action,
        id: meta?.id || '',
        isRestricted: RESTRICTED_SET.has(key),
        isExplicit: rolePerm ? !!(rolePerm as any).isExplicit : false,
      }
    })

    if (!groupMap.has(groupKey)) {
      groupMap.set(groupKey, { label: groupKey, items: [] })
    }
    groupMap.get(groupKey)!.items.push({
      item: itemKey,
      module: moduleKey,
      perms,
    })
  }

  return Array.from(groupMap.values())
})

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
  // 系统保留权限不可编辑
  return !!perm.isRestricted
}

function togglePerm(module: string, action: string, value: boolean) {
  const mod = editingModules.value.find(m => m.module === module)
  if (!mod) return
  const perm = mod.permissions.find((p: any) => p.module === module && p.action === action)
  if (perm) (perm as any).isExplicit = value
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
  margin-bottom: 16px;
  font-weight: 600;
  font-size: 14px;
}

/* 三级权限矩阵 */
.perm-matrix-3level {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.perm-group {
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  overflow: hidden;
}

.group-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 14px;
  background: #f5f7fa;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  border-bottom: 1px solid #e8e8e8;
}

.group-header .el-icon {
  color: #409eff;
}

.group-items {
  display: flex;
  flex-direction: column;
}

.menu-item-row {
  display: flex;
  align-items: flex-start;
  padding: 10px 14px;
  border-bottom: 1px solid #f0f0f0;
}

.menu-item-row:last-child {
  border-bottom: none;
}

.menu-item-label {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 130px;
  flex-shrink: 0;
  font-size: 13px;
  font-weight: 500;
  color: #606266;
  padding-top: 2px;
}

.menu-item-label .el-icon {
  color: #909399;
}

.menu-item-perms {
  flex: 1;
  display: flex;
  flex-wrap: wrap;
  gap: 12px 24px;
}

.perm-cell {
  display: flex;
  align-items: center;
  gap: 4px;
}

.perm-cell .el-checkbox {
  font-size: 13px;
}
</style>
