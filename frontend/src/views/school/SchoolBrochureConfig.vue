<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">招生简章配置</h2>
    </div>
    <el-card>
      <!-- 只读预览模式 -->
      <div v-if="!editing">
        <el-alert v-if="!brochure.title" title="暂无招生简章" type="info" :closable="false" show-icon />
        <div v-else>
          <h3>{{ brochure.title }}</h3>
          <div v-html="brochure.content" class="content-preview"></div>
        </div>
        <el-button type="primary" style="margin-top: 16px" @click="startEdit">
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
import { ref, reactive, onMounted } from 'vue'
import { QuillEditor } from '@vueup/vue-quill'
import '@vueup/vue-quill/dist/vue-quill.snow.css'
import { ElMessage } from 'element-plus'
import axios from '@/api/axios'

const editing = ref(false)
const saving = ref(false)
const brochure = reactive({ brochureId: '', title: '', content: '' })
const form = reactive({ title: '', content: '' })

async function fetchBrochure() {
  const res = await axios.get('/v1/brochures')
  const data = res.data.data
  brochure.brochureId = data.brochureId || ''
  brochure.title = data.title || ''
  brochure.content = data.content || ''
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

onMounted(() => { fetchBrochure() })
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
.content-preview {
  line-height: 1.8;
  color: #303133;
}
:deep(.ql-editor) {
  height: 350px;
}
</style>
