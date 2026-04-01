<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">录取轮次</h2>
    </div>

    <!-- 当前录取周期状态提示 -->
    <div>
      <el-alert
        v-if="currentActiveRound && isWithinPeriod"
        type="success"
        :closable="false"
        style="margin-bottom: 16px"
      >
        <template #title>
          <strong>录取周期进行中</strong> —
          剩余 {{ remainingDays }} 天 {{ remainingHours }} 小时
          ({{ formatTime(currentActiveRound.startTime) }} ~ {{ formatTime(currentActiveRound.endTime) }})
        </template>
      </el-alert>
      <el-alert v-else type="info" :closable="false" style="margin-bottom: 16px">
        <template #title>
          <strong v-if="currentActiveRound">录取周期已过期或未在有效期内</strong>
          <strong v-else>当前无进行中的录取周期</strong>
          — 请创建并开启录取轮次
        </template>
      </el-alert>

      <div class="page-header" style="margin-bottom: 16px">
        <el-button v-if="authStore.isOpAdmin" type="primary" @click="openCreateDialog">
          <el-icon><Plus /></el-icon> 新建轮次
        </el-button>
      </div>

      <!-- 轮次卡片 -->
      <el-row :gutter="16" class="round-cards">
        <el-col :span="8" v-for="round in rounds" :key="round.roundId">
          <el-card class="round-card" :class="round.status" @click="goToStudents(round)">
            <template #header>
              <div class="round-header">
                <span class="round-name">第 {{ round.roundNumber }} 轮</span>
                <el-tag :type="statusType(round.status)" size="small">{{ statusText(round.status) }}</el-tag>
                <el-tag v-if="round.mode === 'MODE_2'" type="warning" size="small" style="margin-left: 8px">邀请模式</el-tag>
              </div>
            </template>
            <!-- 统计数字 -->
            <div class="round-stats">
              <div class="stat-item">
                <div class="stat-val">{{ round.pushedCount || 0 }}</div>
                <div class="stat-lbl">推送</div>
              </div>
              <div class="stat-item">
                <div class="stat-val">{{ round.admittedCount || 0 }}</div>
                <div class="stat-lbl">已录取</div>
              </div>
              <div class="stat-item">
                <div class="stat-val">{{ round.confirmedCount || 0 }}</div>
                <div class="stat-lbl">已确认</div>
              </div>
            </div>
            <div class="round-body">
              <div class="round-time">
                <el-icon><Clock /></el-icon>
                {{ formatTime(round.startTime) }} - {{ formatTime(round.endTime) }}
              </div>
              <div v-if="round.remark" class="round-remark">{{ round.remark }}</div>
            </div>
            <div class="round-actions" v-if="authStore.isOpAdmin">
              <el-button
                type="primary" link size="small"
                @click.stop="openEditDialog(round)"
              >
                编辑
              </el-button>
              <el-button
                v-if="round.status === 'UPCOMING' || round.status === 'CLOSED'"
                type="success" link size="small"
                @click.stop="activateRound(round)"
              >
                开启
              </el-button>
              <el-button
                v-if="round.status === 'ACTIVE'"
                type="warning" link size="small"
                @click.stop="closeRound(round)"
              >
                关闭
              </el-button>
              <el-button
                type="danger" link size="small"
                @click.stop="handleDeleteRound(round)"
              >
                删除
              </el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <EmptyState
        v-if="rounds.length === 0"
        title="暂无录取轮次"
        description="请创建新的录取轮次以继续招生"
        icon="Tickets"
        action="新建轮次"
        @action="openCreate"
      />

      <!-- 新建轮次对话框 -->
      <el-dialog v-model="createDialogVisible" title="新建轮次" width="480px">
        <el-form :model="createForm" label-width="100px">
          <el-form-item label="轮次序号">
            <el-input-number v-model="createForm.roundNumber" :min="1" />
          </el-form-item>
          <el-form-item label="录取模式">
            <el-tag :type="createForm.roundNumber === 1 ? 'success' : 'warning'">
              {{ createForm.roundNumber === 1 ? '考生推送模式（第1轮）' : '邀请模式（第2轮及以后）' }}
            </el-tag>
            <span style="margin-left: 8px; color: #909399; font-size: 12px">由系统自动设置，不可修改</span>
          </el-form-item>
          <el-form-item label="开始时间" required>
            <el-date-picker
              v-model="createForm.startTime"
              type="datetime"
              placeholder="选择开始时间"
              value-format="YYYY-MM-DD HH:mm:ss"
            />
          </el-form-item>
          <el-form-item label="结束时间" required>
            <el-date-picker
              v-model="createForm.endTime"
              type="datetime"
              placeholder="选择结束时间"
              value-format="YYYY-MM-DD HH:mm:ss"
            />
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="createForm.remark" type="textarea" :rows="3" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="createDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleCreate">创建</el-button>
        </template>
      </el-dialog>

      <!-- 编辑轮次对话框 -->
      <el-dialog v-model="editDialogVisible" title="编辑轮次" width="480px">
        <el-form :model="editForm" label-width="100px">
          <el-form-item label="轮次">
            <span>第 {{ editForm.roundNumber }} 轮</span>
            <el-tag v-if="editForm.mode === 'MODE_2'" type="warning" size="small" style="margin-left: 8px">邀请模式</el-tag>
          </el-form-item>
          <el-form-item label="开始时间">
            <el-date-picker
              v-model="editForm.startTime"
              type="datetime"
              placeholder="选择开始时间"
              value-format="YYYY-MM-DD HH:mm:ss"
            />
          </el-form-item>
          <el-form-item label="结束时间">
            <el-date-picker
              v-model="editForm.endTime"
              type="datetime"
              placeholder="选择结束时间"
              value-format="YYYY-MM-DD HH:mm:ss"
            />
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="editForm.remark" type="textarea" :rows="3" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="editDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleUpdate">保存</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Clock } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useYearStore } from '@/stores/year'
import axios from '@/api/axios'
import dayjs from 'dayjs'

const router = useRouter()
const authStore = useAuthStore()
const yearStore = useYearStore()

// ========== 录取轮次 ==========
const rounds = ref<any[]>([])
const createDialogVisible = ref(false)
const editDialogVisible = ref(false)
const submitting = ref(false)
const currentRoundId = ref('')
const editForm = reactive({
  roundNumber: 0,
  mode: '',
  startTime: '',
  endTime: '',
  remark: '',
})
const createForm = reactive({
  roundNumber: 1,
  startTime: '',
  endTime: '',
  remark: '',
})

// 当前进行中的录取周期（ACTIVE状态）
const currentActiveRound = computed(() => {
  return rounds.value.find(r => r.status === 'ACTIVE')
})

// 当前时间是否在周期有效期内
const isWithinPeriod = computed(() => {
  if (!currentActiveRound.value) return false
  const now = dayjs()
  const start = dayjs(currentActiveRound.value.startTime)
  const end = dayjs(currentActiveRound.value.endTime)
  return now.isAfter(start) && now.isBefore(end)
})

// 剩余时间
const remainingDays = computed(() => {
  if (!currentActiveRound.value) return 0
  const now = dayjs()
  const end = dayjs(currentActiveRound.value.endTime)
  const diff = end.diff(now, 'day')
  return diff > 0 ? diff : 0
})

const remainingHours = computed(() => {
  if (!currentActiveRound.value) return 0
  const now = dayjs()
  const end = dayjs(currentActiveRound.value.endTime)
  const diff = end.diff(now, 'hour') % 24
  return diff > 0 ? diff : 0
})

async function fetchRounds() {
  const res = await axios.get('/v1/supplement/rounds')
  rounds.value = res.data.data || []
}

function formatTime(ts: string) {
  return ts ? dayjs(ts).format('YYYY-MM-DD HH:mm') : '-'
}

function statusType(s: string) {
  return s === 'ACTIVE' ? 'success' : s === 'UPCOMING' ? 'info' : 'info'
}

function statusText(s: string) {
  return s
}

function openCreateDialog() {
  createForm.roundNumber = rounds.value.length + 1
  createForm.startTime = ''
  createForm.endTime = ''
  createForm.remark = ''
  createDialogVisible.value = true
}

async function handleCreate() {
  if (!createForm.startTime || !createForm.endTime) {
    ElMessage.warning('请填写完整时间')
    return
  }
  submitting.value = true
  try {
    await axios.post('/v1/supplement/rounds', createForm)
    ElMessage.success('创建成功')
    createDialogVisible.value = false
    fetchRounds()
  } finally {
    submitting.value = false
  }
}

async function activateRound(round: any) {
  await ElMessageBox.confirm(`确认开启第 ${round.roundNumber} 轮录取？`)
  await axios.patch(`/v1/supplement/rounds/${round.roundId}`, { status: 'ACTIVE' })
  ElMessage.success('已开启')
  fetchRounds()
}

async function closeRound(round: any) {
  await ElMessageBox.confirm(`确认关闭第 ${round.roundNumber} 轮录取？`)
  await axios.patch(`/v1/supplement/rounds/${round.roundId}`, { status: 'CLOSED' })
  ElMessage.success('已关闭')
  fetchRounds()
}

function openEditDialog(round: any) {
  currentRoundId.value = round.roundId
  editForm.roundNumber = round.roundNumber
  editForm.mode = round.mode || ''
  editForm.startTime = round.startTime ? dayjs(round.startTime).format('YYYY-MM-DD HH:mm:ss') : ''
  editForm.endTime = round.endTime ? dayjs(round.endTime).format('YYYY-MM-DD HH:mm:ss') : ''
  editForm.remark = round.remark || ''
  editDialogVisible.value = true
}

async function handleUpdate() {
  if (!editForm.startTime && !editForm.endTime && !editForm.remark) {
    ElMessage.warning('请至少修改一项内容')
    return
  }
  submitting.value = true
  try {
    const data: any = {}
    if (editForm.startTime) data.startTime = editForm.startTime
    if (editForm.endTime) data.endTime = editForm.endTime
    if (editForm.remark !== undefined) data.remark = editForm.remark
    await axios.put(`/v1/supplement/rounds/${currentRoundId.value}`, data)
    ElMessage.success('更新成功')
    editDialogVisible.value = false
    fetchRounds()
  } catch (e: any) {
    ElMessage.error(e.message || '更新失败')
  } finally {
    submitting.value = false
  }
}

async function handleDeleteRound(round: any) {
  try {
    await ElMessageBox.confirm(
      `确认删除第 ${round.roundNumber} 轮录取？删除后将无法恢复。`,
      '删除确认',
      { type: 'warning' }
    )
    await axios.delete(`/v1/supplement/rounds/${round.roundId}`)
    ElMessage.success('删除成功')
    fetchRounds()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '删除失败')
    }
  }
}

function goToStudents(round: any) {
  router.push(`/students?round=${round.roundNumber}`)
}

function openCreate() {
  openCreateDialog()
}

// ========== 初始化 ==========
onMounted(async () => {
  fetchRounds()
})

// 年度变化时重新加载数据
watch(() => yearStore.selectedYear, () => {
  fetchRounds()
})
</script>

<style scoped lang="scss">
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.round-cards { margin-top: 8px; }

.round-card {
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  &:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.1); }

  .round-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .round-name { font-weight: 600; }
  }

  .round-stats {
    display: flex;
    gap: 0;
    margin-bottom: 12px;
    border: 1px solid #ebeef5;
    border-radius: 8px;
    overflow: hidden;

    .stat-item {
      flex: 1;
      text-align: center;
      padding: 10px 0;
      background: #fafafa;
      border-right: 1px solid #ebeef5;
      &:last-child { border-right: none; }

      .stat-val {
        font-size: 20px;
        font-weight: 700;
        color: #303133;
        line-height: 1;
      }
      .stat-lbl {
        font-size: 11px;
        color: #909399;
        margin-top: 4px;
      }
    }
  }

  .round-body {
    .round-time {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 14px;
      color: #606266;
      margin-bottom: 4px;
    }

    .round-remark {
      font-size: 13px;
      color: #909399;
      margin-top: 8px;
    }
  }

  &.ACTIVE { border-color: #67c23a; }
}
</style>
