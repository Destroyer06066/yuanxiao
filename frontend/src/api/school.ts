import axios from './axios'
import type { Result, PageResult } from './axios'

export interface School {
  schoolId: string
  schoolName: string
  schoolShortName: string
  province: string
  schoolType: string
  contactName: string
  contactPhone: string
  contactEmail: string
  website?: string
  remark?: string
  status: string
  createdAt?: string
}

export interface SchoolQuery {
  keyword?: string
  province?: string
  schoolType?: string
  status?: string
  page?: number
  pageSize?: number
}

export interface CreateSchoolRequest {
  schoolName: string
  schoolShortName: string
  province: string
  schoolType: string
  contactName: string
  contactPhone: string
  contactEmail: string
  website?: string
  remark?: string
}

export type UpdateSchoolRequest = CreateSchoolRequest

export const getSchools = (params?: SchoolQuery) =>
  axios.get<Result<PageResult<School>>>('/v1/admin/schools', { params })

export const createSchool = (data: CreateSchoolRequest) =>
  axios.post<Result<{ schoolId: string }>>('/v1/admin/schools', data)

export const updateSchool = (schoolId: string, data: UpdateSchoolRequest) =>
  axios.put(`/v1/admin/schools/${schoolId}`, data)

export const toggleSchoolStatus = (schoolId: string, status: string) =>
  axios.patch(`/v1/admin/schools/${schoolId}/status`, { status })
