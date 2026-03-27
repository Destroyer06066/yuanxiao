import axios from 'axios'
import type { AxiosInstance, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

export interface Result<T = any> {
  code: number
  message: string
  data: T
  requestId: string
  timestamp: number
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  pageSize: number
}

const axiosInstance: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },
})

// 请求拦截器：注入 Token
axiosInstance.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('access_token')
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：处理错误 + Token 续期
axiosInstance.interceptors.response.use(
  (response: AxiosResponse<Result>) => {
    const { data } = response

    // X-New-Token：后端自动续期
    const newToken = response.headers['x-new-token']
    if (newToken) {
      localStorage.setItem('access_token', newToken as string)
    }

    if (data.code !== 0) {
      ElMessage.error(data.message || '请求失败')
      return Promise.reject(new Error(data.message))
    }

    return response
  },
  (error) => {
    if (error.response) {
      const { status, data } = error.response

      if (status === 401) {
        localStorage.removeItem('access_token')
        router.push({ name: 'Login' })
        ElMessage.error('登录已过期，请重新登录')
        return Promise.reject(error)
      }

      if (status === 403) {
        ElMessage.error(data?.message || '权限不足')
        return Promise.reject(error)
      }

      if (status >= 500) {
        ElMessage.error('服务器错误，请稍后重试')
        return Promise.reject(error)
      }

      ElMessage.error(data?.message || '请求失败')
    } else {
      ElMessage.error('网络错误，请检查网络连接')
    }

    return Promise.reject(error)
  }
)

export default axiosInstance
