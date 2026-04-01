import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/common/Login.vue'),
    meta: { title: '登录', requiresAuth: false },
  },
  {
    path: '/force-change-password',
    name: 'ForceChangePassword',
    component: () => import('@/views/common/ForceChangePassword.vue'),
    meta: { title: '修改密码', requiresAuth: true },
  },
  {
    path: '/',
    redirect: '/dashboard',
    component: () => import('@/views/common/Layout.vue'),
    meta: { title: '工作台', requiresAuth: true },
    children: [
      {
        path: '/dashboard',
        name: 'Dashboard',
        component: () => import('@/views/common/Dashboard.vue'),
        meta: { title: '首页' },
      },
      // 运营管理员专属路由
      {
        path: '/admin/schools',
        name: 'SchoolManage',
        component: () => import('@/views/admin/SchoolManage.vue'),
        meta: { title: '院校管理', roles: ['OP_ADMIN'] },
      },
      {
        path: '/admin/student-statistics',
        name: 'StudentStatistics',
        component: () => import('@/views/admin/StudentStatistics.vue'),
        meta: { title: '考生统计', roles: ['OP_ADMIN'] },
      },
      // 院校通用路由
      {
        path: '/students',
        name: 'StudentList',
        component: () => import('@/views/school/StudentList.vue'),
        meta: { title: '考生列表', roles: ['SCHOOL_ADMIN', 'SCHOOL_STAFF', 'OP_ADMIN'] },
      },
      {
        path: '/students/:id',
        name: 'StudentDetail',
        component: () => import('@/views/school/StudentDetail.vue'),
        meta: { title: '考生详情', roles: ['SCHOOL_ADMIN', 'SCHOOL_STAFF', 'OP_ADMIN'] },
      },
      {
        path: '/majors',
        name: 'MajorConfig',
        component: () => import('@/views/school/MajorConfig.vue'),
        meta: { title: '专业配置', roles: ['SCHOOL_ADMIN', 'SCHOOL_STAFF'] },
      },
      {
        path: '/quota',
        name: 'QuotaManage',
        component: () => import('@/views/school/QuotaManage.vue'),
        meta: { title: '名额管理', roles: ['SCHOOL_ADMIN', 'SCHOOL_STAFF'] },
      },
      {
        path: '/score-lines',
        name: 'ScoreLineConfig',
        component: () => import('@/views/school/ScoreLineConfig.vue'),
        meta: { title: '分数线配置', roles: ['SCHOOL_ADMIN', 'SCHOOL_STAFF'] },
      },
      {
        path: '/brochure',
        name: 'SchoolBrochureConfig',
        component: () => import('@/views/school/SchoolBrochureConfig.vue'),
        meta: { title: '招生简章', roles: ['SCHOOL_ADMIN', 'SCHOOL_STAFF'] },
      },
      {
        path: '/supplement',
        name: 'SupplementRounds',
        component: () => import('@/views/school/SupplementRounds.vue'),
        meta: { title: '录取轮次', roles: ['OP_ADMIN', 'SCHOOL_ADMIN', 'SCHOOL_STAFF'] },
      },
      {
        path: '/supplements',
        name: 'SupplementManage',
        component: () => import('@/views/school/SupplementManage.vue'),
        meta: { title: '补录管理', roles: ['OP_ADMIN', 'SCHOOL_ADMIN', 'SCHOOL_STAFF'] },
      },
      {
        path: '/verification',
        name: 'ScoreVerification',
        component: () => import('@/views/school/ScoreVerification.vue'),
        meta: { title: '成绩核验', roles: ['OP_ADMIN', 'SCHOOL_ADMIN', 'SCHOOL_STAFF'] },
      },
      {
        path: '/checkin',
        name: 'CheckinManage',
        component: () => import('@/views/school/CheckinManage.vue'),
        meta: { title: '报到管理', roles: ['SCHOOL_ADMIN', 'SCHOOL_STAFF'] },
      },
      {
        path: '/statistics',
        name: 'Statistics',
        component: () => import('@/views/school/Statistics.vue'),
        meta: { title: '数据统计', roles: ['OP_ADMIN', 'SCHOOL_ADMIN', 'SCHOOL_STAFF'] },
      },
      {
        path: '/accounts',
        name: 'AccountManage',
        component: () => import('@/views/admin/AccountManage.vue'),
        meta: { title: '账号管理', roles: ['OP_ADMIN', 'SCHOOL_ADMIN'] },
      },
      {
        path: '/roles',
        name: 'RoleManage',
        component: () => import('@/views/admin/RoleManage.vue'),
        meta: { title: '角色权限', roles: ['OP_ADMIN'] },
      },
      {
        path: '/admin/audit-logs',
        name: 'AuditLogList',
        component: () => import('@/views/admin/AuditLogList.vue'),
        meta: { title: '操作日志', roles: ['OP_ADMIN'] },
      },
      {
        path: '/admin/params',
        name: 'ParamConfig',
        component: () => import('@/views/admin/ParamConfig.vue'),
        meta: { title: '系统参数', roles: ['OP_ADMIN'] },
      },
      {
        path: '/notifications',
        name: 'Notifications',
        component: () => import('@/views/common/Notifications.vue'),
        meta: { title: '站内通知' },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/common/NotFound.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 全局路由守卫
router.beforeEach(async (to, _from, next) => {
  document.title = (to.meta.title as string || '院校管理平台') + ' - 院校管理平台'

  if (to.meta.requiresAuth === false) {
    next()
    return
  }

  const authStore = useAuthStore()
  if (!authStore.token) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
    return
  }

  // 页面刷新后，token 存在但 userInfo 为空，等待加载完成
  if (!authStore.userInfo) {
    try {
      await authStore.fetchUserInfo()
    } catch {
      // token 失效，跳转登录
      next({ name: 'Login' })
      return
    }
  }

  const allowedRoles = to.meta.roles as string[] | undefined
  if (allowedRoles && allowedRoles.length > 0 && !allowedRoles.includes(authStore.role)) {
    ElMessage.error('您没有权限访问该页面')
    next({ name: 'Dashboard' })
    return
  }

  next()
})

export default router
