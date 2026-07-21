import request from '../../utils/axios'

/** 获取平台统计概览 */
export function getDashboardStats() {
  return request.get('/platform/dashboard/stats')
}

/** 获取最近操作日志 */
export function getDashboardLogs(limit = 10) {
  return request.get('/platform/dashboard/logs', { params: { limit } })
}
