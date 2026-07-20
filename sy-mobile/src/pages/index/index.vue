<template>
  <view class="query-page">
    <!-- 顶部 Banner 区域 -->
    <view class="banner">
      <view class="banner-content">
        <text class="banner-title">🏸 SY 穿线服务</text>
        <text class="banner-subtitle">专业穿线 · 实时追踪</text>
      </view>
    </view>

    <!-- 查询卡片 -->
    <view class="query-card">
      <view class="card-header">
        <text class="card-title">订单进度查询</text>
      </view>

      <!-- 输入区域 -->
      <view class="input-group">
        <!-- 切换标签 -->
        <view class="tab-bar">
          <view
            class="tab-item"
            :class="{ active: queryType === 'order' }"
            @click="queryType = 'order'"
          >
            <text>订单号</text>
          </view>
          <view
            class="tab-item"
            :class="{ active: queryType === 'phone' }"
            @click="queryType = 'phone'"
          >
            <text>手机号</text>
          </view>
        </view>

        <!-- 输入框 -->
        <view class="input-wrapper">
          <up-input
            v-model="queryValue"
            :placeholder="placeholderText"
            border="surround"
            shape="circle"
            clearable
            :maxlength="queryType === 'phone' ? 11 : 20"
          />
        </view>

        <!-- 查询按钮 -->
        <up-button
          type="primary"
          shape="circle"
          size="large"
          :loading="loading"
          :disabled="!queryValue.trim()"
          @click="handleQuery"
        >
          立即查询
        </up-button>
      </view>
    </view>

    <!-- 查询结果 -->
    <view v-if="result" class="result-card">
      <view class="result-header">
        <view class="status-badge" :class="result.statusClass">
          <text class="status-text">{{ result.statusText }}</text>
        </view>
        <text class="order-no">订单号：{{ result.orderNo }}</text>
      </view>

      <!-- 进度条 -->
      <view class="progress-section">
        <view class="progress-bar">
          <view class="progress-track">
            <view
              class="progress-fill"
              :style="{ width: `${result.progress}%` }"
            />
          </view>
          <text class="progress-text">{{ result.progress }}%</text>
        </view>
      </view>

      <!-- 时间节点 -->
      <view class="timeline">
        <view
          v-for="(step, idx) in result.steps"
          :key="idx"
          class="timeline-item"
          :class="{ completed: idx < result.currentStep, active: idx === result.currentStep }"
        >
          <view class="timeline-dot" :class="{ completed: idx < result.currentStep, active: idx === result.currentStep }">
            <up-icon v-if="idx < result.currentStep" name="checkmark" size="12" color="#fff" />
          </view>
          <view class="timeline-content">
            <text class="step-title">{{ step.title }}</text>
            <text class="step-time">{{ step.time }}</text>
          </view>
        </view>
      </view>

      <!-- 备注信息 -->
      <view v-if="result.remark" class="remark-box">
        <up-icon name="info-circle" size="16" color="#1890ff" />
        <text class="remark-text">{{ result.remark }}</text>
      </view>
    </view>

    <!-- 空状态提示（查询后无结果） -->
    <view v-if="showEmpty" class="empty-state">
      <up-icon name="search" size="60" color="#c0c4cc" />
      <text class="empty-text">未找到相关订单</text>
      <text class="empty-hint">请检查输入信息后重试</text>
    </view>

    <!-- 底部安全区 -->
    <view class="safe-bottom" />
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

type QueryType = 'order' | 'phone'

interface TimelineStep {
  title: string
  time: string
}

interface QueryResult {
  orderNo: string
  statusText: string
  statusClass: string
  progress: number
  currentStep: number
  steps: TimelineStep[]
  remark?: string
}

// 查询类型
const queryType = ref<QueryType>('order')
const queryValue = ref('')
const loading = ref(false)
const result = ref<QueryResult | null>(null)
const showEmpty = ref(false)

const placeholderText = computed(() =>
  queryType.value === 'order' ? '请输入订单号' : '请输入手机号'
)

/**
 * 模拟查询接口
 * TODO: 替换为真实 API 调用
 */
const mockQuery = async (): Promise<QueryResult | null> => {
  // 模拟网络延迟
  await new Promise((resolve) => setTimeout(resolve, 800))

  const value = queryValue.value.trim()

  // 模拟：输入任意非空值都返回数据，输入 "empty" 返回空
  if (value.toLowerCase() === 'empty') {
    return null
  }

  return {
    orderNo: queryType.value === 'order' ? value : 'SY20260512001',
    statusText: '穿线中',
    statusClass: 'status-processing',
    progress: 60,
    currentStep: 2,
    steps: [
      { title: '订单确认', time: '2026-05-12 09:30' },
      { title: '线材准备', time: '2026-05-12 10:00' },
      { title: '穿线进行中', time: '2026-05-12 10:30' },
      { title: '质检打包', time: '预计 14:00' },
      { title: '已完成', time: '待处理' },
    ],
    remark: '您的球拍正在专业穿线中，预计今日 14:00 前完成。',
  }
}

const handleQuery = async () => {
  if (!queryValue.value.trim()) return

  loading.value = true
  result.value = null
  showEmpty.value = false

  try {
    const data = await mockQuery()
    if (data) {
      result.value = data
    } else {
      showEmpty.value = true
    }
  } catch (error) {
    uni.showToast({
      title: '查询失败，请重试',
      icon: 'error',
    })
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.query-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #e8f4ff 0%, #f5f7fa 30%);
  padding-bottom: env(safe-area-inset-bottom);
}

/* ===== Banner ===== */
.banner {
  padding: 60rpx 40rpx 80rpx;
  text-align: center;

  .banner-content {
    .banner-title {
      display: block;
      font-size: 44rpx;
      font-weight: 700;
      color: #1a1a2e;
      margin-bottom: 12rpx;
    }

    .banner-subtitle {
      display: block;
      font-size: 26rpx;
      color: #606266;
      letter-spacing: 2rpx;
    }
  }
}

/* ===== 查询卡片 ===== */
.query-card {
  margin: -40rpx 30rpx 30rpx;
  background: #fff;
  border-radius: 24rpx;
  padding: 40rpx 30rpx;
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.06);

  .card-header {
    margin-bottom: 30rpx;

    .card-title {
      font-size: 32rpx;
      font-weight: 600;
      color: #303133;
    }
  }
}

/* ===== Tab 切换 ===== */
.tab-bar {
  display: flex;
  background: #f5f7fa;
  border-radius: 12rpx;
  padding: 6rpx;
  margin-bottom: 30rpx;

  .tab-item {
    flex: 1;
    text-align: center;
    padding: 16rpx 0;
    border-radius: 10rpx;
    transition: all 0.3s ease;

    text {
      font-size: 28rpx;
      color: #909399;
    }

    &.active {
      background: #fff;
      box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.08);

      text {
        color: #1890ff;
        font-weight: 600;
      }
    }
  }
}

/* ===== 输入 & 按钮 ===== */
.input-group {
  .input-wrapper {
    margin-bottom: 30rpx;
  }
}

/* ===== 结果卡片 ===== */
.result-card {
  margin: 0 30rpx 30rpx;
  background: #fff;
  border-radius: 24rpx;
  padding: 40rpx 30rpx;
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.06);

  .result-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 30rpx;

    .status-badge {
      padding: 8rpx 20rpx;
      border-radius: 20rpx;

      &.status-processing {
        background: rgba(24, 144, 255, 0.1);

        .status-text {
          color: #1890ff;
        }
      }

      .status-text {
        font-size: 24rpx;
        font-weight: 600;
      }
    }

    .order-no {
      font-size: 24rpx;
      color: #909399;
    }
  }
}

/* ===== 进度条 ===== */
.progress-section {
  margin-bottom: 40rpx;

  .progress-bar {
    display: flex;
    align-items: center;
    gap: 20rpx;

    .progress-track {
      flex: 1;
      height: 16rpx;
      background: #e4e7ed;
      border-radius: 8rpx;
      overflow: hidden;

      .progress-fill {
        height: 100%;
        background: linear-gradient(90deg, #1890ff, #36cfc9);
        border-radius: 8rpx;
        transition: width 0.6s ease;
      }
    }

    .progress-text {
      font-size: 28rpx;
      font-weight: 600;
      color: #1890ff;
      min-width: 70rpx;
      text-align: right;
    }
  }
}

/* ===== 时间线 ===== */
.timeline {
  padding-left: 20rpx;

  .timeline-item {
    display: flex;
    gap: 20rpx;
    padding-bottom: 30rpx;
    position: relative;

    &:not(:last-child)::after {
      content: '';
      position: absolute;
      left: 18rpx;
      top: 40rpx;
      bottom: 0;
      width: 2rpx;
      background: #e4e7ed;
    }

    &.completed:not(:last-child)::after {
      background: #1890ff;
    }

    .timeline-dot {
      width: 36rpx;
      height: 36rpx;
      border-radius: 50%;
      background: #e4e7ed;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
      z-index: 1;

      &.completed {
        background: #1890ff;
      }

      &.active {
        background: #fff;
        border: 3rpx solid #1890ff;
        box-shadow: 0 0 0 4rpx rgba(24, 144, 255, 0.15);
      }
    }

    .timeline-content {
      padding-top: 4rpx;

      .step-title {
        display: block;
        font-size: 28rpx;
        color: #303133;
        margin-bottom: 6rpx;
      }

      .step-time {
        display: block;
        font-size: 22rpx;
        color: #909399;
      }
    }
  }
}

/* ===== 备注 ===== */
.remark-box {
  margin-top: 30rpx;
  padding: 20rpx 24rpx;
  background: rgba(24, 144, 255, 0.06);
  border-radius: 12rpx;
  display: flex;
  align-items: flex-start;
  gap: 12rpx;

  .remark-text {
    flex: 1;
    font-size: 24rpx;
    color: #606266;
    line-height: 1.6;
  }
}

/* ===== 空状态 ===== */
.empty-state {
  text-align: center;
  padding: 100rpx 60rpx;

  .empty-text {
    display: block;
    font-size: 30rpx;
    color: #606266;
    margin-top: 20rpx;
  }

  .empty-hint {
    display: block;
    font-size: 24rpx;
    color: #909399;
    margin-top: 10rpx;
  }
}

/* ===== 底部安全区 ===== */
.safe-bottom {
  height: 40rpx;
}
</style>
