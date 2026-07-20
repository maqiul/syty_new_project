<template>
  <div class="kanban-page">
    <div class="kanban-header">
      <h1 class="kanban-title">🏸 穿线看板</h1>
      <a-button @click="refreshOrders" :loading="loading" size="large">
        <template #icon><ReloadOutlined /></template>
        刷新
      </a-button>
    </div>

    <div class="kanban-board">
      <!-- 待穿 -->
      <div class="kanban-column" :class="`column-${STATUS_PENDING}`">
        <div class="column-header">
          <span class="column-icon">📋</span>
          <h2 class="column-title">待穿</h2>
          <a-badge :count="pendingOrders.length" :number-style="{ backgroundColor: '#fff', color: '#666', fontSize: '18px', fontWeight: 'bold' }" />
        </div>
        <div class="column-body">
          <div v-if="loading" class="loading-placeholder">加载中...</div>
          <div v-else-if="pendingOrders.length === 0" class="empty-placeholder">暂无订单</div>
          <div
            v-for="order in pendingOrders"
            :key="order.id"
            class="order-card"
            @click="viewOrder(order)"
          >
            <div class="card-order-no">{{ order.orderNo }}</div>
            <div class="card-player">
              <UserOutlined /> {{ order.playerName || '未指定' }}
            </div>
            <div class="card-detail">
              <span class="detail-label">主线</span>
              <span class="detail-value">{{ order.mainStringName || '-' }} / {{ order.mainTension }}lbs</span>
            </div>
            <div class="card-detail" v-if="order.crossStringName">
              <span class="detail-label">横线</span>
              <span class="detail-value">{{ order.crossStringName }} / {{ order.crossTension }}lbs</span>
            </div>
            <div class="card-detail" v-if="order.stringerName">
              <span class="detail-label">穿线师</span>
              <span class="detail-value">{{ order.stringerName }}</span>
            </div>
            <div class="card-footer">
              <span class="card-shop">{{ order.shopName }}</span>
              <span class="card-time">{{ formatTime(order.createdAt) }}</span>
            </div>
            <div class="card-actions">
              <a-button type="primary" size="small" @click.stop="startStringing(order)">开始穿线</a-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 穿线中 -->
      <div class="kanban-column" :class="`column-${STATUS_PROCESSING}`">
        <div class="column-header">
          <span class="column-icon">🧵</span>
          <h2 class="column-title">穿线中</h2>
          <a-badge :count="processingOrders.length" :number-style="{ backgroundColor: '#fff', color: '#666', fontSize: '18px', fontWeight: 'bold' }" />
        </div>
        <div class="column-body">
          <div v-if="loading" class="loading-placeholder">加载中...</div>
          <div v-else-if="processingOrders.length === 0" class="empty-placeholder">暂无订单</div>
          <div
            v-for="order in processingOrders"
            :key="order.id"
            class="order-card card-processing"
            @click="viewOrder(order)"
          >
            <div class="card-order-no">{{ order.orderNo }}</div>
            <div class="card-player">
              <UserOutlined /> {{ order.playerName || '未指定' }}
            </div>
            <div class="card-detail">
              <span class="detail-label">主线</span>
              <span class="detail-value">{{ order.mainStringName || '-' }} / {{ order.mainTension }}lbs</span>
            </div>
            <div class="card-detail" v-if="order.crossStringName">
              <span class="detail-label">横线</span>
              <span class="detail-value">{{ order.crossStringName }} / {{ order.crossTension }}lbs</span>
            </div>
            <div class="card-detail" v-if="order.stringerName">
              <span class="detail-label">穿线师</span>
              <span class="detail-value">{{ order.stringerName }}</span>
            </div>
            <div class="card-footer">
              <span class="card-shop">{{ order.shopName }}</span>
              <span class="card-time">{{ formatTime(order.createdAt) }}</span>
            </div>
            <div class="card-actions">
              <a-button type="primary" size="small" @click.stop="completeOrder(order)">完成</a-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 已完成 -->
      <div class="kanban-column" :class="`column-${STATUS_COMPLETED}`">
        <div class="column-header">
          <span class="column-icon">✅</span>
          <h2 class="column-title">已完成</h2>
          <a-badge :count="completedOrders.length" :number-style="{ backgroundColor: '#fff', color: '#666', fontSize: '18px', fontWeight: 'bold' }" />
        </div>
        <div class="column-body">
          <div v-if="loading" class="loading-placeholder">加载中...</div>
          <div v-else-if="completedOrders.length === 0" class="empty-placeholder">暂无订单</div>
          <div
            v-for="order in completedOrders"
            :key="order.id"
            class="order-card card-completed"
            @click="viewOrder(order)"
          >
            <div class="card-order-no">{{ order.orderNo }}</div>
            <div class="card-player">
              <UserOutlined /> {{ order.playerName || '未指定' }}
            </div>
            <div class="card-detail">
              <span class="detail-label">主线</span>
              <span class="detail-value">{{ order.mainStringName || '-' }} / {{ order.mainTension }}lbs</span>
            </div>
            <div class="card-detail" v-if="order.crossStringName">
              <span class="detail-label">横线</span>
              <span class="detail-value">{{ order.crossStringName }} / {{ order.crossTension }}lbs</span>
            </div>
            <div class="card-detail" v-if="order.stringerName">
              <span class="detail-label">穿线师</span>
              <span class="detail-value">{{ order.stringerName }}</span>
            </div>
            <div class="card-footer">
              <span class="card-shop">{{ order.shopName }}</span>
              <span class="card-time">完成: {{ formatTime(order.completedAt) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'antdv-next'
import { ReloadOutlined, UserOutlined } from '@antdv-next/icons'
import { getOrderPage, completeOrder as apiCompleteOrder, type StringingOrder } from '@/api'

const router = useRouter()
const loading = ref(false)
const orders = ref<StringingOrder[]>([])

/** 订单状态常量 */
const STATUS_PENDING = 0     // 待穿
const STATUS_PROCESSING = 1  // 穿线中
const STATUS_COMPLETED = 2   // 已完成

/** 按状态分组 */
const pendingOrders = computed(() => orders.value.filter(o => o.status === STATUS_PENDING))
const processingOrders = computed(() => orders.value.filter(o => o.status === STATUS_PROCESSING))
const completedOrders = computed(() => orders.value.filter(o => o.status === STATUS_COMPLETED))

/** 加载订单 */
const loadOrders = async () => {
  loading.value = true
  try {
    const res = await getOrderPage({ status: -1, size: 200 })
    orders.value = res.data?.records || []
  } catch {
    message.error('加载订单失败')
  } finally {
    loading.value = false
  }
}

/** 刷新订单 */
const refreshOrders = () => {
  loadOrders()
}

/** 开始穿线 */
const startStringing = async (order: StringingOrder) => {
  try {
    // TODO: 调用后端接口更新状态为穿线中 (status=1)
    // await updateOrderStatus(order.id!, 1)
    message.info(`开始穿线: ${order.orderNo} (待后端接口就绪)`)
  } catch {
    message.error('操作失败')
  }
}

/** 完成穿线 */
const completeOrder = async (order: StringingOrder) => {
  try {
    await apiCompleteOrder(order.id!)
    message.success(`${order.orderNo} 已完成`)
    await loadOrders()
  } catch {
    message.error('操作失败')
  }
}

/** 查看订单详情 */
const viewOrder = (order: StringingOrder) => {
  router.push(`/order/detail?id=${order.id}`)
}

/** 格式化时间 */
const formatTime = (time?: string): string => {
  if (!time) return '-'
  const date = new Date(time)
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${month}-${day} ${hour}:${minute}`
}

/** 自动刷新定时器 */
let refreshTimer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  loadOrders()
  // 每30秒自动刷新
  refreshTimer = setInterval(loadOrders, 30000)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
})
</script>

<style scoped>
.kanban-page {
  padding: 20px;
  min-height: 100vh;
  background: #f0f2f5;
}

.kanban-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.kanban-title {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  color: #1a1a1a;
}

/* ========== 看板容器 ========== */
.kanban-board {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  min-height: calc(100vh - 120px);
}

/* ========== 列样式 ========== */
.kanban-column {
  background: #e8eaf0;
  border-radius: 12px;
  padding: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.column-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 20px;
  border-radius: 12px 12px 0 0;
}

.column-0 .column-header {
  background: #fadb14;
  color: #333;
}

.column-1 .column-header {
  background: #1890ff;
  color: #fff;
}

.column-2 .column-header {
  background: #52c41a;
  color: #fff;
}

.column-icon {
  font-size: 24px;
}

.column-title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  flex: 1;
}

.column-body {
  padding: 16px;
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.loading-placeholder,
.empty-placeholder {
  text-align: center;
  padding: 40px 20px;
  color: #999;
  font-size: 18px;
}

/* ========== 卡片样式 ========== */
.order-card {
  background: #fff;
  border-radius: 10px;
  padding: 18px 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
  border-left: 5px solid #1890ff;
}

.order-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
}

.card-processing {
  border-left-color: #1890ff;
  background: linear-gradient(135deg, #fff 85%, #e6f7ff 100%);
}

.card-completed {
  border-left-color: #52c41a;
  opacity: 0.9;
}

.card-order-no {
  font-size: 20px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 8px;
  font-family: 'Courier New', monospace;
}

.card-player {
  font-size: 18px;
  color: #333;
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.card-detail {
  display: flex;
  justify-content: space-between;
  font-size: 16px;
  padding: 4px 0;
}

.detail-label {
  color: #666;
  font-weight: 500;
}

.detail-value {
  color: #333;
  font-weight: 600;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid #f0f0f0;
  font-size: 14px;
  color: #888;
}

.card-shop {
  font-weight: 500;
}

.card-time {
  font-style: italic;
}

.card-actions {
  margin-top: 12px;
  display: flex;
  gap: 8px;
}

.card-actions .ant-btn {
  flex: 1;
  height: 38px;
  font-size: 16px;
  font-weight: 600;
}

/* ========== 响应式 ========== */
@media (max-width: 1200px) {
  .kanban-board {
    grid-template-columns: 1fr;
  }
}
</style>
