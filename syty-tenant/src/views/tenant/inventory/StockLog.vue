<template>
  <div>
    <a-card>
      <template #title>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>📋 库存流水</span>
          <a-button @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button>
        </div>
      </template>

      <!-- 筛选栏 -->
      <div style="margin-bottom: 16px">
        <a-space wrap>
          <a-select v-model:value="filters.shopId" style="width: 160px" placeholder="选择店铺" allow-clear @change="loadData">
            <a-select-option v-for="s in shopList" :key="s.id" :value="s.id">{{ s.name }}</a-select-option>
          </a-select>
          <a-select v-model:value="filters.changeType" style="width: 140px" placeholder="变动类型" allow-clear @change="loadData">
            <a-select-option value="PURCHASE_IN">采购入库</a-select-option>
            <a-select-option value="ORDER_OUT">订单扣减</a-select-option>
            <a-select-option value="MANUAL_ADJUST">手动调整</a-select-option>
            <a-select-option value="RETURN_IN">退货入库</a-select-option>
            <a-select-option value="TRANSFER">调拨</a-select-option>
          </a-select>
          <a-range-picker v-model:value="filters.dateRange" @change="loadData" />
        </a-space>
      </div>

      <a-table :columns="columns" :dataSource="tableData" :loading="loading" :pagination="pagination" rowKey="id" @change="handleTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'quantity'">
            <span :style="{ color: record.quantity > 0 ? '#52c41a' : '#ff4d4f', fontWeight: 'bold' }">
              {{ record.quantity > 0 ? '+' : '' }}{{ record.quantity }}
            </span>
          </template>
          <template v-if="column.dataIndex === 'changeType'">
            <a-tag :color="changeTypeColor[record.changeType]">{{ changeTypeText[record.changeType] }}</a-tag>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ReloadOutlined } from '@antdv-next/icons'
import { getStockLogPage } from '@/api/tenant/inventory'

const loading = ref(false)
const tableData = ref<any[]>([])
const shopList = ref<any[]>([{ id: 1, name: '默认店铺' }])
const pagination = reactive({ current: 1, pageSize: 20, total: 0, showSizeChanger: true, showTotal: (t: number) => `共 ${t} 条` })
const filters = reactive<{ shopId: number | undefined; changeType: string | undefined; dateRange: any }>({
  shopId: undefined, changeType: undefined, dateRange: null
})

const changeTypeText: Record<string, string> = {
  PURCHASE_IN: '采购入库', ORDER_OUT: '订单扣减', MANUAL_ADJUST: '手动调整', RETURN_IN: '退货入库', TRANSFER: '调拨'
}
const changeTypeColor: Record<string, string> = {
  PURCHASE_IN: 'green', ORDER_OUT: 'red', MANUAL_ADJUST: 'orange', RETURN_IN: 'blue', TRANSFER: 'purple'
}

const columns = [
  { title: '时间', dataIndex: 'createdAt', key: 'createdAt', width: 180 },
  { title: '变动类型', dataIndex: 'changeType', key: 'changeType', width: 120 },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 100 },
  { title: '变动前', dataIndex: 'beforeQuantity', key: 'beforeQuantity', width: 90 },
  { title: '变动后', dataIndex: 'afterQuantity', key: 'afterQuantity', width: 90 },
  { title: '关联订单', dataIndex: 'orderNo', key: 'orderNo', width: 150 },
  { title: '操作人', dataIndex: 'operatorName', key: 'operatorName', width: 100 },
  { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true }
]

const loadData = async () => {
  loading.value = true
  try {
    const params: any = { page: pagination.current, size: pagination.pageSize }
    if (filters.shopId) params.shopId = filters.shopId
    if (filters.changeType) params.changeType = filters.changeType
    if (filters.dateRange && filters.dateRange.length === 2) {
      params.startDate = filters.dateRange[0].format('YYYY-MM-DD')
      params.endDate = filters.dateRange[1].format('YYYY-MM-DD')
    }
    const res: any = await getStockLogPage(params)
    const data = res?.data?.data ?? res?.data ?? res
    tableData.value = data?.records || data?.list || []
    pagination.total = data?.total || 0
  } catch { tableData.value = [] }
  finally { loading.value = false }
}

const handleTableChange = (p: any) => { pagination.current = p.current; pagination.pageSize = p.pageSize; loadData() }

onMounted(loadData)
</script>
