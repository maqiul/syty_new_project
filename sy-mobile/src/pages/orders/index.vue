<template>
  <view class="orders-page">
    <!-- 顶部标题 -->
    <view class="page-header">
      <text class="page-title">我的订单</text>
      <text class="page-subtitle">查看历史穿线记录</text>
    </view>

    <!-- 手机号输入（首次使用） -->
    <view v-if="!phoneConfirmed" class="phone-card">
      <view class="card-title">
        <up-icon name="phone" size="20" color="#1890ff" />
        <text>输入手机号查询</text>
      </view>
      <view class="phone-input">
        <up-input
          v-model="phone"
          placeholder="请输入登记时的手机号"
          border="surround"
          shape="circle"
          type="number"
          :maxlength="11"
          clearable
        />
      </view>
      <up-button
        type="primary"
        shape="circle"
        :loading="loading"
        :disabled="phone.length !== 11"
        @click="fetchOrders"
      >
        查询订单
      </up-button>
    </view>

    <!-- 订单列表 -->
    <view v-else class="order-list">
      <!-- 切换手机号 -->
      <view class="switch-bar" @click="phoneConfirmed = false">
        <up-icon name="account" size="16" color="#1890ff" />
        <text class="switch-phone">{{ phone }}</text>
        <up-icon name="arrow-right" size="14" color="#909399" />
      </view>

      <!-- 加载中 -->
      <view v-if="loading" class="loading-state">
        <up-loading-icon mode="circle" size="40" color="#1890ff" />
        <text class="loading-text">加载中...</text>
      </view>

      <!-- 订单卡片列表 -->
      <view v-else-if="orders.length > 0">
        <view
          v-for="order in orders"
          :key="order.id"
          class="order-card"
          @click="toggleDetail(order.id)"
        >
          <!-- 卡片头部 -->
          <view class="card-header">
            <view class="status-tag" :class="getStatusClass(order.status)">
              <text>{{ getStatusText(order.status) }}</text>
            </view>
            <text class="order-time">{{ formatDate(order.createdAt) }}</text>
          </view>

          <!-- 订单信息 -->
          <view class="card-body">
            <view class="info-row">
              <text class="info-label">订单号</text>
              <text class="info-value">{{ order.orderNo }}</text>
            </view>
            <view v-if="order.racketModel" class="info-row">
              <text class="info-label">球拍</text>
              <text class="info-value">{{ order.racketModel }}</text>
            </view>
            <view v-if="order.mainStringName" class="info-row">
              <text class="info-label">主线</text>
              <text class="info-value">{{ order.mainStringName }}</text>
            </view>
            <view v-if="order.crossStringName" class="info-row">
              <text class="info-label">横线</text>
              <text class="info-value">{{ order.crossStringName }}</text>
            </view>
            <view v-if="order.mainPounds || order.crossPounds" class="info-row">
              <text class="info-label">磅数</text>
              <text class="info-value">
                主 {{ order.mainPounds || '-' }} / 横 {{ order.crossPounds || '-' }}
              </text>
            </view>
          </view>

          <!-- 展开的进度详情 -->
          <view v-if="expandedId === order.id" class="card-detail">
            <view class="progress-bar">
              <view class="progress-track">
                <view
                  class="progress-fill"
                  :style="{ width: `${getProgress(order.status)}%` }"
                />
              </view>
              <text class="progress-label">{{ getProgress(order.status) }}%</text>
            </view>

            <!-- 时间线 -->
            <view class="mini-timeline">
              <view
                v-for="(step, idx) in getTimelineSteps(order)"
                :key="idx"
                class="tl-item"
                :class="{ done: idx < getCurrentStep(order.status), active: idx === getCurrentStep(order.status) }"
              >
                <view class="tl-dot" />
                <view class="tl-info">
                  <text class="tl-title">{{ step.title }}</text>
                  <text v-if="step.time" class="tl-time">{{ step.time }}</text>
                </view>
              </view>
            </view>

            <!-- 备注 -->
            <view v-if="order.remark" class="order-remark">
              <up-icon name="info-circle" size="14" color="#faad14" />
              <text>{{ order.remark }}</text>
            </view>
          </view>

          <!-- 展开/收起指示 -->
          <view class="card-footer">
            <text class="footer-hint">
              {{ expandedId === order.id ? '收起详情' : '查看进度' }}
            </text>
            <up-icon
              :name="expandedId === order.id ? 'arrow-up' : 'arrow-down'"
              size="14"
              color="#909399"
            />
          </view>
        </view>
      </view>

      <!-- 空状态 -->
      <view v-else class="empty-state">
        <up-icon name="order" size="60" color="#dcdfe6" />
        <text class="empty-text">暂无订单记录</text>
        <text class="empty-hint">该手机号下没有穿线订单</text>
      </view>
    </view>

    <view class="safe-bottom" />
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { queryOrderByPhone } from '@/api/h5'

interface OrderItem {
  id: number
  orderNo: string
  status: number
  createdAt: string
  completedAt?: string
  racketModel?: string
  mainStringName?: string
  crossStringName?: string
  mainPounds?: number
  crossPounds?: number
  remark?: string
  [key: string]: any
}

const phone = ref('')
const phoneConfirmed = ref(false)
const loading = ref(false)
const orders = ref<OrderItem[]>([])
const expandedId = ref<number | null>(null)

/** 查询订单列表 */
const fetchOrders = async () => {
  if (phone.value.length !== 11) return

  loading.value = true
  phoneConfirmed.value = true

  try {
    const res = await queryOrderByPhone(phone.value)
    if (res.code === 200 && Array.isArray(res.data)) {
      orders.value = res.data
    } else {
      orders.value = []
    }
  } catch {
    uni.showToast({ title: '查询失败，请重试', icon: 'error' })
    orders.value = []
  } finally {
    loading.value = false
  }
}

/** 展开/收起订单详情 */
const toggleDetail = (id: number) => {
  expandedId.value = expandedId.value === id ? null : id
}

/** 状态文本 */
const getStatusText = (status: number): string => {
  const map: Record<number, string> = {
    0: '待确认',
    1: '已确认',
    2: '穿线中',
    3: '已完成',
    4: '已取件',
  }
  return map[status] || '未知'
}

/** 状态样式 */
const getStatusClass = (status: number): string => {
  const map: Record<number, string> = {
    0: 'tag-pending',
    1: 'tag-confirmed',
    2: 'tag-processing',
    3: 'tag-completed',
    4: 'tag-picked',
  }
  return map[status] || 'tag-pending'
}

/** 进度百分比 */
const getProgress = (status: number): number => {
  const map: Record<number, number> = { 0: 10, 1: 30, 2: 60, 3: 90, 4: 100 }
  return map[status] ?? 0
}

/** 当前步骤索引 */
const getCurrentStep = (status: number): number => {
  const map: Record<number, number> = { 0: 0, 1: 1, 2: 2, 3: 3, 4: 4 }
  return map[status] ?? 0
}

/** 时间线步骤 */
const getTimelineSteps = (order: OrderItem) => [
  { title: '订单创建', time: formatDate(order.createdAt) },
  { title: '已确认', time: '' },
  { title: '穿线中', time: '' },
  { title: '已完成', time: order.completedAt ? formatDate(order.completedAt) : '' },
  { title: '已取件', time: '' },
]

/** 日期格式化 */
const formatDate = (dateStr: string): string => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const h = String(d.getHours()).padStart(2, '0')
  const min = String(d.getMinutes()).padStart(2, '0')
  return `${m}-${day} ${h}:${min}`
}
</script>

<style lang="scss" scoped>
.orders-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding-bottom: env(safe-area-inset-bottom);
}

/* ===== 页面头部 ===== */
.page-header {
  padding: 60rpx 40rpx 40rpx;
  background: linear-gradient(135deg, #1890ff 0%, #36cfc9 100%);

  .page-title {
    display: block;
    font-size: 40rpx;
    font-weight: 700;
    color: #fff;
  }

  .page-subtitle {
    display: block;
    font-size: 24rpx;
    color: rgba(255, 255, 255, 0.8);
    margin-top: 8rpx;
  }
}

/* ===== 手机号输入卡片 ===== */
.phone-card {
  margin: -20rpx 30rpx 30rpx;
  background: #fff;
  border-radius: 24rpx;
  padding: 40rpx 30rpx;
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.06);

  .card-title {
    display: flex;
    align-items: center;
    gap: 12rpx;
    margin-bottom: 30rpx;

    text {
      font-size: 30rpx;
      font-weight: 600;
      color: #303133;
    }
  }

  .phone-input {
    margin-bottom: 30rpx;
  }
}

/* ===== 切换手机号 ===== */
.switch-bar {
  display: flex;
  align-items: center;
  gap: 10rpx;
  padding: 20rpx 30rpx;

  .switch-phone {
    flex: 1;
    font-size: 26rpx;
    color: #606266;
  }
}

/* ===== 加载状态 ===== */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80rpx 0;

  .loading-text {
    margin-top: 20rpx;
    font-size: 26rpx;
    color: #909399;
  }
}

/* ===== 订单卡片 ===== */
.order-card {
  margin: 0 30rpx 24rpx;
  background: #fff;
  border-radius: 20rpx;
  overflow: hidden;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);

  .card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 24rpx 30rpx;
    border-bottom: 1rpx solid #f0f2f5;

    .status-tag {
      padding: 6rpx 16rpx;
      border-radius: 8rpx;

      text {
        font-size: 22rpx;
        font-weight: 600;
      }

      &.tag-pending {
        background: #fff7e6;
        text { color: #fa8c16; }
      }
      &.tag-confirmed {
        background: #e6f7ff;
        text { color: #1890ff; }
      }
      &.tag-processing {
        background: #f0f5ff;
        text { color: #2f54eb; }
      }
      &.tag-completed {
        background: #f6ffed;
        text { color: #52c41a; }
      }
      &.tag-picked {
        background: #f5f5f5;
        text { color: #8c8c8c; }
      }
    }

    .order-time {
      font-size: 22rpx;
      color: #909399;
    }
  }

  .card-body {
    padding: 24rpx 30rpx;

    .info-row {
      display: flex;
      justify-content: space-between;
      padding: 8rpx 0;

      .info-label {
        font-size: 26rpx;
        color: #909399;
      }

      .info-value {
        font-size: 26rpx;
        color: #303133;
      }
    }
  }

  .card-detail {
    padding: 0 30rpx 24rpx;
    border-top: 1rpx dashed #e4e7ed;

    .progress-bar {
      display: flex;
      align-items: center;
      gap: 16rpx;
      padding: 24rpx 0;

      .progress-track {
        flex: 1;
        height: 12rpx;
        background: #e4e7ed;
        border-radius: 6rpx;
        overflow: hidden;

        .progress-fill {
          height: 100%;
          background: linear-gradient(90deg, #1890ff, #36cfc9);
          border-radius: 6rpx;
          transition: width 0.5s ease;
        }
      }

      .progress-label {
        font-size: 24rpx;
        font-weight: 600;
        color: #1890ff;
        min-width: 60rpx;
        text-align: right;
      }
    }
  }

  .card-footer {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8rpx;
    padding: 16rpx;
    background: #fafafa;

    .footer-hint {
      font-size: 22rpx;
      color: #909399;
    }
  }
}

/* ===== 迷你时间线 ===== */
.mini-timeline {
  padding-left: 10rpx;

  .tl-item {
    display: flex;
    gap: 16rpx;
    padding-bottom: 24rpx;
    position: relative;

    &:not(:last-child)::after {
      content: '';
      position: absolute;
      left: 11rpx;
      top: 28rpx;
      bottom: 0;
      width: 2rpx;
      background: #e4e7ed;
    }

    &.done:not(:last-child)::after {
      background: #1890ff;
    }

    .tl-dot {
      width: 24rpx;
      height: 24rpx;
      border-radius: 50%;
      background: #e4e7ed;
      flex-shrink: 0;
      margin-top: 4rpx;
      z-index: 1;
    }

    &.done .tl-dot {
      background: #1890ff;
    }

    &.active .tl-dot {
      background: #fff;
      border: 3rpx solid #1890ff;
      box-shadow: 0 0 0 4rpx rgba(24, 144, 255, 0.15);
    }

    .tl-info {
      .tl-title {
        display: block;
        font-size: 24rpx;
        color: #303133;
      }

      .tl-time {
        display: block;
        font-size: 20rpx;
        color: #c0c4cc;
        margin-top: 4rpx;
      }
    }
  }
}

/* ===== 备注 ===== */
.order-remark {
  display: flex;
  align-items: flex-start;
  gap: 10rpx;
  margin-top: 16rpx;
  padding: 16rpx 20rpx;
  background: #fffbe6;
  border-radius: 10rpx;

  text {
    flex: 1;
    font-size: 22rpx;
    color: #8c6d1f;
    line-height: 1.5;
  }
}

/* ===== 空状态 ===== */
.empty-state {
  text-align: center;
  padding: 120rpx 60rpx;

  .empty-text {
    display: block;
    font-size: 30rpx;
    color: #606266;
    margin-top: 24rpx;
  }

  .empty-hint {
    display: block;
    font-size: 24rpx;
    color: #c0c4cc;
    margin-top: 10rpx;
  }
}

.safe-bottom {
  height: 40rpx;
}
</style>
