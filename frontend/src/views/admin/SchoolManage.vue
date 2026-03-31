<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">院校管理</h2>
      <el-button type="primary" @click="openCreateDialog">
        <el-icon><Plus /></el-icon> 新增院校
      </el-button>
    </div>

    <el-card>
      <!-- 筛选条件 -->
      <div class="filter-row">
        <el-input
          v-model="query.keyword"
          placeholder="搜索院校名称"
          style="width: 200px"
          clearable
          @clear="doSearch"
          @keyup.enter="doSearch"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="query.province" placeholder="省份" style="width: 140px" clearable @change="doSearch">
          <el-option v-for="p in provinces" :key="p" :label="p" :value="p" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" style="width: 120px" clearable @change="doSearch">
          <el-option label="启用" value="ACTIVE" />
          <el-option label="停用" value="INACTIVE" />
        </el-select>
        <el-button @click="doSearch">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </div>

      <!-- 表格 -->
      <el-table :data="list" v-loading="loading" stripe style="margin-top: 12px">
        <el-table-column prop="schoolName" label="院校全称" min-width="160" />
        <el-table-column prop="schoolShortName" label="简称" width="100" />
        <el-table-column prop="province" label="省份" width="100" />
        <el-table-column prop="schoolType" label="类型" width="100" />
        <el-table-column prop="contactName" label="联系人" width="100" />
        <el-table-column prop="contactPhone" label="联系电话" width="130" />
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" size="small">
              {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEditDialog(row)">编辑</el-button>
            <el-button
              :type="row.status === 'ACTIVE' ? 'danger' : 'success'" link
              @click="handleToggleStatus(row)"
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
        @size-change="fetchSchools"
        @current-change="fetchSchools"
      />
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑院校' : '新增院校'"
      width="560px"
      :close-on-click-modal="false"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <!-- 基本信息 -->
        <div class="form-group-title">基本信息</div>
        <el-form-item label="院校全称" prop="schoolName">
          <el-input v-model="form.schoolName" placeholder="请输入院校名称" maxlength="100" />
        </el-form-item>
        <el-form-item label="院校简称" prop="schoolShortName">
          <el-input v-model="form.schoolShortName" placeholder="请输入院校简称" maxlength="20" />
        </el-form-item>
        <el-form-item label="所在省份" prop="province">
          <el-select v-model="form.province" placeholder="请选择省份" style="width: 100%">
            <el-option v-for="p in provinces" :key="p" :label="p" :value="p" />
          </el-select>
        </el-form-item>
        <el-form-item label="院校类型" prop="schoolType">
          <el-select v-model="form.schoolType" placeholder="请选择类型" style="width: 100%">
            <el-option label="综合类" value="综合类" />
            <el-option label="理工类" value="理工类" />
            <el-option label="文史类" value="文史类" />
            <el-option label="艺术类" value="艺术类" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>

        <!-- 联系信息 -->
        <div class="form-group-title">联系信息</div>
        <el-form-item label="联系人" prop="contactName">
          <el-input v-model="form.contactName" placeholder="请输入联系人姓名" maxlength="50" />
        </el-form-item>
        <el-form-item label="手机号" prop="contactPhone">
          <el-input v-model="form.contactPhone" placeholder="请输入联系人手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="邮箱" prop="contactEmail">
          <el-input v-model="form.contactEmail" placeholder="请输入联系人邮箱" />
        </el-form-item>

        <!-- 选填信息 -->
        <div class="form-group-title">选填信息</div>
        <el-form-item label="官网" prop="website">
          <el-input v-model="form.website" placeholder="请输入院校官网" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="选填" maxlength="500" />
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
import { Plus, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  getSchoolById,
  getSchools,
  createSchool,
  updateSchool,
  toggleSchoolStatus,
  type School,
  type CreateSchoolRequest,
} from '@/api/school'

// ========== 数据 ==========
const loading = ref(false)
const submitting = ref(false)
const list = ref<School[]>([])
const total = ref(0)
const isEdit = ref(false)
const dialogVisible = ref(false)
const editingId = ref('')
const formRef = ref<FormInstance>()

const query = reactive({
  keyword: '',
  province: '',
  status: 'ACTIVE',
  page: 1,
  pageSize: 20,
})

const form = reactive<CreateSchoolRequest>({
  schoolName: '',
  schoolShortName: '',
  province: '',
  schoolType: '',
  contactName: '',
  contactPhone: '',
  contactEmail: '',
  website: '',
  remark: '',
})

const rules: FormRules = {
  schoolName: [
    { required: true, message: '请输入院校名称', trigger: 'blur' },
    { max: 100, message: '院校名称最多100个字符', trigger: 'blur' },
  ],
  schoolShortName: [
    { required: true, message: '请输入院校简称', trigger: 'blur' },
    { max: 20, message: '简称最多20个字符', trigger: 'blur' },
  ],
  province: [{ required: true, message: '请输入所在省份', trigger: 'blur' }],
  schoolType: [{ required: true, message: '请选择院校类型', trigger: 'change' }],
  contactName: [
    { required: true, message: '请输入联系人', trigger: 'blur' },
    { max: 50, message: '联系人姓名最多50个字符', trigger: 'blur' },
  ],
  contactPhone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' },
  ],
  contactEmail: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' },
  ],
}

// ========== 常用省份 ==========
const provinces = [
  '北京', '天津', '河北', '山西', '内蒙古',
  '辽宁', '吉林', '黑龙江',
  '上海', '江苏', '浙江', '安徽', '福建', '江西', '山东',
  '河南', '湖北', '湖南', '广东', '广西', '海南',
  '重庆', '四川', '贵州', '云南', '西藏',
  '陕西', '甘肃', '青海', '宁夏', '新疆',
]

// ========== 接口 ==========
async function fetchSchools() {
  loading.value = true
  try {
    const params: any = { page: query.page, pageSize: query.pageSize }
    if (query.keyword) params.keyword = query.keyword
    if (query.province) params.province = query.province
    if (query.status) params.status = query.status
    const res = await getSchools(params)
    list.value = res.data.data.records
    total.value = res.data.data.total
  } catch {
    // error handled by axios interceptor
  } finally {
    loading.value = false
  }
}

function doSearch() {
  query.page = 1
  fetchSchools()
}

function resetQuery() {
  query.keyword = ''
  query.province = ''
  query.status = 'ACTIVE'
  query.page = 1
  fetchSchools()
}

// ========== 弹窗 ==========
function openCreateDialog() {
  isEdit.value = false
  dialogVisible.value = true
}

async function openEditDialog(row: School) {
  isEdit.value = true
  editingId.value = row.schoolId
  // Fetch fresh data from backend to ensure reactive form has latest values
  try {
    const res = await getSchoolById(row.schoolId)
    Object.assign(form, {
      schoolName: res.data.data.schoolName,
      schoolShortName: res.data.data.schoolShortName,
      province: res.data.data.province,
      schoolType: res.data.data.schoolType,
      contactName: res.data.data.contactName,
      contactPhone: res.data.data.contactPhone,
      contactEmail: res.data.data.contactEmail,
      website: res.data.data.website || '',
      remark: res.data.data.remark || '',
    })
  } catch {
    ElMessage.error('加载院校信息失败')
    return
  }
  dialogVisible.value = true
}

function resetForm() {
  formRef.value?.resetFields()
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value) {
      await updateSchool(editingId.value, { ...form })
      ElMessage.success('院校更新成功')
    } else {
      await createSchool({ ...form })
      ElMessage.success('院校创建成功')
    }
    dialogVisible.value = false
    fetchSchools()
  } catch {
    // error handled by axios interceptor
  } finally {
    submitting.value = false
  }
}

async function handleToggleStatus(row: School) {
  const action = row.status === 'ACTIVE' ? '停用' : '启用'
  try {
    await ElMessageBox.confirm(
      `确定要${action}院校「${row.schoolName}」吗？`,
      `${action}确认`,
      { type: 'warning' }
    )
    const newStatus = row.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
    await toggleSchoolStatus(row.schoolId, newStatus)
    ElMessage.success(`${action}成功`)
    fetchSchools()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(`${action}失败`)
    }
  }
}

// ========== 初始化 ==========
onMounted(() => {
  fetchSchools()
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
  .form-group-title {
    font-size: 14px;
    font-weight: 600;
    color: #303133;
    margin: 12px 0 8px;
    padding-left: 8px;
    border-left: 3px solid #409eff;
  }
  .form-group-title:first-child {
    margin-top: 0;
  }
.filter-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
