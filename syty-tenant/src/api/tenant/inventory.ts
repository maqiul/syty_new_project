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
