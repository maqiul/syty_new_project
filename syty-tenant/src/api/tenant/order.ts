import request from '../../utils/axios'

export interface StringingOrder {
  id?: number
  orderNo: string
  shopId: number
  shopName?: string
  playerId?: number
  playerName?: string
  racketId?: number
  racketName?: string
  mainStringId?: number
  mainStringName?: string
  crossStringId?: number
  crossStringName?: string
  mainTension: number
  crossTension: number
  status: number
  totalPrice?: number
  remark?: string
  operatorId?: number
  operatorName?: string
  stringerId?: number
  stringerName?: string
  createdAt?: string
  completedAt?: string
}

export function getOrderPage(params?: any) { return request.get('/order/page', { params }) }
export const getStringingOrderPage = getOrderPage
export function getOrder(id: number) { return request.get(`/order/${id}`) }
export const getOrderDetail = getOrder
export function createOrder(data: StringingOrder) { return request.post('/order', data) }
export function updateOrder(data: StringingOrder) { return request.put(`/order/${data.id}`, data) }
export function completeOrder(id: number) { return request.post(`/order/${id}/complete`) }
export function startStringing(id: number) { return request.put(`/order/${id}/start`) }
export function deleteOrder(id: number) { return request.delete(`/order/${id}`) }
export function printOrder(id: number) { return request.post(`/order/${id}/print`) }
export function exportOrders(params?: any) { return request.get('/order/export', { params, responseType: 'blob' }) }

// ============ 网球订单 ============
export interface TennisOrder extends StringingOrder {}

export function getTennisOrderPage(params?: any) { return request.get('/tennis/order/page', { params }) }
export function getTennisOrder(id: number) { return request.get(`/tennis/order/${id}`) }
export function createTennisOrder(data: TennisOrder) { return request.post('/tennis/order', data) }
export function updateTennisOrder(data: TennisOrder) { return request.put(`/tennis/order/${data.id}`, data) }
export function completeTennisOrder(id: number) { return request.put(`/tennis/order/${id}/complete`) }
export function deleteTennisOrder(id: number) { return request.delete(`/tennis/order/${id}`) }
