import request from '../../utils/axios'

export interface SysUser {
  id?: number
  username: string
  password?: string
  realName: string
  phone?: string
  role?: string
  tenantId?: number | null
  status?: number
  createdAt?: string
}

export function getUserList(params?: any) { return request.get('/user/list', { params }) }
export function getUser(id: number) { return request.get(`/user/${id}`) }
export function updateUser(data: SysUser) { return request.put(`/user/${data.id}`, data) }

/** 获取所有穿线师列表 */
export function getStringers() { return request.get('/user/stringers') }
