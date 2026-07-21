import request from '../../utils/axios'

/** 分页查询租户列表 */
export function getTenantList(page = 1, size = 20, keyword = '') {
  return request.get('/platform/tenants/list', { params: { page, size, keyword } })
}

/** 获取租户详情 */
export function getTenantDetail(id: number) {
  return request.get(`/platform/tenants/${id}`)
}

/** 新增租户 */
export function addTenant(data: any) {
  return request.post('/platform/tenants', data)
}

/** 修改租户 */
export function updateTenant(data: any) {
  return request.put('/platform/tenants', data)
}

/** 删除租户 */
export function deleteTenant(id: number) {
  return request.delete(`/platform/tenants/${id}`)
}

/** 租户续费/调整套餐 */
export function renewTenantPackage(id: number, data: { packageId: number; durationDays?: number; operateType?: string; remark?: string }) {
  return request.put(`/platform/tenants/${id}/package`, data)
}
