import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '../utils/axios'
import { adminLogin as apiAdminLogin } from '../api/common/auth'
import { resetDynamicRoutes } from '../router'

// ============ 类型定义 ============

export type UserRole = 'SUPER_ADMIN' | 'TENANT_ADMIN' | 'STAFF'

export interface UserInfo {
  id: number
  username: string
  realName: string
  role: UserRole
  tenantId: number | null
  tenantName: string | null
  permissions: string[]
  enableBadmintonTournament?: boolean
  enableTennisTournament?: boolean
}

/** 后端登录接口返回格式 */
export interface LoginResponse {
  token: string
  tokenName: string
  userId: number
  username: string
}

/** 后端菜单项（路由动态生成用） */
export interface BackendMenuItem {
  id: number
  name: string
  path: string
  icon?: string
  permission?: string
  children?: BackendMenuItem[]
  component?: string
}

/** 前端侧边栏菜单项 */
export interface MenuItem {
  key: string
  label: string
  icon?: string
  permission?: string
  path?: string
  component?: string
  menuType?: string  // DIRECTORY | MENU | BUTTON
  children?: MenuItem[]
}

/** 将后端菜单树转换为前端 MenuItem 格式 */
function convertBackendMenus(menus: any[]): MenuItem[] {
  return menus.map((item: any) => ({
    key: item.path || String(item.id),
    label: item.menuName || item.menu_name || item.name || '',
    icon: item.icon || undefined,
    permission: item.permission || undefined,
    path: item.path || undefined,
    component: item.component || undefined,
    menuType: item.menuType || undefined,
    children: item.children ? convertBackendMenus(item.children) : undefined,
  }))
}

// ============ Store ============

export const useUserStore = defineStore('user', () => {
  // ---- State ----
  // 核心修复：初始化时从 localStorage 读取 token，防止刷新丢失
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<UserInfo | null>(null)
  const roleCodes = ref<string[]>([])
  const menus = ref<MenuItem[]>([])
  const permissions = ref<string[]>([])
  const isLoading = ref(false)

  // ---- Getters ----
  const isLoggedIn = computed(() => !!token.value)
  const isSuperAdmin = computed(() =>
    roleCodes.value.includes('SUPER_ADMIN') || userInfo.value?.role === 'SUPER_ADMIN'
  )
  const userName = computed(() => userInfo.value?.realName || userInfo.value?.username || '')
  const userRole = computed(() => userInfo.value?.role || 'STAFF')
  const tenantId = computed(() => userInfo.value?.tenantId)
  const tenantName = computed(() => userInfo.value?.tenantName)

  /** 检查是否拥有某个权限 */
  function hasPermission(perm: string): boolean {
    if (!perm) return true
    if (isSuperAdmin.value) return true
    return permissions.value.includes(perm)
  }

  /** 检查是否拥有任一权限 */
  function hasAnyPermission(perms: string[]): boolean {
    if (!perms.length) return true
    if (isSuperAdmin.value) return true
    return perms.some(perm => permissions.value.includes(perm))
  }

  /** 检查是否拥有全部权限 */
  function hasAllPermissions(perms: string[]): boolean {
    if (!perms.length) return true
    if (isSuperAdmin.value) return true
    return perms.every(perm => permissions.value.includes(perm))
  }

  // ---- Actions ----

  /** 管理端登录：调用后端接口，成功后存储 token 并拉取权限 */
  async function adminLogin(form: { username: string; password: string }): Promise<void> {
    const res: any = await apiAdminLogin(form)
    const data = res.data ?? res
    if (data?.token) {
      await loginSuccess(data.token)
    } else {
      throw new Error('登录失败：未获取到 token')
    }
  }

  /** 登录成功后的处理：存储 token 并拉取权限 */
  async function loginSuccess(newToken: string): Promise<void> {
    token.value = newToken
    localStorage.setItem('token', newToken) // 持久化 token
    // 立即从后端拉取完整用户信息和权限
    await fetchPermissions()
    // 权限拉取成功后，通知 router 添加动态路由（通过 import 触发）
    import('../router').then(({ addDynamicRoutes }) => addDynamicRoutes())
  }

  /** 从后端拉取用户信息和权限列表
   * 后端接口：GET /api/v1/auth/me
   * 返回格式：{ code: 200, data: { userId, username, realName, phone, email, avatar, roleCodes, menus } }
   */
  async function fetchPermissions(): Promise<void> {
    if (!token.value) return

    try {
      isLoading.value = true
      const res = await request.get('/platform/me')

      // axios 拦截器已处理外层 code，res 即为 ApiResponse
      const data = (res as any).data ?? res

      if (data) {
        // 后端返回扁平字段，不含 user 嵌套
        const primaryRole = data.roleCodes?.[0] || 'STAFF'
        userInfo.value = {
          id: data.userId,
          username: data.username || '',
          realName: data.realName || '',
          role: primaryRole,
          tenantId: data.tenantId ?? null,
          tenantName: data.tenantName ?? null,
          permissions: data.permissions || [],
        }

        // 存储 roleCodes
        if (data.roleCodes && Array.isArray(data.roleCodes)) {
          roleCodes.value = data.roleCodes
        }

        // 存储 menus (后端返回的是扁平列表，需要前端转换)
        if (data.menus && Array.isArray(data.menus)) {
          menus.value = convertBackendMenus(data.menus)
        } else {
          menus.value = []
        }

        // 同步 permissions 到独立 ref（hasPermission 依赖此值）
        permissions.value = data.permissions || []
      }
    } catch (error) {
      console.error('[UserStore] 获取权限失败:', error)
      throw error
    } finally {
      isLoading.value = false
    }
  }

  /** 刷新权限（用于权限变更后重新拉取） */
  async function refreshPermissions(): Promise<void> {
    await fetchPermissions()
  }

  /** 退出登录：清空所有状态 */
  function logout(): void {
    token.value = ''
    localStorage.removeItem('token') // 清除本地存储
    userInfo.value = null
    roleCodes.value = []
    permissions.value = []
    menus.value = []
    // 重置动态路由状态，确保下次登录时重新添加
    resetDynamicRoutes()
    // 注意：不在这里跳路由，由调用方处理
  }

  /** 设置 token（用于初始化时恢复） */
  function setToken(newToken: string): void {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  return {
    // State
    token,
    userInfo,
    roleCodes,
    menus,
    permissions,
    isLoading,
    // Getters
    isLoggedIn,
    isSuperAdmin,
    userName,
    userRole,
    tenantId,
    tenantName,
    // Methods
    adminLogin, // 修复：导出登录函数
    hasPermission,
    hasAnyPermission,
    hasAllPermissions,
    loginSuccess,
    fetchPermissions,
    refreshPermissions,
    logout,
    setToken,
  }
})
