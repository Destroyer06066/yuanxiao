<template>
  <div class="force-pwd-page">
    <el-card class="pwd-card">
      <template #header>
        <div class="card-header">
          <el-icon size="32" color="#E6A23C"><Lock /></el-icon>
          <h2>首次登录 · 修改密码</h2>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="form.newPassword"
            type="password"
            show-password
            placeholder="8-20位，须包含大小写字母和数字"
            size="large"
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            show-password
            placeholder="再次输入新密码"
            size="large"
          />
        </el-form-item>
        <el-button
          type="primary"
          size="large"
          :loading="loading"
          class="submit-btn"
          @click="handleSubmit"
        >
          确认修改
        </el-button>
      </el-form>

      <div class="pwd-rules">
        <ul>
          <li :class="{ ok: hasUpper }">至少一个大写字母</li>
          <li :class="{ ok: hasLower }">至少一个小写字母</li>
          <li :class="{ ok: hasNumber }">至少一个数字</li>
          <li :class="{ ok: hasLength }">8-20 位字符</li>
        </ul>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { forceChangePassword } from '@/api/auth'

const router = useRouter()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  newPassword: '',
  confirmPassword: '',
})

const hasUpper = computed(() => /[A-Z]/.test(form.newPassword))
const hasLower = computed(() => /[a-z]/.test(form.newPassword))
const hasNumber = computed(() => /\d/.test(form.newPassword))
const hasLength = computed(() => form.newPassword.length >= 8 && form.newPassword.length <= 20)

const validatePassword = (_rule: any, value: string, callback: any) => {
  if (!value) callback(new Error('请输入新密码'))
  else if (!hasUpper.value) callback(new Error('须包含大写字母'))
  else if (!hasLower.value) callback(new Error('须包含小写字母'))
  else if (!hasNumber.value) callback(new Error('须包含数字'))
  else callback()
}

const validateConfirm = (_rule: any, value: string, callback: any) => {
  if (value !== form.newPassword) callback(new Error('两次密码不一致'))
  else callback()
}

const rules = {
  newPassword: [{ validator: validatePassword, trigger: 'blur' }],
  confirmPassword: [{ validator: validateConfirm, trigger: 'blur' }],
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await forceChangePassword(form.newPassword)
    ElMessage.success('密码修改成功，请重新登录')
    router.push({ name: 'Login' })
  } catch (e) {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.force-pwd-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.pwd-card {
  width: 420px;

  .card-header {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 12px;
    padding: 8px 0;

    h2 { font-size: 18px; font-weight: 600; }
  }

  .submit-btn { width: 100%; margin-top: 8px; height: 44px; }
}

.pwd-rules {
  margin-top: 16px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 8px;

  ul {
    list-style: none;
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 8px;
    font-size: 13px;
    color: #c0c4cc;

    li { display: flex; align-items: center; gap: 6px; }
    li::before { content: '✗'; color: #f56c6c; }
    li.ok { color: #67c23a; }
    li.ok::before { content: '✓'; color: #67c23a; }
  }
}
</style>
