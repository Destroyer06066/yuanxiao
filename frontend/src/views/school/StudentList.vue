<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">考生列表</h2>
      <div class="header-actions">
        <el-button type="success" :disabled="selectedIds.length === 0" @click="handleBatchReject">
          批量拒绝
        </el-button>
        <el-button type="primary" @click="handleExport">
          <el-icon><Download /></el-icon>
          导出
        </el-button>
      </div>
    </div>

    <!-- 筛选区 -->
    <el-card class="filter-card">
      <el-form :inline="true" :model="query">
        <!-- 院校筛选（OP_ADMIN 可见） -->
        <el-form-item v-if="authStore.isOpAdmin" label="推送院校">
          <el-select v-model="query.schoolId" placeholder="全部院校" clearable style="width: 180px">
            <el-option v-for="s in schoolOptions" :key="s.schoolId" :label="s.schoolName" :value="s.schoolId" />
          </el-select>
        </el-form-item>

        <el-form-item label="状态">
          <el-select v-model="query.status" multiple collapse-tags placeholder="全部状态" clearable>
            <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>

        <el-form-item label="总分区间">
          <el-input-number v-model="query.minScore" :min="0" :max="300" placeholder="最低分" style="width: 110px" />
          <span class="ml-8 mr-8">-</span>
          <el-input-number v-model="query.maxScore" :min="0" :max="300" placeholder="最高分" style="width: 110px" />
        </el-form-item>

        <el-form-item label="意向方向">
          <el-input v-model="query.intentionKeyword" placeholder="关键词搜索" clearable />
        </el-form-item>

        <el-form-item label="推送时间">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>

        <el-form-item label="推送轮次">
          <el-input-number
            v-model="query.round"
            :min="0"
            placeholder="轮次号"
            style="width: 120px"
            clearable
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 快捷状态标签 -->
      <div class="quick-status">
        <span class="quick-label">快捷筛选：</span>
        <el-check-tag
          v-for="s in quickStatusTags"
          :key="s.value"
          :checked="query.status?.length === 1 && query.status[0] === s.value"
          @change="toggleQuickStatus(s.value)"
        >
          {{ s.label }}
        </el-check-tag>
      </div>
    </el-card>

    <!-- 列表 -->
    <el-card class="table-card">
      <el-table
        ref="tableRef"
        :data="tableData"
        v-loading="loading"
        stripe
        @selection-change="handleSelection"
      >
        <el-table-column type="selection" width="55" />

        <!-- 院校列（仅 OP_ADMIN 可见） -->
        <el-table-column v-if="authStore.isOpAdmin" prop="schoolName" label="推送院校" width="160" />

        <el-table-column prop="candidateName" label="姓名" width="120" />
        <el-table-column prop="nationality" label="国籍" width="100" />
        <el-table-column prop="totalScore" label="总分" width="80" sortable>
          <template #default="{ row }">
            <span class="score-highlight">{{ row.totalScore }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="intention" label="意向方向" min-width="150" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="130">
          <template #default="{ row }">
            <el-tag :class="'status-tag ' + row.status" size="small">
              {{ row.statusDesc }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="admissionMajor" label="录取专业" width="130" show-overflow-tooltip />
        <el-table-column v-if="hasConditional" label="条件截止日" width="130">
          <template #default="{ row }">
            <span v-if="row.status === 'CONDITIONAL'" class="deadline-text">
              {{ formatDeadline(row.conditionDeadline) }}
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="pushedAt" label="推送时间" width="160" sortable />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="$router.push(`/students/${row.pushId}`)">
              详情
            </el-button>
            <!-- 操作按钮：非 OP_ADMIN 可见 -->
            <template v-if="!authStore.isOpAdmin">
              <el-button v-if="row.status === 'PENDING'" type="success" link @click="openAdmitDialog(row)">
                录取
              </el-button>
              <el-button v-if="row.status === 'PENDING'" type="warning" link @click="openConditionalDialog(row)">
                有条件录取
              </el-button>
              <el-button v-if="row.status === 'PENDING'" type="danger" link @click="handleReject(row)">
                拒绝
              </el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-area">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          @size-change="search"
          @current-change="search"
        />
      </div>
    </el-card>

    <!-- 录取对话框 -->
    <el-dialog v-model="admitDialogVisible" title="直接录取" width="500px">
      <el-form :model="admitForm" label-width="80px">
        <el-form-item label="录取专业" required>
          <el-select v-model="admitForm.majorId" placeholder="请选择专业" class="full-width">
            <el-option
              v-for="m in majorOptions"
              :key="m.majorId"
              :label="`${m.majorName}（剩余${m.remainQuota}名额）`"
              :value="m.majorId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="admitForm.remark" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="admitDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleAdmit">确认录取</el-button>
      </template>
    </el-dialog>

    <!-- 有条件录取对话框 -->
    <el-dialog v-model="conditionalDialogVisible" title="有条件录取" width="520px">
      <el-form :model="condForm" label-width="90px">
        <el-form-item label="意向专业" required>
          <el-select v-model="condForm.majorId" placeholder="请选择专业" class="full-width">
            <el-option v-for="m in majorOptions" :key="m.majorId" :label="m.majorName" :value="m.majorId" />
          </el-select>
        </el-form-item>
        <el-form-item label="录取条件" required>
          <el-input v-model="condForm.conditionDesc" type="textarea" :rows="3" maxlength="1000" show-word-limit
            placeholder="请描述考生需满足的条件，如：补充语言证书、通过面试等" />
        </el-form-item>
        <el-form-item label="条件截止日期" required>
          <el-date-picker
            v-model="condForm.conditionDeadline"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            :disabled-date="(d: Date) => d <= new Date()"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="conditionalDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleConditional">确认</el-button>
      </template>
    </el-dialog>

    <!-- 拒绝对话框 -->
    <el-dialog v-model="rejectDialogVisible" title="拒绝录取" width="420px">
      <el-form :model="rejectForm" label-width="70px">
        <el-form-item label="拒绝原因">
          <el-input v-model="rejectForm.reason" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="submitting" @click="handleConfirmReject">确认拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { queryStudents, type StudentQuery, type Student, directAdmission, conditionalAdmission, batchReject } from '@/api/student'
import { getSchools } from '@/api/school'

const route = useRoute()
const authStore = useAuthStore()
const loading = ref(false)
const tableData = ref<Student[]>([])
const total = ref(0)
const selectedIds = ref<string[]>([])
const tableRef = ref()

const dateRange = ref<string[]>([])

// 院校选项（OP_ADMIN 用）
const schoolOptions = ref<any[]>([])

const query = reactive<StudentQuery & { schoolId?: string; round?: number }>({
  status: [],
  page: 1,
  pageSize: 20,
  sort: 'pushedAt',
  order: 'DESC',
  round: undefined,
})

const statusOptions = [
  { value: 'PENDING', label: '待处理' },
  { value: 'CONDITIONAL', label: '有条件录取中' },
  { value: 'ADMITTED', label: '已录取（待确认）' },
  { value: 'CONFIRMED', label: '已确认' },
  { value: 'MATERIAL_RECEIVED', label: '材料已收' },
  { value: 'CHECKED_IN', label: '已报到' },
  { value: 'REJECTED', label: '已拒绝' },
  { value: 'INVALIDATED', label: '录取已失效' },
]

// 快捷状态标签
const quickStatusTags = [
  { value: 'PENDING', label: '待处理' },
  { value: 'CONDITIONAL', label: '有条件录取' },
  { value: 'ADMITTED', label: '已录取' },
  { value: 'CONFIRMED', label: '已确认' },
]

const hasConditional = computed(() =>
  tableData.value.some(row => row.status === 'CONDITIONAL')
)

function toggleQuickStatus(value: string) {
  if (query.status?.length === 1 && query.status[0] === value) {
    query.status = []
  } else {
    query.status = [value]
  }
  // 筛选 CONDITIONAL 时，按截止日期升序（最紧急排前）
  if (value === 'CONDITIONAL') {
    query.sort = 'conditionDeadline'
    query.order = 'ASC'
  } else {
    query.sort = 'pushedAt'
    query.order = 'DESC'
  }
  search()
}

function formatDeadline(iso: string) {
  if (!iso) return '-'
  return new Date(iso).toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })
}

const majorOptions = ref<any[]>([])

// 录取对话框
const admitDialogVisible = ref(false)
const admitForm = reactive({ pushId: '', majorId: '', remark: '' })
const submitting = ref(false)

// 有条件录取
const conditionalDialogVisible = ref(false)
const condForm = reactive({ pushId: '', majorId: '', conditionDesc: '', conditionDeadline: '' })

// 拒绝
const rejectDialogVisible = ref(false)
const rejectForm = reactive({ pushId: '', reason: '' })

async function loadSchools() {
  if (!authStore.isOpAdmin) return
  try {
    const res = await getSchools({ status: 'ACTIVE', pageSize: 500 })
    schoolOptions.value = res.data.data.records
  } catch { /* ignore */ }
}

async function search() {
  loading.value = true
  try {
    const params: any = { ...query }
    if (dateRange.value?.length === 2) {
      params.pushTimeStart = dateRange.value[0]
      params.pushTimeEnd = dateRange.value[1]
    }
    const res = await queryStudents(params)
    tableData.value = res.data.data.records
    total.value = res.data.data.total
  } finally {
    loading.value = false
  }
}

function reset() {
  query.status = []
  query.minScore = undefined
  query.maxScore = undefined
  query.intentionKeyword = ''
  query.schoolId = undefined
  query.round = undefined
  dateRange.value = []
  search()
}

function handleSelection(selection: Student[]) {
  selectedIds.value = selection.map((s: Student) => s.pushId)
}

function openAdmitDialog(row: Student) {
  admitForm.pushId = row.pushId
  admitForm.majorId = ''
  admitForm.remark = ''
  admitDialogVisible.value = true
}

function openConditionalDialog(row: Student) {
  condForm.pushId = row.pushId
  condForm.majorId = ''
  condForm.conditionDesc = ''
  condForm.conditionDeadline = ''
  conditionalDialogVisible.value = true
}

function handleReject(row: Student) {
  rejectForm.pushId = row.pushId
  rejectForm.reason = ''
  rejectDialogVisible.value = true
}

async function handleAdmit() {
  if (!admitForm.majorId) {
    ElMessage.warning('请选择录取专业')
    return
  }
  submitting.value = true
  try {
    await directAdmission({ pushId: admitForm.pushId, majorId: admitForm.majorId, remark: admitForm.remark })
    ElMessage.success('录取成功')
    admitDialogVisible.value = false
    search()
  } finally {
    submitting.value = false
  }
}

async function handleConditional() {
  if (!condForm.majorId || !condForm.conditionDesc || !condForm.conditionDeadline) {
    ElMessage.warning('请填写完整信息')
    return
  }
  submitting.value = true
  try {
    await conditionalAdmission(condForm as any)
    ElMessage.success('有条件录取成功')
    conditionalDialogVisible.value = false
    search()
  } finally {
    submitting.value = false
  }
}

async function handleConfirmReject() {
  submitting.value = true
  try {
    await batchReject([rejectForm.pushId])
    ElMessage.success('已拒绝')
    rejectDialogVisible.value = false
    search()
  } finally {
    submitting.value = false
  }
}

async function handleBatchReject() {
  await ElMessageBox.confirm(`确认批量拒绝选中的 ${selectedIds.value.length} 名考生？`, '批量拒绝')
  loading.value = true
  try {
    await batchReject(selectedIds.value)
    ElMessage.success('批量拒绝成功')
    search()
  } finally {
    loading.value = false
  }
}

async function handleExport() {
  try {
    ElMessage.info('正在生成导出文件...')
    const params: any = {}
    if (query.status && query.status.length > 0) params.status = query.status
    if (query.minScore != null) params.minScore = query.minScore
    if (query.maxScore != null) params.maxScore = query.maxScore
    if (query.intentionKeyword) params.intentionKeyword = query.intentionKeyword
    if (query.nationality) params.nationality = query.nationality
    if (query.schoolId) params.schoolId = query.schoolId
    if (dateRange.value && dateRange.value.length === 2) {
      params.pushTimeStart = dateRange.value[0]
      params.pushTimeEnd = dateRange.value[1]
    }
    if (query.majorId) params.majorId = query.majorId
    if (query.round != null) params.round = query.round

    const res = await axios.get('/v1/students/export', {
      params,
      responseType: 'blob',
    })
    const blob = new Blob([res.data], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `考生列表_${new Date().toISOString().slice(0, 10).replace(/-/g, '')}.xlsx`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败，请重试')
  }
}

onMounted(() => {
  // 从 URL 参数读取轮次筛选（来自补录管理跳转）
  const roundParam = route.query.round
  if (roundParam) {
    query.round = Number(roundParam)
  }
  loadSchools()
  search()
})
</script>

<style scoped lang="scss">
.header-actions { display: flex; gap: 8px; }
.filter-card { margin-bottom: 16px; }

.quick-status {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;

  .quick-label { font-size: 13px; color: #909399; }
}

.score-highlight {
  font-weight: 700;
  color: #409eff;
  font-size: 15px;
}

.deadline-text { color: #e6a23c; font-weight: 600; font-size: 13px; }

.full-width { width: 100%; }

.pagination-area {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.ml-8 { margin-left: 8px; }
.mr-8 { margin-right: 8px; }
</style>
