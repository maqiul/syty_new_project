import request from '../../utils/axios'

// ============ 管理端绩效统计 ============
export interface AdminPerformanceStats {
  totalOrders: number
  totalStringingFee: number
  avgDuration: number
  newMembersThisMonth: number
  dailyOrdersTrend: { date: string; count: number }[]
  stringerRanking: { name: string; orders: number; totalAmount: number; avgDuration: number }[]
  shopCount: number
}

export function getAdminPerformanceStats(params?: { period?: string; shopId?: number }) {
  return request.get<AdminPerformanceStats>('/api/v1/admin/performance/stats', { params })
}

export interface AdminShopInfo {
  id: number
  name: string
}

export function getAdminShopList() {
  return request.get<AdminShopInfo[]>('/api/v1/admin/shops/list')
}
