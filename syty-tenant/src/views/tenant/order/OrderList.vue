<template>
  <div class="order-list-container">
    <a-card title="订单列表" :bordered="false">
      <template #extra>
        <a-button type="primary" @click="fetchData" :loading="loading">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </template>

      <a-table
        :columns="columns"
        :data-source="data"
        :pagination="pagination"
        :loading="loading"
        rowKey="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'playerInfo'">
            <div>{{ record.playerName }}</div>
            <div class="text-gray">{{ record.phone }}</div>
          </template>

          <template v-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">
              {{ getStatusText(record.status) }}
            </a-tag>
          </template>

          <template v-if="column.key === 'totalAmount'">
            <span class="text-orange">¥{{ record.totalAmount }}</span>
          </template>

          <template v-if="column.key === 'action'">
            <a-button 
              v-if="record.status === 'PENDING'" 
              type="link" 
              size="small" 
              @click="handleAction(record, 'STRINGING')"
              :loading="actionLoading[record.id]"
            >
              接单
            </a-button>
            <a-button 
              v-if="record.status === 'STRINGING'" 
              type="link" 
              size="small" 
              @click="handleAction(record, 'DONE')"
              :loading="actionLoading[record.id]"
            >
              完工
            </a-button>
            <span v-if="record.status === 'DONE'" class="text-gray">已完成</span>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ReloadOutlined } from '@antdv-next/icons'
import request from '@/utils/axios' 

const data = ref<any[]>([])
const loading = ref(false)
const pagination = ref({
  current: 1,
  pageSize: 10,
  total: 0
})

const columns = [
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo', width: 180 },
  { title: '客户信息', key: 'playerInfo', width: 150 },
  { title: '球拍型号', dataIndex: 'racketModel', key: 'racketModel', width: 150 },
  { title: '状态', key: 'status', width: 100 },
  { title: '金额', key: 'totalAmount', width: 100, align: 'right' },
  { title: '下单时间', dataIndex: 'createdAt', key: 'createdAt', width: 180 },
  { title: '操作', key: 'action', width: 150, fixed: 'right' }
]

const actionLoading = ref<Record<string, boolean>>({})

const handleAction = async (record: any, status: string) => {
  actionLoading.value[record.id] = true
  try {
    await request.put(`/tenant/orders/${record.id}/status`, { status })
    record.status = status // 乐观更新
  } catch (e) {
    console.error('Action failed', e)
  } finally {
    actionLoading.value[record.id] = false
  }
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await request.get('/tenant/orders/list', {
      params: {
        page: pagination.value.current,
        size: pagination.value.pageSize
      }
    })
    const records = res.data.records || res.data || []
    data.value = records
    pagination.value.total = res.data.total || records.length
  } catch (e) {
    console.error('Failed to fetch orders', e)
  } finally {
    loading.value = false
  }
}

const getStatusColor = (status: string) => {
  const map: Record<string, string> = {
    PENDING: 'orange',
    STRINGING: 'blue',
    DONE: 'green'
  }
  return map[status] || 'default'
}

const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    PENDING: '待接单',
    STRINGING: '穿线中',
    DONE: '已完成'
  }
  return map[status] || status
}

onMounted(fetchData)
</script>

<style scoped>
.text-gray { color: #999; font-size: 12px; }
.text-orange { color: #fa8c16; font-weight: bold; }
.order-list-container { padding: 24px; background: #f0f2f5; min-height: 100vh; }
</style>
