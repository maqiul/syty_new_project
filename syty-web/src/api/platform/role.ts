import request from '../../utils/axios'

/** 角色列表 */
export function getRoleList() {
  return request.get('/role/list')
}

/** 新增角色 */
export function addRole(data: any) {
  return request.post('/role/add', data)
}

/** 更新角色 */
export function updateRole(data: any) {
  return request.post('/role/update', data)
}

/** 删除角色 */
export function deleteRole(id: number) {
  return request.post('/role/delete', null, { params: { id } })
}
