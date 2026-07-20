import { createRouter, createWebHistory, type RouteRecordRaw, type NavigationGuardNext, type RouteLocationNormalized } from 'vue-router'
import type { useUserStore } from '../store/user'
import type { MenuItem } from '../store/user'

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    icon?: string
    permission?: string
    hidden?: boolean
  }
}

/* ========== 模块级变量 ========== */
let userStore: ReturnType<typeof useUserStore> | null = null
let dynamicRoutesAdded = false

/** main.ts 调用此方法注入 store 实例 */
export function setUserStore(store: ReturnType<typeof useUserStore>): void {
  userStore = store
}

/** 权限检查辅助函数 */
function hasPermission(perm?: string): boolean {
  if (!perm) return true
  if (!userStore) return false
  return userStore.hasPermission(perm)
}

/* ========== 路径 → 组件映射表 (管理端/平台端) ========== */
const componentMap: Record<string, () => Promise<any>> = {
  // --- Platform (V1.9): 平台端新页面 ---
  '/dashboard': () => import('../views/platform/Dashboard.vue'),
  '/tenant/list': () => import('../views/platform/tenant/List.vue'),
  '/tenant/plans': () => import('../views/platform/tenant/Plans.vue'),
  '/system/user': () => import('../views/platform/system/User.vue'),
  '/system/role': () => import('../views/platform/system/Role.vue'),
  '/system/config': () => import('../views/platform/system/Config.vue'),
  '/operation/notice': () => import('../views/platform/operation/Notice.vue'),
  '/tenant/shop': () => import('../views/platform/shop/List.vue'),

  // --- Admin (旧): 兼容旧路由 ---
  '/admin-dashboard': () => import('../views/admin/dashboard/AdminDashboard.vue'),
  '/admin-performance': () => import('../views/admin/dashboard/Performance.vue'),
  '/tenant': () => import('../views/admin/tenants/Tenant.vue'),
  '/user': () => import('../views/admin/users/User.vue'),
  '/system/role-old': () => import('../views/admin/users/Role.vue'),
  '/system/menu': () => import('../views/admin/users/MenuManage.vue'),
  '/system/perm': () => import('../views/admin/users/PermManage.vue'),
  '/operation-log': () => import('../views/admin/users/OperationLog.vue'),
  '/racket': () => import('../views/admin/assets/Racket.vue'),
  '/player': () => import('../views/admin/assets/Player.vue'),
  '/string-info': () => import('../views/admin/assets/StringInfo.vue'),
}

/** 页面标题映射 */
const titleMap: Record<string, string> = {
  '/admin-dashboard': '管理总览',
  '/admin-performance': '穿线师绩效',
  '/user': '用户管理',
  '/operation-log': '操作日志',
  '/system/menu': '菜单管理',
  '/system/perm': '权限管理',
  '/system/role': '角色管理',
  '/tenant': '租户管理',
  '/player': '球员管理',
  '/racket': '球拍管理',
  '/string-info': '球线管理',
}

/* ========== 公开路由（无需登录） ========== */
const publicRoutes: RouteRecordRaw[] = [
  {
    path: '/admin/login',
    name: 'AdminLogin',
    component: () => import('../views/admin/login/AdminLogin.vue'),
    meta: { title: '管理后台登录' }
  }
]

/* ========== 静态兜底路由 ========== */
const layoutRoute: RouteRecordRaw = {
  path: '/',
  name: 'Layout',
  component: () => import('../views/Layout.vue'),
  redirect: '/dashboard',
  children: []
}

const fallbackRoutes: RouteRecordRaw[] = [
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('../views/NotFound.vue'),
    meta: { title: '404' }
  }
]

/** 将后端菜单树转换为 Vue Router 路由配置 */
function generateRoutesFromMenus(menus: MenuItem[]): RouteRecordRaw[] {
  const routes: RouteRecordRaw[] = []
  function walk(items: MenuItem[], parentPath = ''): RouteRecordRaw[] {
    const result: RouteRecordRaw[] = []
    for (const item of items) {
      const rawPath = item.path || item.key || ''
      if (!rawPath) continue
      if (item.menuType === 'DIRECTORY') {
        if (item.children && item.children.length > 0) {
          const childRoutes = walk(item.children, rawPath)
          result.push(...childRoutes)
        }
        continue
      }
      const component = componentMap[rawPath]
      if (!component) {
        if (item.children && item.children.length > 0) {
          const childRoutes = walk(item.children, rawPath)
          result.push(...childRoutes)
        }
        continue
      }
      const finalPath = rawPath.startsWith('/') ? rawPath : `/${rawPath}`
      const route: RouteRecordRaw = {
        path: finalPath,
        name: item.key || finalPath.replace(/^\//, '').replace(/[/-]/g, '_'),
        component: () => import('../views/Layout.vue'),
        redirect: finalPath,
        children: [{
          path: '',
          name: (item.key || finalPath) + '_view',
          component,
          meta: { title: item.label || titleMap[rawPath] || '', icon: item.icon, permission: item.permission },
        }],
      }
      result.push(route)
    }
    return result
  }
  routes.push(...walk(menus))
  return routes
}

export function addDynamicRoutes(): boolean {
  if (dynamicRoutesAdded) return true
  if (!userStore?.menus?.length) {
    console.warn('[Router] 菜单为空，跳过动态路由添加')
    return false
  }
  const menus = userStore.menus as MenuItem[]
  const dynamicRoutes = generateRoutesFromMenus(menus)
  dynamicRoutes.forEach(route => router.addRoute(route))
  fallbackRoutes.forEach(route => router.addRoute(route))
  dynamicRoutesAdded = true
  console.log(`[Router] ✅ 动态路由已添加，共 ${dynamicRoutes.length} 条`)
  return true
}

export function resetDynamicRoutes(): void {
  dynamicRoutesAdded = false
}

const router = createRouter({
  history: createWebHistory(),
  routes: [...publicRoutes, layoutRoute]
})

const whiteList = ['/admin/login']

router.beforeEach(async (to: RouteLocationNormalized, _from: RouteLocationNormalized, next: NavigationGuardNext) => {
  // 1. 未登录 → 放行白名单，其余跳登录
  if (!userStore || !userStore.isLoggedIn) {
    if (whiteList.includes(to.path)) { next(); return }
    next(`/admin/login?redirect=${to.path}`)
    return
  }

  // 2. 已登录访问登录页 → 跳转看板
  if (to.path === '/admin/login') { next('/dashboard'); return }

  // 3. 动态路由加载
  if (!dynamicRoutesAdded) {
    try {
      if (!userStore.menus?.length) await userStore.fetchPermissions()
      const success = addDynamicRoutes()
      if (!success) { userStore.logout(); next(`/admin/login?redirect=${to.path}`); return }
      next({ ...to, replace: true })
      return
    } catch (e) {
      userStore.logout(); next(`/admin/login?redirect=${to.path}`); return
    }
  }

  // 4. 权限检查
  const requiredPerm = to.meta?.permission as string | undefined
  if (requiredPerm && !hasPermission(requiredPerm)) {
    next('/dashboard')
    return
  }
  next()
})

export default router
