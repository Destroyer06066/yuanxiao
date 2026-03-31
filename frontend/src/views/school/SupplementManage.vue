<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">补录管理</h2>
      <el-button v-if="authStore.isOpAdmin" type="primary" @click="openCreateDialog">
        <el-icon><Plus /></el-icon> 新建补录轮次
      </el-button>
    </div>

    <!-- 轮次卡片 -->
    <el-row :gutter="16" class="round-cards">
      <el-col :span="8" v-for="round in rounds" :key="round.roundId">
        <el-card class="round-card" :class="round.status" @click="goToStudents(round)">
          <template #header>
            <div class="round-header">
              <span class="round-name">第 {{ round.roundNumber }} 轮补录</span>
              <el-tag :type="statusType(round.status)" size="small">{{ statusText(round.status) }}</el-tag>
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
              v-if="round.status === 'UPCOMING'"
              type="success" link size="small"
              @click="activateRound(round)"
            >
              开启
            </el-button>
            <el-button
              v-if="round.status === 'ACTIVE'"
              type="warning" link size="small"
              @click="closeRound(round)"
            >
              关闭
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <EmptyState
      v-if="rounds.length === 0"
      title="暂无补录轮次"
      description="请创建新的补录轮次以继续招生"
      icon="Tickets"
      action="新建补录轮次"
      @action="openCreate"
    />

    <!-- 新建轮次对话框 -->
    <el-dialog v-model="createDialogVisible" title="新建补录轮次" width="480px">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="轮次序号">
          <el-input-number v-model="createForm.roundNumber" :min="1" />
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Clock } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import axios from '@/api/axios'
import dayjs from 'dayjs'

const router = useRouter()
const authStore = useAuthStore()

const rounds = ref<any[]>([])
const createDialogVisible = ref(false)
const submitting = ref(false)
const createForm = reactive({
  roundNumber: 1,
  startTime: '',
  endTime: '',
  remark: '',
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
  return s === 'ACTIVE' ? '进行中' : s === 'UPCOMING' ? '未开始' : '已结束'
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
  await ElMessageBox.confirm(`确认开启第 ${round.roundNumber} 轮补录？`)
  await axios.patch(`/v1/supplement/rounds/${round.roundId}`, { status: 'ACTIVE' })
  ElMessage.success('已开启')
  fetchRounds()
}

async function closeRound(round: any) {
  await ElMessageBox.confirm(`确认关闭第 ${round.roundNumber} 轮补录？`)
  await axios.patch(`/v1/supplement/rounds/${round.roundId}`, { status: 'CLOSED' })
  ElMessage.success('已关闭')
  fetchRounds()
}

function goToStudents(round: any) {
  router.push(`/students?round=${round.roundNumber}`)
}

onMounted(fetchRounds)
</script>

<style scoped lang="scss">
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
