/**
 * 供应商管理 API
 */
import request from '@/utils/request'

export function getSupplierPage(params: any) {
  return request.get('/api/supplier/page', { params })
}

export function getSupplierList(params?: any) {
  return request.get('/api/supplier/list', { params })
}

export function getSupplierById(id: number) {
  return request.get(`/api/supplier/${id}`)
}

export function addSupplier(data: any) {
  return request.post('/api/supplier', data)
}

export function updateSupplier(data: any) {
  return request.put('/api/supplier', data)
}

export function deleteSupplier(id: number) {
  return request.delete(`/api/supplier/${id}`)
}
