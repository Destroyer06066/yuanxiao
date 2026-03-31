<template>
  <div class="page-container">
    <div class="page-header">
      <el-page-header @back="$router.push('/students')" title="返回列表">
        <template #content>
          <span class="page-title">考生详情</span>
        </template>
      </el-page-header>
    </div>

    <div v-loading="loading">
      <!-- 基本信息 + 操作面板 -->
      <el-card class="info-card">
        <div class="info-layout">
          <!-- 基本信息 -->
          <div class="info-section">
            <h4 class="section-title">基本信息</h4>
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="考生编号">{{ student?.candidateId || '-' }}</el-descriptions-item>
              <el-descriptions-item label="姓名">{{ student?.candidateName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="国籍">{{ student?.nationality || '-' }}</el-descriptions-item>
              <el-descriptions-item label="证件号">{{ student?.idNumber || '-' }}</el-descriptions-item>
              <el-descriptions-item label="邮箱">{{ student?.email || '-' }}</el-descriptions-item>
              <el-descriptions-item label="意向方向">{{ student?.intention || '-' }}</el-descriptions-item>
              <el-descriptions-item label="推送时间">{{ student?.pushedAt ? formatTime(student.pushedAt) : '-' }}</el-descriptions-item>
              <el-descriptions-item label="推送院校" v-if="student?.schoolName">{{ student.schoolName }}</el-descriptions-item>
            </el-descriptions>

            <!-- 状态标签 -->
            <div class="status-row">
              <el-tag :class="'status-tag ' + student?.status" size="large">
                {{ statusLabelMap[student?.status] || student?.status }}
              </el-tag>
              <span v-if="student?.pushRound && student.pushRound > 0" class="round-tag">第 {{ student.pushRound }} 轮补录</span>
            </div>
          </div>

          <!-- 操作面板（非 OP_ADMIN 且当前状态可操作时显示） -->
          <div class="action-section" v-if="!authStore.isOpAdmin && canOperate">
            <h4 class="section-title">录取操作</h4>

            <!-- PENDING 状态操作 -->
            <template v-if="student?.status === 'PENDING'">
              <el-button-group class="action-btn-group">
                <el-button type="success" @click="openAdmitDialog">
                  直接录取
                </el-button>
                <el-button type="warning" @click="openConditionalDialog">
                  有条件录取
                </el-button>
              </el-button-group>
              <el-button type="danger" class="action-btn" @click="openRejectDialog">
                拒绝
              </el-button>
            </template>

            <!-- CONDITIONAL 状态操作 -->
            <template v-if="student?.status === 'CONDITIONAL'">
              <el-alert
                v-if="student.conditionDeadline"
                :title="'条件截止日期: ' + formatDate(student.conditionDeadline)"
                type="warning"
                :closable="false"
                show-icon
                style="margin-bottom: 12px"
              />
              <p class="condition-text">条件：{{ student.conditionDesc }}</p>
              <el-button type="success" class="action-btn" @click="handleFinalize">
                确认条件满足 → 转正式录取
              </el-button>
              <el-button type="info" class="action-btn" @click="handleRevoke">
                撤销条件录取
              </el-button>
            </template>

            <!-- CONFIRMED 状态操作 -->
            <template v-if="student?.status === 'CONFIRMED'">
              <p class="status-hint">考生已确认录取，等待材料收件</p>
              <el-button type="primary" class="action-btn" @click="openMaterialDialog">
                登记材料收件
              </el-button>
            </template>

            <!-- MATERIAL_RECEIVED 状态操作 -->
            <template v-if="student?.status === 'MATERIAL_RECEIVED'">
              <p class="status-hint">材料已收件，等待考生报到</p>
              <el-button type="success" class="action-btn" @click="openCheckinDialog">
                确认报到
              </el-button>
            </template>

            <!-- ADMITTED 状态提示 -->
            <template v-if="student?.status === 'ADMITTED'">
              <el-alert
                title="等待考生在报名平台确认录取"
                type="info"
                :closable="false"
                show-icon
              />
            </template>

            <!-- 已终态 -->
            <template v-if="['CHECKED_IN', 'REJECTED', 'INVALIDATED'].includes(student?.status || '')">
              <el-alert
                :title="statusFinalText"
                type="info"
                :closable="false"
                show-icon
              />
            </template>
          </div>

          <!-- OP_ADMIN 视图 -->
          <div class="action-section" v-if="authStore.isOpAdmin">
            <h4 class="section-title">查看模式</h4>
            <el-alert
              title="运营管理员仅可查看考生详情，招生操作（录取/有条件录取/拒绝）由各院校管理员负责"
              type="info"
              :closable="false"
              show-icon
            />
          </div>
        </div>
      </el-card>

      <!-- 分科成绩 -->
      <el-card class="info-card" v-if="subjectColumns.length > 0">
        <h4 class="section-title">分科成绩</h4>
        <el-table :data="[student]" stripe size="small">
          <el-table-column
            v-for="col in subjectColumns"
            :key="col.prop"
            :label="col.label"
            width="120"
            align="center"
          >
            <template #default="{ row }">
              {{ row.subjectScores?.[col.prop] ?? '-' }}
            </template>
          </el-table-column>
          <el-table-column label="总分" width="100" align="center">
            <template #default>
              <strong>{{ student?.totalScore }}</strong>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 录取信息（已录取时显示） -->
      <el-card class="info-card" v-if="student?.status !== 'PENDING' && student?.status !== 'INVALIDATED'">
        <h4 class="section-title">录取信息</h4>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="录取专业">{{ student?.admissionMajor || '-' }}</el-descriptions-item>
          <el-descriptions-item label="录取备注">{{ student?.admissionRemark || '-' }}</el-descriptions-item>
          <el-descriptions-item label="条件描述" v-if="student?.conditionDesc">
            <span class="condition-text">{{ student.conditionDesc }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="条件截止日" v-if="student?.conditionDeadline">
            <el-tag type="warning" size="small">{{ formatDate(student.conditionDeadline) }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 操作时间线 -->
      <el-card class="info-card">
        <h4 class="section-title">操作时间线</h4>
        <el-timeline v-if="timeline.length > 0" size="large">
          <el-timeline-item
            v-for="item in timeline"
            :key="item.logId"
            :timestamp="formatTime(item.createdAt)"
            :type="getActionType(item.action)"
          >
            <div class="timeline-content">
              <span class="action-badge" :class="'action-' + item.action.toLowerCase()">
                {{ actionLabel(item.action) }}
              </span>
              <span v-if="item.operatorName" class="operator-name">by {{ item.operatorName }}</span>
              <p v-if="item.remark" class="timeline-remark">{{ item.remark }}</p>
            </div>
          </el-timeline-item>
        </el-timeline>
        <EmptyState
          v-else
          title="暂无操作记录"
          description="对该考生的录取、拒绝等操作会显示在时间线上"
          icon="Clock"
          size="medium"
        />
      </el-card>
    </div>

    <!-- 直接录取对话框 -->
    <el-dialog v-model="admitDialogVisible" title="直接录取" width="500px">
      <el-form :model="admitForm" label-width="80px">
        <el-form-item label="录取专业" required>
          <el-select v-model="admitForm.majorId" placeholder="请选择专业" class="full-width">
            <el-option v-for="m in majorOptions" :key="m.majorId"
              :label="`${m.majorName}（剩余${m.remainQuota}名额）`" :value="m.majorId" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="admitForm.remark" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="admitDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleAdmit">确认录取</el-button>
      </template>
    </el-dialog>

    <!-- 有条件录取对话框 -->
    <el-dialog v-model="conditionalDialogVisible" title="有条件录取" width="520px">
      <el-form :model="condForm" label-width="90px">
        <el-form-item label="意向专业" required>
          <el-select v-model="condForm.majorId" placeholder="请选择专业" class="full-width">
            <el-option v-for="m in majorOptions" :key="m.majorId" :label="m.majorName" :value="m.majorId" />
          </el-select>
        </el-form-item>
        <el-form-item label="录取条件" required>
          <el-input v-model="condForm.conditionDesc" type="textarea" :rows="3" maxlength="1000" show-word-limit
            placeholder="请描述考生需满足的条件" />
        </el-form-item>
        <el-form-item label="条件截止日期" required>
          <el-date-picker v-model="condForm.conditionDeadline" type="date"
            value-format="YYYY-MM-DD" placeholder="选择日期" :disabled-date="(d: Date) => d <= new Date()" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="conditionalDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleConditional">确认</el-button>
      </template>
    </el-dialog>

    <!-- 拒绝对话框 -->
    <el-dialog v-model="rejectDialogVisible" title="拒绝录取" width="420px">
      <el-form :model="rejectForm" label-width="70px">
        <el-form-item label="拒绝原因">
          <el-input v-model="rejectForm.reason" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="submitting" @click="handleReject">确认拒绝</el-button>
      </template>
    </el-dialog>

    <!-- 材料收件对话框 -->
    <el-dialog v-model="materialDialogVisible" title="登记材料收件" width="420px">
      <el-form :model="materialForm" label-width="80px">
        <el-form-item label="备注">
          <el-input v-model="materialForm.note" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="可选备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="materialDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleMaterial">确认收件</el-button>
      </template>
    </el-dialog>

    <!-- 确认报到对话框 -->
    <el-dialog v-model="checkinDialogVisible" title="确认报到" width="420px">
      <el-form :model="checkinForm" label-width="80px">
        <el-form-item label="备注">
          <el-input v-model="checkinForm.note" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="可选备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="checkinDialogVisible = false">取消</el-button>
        <el-button type="success" :loading="submitting" @click="handleCheckin">确认报到</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import EmptyState from '@/components/EmptyState.vue'
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getStudent, directAdmission, conditionalAdmission, batchReject,
         finalizeAdmission, revokeAdmission,
         getTimeline, receiveMaterial, confirmCheckin } from '@/api/student'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const student = ref<any>(null)
const timeline = ref<any[]>([])
const majorOptions = ref<any[]>([])
const submitting = ref(false)

const statusLabelMap: Record<string, string> = {
  PENDING: '待处理',
  CONDITIONAL: '有条件录取中',
  ADMITTED: '已录取（待确认）',
  CONFIRMED: '已确认',
  MATERIAL_RECEIVED: '材料已收',
  CHECKED_IN: '已报到',
  REJECTED: '已拒绝',
  INVALIDATED: '录取已失效',
}

const canOperate = computed(() =>
  ['PENDING', 'CONDITIONAL', 'CONFIRMED', 'MATERIAL_RECEIVED', 'ADMITTED'].includes(student.value?.status || ''))

const statusFinalText = computed(() => {
  const map: Record<string, string> = {
    CHECKED_IN: '考生已报到，流程结束',
    REJECTED: '考生已被拒绝',
    INVALIDATED: '录取已失效（被其他院校确认）',
  }
  return map[student.value?.status] || ''
})

const subjectColumns = computed(() => {
  if (!student.value?.subjectScores) return []
  return Object.entries(student.value.subjectScores).map(([subject, score]) => ({
    label: subject,
    prop: subject,
    value: score,
  }))
})

function formatTime(ts: string) {
  return ts ? dayjs(ts).format('YYYY-MM-DD HH:mm') : '-'
}

function formatDate(d: string) {
  return d ? dayjs(d).format('YYYY-MM-DD') : '-'
}

function actionLabel(action: string) {
  const map: Record<string, string> = {
    PUSH: '推送',
    ADMIT: '直接录取',
    CONDITIONAL: '有条件录取',
    FINALIZE: '终裁录取',
    REVOKE: '撤销录取',
    REJECT: '拒绝',
    CONFIRM: '确认录取',
    MATERIAL_RECEIVE: '材料收件',
    CHECKIN: '确认报到',
    INVALIDATE: '录取失效',
    CONDITION_EXPIRED: '条件到期',
  }
  return map[action] || action
}

function getActionIcon(action: string) {
  const map: Record<string, string> = {
    PUSH: 'Collection',
    ADMIT: 'Check',
    CONDITIONAL: 'Warning',
    FINALIZE: 'CircleCheckFilled',
    REVOKE: 'Close',
    REJECT: 'CloseBold',
    CONFIRM: 'CircleCheck',
    MATERIAL_RECEIVE: 'Box',
    CHECKIN: 'LocationFilled',
    INVALIDATE: 'RemoveFilled',
    CONDITION_EXPIRED: 'Clock',
  }
  return map[action] || 'Document'
}

function getActionType(action: string) {
  const map: Record<string, string> = {
    PUSH: 'primary',
    ADMIT: 'success',
    CONDITIONAL: 'warning',
    FINALIZE: 'success',
    REVOKE: 'info',
    REJECT: 'danger',
    CONFIRM: 'success',
    MATERIAL_RECEIVE: 'primary',
    CHECKIN: 'success',
    INVALIDATE: 'danger',
    CONDITION_EXPIRED: 'warning',
  }
  return map[action] || 'primary'
}

// 操作表单
const admitDialogVisible = ref(false)
const admitForm = { majorId: '', remark: '' }
const conditionalDialogVisible = ref(false)
const condForm = { majorId: '', conditionDesc: '', conditionDeadline: '' }
const rejectDialogVisible = ref(false)
const rejectForm = { reason: '' }
const materialDialogVisible = ref(false)
const materialForm = { note: '' }
const checkinDialogVisible = ref(false)
const checkinForm = { note: '' }

function openAdmitDialog() {
  admitForm.majorId = ''
  admitForm.remark = ''
  admitDialogVisible.value = true
}

function openConditionalDialog() {
  condForm.majorId = ''
  condForm.conditionDesc = ''
  condForm.conditionDeadline = ''
  conditionalDialogVisible.value = true
}

function openRejectDialog() {
  rejectForm.reason = ''
  rejectDialogVisible.value = true
}

function openMaterialDialog() {
  materialForm.note = ''
  materialDialogVisible.value = true
}

function openCheckinDialog() {
  checkinForm.note = ''
  checkinDialogVisible.value = true
}

async function handleAdmit() {
  if (!admitForm.majorId) { ElMessage.warning('请选择录取专业'); return }
  submitting.value = true
  try {
    await directAdmission({ pushId: student.value.pushId, majorId: admitForm.majorId, remark: admitForm.remark })
    ElMessage.success('录取成功')
    admitDialogVisible.value = false
    await reload()
  } finally { submitting.value = false }
}

async function handleConditional() {
  if (!condForm.majorId || !condForm.conditionDesc || !condForm.conditionDeadline) {
    ElMessage.warning('请填写完整信息'); return
  }
  submitting.value = true
  try {
    await conditionalAdmission({ pushId: student.value.pushId, majorId: condForm.majorId,
      conditionDesc: condForm.conditionDesc, conditionDeadline: condForm.conditionDeadline })
    ElMessage.success('有条件录取成功')
    conditionalDialogVisible.value = false
    await reload()
  } finally { submitting.value = false }
}

async function handleReject() {
  submitting.value = true
  try {
    await batchReject([student.value.pushId])
    ElMessage.success('已拒绝')
    rejectDialogVisible.value = false
    await reload()
  } finally { submitting.value = false }
}

async function handleFinalize() {
  await ElMessageBox.confirm('确认考生条件已满足，转为正式录取？', '确认操作')
  submitting.value = true
  try {
    await finalizeAdmission(student.value.pushId)
    ElMessage.success('已转为正式录取')
    await reload()
  } finally { submitting.value = false }
}

async function handleRevoke() {
  await ElMessageBox.confirm('确认撤销该考生的条件录取？撤销后将恢复为待处理状态', '撤销操作')
  submitting.value = true
  try {
    await revokeAdmission(student.value.pushId)
    ElMessage.success('已撤销')
    await reload()
  } finally { submitting.value = false }
}

async function handleMaterial() {
  submitting.value = true
  try {
    await receiveMaterial(student.value.pushId, materialForm.note || undefined)
    ElMessage.success('材料收件已登记')
    materialDialogVisible.value = false
    await reload()
  } finally { submitting.value = false }
}

async function handleCheckin() {
  submitting.value = true
  try {
    await confirmCheckin(student.value.pushId, checkinForm.note || undefined)
    ElMessage.success('报到已确认')
    checkinDialogVisible.value = false
    await reload()
  } finally { submitting.value = false }
}

async function reload() {
  const pushId = route.params.id as string
  const [sRes, tRes] = await Promise.all([
    getStudent(pushId),
    getTimeline(pushId),
  ])
  student.value = sRes.data.data
  timeline.value = tRes.data.data || []
}

onMounted(reload)
</script>

<style scoped lang="scss">
.page-header { margin-bottom: 16px; }
.info-card { margin-bottom: 16px; }

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}

.info-layout {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 32px;
  align-items: start;
}

.action-section {
  min-width: 220px;
  border-left: 1px solid #ebeef5;
  padding-left: 24px;
}

.action-btn-group {
  display: flex;
  width: 100%;
  margin-bottom: 8px;
  :deep(.el-button) {
    flex: 1;
  }
}

.action-btn {
  display: block;
  width: 100%;
  margin-bottom: 8px;
}

.status-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
}

.round-tag {
  font-size: 12px;
  color: #909399;
}

.condition-text {
  font-size: 13px;
  color: #e6a23c;
  margin: 8px 0;
}

.status-hint {
  font-size: 13px;
  color: #909399;
  margin-bottom: 12px;
}

.timeline-content {
  line-height: 1.6;
}

.action-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
  margin-right: 8px;
}

.action-push { background: #ecf5ff; color: #409eff; }
.action-admit, .action-finalize, .action-confirm, .action-checkin { background: #f0f9eb; color: #67c23a; }
.action-conditional, .action-condition_expired { background: #fdf6ec; color: #e6a23c; }
.action-revoke, .action-invalidate { background: #f4f4f5; color: #909399; }
.action-reject { background: #fef0f0; color: #f56c6c; }
.action-material_receive { background: #ecf5ff; color: #409eff; }

.operator-name {
  font-size: 12px;
  color: #909399;
  margin-left: 8px;
}

.timeline-remark {
  font-size: 12px;
  color: #606266;
  margin: 4px 0 0 0;
}

.full-width { width: 100%; }
</style>
