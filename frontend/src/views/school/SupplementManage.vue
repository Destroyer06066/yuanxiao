<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">补录管理</h2>
    </div>

    <!-- 模式二：院校主动邀请（模式二额外功能） -->
    <template v-if="isMode2">
      <el-alert type="info" :closable="false" style="margin-bottom: 20px">
        <template #title>
          当前已启用「院校主动邀请」模式。您可以设置条件检索符合补录条件的考生，并发送邀请。
        </template>
      </el-alert>

      <!-- 模式二Tab切换 -->
      <el-tabs v-model="mode2ActiveTab" class="mode2-tabs">
        <!-- Tab1: 检索记录 -->
        <el-tab-pane label="检索记录" name="search">
          <!-- 检索条件 -->
          <el-card style="margin-bottom: 16px">
            <div class="filter-section">
              <el-row :gutter="16">
                <el-col :span="8">
                  <el-form-item label="最低分数">
                    <el-input-number v-model="filter.minScore" :min="0" :max="750" placeholder="总分" />
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="最高分数">
                    <el-input-number v-model="filter.maxScore" :min="0" :max="750" placeholder="总分" />
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="专业">
                    <el-select v-model="filter.majorId" placeholder="请选择专业" clearable filterable style="width: 100%">
                      <el-option v-for="m in majorList" :key="m.majorId" :label="m.majorName" :value="m.majorId" />
                    </el-select>
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="16">
                <el-col :span="8">
                  <el-form-item label="意向关键词">
                    <el-input v-model="filter.intentionKeyword" placeholder="专业意向关键词" clearable />
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="状态">
                    <el-select v-model="filter.status" placeholder="考生状态" clearable multiple style="width: 100%">
                      <el-option value="PENDING" label="待处理" />
                      <el-option value="REJECTED" label="已拒绝" />
                      <el-option value="INVALIDATED" label="已失效" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="推送轮次">
                    <el-select v-model="filter.pushRound" placeholder="全部轮次" clearable style="width: 100%">
                      <el-option v-for="r in rounds" :key="r.roundId" :label="'第' + r.roundNumber + '轮'" :value="r.roundNumber" />
                    </el-select>
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row>
                <el-col :span="24">
                  <el-button type="primary" @click="searchCandidates" :loading="searching">
                    <el-icon><Search /></el-icon> 检索考生
                  </el-button>
                  <el-button @click="resetFilter">重置</el-button>
                </el-col>
              </el-row>
            </div>
          </el-card>

          <!-- 检索结果（第一轮记录） -->
          <el-card v-if="searchResult.length > 0">
            <template #header>
              <div class="result-header">
                <span>检索结果 ({{ searchResult.length }} 条)</span>
                <el-button type="primary" size="small" :disabled="selectedCandidates.length === 0" @click="openBatchInviteDialog">
                  发送邀请 ({{ selectedCandidates.length }})
                </el-button>
              </div>
            </template>
            <el-table :data="searchResult" @selection-change="handleSelectionChange" stripe>
              <el-table-column type="selection" width="55" />
              <el-table-column prop="candidateName" label="姓名" width="100" />
              <el-table-column prop="totalScore" label="总分" width="80">
                <template #default="{ row }">{{ row.totalScore ?? '-' }}</template>
              </el-table-column>
              <el-table-column prop="intention" label="意向专业" min-width="150" show-overflow-tooltip />
              <el-table-column prop="pushRound" label="推送轮次" width="100">
                <template #default="{ row }">第 {{ row.pushRound }} 轮</template>
              </el-table-column>
              <el-table-column prop="status" label="状态" width="100">
                <template #default="{ row }">
                  <el-tag size="small">{{ statusText(row.status) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="pushedAt" label="推送时间" width="160">
                <template #default="{ row }">{{ formatTime(row.pushedAt) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="140" fixed="right">
                <template #default="{ row }">
                  <el-button type="primary" link size="small" @click="openDetailDialog(row)">查看</el-button>
                  <el-button type="primary" link size="small" @click="openInviteDialog(row)">发送邀请</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-card>

          <EmptyState
            v-if="searched && searchResult.length === 0"
            title="未找到符合条件的考生"
            description="请调整检索条件后重试"
            icon="Search"
          />
        </el-tab-pane>

        <!-- Tab2: 补录邀请记录 -->
        <el-tab-pane label="补录邀请记录" name="invitations">
          <el-card>
            <template #header>
              <div class="result-header">
                <span>补录邀请记录 ({{ invitationList.length }} 条)</span>
                <el-button type="primary" size="small" @click="fetchInvitations" :loading="loadingInvitations">
                  刷新
                </el-button>
              </div>
            </template>
            <el-table :data="invitationList" stripe v-loading="loadingInvitations">
              <el-table-column prop="candidateName" label="考生姓名" width="100" />
              <el-table-column prop="candidateId" label="考生编号" width="120" />
              <el-table-column prop="majorName" label="邀请专业" min-width="150" show-overflow-tooltip />
              <el-table-column prop="status" label="邀请状态" width="100">
                <template #default="{ row }">
                  <el-tag size="small" :type="invitationStatusType(row.status)">
                    {{ invitationStatusText(row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="supplementRound" label="补录轮次" width="100">
                <template #default="{ row }">第 {{ row.supplementRound }} 轮</template>
              </el-table-column>
              <el-table-column prop="sentAt" label="发送时间" width="160">
                <template #default="{ row }">{{ formatTime(row.sentAt) }}</template>
              </el-table-column>
              <el-table-column prop="expiresAt" label="过期时间" width="160">
                <template #default="{ row }">{{ formatTime(row.expiresAt) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="100" fixed="right">
                <template #default="{ row }">
                  <el-button type="primary" link size="small" @click="openInvitationDetailDialog(row)">查看</el-button>
                </template>
              </el-table-column>
            </el-table>
            <EmptyState
              v-if="!loadingInvitations && invitationList.length === 0"
              title="暂无补录邀请记录"
              description="在左侧检索考生并发送邀请后，将在此显示邀请记录"
              icon="Message"
            />
          </el-card>
        </el-tab-pane>
      </el-tabs>

      <!-- 查看考生详情对话框 -->
      <el-dialog v-model="detailDialogVisible" title="考生详情" width="600px">
        <el-descriptions :column="2" border size="small" v-if="detailData">
          <el-descriptions-item label="考生编号">{{ detailData.candidateId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="姓名">{{ detailData.candidateName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="国籍">{{ detailData.nationality || '-' }}</el-descriptions-item>
          <el-descriptions-item label="证件号">{{ detailData.idNumber || '-' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ detailData.email || '-' }}</el-descriptions-item>
          <el-descriptions-item label="意向方向">{{ detailData.intention || '-' }}</el-descriptions-item>
          <el-descriptions-item label="总分">{{ detailData.totalScore ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag size="small">{{ statusText(detailData.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="推送时间">{{ detailData.pushedAt ? formatTime(detailData.pushedAt) : '-' }}</el-descriptions-item>
          <el-descriptions-item label="推送院校" v-if="detailData.schoolName">{{ detailData.schoolName }}</el-descriptions-item>
        </el-descriptions>
        <!-- 分科成绩 -->
        <div v-if="detailData?.subjectScores && Object.keys(detailData.subjectScores).length > 0" style="margin-top: 16px">
          <h4 style="font-size: 14px; font-weight: 600; margin-bottom: 8px">分科成绩</h4>
          <el-descriptions :column="3" border size="small">
            <el-descriptions-item v-for="(score, subject) in detailData.subjectScores" :key="subject" :label="String(subject)">
              {{ score ?? '-' }}
            </el-descriptions-item>
          </el-descriptions>
        </div>
        <template #footer>
          <el-button @click="detailDialogVisible = false">关闭</el-button>
        </template>
      </el-dialog>

      <!-- 发送邀请对话框 -->
      <el-dialog v-model="inviteDialogVisible" title="发送补录邀请" width="500px">
        <el-form :model="inviteForm" label-width="100px">
          <el-form-item label="考生">
            <span>{{ inviteForm.candidateName }}</span>
          </el-form-item>
          <el-form-item label="录取专业" required>
            <el-select v-model="inviteForm.majorId" placeholder="请选择专业" filterable style="width: 100%">
              <el-option v-for="m in majorList" :key="m.majorId" :label="m.majorName" :value="m.majorId" />
            </el-select>
          </el-form-item>
          <el-form-item label="邀请留言">
            <el-input v-model="inviteForm.message" type="textarea" :rows="3"
              placeholder="请输入邀请留言，向考生说明补录情况" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="inviteDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="inviting" @click="handleSendInvite">发送邀请</el-button>
        </template>
      </el-dialog>

      <!-- 批量发送邀请对话框 -->
      <el-dialog v-model="batchInviteDialogVisible" title="批量发送补录邀请" width="500px">
        <el-form :model="inviteForm" label-width="100px">
          <el-form-item label="已选考生">
            <span>{{ selectedCandidates.length }} 人</span>
          </el-form-item>
          <el-form-item label="录取专业" required>
            <el-select v-model="inviteForm.majorId" placeholder="请选择专业" filterable style="width: 100%">
              <el-option v-for="m in majorList" :key="m.majorId" :label="m.majorName" :value="m.majorId" />
            </el-select>
          </el-form-item>
          <el-form-item label="邀请留言">
            <el-input v-model="inviteForm.message" type="textarea" :rows="3"
              placeholder="请输入邀请留言，向考生说明补录情况" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="batchInviteDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="inviting" @click="handleBatchSendInvite">
            确认发送 ({{ selectedCandidates.length }})
          </el-button>
        </template>
      </el-dialog>

      <!-- 补录邀请详情对话框 -->
      <el-dialog v-model="invitationDetailDialogVisible" title="补录邀请详情" width="500px">
        <el-descriptions :column="2" border size="small" v-if="invitationDetailData">
          <el-descriptions-item label="考生姓名">{{ invitationDetailData.candidateName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="考生编号">{{ invitationDetailData.candidateId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="邀请专业">{{ invitationDetailData.majorName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="补录轮次">第 {{ invitationDetailData.supplementRound }} 轮</el-descriptions-item>
          <el-descriptions-item label="邀请状态">
            <el-tag size="small" :type="invitationStatusType(invitationDetailData.status)">
              {{ invitationStatusText(invitationDetailData.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="发送时间">{{ formatTime(invitationDetailData.sentAt) }}</el-descriptions-item>
          <el-descriptions-item label="过期时间">{{ formatTime(invitationDetailData.expiresAt) }}</el-descriptions-item>
          <el-descriptions-item label="邀请留言" :span="2">{{ invitationDetailData.message || '-' }}</el-descriptions-item>
        </el-descriptions>
        <template #footer>
          <el-button @click="invitationDetailDialogVisible = false">关闭</el-button>
        </template>
      </el-dialog>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useYearStore } from '@/stores/year'
import axios from '@/api/axios'
import dayjs from 'dayjs'

const yearStore = useYearStore()

// ========== 公共部分 ==========
const isMode2 = ref(false)
const rounds = ref<any[]>([])

async function fetchRounds() {
  const res = await axios.get('/v1/supplement/rounds')
  rounds.value = res.data.data || []
}

function formatTime(ts: string) {
  return ts ? dayjs(ts).format('YYYY-MM-DD HH:mm') : '-'
}

function statusText(s: string) {
  const map: Record<string, string> = {
    PENDING: '待处理',
    REJECTED: '已拒绝',
    INVALIDATED: '已失效',
    CONDITIONAL: '有条件录取',
    ADMITTED: '已录取',
    CONFIRMED: '已确认',
    CHECKED_IN: '已报到',
    INVITED: '已邀请'
  }
  return map[s] || s
}

// ========== 模式二部分 ==========
const mode2ActiveTab = ref('search')
const filter = reactive({
  minScore: null as number | null,
  maxScore: null as number | null,
  majorId: null as string | null,
  intentionKeyword: '',
  status: [] as string[],
  pushRound: null as number | null,
})

const searchResult = ref<any[]>([])
const searched = ref(false)
const searching = ref(false)
const majorList = ref<any[]>([])

const selectedCandidates = ref<any[]>([])
const inviteDialogVisible = ref(false)
const batchInviteDialogVisible = ref(false)
const inviting = ref(false)
const detailDialogVisible = ref(false)
const detailData = ref<any>(null)
const detailLoading = ref(false)

const inviteForm = reactive({
  pushId: '',
  candidateName: '',
  majorId: null as string | null,
  message: '',
})

// 补录邀请相关
const invitationList = ref<any[]>([])
const loadingInvitations = ref(false)
const invitationDetailDialogVisible = ref(false)
const invitationDetailData = ref<any>(null)

// 邀请状态文本
function invitationStatusText(status: string) {
  const map: Record<string, string> = {
    INVITED: '已发出邀请',
    ACCEPTED: '已接受',
    REJECTED: '已拒绝',
    EXPIRED: '已过期'
  }
  return map[status] || status
}

function invitationStatusType(status: string): 'success' | 'warning' | 'info' | 'danger' {
  const map: Record<string, 'success' | 'warning' | 'info' | 'danger'> = {
    INVITED: 'warning',
    ACCEPTED: 'success',
    REJECTED: 'danger',
    EXPIRED: 'info'
  }
  return map[status] || 'info'
}

async function fetchInvitations() {
  loadingInvitations.value = true
  try {
    const res = await axios.get('/v1/supplement/invitations')
    invitationList.value = res.data.data || []
  } catch (e) {
    ElMessage.error('获取邀请记录失败')
    invitationList.value = []
  } finally {
    loadingInvitations.value = false
  }
}

function openInvitationDetailDialog(invitation: any) {
  invitationDetailData.value = invitation
  invitationDetailDialogVisible.value = true
}

async function fetchSupplementMode() {
  try {
    const res = await axios.get('/v1/admin/params/global')
    const modeConfig = (res.data.data || []).find((c: any) => c.configKey === 'supplement_mode')
    isMode2.value = modeConfig?.configValue === 'MODE_2'
  } catch (e) {
    isMode2.value = false
  }
}

async function fetchMajors() {
  try {
    const res = await axios.get('/v1/majors', { _silent: true } as any)
    majorList.value = res.data.data?.records || res.data.data || []
  } catch (e) {
    majorList.value = []
  }
}

async function searchCandidates() {
  searching.value = true
  searched.value = true
  try {
    const params: any = { status: ['PENDING', 'REJECTED', 'INVALIDATED'] }
    if (filter.minScore != null) params.minScore = filter.minScore
    if (filter.maxScore != null) params.maxScore = filter.maxScore
    if (filter.majorId) params.majorId = filter.majorId
    if (filter.intentionKeyword) params.intentionKeyword = filter.intentionKeyword
    if (filter.pushRound != null) params.round = filter.pushRound

    const res = await axios.get('/v1/students', { params })
    searchResult.value = res.data.data?.records || []
  } catch (e) {
    ElMessage.error('检索失败')
    searchResult.value = []
  } finally {
    searching.value = false
  }
}

function resetFilter() {
  filter.minScore = null
  filter.maxScore = null
  filter.majorId = null
  filter.intentionKeyword = ''
  filter.status = []
  filter.pushRound = null
  searchResult.value = []
  searched.value = false
}

function handleSelectionChange(val: any[]) {
  selectedCandidates.value = val
}

function openInviteDialog(candidate: any) {
  inviteForm.pushId = candidate.pushId
  inviteForm.candidateName = candidate.candidateName
  inviteForm.majorId = null
  inviteForm.message = ''
  inviteDialogVisible.value = true
}

async function openDetailDialog(candidate: any) {
  detailLoading.value = true
  detailDialogVisible.value = true
  detailData.value = null
  try {
    const res = await axios.get(`/v1/students/${candidate.pushId}`)
    detailData.value = res.data.data
  } catch {
    ElMessage.error('获取考生详情失败')
    detailDialogVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

function openBatchInviteDialog() {
  inviteForm.majorId = null
  inviteForm.message = ''
  batchInviteDialogVisible.value = true
}

async function handleSendInvite() {
  if (!inviteForm.majorId) {
    ElMessage.warning('请选择录取专业')
    return
  }
  inviting.value = true
  try {
    await axios.post('/v1/supplement/invitations', {
      pushId: inviteForm.pushId,
      majorId: inviteForm.majorId,
      message: inviteForm.message
    })
    ElMessage.success('邀请已发送')
    inviteDialogVisible.value = false
    // 刷新邀请记录列表
    if (mode2ActiveTab.value === 'invitations') {
      fetchInvitations()
    }
  } catch (e: any) {
    ElMessage.error(e.message || '发送失败')
  } finally {
    inviting.value = false
  }
}

async function handleBatchSendInvite() {
  if (!inviteForm.majorId) {
    ElMessage.warning('请选择录取专业')
    return
  }
  inviting.value = true
  try {
    const pushIds = selectedCandidates.value.map(c => c.pushId)
    await axios.post('/v1/supplement/invitations/batch', {
      pushIds,
      majorId: inviteForm.majorId,
      message: inviteForm.message
    })
    ElMessage.success(`已向 ${pushIds.length} 名考生发送邀请`)
    batchInviteDialogVisible.value = false
    // 刷新邀请记录列表
    if (mode2ActiveTab.value === 'invitations') {
      fetchInvitations()
    }
  } catch (e: any) {
    ElMessage.error(e.message || '发送失败')
  } finally {
    inviting.value = false
  }
}

// ========== 初始化 ==========
onMounted(async () => {
  await fetchSupplementMode()
  // 两种模式都需要获取补录轮次
  fetchRounds()
  if (isMode2.value) {
    fetchMajors()
    fetchInvitations()
  }
})

// 年度变化时重新加载数据
watch(() => yearStore.selectedYear, () => {
  fetchRounds()
  if (isMode2.value) {
    fetchInvitations()
  }
})
</script>

<style scoped lang="scss">
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.filter-section {
  .el-form-item {
    margin-bottom: 12px;
  }
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
