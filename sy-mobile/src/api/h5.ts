import request from '@/utils/request'

export interface HistoryRecord {
  racketModel: string
  mainPounds: number
  crossPounds: number
  knotType: string
  /** 其他可能字段 */
  [key: string]: any
}

/** 查询历史穿线记录 */
export const getHistory = (phone: string) => {
  return request.get<any, { code: number; data: HistoryRecord | null; message?: string }>(
    '/api/v1/h5/history',
    { params: { phone } }
  )
}

export interface OrderPayload {
  phone: string
  racketModel: string
  mainPounds: number
  crossPounds: number
  knotType: string
  // V1.7
  usePunchCard?: boolean
  punchCardId?: number
}

export interface OrderResponse {
  code: number
  message?: string
  warning?: string
  data?: any
}

/** 提交穿线订单 */
export const submitOrder = (data: OrderPayload) => {
  return request.post<any, OrderResponse>('/api/v1/h5/order', data)
}

/** 查询可用次卡 */
export const getAvailableCards = (phone: string) => {
  return request.get<any, { code: number; data: any[] | null; message?: string }>(
    '/api/v1/h5/member/cards',
    { params: { phone } }
  )
}

/** 订单进度查询（按订单号） */
export const queryOrderByNo = (orderNo: string) => {
  return request.get<any, { code: number; data: any; message?: string }>(
    '/api/order/query',
    { params: { orderNo } }
  )
}

/** 订单进度查询（按手机号） */
export const queryOrderByPhone = (phone: string) => {
  return request.get<any, { code: number; data: any[]; message?: string }>(
    '/api/order/query',
    { params: { phone } }
  )
}
