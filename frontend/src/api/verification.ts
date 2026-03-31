import axios from './axios'
import type { Result, PageResult } from './axios'

export interface VerificationLog {
  verificationId: string
  pushId: string
  candidateName: string
  majorName: string
  certificateNo: string
  result: string
  action: string
  note: string
  operatorName: string
  createdAt: string
}

export interface VerificationQuery {
  pushId?: string
  result?: string
  page?: number
  pageSize?: number
}

export const getVerifications = (params?: VerificationQuery) =>
  axios.get<Result<PageResult<VerificationLog>>>('/v1/verifications', { params })

export const submitVerification = (data: {
  pushId: string
  certificateNo: string
  result: string
  note?: string
}) => axios.post('/v1/verifications', data)

export const batchVerify = (data: {
  items: { pushId: string; certificateNo: string; result: string }[]
}) => axios.post('/v1/verifications/batch', data)
