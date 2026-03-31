<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">站内通知</h2>
      <el-button v-if="hasUnread" type="primary" @click="markAllRead">
        全部标为已读
      </el-button>
    </div>

    <el-card>
      <el-empty v-if="list.length === 0" description="暂无通知" />

      <div v-else class="notif-list">
        <div
          v-for="item in list"
          :key="item.id"
          class="notif-item"
          :class="{ unread: !item.isRead }"
          @click="handleRead(item)"
        >
          <div class="notif-icon">
            <el-icon size="20"><Bell /></el-icon>
          </div>
          <div class="notif-content">
            <div class="notif-title">{{ item.title }}</div>
            <div class="notif-body">{{ item.content }}</div>
            <div class="notif-footer">
              <span class="notif-time">{{ formatTime(item.createdAt) }}</span>
              <el-button v-if="item.pushId" type="primary" link @click.stop="viewDetail(item)">
                查看详情
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <div v-if="list.length > 0" class="pagination-area">
        <el-pagination
          v-model:current-page="page"
          :page-size="20"
          layout="prev, pager, next"
          :total="total"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { Bell } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { ElMessage } from 'element-plus'
import axios from '@/api/axios'

interface Notification {
  notificationId: string
  pushId: string | null
  title: string
  content: string
  isRead: boolean
  createdAt: string
}

const list = ref<Notification[]>([])
const page = ref(1)
const total = ref(0)
const router = useRouter()
const hasUnread = ref(false)

async function fetchNotifications() {
  const res = await axios.get('/v1/notifications', {
    params: { page: page.value, pageSize: 20 },
  })
  list.value = res.data.data.records.map((n: any) => ({
    ...n,
    notificationId: n.notificationId || n.id,
    pushId: n.pushId,
  }))
  total.value = res.data.data.total
  hasUnread.value = list.value.some((n: Notification) => !n.isRead)
}

function formatTime(ts: string) {
  return dayjs(ts).format('YYYY-MM-DD HH:mm')
}

async function handleRead(item: Notification) {
  if (item.isRead) return
  await axios.patch(`/v1/notifications/${item.notificationId}/read`)
  item.isRead = true
}

function viewDetail(item: Notification) {
  if (item.pushId) {
    router.push(`/students/${item.pushId}`)
  }
}

async function markAllRead() {
  await axios.post('/v1/notifications/read-all')
  list.value.forEach((n: Notification) => (n.isRead = true))
  hasUnread.value = false
  ElMessage.success('已全部标为已读')
}

let timer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  fetchNotifications()
  // 每 30s 轮询通知
  timer = setInterval(fetchNotifications, 30000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped lang="scss">
.notif-list {
  .notif-item {
    display: flex;
    gap: 12px;
    padding: 16px 0;
    border-bottom: 1px solid #f0f0f0;
    cursor: pointer;
    transition: background 0.2s;

    &:hover { background: #f9fafb; }

    &.unread .notif-title {
      font-weight: 600;
      color: #409eff;
    }

    .notif-icon {
      width: 40px;
      height: 40px;
      border-radius: 50%;
      background: #ecf5ff;
      color: #409eff;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }

    .notif-content {
      flex: 1;

      .notif-title { font-size: 14px; color: #303133; margin-bottom: 4px; }
      .notif-body { font-size: 13px; color: #606266; margin-bottom: 4px; }
      .notif-time { font-size: 12px; color: #909399; }
      .notif-footer {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-top: 4px;
      }
    }
  }
}

.pagination-area {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}
</style>
