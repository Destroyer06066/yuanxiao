import axios from './axios'
import type { Result } from './axios'

export interface KpiData {
  totalPushed: number
  admitted: number
  confirmed: number
  checkedIn: number
  totalLastYear: number
  admittedLastYear: number
  confirmedLastYear: number
  checkedInLastYear: number
}

export interface MonthlyTrend {
  month: string
  pending: number
  admitted: number
  confirmed: number
  checkedIn: number
  rejected: number
}

export interface DailyTrend {
  date: string
  admitted: number
}

export interface StatusDistItem {
  status: string
  label: string
  color: string
  count: number
}

export interface MajorRankingItem {
  majorId: string
  majorName: string
  admitted: number
  confirmed: number
  checkedIn: number
}

export const getKpis = (params?: { year?: number; schoolId?: string }) =>
  axios.get<Result<KpiData>>('/v1/statistics/kpis', { params })

export const getTrend = (params?: { year?: number; month?: number; schoolId?: string }) =>
  axios.get<Result<MonthlyTrend[] | DailyTrend[]>>('/v1/statistics/trend', { params })

export const getStatusDistribution = () =>
  axios.get<Result<StatusDistItem[]>>('/v1/statistics/status-dist')

export const getMajorRanking = (params?: { limit?: number }) =>
  axios.get<Result<MajorRankingItem[]>>('/v1/statistics/major-ranking', { params })

export const getAvailableYears = () =>
  axios.get<Result<number[]>>('/v1/statistics/years')

// 新增统计维度 API
export interface RoundDistribution {
  [key: string]: number
}

export interface PushCountDistribution {
  once: number
  twice: number
  three: number
  four: number
  five: number
  sixPlus: number
  total: number
}

export interface GenderDistribution {
  male: number
  female: number
  other: number
  unknown: number
  total: number
}

export interface AgeDistribution {
  under18: number
  age18to25: number
  age26to30: number
  age31to35: number
  age36to40: number
  over41: number
  unknown: number
  total: number
}

export interface SchoolAdmissionRange {
  range: string
  schoolCount: number
  admissionCount: number
}

export const getRoundDistribution = () =>
  axios.get<Result<RoundDistribution>>('/v1/statistics/round-dist')

export const getPushCountDistribution = () =>
  axios.get<Result<PushCountDistribution>>('/v1/statistics/push-count-dist')

export const getSchoolAdmissionRanges = () =>
  axios.get<Result<SchoolAdmissionRange[]>>('/v1/statistics/school-admission-ranges')

export const getGenderDistribution = () =>
  axios.get<Result<GenderDistribution>>('/v1/statistics/gender-dist')

export const getAgeDistribution = () =>
  axios.get<Result<AgeDistribution>>('/v1/statistics/age-dist')
