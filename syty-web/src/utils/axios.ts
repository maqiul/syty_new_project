import axios from 'axios'
import { message } from 'ant-design-vue'
import router from '../router'
import type { useUserStore } from '../store/user'

// ============ 类型定义 ============

export interface ApiResponse<T = any> {
  code: number
  msg: string
  data: T
}

export interface PageData<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

// 用户信息类型（与 store 保持一致，向后兼容旧代码引用）
export interface UserInfo {
  id: number
  username: string
  realName: string
  role: 'SUPER_ADMIN' | 'TENANT_ADMIN' | 'STAFF'
  tenantId: number | null
  tenantName: string | null
  permissions: string[]
  enableBadmintonTournament?: boolean
  enableTennisTournament?: boolean
}

/* ========== 模块级变量：由 main.ts 注入 store 实例 ========== */
let userStore: ReturnType<typeof useUserStore> | null = null

/** main.ts 调用此方法注入 store 实例 */
export function setUserStore(store: ReturnType<typeof useUserStore>): void {
  userStore = store
}

/* ========== 创建 request 实例 ========== */
const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

// ============ 请求拦截器 ============

request.interceptors.request.use(
  config => {
    // 公开接口不传 token（客户登记等）
    const isPublic = config.url?.includes('/order/customer') ||
                     config.url?.includes('/tennis/tournament/customer') ||
                     config.url?.includes('/string-info')
    if (!isPublic) {
      // 从 store 读取 token
      if (userStore?.token) {
        // 动态判断 Token Header Key：
        // - 超管 或 请求路径含 /platform → plat-token
        // - 其他（租户端） → tenant-token
        const isPlatform =
          userStore.userInfo?.role === 'SUPER_ADMIN' ||
          config.url?.includes('/platform')
        const tokenHeaderKey = isPlatform ? 'plat-token' : 'tenant-token'
        config.headers[tokenHeaderKey] = userStore.token
      }
      // 从 store 读取租户ID
      if (userStore?.tenantId) {
        config.headers['X-Tenant-Id'] = userStore.tenantId
      }
    }
    return config
  },
  error => Promise.reject(error)
)

// ============ 响应拦截器 ============

request.interceptors.response.use(
  response => {
    const res = response.data as ApiResponse
    if (res.code === 401) {
      // token 过期/无效：清空 store 状态，跳登录
      userStore?.logout()
      router.push('/login')
      message.error('登录已过期，请重新登录')
      return Promise.reject(new Error('未授权'))
    }
    if (res.code === 403) {
      message.error(res.msg || '没有权限')
      return Promise.reject(new Error(res.msg))
    }
    if (res.code !== 200) {
      message.error(res.msg || '请求失败')
      return Promise.reject(new Error(res.msg))
    }
    return res as any
  },
  error => {
    if (error.response?.status === 401) {
      userStore?.logout()
      router.push('/login')
    } else if (error.response?.status === 403) {
      message.error('没有权限执行此操作')
    } else {
      message.error(error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default request
