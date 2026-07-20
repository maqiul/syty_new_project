import request from '../../utils/axios'

// ============ 绩效统计 ============
export interface PerformanceStats {
  totalOrders: number
  totalStringingFee: number
  avgDuration: number
  newMembersThisMonth: number
  dailyOrdersTrend: { date: string; count: number }[]
  stringerRanking: { name: string; orders: number; totalAmount: number; avgDuration: number }[]
}

export function getPerformanceStats(params?: { period?: string }) {
  return request.get<PerformanceStats>('/api/v1/tenant/performance/stats', { params })
}

// ============ 穿线师排行 ============
export interface StringerRank {
  name: string
  orders: number
  totalAmount: number
  avgDuration: number
}

export function getStringerRank(params?: { period?: string; limit?: number }) {
  return request.get<StringerRank[]>('/api/v1/tenant/performance/stringer-rank', { params })
}
