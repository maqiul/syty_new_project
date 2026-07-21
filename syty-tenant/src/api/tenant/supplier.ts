/**
 * 供应商管理 API
 */
import request from '@/utils/axios'

export function getSupplierPage(params: any) {
  return request.get('/supplier/page', { params })
}

export function getSupplierList(params?: any) {
  return request.get('/supplier/list', { params })
}

export function getSupplierById(id: number) {
  return request.get(`/supplier/${id}`)
}

export function addSupplier(data: any) {
  return request.post('/supplier', data)
}

export function updateSupplier(data: any) {
  return request.put('/supplier', data)
}

export function deleteSupplier(id: number) {
  return request.delete(`/supplier/${id}`)
}
