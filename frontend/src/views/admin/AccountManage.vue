<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">账号管理</h2>
      <el-button v-if="can('account:create')" type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon> 新增账号
      </el-button>
    </div>

    <el-card>
      <!-- 筛选条件 -->
      <div class="filter-row">
        <el-input
          v-model="query.keyword"
          placeholder="账号/姓名"
          style="width: 180px"
          clearable
          @clear="doSearch"
          @keyup.enter="doSearch"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="query.role" placeholder="角色" style="width: 160px" clearable @change="doSearch">
          <el-option label="运营管理员" value="OP_ADMIN" />
          <el-option label="院校管理员" value="SCHOOL_ADMIN" />
          <el-option label="院校工作人员" value="SCHOOL_STAFF" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" style="width: 120px" clearable @change="doSearch">
          <el-option label="启用" value="ACTIVE" />
          <el-option label="停用" value="INACTIVE" />
        </el-select>
        <el-button @click="doSearch">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </div>

      <!-- 表格 -->
      <el-table :data="list" v-loading="loading" stripe style="margin-top: 12px">
        <el-table-column prop="username" label="账号" width="150" />
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column prop="role" label="角色" width="140">
          <template #default="{ row }">
            <el-tag :type="roleTagType(row.role)" size="small">{{ roleLabel(row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="schoolName" label="所属院校" min-width="140">
          <template #default="{ row }">
            {{ row.schoolName || '—' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" size="small">
              {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button v-if="can('account:edit')" type="primary" link @click="openEdit(row)">编辑</el-button>
            <el-button type="warning" link @click="handleResetPassword(row)">重置密码</el-button>
            <el-button
              v-if="can('account:disable')"
              :type="row.status === 'ACTIVE' ? 'danger' : 'success'" link
              @click="handleToggle(row)"
            >
              {{ row.status === 'ACTIVE' ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        style="margin-top: 16px; justify-content: flex-end"
        @size-change="fetchAccounts"
        @current-change="fetchAccounts"
      />
    </el-card>

    <!-- 新增弹窗 -->
    <el-dialog
      v-model="createVisible"
      title="新增账号"
      width="520px"
      :close-on-click-modal="false"
      @closed="resetCreateForm"
    >
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="createForm.username" placeholder="请输入用户名" maxlength="50" />
        </el-form-item>
        <el-form-item label="初始密码" prop="password">
          <el-input v-model="createForm.password" type="password" placeholder="请输入初始密码" show-password maxlength="50" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="createForm.realName" placeholder="请输入姓名" maxlength="50" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="createForm.role" placeholder="请选择角色" style="width: 100%">
            <el-option label="院校管理员" value="SCHOOL_ADMIN" />
            <el-option label="院校工作人员" value="SCHOOL_STAFF" />
            <el-option v-if="isOpAdmin" label="运营管理员" value="OP_ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="createForm.role !== 'OP_ADMIN'" label="所属院校" prop="schoolId">
          <el-select v-model="createForm.schoolId" placeholder="请选择院校" style="width: 100%" filterable>
            <el-option v-for="s in schoolList" :key="s.schoolId" :label="s.schoolName" :value="s.schoolId" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitCreate">确定</el-button>
      </template>
    </el-dialog>

    <!-- 编辑弹窗 -->
    <el-dialog
      v-model="editVisible"
      title="编辑账号"
      width="520px"
      :close-on-click-modal="false"
      @closed="resetEditForm"
    >
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="100px">
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="editForm.realName" placeholder="请输入姓名" maxlength="50" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-if="isOpAdmin" v-model="editForm.role" placeholder="请选择角色" style="width: 100%">
            <el-option label="院校管理员" value="SCHOOL_ADMIN" />
            <el-option label="院校工作人员" value="SCHOOL_STAFF" />
            <el-option label="运营管理员" value="OP_ADMIN" />
          </el-select>
          <el-select v-else v-model="editForm.role" placeholder="请选择角色" style="width: 100%" disabled>
            <el-option label="院校管理员" value="SCHOOL_ADMIN" />
            <el-option label="院校工作人员" value="SCHOOL_STAFF" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="editForm.role !== 'OP_ADMIN'" label="所属院校" prop="schoolId">
          <el-select v-model="editForm.schoolId" placeholder="请选择院校" style="width: 100%" filterable>
            <el-option v-for="s in schoolList" :key="s.schoolId" :label="s.schoolName" :value="s.schoolId" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitEdit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Plus, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  getAccounts,
  createAccount,
  updateAccount,
  resetAccountPassword,
  toggleAccountStatus,
  type Account,
  type CreateAccountRequest,
} from '@/api/account'
import { getSchools, type School } from '@/api/school'
import { usePermission } from '@/composables/usePermission'

const { can, isOpAdmin } = usePermission()

// ========== 角色辅助 ==========
function roleLabel(role: string) {
  const map: Record<string, string> = {
    OP_ADMIN: '运营管理员',
    SCHOOL_ADMIN: '院校管理员',
    SCHOOL_STAFF: '院校工作人员',
  }
  return map[role] || role
}
function roleTagType(role: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' {
  const map: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
    OP_ADMIN: 'primary',
    SCHOOL_ADMIN: 'success',
    SCHOOL_STAFF: 'info',
  }
  return map[role] || 'info'
}

// ========== 数据 ==========
const loading = ref(false)
const submitting = ref(false)
const list = ref<Account[]>([])
const schoolList = ref<School[]>([])
const total = ref(0)
const createVisible = ref(false)
const editVisible = ref(false)
const editingId = ref('')
const createFormRef = ref<FormInstance>()
const editFormRef = ref<FormInstance>()

const query = reactive({
  keyword: '',
  role: '',
  status: '',
  page: 1,
  pageSize: 20,
})

const createForm = reactive<CreateAccountRequest>({
  username: '',
  password: '',
  realName: '',
  role: '',
  schoolId: '',
})

const createRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度在 3 到 50 个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入初始密码', trigger: 'blur' },
    { min: 6, max: 50, message: '密码长度至少 6 个字符', trigger: 'blur' },
  ],
  realName: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { max: 50, message: '姓名最多 50 个字符', trigger: 'blur' },
  ],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  schoolId: [{ required: true, message: '请选择院校', trigger: 'change' }],
}

const editRules: FormRules = {
  realName: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { max: 50, message: '姓名最多 50 个字符', trigger: 'blur' },
  ],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  schoolId: [{ required: true, message: '请选择院校', trigger: 'change' }],
}

const editForm = reactive({
  realName: '',
  role: '',
  schoolId: '',
})

// ========== 接口 ==========
async function fetchSchools() {
  try {
    const res = await getSchools({ page: 1, pageSize: 500, status: 'ACTIVE' })
    schoolList.value = res.data.data.records || []
  } catch {
    // error handled by axios interceptor
  }
}

async function fetchAccounts() {
  loading.value = true
  try {
    const params: any = { page: query.page, pageSize: query.pageSize }
    if (query.keyword) params.keyword = query.keyword
    if (query.role) params.role = query.role
    if (query.status) params.status = query.status
    const res = await getAccounts(params)
    list.value = res.data.data.records || []
    total.value = res.data.data.total || 0
  } catch {
    // error handled by axios interceptor
  } finally {
    loading.value = false
  }
}

function doSearch() {
  query.page = 1
  fetchAccounts()
}

function resetQuery() {
  query.keyword = ''
  query.role = ''
  query.status = ''
  query.page = 1
  fetchAccounts()
}

// ========== 弹窗 ==========
function openCreate() {
  createVisible.value = true
}

function resetCreateForm() {
  createFormRef.value?.resetFields()
  createForm.username = ''
  createForm.password = ''
  createForm.realName = ''
  createForm.role = ''
  createForm.schoolId = ''
}

async function submitCreate() {
  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await createAccount({ ...createForm })
    ElMessage.success('账号创建成功')
    createVisible.value = false
    fetchAccounts()
  } catch {
    // error handled by axios interceptor
  } finally {
    submitting.value = false
  }
}

async function openEdit(row: Account) {
  editingId.value = row.accountId
  Object.assign(editForm, {
    realName: row.realName,
    role: row.role,
    schoolId: row.schoolId || '',
  })
  editVisible.value = true
}

function resetEditForm() {
  editFormRef.value?.resetFields()
  editForm.realName = ''
  editForm.role = ''
  editForm.schoolId = ''
}

async function submitEdit() {
  const valid = await editFormRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await updateAccount(editingId.value, {
      realName: editForm.realName,
      role: editForm.role,
      schoolId: editForm.schoolId || undefined,
    })
    ElMessage.success('账号更新成功')
    editVisible.value = false
    fetchAccounts()
  } catch {
    // error handled by axios interceptor
  } finally {
    submitting.value = false
  }
}

async function handleResetPassword(row: Account) {
  try {
    await ElMessageBox.confirm(
      `确定要重置账号「${row.username}」的密码吗？`,
      '重置密码确认',
      { type: 'warning' }
    )
    await resetAccountPassword(row.accountId)
    ElMessage.success('密码已重置为默认密码')
  } catch (e: any) {
    if (e !== 'cancel') {
      // error handled by axios interceptor
    }
  }
}

async function handleToggle(row: Account) {
  const action = row.status === 'ACTIVE' ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(
      `确定要${action}账号「${row.username}」吗？`,
      `${action}确认`,
      { type: 'warning' }
    )
    const newStatus = row.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
    await toggleAccountStatus(row.accountId, newStatus)
    ElMessage.success(`${action}成功`)
    fetchAccounts()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(`${action}失败`)
    }
  }
}

// ========== 初始化 ==========
onMounted(() => {
  fetchSchools()
  fetchAccounts()
})
</script>

<style scoped>
.page-container {
  padding: 20px;
}
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.page-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}
.filter-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
