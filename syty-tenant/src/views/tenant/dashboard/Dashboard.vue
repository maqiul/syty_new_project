<template>
  <div class="dashboard">
    <div class="dash-head">
      <div>
        <h2>工作台</h2>
        <p>{{ greeting }}，{{ user.realName || user.username }}</p>
      </div>
      <span class="dash-badge">{{ user.tenantName || '平台管理' }}</span>
    </div>

    <!-- 统计 -->
    <div class="stat-row">
      <div class="stat-item" v-for="s in statsArr" :key="s.label">
        <div class="stat-icon-box" :style="{ backgroundColor: s.bg, color: s.color }">
          <component :is="s.icon" />
        </div>
        <div class="stat-info">
          <span class="stat-label">{{ s.label }}</span>
          <span class="stat-value">{{ s.value }}</span>
        </div>
      </div>
    </div>

    <!-- 快捷操作 -->
    <h3 class="section-label">快捷操作</h3>
    <div class="action-row">
      <div class="action-item" v-for="a in actions" :key="a.label" @click="router.push(a.path)">
        <component :is="a.icon" class="action-icon" />
        <div>
          <span class="action-label">{{ a.label }}</span>
          <span class="action-desc">{{ a.desc }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  PlusOutlined, TeamOutlined, ShoppingOutlined, UnorderedListOutlined,
  FileTextOutlined, ClockCircleOutlined, CheckCircleOutlined, ShopOutlined, BankOutlined
} from '@antdv-next/icons'
import { getDashboardStats, type DashboardStats } from '@/api'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const user = computed(() => userStore.userInfo || { id: 0, username: '', realName: '', role: 'STAFF' as const, tenantId: null, tenantName: null, permissions: [] })
const stats = ref<DashboardStats>({ todayOrders: 0, pendingOrders: 0, totalPlayers: 0, totalTenants: 0, shopCount: 0, totalOrders: 0, completedOrders: 0, todayRevenue: 0, totalRevenue: 0, totalShops: 0 })
const isSuperAdmin = userStore.isSuperAdmin

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 9) return '早上好'
  if (h < 12) return '上午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

const statsArr = computed(() => {
  const list = [
    { label: '今日订单', value: stats.value.todayOrders, icon: FileTextOutlined, bg: '#EEF2FF', color: '#4F46E5' },
    { label: '待处理', value: stats.value.pendingOrders, icon: ClockCircleOutlined, bg: '#FFF7ED', color: '#EA580C' },
    { label: '已完成', value: stats.value.completedOrders || 0, icon: CheckCircleOutlined, bg: '#F0FDF4', color: '#16A34A' },
    { label: '球员总数', value: stats.value.totalPlayers, icon: TeamOutlined, bg: '#FAF5FF', color: '#9333EA' },
  ]
  list.push(isSuperAdmin
    ? { label: '租户总数', value: stats.value.totalTenants, icon: BankOutlined, bg: '#FFF1F2', color: '#E11D48' }
    : { label: '店铺总数', value: stats.value.shopCount, icon: ShopOutlined, bg: '#F0F9FF', color: '#0284C7' }
  )
  return list
})

const actions = computed(() => {
  const list = [
    { label: '新建订单', desc: '登记穿线信息', path: '/order/create', icon: PlusOutlined },
    { label: '订单列表', desc: '查看与管理所有订单', path: '/order', icon: UnorderedListOutlined },
    { label: '球员管理', desc: '维护球员基础数据', path: '/player', icon: TeamOutlined },
  ]
  if (!isSuperAdmin) list.push({ label: '店铺管理', desc: '管理球线与库存', path: '/shop', icon: ShoppingOutlined })
  return list
})

onMounted(async () => {
  try { const res = await getDashboardStats(); stats.value = res.data } catch { /* */ }
})
</script>

<style scoped>
.dashboard { max-width: 1200px; }

.dash-head {
  display: flex; justify-content: space-between; align-items: flex-start;
  margin-bottom: 28px;
}
.dash-head h2 { font-size: 22px; font-weight: 700; color: #111827; margin: 0 0 4px; }
.dash-head p { font-size: 13px; color: #9CA3AF; margin: 0; }
.dash-badge {
  font-size: 12px; font-weight: 500; padding: 3px 12px; border-radius: 100px;
  background: #EEF2FF; color: #4F46E5;
}

/* 统计卡片 */
.stat-row {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(210px, 1fr));
  gap: 16px;
  margin-bottom: 32px;
}
.stat-item {
  display: flex; align-items: center; gap: 16px;
  background: #fff; border-radius: 10px; padding: 22px 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  border: 1px solid #f1f5f9;
  transition: box-shadow .2s;
}
.stat-item:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.06); }
.stat-icon-box {
  width: 48px; height: 48px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  font-size: 22px; flex-shrink: 0;
}
.stat-label { display: block; font-size: 13px; color: #6B7280; margin-bottom: 2px; }
.stat-value { font-size: 26px; font-weight: 700; color: #111827; }

/* 快捷操作 */
.section-label { font-size: 16px; font-weight: 600; color: #111827; margin: 0 0 14px; }
.action-row {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 12px;
}
.action-item {
  display: flex; align-items: center; gap: 14px;
  background: #fff; border-radius: 8px; padding: 18px 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  border: 1px solid #f1f5f9;
  cursor: pointer;
  transition: box-shadow .2s, border-color .2s;
}
.action-item:hover { border-color: #4F46E5; box-shadow: 0 4px 12px rgba(0,0,0,0.06); }
.action-icon { font-size: 20px; color: #4F46E5; flex-shrink: 0; }
.action-label { display: block; font-size: 14px; font-weight: 600; color: #111827; }
.action-desc { display: block; font-size: 12px; color: #9CA3AF; margin-top: 2px; }
</style>
