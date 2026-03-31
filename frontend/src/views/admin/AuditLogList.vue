<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">操作日志</h2>
    </div>

    <el-card>
      <!-- 筛选 -->
      <div class="filter-row">
        <el-input
          v-model="query.operatorName"
          placeholder="操作人姓名"
          style="width: 160px"
          clearable
          @change="doSearch"
        />
        <el-select v-model="query.action" placeholder="操作类型" style="width: 160px" clearable @change="doSearch">
          <el-option label="录取" value="ADMIT" />
          <el-option label="有条件录取" value="CONDITIONAL" />
          <el-option label="终裁录取" value="FINALIZE" />
          <el-option label="拒绝" value="REJECT" />
          <el-option label="撤销" value="REVOKE" />
          <el-option label="确认报到" value="CONFIRM" />
          <el-option label="材料收件" value="MATERIAL_RECEIVE" />
          <el-option label="报到" value="CHECKIN" />
          <el-option label="失效" value="INVALIDATE" />
        </el-select>
        <el-date-picker
          v-model="query.dateRange"
          type="datetimerange"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          style="width: 340px"
          @change="doSearch"
          format="YYYY-MM-DD HH:mm"
          value-format="YYYY-MM-DD HH:mm:ss"
        />
        <el-button @click="resetQuery">重置</el-button>
      </div>

      <!-- 表格 -->
      <el-table :data="list" v-loading="loading" stripe style="margin-top: 16px" show-overflow-tooltip>
        <el-table-column prop="operatedAt" label="时间" width="170">
          <template #default="{ row }">{{ formatTime(row.operatedAt) }}</template>
        </el-table-column>
        <el-table-column prop="operatorName" label="操作人" width="120" />
        <el-table-column prop="operatorRole" label="角色" width="120">
          <template #default="{ row }">{{ roleLabel(row.operatorRole) }}</template>
        </el-table-column>
        <el-table-column prop="schoolName" label="院校" width="160" />
        <el-table-column prop="action" label="操作类型" width="130">
          <template #default="{ row }">
            <el-tag size="small" :type="actionTagType(row.action)">{{ actionLabel(row.action) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetType" label="目标类型" width="100" />
        <el-table-column prop="targetId" label="目标ID" width="220">
          <template #default="{ row }">
            <span class="mono-text">{{ row.targetId || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="ipAddress" label="IP" width="140">
          <template #default="{ row }">{{ row.ipAddress || '-' }}</template>
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
        @size-change="fetchLogs"
        @current-change="fetchLogs"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import axios from '@/api/axios'

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)

const query = reactive({
  page: 1,
  pageSize: 20,
  operatorName: '',
  action: '',
  dateRange: [] as string[],
})

async function fetchLogs() {
  loading.value = true
  try {
    const params: any = {
      page: query.page,
      pageSize: query.pageSize,
    }
    if (query.operatorName) params.operatorName = query.operatorName
    if (query.action) params.action = query.action
    if (query.dateRange && query.dateRange.length === 2) {
      params.startTime = query.dateRange[0]
      params.endTime = query.dateRange[1]
    }

    const res = await axios.get('/v1/admin/audit-logs', { params })
    const page = res.data.data
    list.value = page.records || []
    total.value = page.total || 0
  } finally {
    loading.value = false
  }
}

function doSearch() {
  query.page = 1
  fetchLogs()
}

function resetQuery() {
  query.operatorName = ''
  query.action = ''
  query.dateRange = []
  query.page = 1
  fetchLogs()
}

function formatTime(iso: string) {
  if (!iso) return '-'
  return new Date(iso).toLocaleString('zh-CN', { hour12: false })
}

const roleMap: Record<string, string> = {
  OP_ADMIN: '运营管理员',
  SCHOOL_ADMIN: '院校管理员',
  SCHOOL_STAFF: '院校工作人员',
}
function roleLabel(role: string) {
  return roleMap[role] || role || '-'
}

const actionMap: Record<string, string> = {
  ADMIT: '直接录取',
  CONDITIONAL: '有条件录取',
  FINALIZE: '终裁录取',
  REJECT: '拒绝',
  REVOKE: '撤销录取',
  CONFIRM: '确认录取',
  MATERIAL_RECEIVE: '材料收件',
  CHECKIN: '确认报到',
  PUSH: '推送',
  CONDITION_EXPIRED: '条件到期',
  INVALIDATE: '录取失效',
}
function actionLabel(action: string) {
  return actionMap[action] || action || '-'
}

const actionTypeMap: Record<string, string> = {
  ADMIT: 'success',
  CONDITIONAL: 'warning',
  FINALIZE: 'success',
  REJECT: 'danger',
  REVOKE: 'info',
  CONFIRM: 'success',
  MATERIAL_RECEIVE: 'warning',
  CHECKIN: 'success',
  PUSH: '',
  CONDITION_EXPIRED: 'warning',
  INVALIDATE: 'danger',
}
function actionTagType(action: string): '' | 'success' | 'warning' | 'danger' | 'info' {
  return (actionTypeMap[action] || '') as any
}

onMounted(fetchLogs)
</script>

<style scoped lang="scss">
.page-container { padding: 20px; }
.page-header {
  display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px;
}
.page-title { margin: 0; font-size: 18px; font-weight: 600; }
.filter-row {
  display: flex; align-items: center; gap: 8px; flex-wrap: wrap;
}
.mono-text { font-family: monospace; font-size: 12px; color: #606266; }
</style>
