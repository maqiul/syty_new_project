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

/* ========== 路径 → 组件映射表 (租户端专属) ========== */
const componentMap: Record<string, () => Promise<any>> = {
  // --- Tenant: 看板 ---
  '/dashboard': () => import('../views/tenant/dashboard/Dashboard.vue'),
  '/performance': () => import('../views/tenant/dashboard/Performance.vue'),

  // --- Tenant: 订单 ---
  '/order': () => import('../views/tenant/orders/Order.vue'),
  '/order/create': () => import('../views/tenant/orders/CreateOrder.vue'),
  '/order/list': () => import('../views/tenant/order/OrderList.vue'), // V2.0 新订单列表
  '/kanban': () => import('../views/tenant/orders/Kanban.vue'),
  '/tournament': () => import('../views/tenant/orders/TournamentOrder.vue'),
  '/tournament/list': () => import('../views/tenant/orders/BadmintonTournamentList.vue'),
  
  // --- Tenant: 网球订单 ---
  '/tennis/order': () => import('../views/tenant/orders/TennisOrder.vue'),
  '/tennis/tournament': () => import('../views/tenant/orders/TennisTournament.vue'),
  '/tennis/tournament/list': () => import('../views/tenant/orders/TennisTournamentList.vue'),

  // --- Tenant: 库存与资产 ---
  '/stock': () => import('../views/tenant/inventory/List.vue'),
  '/shop': () => import('../views/tenant/assets/Shop.vue'),
  '/shop-string': () => import('../views/tenant/assets/ShopStrings.vue'),
  '/finance': () => import('../views/tenant/Finance.vue'),

  // --- Tenant: 统一资产（羽网合并） ---
  '/player': () => import('../views/tenant/assets/Player.vue'),
  '/racket': () => import('../views/tenant/assets/Racket.vue'),
  '/string': () => import('../views/tenant/assets/String.vue'),
  '/supplier': () => import('../views/tenant/assets/Supplier.vue'),

  // --- Tenant: 网球资产（旧，保留兼容） ---
  '/tennis/player': () => import('../views/tenant/assets/TennisPlayer.vue'),
  '/tennis/racket': () => import('../views/tenant/assets/TennisRacket.vue'),
  '/tennis/string': () => import('../views/tenant/assets/TennisString.vue'),
  '/tennis/shop-string': () => import('../views/tenant/assets/TennisShopString.vue'),

  // --- Tenant: 穿线 ---
  '/print-config': () => import('../views/tenant/stringing/PrintConfig.vue'),
  '/print-template': () => import('../views/tenant/stringing/PrintTemplate.vue'),
  '/print-policy': () => import('../views/tenant/stringing/PrintPolicy.vue'),
  '/print-logs': () => import('../views/tenant/stringing/PrintLogs.vue'),

  // --- Tenant: 客户注册 ---
  '/customer-register': () => import('../views/tenant/auth/CustomerRegister.vue'),
  '/tennis-customer-register': () => import('../views/tenant/auth/TennisCustomerRegister.vue'),
}

/** 页面标题映射 */
const titleMap: Record<string, string> = {
  '/dashboard': '工作台',
  '/performance': '穿线师绩效',
  '/order': '订单管理',
  '/order/create': '新建订单',
  '/kanban': '看板',
  '/stock': '库存管理',
  '/shop': '门店管理',
  '/shop-string': '门店穿线',
  '/tournament': '赛事订单',
  '/tournament/list': '赛事列表',
  '/tennis/order': '网球订单',
  '/tennis/tournament': '网球赛事',
  '/tennis/tournament/list': '网球赛事列表',
  '/player': '球员管理',
  '/racket': '球拍管理',
  '/string': '球线管理',
  '/supplier': '供应商管理',
  '/player': '球员管理',
  '/racket': '球拍管理',
  '/string': '球线管理',
  '/supplier': '供应商管理',
  '/player': '球员管理',
  '/racket': '球拍管理',
  '/string': '球线管理',
  '/supplier': '供应商管理',
  '/player': '球员管理',
  '/racket': '球拍管理',
  '/string': '球线管理',
  '/supplier': '供应商管理',
  '/tennis/player': '网球球员',
  '/tennis/racket': '网球球拍',
  '/tennis/string': '网球线材',
  '/tennis/shop-string': '网球门店穿线',
  '/finance': '财务管理',
  '/print-config': '打印配置',
  '/print-template': '打印模板',
  '/print-policy': '打印策略',
  '/print-logs': '打印日志',
  '/customer-register': '客户注册',
  '/tennis-customer-register': '网球客户注册',
}

/* ========== 公开路由（无需登录） ========== */
const publicRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/tenant/auth/Login.vue'),
    meta: { title: '租户登录' }
  },
  {
    path: '/customer-register',
    name: 'CustomerRegister',
    component: () => import('../views/tenant/auth/CustomerRegister.vue'),
    meta: { title: '客户注册' }
  },
  {
    path: '/tennis-customer-register',
    name: 'TennisCustomerRegister',
    component: () => import('../views/tenant/auth/TennisCustomerRegister.vue'),
    meta: { title: '网球客户注册' }
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

const whiteList = ['/login', '/customer-register', '/tennis-customer-register']

router.beforeEach(async (to: RouteLocationNormalized, _from: RouteLocationNormalized, next: NavigationGuardNext) => {
  // 1. 未登录 → 放行白名单，其余跳登录
  if (!userStore || !userStore.isLoggedIn) {
    if (whiteList.includes(to.path)) { next(); return }
    next(`/login?redirect=${to.path}`)
    return
  }

  // 2. 已登录访问登录页 → 跳转看板
  if (to.path === '/login') { next('/dashboard'); return }

  // 3. 动态路由加载
  if (!dynamicRoutesAdded) {
    try {
      if (!userStore.menus?.length) await userStore.fetchPermissions()
      const success = addDynamicRoutes()
      if (!success) { userStore.logout(); next(`/login?redirect=${to.path}`); return }
      next({ ...to, replace: true })
      return
    } catch (e) {
      userStore.logout(); next(`/login?redirect=${to.path}`); return
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
