import request from '../../utils/axios'

export interface LoginForm {
  tenantCode?: string
  username: string
  password: string
}

/** 管理端登录 */
export function adminLogin(data: { username: string; password: string }) {
  return request.post('/platform/login', data)
}

/** 租户端登录 */
export function tenantLogin(data: { tenantId: number; username: string; password: string }) {
  return request.post('/auth/login', data)
}

/** 通用登录（兼容旧版） */
export function login(data: { username: string; password: string }) {
  return request.post('/auth/login', data)
}

/** 重置密码 */
export function resetPassword(username: string) {
  return request.post('/auth/reset-password', { username })
}
