import request from '../../utils/axios'

/** 获取全部菜单树 */
export function getAllMenus() {
  return request.get('/menu/all')
}

/** 获取角色菜单树 */
export function getMenuTreeByRole(roleCode: string) {
  return request.get('/menu/tree', { params: { roleCode } })
}

/** 新增菜单 */
export function addMenu(data: any) {
  return request.post('/menu', data)
}

/** 更新菜单 */
export function updateMenu(id: number, data: any) {
  return request.put(`/menu/${id}`, data)
}

/** 删除菜单 */
export function deleteMenu(id: number) {
  return request.delete(`/menu/${id}`)
}

/** 获取角色菜单ID列表 */
export function getRoleMenuIds(roleCode: string) {
  return request.get(`/menu/role/${roleCode}`)
}

/** 设置角色菜单 */
export function setRoleMenus(roleCode: string, menuIds: number[]) {
  return request.put(`/menu/role/${roleCode}`, menuIds)
}
