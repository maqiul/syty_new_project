import request from '@/utils/axios'

export interface InventoryItem {
  id: number
  name: string
  spec: string
  stock: number
  threshold: number
  price: number
  unit: string
}

export function getInventoryList(params: { page: number; size: number }) {
  return request.get('/tenant/inventory/list', { params })
}

// ============ 库存台账 ============
export interface StockItem {
  id: number
  shopId?: number
  stringInfoId?: number
  stringName: string
  spec: string
  stock: number
  reserved: number
  available: number
  warningThreshold: number
  unit: string
}

export function getStockList(params?: any) {
  return request.get('/api/v1/tenant/inventory/stock', { params })
}

export function getStockPage(params?: any) {
  return request.get('/api/v1/tenant/inventory/stock', { params })
}

export function inboundStock(data: { stringInfoId: number; quantity: number; remark?: string }) {
  return request.post('/api/inbound', data)
}

export function adjustStock(data: { stockId: number; newStock: number; reason: string }) {
  return request.post('/api/inbound', { ...data, changeType: 'ADJUST' })
}

export function getAllStringInfos(params?: any) {
  return request.get('/api/string/list', { params })
}

// ============ 库存流水 ============
export interface StockLog {
  id: number
  shopId: number
  stringId: number
  changeType: string
  quantity: number
  beforeQuantity: number
  afterQuantity: number
  orderId?: number
  orderNo?: string
  operatorId?: number
  operatorName?: string
  remark?: string
  createdAt?: string
}

export function getStockLogPage(params?: any) {
  return request.get('/api/stock-log/page', { params })
}

// ============ 盘点管理 ============
export interface InventoryCheck {
  id: number
  checkNo: string
  createdAt: string
  operatorName?: string
  status: string
  remark?: string
}

export interface CheckItem {
  id?: number
  checkId?: number
  stringId?: number
  stringName?: string
  bookQuantity: number
  actualQuantity: number
  difference?: number
}

export function getCheckList(params?: any) {
  return request.get('/v1/tenant/inventory-check', { params })
}

export function createCheck(data?: { remark?: string }) {
  return request.post('/v1/tenant/inventory-check', null, { params: data })
}

export function getCheckItems(checkId: number) {
  return request.get(`/v1/tenant/inventory-check/${checkId}/items`)
}

export function submitCheckItems(checkId: number, items: CheckItem[]) {
  return request.put(`/v1/tenant/inventory-check/${checkId}/items`, items)
}

export function confirmCheck(checkId: number) {
  return request.post(`/v1/tenant/inventory-check/${checkId}/confirm`)
}
