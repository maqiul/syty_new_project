import request from '../../utils/axios'

export interface OperateLog {
  id?: number
  userId?: number
  username?: string
  module?: string
  action?: string
  description?: string
  ip?: string
  createdAt?: string
}

export function getOperateLogPage(params?: any) { return request.get('/log/operate/page', { params }) }
export function getPrintLogPage(params?: any) { return request.get('/log/print/page', { params }) }
