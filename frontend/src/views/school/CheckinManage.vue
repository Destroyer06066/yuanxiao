<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">报到管理</h2>
    </div>

    <el-card>
      <!-- 筛选条件 -->
      <div class="filter-row">
        <el-select v-model="query.status" placeholder="报到状态" style="width: 180px" clearable @change="doSearch">
          <el-option label="待材料收件" value="CONFIRMED" />
          <el-option label="材料已收件" value="MATERIAL_RECEIVED" />
          <el-option label="已完成报到" value="CHECKED_IN" />
        </el-select>
        <el-select v-model="query.materialReceived" placeholder="材料收件状态" style="width: 180px" clearable @change="doSearch">
          <el-option label="未收件" value="false" />
          <el-option label="已收件" value="true" />
        </el-select>
        <el-button @click="doSearch">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </div>

      <!-- 表格 -->
      <el-table :data="list" v-loading="loading" stripe style="margin-top: 12px">
        <el-table-column label="考生姓名" width="120">
          <template #default="{ row }">
            <router-link :to="`/students/${row.pushId}`" class="candidate-link">
              {{ row.candidateName }}
            </router-link>
          </template>
        </el-table-column>
        <el-table-column prop="majorName" label="录取专业" min-width="160" />
        <el-table-column prop="totalScore" label="总分" width="80" />
        <el-table-column prop="status" label="报到状态" width="130">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">
              {{ row.statusDesc }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="materialStatus" label="材料收件状态" width="130">
          <template #default="{ row }">
            <el-tag :type="row.receiveTime ? 'success' : 'info'" size="small">
              {{ row.receiveTime ? '已收件' : '未收件' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态说明" min-width="160">
          <template #default="{ row }">
            {{ statusExplain(row.status) }}
          </template>
        </el-table-column>
        <el-table-column prop="receiveTime" label="材料收件时间" width="170" />
        <el-table-column prop="checkinTime" label="报到时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="can('checkin:material') && row.status === 'CONFIRMED'"
              type="success" link
              @click="handleReceiveMaterial(row)"
            >
              登记收件
            </el-button>
            <el-button
              v-if="can('checkin:confirm') && row.status === 'MATERIAL_RECEIVED'"
              type="primary" link
              @click="handleCheckin(row)"
            >
              确认报到
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
        @size-change="fetchCheckins"
        @current-change="fetchCheckins"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getCheckinList, receiveMaterial, doCheckin, type CheckinRecord } from '@/api/checkin'
import { usePermission } from '@/composables/usePermission'

const { can } = usePermission()

const loading = ref(false)
const list = ref<CheckinRecord[]>([])
const total = ref(0)

const query = reactive({
  status: '',
  materialReceived: '',
  page: 1,
  pageSize: 20,
})

function statusTagType(status: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' {
  const map: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
    CONFIRMED: 'info',
    MATERIAL_RECEIVED: 'warning',
    CHECKED_IN: 'success',
  }
  return map[status] || 'info'
}

function statusExplain(status: string) {
  const map: Record<string, string> = {
    CONFIRMED: '已预录取，等待考生提交材料',
    MATERIAL_RECEIVED: '材料已收件，等待确认报到',
    CHECKED_IN: '已完成报到入学',
  }
  return map[status] || ''
}

async function fetchCheckins() {
  loading.value = true
  try {
    const res = await getCheckinList()
    let data: CheckinRecord[] = res.data.data || []
    // 前端筛选
    if (query.status) {
      data = data.filter(d => d.status === query.status)
    }
    if (query.materialReceived === 'true') {
      data = data.filter(d => d.receiveTime)
    } else if (query.materialReceived === 'false') {
      data = data.filter(d => !d.receiveTime)
    }
    list.value = data
    total.value = data.length
  } catch {
    // error handled by axios interceptor
  } finally {
    loading.value = false
  }
}

function doSearch() {
  query.page = 1
  fetchCheckins()
}

function resetQuery() {
  query.status = ''
  query.materialReceived = ''
  query.page = 1
  fetchCheckins()
}

async function handleReceiveMaterial(row: CheckinRecord) {
  try {
    await receiveMaterial(row.pushId)
    ElMessage.success('已登记收件')
    fetchCheckins()
  } catch {
    // error handled by axios interceptor
  }
}

async function handleCheckin(row: CheckinRecord) {
  try {
    await doCheckin(row.pushId)
    ElMessage.success('已确认报到')
    fetchCheckins()
  } catch {
    // error handled by axios interceptor
  }
}

onMounted(fetchCheckins)
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
.candidate-link {
  color: #409eff;
  text-decoration: none;
  &:hover { text-decoration: underline; }
}
</style>
