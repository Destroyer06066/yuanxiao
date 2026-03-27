<template>
  <div class="dashboard">
    <div class="page-header">
      <h2 class="page-title">工作台</h2>
      <span class="greeting">欢迎回来，{{ authStore.realName }}</span>
    </div>

    <!-- ========== OP_ADMIN 专属内容 ========== -->
    <template v-if="authStore.isOpAdmin">

      <!-- 第一行：全局 KPI -->
      <el-row :gutter="20" class="stat-row">
        <el-col :span="6" v-for="stat in stats" :key="stat.label">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-icon" :style="{ background: stat.color }">
              <el-icon :size="24"><component :is="stat.icon" /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stat.value }}</div>
              <div class="stat-label">{{ stat.label }}</div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 第二行：各校录取进度排行 -->
      <el-card class="recent-card">
        <template #header>
          <div class="card-header">
            <span>各校录取进度</span>
          </div>
        </template>
        <el-table :data="schoolProgress" stripe v-loading="loadingSchoolProgress">
          <el-table-column prop="schoolName" label="院校" min-width="160" />
          <el-table-column prop="pushed" label="推送人数" width="100" align="center" />
          <el-table-column prop="admitted" label="已录取" width="100" align="center" />
          <el-table-column prop="confirmed" label="已确认" width="100" align="center" />
          <el-table-column prop="checkedIn" label="已报到" width="100" align="center" />
          <el-table-column prop="admissionRate" label="录取率" width="100" align="center">
            <template #default="{ row }">
              <span :class="row.admissionRate >= 50 ? 'rate-high' : 'rate-low'">
                {{ row.admissionRate }}%
              </span>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 第三行：异常提醒 -->
      <el-row :gutter="16" class="alert-row">
        <el-col :span="8">
          <el-card shadow="hover" class="alert-card alert-warning" @click="router.push('/students?status=CONDITIONAL')">
            <div class="alert-icon"><el-icon :size="28"><Clock /></el-icon></div>
            <div class="alert-info">
              <div class="alert-value">{{ alerts.conditionExpiringSoon }}</div>
              <div class="alert-label">有条件录取即将到期（3天内）</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="alert-card alert-danger" @click="router.push('/quota')">
            <div class="alert-icon"><el-icon :size="28"><Warning /></el-icon></div>
            <div class="alert-info">
              <div class="alert-value">{{ alerts.quotaOver90 }}</div>
              <div class="alert-label">名额使用超 90% 的专业</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="alert-card alert-info" @click="router.push('/students')">
            <div class="alert-icon"><el-icon :size="28"><User /></el-icon></div>
            <div class="alert-info">
              <div class="alert-value">{{ alerts.todayPushed }}</div>
              <div class="alert-label">今日新推送考生</div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 第四行：近期操作动态 -->
      <el-card class="recent-card">
        <template #header>
          <div class="card-header">
            <span>近期操作动态</span>
          </div>
        </template>
        <el-table :data="recentOps" stripe v-loading="loadingRecentOps">
          <el-table-column prop="createdAt" label="时间" width="170">
            <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column prop="schoolName" label="院校" width="160" />
          <el-table-column prop="operatorName" label="操作人" width="120" />
          <el-table-column prop="action" label="操作类型" width="120" />
          <el-table-column prop="candidateName" label="考生" width="120" />
          <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
        </el-table>
      </el-card>

    </template>

    <!-- ========== SCHOOL_ADMIN / STAFF 专属内容 ========== -->
    <template v-else>

      <!-- 第一行：本校 KPI -->
      <el-row :gutter="20" class="stat-row">
        <el-col :span="6" v-for="stat in stats" :key="stat.label">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-icon" :style="{ background: stat.color }">
              <el-icon :size="24"><component :is="stat.icon" /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stat.value }}</div>
              <div class="stat-label">{{ stat.label }}</div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 第二行：待办事项 -->
      <el-card class="recent-card">
        <template #header>
          <div class="card-header">
            <span>待办事项</span>
          </div>
        </template>
        <el-row :gutter="16">
          <el-col :span="6" v-for="item in todoItems" :key="item.label">
            <div class="todo-card" @click="item.onClick">
              <div class="todo-value" :style="{ color: item.color }">{{ item.value }}</div>
              <div class="todo-label">{{ item.label }}</div>
            </div>
          </el-col>
        </el-row>
      </el-card>

      <!-- 第三行：名额使用概览 -->
      <el-card class="recent-card" v-if="quotaUsage.length > 0">
        <template #header>
          <div class="card-header">
            <span>名额使用概览</span>
          </div>
        </template>
        <div class="quota-list">
          <div
            v-for="q in quotaUsage"
            :key="q.quotaId"
            class="quota-item"
          >
            <span class="quota-name">{{ q.majorName }}</span>
            <div class="quota-bar-wrap">
              <div
                class="quota-bar"
                :style="{ width: q.usageRate + '%', background: q.usageRate >= 90 ? '#f56c6c' : q.usageRate >= 70 ? '#e6a23c' : '#67c23a' }"
              />
            </div>
            <span class="quota-text">{{ q.used }}/{{ q.totalQuota }}（剩余 {{ q.remaining }}）</span>
          </div>
        </div>
      </el-card>

      <!-- 第四行：最近录取操作 -->
      <el-card class="recent-card">
        <template #header>
          <div class="card-header">
            <span>最近录取操作</span>
            <el-button type="primary" link @click="router.push('/students')">查看全部</el-button>
          </div>
        </template>
        <el-table :data="recentOps" stripe v-loading="loadingRecentOps">
          <el-table-column prop="candidateName" label="考生姓名" />
          <el-table-column prop="action" label="操作" />
          <el-table-column prop="operatorName" label="操作人" />
          <el-table-column prop="createdAt" label="时间" width="180">
            <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
          </el-table-column>
        </el-table>
      </el-card>

    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import axios from '@/api/axios'
import {
  Clock, Trophy, Check, User, Warning, Bell
} from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()

// ========== 通用 KPI ==========
const stats = ref([
  { label: '待处理考生', value: 0, icon: 'Clock', color: '#409eff' },
  { label: '已录取', value: 0, icon: 'Trophy', color: '#67c23a' },
  { label: '已确认', value: 0, icon: 'Check', color: '#e6a23c' },
  { label: '已报到', value: 0, icon: 'User', color: '#25a861' },
])

// ========== OP_ADMIN 特有 ==========
const schoolProgress = ref<any[]>([])
const alerts = ref({ conditionExpiringSoon: 0, quotaOver90: 0, todayPushed: 0 })
const loadingSchoolProgress = ref(false)

// ========== SCHOOL 特有 ==========
const todoItems = ref<any[]>([])
const quotaUsage = ref<any[]>([])

// ========== 通用 ==========
const recentOps = ref<any[]>([])
const loadingRecentOps = ref(false)

async function loadStats() {
  try {
    const res = await axios.get('/v1/statistics/kpis')
    const d = res.data.data || {}
    stats.value[0].value = d.totalPushed ?? 0
    stats.value[1].value = d.admitted ?? 0
    stats.value[2].value = d.confirmed ?? 0
    stats.value[3].value = d.checkedIn ?? 0
  } catch { /* ignore */ }
}

async function loadSchoolProgress() {
  loadingSchoolProgress.value = true
  try {
    const res = await axios.get('/v1/statistics/school-progress')
    schoolProgress.value = res.data.data || []
  } catch { /* ignore */ } finally {
    loadingSchoolProgress.value = false
  }
}

async function loadAlerts() {
  try {
    const res = await axios.get('/v1/statistics/alerts')
    alerts.value = res.data.data || {}
  } catch { /* ignore */ }
}

async function loadRecentOps(limit = 20) {
  loadingRecentOps.value = true
  try {
    const res = await axios.get('/v1/statistics/recent-operations', { params: { limit } })
    recentOps.value = res.data.data || []
  } catch { /* ignore */ } finally {
    loadingRecentOps.value = false
  }
}

async function loadTodoItems() {
  // 复用 alerts 数据中的部分
  await loadAlerts()
  todoItems.value = [
    {
      label: '待处理考生',
      value: alerts.value.todayPushed || 0,
      color: '#409eff',
      onClick: () => router.push('/students'),
    },
    {
      label: '有条件录取即将到期',
      value: alerts.value.conditionExpiringSoon || 0,
      color: '#e6a23c',
      onClick: () => router.push('/students?status=CONDITIONAL'),
    },
    {
      label: '名额超 90%',
      value: alerts.value.quotaOver90 || 0,
      color: '#f56c6c',
      onClick: () => router.push('/quota'),
    },
  ]
}

async function loadQuotaUsage() {
  try {
    const res = await axios.get('/v1/statistics/quota-usage')
    quotaUsage.value = res.data.data || []
  } catch { /* ignore */ }
}

function formatTime(iso: string) {
  if (!iso) return ''
  return new Date(iso).toLocaleString('zh-CN', { hour12: false })
}

onMounted(async () => {
  await authStore.fetchUserInfo()
  await loadStats()

  if (authStore.isOpAdmin) {
    loadSchoolProgress()
    loadAlerts()
    loadRecentOps()
  } else {
    loadTodoItems()
    loadQuotaUsage()
    loadRecentOps(10)
  }
})
</script>

<style scoped lang="scss">
.dashboard {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;

  .page-title {
    font-size: 20px;
    font-weight: 600;
  }

  .greeting {
    color: #909399;
    font-size: 14px;
  }
}

.stat-row {
  margin-bottom: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px;

  .stat-icon {
    width: 56px;
    height: 56px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    flex-shrink: 0;
  }

  .stat-info {
    .stat-value {
      font-size: 24px;
      font-weight: 700;
      color: #303133;
      line-height: 1.2;
    }
    .stat-label {
      font-size: 13px;
      color: #909399;
      margin-top: 4px;
    }
  }
}

.recent-card {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.alert-row {
  margin-bottom: 16px;
}

.alert-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  cursor: pointer;
  transition: transform 0.2s;

  &:hover { transform: translateY(-2px); }

  .alert-icon {
    width: 48px;
    height: 48px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    flex-shrink: 0;
  }

  .alert-info {
    .alert-value {
      font-size: 28px;
      font-weight: 700;
      line-height: 1.2;
    }
    .alert-label {
      font-size: 12px;
      color: #909399;
      margin-top: 4px;
    }
  }
}

.alert-warning .alert-icon { background: #e6a23c; }
.alert-danger  .alert-icon { background: #f56c6c; }
.alert-info    .alert-icon { background: #409eff; }

.rate-high { color: #67c23a; font-weight: 600; }
.rate-low  { color: #f56c6c; font-weight: 600; }

.todo-card {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 16px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;

  &:hover { border-color: #409eff; background: #f0f7ff; }

  .todo-value {
    font-size: 32px;
    font-weight: 700;
    line-height: 1;
  }
  .todo-label {
    font-size: 12px;
    color: #606266;
    margin-top: 8px;
  }
}

.quota-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.quota-item {
  display: flex;
  align-items: center;
  gap: 12px;

  .quota-name {
    width: 140px;
    font-size: 13px;
    color: #303133;
    flex-shrink: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .quota-bar-wrap {
    flex: 1;
    height: 12px;
    background: #f0f2f5;
    border-radius: 6px;
    overflow: hidden;
  }

  .quota-bar {
    height: 100%;
    border-radius: 6px;
    transition: width 0.5s ease;
  }

  .quota-text {
    width: 140px;
    font-size: 12px;
    color: #909399;
    text-align: right;
    flex-shrink: 0;
  }
}
</style>
