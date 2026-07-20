<template>
  <a-layout class="app-root">
    <!-- 侧边栏 -->
    <a-layout-sider v-model:collapsed="collapsed" :width="220" :collapsed-width="64" collapsible class="app-sider" :trigger="null">
      <div class="logo" @click="router.push('/dashboard')">
        <svg viewBox="0 0 32 32" fill="none" class="logo-svg">
          <ellipse cx="16" cy="10" rx="9" ry="4" stroke="#818CF8" stroke-width="2.2"/>
          <line x1="16" y1="14" x2="16" y2="24" stroke="#818CF8" stroke-width="2.2" stroke-linecap="round"/>
          <path d="M10 18 C13.5 15, 18.5 15, 22 18" stroke="#818CF8" stroke-width="2" stroke-linecap="round"/>
        </svg>
        <span v-show="!collapsed" class="logo-txt">三益穿线</span>
      </div>

      <nav class="nav-wrap">
        <div v-for="g in menuTree" :key="g.key" class="nav-group">
          <!-- 分组标题 -->
          <div class="nav-group-hd" @click="collapsed ? null : toggleGroup(g.key)" v-show="!collapsed">
            <span>{{ g.label }}</span>
            <DownOutlined class="group-arrow" :class="{ rotated: openGroups.has(g.key) }" />
          </div>
          <div v-show="collapsed" class="nav-group-hd collapsed-hd" />

          <!-- 子菜单滚动展开 -->
          <div class="nav-group-bd" :class="{ expanded: collapsed || openGroups.has(g.key) }">
            <div class="nav-group-inner">
              <div
                v-for="ch in g.children"
                :key="ch.key"
                class="nav-link"
                :class="{ active: route.path === ch.key }"
                @click="router.push(ch.key)"
                :title="collapsed ? ch.label : ''"
              >
                <span class="nav-link-icon" v-if="ch.icon"><component :is="ch.icon" /></span>
                <span class="nav-link-icon" v-else><AppstoreOutlined /></span>
                <span class="nav-link-txt">{{ ch.label }}</span>
              </div>
            </div>
          </div>
          <div v-if="collapsed" class="nav-sep" />
        </div>
      </nav>

      <div class="trigger" @click="collapsed = !collapsed">
        <MenuFoldOutlined v-if="!collapsed" /><MenuUnfoldOutlined v-else />
      </div>
    </a-layout-sider>

    <!-- 主区域 -->
    <a-layout class="main">
      <a-layout-header class="topbar">
        <a-breadcrumb>
          <a-breadcrumb-item v-for="b in breadcrumbs" :key="b">{{ b }}</a-breadcrumb-item>
        </a-breadcrumb>
        <a-dropdown :trigger="['click']">
          <div class="user">
            <a-avatar :style="{ backgroundColor: '#4F46E5' }" size="small">{{ userName.charAt(0) }}</a-avatar>
            <span class="user-name">{{ userName }}</span><DownOutlined class="user-arrow" />
          </div>
          <template #overlay>
            <a-menu :items="userMenuItems" @click="onUserMenuClick" />
          </template>
        </a-dropdown>
      </a-layout-header>
      <a-layout-content class="body"><router-view /></a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup lang="ts">
import { ref, computed, h, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  DashboardOutlined, TeamOutlined, AuditOutlined, PrinterOutlined,
  SettingOutlined, AppstoreOutlined, ShoppingOutlined, TrophyOutlined,
  LogoutOutlined, MenuFoldOutlined, MenuUnfoldOutlined, DownOutlined,
  LockOutlined, SafetyOutlined
} from '@ant-design/icons-vue'
import { useUserStore } from '@/store/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const collapsed = ref(false)
const openGroups = ref<Set<string>>(new Set())

// ---- 从 store 读取用户信息（不再读 localStorage） ----
const userName = computed(() => userStore.userName || '?')

const iconMap: Record<string, any> = {
  DashboardOutlined, TeamOutlined, AuditOutlined, PrinterOutlined,
  SettingOutlined, AppstoreOutlined, ShoppingOutlined, TrophyOutlined,
  LockOutlined, SafetyOutlined
}
const roleLabelMap: Record<string, string> = { SUPER_ADMIN: '超管', TENANT_ADMIN: '租户管理员', STAFF: '员工' }

// ---- 菜单树：从 store 读取并转换为前端格式 ----
const menuTree = computed(() => {
  const bOk = userStore.userInfo?.enableBadmintonTournament === true
  const tOk = userStore.userInfo?.enableTennisTournament === true

  // 直接使用 store 返回的 MenuItem 结构
  const rawMenus = userStore.menus as any[]
  if (!rawMenus?.length) return []

  return rawMenus
    .map(g => ({
      key: g.key || String(g.id || ''),
      label: g.label || '',
      children: (g.children || [])
        .filter((ch: any) => {
          const p = ch.path || ''
          if (p === '/tournament' || p === '/tournament/list') return bOk
          if (p === '/tennis/tournament' || p === '/tennis/tournament/list') return tOk
          return true
        })
        .map((ch: any) => ({
          key: ch.path || ch.key || '',
          label: ch.label || '',
          icon: ch.icon && iconMap[ch.icon] ? iconMap[ch.icon] : null
        }))
    }))
    .filter((g: any) => g.children.length > 0)
})

// 自动展开当前路由所在分组
watch(() => route.path, () => {
  for (const g of menuTree.value) {
    if (g.children.some(ch => ch.key === route.path)) {
      openGroups.value.add(g.key)
    }
  }
  openGroups.value = new Set(openGroups.value)
}, { immediate: true })

function toggleGroup(key: string) {
  const s = new Set(openGroups.value)
  s.has(key) ? s.delete(key) : s.add(key)
  openGroups.value = s
}

const breadcrumbs = computed(() =>
  route.matched.filter(r => r.meta.title).map(r => r.meta.title as string)
)

// ---- 用户下拉菜单 ----
const userMenuItems = computed(() => [
  { key: 'role', label: roleLabelMap[userStore.userRole] || '未知角色', disabled: true },
  { type: 'divider' as const },
  { key: 'logout', label: '退出登录', icon: h(LogoutOutlined) }
])

function onUserMenuClick({ key }: { key: string }) {
  if (key === 'logout') {
    // 使用 store.logout() 清理状态
    userStore.logout()
    router.push('/admin/login')
  }
}
</script>

<style scoped>
.app-root { height: 100vh; overflow: hidden; }
.app-sider { background: #111827 !important; display: flex; flex-direction: column; }

/* logo */
.logo { height: 60px; display: flex; align-items: center; justify-content: center; gap: 10px; cursor: pointer; border-bottom: 1px solid #1F2937; user-select: none; }
.logo-svg { width: 28px; height: 28px; flex-shrink: 0; }
.logo-txt { font-size: 17px; font-weight: 700; color: #F9FAFB; white-space: nowrap; letter-spacing: 1px; }

/* 导航 */
.nav-wrap { flex: 1; overflow-y: auto; padding: 4px 0; }
.nav-group { margin-bottom: 2px; }

.nav-group-hd {
  display: flex; align-items: center; justify-content: space-between;
  padding: 8px 16px 4px;
  font-size: 11px; font-weight: 600; letter-spacing: .08em;
  color: #6B7280; cursor: pointer; user-select: none;
  text-transform: uppercase;
}
.group-arrow { font-size: 10px; transition: transform .2s; }
.group-arrow.rotated { transform: rotate(180deg); }

.nav-group-bd { overflow: hidden; max-height: 0; transition: max-height .25s ease; }
.nav-group-bd.expanded { max-height: 600px; }
.nav-group-inner { padding: 2px 0; }

.nav-link {
  display: flex; align-items: center; gap: 10px;
  height: 40px; margin: 2px 8px; padding: 0 12px;
  border-radius: 6px; cursor: pointer;
  color: #D1D5DB; font-size: 13px;
  transition: background-color .2s, color .2s;
}
.nav-link:hover { background: rgba(255,255,255,.06); color: #F9FAFB; }
.nav-link.active { background: #4F46E5; color: #fff; font-weight: 600; }
.nav-link-icon { font-size: 16px; width: 16px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.nav-link-txt { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.nav-sep { height: 4px; }

/* trigger */
.trigger { height: 44px; display: flex; align-items: center; justify-content: center; color: #6B7280; font-size: 15px; cursor: pointer; border-top: 1px solid #1F2937; transition: color .2s; }
.trigger:hover { color: #D1D5DB; }

/* 主区域 */
.main { background: #f5f6fa; }
.topbar { height: 56px; line-height: 56px; padding: 0 24px; display: flex; justify-content: space-between; align-items: center; background: #fff; border-bottom: 1px solid #e8ecf1; }
.topbar :deep(.ant-breadcrumb-link) { font-size: 13px; color: #6B7280; }
.topbar :deep(.ant-breadcrumb-separator) { color: #D1D5DB; }
.user { display: flex; align-items: center; gap: 8px; cursor: pointer; padding: 4px 10px; border-radius: 6px; transition: background-color .2s; }
.user:hover { background: #F3F4F6; }
.user-name { font-size: 13px; color: #374151; font-weight: 500; }
.user-arrow { font-size: 10px; color: #9CA3AF; }
.body { padding: 24px; overflow-y: auto; min-height: 0; }
</style>
