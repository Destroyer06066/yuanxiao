import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login, getCurrentUser, logout } from '@/api/auth'
import type { LoginRequest, UserInfo } from '@/api/auth'
import router from '@/router'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(localStorage.getItem('access_token') || '')
  const userInfo = ref<UserInfo | null>(null)

  const role = computed(() => userInfo.value?.role || '')
  const schoolId = computed(() => userInfo.value?.schoolId || '')
  const realName = computed(() => userInfo.value?.realName || '')
  const isOpAdmin = computed(() => role.value === 'OP_ADMIN')

  async function loginAction(form: LoginRequest) {
    const res = await login(form)
    const data = res.data.data as LoginResponse
    token.value = data.accessToken
    localStorage.setItem('access_token', data.accessToken)
    userInfo.value = data
    return data
  }

  async function fetchUserInfo() {
    if (!token.value) return
    const res = await getCurrentUser()
    userInfo.value = res.data.data as UserInfo
  }

  async function logoutAction() {
    await logout()
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('access_token')
    router.push({ name: 'Login' })
  }

  return {
    token,
    userInfo,
    role,
    schoolId,
    realName,
    isOpAdmin,
    loginAction,
    fetchUserInfo,
    logoutAction,
  }
})
