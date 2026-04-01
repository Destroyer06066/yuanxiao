<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">专业配置</h2>
      <el-button v-if="can('major:create')" type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon> 新增专业
      </el-button>
    </div>

    <el-card>
      <!-- 筛选条件 -->
      <div class="filter-row">
        <el-select v-model="query.status" placeholder="状态" style="width: 120px" clearable @change="doSearch">
          <el-option label="全部" value="" />
          <el-option label="启用" value="ACTIVE" />
          <el-option label="停用" value="INACTIVE" />
        </el-select>
        <el-button @click="doSearch">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </div>

      <!-- 表格 -->
      <el-table :data="list" v-loading="loading" stripe style="margin-top: 12px">
        <el-table-column prop="majorName" label="专业名称" min-width="160" />
        <el-table-column prop="degreeLevel" label="学位层次" width="120">
          <template #default="{ row }">
            {{ degreeLevelMap[row.degreeLevel] || row.degreeLevel }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" size="small">
              {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="can('major:edit')" type="primary" link @click="openEdit(row)">编辑</el-button>
            <el-button
              v-if="can('major:disable')"
              :type="row.status === 'ACTIVE' ? 'danger' : 'success'" link
              @click="handleToggle(row)"
            >
              {{ row.status === 'ACTIVE' ? '停用' : '启用' }}
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
        @size-change="fetchMajors"
        @current-change="fetchMajors"
      />
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑专业' : '新增专业'"
      width="500px"
      :close-on-click-modal="false"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="专业名称" prop="majorName">
          <el-input v-model="form.majorName" placeholder="请输入专业名称" maxlength="100" />
        </el-form-item>
        <el-form-item label="学位层次" prop="degreeLevel">
          <el-select v-model="form.degreeLevel" placeholder="请选择学位层次" style="width: 100%">
            <el-option label="本科" value="本科" />
            <el-option label="硕士" value="硕士" />
            <el-option label="博士" value="博士" />
          </el-select>
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

const { can } = usePermission()

const degreeLevelMap: Record<string, string> = {
  '本科': '本科',
  '硕士': '硕士',
  '博士': '博士',
}

// ========== 数据 ==========
const loading = ref(false)
const submitting = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const isEdit = ref(false)
const dialogVisible = ref(false)
const editingId = ref('')
const formRef = ref<FormInstance>()

const query = reactive({
  status: '',
  page: 1,
  pageSize: 20,
})

const form = reactive({
  majorName: '',
  degreeLevel: '',
})

const rules: FormRules = {
  majorName: [
    { required: true, message: '请输入专业名称', trigger: 'blur' },
    { max: 100, message: '专业名称最多100个字符', trigger: 'blur' },
  ],
  degreeLevel: [
    { required: true, message: '请选择学位层次', trigger: 'change' },
  ],
}

// ========== 接口 ==========
async function fetchMajors() {
  loading.value = true
  try {
    const params: any = { page: query.page, pageSize: query.pageSize }
    if (query.status) params.status = query.status
    const res = await axios.get('/v1/majors', { params })
    list.value = res.data.data.records || []
    total.value = res.data.data.total || 0
  } catch {
    // error handled by axios interceptor
  } finally {
    loading.value = false
  }
}

function doSearch() {
  query.page = 1
  fetchMajors()
}

function resetQuery() {
  query.status = ''
  query.page = 1
  fetchMajors()
}

// ========== 弹窗 ==========
function openCreate() {
  isEdit.value = false
  dialogVisible.value = true
}

async function openEdit(row: any) {
  isEdit.value = true
  editingId.value = row.majorId
  Object.assign(form, {
    majorName: row.majorName,
    degreeLevel: row.degreeLevel,
  })
  dialogVisible.value = true
}

function resetForm() {
  formRef.value?.resetFields()
  form.majorName = ''
  form.degreeLevel = ''
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value) {
      await axios.put(`/v1/majors/${editingId.value}`, { ...form })
      ElMessage.success('专业更新成功')
    } else {
      await axios.post('/v1/majors', { ...form })
      ElMessage.success('专业创建成功')
    }
    dialogVisible.value = false
    fetchMajors()
  } catch {
    // error handled by axios interceptor
  } finally {
    submitting.value = false
  }
}

async function handleToggle(row: any) {
  const action = row.status === 'ACTIVE' ? '停用' : '启用'
  try {
    await ElMessageBox.confirm(
      `确定要${action}专业「${row.majorName}」吗？`,
      `${action}确认`,
      { type: 'warning' }
    )
    const newStatus = row.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
    await axios.patch(`/v1/majors/${row.majorId}/status`, { status: newStatus })
    ElMessage.success(`${action}成功`)
    fetchMajors()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(`${action}失败`)
    }
  }
}

// ========== 初始化 ==========
onMounted(() => {
  fetchMajors()
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
.filter-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
