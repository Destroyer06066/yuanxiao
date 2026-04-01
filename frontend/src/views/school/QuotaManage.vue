<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">名额管理</h2>
      <el-button v-if="can('quota:create')" type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon> 新增名额
      </el-button>
    </div>

    <el-card>
      <!-- 筛选条件 -->
      <div class="filter-row">
        <el-select v-model="query.majorId" placeholder="选择专业" style="width: 200px" clearable filterable @change="doSearch">
          <el-option v-for="m in majorList" :key="m.majorId" :label="m.majorName" :value="m.majorId" />
        </el-select>
        <el-select v-model="query.year" placeholder="选择年份" style="width: 140px" clearable @change="doSearch">
          <el-option :label="currentYear" :value="currentYear" />
          <el-option :label="nextYear" :value="nextYear" />
        </el-select>
        <el-button @click="doSearch">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </div>

      <!-- 表格 -->
      <el-table :data="list" v-loading="loading" stripe style="margin-top: 12px"
        :row-class-name="quotaRowClass">
        <el-table-column prop="majorName" label="专业" min-width="160" />
        <el-table-column prop="year" label="年份" width="100" />
        <el-table-column prop="totalQuota" label="总名额" width="100" />
        <el-table-column prop="enrolledCount" label="已录取（已确认）" width="120" />
        <el-table-column prop="reservedCount" label="已录取（未确认）" width="120" />
        <el-table-column label="剩余" width="100">
          <template #default="{ row }">
            <span :class="remainingClass(row)">{{ remaining(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="使用率" width="120">
          <template #default="{ row }">
            <el-progress
              :percentage="usageRate(row)"
              :color="usageColor(row)"
              :stroke-width="8"
              style="width: 100px"
            />
          </template>
        </el-table-column>
        <el-table-column label="录取分数区间" width="130">
          <template #default="{ row }">
            {{ row.minScore != null || row.maxScore != null
              ? (row.minScore ?? '-') + ' ~ ' + (row.maxScore ?? '-')
              : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button v-if="can('quota:edit')" type="primary" link @click="openEdit(row)">编辑</el-button>
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
        @size-change="fetchQuotas"
        @current-change="fetchQuotas"
      />
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑名额' : '新增名额'"
      width="680px"
      :close-on-click-modal="false"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="专业" prop="majorId">
          <el-select v-model="form.majorId" placeholder="请选择专业" style="width: 100%" filterable :disabled="isEdit">
            <el-option v-for="m in majorList" :key="m.majorId" :label="m.majorName" :value="m.majorId" />
          </el-select>
        </el-form-item>
        <el-form-item label="年份" prop="year">
          <el-select v-model="form.year" placeholder="请选择年份" style="width: 100%" :disabled="isEdit">
            <el-option :label="currentYear" :value="currentYear" />
            <el-option :label="nextYear" :value="nextYear" />
          </el-select>
        </el-form-item>
        <el-form-item label="总名额" prop="totalQuota">
          <el-input-number v-model="form.totalQuota" :min="0" :max="99999" placeholder="请输入总名额数" style="width: 100%" />
        </el-form-item>
        <el-form-item label="录取分数区间">
          <div style="display: flex; gap: 8px; align-items: center; width: 100%">
            <el-input-number v-model="form.minScore" :min="0" :max="750" placeholder="最低分" style="width: 50%" />
            <span style="color: #909399">~</span>
            <el-input-number v-model="form.maxScore" :min="0" :max="750" placeholder="最高分" style="width: 50%" />
          </div>
        </el-form-item>
        <el-form-item label="录取时间段">
          <div style="display: flex; gap: 8px; align-items: center; width: 100%">
            <el-date-picker
              v-model="form.startTime"
              type="datetime"
              placeholder="开始时间（可空）"
              format="YYYY-MM-DD HH:mm"
              style="width: 50%"
            />
            <span style="color: #909399">~</span>
            <el-date-picker
              v-model="form.deadline"
              type="datetime"
              placeholder="截止时间"
              format="YYYY-MM-DD HH:mm"
              style="width: 50%"
            />
          </div>
        </el-form-item>
        <hr style="border: none; border-top: 1px solid #eee; margin: 16px 0" />
        <div style="font-weight: 600; margin-bottom: 12px">分数线配置</div>
        <el-form-item label="总分最低分">
          <el-input-number v-model="scoreLineForm.totalScore" :min="0" :max="750" style="width: 100%" />
        </el-form-item>
        <div style="margin-bottom: 8px; color: #606266">单科线</div>
        <el-table :data="scoreLineForm.subjects" stripe size="small" style="margin-bottom: 8px">
          <el-table-column prop="subject" label="科目" width="200">
            <template #default="{ row }">
              <el-input v-model="row.subject" placeholder="如：语文" />
            </template>
          </el-table-column>
          <el-table-column prop="minScore" label="最低分" min-width="150">
            <template #default="{ row }">
              <el-input-number v-model="row.minScore" :min="0" :max="200" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column width="80">
            <template #default="{ $index }">
              <el-button type="danger" link @click="scoreLineForm.subjects.splice($index, 1)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-button size="small" @click="scoreLineForm.subjects.push({ subject: '', minScore: 0 })">+ 添加单科</el-button>
        <!-- 编辑时显示已占用信息 -->
        <el-alert
          v-if="isEdit && editingRow"
          type="warning"
          :closable="false"
          show-icon
          style="margin-top: 4px"
        >
          当前已录取（已确认）<strong>{{ editingRow.enrolledCount }}</strong> 人，已录取（未确认）<strong>{{ editingRow.reservedCount }}</strong> 人，
          剩余可用名额 <strong>{{ (editingRow.totalQuota || 0) - (editingRow.enrolledCount || 0) - (editingRow.reservedCount || 0) }}</strong> 个。
          新总名额不得少于 <strong>{{ (editingRow.enrolledCount || 0) + (editingRow.reservedCount || 0) }}</strong>。
        </el-alert>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import axios from '@/api/axios'
import { usePermission } from '@/composables/usePermission'
import { useAuthStore } from '@/stores/auth'

const { can } = usePermission()
const auth = useAuthStore()

const currentYear = new Date().getFullYear()
const nextYear = currentYear + 1

// ========== 数据 ==========
const loading = ref(false)
const submitting = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const majorList = ref<any[]>([])
const isEdit = ref(false)
const dialogVisible = ref(false)
const editingId = ref('')
const editingRow = ref<any>(null)
const formRef = ref<FormInstance>()

const query = reactive({
  majorId: '',
  year: '',
  page: 1,
  pageSize: 20,
})

const form = reactive({
  majorId: '',
  year: currentYear,
  totalQuota: 0,
  minScore: null as number | null,
  maxScore: null as number | null,
  startTime: null as Date | null,
  deadline: null as Date | null,
})

const rules: FormRules = {
  majorId: [{ required: true, message: '请选择专业', trigger: 'change' }],
  year: [{ required: true, message: '请选择年份', trigger: 'change' }],
  totalQuota: [{ required: true, message: '请输入总名额数', trigger: 'blur' }],
}

function remaining(row: any): number {
  return (row.totalQuota || 0) - (row.enrolledCount || 0) - (row.reservedCount || 0)
}

function remainingClass(row: any): string {
  const rem = remaining(row)
  const total = row.totalQuota || 0
  if (total === 0) return ''
  if (rem === 0) return 'quota-zero'
  if (rem / total <= 0.2) return 'quota-warn'
  return ''
}

function usageRate(row: any): number {
  const total = row.totalQuota || 0
  if (total === 0) return 0
  return Math.min(100, Math.round(((row.enrolledCount || 0) + (row.reservedCount || 0)) / total * 100))
}

function usageColor(row: any): string {
  const rem = remaining(row)
  const total = row.totalQuota || 0
  if (total === 0) return '#909399'
  if (rem === 0) return '#f56c6c'
  if (rem / total <= 0.2) return '#e6a23c'
  return '#67c23a'
}

function quotaRowClass({ row }: { row: any }): string {
  return remainingClass(row)
}

// ========== 接口 ==========
async function fetchMajors() {
  try {
    const params: any = { page: 1, pageSize: 200 }
    if (auth.schoolId) params.schoolId = auth.schoolId
    const res = await axios.get('/v1/majors', { params })
    majorList.value = res.data.data.records || []
  } catch {
    // error handled by axios interceptor
  }
}

async function fetchQuotas() {
  loading.value = true
  try {
    const params: any = { page: query.page, pageSize: query.pageSize }
    if (query.majorId) params.majorId = query.majorId
    if (query.year) params.year = query.year
    if (auth.schoolId) params.schoolId = auth.schoolId
    const res = await axios.get('/v1/quotas', { params })
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
  fetchQuotas()
}

function resetQuery() {
  query.majorId = ''
  query.year = ''
  query.page = 1
  fetchQuotas()
}

// ========== 弹窗 ==========
function openCreate() {
  isEdit.value = false
  dialogVisible.value = true
}

async function openEdit(row: any) {
  isEdit.value = true
  editingId.value = row.quotaId
  editingRow.value = row
  Object.assign(form, {
    majorId: row.majorId,
    year: row.year,
    totalQuota: row.totalQuota,
    minScore: row.minScore ?? null,
    maxScore: row.maxScore ?? null,
    startTime: row.startTime ? new Date(row.startTime) : null,
    deadline: row.deadline ? new Date(row.deadline) : null,
  })
  // 加载分数线
  const res = await axios.get('/v1/score-lines')
  const lines = (res.data.data || []).filter((l: any) => l.majorId === row.majorId && l.year === row.year)
  scoreLineForm.totalScore = null
  scoreLineForm.subjects = []
  for (const line of lines) {
    if (line.subject === 'TOTAL') {
      scoreLineForm.totalScore = line.minScore
    } else {
      scoreLineForm.subjects.push({ subject: line.subject, minScore: line.minScore })
    }
  }
  dialogVisible.value = true
}

function resetForm() {
  formRef.value?.resetFields()
  form.majorId = ''
  form.year = currentYear
  form.totalQuota = 0
  form.minScore = null
  form.maxScore = null
  form.startTime = null
  form.deadline = null
  scoreLineForm.totalScore = null
  scoreLineForm.subjects = []
  editingRow.value = null
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  if (isEdit.value && editingRow.value) {
    const minRequired = (editingRow.value.enrolledCount || 0) + (editingRow.value.reservedCount || 0)
    if (form.totalQuota < minRequired) {
      ElMessage.error(`总名额不得少于已占用名额（${minRequired}），请先调整录取状态`)
      return
    }
  }

  submitting.value = true
  try {
    if (isEdit.value) {
      await axios.put(`/v1/quotas/${editingId.value}`, {
        totalQuota: form.totalQuota,
        minScore: form.minScore,
        maxScore: form.maxScore,
        startTime: form.startTime ? new Date(form.startTime).toISOString() : null,
        deadline: form.deadline ? new Date(form.deadline).toISOString() : null,
      })
      await saveScoreLines()
    } else {
      await axios.post('/v1/quotas', {
        majorId: form.majorId,
        year: form.year,
        totalQuota: form.totalQuota,
        minScore: form.minScore,
        maxScore: form.maxScore,
        startTime: form.startTime ? new Date(form.startTime).toISOString() : null,
        deadline: form.deadline ? new Date(form.deadline).toISOString() : null,
      })
      await saveScoreLines()
    }
    dialogVisible.value = false
    fetchQuotas()
  } catch {
    // error handled by axios interceptor
  } finally {
    submitting.value = false
  }
}

// ========== 分数线 ==========
const scoreLineForm = reactive({
  totalScore: null as number | null,
  subjects: [] as { subject: string; minScore: number }[]
})

async function saveScoreLines() {
  const majorId = form.majorId
  const year = form.year
  const res = await axios.get('/v1/score-lines')
  const existing = (res.data.data || []).filter((l: any) => l.majorId === majorId && l.year === year)

  const totalLine = existing.find((l: any) => l.subject === 'TOTAL')
  if (scoreLineForm.totalScore != null) {
    if (totalLine) {
      await axios.put(`/v1/score-lines/${totalLine.lineId}`, {
        majorId, year, subject: 'TOTAL', minScore: scoreLineForm.totalScore
      })
    } else {
      await axios.post('/v1/score-lines', {
        majorId, year, subject: 'TOTAL', minScore: scoreLineForm.totalScore
      })
    }
  } else if (totalLine) {
    await axios.delete(`/v1/score-lines/${totalLine.lineId}`)
  }

  const subjectLines = existing.filter((l: any) => l.subject !== 'TOTAL')
  for (const sl of subjectLines) {
    await axios.delete(`/v1/score-lines/${sl.lineId}`)
  }
  for (const s of scoreLineForm.subjects) {
    if (s.subject && s.minScore != null) {
      await axios.post('/v1/score-lines', {
        majorId, year, subject: s.subject, minScore: s.minScore
      })
    }
  }
  ElMessage.success('保存成功')
}

// ========== 初始化 ==========
onMounted(() => {
  fetchMajors()
  fetchQuotas()
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
.quota-zero { color: #f56c6c; font-weight: 700; }
.quota-warn { color: #e6a23c; font-weight: 600; }

:deep(.el-table .quota-zero) { background-color: rgba(245, 108, 108, 0.08) !important; }
:deep(.el-table .quota-warn) { background-color: rgba(230, 162, 60, 0.08) !important; }
</style>
