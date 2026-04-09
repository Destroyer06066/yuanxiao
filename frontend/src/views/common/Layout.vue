<template>
  <el-container class="app-layout">
    <el-aside :width="isCollapse ? '64px' : '220px'" class="sidebar">
      <div class="logo-area">
        <img v-if="!isCollapse" src="/vite.svg" alt="logo" class="logo-img" />
        <span v-if="!isCollapse" class="logo-text">院校管理平台</span>
        <el-icon v-else><School /></el-icon>
      </div>

      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :router="true"
        class="sidebar-menu"
      >
        <el-menu-item index="/dashboard">
          <el-icon><HomeFilled /></el-icon>
          <template #title>首页</template>
        </el-menu-item>

        <!-- OP_ADMIN 分组菜单 -->
        <template v-if="authStore.isOpAdmin">
          <el-sub-menu index="admission">
            <template #title><el-icon><TrendCharts /></el-icon><span>招生管理</span></template>
            <el-menu-item index="/students">
              <el-icon><User /></el-icon>
              <template #title>考生列表</template>
            </el-menu-item>
            <el-menu-item index="/supplement">
              <el-icon><RefreshRight /></el-icon>
              <template #title>录取轮次</template>
            </el-menu-item>
            <el-menu-item index="/supplements">
              <el-icon><Message /></el-icon>
              <template #title>补录管理</template>
            </el-menu-item>
            <el-menu-item index="/verification">
              <el-icon><CircleCheck /></el-icon>
              <template #title>成绩核验</template>
            </el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="data-stats">
            <template #title><el-icon><DataAnalysis /></el-icon><span>数据统计</span></template>
            <el-menu-item index="/statistics">
              <el-icon><DataBoard /></el-icon>
              <template #title>数据统计</template>
            </el-menu-item>
            <el-menu-item index="/admin/student-statistics">
              <el-icon><DataAnalysis /></el-icon>
              <template #title>考生统计</template>
            </el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="school-mgmt">
            <template #title><el-icon><OfficeBuilding /></el-icon><span>院校管理</span></template>
            <el-menu-item index="/admin/schools">
              <el-icon><OfficeBuilding /></el-icon>
              <template #title>院校列表</template>
            </el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="config">
            <template #title><el-icon><Reading /></el-icon><span>配置管理</span></template>
            <el-menu-item index="/majors">
              <el-icon><Reading /></el-icon>
              <template #title>专业配置</template>
            </el-menu-item>
            <el-menu-item index="/quota">
              <el-icon><DataLine /></el-icon>
              <template #title>名额管理</template>
            </el-menu-item>
            <el-menu-item index="/brochure">
              <el-icon><Document /></el-icon>
              <template #title>招生简章</template>
            </el-menu-item>
          </el-sub-menu>

          <el-menu-item index="/notifications">
            <el-icon><Bell /></el-icon>
            <template #title>站内通知</template>
          </el-menu-item>

          <el-sub-menu index="system">
            <template #title><el-icon><Setting /></el-icon><span>系统</span></template>
            <el-menu-item index="/accounts">
              <el-icon><Avatar /></el-icon>
              <template #title>账号管理</template>
            </el-menu-item>
            <el-menu-item index="/roles">
              <el-icon><Key /></el-icon>
              <template #title>角色管理</template>
            </el-menu-item>
            <el-menu-item index="/admin/audit-logs">
              <el-icon><Document /></el-icon>
              <template #title>操作日志</template>
            </el-menu-item>
            <el-menu-item index="/admin/params">
              <el-icon><Setting /></el-icon>
              <template #title>系统参数</template>
            </el-menu-item>
          </el-sub-menu>
        </template>

        <!-- SCHOOL_ADMIN / STAFF 分组菜单 -->
        <template v-else>
          <el-sub-menu index="admission">
            <template #title><el-icon><TrendCharts /></el-icon><span>招生管理</span></template>
            <el-menu-item index="/students">
              <el-icon><User /></el-icon>
              <template #title>考生列表</template>
            </el-menu-item>
            <el-menu-item index="/checkin">
              <el-icon><Check /></el-icon>
              <template #title>报到管理</template>
            </el-menu-item>
            <el-menu-item index="/supplement">
              <el-icon><RefreshRight /></el-icon>
              <template #title>录取轮次</template>
            </el-menu-item>
            <el-menu-item index="/supplements">
              <el-icon><Message /></el-icon>
              <template #title>补录管理</template>
            </el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="config">
            <template #title><el-icon><Reading /></el-icon><span>配置管理</span></template>
            <el-menu-item index="/majors">
              <el-icon><Reading /></el-icon>
              <template #title>专业配置</template>
            </el-menu-item>
            <el-menu-item index="/quota">
              <el-icon><DataLine /></el-icon>
              <template #title>名额管理</template>
            </el-menu-item>
            <el-menu-item index="/brochure">
              <el-icon><Document /></el-icon>
              <template #title>招生简章</template>
            </el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="data-stats-school">
            <template #title><el-icon><DataAnalysis /></el-icon><span>数据统计</span></template>
            <el-menu-item index="/statistics">
              <el-icon><DataBoard /></el-icon>
              <template #title>数据统计</template>
            </el-menu-item>
          </el-sub-menu>

          <el-menu-item index="/notifications">
            <el-icon><Bell /></el-icon>
            <template #title>站内通知</template>
          </el-menu-item>

          <!-- SCHOOL_ADMIN 系统菜单 -->
          <el-sub-menu v-if="authStore.role === 'SCHOOL_ADMIN'" index="school-system">
            <template #title><el-icon><Setting /></el-icon><span>系统</span></template>
            <el-menu-item index="/accounts">
              <el-icon><Avatar /></el-icon>
              <template #title>账号管理</template>
            </el-menu-item>
          </el-sub-menu>
        </template>

      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="topbar">
        <el-icon class="collapse-btn" @click="isCollapse = !isCollapse">
          <Expand v-if="isCollapse" /><Fold v-else />
        </el-icon>

        <div class="topbar-right">
          <!-- 全局考生搜索 -->
          <div class="global-search">
            <el-select
              v-model="searchKeyword"
              filterable
              remote
              reserve-keyword
              placeholder="搜索考生..."
              :remote-method="handleSearch"
              :loading="searchLoading"
              size="small"
              style="width: 240px"
              @change="handleSearchSelect"
              clearable
              @clear="searchResults = []"
            >
              <el-option
                v-for="item in searchResults"
                :key="item.pushId"
                :label="item.candidateName"
                :value="item.pushId"
              >
                <span style="float:left">{{ item.candidateName }}</span>
                <span style="float:right; color:#409eff; font-size:12px; font-weight:600; margin-right:8px">
                  {{ item.totalScore != null ? '分数: ' + item.totalScore : '' }}
                </span>
                <span style="float:right; color:#999; font-size:12px; margin-right:8px">
                  {{ item.schoolName || '' }}
                </span>
                <span style="float:right; margin-right:8px">
                  <el-tag size="small" :class="'status-tag ' + item.status">{{ item.statusDesc }}</el-tag>
                </span>
              </el-option>
            </el-select>
          </div>

          <!-- 全局年度选择器 -->
          <el-select
            v-model="yearStore.selectedYear"
            style="width: 100px; margin-right: 8px"
            size="small"
          >
            <el-option
              v-for="y in yearStore.availableYears"
              :key="y"
              :label="y + '年'"
              :value="y"
            />
          </el-select>

          <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="notif-badge">
            <el-icon size="20" @click="router.push('/notifications')">
              <Bell />
            </el-icon>
          </el-badge>

          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="32">{{ authStore.realName?.[0] }}</el-avatar>
              <span class="user-name">{{ authStore.realName }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notification'
import { useYearStore } from '@/stores/year'
import { searchStudents } from '@/api/student'
import {
  HomeFilled, OfficeBuilding, User, Reading,
  DataLine, RefreshRight, Stamp, Check,
  DataBoard, Avatar, Bell, Expand, Fold,
  ArrowDown, SwitchButton, School,
  TrendCharts, Setting, Document, Key,
  Message, DataAnalysis
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const notifStore = useNotificationStore()
const yearStore = useYearStore()

const isCollapse = ref(false)
const unreadCount = computed(() => notifStore.unreadCount)

// 全局搜索
const searchKeyword = ref('')
const searchResults = ref<any[]>([])
const searchLoading = ref(false)
let searchTimer: ReturnType<typeof setTimeout> | null = null

async function handleSearch(query: string) {
  if (!query || query.length < 2) {
    searchResults.value = []
    return
  }
  searchLoading.value = true
  try {
    const res = await searchStudents(query)
    searchResults.value = res.data.data || []
  } catch {
    searchResults.value = []
  } finally {
    searchLoading.value = false
  }
}

function handleSearchSelect(pushId: string) {
  if (pushId) {
    router.push(`/students/${pushId}`)
    searchKeyword.value = ''
    searchResults.value = []
  }
}

const activeMenu = computed(() => route.path)

onMounted(async () => {
  // 页面刷新后补充加载用户信息（Pinia store 重新初始化）
  if (authStore.token && !authStore.userInfo) {
    await authStore.fetchUserInfo()
  }
  notifStore.startPolling()
  yearStore.loadYears()
})

function handleCommand(cmd: string) {
  if (cmd === 'logout') {
    notifStore.stopPolling()
    authStore.logoutAction()
  }
}
</script>

<style scoped lang="scss">
.app-layout {
  height: 100vh;
}

.sidebar {
  background: #304156;
  transition: width 0.3s;
  overflow: hidden;

  .logo-area {
    height: 60px;
    display: flex;
    align-items: center;
    padding: 0 16px;
    color: #fff;
    border-bottom: 1px solid rgba(255,255,255,0.1);

    .logo-img {
      width: 28px;
      height: 28px;
      margin-right: 10px;
    }

    .logo-text {
      font-size: 16px;
      font-weight: 600;
      white-space: nowrap;
    }

    .el-icon {
      font-size: 22px;
    }
  }

  .sidebar-menu {
    border-right: none;
    background: transparent;
    --el-menu-bg-color: transparent;
    --el-menu-hover-bg-color: #263445;
    --el-menu-text-color: #bfcbd9;
    --el-menu-active-color: #409eff;

    :deep(.el-menu-item),
    :deep(.el-sub-menu__title) {
      color: #bfcbd9;
      &:hover { background: #263445; color: #fff; }
    }
    :deep(.el-menu-item.is-active) {
      background: #263445;
      color: #409eff;
    }
    :deep(.el-sub-menu .el-menu) {
      --el-menu-bg-color: transparent;
    }
  }
}

.topbar {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  border-bottom: 1px solid #e6e6e6;
  box-shadow: 0 1px 4px rgba(0,0,0,0.08);

  .collapse-btn {
    font-size: 20px;
    cursor: pointer;
    color: #606266;
    &:hover { color: #409eff; }
  }

  .topbar-right {
    display: flex;
    align-items: center;
    gap: 16px;

    .global-search { display: flex; align-items: center; }

    .notif-badge { cursor: pointer; }

    .user-info {
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;
      color: #606266;

      .user-name { font-size: 14px; }
    }
  }
}

.main-content {
  background: #f0f2f5;
  overflow-y: auto;
}
</style>
