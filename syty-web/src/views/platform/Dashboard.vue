<template>
  <div class="platform-dashboard">
    <!-- 欢迎横幅 -->
    <a-card :bordered="false" class="welcome-card" style="margin-bottom: 24px">
      <h1 class="welcome-title">👋 欢迎使用平台管理系统</h1>
      <p class="welcome-desc">在这里你可以管理租户、用户、角色等核心数据。</p>
    </a-card>

    <!-- 统计卡片 -->
    <a-row :gutter="16" style="margin-bottom: 24px">
      <a-col :xs="24" :sm="12" :lg="6" v-for="stat in statsCards" :key="stat.title">
        <a-card :bordered="false" class="stat-card" :style="{ borderLeft: `4px solid ${stat.color}` }">
          <a-statistic
            :title="stat.title"
            :value="stat.value"
            :prefix="stat.prefix"
            :suffix="stat.suffix"
            :value-style="{ color: stat.color }"
          />
        </a-card>
      </a-col>
    </a-row>

    <!-- 活跃套餐分布 & 操作日志 -->
    <a-row :gutter="24">
      <a-col :xs="24" :lg="14">
        <a-card title="📝 最近操作日志" :bordered="false">
          <a-list :data-source="logs" item-layout="horizontal">
            <template #renderItem="{ item }">
              <a-list-item>
                <a-list-item-meta :description="item.time">
                  <template #title>
                    {{ item.content }}
                  </template>
                  <template #avatar>
                    <a-avatar :style="{ backgroundColor: item.avatarColor }">
                      {{ item.user.charAt(0) }}
                    </a-avatar>
                  </template>
                </a-list-item-meta>
              </a-list-item>
            </template>
          </a-list>
        </a-card>
      </a-col>

      <a-col :xs="24" :lg="10">
        <a-card title="📊 活跃套餐分布" :bordered="false">
          <a-descriptions :column="1" bordered size="small">
            <a-descriptions-item label="基础版">
              <a-progress
                :percent="80"
                :stroke-color="'#52c41a'"
                :show-info="true"
                status="normal"
              />
            </a-descriptions-item>
            <a-descriptions-item label="专业版">
              <a-progress
                :percent="20"
                :stroke-color="'#1677ff'"
                :show-info="true"
                status="normal"
              />
            </a-descriptions-item>
          </a-descriptions>
          <div class="package-summary">
            <p class="summary-item">
              <span class="label">基础版</span>
              <span class="value">80%</span>
              <span class="subtext">(102 个租户)</span>
            </p>
            <p class="summary-item">
              <span class="label">专业版</span>
              <span class="value">20%</span>
              <span class="subtext">(26 个租户)</span>
            </p>
          </div>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getPlatformDashboardStats, getPlatformRecentLogs, type PlatformLogItem } from '@/api'

// ---- 统计卡片数据 ----
const statsCards = ref([
  { title: '租户总数', value: 0, color: '#3f8600' },
  { title: '活跃租户', value: 0, color: '#1890ff', prefix: '👥' },
  { title: '用户总数', value: 0, color: '#cf1322' },
  { title: '套餐数', value: 0, color: '#faad14' },
])

// ---- 操作日志数据 ----
const logs = ref<PlatformLogItem[]>([])

// ---- 加载数据 ----
onMounted(async () => {
  try {
    const res: any = await getPlatformDashboardStats()
    const data = res?.data?.data ?? res?.data ?? res
    if (data) {
      statsCards.value[0].value = data.tenantCount ?? 0
      statsCards.value[1].value = data.activeTenantCount ?? 0
      statsCards.value[2].value = data.userCount ?? 0
      statsCards.value[3].value = data.packageDistribution?.length ?? 0
    }
  } catch (e) {
    console.error('加载平台统计失败', e)
  }
  
  try {
    const res: any = await getPlatformRecentLogs(10)
    const data = res?.data?.data ?? res?.data ?? res
    if (Array.isArray(data)) {
      logs.value = data
    }
  } catch (e) {
    console.error('加载操作日志失败', e)
  }
})
</script>

<style scoped>
.platform-dashboard {
  padding: 24px;
  background: #f5f5f5;
  min-height: calc(100vh - 64px);
}

/* 欢迎横幅 */
.welcome-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.welcome-title {
  font-size: 28px;
  margin: 0;
  font-weight: 700;
}

.welcome-desc {
  color: rgba(255, 255, 255, 0.85);
  margin-top: 8px;
  font-size: 15px;
}

/* 统计卡片 */
.stat-card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
  margin-bottom: 16px;
}

.stat-card :deep(.ant-statistic-title) {
  font-size: 14px;
  color: #8c8c8c;
  margin-bottom: 8px;
}

.stat-card :deep(.ant-statistic-content) {
  font-size: 26px;
  font-weight: 600;
}

/* 日志列表 */
.package-summary {
  margin-top: 16px;
}

.summary-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
  border-bottom: 1px dashed #f0f0f0;
  margin: 0;
}

.summary-item:last-child {
  border-bottom: none;
}

.summary-item .label {
  color: #595959;
  font-weight: 500;
}

.summary-item .value {
  color: #262626;
  font-weight: 600;
}

.summary-item .subtext {
  color: #bfbfbf;
  font-size: 12px;
}
</style>
