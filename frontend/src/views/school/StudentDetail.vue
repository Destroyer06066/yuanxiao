<template>
  <div class="page-container">
    <el-page-header @back="$router.push('/students')" title="返回列表">
      <template #content>
        <span class="page-title">考生详情</span>
      </template>
    </el-page-header>

    <el-card v-loading="loading">
      <el-descriptions :column="2" border v-if="student">
        <el-descriptions-item label="考生编号">{{ student.candidateId }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ student.candidateName }}</el-descriptions-item>
        <el-descriptions-item label="国籍">{{ student.nationality }}</el-descriptions-item>
        <el-descriptions-item label="总分">{{ student.totalScore }}</el-descriptions-item>
        <el-descriptions-item label="意向方向">{{ student.intention || '未填写' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :class="'status-tag ' + student.status">{{ student.statusDesc }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getStudent } from '@/api/student'

const route = useRoute()
const loading = ref(false)
const student = ref<any>(null)

onMounted(async () => {
  loading.value = true
  try {
    const res = await getStudent(route.params.id as string)
    student.value = res.data.data
  } finally {
    loading.value = false
  }
})
</script>
