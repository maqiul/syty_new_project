import request from '../../utils/axios'

export interface FinanceStats {
  todayOrderCount: number
  todayRevenue: number
  monthRevenue: number
}

export interface PaymentRecord {
  id?: number
  orderId: number
  payMethod: 'CASH' | 'WECHAT' | 'ALIPAY' | 'TRANSFER' | 'OTHER'
  amount: number
  status: number
  operatorName?: string
  remark?: string
  createdAt?: string
}

export function getFinanceStats(params?: any) { return request.get('/finance/stats', { params }) }
export function getPaymentRecordList(params?: any) { return request.get('/finance/payment-record', { params }) }
