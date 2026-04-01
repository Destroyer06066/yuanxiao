<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title" @dblclick="toggleHidden">系统参数配置</h2>
    </div>

    <el-card>
      <el-table :data="configList" v-loading="loading" stripe>
        <el-table-column prop="configKey" label="参数名称" width="220">
          <template #default="{ row }">
            <span class="config-key">{{ getConfigName(row.configKey) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="configValue" label="参数值" min-width="200">
          <template #default="{ row }">
            <template v-if="row.configKey === 'supplement_mode'">
              <el-select v-model="row.configValue" @change="handleSave(row)" style="width: 200px" disabled>
                <el-option value="MODE_1" label="模式一 - 考生重新推送" />
                <el-option value="MODE_2" label="模式二 - 院校主动邀请" />
              </el-select>
            </template>
            <template v-else-if="row.editType === 'number'">
              <el-input-number
                v-model="row.numberValue"
                :min="row.minValue || 0"
                :max="row.maxValue || 9999"
                @change="handleSave(row)"
              />
              <span v-if="row.unit" class="unit-suffix">{{ row.unit }}</span>
            </template>
            <template v-else-if="row.editType === 'text'">
              <el-input v-model="row.configValue" @blur="handleSave(row)" style="width: 200px" />
            </template>
            <template v-else>
              <span>{{ row.configValue }}</span>
            </template>
          </template>
        </el-table-column>
        <el-table-column label="说明" min-width="300">
          <template #default="{ row }">
            <span class="config-desc">{{ getConfigDescription(row.configKey) }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import axios from '@/api/axios'
import { updateGlobalConfig } from '@/api/paramConfig'

const loading = ref(false)
const allConfigs = ref<any[]>([])
const showHidden = ref(false)

// 过滤后的配置列表
const configList = computed(() => {
  return allConfigs.value.filter(c =>
    showHidden.value || !hiddenKeys.includes(c.configKey)
  )
})

// 隐藏的参数（双击标题显示）
const hiddenKeys = ['supplement_mode', 'default_admission_deadline']

// 双击标题切换显示隐藏参数
function toggleHidden() {
  showHidden.value = !showHidden.value
}

const configDescriptions: Record<string, string> = {
  supplement_mode: '二次补录工作模式：模式一由考生重新推送成绩，模式二由院校主动检索并邀请考生',
  invitation_default_days: '补录邀请发出后，考生未确认的有效天数，超过后邀请自动失效',
  max_staff_per_school: '每所院校可添加的，工作人员账号数量上限',
  default_admission_deadline: '录取通知默认截止时间，格式：yyyy-MM-dd HH:mm:ss',
  session_validity_hours: '用户登录后，会话保持有效的时长（小时），超时需重新登录',
  login_max_attempts: '连续登录失败次数达到此值后，账号将被锁定',
  lock_duration_minutes: '账号被锁定后，自动解锁所需的时长（分钟）',
  sms_code_ttl_seconds: '发送的短信验证码有效期（秒），超时后需重新获取',
  sms_daily_limit: '每个手机号码每天可接收短信验证码的最大次数',
  first_round_confirmation_days: '首轮录取时，考生未确认的有效天数，超过后录取通知自动失效',
  max_schools_per_candidate: '考生可推送成绩的院校数量上限（按自然年计算，每年重置），超过后不允许继续推送',
  score_validity_days: '考生成绩有效期（天），超过有效期平台不允许推送成绩'
}

const configNames: Record<string, string> = {
  supplement_mode: '补录模式',
  invitation_default_days: '邀请有效期',
  max_staff_per_school: '院校最大工作人员数',
  default_admission_deadline: '默认录取截止时间',
  session_validity_hours: '会话有效期',
  login_max_attempts: '登录最大尝试次数',
  lock_duration_minutes: '账号锁定时长',
  sms_code_ttl_seconds: '短信验证码有效期',
  sms_daily_limit: '每日短信上限',
  first_round_confirmation_days: '首轮录取确认有效期',
  max_schools_per_candidate: '考生可推送院校数量',
  score_validity_days: '成绩有效期'
}

const configMeta: Record<string, { editType: string; unit?: string; min?: number; max?: number }> = {
  supplement_mode: { editType: 'select' },
  invitation_default_days: { editType: 'number', unit: '天', min: 1, max: 365 },
  max_staff_per_school: { editType: 'number', unit: '人', min: 1, max: 1000 },
  default_admission_deadline: { editType: 'text' },
  session_validity_hours: { editType: 'number', unit: '小时', min: 1, max: 72 },
  login_max_attempts: { editType: 'number', unit: '次', min: 1, max: 20 },
  lock_duration_minutes: { editType: 'number', unit: '分钟', min: 1, max: 1440 },
  sms_code_ttl_seconds: { editType: 'number', unit: '秒', min: 60, max: 3600 },
  sms_daily_limit: { editType: 'number', unit: '条/天', min: 1, max: 100 },
  first_round_confirmation_days: { editType: 'number', unit: '天', min: 1, max: 30 },
  max_schools_per_candidate: { editType: 'number', unit: '所', min: 1, max: 20 },
  score_validity_days: { editType: 'number', unit: '天', min: 30, max: 730 }
}

function getConfigName(key: string): string {
  return configNames[key] || key
}

function getConfigDescription(key: string): string {
  return configDescriptions[key] || ''
}

async function fetchConfigs() {
  loading.value = true
  try {
    const res = await axios.get('/v1/admin/params/global')
    allConfigs.value = (res.data.data || []).map((c: any) => {
      const meta = configMeta[c.configKey] || {}
      const item: any = { ...c }
      if (meta.editType === 'number') {
        item.editType = 'number'
        item.numberValue = parseFloat(c.configValue) || 0
        item.minValue = meta.min
        item.maxValue = meta.max
        item.unit = meta.unit
      } else {
        item.editType = meta.editType || 'text'
      }
      return item
    })
  } catch (error) {
    ElMessage.error('获取配置失败')
  } finally {
    loading.value = false
  }
}

async function handleSave(row: any) {
  try {
    let value = row.configValue
    if (row.editType === 'number') {
      value = String(row.numberValue)
    }
    await updateGlobalConfig(row.configKey, value)
    row.configValue = value
    ElMessage.success('配置已更新')
  } catch (error) {
    ElMessage.error('配置更新失败')
    fetchConfigs()
  }
}

onMounted(() => {
  fetchConfigs()
})
</script>

<style scoped>
.config-key {
  color: #409eff;
  font-weight: 500;
}

.config-desc {
  color: #909399;
  font-size: 13px;
}

.unit-suffix {
  margin-left: 8px;
  color: #909399;
}
</style>
