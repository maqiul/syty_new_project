import request from '../utils/axios'

/** 可用门店信息 */
export interface AvailableShop {
  shopCode: string
  shopName: string
  expiredAt: string
  status: 'ACTIVE' | 'EXPIRED' | 'DISABLED'
}

/** 获取当前租户下可用的门店列表 */
export function getAvailableShops(): Promise<AvailableShop[]> {
  return request.get('/tenant/shops/available')
}
