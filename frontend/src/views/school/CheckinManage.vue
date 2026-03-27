<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">报到管理</h2>
    </div>

    <el-card>
      <el-table :data="checkins" v-loading="loading" stripe>
        <el-table-column prop="candidateName" label="考生姓名" />
        <el-table-column prop="majorName" label="录取专业" />
        <el-table-column prop="statusDesc" label="当前状态" width="130">
          <template #default="{ row }">
            <el-tag :class="'status-tag ' + row.status">{{ row.statusDesc }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="receiveTime" label="材料收件时间" width="170" />
        <el-table-column prop="checkinTime" label="报到时间" width="170" />
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'CONFIRMED'"
              type="success" link
              @click="handleReceiveMaterial(row)"
            >
              登记收件
            </el-button>
            <el-button
              v-if="row.status === 'MATERIAL_RECEIVED'"
              type="primary" link
              @click="handleCheckin(row)"
            >
              确认报到
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import axios from '@/api/axios'

const loading = ref(false)
const checkins = ref<any[]>([])

async function fetch() {
  loading.value = true
  try {
    const res = await axios.get('/api/v1/checkins')
    checkins.value = res.data.data || []
  } finally {
    loading.value = false
  }
}

async function handleReceiveMaterial(row: any) {
  await axios.post(`/api/v1/material-receive`, { pushId: row.pushId })
  ElMessage.success('已登记收件')
  fetch()
}

async function handleCheckin(row: any) {
  await axios.post(`/api/v1/checkin`, { pushId: row.pushId })
  ElMessage.success('已确认报到')
  fetch()
}

onMounted(fetch)
</script>
