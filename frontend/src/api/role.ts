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

export interface Role {
  id: string
  roleKey: string
  name: string
  description: string
  isPreset: boolean
  presetKey: string | null
  status: 'ACTIVE' | 'INACTIVE'
  permissions: PermissionItem[]
}

export interface CreateRoleRequest {
  roleKey: string
  name: string
  description?: string
  presetKey: string
  status?: string
}

export interface UpdateRoleRequest {
  name?: string
  description?: string
  status?: string
}

export const getRoles = () =>
  axios.get<Result<Role[]>>('/v1/roles')

export const getRole = (id: string) =>
  axios.get<Result<Role>>(`/v1/roles/${id}`)

export const createRole = (data: CreateRoleRequest) =>
  axios.post<Result<Role>>('/v1/roles', data)

export const updateRole = (id: string, data: UpdateRoleRequest) =>
  axios.put<Result<Role>>(`/v1/roles/${id}`, data)

export const deleteRole = (id: string) =>
  axios.delete(`/v1/roles/${id}`)

export const updateRolePermissions = (id: string, permissionIds: string[]) =>
  axios.put<Result<Role>>(`/v1/roles/${id}/permissions`, { permissionIds })
