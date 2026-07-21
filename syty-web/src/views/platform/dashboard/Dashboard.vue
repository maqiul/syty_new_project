<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <a-row :gutter="16" class="stat-row">
      <a-col :span="6">
        <a-card class="stat-card">
          <a-statistic title="租户总数" :value="stats.tenantCount" :value-style="{ color: '#4F46E5' }">
            <template #prefix><TeamOutlined /></template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card class="stat-card">
          <a-statistic title="活跃租户" :value="stats.activeTenantCount" :value-style="{ color: '#10B981' }">
            <template #prefix><CheckCircleOutlined /></template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card class="stat-card">
          <a-statistic title="用户总数" :value="stats.userCount" :value-style="{ color: '#F59E0B' }">
            <template #prefix><UserOutlined /></template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card class="stat-card">
          <a-statistic title="套餐数" :value="packageCount" :value-style="{ color: '#8B5CF6' }">
            <template #prefix><AppstoreOutlined /></template>
          </a-statistic>
        </a-card>
      </a-col>
    </a-row>

    <!-- 套餐分布 + 最近日志 -->
    <a-row :gutter="16" style="margin-top: 16px;">
      <a-col :span="12">
        <a-card title="套餐分布">
          <a-list :data-source="stats.packageDistribution || []" size="small">
            <template #renderItem="{ item }">
              <a-list-item>
                <a-list-item-meta :title="item.packageName" :description="`价格: ¥${item.price}`" />
                <a-tag :color="item.status === 'ACTIVE' ? 'green' : 'default'">{{ item.status }}</a-tag>
              </a-list-item>
            </template>
          </a-list>
        </a-card>
      </a-col>
      <a-col :span="12">
        <a-card title="最近操作">
          <a-timeline>
            <a-timeline-item v-for="(log, idx) in recentLogs" :key="idx" :color="log.avatarColor || '#4F46E5'">
              <p style="margin: 0;"><strong>{{ log.user }}</strong> {{ log.content }}</p>
              <p style="margin: 0; font-size: 12px; color: #999;">{{ log.time }}</p>
            </a-timeline-item>
          </a-timeline>
          <a-empty v-if="!recentLogs.length" description="暂无操作记录" />
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { TeamOutlined, UserOutlined, AppstoreOutlined, CheckCircleOutlined } from '@antdv-next/icons'
import { getDashboardStats, getDashboardLogs } from '@/api/platform/dashboard'

const stats = ref<any>({})
const recentLogs = ref<any[]>([])

const packageCount = computed(() => stats.value.packageDistribution?.length || 0)

onMounted(async () => {
  try {
    const [statsRes, logsRes] = await Promise.all([
      getDashboardStats(),
      getDashboardLogs(10)
    ])
    stats.value = (statsRes as any).data || {}
    recentLogs.value = (logsRes as any).data || []
  } catch (e) {
    console.error('Dashboard load failed', e)
  }
})
</script>

<style scoped>
.stat-card { border-radius: 8px; }
.stat-row .ant-card { box-shadow: 0 1px 3px rgba(0,0,0,0.04); }
</style>
