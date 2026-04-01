<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">成绩核验</h2>
    </div>

    <!-- 核验表单 -->
    <el-card class="verify-form-card">
      <el-form :model="verifyForm" inline @submit.prevent="submitVerify">
        <el-form-item label="证书编号">
          <el-input
            v-model="verifyForm.certificateNo"
            placeholder="请输入证书编号"
            style="width: 200px"
            clearable
          />
        </el-form-item>
        <el-form-item label="防伪验证码">
          <el-input
            v-model="verifyForm.verifyCode"
            placeholder="请输入防伪验证码"
            style="width: 180px"
            clearable
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="submitVerify">
            开始核验
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 核验结果 -->
    <el-card v-if="result" :class="['result-card', result.valid ? 'success' : 'failure']">
      <template #header>
        <span class="result-title">
          <el-icon v-if="result.valid" color="#67c23a" :size="20"><CircleCheck /></el-icon>
          <el-icon v-else color="#f56c6c" :size="20"><CircleClose /></el-icon>
          {{ result.valid ? '证书核验通过' : '证书核验失败' }}
        </span>
      </template>

      <!-- 有效证书详情 - PDF显示 -->
      <div v-if="result.valid" class="certificate-pdf-container">
        <embed
          :src="'/certificate-template.pdf#toolbar=0&navpanes=0&scrollbar=0'"
          type="application/pdf"
          class="certificate-embed"
        />
        <div class="result-message success">
          <el-icon color="#67c23a"><SuccessFilled /></el-icon>
          {{ result.message }}
        </div>
      </div>

      <!-- 失败信息 -->
      <div v-else>
        <el-result icon="error" :title="result.message || '该证书不存在或已失效'" />
      </div>
    </el-card>

    <!-- 空状态提示 -->
    <el-card v-else class="empty-card">
      <el-empty description="请输入证书编号和防伪验证码进行核验" :image-size="80" />
    </el-card>

    <!-- 成绩说明 -->
    <el-card class="info-card">
      <el-collapse>
        <el-collapse-item title="成绩说明" name="info">
          <div class="info-section">
            <div class="info-title">一、百分位排名 (Percentile Rank) 说明</div>
            <div class="info-content">
              CSCA成绩百分位排名用于比较该考生在本次考试同一科目中与全球其他考生的表现，反映其成绩在整体考生群体中的相对位置。具体而言，百分位排名的数值表示，该考生的成绩在本次考试同一科目中，超越了百分之多少的全球考生。例如，某考生的百分位排名为0m，即表示其成绩等于或高于约90%的全球考生，即处于全球考生中的前10%。
            </div>
          </div>
          <div class="info-section">
            <div class="info-title">二、成绩状态说明</div>
            <div class="info-content">
              根据本次考试结果，考生的成绩报告可能标注以下一种或多种成绩状态:
            </div>
            <div class="status-list">
              <div class="status-item">
                <el-tag type="info" size="small">缺席 / Absent</el-tag>
                <span>考生未参加考试，或因设备、网络、系统等客观原因未能完成考试。</span>
              </div>
              <div class="status-item">
                <el-tag type="danger" size="small">无效 / Invalid</el-tag>
                <span>考生在本次考试中存在违规行为或其他可能破坏考试公平的行为，该科目成绩被判定为无效。</span>
              </div>
              <div class="status-item">
                <el-tag type="warning" size="small">待发布 / Pending</el-tag>
                <span>因存在需要进一步核实的情况，成绩暂缓发布。待相关情况核实完成后，将更新最终结果。</span>
              </div>
              <div class="status-item">
                <el-tag type="info" size="small">取消 / Cancelled</el-tag>
                <span>考生主动申请取消成绩。</span>
              </div>
            </div>
          </div>
        </el-collapse-item>
      </el-collapse>
    </el-card>

    <!-- 核验历史记录 -->
    <el-card class="history-card">
      <template #header>
        <div class="history-header">
          <span>核验记录</span>
          <el-select
            v-model="logQuery.result"
            placeholder="核验结果"
            style="width: 120px"
            clearable
            @change="fetchLogs"
          >
            <el-option label="全部" value="" />
            <el-option label="通过" value="PASSED" />
            <el-option label="未通过" value="FAILED" />
          </el-select>
        </div>
      </template>
      <el-table :data="logs" v-loading="logLoading" size="small">
        <el-table-column prop="certificateNo" label="证书号" width="150" />
        <el-table-column prop="candidateName" label="考生姓名" width="100">
          <template #default="{ row }">
            {{ row.candidateName || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="result" label="核验结果" width="90">
          <template #default="{ row }">
            <el-tag :type="row.result === 'PASSED' ? 'success' : 'danger'" size="small">
              {{ row.result === 'PASSED' ? '通过' : '未通过' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="note" label="备注" min-width="200">
          <template #default="{ row }">
            {{ row.note || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="核验时间" width="170" />
      </el-table>
      <el-pagination
        v-model:current-page="logQuery.page"
        v-model:page-size="logQuery.pageSize"
        :total="logTotal"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        style="margin-top: 12px; justify-content: flex-end"
        @size-change="fetchLogs"
        @current-change="fetchLogs"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { CircleCheck, CircleClose, SuccessFilled } from '@element-plus/icons-vue'
import {
  verifyCertificate,
  getVerificationLogs,
  type CertificateVerificationResponse,
  type VerificationLogRecord,
} from '@/api/verification'

// ========== 核验表单 ==========
const verifyForm = reactive({
  certificateNo: '',
  verifyCode: '',
})

const result = ref<CertificateVerificationResponse | null>(null)
const submitting = ref(false)

async function submitVerify() {
  if (!verifyForm.certificateNo.trim()) {
    ElMessage.warning('请输入证书编号')
    return
  }
  if (!verifyForm.verifyCode.trim()) {
    ElMessage.warning('请输入防伪验证码')
    return
  }

  submitting.value = true
  try {
    const res = await verifyCertificate({
      certificateNo: verifyForm.certificateNo.trim(),
      verifyCode: verifyForm.verifyCode.trim(),
    })
    result.value = res.data.data
    ElMessage.success(result.value.valid ? '证书核验通过' : '证书核验失败')
    // 刷新历史记录
    fetchLogs()
    // 清空表单
    verifyForm.certificateNo = ''
    verifyForm.verifyCode = ''
  } catch {
    // 错误由 axios 拦截器处理
  } finally {
    submitting.value = false
  }
}

// ========== 核验记录 ==========
const logs = ref<VerificationLogRecord[]>([])
const logTotal = ref(0)
const logLoading = ref(false)

const logQuery = reactive({
  result: '',
  page: 1,
  pageSize: 10,
})

async function fetchLogs() {
  logLoading.value = true
  try {
    const params: any = {
      page: logQuery.page,
      pageSize: logQuery.pageSize,
    }
    if (logQuery.result) {
      params.result = logQuery.result
    }
    const res = await getVerificationLogs(params)
    logs.value = res.data.data.records || []
    logTotal.value = res.data.data.total || 0
  } catch {
    // 错误由 axios 拦截器处理
  } finally {
    logLoading.value = false
  }
}

// ========== 初始化 ==========
onMounted(() => {
  fetchLogs()
})
</script>

<style scoped lang="scss">
.page-container {
  padding: 20px;
}

.page-header {
  margin-bottom: 16px;
}

.page-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.verify-form-card {
  margin-bottom: 16px;
}

.result-card {
  margin-bottom: 16px;
  border: 2px solid;

  &.success {
    border-color: #67c23a;
  }

  &.failure {
    border-color: #f56c6c;
  }

  .result-title {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 16px;
    font-weight: 600;
  }

  .subject-scores {
    margin-top: 16px;
    padding-top: 16px;
    border-top: 1px solid #eee;

    .subject-label {
      font-weight: 500;
      color: #606266;
    }
  }

  .certificate-pdf-container {
    width: 100%;
    background: #fff;

    .certificate-embed {
      width: 100%;
      height: calc(100vh - 280px);
      border: none;
      display: block;
      background: #fff;
    }
  }

  .result-message {
    margin-top: 16px;
    padding: 12px;
    border-radius: 4px;
    display: flex;
    align-items: center;
    gap: 8px;

    &.success {
      background: #f0f9eb;
      color: #67c23a;
    }
  }
}

.empty-card {
  margin-bottom: 16px;
  text-align: center;
}

.history-card {
  .history-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-weight: 600;
  }
}

.info-card {
  margin-bottom: 16px;

  .info-section {
    margin-bottom: 16px;

    &:last-child {
      margin-bottom: 0;
    }
  }

  .info-title {
    font-weight: 600;
    margin-bottom: 8px;
    color: #303133;
  }

  .info-content {
    line-height: 1.8;
    color: #606266;
    font-size: 14px;
  }

  .status-list {
    margin-top: 12px;

    .status-item {
      display: flex;
      align-items: flex-start;
      gap: 12px;
      margin-bottom: 12px;
      line-height: 1.6;

      .el-tag {
        flex-shrink: 0;
        margin-top: 2px;
      }

      span {
        color: #606266;
        font-size: 14px;
      }
    }
  }
}
</style>
