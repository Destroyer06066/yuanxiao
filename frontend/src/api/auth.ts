import axios from './axios'
import type { Result } from './axios'

export interface LoginRequest {
  username: string
  password: string
}

export interface UserInfo {
  accountId: string
  username: string
  role: string
  schoolId: string | null
  realName: string
  requirePasswordChange: boolean
  accessToken: string
}

export interface LoginResponse {
  accessToken: string
  role: string
  schoolId: string | null
  realName: string
  requirePasswordChange: boolean
}

export const login = (data: LoginRequest) =>
  axios.post<Result<LoginResponse>>('/v1/auth/login', data)

export const getCurrentUser = () =>
  axios.get<Result<UserInfo>>('/v1/auth/me')

export const logout = () =>
  axios.post('/v1/auth/logout')

export const forceChangePassword = (newPassword: string) =>
  axios.put('/v1/auth/password/force-change', { newPassword })

export const sendResetCode = (username: string) =>
  axios.post('/v1/auth/password/reset/send-code', { username })

export const resetPassword = (data: { username: string; code: string; newPassword: string }) =>
  axios.post('/v1/auth/password/reset/confirm', data)
