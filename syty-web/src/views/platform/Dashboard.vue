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
import { computed } from 'vue'

// ---- Mock 统计卡片数据 ----
const statsCards = computed(() => [
  {
    title: '租户总数',
    value: 128,
    color: '#3f8600',
  },
  {
    title: '在线用户',
    value: 45,
    color: '#1890ff',
    prefix: '👥',
  },
  {
    title: '今日收入',
    value: 3200,
    color: '#cf1322',
    prefix: '¥',
    precision: 2,
  },
  {
    title: '活跃套餐',
    value: '基础版',
    suffix: ' 80%',
    color: '#faad14',
  },
])

// ---- Mock 操作日志数据 ----
const logs = [
  {
    user: '管理员',
    avatarColor: '#1677ff',
    content: '新增租户「星辰教育」',
    time: '2026-05-13 10:30',
  },
  {
    user: '管理员',
    avatarColor: '#1677ff',
    content: '将「云端科技」套餐升级至专业版',
    time: '2026-05-13 09:45',
  },
  {
    user: '系统',
    avatarColor: '#52c41a',
    content: '租户「启航体育」已过期，自动停用',
    time: '2026-05-13 08:00',
  },
  {
    user: '管理员',
    avatarColor: '#1677ff',
    content: '新增套餐「企业定制版」',
    time: '2026-05-12 16:20',
  },
  {
    user: '管理员',
    avatarColor: '#1677ff',
    content: '删除租户「测试公司」',
    time: '2026-05-12 14:10',
  },
]
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
