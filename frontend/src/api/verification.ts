import axios from './axios'
import type { Result, PageResult } from './axios'

// ========== 证书核验 ==========

export interface CertificateVerificationRequest {
  certificateNo: string
  verifyCode: string
}

export interface SubjectScore {
  subject: string
  language: string
  score: number
}

export interface CertificateVerificationResponse {
  valid: boolean
  testCenterCode?: string
  testDate?: string
  testFormat?: string
  certificateNo?: string
  name?: string
  gender?: string
  dateOfBirth?: string
  nationality?: string
  subjectScores?: SubjectScore[]
  verifyCode?: string
  issueDate?: string
  issueOrganization?: string
  message?: string
}

// 证书核验
export const verifyCertificate = (data: CertificateVerificationRequest) =>
  axios.post<Result<CertificateVerificationResponse>>('/v1/verifications/verify', data)

// ========== 核验记录 ==========

export interface VerificationLogRecord {
  verificationId: string
  pushId: string
  candidateName: string
  action: string
  certificateNo: string
  result: string
  note: string
  operatorId: string
  createdAt: string
}

export interface VerificationQuery {
  certificateNo?: string
  result?: string
  page?: number
  pageSize?: number
}

// 获取核验记录
export const getVerificationLogs = (params?: VerificationQuery) =>
  axios.get<Result<PageResult<VerificationLogRecord>>>('/v1/verifications', { params })
