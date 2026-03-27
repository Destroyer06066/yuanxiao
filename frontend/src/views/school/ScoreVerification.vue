<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">成绩核验</h2>
      <div class="header-actions">
        <el-button v-if="can('verification:create')" type="success" @click="openBatchDialog">
          <el-icon><FolderOpened /></el-icon> 批量核验
        </el-button>
      </div>
    </div>

    <!-- 左右分栏布局 -->
    <div class="split-layout">
      <!-- 左侧：考生列表 -->
      <div class="split-left">
        <el-card shadow="never" class="left-card">
          <template #header>
            <div class="left-header">
              <span>待核验考生</span>
              <span class="badge">{{ filteredCandidates.length }}</span>
            </div>
          </template>
          <!-- 筛选 -->
          <div class="filter-row">
            <el-input
              v-model="candidateQuery.keyword"
              placeholder="姓名/身份证"
              style="width: 100%"
              clearable
              @clear="loadCandidates"
              @keyup.enter="loadCandidates"
            >
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <el-select v-model="candidateQuery.status" placeholder="状态" style="width: 100%; margin-top: 8px" clearable @change="loadCandidates">
              <el-option label="待核验" value="PENDING" />
              <el-option label="已通过" value="VERIFIED" />
              <el-option label="未通过" value="FAILED" />
            </el-select>
          </div>

          <!-- 考生列表 -->
          <el-table
            :data="filteredCandidates"
            v-loading="candidateLoading"
            size="small"
            style="margin-top: 8px"
            max-height="400"
            @row-click="selectCandidate"
            :row-class-name="selectedRowClass"
          >
            <el-table-column prop="candidateName" label="姓名" width="80" />
            <el-table-column prop="idCard" label="身份证" min-width="150" />
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="candidateStatusTagType(row.status)" size="small">
                  {{ candidateStatusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-if="can('verification:create') && row.status === 'PENDING'"
                  type="primary" link
                  size="small"
                  @click.stop="openVerifyDialog(row)"
                >
                  提交核验
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </div>

      <!-- 右侧：核验记录 -->
      <div class="split-right">
        <el-card shadow="never">
          <template #header>
            <div class="right-header">
              <span>核验记录</span>
              <el-select v-model="logQuery.result" placeholder="核验结果" style="width: 140px" clearable @change="fetchLogs">
                <el-option label="全部" value="" />
                <el-option label="通过" value="PASSED" />
                <el-option label="未通过" value="FAILED" />
              </el-select>
            </div>
          </template>

          <el-table :data="logs" v-loading="logLoading" size="small" max-height="500">
            <el-table-column prop="candidateName" label="考生姓名" width="100" />
            <el-table-column prop="majorName" label="专业" min-width="120" />
            <el-table-column prop="certificateNo" label="证书号" min-width="140" />
            <el-table-column prop="result" label="核验结果" width="90">
              <template #default="{ row }">
                <el-tag :type="row.result === 'PASSED' ? 'success' : 'danger'" size="small">
                  {{ row.result === 'PASSED' ? '通过' : '未通过' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="operatorName" label="操作人" width="100" />
            <el-table-column prop="createdAt" label="核验时间" width="170" />
          </el-table>

          <!-- 分页 -->
          <el-pagination
            v-model:current-page="logQuery.page"
            v-model:page-size="logQuery.pageSize"
            :total="logTotal"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            style="margin-top: 12px; justify-content: flex-end"
            @size-change="fetchLogs"
            @current-change="fetchLogs"
          />
        </el-card>
      </div>
    </div>

    <!-- 核验提交弹窗 -->
    <el-dialog
      v-model="verifyDialogVisible"
      title="提交核验"
      width="500px"
      :close-on-click-modal="false"
      @closed="resetVerifyForm"
    >
      <el-form ref="verifyFormRef" :model="verifyForm" :rules="verifyRules" label-width="100px">
        <el-form-item label="考生">
          <span>{{ verifyForm.candidateName }}</span>
        </el-form-item>
        <el-form-item label="证书号" prop="certificateNo">
          <el-input v-model="verifyForm.certificateNo" placeholder="请输入证书编号" maxlength="50" />
        </el-form-item>
        <el-form-item label="核验结果" prop="result">
          <el-radio-group v-model="verifyForm.result">
            <el-radio value="PASSED">通过</el-radio>
            <el-radio value="FAILED">未通过</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="note">
          <el-input v-model="verifyForm.note" type="textarea" :rows="2" placeholder="选填" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="verifyDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitVerify">确定</el-button>
      </template>
    </el-dialog>

    <!-- 批量核验弹窗 -->
    <el-dialog
      v-model="batchDialogVisible"
      title="批量核验"
      width="640px"
      :close-on-click-modal="false"
      @closed="resetBatchForm"
    >
      <el-alert
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 16px"
      >
        选择需要核验的考生，批量提交核验结果。
      </el-alert>
      <el-form ref="batchFormRef" :model="batchForm" label-width="80px">
        <el-form-item label="核验结果" prop="result">
          <el-radio-group v-model="batchForm.result">
            <el-radio value="PASSED">全部通过</el-radio>
            <el-radio value="FAILED">全部未通过</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <el-table :data="pendingCandidates" v-loading="candidateLoading" size="small" max-height="300">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="candidateName" label="考生姓名" width="120" />
        <el-table-column prop="idCard" label="身份证" min-width="150" />
        <el-table-column label="证书号" width="160">
          <template #default="{ row }">
            <el-input v-model="row.certificateNo" size="small" placeholder="证书号" />
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="batchDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitBatch">确定提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Plus, Search, FolderOpened } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import axios from '@/api/axios'
import { usePermission } from '@/composables/usePermission'

const { can } = usePermission()

// ========== 考生列表 ==========
const candidateLoading = ref(false)
const candidates = ref<any[]>([])
const selectedPushId = ref('')

const candidateQuery = reactive({
  keyword: '',
  status: '',
})

const filteredCandidates = computed(() => {
  return candidates.value
})

async function loadCandidates() {
  candidateLoading.value = true
  try {
    const params: any = { page: 1, pageSize: 200 }
    if (candidateQuery.keyword) params.keyword = candidateQuery.keyword
    if (candidateQuery.status) params.status = candidateQuery.status
    const res = await axios.get('/v1/students', { params })
    candidates.value = res.data.data.records || []
  } catch {
    // error handled by axios interceptor
  } finally {
    candidateLoading.value = false
  }
}

function selectCandidate(row: any) {
  selectedPushId.value = row.pushId
}

function selectedRowClass({ row }: any) {
  return row.pushId === selectedPushId.value ? 'selected-row' : ''
}

function candidateStatusTagType(status: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' {
  const map: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
    PENDING: 'info',
    VERIFIED: 'success',
    FAILED: 'danger',
  }
  return map[status] || 'info'
}

function candidateStatusLabel(status: string) {
  const map: Record<string, string> = {
    PENDING: '待核验',
    VERIFIED: '已通过',
    FAILED: '未通过',
  }
  return map[status] || status
}

// ========== 核验记录 ==========
const logLoading = ref(false)
const logs = ref<any[]>([])
const logTotal = ref(0)

const logQuery = reactive({
  result: '',
  page: 1,
  pageSize: 20,
})

async function fetchLogs() {
  logLoading.value = true
  try {
    const params: any = { page: logQuery.page, pageSize: logQuery.pageSize }
    if (logQuery.result) params.result = logQuery.result
    const res = await axios.get('/v1/verifications', { params })
    logs.value = res.data.data.records || []
    logTotal.value = res.data.data.total || 0
  } catch {
    // error handled by axios interceptor
  } finally {
    logLoading.value = false
  }
}

// ========== 核验弹窗 ==========
const verifyDialogVisible = ref(false)
const submitting = ref(false)
const verifyFormRef = ref<FormInstance>()

const verifyForm = reactive({
  pushId: '',
  candidateName: '',
  certificateNo: '',
  result: 'PASSED',
  note: '',
})

const verifyRules: FormRules = {
  certificateNo: [
    { required: true, message: '请输入证书编号', trigger: 'blur' },
    { max: 50, message: '证书编号最多50个字符', trigger: 'blur' },
  ],
  result: [
    { required: true, message: '请选择核验结果', trigger: 'change' },
  ],
}

function openVerifyDialog(row: any) {
  verifyForm.pushId = row.pushId
  verifyForm.candidateName = row.candidateName
  verifyForm.certificateNo = ''
  verifyForm.result = 'PASSED'
  verifyForm.note = ''
  verifyDialogVisible.value = true
}

function resetVerifyForm() {
  verifyFormRef.value?.resetFields()
}

async function submitVerify() {
  const valid = await verifyFormRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await axios.post('/v1/verifications', {
      pushId: verifyForm.pushId,
      certificateNo: verifyForm.certificateNo,
      result: verifyForm.result,
      note: verifyForm.note || undefined,
    })
    ElMessage.success('核验提交成功')
    verifyDialogVisible.value = false
    loadCandidates()
    fetchLogs()
  } catch {
    // error handled by axios interceptor
  } finally {
    submitting.value = false
  }
}

// ========== 批量核验 ==========
const batchDialogVisible = ref(false)
const pendingCandidates = ref<any[]>([])

const batchForm = reactive({
  result: 'PASSED',
})

function openBatchDialog() {
  pendingCandidates.value = candidates.value
    .filter(c => c.status === 'PENDING')
    .map(c => ({ ...c, certificateNo: '' }))
  batchDialogVisible.value = true
}

function resetBatchForm() {
  batchForm.result = 'PASSED'
  pendingCandidates.value = []
}

async function submitBatch() {
  const items = pendingCandidates.value
    .filter(c => c.certificateNo?.trim())
    .map(c => ({
      pushId: c.pushId,
      certificateNo: c.certificateNo.trim(),
      result: batchForm.result,
    }))

  if (items.length === 0) {
    ElMessage.warning('请至少填写一条核验记录')
    return
  }

  submitting.value = true
  try {
    await axios.post('/v1/verifications/batch', { items })
    ElMessage.success(`批量核验成功，共提交 ${items.length} 条`)
    batchDialogVisible.value = false
    loadCandidates()
    fetchLogs()
  } catch {
    // error handled by axios interceptor
  } finally {
    submitting.value = false
  }
}

// ========== 初始化 ==========
onMounted(() => {
  loadCandidates()
  fetchLogs()
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
  gap: 8px;
}
.split-layout {
  display: grid;
  grid-template-columns: 40% 58%;
  gap: 16px;
  align-items: start;
}
.split-left,
.split-right {
  min-width: 0;
}
.left-card {
  height: 100%;
}
.left-header,
.right-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.badge {
  background: #409eff;
  color: #fff;
  border-radius: 10px;
  padding: 0 8px;
  font-size: 12px;
  line-height: 20px;
}
.filter-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.right-header {
  font-weight: 600;
}
</style>

<style>
.selected-row {
  background-color: #ecf5ff !important;
  cursor: pointer;
}
</style>
