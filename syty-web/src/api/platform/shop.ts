import request from '../../utils/axios'

/** 分页查询门店列表 */
export function getShopList(page = 1, size = 10, keyword = '') {
  return request.get('/platform/shops/list', { params: { page, size, keyword } })
}

/** 新增门店 */
export function addShop(data: { tenantCode: string; shopName: string; packageId: number; shopCode?: string }) {
  return request.post('/platform/shops', data)
}

/** 门店续费 */
export function renewShop(id: number, data: { packageId?: number; days?: number }) {
  return request.put(`/platform/shops/${id}/renew`, data)
}
