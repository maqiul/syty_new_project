import request from '../../utils/axios'

/** 分页查询用户 */
export function getUserPage(page = 1, size = 20, keyword = '', role = '') {
  return request.get('/user/page', { params: { page, size, keyword, role } })
}

/** 新增用户 */
export function addUser(data: any) {
  return request.post('/user', data)
}

/** 修改用户 */
export function updateUser(data: any) {
  return request.put('/user', data)
}

/** 删除用户 */
export function deleteUser(id: number) {
  return request.delete(`/user/${id}`)
}

/** 重置密码 */
export function resetPassword(id: number) {
  return request.put(`/user/${id}/reset-password`)
}

/** 转移用户到新租户 */
export function transferUserTenant(id: number, tenantId: number) {
  return request.put(`/user/${id}/transfer-tenant`, { tenantId })
}
