<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">分数线配置</h2>
      <el-button v-if="can('score-line:create')" type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon> 新增分数线
      </el-button>
    </div>

    <el-card>
      <!-- 表格 -->
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="majorName" label="专业名称" min-width="160" />
        <el-table-column prop="year" label="年份" width="100" />
        <el-table-column prop="subject" label="科目" min-width="120" />
        <el-table-column prop="minScore" label="最低分" width="100" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button v-if="can('score-line:edit')" type="primary" link @click="openEdit(row)">编辑</el-button>
            <el-button v-if="can('score-line:delete')" type="danger" link @click="handleDelete(row)">删除</el-button>
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
        @size-change="fetchLines"
        @current-change="fetchLines"
      />
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑分数线' : '新增分数线'"
      width="500px"
      :close-on-click-modal="false"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="专业" prop="majorId">
          <el-select v-model="form.majorId" placeholder="请选择专业" style="width: 100%" filterable>
            <el-option v-for="m in majorList" :key="m.majorId" :label="m.majorName" :value="m.majorId" />
          </el-select>
        </el-form-item>
        <el-form-item label="年份" prop="year">
          <el-input-number v-model="form.year" :min="2000" :max="2100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="科目" prop="subject">
          <el-input v-model="form.subject" placeholder="请输入科目名称，如：文科综合" maxlength="50" />
        </el-form-item>
        <el-form-item label="最低分" prop="minScore">
          <el-input-number v-model="form.minScore" :min="0" :max="1000" :precision="1" style="width: 100%" />
        </el-form-item>
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
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import axios from '@/api/axios'
import { usePermission } from '@/composables/usePermission'
import { useAuthStore } from '@/stores/auth'

const { can } = usePermission()
const auth = useAuthStore()

// ========== 数据 ==========
const loading = ref(false)
const submitting = ref(false)
const list = ref<any[]>([])
const majorList = ref<any[]>([])
const total = ref(0)
const isEdit = ref(false)
const dialogVisible = ref(false)
const editingId = ref('')
const formRef = ref<FormInstance>()

const query = reactive({
  page: 1,
  pageSize: 20,
})

const form = reactive({
  majorId: '',
  year: new Date().getFullYear(),
  subject: '',
  minScore: 0,
})

const rules: FormRules = {
  majorId: [{ required: true, message: '请选择专业', trigger: 'change' }],
  year: [{ required: true, message: '请输入年份', trigger: 'blur' }],
  subject: [
    { required: true, message: '请输入科目名称', trigger: 'blur' },
    { max: 50, message: '科目名称最多50个字符', trigger: 'blur' },
  ],
  minScore: [{ required: true, message: '请输入最低分', trigger: 'blur' }],
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

async function fetchLines() {
  loading.value = true
  try {
    const res = await axios.get('/v1/score-lines', { params: { page: query.page, pageSize: query.pageSize } })
    list.value = res.data.data.records || []
    total.value = res.data.data.total || 0
  } catch {
    // error handled by axios interceptor
  } finally {
    loading.value = false
  }
}

function resetForm() {
  formRef.value?.resetFields()
  form.majorId = ''
  form.year = new Date().getFullYear()
  form.subject = ''
  form.minScore = 0
}

function openCreate() {
  isEdit.value = false
  dialogVisible.value = true
}

async function openEdit(row: any) {
  isEdit.value = true
  editingId.value = row.lineId
  Object.assign(form, {
    majorId: row.majorId,
    year: row.year,
    subject: row.subject,
    minScore: row.minScore,
  })
  dialogVisible.value = true
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value) {
      await axios.put(`/v1/score-lines/${editingId.value}`, { ...form })
      ElMessage.success('分数线更新成功')
    } else {
      await axios.post('/v1/score-lines', { ...form })
      ElMessage.success('分数线创建成功')
    }
    dialogVisible.value = false
    fetchLines()
  } catch {
    // error handled by axios interceptor
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(
      `确定要删除「${row.majorName}」${row.year}年「${row.subject}」分数线吗？`,
      '删除确认',
      { type: 'warning' }
    )
    await axios.delete(`/v1/score-lines/${row.lineId}`)
    ElMessage.success('删除成功')
    fetchLines()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// ========== 初始化 ==========
onMounted(() => {
  fetchMajors()
  fetchLines()
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
</style>
