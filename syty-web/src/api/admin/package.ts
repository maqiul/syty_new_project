import request from '../../utils/axios'

export interface PackageInfo {
  id?: number
  name: string
  price: number
  maxUsers?: number
  durationDays?: number
  features?: string
  status?: number
}

export function getPlatformPackagesList(params?: any) {
  return request.get<PackageInfo[]>('/platform/packages/list', { params })
}

export function addPlatformPackage(data: PackageInfo) {
  return request.post('/platform/packages', data)
}

export function updatePlatformPackage(data: PackageInfo) {
  return request.put(`/platform/packages/${data.id}`, data)
}

export function deletePlatformPackage(id: number) {
  return request.delete(`/platform/packages/${id}`)
}
