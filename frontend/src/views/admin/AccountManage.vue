<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">账号管理</h2>
      <div class="header-actions">
        <el-button v-if="can('account:create')" type="primary" @click="openCreate">
          <el-icon><Plus /></el-icon> 新增账号
        </el-button>
        <el-button v-if="can('account:create')" @click="openImportDialog">
          <el-icon><Upload /></el-icon> 批量导入
        </el-button>
        <el-button @click="downloadTemplate">
          <el-icon><Download /></el-icon> 下载模板
        </el-button>
      </div>
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
            <el-tooltip :content="rolePermissionTip(row.role)" placement="top" :enterable="false">
              <el-tag :type="roleTagType(row.role)" size="small">{{ roleLabel(row.role) }}</el-tag>
            </el-tooltip>
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
              v-if="can('account:disable') && row.accountId !== auth.accountId"
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

    <!-- 批量导入弹窗 -->
    <el-dialog
      v-model="importVisible"
      title="批量导入账号"
      width="680px"
      :close-on-click-modal="false"
      @closed="resetImport"
    >
      <!-- 上传区域 -->
      <div v-if="!showResults" class="import-content">
        <div
          class="upload-zone"
          :class="{ 'drag-over': dragOver }"
          @dragover="handleDragOver"
          @dragleave="handleDragLeave"
          @drop="handleDrop"
        >
          <el-icon class="upload-icon"><Upload /></el-icon>
          <p class="upload-text">拖拽 Excel 文件到此处，或 <label class="upload-link">点击上传</label></p>
          <p class="upload-hint">支持 .xlsx 和 .xls 格式</p>
          <input type="file" class="file-input" accept=".xlsx,.xls" @change="handleFileChange" />
        </div>

        <!-- 预览表格 -->
        <div v-if="previewData.length > 0" class="preview-section">
          <div class="preview-header">
            <span>预览数据（{{ previewData.length }} 条）</span>
            <el-button type="danger" size="small" @click="previewData = []">清空</el-button>
          </div>
          <el-table :data="previewData" stripe max-height="250" size="small">
            <el-table-column prop="username" label="用户名" width="120" show-overflow-tooltip />
            <el-table-column prop="realName" label="姓名" width="100" show-overflow-tooltip />
            <el-table-column prop="role" label="角色" width="130" show-overflow-tooltip />
            <el-table-column prop="schoolName" label="所属院校" min-width="120" show-overflow-tooltip>
              <template #default="{ row }">{{ row.schoolName || '—' }}</template>
            </el-table-column>
            <el-table-column prop="password" label="初始密码" width="110" show-overflow-tooltip>
              <template #default="{ row }">{{ row.password || '默认' }}</template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <!-- 导入结果 -->
      <div v-else class="import-results">
        <div class="results-summary">
          <el-tag type="success" size="large">成功：{{ importResults.filter(r => r.status === 'success').length }}</el-tag>
          <el-tag type="danger" size="large">失败：{{ importResults.filter(r => r.status === 'failed').length }}</el-tag>
        </div>
        <el-table :data="importResults" stripe max-height="300" size="small">
          <el-table-column prop="username" label="用户名" width="120" show-overflow-tooltip />
          <el-table-column prop="realName" label="姓名" width="100" show-overflow-tooltip />
          <el-table-column prop="status" label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.status === 'success' ? 'success' : 'danger'" size="small">
                {{ row.status === 'success' ? '成功' : '失败' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="password" label="密码" width="110">
            <template #default="{ row }">
              <span v-if="row.status === 'success'">{{ row.password || '默认' }}</span>
              <span v-else class="error-text">{{ row.error }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <template #footer>
        <el-button @click="importVisible = false">关闭</el-button>
        <el-button v-if="!showResults" type="primary" :loading="importLoading" :disabled="previewData.length === 0" @click="confirmImport">
          确认导入
        </el-button>
        <el-button v-else @click="showResults = false; previewData = []">继续导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Plus, Search, Upload, Download } from '@element-plus/icons-vue'
import * as XLSX from 'xlsx'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  getAccounts,
  createAccount,
  updateAccount,
  resetAccountPassword,
  toggleAccountStatus,
  batchImportAccounts,
  type Account,
  type CreateAccountRequest,
  type BatchImportItem,
  type BatchImportResult,
} from '@/api/account'
import { getSchools, type School } from '@/api/school'
import { usePermission } from '@/composables/usePermission'
import { useAuthStore } from '@/stores/auth'

const { can, isOpAdmin } = usePermission()
const auth = useAuthStore()

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

function rolePermissionTip(role: string): string {
  const map: Record<string, string> = {
    OP_ADMIN: '运营管理员：可查看全局数据、统计数据，设置系统参数',
    SCHOOL_ADMIN: '院校管理员：可管理本校考生录取、报到、专业配置和名额管理',
    SCHOOL_STAFF: '院校工作人员：可查看和辅助操作本校数据',
  }
  return map[role] || role
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

// ========== 批量导入 ==========
const importVisible = ref(false)
const importLoading = ref(false)
const previewData = ref<BatchImportItem[]>([])
const importResults = ref<BatchImportResult[]>([])
const showResults = ref(false)
const dragOver = ref(false)

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
  const auth = useAuthStore()
  // 非 OP_ADMIN 无权访问 /api/v1/admin/schools，直接用本校
  if (!auth.isOpAdmin) {
    if (auth.schoolId) {
      schoolList.value = [{ schoolId: auth.schoolId, schoolName: '本校' }]
    }
    return
  }
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
  resetCreateForm()
  const auth = useAuthStore()
  if (!auth.isOpAdmin && auth.schoolId) {
    createForm.schoolId = auth.schoolId
  }
  fetchSchools()
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
    ElMessage.success('密码已重置为默认密码：Aa123456!')
  } catch (e: any) {
    if (e !== 'cancel') {
      // error handled by axios interceptor
    }
  }
}

async function handleToggle(row: Account) {
  if (row.accountId === auth.accountId) {
    ElMessage.error('不能禁用自己的账号')
    return
  }
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

// ========== 批量导入 ==========
function openImportDialog() {
  previewData.value = []
  importResults.value = []
  showResults.value = false
  importVisible.value = true
}

function downloadTemplate() {
  window.open('/account-import-template.xlsx', '_blank')
}

function handleDragOver(e: DragEvent) {
  e.preventDefault()
  dragOver.value = true
}

function handleDragLeave() {
  dragOver.value = false
}

function handleDrop(e: DragEvent) {
  e.preventDefault()
  dragOver.value = false
  const files = e.dataTransfer?.files
  if (files && files.length > 0) {
    handleFileUpload(files[0])
  }
}

function handleFileChange(e: Event) {
  const target = e.target as HTMLInputElement
  if (target.files && target.files.length > 0) {
    handleFileUpload(target.files[0])
    target.value = '' // 清空input以便重复选择同一文件
  }
}

function handleFileUpload(file: File) {
  if (!file.name.endsWith('.xlsx') && !file.name.endsWith('.xls')) {
    ElMessage.error('请上传 Excel 文件（.xlsx 或 .xls）')
    return
  }

  const reader = new FileReader()
  reader.onload = (e) => {
    try {
      const data = new Uint8Array(e.target?.result as ArrayBuffer)
      const workbook = XLSX.read(data, { type: 'array' })
      const firstSheet = workbook.Sheets[workbook.SheetNames[0]]
      const jsonData = XLSX.utils.sheet_to_json<any>(firstSheet, { header: 1 }) as any[][]

      // 解析数据（跳过前8行标题和说明，从第9行开始是表头，第10行开始是数据）
      if (jsonData.length < 10) {
        ElMessage.error('Excel文件格式不正确，请使用提供的模板')
        return
      }

      // 检查表头
      const headerRow = jsonData[8]
      if (headerRow[0] !== '用户名*' || headerRow[1] !== '姓名*' || headerRow[2] !== '角色*') {
        ElMessage.error('Excel文件表头格式不正确，请使用提供的模板')
        return
      }

      // 解析数据行
      const parsedData: BatchImportItem[] = []
      for (let i = 9; i < jsonData.length; i++) {
        const row = jsonData[i]
        if (!row || row.length === 0 || !row[0]) continue // 跳过空行

        parsedData.push({
          username: String(row[0] || '').trim(),
          realName: String(row[1] || '').trim(),
          role: String(row[2] || '').trim(),
          schoolName: row[3] ? String(row[3]).trim() : undefined,
          password: row[4] ? String(row[4]).trim() : undefined,
        })
      }

      if (parsedData.length === 0) {
        ElMessage.error('没有找到有效的数据行')
        return
      }

      previewData.value = parsedData
      ElMessage.success(`已解析 ${parsedData.length} 条数据，请确认后点击"确认导入"`)
    } catch (err) {
      console.error(err)
      ElMessage.error('解析Excel文件失败')
    }
  }
  reader.readAsArrayBuffer(file)
}

async function confirmImport() {
  if (previewData.value.length === 0) {
    ElMessage.warning('没有可导入的数据')
    return
  }

  importLoading.value = true
  try {
    const results = await batchImportAccounts(previewData.value)
    importResults.value = results.data.data || []
    showResults.value = true

    const successCount = importResults.value.filter(r => r.status === 'success').length
    const failCount = importResults.value.filter(r => r.status === 'failed').length
    ElMessage.success(`导入完成：成功 ${successCount} 条，失败 ${failCount} 条`)

    if (successCount > 0) {
      fetchAccounts()
    }
  } catch {
    // error handled by axios interceptor
  } finally {
    importLoading.value = false
  }
}

function resetImport() {
  previewData.value = []
  importResults.value = []
  showResults.value = false
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
.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.filter-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.import-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.upload-zone {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  border: 2px dashed #dcdfe6;
  border-radius: 8px;
  background: #fafafa;
  transition: all 0.3s;
  cursor: pointer;
}
.upload-zone:hover,
.upload-zone.drag-over {
  border-color: #409eff;
  background: #ecf5ff;
}
.upload-icon {
  font-size: 48px;
  color: #909399;
  margin-bottom: 12px;
}
.upload-text {
  margin: 0;
  color: #606266;
  font-size: 14px;
}
.upload-hint {
  margin: 4px 0 0;
  color: #909399;
  font-size: 12px;
}
.upload-link {
  color: #409eff;
  cursor: pointer;
}
.file-input {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  cursor: pointer;
}
.preview-section {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 12px;
}
.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  font-size: 14px;
  color: #606266;
}
.import-results {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.results-summary {
  display: flex;
  gap: 16px;
  justify-content: center;
}
.error-text {
  color: #f56c6c;
  font-size: 12px;
}
</style>
