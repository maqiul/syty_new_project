import type { Directive, DirectiveBinding } from 'vue'
import { useUserStore } from '../store/user'

/**
 * v-hasPermi 指令
 *
 * 用法：
 *   <a-button v-hasPermi="'user:create'">新增用户</a-button>
 *   <a-button v-hasPermi="['user:create', 'user:update']">操作</a-button>  -- 满足任一权限即显示
 *
 * 原理：
 *   - 从 Pinia store 中获取当前用户的权限列表
 *   - 超级管理员拥有所有权限，直接放行
 *   - 无权限时移除 DOM 元素（或设为 display: none）
 */
export const vHasPermi: Directive<HTMLElement, string | string[]> = {
  mounted(el: HTMLElement, binding: DirectiveBinding<string | string[]>) {
    const { value } = binding
    if (!value) return

    const userStore = useUserStore()

    // 支持传入单个权限字符串或权限数组
    const perms = Array.isArray(value) ? value : [value]

    // 检查权限：有任一权限 或 是超级管理员
    const hasAccess = userStore.isSuperAdmin || perms.some(perm => userStore.permissions.includes(perm))

    if (!hasAccess) {
      // 无权限：移除 DOM 节点
      el.parentNode?.removeChild(el)
    }
  },

  // 响应式：当权限变化时更新显隐
  updated(el: HTMLElement, binding: DirectiveBinding<string | string[]>) {
    const { value, oldValue } = binding
    if (JSON.stringify(value) === JSON.stringify(oldValue)) return

    const userStore = useUserStore()
    const perms = Array.isArray(value) ? value : [value]
    const hasAccess = userStore.isSuperAdmin || perms.some(perm => userStore.permissions.includes(perm))

    if (!hasAccess) {
      el.parentNode?.removeChild(el)
    }
  },
}

/**
 * v-hasRole 指令（附加赠送：角色控制）
 *
 * 用法：
 *   <a-button v-hasRole="'SUPER_ADMIN'">仅超管可见</a-button>
 *   <a-button v-hasRole="['SUPER_ADMIN', 'TENANT_ADMIN']">超管或租户管理员可见</a-button>
 */
export const vHasRole: Directive<HTMLElement, string | string[]> = {
  mounted(el: HTMLElement, binding: DirectiveBinding<string | string[]>) {
    const { value } = binding
    if (!value) return

    const userStore = useUserStore()
    const roles = Array.isArray(value) ? value : [value]

    const hasAccess = roles.includes(userStore.userRole)

    if (!hasAccess) {
      el.parentNode?.removeChild(el)
    }
  },

  updated(el: HTMLElement, binding: DirectiveBinding<string | string[]>) {
    const { value, oldValue } = binding
    if (JSON.stringify(value) === JSON.stringify(oldValue)) return

    const userStore = useUserStore()
    const roles = Array.isArray(value) ? value : [value]
    const hasAccess = roles.includes(userStore.userRole)

    if (!hasAccess) {
      el.parentNode?.removeChild(el)
    }
  },
}
