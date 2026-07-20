import request from '../../utils/axios'

export interface Tenant {
  id?: number
  name: string
  code: string
  contactPerson?: string
  phone?: string
  status?: number
  enableBadmintonTournament?: number
  enableTennisTournament?: number
  createdAt?: string
}

export function getTenantPage(params?: any) { return request.get('/tenant/page', { params }) }
export function getTenantList(params?: any) { return request.get('/tenant/list', { params }) }
export function getTenant(id: number) { return request.get(`/tenant/${id}`) }
export function addTenant(data: Tenant) { return request.post('/tenant', data) }
export function updateTenant(data: Tenant) { return request.put(`/tenant/${data.id}`, data) }
export function deleteTenant(id: number) { return request.delete(`/tenant/${id}`) }

/** 平台端专用 — 租户列表分页查询（带 plat-token） */
export interface PlatformTenantInfo {
  id: number
  name: string
  tenantCode?: string
  packageId?: number
  packageName?: string
  packageExpiredAt?: string
  packageStatus?: string
  contact?: string
  phone?: string
  status: number
  createdAt?: string
}

export interface PlatformTenantListParams {
  page?: number
  size?: number
  keyword?: string
}

export function getPlatformTenantsList(params?: PlatformTenantListParams) {
  return request.get<{
    records: PlatformTenantInfo[]
    total: number
    size: number
    current: number
    pages: number
  }>('/platform/tenants/list', { params })
}

export function deletePlatformTenant(id: number) {
  return request.delete(`/platform/tenants/${id}`)
}

export function addPlatformTenant(data: Partial<PlatformTenantInfo>) {
  return request.post('/platform/tenants', data)
}

export function updatePlatformTenant(data: Partial<PlatformTenantInfo>) {
  return request.put('/platform/tenants', data)
}

/** 租户续费/换套餐 */
export function updateTenantPackage(id: number, data: { packageId: number; durationDays: number }) {
  return request.put(`/platform/tenants/${id}/package`, data)
}
