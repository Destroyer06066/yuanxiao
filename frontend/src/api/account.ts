import axios from './axios'
import type { Result, PageResult } from './axios'

export interface Account {
  accountId: string
  username: string
  realName: string
  role: string
  schoolId: string | null
  schoolName?: string
  status: string
  mustChangePassword: boolean
  createdAt: string
}

export interface AccountQuery {
  keyword?: string
  role?: string
  status?: string
  schoolId?: string
  page?: number
  pageSize?: number
}

export interface CreateAccountRequest {
  username: string
  password: string
  realName: string
  role: string
  schoolId?: string
}

export interface UpdateAccountRequest {
  realName: string
  role: string
  schoolId?: string
}

export interface BatchImportItem {
  username: string
  realName: string
  role: string
  schoolName?: string
  password?: string
}

export interface BatchImportResult {
  username: string
  realName: string
  status: 'pending' | 'success' | 'failed'
  password?: string
  error?: string
}

export const getAccounts = (params?: AccountQuery) =>
  axios.get<Result<PageResult<Account>>>('/v1/accounts', { params })

export const createAccount = (data: CreateAccountRequest) =>
  axios.post<Result<{ accountId: string }>>('/v1/accounts', data)

export const updateAccount = (accountId: string, data: UpdateAccountRequest) =>
  axios.put(`/v1/accounts/${accountId}`, data)

export const resetAccountPassword = (accountId: string) =>
  axios.post(`/v1/accounts/${accountId}/reset-password`)

export const toggleAccountStatus = (accountId: string, status: string) =>
  axios.patch(`/v1/accounts/${accountId}/status`, { status })

export const batchImportAccounts = (data: BatchImportItem[]) =>
  axios.post<Result<BatchImportResult[]>>('/v1/accounts/batch-import', data)
