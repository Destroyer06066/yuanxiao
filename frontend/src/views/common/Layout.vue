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

        <el-menu-item v-if="authStore.isOpAdmin" index="/admin/schools">
          <el-icon><OfficeBuilding /></el-icon>
          <template #title>院校管理</template>
        </el-menu-item>

        <el-menu-item index="/students">
          <el-icon><User /></el-icon>
          <template #title>考生列表</template>
        </el-menu-item>

        <el-menu-item index="/majors">
          <el-icon><Reading /></el-icon>
          <template #title>专业配置</template>
        </el-menu-item>

        <el-menu-item index="/quota">
          <el-icon><DataLine /></el-icon>
          <template #title>名额管理</template>
        </el-menu-item>

        <el-menu-item v-if="authStore.isOpAdmin || true" index="/supplement">
          <el-icon><RefreshRight /></el-icon>
          <template #title>补录管理</template>
        </el-menu-item>

        <el-menu-item index="/verification">
          <el-icon><Stamp /></el-icon>
          <template #title>成绩核验</template>
        </el-menu-item>

        <el-menu-item index="/checkin">
          <el-icon><Check /></el-icon>
          <template #title>报到管理</template>
        </el-menu-item>

        <el-menu-item index="/statistics">
          <el-icon><DataBoard /></el-icon>
          <template #title>数据统计</template>
        </el-menu-item>

        <el-menu-item v-if="authStore.isOpAdmin" index="/accounts">
          <el-icon><Avatar /></el-icon>
          <template #title>账号管理</template>
        </el-menu-item>

        <el-menu-item v-if="authStore.isOpAdmin" index="/roles">
          <el-icon><Key /></el-icon>
          <template #title>角色管理</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="topbar">
        <el-icon class="collapse-btn" @click="isCollapse = !isCollapse">
          <Expand v-if="isCollapse" /><Fold v-else />
        </el-icon>

        <div class="topbar-right">
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
import {
  HomeFilled, OfficeBuilding, User, Reading,
  DataLine, RefreshRight, Stamp, Check,
  DataBoard, Avatar, Bell, Expand, Fold,
  ArrowDown, SwitchButton, School
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const notifStore = useNotificationStore()

const isCollapse = ref(false)
const unreadCount = computed(() => notifStore.unreadCount)

const activeMenu = computed(() => route.path)

onMounted(async () => {
  // 页面刷新后补充加载用户信息（Pinia store 重新初始化）
  if (authStore.token && !authStore.userInfo) {
    await authStore.fetchUserInfo()
  }
  notifStore.startPolling()
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

    :deep(.el-menu-item) {
      color: #bfcbd9;
      &:hover { background: #263445; color: #fff; }
      &.is-active { background: #263445; color: #409eff; }
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
