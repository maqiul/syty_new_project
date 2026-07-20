import request from '../../utils/axios'

export interface SysUser {
  id?: number
  username: string
  password?: string
  realName: string
  phone?: string
  role: 'SUPER_ADMIN' | 'TENANT_ADMIN' | 'STAFF'
  tenantId?: number | null
  status?: number
  createdAt?: string
}

export function getUserPage(params?: any) { return request.get('/user/page', { params }) }
export function getUserList(params?: any) { return request.get('/user/list', { params }) }
export function getUser(id: number) { return request.get(`/user/${id}`) }
export function addUser(data: SysUser) { return request.post('/user', data) }
export function updateUser(data: SysUser) { return request.put(`/user/${data.id}`, data) }
export function deleteUser(id: number) { return request.delete(`/user/${id}`) }

export function getStringers() { return request.get('/user/stringers') }

export interface OperateLog {
  id?: number
  userId?: number
  username?: string
  module?: string
  action?: string
  description?: string
  ip?: string
  createdAt?: string
}

export function getOperateLogPage(params?: any) { return request.get('/log/operate/page', { params }) }
