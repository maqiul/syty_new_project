<template>
  <div class="admin-dashboard">
    <a-row :gutter="[16, 16]" class="stat-cards">
      <a-col :xs="12" :sm="6" v-for="item in stats" :key="item.label">
        <a-card hoverable class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" :style="{ background: item.color }">
              <component :is="item.icon" />
            </div>
            <div class="stat-info">
              <p class="stat-value">{{ item.value }}</p>
              <p class="stat-label">{{ item.label }}</p>
            </div>
          </div>
        </a-card>
      </a-col>
    </a-row>

    <a-row :gutter="[16, 16]" class="content-row">
      <a-col :xs="24" :lg="16">
        <a-card title="最近操作日志">
          <a-table :dataSource="recentLogs" :columns="logColumns" :pagination="false" size="small" />
        </a-card>
      </a-col>
      <a-col :xs="24" :lg="8">
        <a-card title="快捷入口">
          <div class="quick-links">
            <a-button
              v-for="link in quickLinks"
              :key="link.path"
              type="primary"
              class="quick-link-btn"
              block
              @click="$router.push(link.path)"
            >
              <template #icon><component :is="link.icon" /></template>
              {{ link.label }}
            </a-button>
          </div>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import {
  UserOutlined,
  ApartmentOutlined,
  FileTextOutlined,
  DesktopOutlined,
  SettingOutlined,
} from '@ant-design/icons-vue'
import { getPlatformDashboardStats, getPlatformRecentLogs } from '@/api'

const stats = ref([
  { label: '租户数', value: '--', icon: ApartmentOutlined, color: '#1677ff' },
  { label: '用户数', value: '--', icon: UserOutlined, color: '#52c41a' },
  { label: '活跃租户', value: '--', icon: FileTextOutlined, color: '#faad14' },
  { label: '系统状态', value: '运行中', icon: DesktopOutlined, color: '#8c8c8c' },
])

const recentLogs = ref<any[]>([])
const logColumns = [
  { title: '操作人', dataIndex: 'user', key: 'user', width: 120 },
  { title: '操作', dataIndex: 'action', key: 'action' },
  { title: '目标', dataIndex: 'target', key: 'target' },
  { title: '时间', dataIndex: 'time', key: 'time', width: 180 },
]

const quickLinks = [
  { label: '用户管理', path: '/user', icon: UserOutlined },
  { label: '租户管理', path: '/tenant', icon: ApartmentOutlined },
  { label: '角色管理', path: '/system/role', icon: SettingOutlined },
  { label: '菜单管理', path: '/system/menu', icon: SettingOutlined },
  { label: '操作日志', path: '/operation-log', icon: FileTextOutlined },
]

onMounted(async () => {
  // 加载统计数据
  try {
    const res: any = await getPlatformDashboardStats()
    const data = res?.data?.data ?? res?.data ?? res
    if (data) {
      stats.value[0].value = data.tenantCount ?? 0
      stats.value[1].value = data.userCount ?? 0
      stats.value[2].value = data.activeTenantCount ?? 0
    }
  } catch (e) {
    console.error('加载统计数据失败', e)
  }
  
  // 加载操作日志
  try {
    const res: any = await getPlatformRecentLogs(10)
    const data = res?.data?.data ?? res?.data ?? res
    if (Array.isArray(data)) {
      recentLogs.value = data.map((item: any) => ({
        user: item.user || '系统',
        action: item.content || '',
        target: '',
        time: item.time || '',
      }))
    }
  } catch (e) {
    console.error('加载操作日志失败', e)
  }
})
</script>

<style scoped>
.admin-dashboard {
  padding: 20px;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 24px;
  flex-shrink: 0;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  margin: 0;
  color: #303133;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin: 4px 0 0;
}

.quick-links {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>
