import axios from './axios'
import type { Result, PageResult } from './axios'

export interface Student {
  pushId: string
  candidateId: string
  candidateName: string
  nationality: string
  totalScore: number
  subjectScores: Record<string, number>
  intention: string
  status: string
  statusDesc: string
  admissionMajor: string
  pushRound: number
  pushedAt: string
}

export interface StudentQuery {
  status?: string[]
  minScore?: number
  maxScore?: number
  intentionKeyword?: string
  nationality?: string
  pushTimeStart?: string
  pushTimeEnd?: string
  majorId?: string
  round?: number
  sort?: string
  order?: string
  page?: number
  pageSize?: number
}

export const queryStudents = (params: StudentQuery) =>
  axios.get<Result<PageResult<Student>>>('/v1/students', { params })

export const getStudent = (pushId: string) =>
  axios.get<Result<Student>>(`/v1/students/${pushId}`)

export interface AdmissionRequest {
  pushId: string
  majorId: string
  remark?: string
}

export interface ConditionalAdmissionRequest {
  pushId: string
  majorId: string
  conditionDesc: string
  conditionDeadline: string
}

export interface RejectRequest {
  pushId: string
  reason?: string
}

export const directAdmission = (data: AdmissionRequest) =>
  axios.post('/v1/students/admit', data)

export const conditionalAdmission = (data: ConditionalAdmissionRequest) =>
  axios.post('/v1/students/conditional', data)

export const finalAdmission = (pushId: string) =>
  axios.post(`/v1/admissions/final/${pushId}`)

export const revokeAdmission = (pushId: string) =>
  axios.post(`/v1/admissions/revoke/${pushId}`)

export const batchReject = (pushIds: string[]) =>
  axios.post('/v1/students/batch-reject', { pushIds })
