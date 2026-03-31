<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">报到管理</h2>
    </div>

    <el-card>
      <!-- 筛选条件 -->
      <div class="filter-row">
        <el-select v-model="query.status" placeholder="报到状态" style="width: 180px" clearable @change="doSearch">
          <el-option label="待材料收件" value="CONFIRMED" />
          <el-option label="材料已收件" value="MATERIAL_RECEIVED" />
          <el-option label="已完成报到" value="CHECKED_IN" />
        </el-select>
        <el-select v-model="query.materialReceived" placeholder="材料收件状态" style="width: 180px" clearable @change="doSearch">
          <el-option label="未收件" value="false" />
          <el-option label="已收件" value="true" />
        </el-select>
        <el-button @click="doSearch">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </div>

      <!-- 表格 -->
      <el-table :data="list" v-loading="loading" stripe style="margin-top: 12px">
        <el-table-column label="考生姓名" width="120">
          <template #default="{ row }">
            <router-link :to="`/students/${row.pushId}`" class="candidate-link">
              {{ row.candidateName }}
            </router-link>
          </template>
        </el-table-column>
        <el-table-column prop="majorName" label="录取专业" min-width="160" />
        <el-table-column prop="totalScore" label="总分" width="80" />
        <el-table-column prop="status" label="报到状态" width="130">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">
              {{ row.statusDesc }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="materialStatus" label="材料收件状态" width="130">
          <template #default="{ row }">
            <el-tag :type="row.receiveTime ? 'success' : 'info'" size="small">
              {{ row.receiveTime ? '已收件' : '未收件' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态说明" min-width="160">
          <template #default="{ row }">
            {{ statusExplain(row.status) }}
          </template>
        </el-table-column>
        <el-table-column prop="receiveTime" label="材料收件时间" width="170" />
        <el-table-column prop="checkinTime" label="报到时间" width="170" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="can('checkin:material') && row.status === 'CONFIRMED'"
              type="success" link
              @click="openReceiveDialog(row)"
            >
              登记收件
            </el-button>
            <el-button
              v-if="can('checkin:confirm') && row.status === 'MATERIAL_RECEIVED'"
              type="primary" link
              @click="openCheckinDialog(row)"
            >
              确认报到
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
        @size-change="fetchCheckins"
        @current-change="fetchCheckins"
      />
    </el-card>

    <!-- 登记收件对话框 -->
    <el-dialog v-model="receiveDialogVisible" title="登记材料收件" width="420px">
      <el-form :model="receiveForm" label-width="70px">
        <el-form-item label="考生">
          <span>{{ receiveForm.candidateName }}</span>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="receiveForm.note" type="textarea" :rows="3" placeholder="选填，补充说明收件情况" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="receiveDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="confirmReceiveMaterial">确认收件</el-button>
      </template>
    </el-dialog>

    <!-- 确认报到对话框 -->
    <el-dialog v-model="checkinDialogVisible" title="确认报到" width="420px">
      <el-form :model="checkinForm" label-width="70px">
        <el-form-item label="考生">
          <span>{{ checkinForm.candidateName }}</span>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="checkinForm.note" type="textarea" :rows="3" placeholder="选填，补充说明报到情况" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="checkinDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="confirmCheckin">确认报到</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getCheckinList, receiveMaterial, doCheckin, type CheckinRecord } from '@/api/checkin'
import { usePermission } from '@/composables/usePermission'

const { can } = usePermission()

const loading = ref(false)
const submitting = ref(false)
const list = ref<CheckinRecord[]>([])
const total = ref(0)

const receiveDialogVisible = ref(false)
const receiveForm = reactive({ pushId: '', candidateName: '', note: '' })
const checkinDialogVisible = ref(false)
const checkinForm = reactive({ pushId: '', candidateName: '', note: '' })

const query = reactive({
  status: '',
  materialReceived: '',
  page: 1,
  pageSize: 20,
})

function statusTagType(status: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' {
  const map: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
    CONFIRMED: 'info',
    MATERIAL_RECEIVED: 'warning',
    CHECKED_IN: 'success',
  }
  return map[status] || 'info'
}

function statusExplain(status: string) {
  const map: Record<string, string> = {
    CONFIRMED: '已预录取，等待考生提交材料',
    MATERIAL_RECEIVED: '材料已收件，等待确认报到',
    CHECKED_IN: '已完成报到入学',
  }
  return map[status] || ''
}

async function fetchCheckins() {
  loading.value = true
  try {
    const params: { status?: string; materialReceived?: boolean } = {}
    if (query.status) params.status = query.status
    if (query.materialReceived === 'true') params.materialReceived = true
    else if (query.materialReceived === 'false') params.materialReceived = false

    const res = await getCheckinList(params)
    list.value = res.data.data || []
    total.value = list.value.length
  } catch {
    // error handled by axios interceptor
  } finally {
    loading.value = false
  }
}

function doSearch() {
  query.page = 1
  fetchCheckins()
}

function resetQuery() {
  query.status = ''
  query.materialReceived = ''
  query.page = 1
  fetchCheckins()
}

function openReceiveDialog(row: CheckinRecord) {
  receiveForm.pushId = row.pushId
  receiveForm.candidateName = row.candidateName
  receiveForm.note = ''
  receiveDialogVisible.value = true
}

async function confirmReceiveMaterial() {
  submitting.value = true
  try {
    await receiveMaterial(receiveForm.pushId, receiveForm.note || undefined)
    ElMessage.success('已登记收件')
    receiveDialogVisible.value = false
    fetchCheckins()
  } catch {
    // error handled by axios interceptor
  } finally {
    submitting.value = false
  }
}

function openCheckinDialog(row: CheckinRecord) {
  checkinForm.pushId = row.pushId
  checkinForm.candidateName = row.candidateName
  checkinForm.note = ''
  checkinDialogVisible.value = true
}

async function confirmCheckin() {
  submitting.value = true
  try {
    await doCheckin(checkinForm.pushId, checkinForm.note || undefined)
    ElMessage.success('已确认报到')
    checkinDialogVisible.value = false
    fetchCheckins()
  } catch {
    // error handled by axios interceptor
  } finally {
    submitting.value = false
  }
}

onMounted(fetchCheckins)
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
.candidate-link {
  color: #409eff;
  text-decoration: none;
  &:hover { text-decoration: underline; }
}
</style>
