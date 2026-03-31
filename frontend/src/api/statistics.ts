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

export const getTrend = (params?: { year?: number; schoolId?: string }) =>
  axios.get<Result<MonthlyTrend[]>>('/v1/statistics/trend', { params })

export const getStatusDistribution = () =>
  axios.get<Result<StatusDistItem[]>>('/v1/statistics/status-dist')

export const getMajorRanking = (params?: { limit?: number }) =>
  axios.get<Result<MajorRankingItem[]>>('/v1/statistics/major-ranking', { params })

export const getAvailableYears = () =>
  axios.get<Result<number[]>>('/v1/statistics/years')
