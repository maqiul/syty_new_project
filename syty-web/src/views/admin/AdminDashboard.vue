<template>
  <div class="admin-dashboard">
    <el-row :gutter="20" class="stat-cards">
      <el-col :xs="12" :sm="6" v-for="item in stats" :key="item.label">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" :style="{ background: item.color }">
            <el-icon :size="28"><component :is="item.icon" /></el-icon>
          </div>
          <div class="stat-info">
            <p class="stat-value">{{ item.value }}</p>
            <p class="stat-label">{{ item.label }}</p>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="content-row">
      <el-col :xs="24" :lg="16">
        <el-card header="最近操作日志">
          <el-table :data="recentLogs" stripe style="width: 100%">
            <el-table-column prop="user" label="操作人" width="120" />
            <el-table-column prop="action" label="操作" />
            <el-table-column prop="target" label="目标" />
            <el-table-column prop="time" label="时间" width="180" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="8">
        <el-card header="快捷入口">
          <div class="quick-links">
            <el-button
              v-for="link in quickLinks"
              :key="link.path"
              type="primary"
              class="quick-link-btn"
              @click="$router.push(link.path)"
            >
              <el-icon><component :is="link.icon" /></el-icon>
              {{ link.label }}
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { User, OfficeBuilding, Document, Monitor, Setting } from '@element-plus/icons-vue'

interface StatItem {
  label: string
  value: string | number
  icon: any
  color: string
}

interface LogItem {
  user: string
  action: string
  target: string
  time: string
}

const stats = ref<StatItem[]>([
  { label: '租户数', value: '--', icon: OfficeBuilding, color: '#409EFF' },
  { label: '用户数', value: '--', icon: User, color: '#67C23A' },
  { label: '订单数', value: '--', icon: Document, color: '#E6A23C' },
  { label: '系统状态', value: '运行中', icon: Monitor, color: '#909399' },
])

const recentLogs = ref<LogItem[]>([])

const quickLinks = [
  { label: '用户管理', path: '/user', icon: User },
  { label: '租户管理', path: '/tenant', icon: OfficeBuilding },
  { label: '角色管理', path: '/system/role', icon: Setting },
  { label: '菜单管理', path: '/system/menu', icon: Setting },
  { label: '操作日志', path: '/operation-log', icon: Document },
]

const fetchStats = async () => {
  try {
    // TODO: 调用后端接口获取统计数据
    // const res = await api.getAdminDashboard()
    // stats.value = res.data
  } catch (e) {
    console.error('Failed to fetch admin dashboard stats', e)
  }
}

const fetchRecentLogs = async () => {
  try {
    // TODO: 调用后端接口获取最近操作日志
    // const res = await api.getRecentLogs({ limit: 10 })
    // recentLogs.value = res.data
  } catch (e) {
    console.error('Failed to fetch recent logs', e)
  }
}

onMounted(() => {
  fetchStats()
  fetchRecentLogs()
})
</script>

<style scoped>
.admin-dashboard {
  padding: 20px;
}

.stat-cards {
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
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

.content-row {
  margin-bottom: 20px;
}

.quick-links {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.quick-link-btn {
  width: 100%;
  justify-content: flex-start;
  gap: 8px;
}
</style>
