<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">招生简章配置</h2>
    </div>

    <!-- OP_ADMIN 模式：院校筛选 -->
    <el-card v-if="auth.isOpAdmin" style="margin-bottom: 16px">
      <div class="filter-row">
        <el-select v-model="querySchoolId" placeholder="选择院校" style="width: 280px" clearable filterable @change="onSchoolChange">
          <el-option v-for="s in schoolList" :key="s.schoolId" :label="s.schoolName" :value="s.schoolId" />
        </el-select>
      </div>
    </el-card>

    <el-card>
      <!-- 只读预览模式 -->
      <div v-if="!editing">
        <el-alert v-if="!brochure.title" title="暂无招生简章" type="info" :closable="false" show-icon />
        <div v-else>
          <div class="brochure-header">
            <h3>{{ brochure.title }}</h3>
            <span v-if="auth.isOpAdmin && selectedSchoolName" class="school-badge">{{ selectedSchoolName }}</span>
          </div>
          <div v-html="brochure.content" class="content-preview"></div>
        </div>
        <el-button v-if="!auth.isOpAdmin" type="primary" style="margin-top: 16px" @click="startEdit">
          {{ brochure.title ? '编辑简章' : '创建简章' }}
        </el-button>
      </div>
      <!-- 编辑模式 -->
      <div v-else>
        <el-form :model="form" label-width="100px">
          <el-form-item label="简章标题">
            <el-input v-model="form.title" placeholder="请输入简章标题" maxlength="200" />
          </el-form-item>
          <el-form-item label="简章内容">
            <QuillEditor v-model:content="form.content" contentType="html" theme="snow" style="height: 400px" />
          </el-form-item>
        </el-form>
        <div style="margin-top: 16px; text-align: right">
          <el-button @click="cancelEdit">取消</el-button>
          <el-button type="primary" :loading="saving" @click="save">保存</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { QuillEditor } from '@vueup/vue-quill'
import '@vueup/vue-quill/dist/vue-quill.snow.css'
import { ElMessage } from 'element-plus'
import axios from '@/api/axios'
import { useAuthStore } from '@/stores/auth'
import { getSchools } from '@/api/school'

const auth = useAuthStore()

const editing = ref(false)
const saving = ref(false)
const schoolList = ref<any[]>([])
const querySchoolId = ref('')
const brochure = reactive({ brochureId: '', schoolId: '', title: '', content: '' })
const form = reactive({ title: '', content: '' })

const selectedSchoolName = computed(() => {
  const school = schoolList.value.find(s => s.schoolId === querySchoolId.value)
  return school ? school.schoolName : ''
})

async function fetchSchools() {
  try {
    const res = await getSchools({ page: 1, pageSize: 200 })
    schoolList.value = res.data.data.records || []
  } catch {
    // error handled by axios interceptor
  }
}

async function fetchBrochure() {
  const params: any = {}
  if (auth.isOpAdmin && querySchoolId.value) {
    params.schoolId = querySchoolId.value
  }
  const res = await axios.get('/v1/brochures', { params })
  const data = res.data.data
  brochure.brochureId = data.brochureId || ''
  brochure.schoolId = data.schoolId || ''
  brochure.title = data.title || ''
  brochure.content = data.content || ''
}

function onSchoolChange() {
  brochure.brochureId = ''
  brochure.schoolId = ''
  brochure.title = ''
  brochure.content = ''
  fetchBrochure()
}

function startEdit() {
  form.title = brochure.title
  form.content = brochure.content
  editing.value = true
}

function cancelEdit() {
  editing.value = false
}

async function save() {
  if (!form.title.trim()) {
    ElMessage.warning('请输入简章标题')
    return
  }
  saving.value = true
  try {
    await axios.put('/v1/brochures', { title: form.title, content: form.content })
    Object.assign(brochure, form)
    editing.value = false
    ElMessage.success('保存成功')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  if (auth.isOpAdmin) {
    fetchSchools()
  }
  fetchBrochure()
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
}
.brochure-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.brochure-header h3 {
  margin: 0;
}
.school-badge {
  background-color: #409eff;
  color: white;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}
.content-preview {
  line-height: 1.8;
  color: #303133;
}
:deep(.ql-editor) {
  height: 350px;
}
</style>
