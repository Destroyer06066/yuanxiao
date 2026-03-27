<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">分数线配置</h2>
    </div>

    <el-card>
      <el-table :data="lines" v-loading="loading" stripe>
        <el-table-column prop="majorName" label="专业" />
        <el-table-column prop="subject" label="科目" />
        <el-table-column prop="minScore" label="最低分" width="100" />
        <el-table-column prop="year" label="年份" width="100" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import axios from '@/api/axios'

const loading = ref(false)
const lines = ref<any[]>([])

async function fetchLines() {
  loading.value = true
  try {
    const res = await axios.get('/api/v1/score-lines')
    lines.value = res.data.data || []
  } finally {
    loading.value = false
  }
}

function handleEdit(row: any) {
  console.log('edit', row)
}

onMounted(fetchLines)
</script>
