import request from '../../utils/axios'

/** 获取套餐列表 */
export function getPackageList() {
  return request.get('/platform/packages/list')
}

/** 新增套餐 */
export function addPackage(data: any) {
  return request.post('/platform/packages', data)
}

/** 修改套餐 */
export function updatePackage(id: number, data: any) {
  return request.put(`/platform/packages/${id}`, data)
}

/** 删除套餐 */
export function deletePackage(id: number) {
  return request.delete(`/platform/packages/${id}`)
}
