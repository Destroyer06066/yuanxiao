<template>
  <div class="dashboard">
    <div class="page-header">
      <h2 class="page-title">工作台</h2>
      <span class="greeting">欢迎回来，{{ authStore.realName }}</span>
    </div>

    <!-- 统计卡片 -->
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

    <!-- 最近录取操作 -->
    <el-card class="recent-card">
      <template #header>
        <div class="card-header">
          <span>最近录取操作</span>
          <el-button type="primary" link @click="$router.push('/students')">查看全部</el-button>
        </div>
      </template>
      <el-table :data="recentOps" stripe>
        <el-table-column prop="candidateName" label="考生姓名" />
        <el-table-column prop="schoolName" label="院校" />
        <el-table-column prop="majorName" label="专业" />
        <el-table-column prop="action" label="操作" />
        <el-table-column prop="operatorName" label="操作人" />
        <el-table-column prop="operatedAt" label="时间" width="180" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import axios from '@/api/axios'

const authStore = useAuthStore()

const stats = ref([
  { label: '待处理考生', value: 0, icon: 'Clock', color: '#409eff' },
  { label: '已录取', value: 0, icon: 'Trophy', color: '#67c23a' },
  { label: '已确认', value: 0, icon: 'Check', color: '#e6a23c' },
  { label: '已报到', value: 0, icon: 'User', color: '#25a861' },
])

const recentOps = ref<any[]>([])

async function loadStats() {
  try {
    const res = await axios.get('/api/v1/statistics/kpis')
    const d = res.data.data || {}
    stats.value[0].value = d.totalPushed ?? 0
    stats.value[1].value = d.admitted ?? 0
    stats.value[2].value = d.confirmed ?? 0
    stats.value[3].value = d.checkedIn ?? 0
  } catch (e) {
    // 统计接口失败不影响页面渲染
  }
}

onMounted(() => {
  authStore.fetchUserInfo()
  loadStats()
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
  margin-bottom: 20px;
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

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
