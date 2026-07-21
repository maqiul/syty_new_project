import request from '../../utils/axios'

/** 权限列表（扁平） */
export function getPermissionList() {
  return request.get('/permission')
}

/** 权限树 */
export function getPermissionTree() {
  return request.get('/permission/tree')
}

/** 新增权限 */
export function addPermission(data: any) {
  return request.post('/permission', data)
}

/** 更新权限 */
export function updatePermission(id: number, data: any) {
  return request.put(`/permission/${id}`, data)
}

/** 删除权限 */
export function deletePermission(id: number) {
  return request.delete(`/permission/${id}`)
}

/** 获取角色权限码 */
export function getRolePermissionCodes(roleCode: string) {
  return request.get(`/permission/role/${roleCode}/codes`)
}

/** 设置角色权限 */
export function setRolePermissions(roleCode: string, codes: string[]) {
  return request.put(`/permission/role/${roleCode}`, codes)
}
