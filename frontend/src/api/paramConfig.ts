import axios from './axios'
import type { Result } from './axios'

export interface SchoolConfig {
  configId: string
  schoolId: string | null
  configKey: string
  configValue: string
}

export const getGlobalConfigs = () =>
  axios.get<Result<SchoolConfig[]>>('/v1/admin/params/global')

export const updateGlobalConfig = (configKey: string, configValue: string) =>
  axios.put(`/v1/admin/params/global/${configKey}`, { configValue })
