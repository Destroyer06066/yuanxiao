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
  admissionMajorId: string
  pushRound: number
  pushedAt: string
  schoolId?: string
  schoolName?: string
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

export const batchReject = (pushIds: string[]) =>
  axios.post('/v1/students/batch-reject', { pushIds })

export interface BatchAdmitRequest {
  pushIds: string[]
  majorId: string
  remark?: string
}

export const batchAdmit = (data: BatchAdmitRequest) =>
  axios.post('/v1/students/batch-admit', data)

export const searchStudents = (keyword: string) =>
  axios.get<Result<Student[]>>('/v1/students/search', { params: { keyword } })

export const getTimeline = (pushId: string) =>
  axios.get<Result<any[]>>(`/v1/students/${pushId}/timeline`)

export const finalizeAdmission = (pushId: string) =>
  axios.post('/v1/students/finalize', { pushId })

export const revokeAdmission = (pushId: string) =>
  axios.post('/v1/students/revoke', { pushId })

export const receiveMaterial = (pushId: string, note?: string) =>
  axios.post('/v1/checkins/material-receive', { pushId, note })

export const confirmCheckin = (pushId: string, note?: string) =>
  axios.post('/v1/checkins/checkin', { pushId, note })

export const getStudentStatusCounts = () =>
  axios.get<Result<Array<{ status: string; label: string; color: string; count: number }>>>('/v1/statistics/status-dist')
