import request from '../../utils/axios'

export interface LoginForm {
  username: string
  password: string
}

/**
 * 租户端登录（员工模式）
 * - 请求体：{ username, password }
 * - 请求头：X-Tenant-Code: 门店编码
 */
export function tenantLogin(data: { username: string; password: string }, tenantCode: string) {
  return request.post('/tenant/login', data, {
    headers: { 'X-Tenant-Code': tenantCode }
  })
}

/**
 * 管理端登录（管理员模式）
 * - 请求体：{ username, password }
 * - 不携带租户相关 Header
 */
export function adminLogin(data: { username: string; password: string }) {
  return request.post('/tenant/login', data)
}

/** 通用登录（兼容旧版） */
export function login(data: { username: string; password: string }) {
  return request.post('/auth/login', data)
}

/** 重置密码 */
export function resetPassword(username: string) {
  return request.post('/auth/reset-password', { username })
}
