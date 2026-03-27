import axios from './axios'
import type { Result } from './axios'

export interface PermissionItem {
  id: string
  module: string
  action: string
  label: string
  isExplicit: boolean
  isRestricted: boolean
}

export interface PermissionModule {
  module: string
  moduleLabel: string
  permissions: PermissionItem[]
}

export const getPermissionTree = () =>
  axios.get<Result<PermissionModule[]>>('/v1/permissions')
