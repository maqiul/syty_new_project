import request from '../../utils/axios'

export interface DashboardStats {
  todayOrders: number
  pendingOrders: number
  completedOrders: number
  totalOrders: number
  todayRevenue: number
  totalRevenue: number
  totalShops: number
  totalPlayers: number
  totalTenants: number
  shopCount: number
}

export function getDashboardStats() { return request.get('/dashboard/stats') }
