<template>
  <div>
    <a-card>
      <template #title>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>穿线订单</span>
          <div>
            <a-button style="margin-right: 8px" @click="handleExport">
              <template #icon><DownloadOutlined /></template>
              导出Excel
            </a-button>
            <a-button type="primary" @click="$router.push('/order/create')">
              <template #icon><PlusOutlined /></template>
              新建订单
            </a-button>
          </div>
        </div>
      </template>

      <div style="margin-bottom: 16px">
        <a-space wrap>
          <span>店铺：</span>
          <a-select v-model:value="query.shopId" placeholder="选择店铺" allow-clear style="width:180px">
            <a-select-option v-for="s in shopList" :key="s.id" :value="s.id">{{ s.name }}</a-select-option>
          </a-select>
          <span>状态：</span>
          <a-select v-model:value="query.status" placeholder="选择状态" allow-clear style="width:120px">
            <a-select-option :value="0">待穿</a-select-option>
            <a-select-option :value="1">已穿</a-select-option>
            <a-select-option :value="2">已取</a-select-option>
          </a-select>
          <span>单号：</span>
          <a-input v-model:value="query.keyword" placeholder="订单号" allow-clear style="width:180px" @pressEnter="search" />
          <a-button type="primary" @click="search">搜索</a-button>
          <a-button @click="resetSearch">重置</a-button>
        </a-space>
      </div>

      <a-table :columns="columns" :dataSource="tableData" :loading="loading" :pagination="pagination" rowKey="id" @change="handleTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'mainTension'">{{ record.mainTension }} lbs</template>
          <template v-if="column.dataIndex === 'totalPrice'">¥{{ record.totalPrice?.toFixed(2) }}</template>
          <template v-if="column.dataIndex === 'status'">
            <a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a @click="$router.push(`/order/${record.id}/detail`)">详情</a>
              <a-divider type="vertical" />
              <a @click="handleEdit(record)">编辑</a>
              <a-divider type="vertical" />
              <a type="danger" @click="handleDelete(record.id)">删除</a>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'antdv-next'
import { PlusOutlined, DownloadOutlined } from '@antdv-next/icons'
import { getOrderPage, deleteOrder, exportOrders, getShopList, type Shop, type StringingOrder } from '@/api'

const router = useRouter()
const loading = ref(false)
const tableData = ref<StringingOrder[]>([])
const shopList = ref<Shop[]>([])
const query = reactive({ page: 1, size: 20, shopId: undefined as number | undefined, status: undefined as number | undefined, keyword: '' })
const pagination = reactive({ current: 1, pageSize: 20, total: 0, showTotal: (t: number) => `共 ${t} 条` })

const columns = [
  { title: '订单号', dataIndex: 'orderNo', width: 180 },
  { title: '店铺', dataIndex: 'shopName', width: 150 },
  { title: '球员', dataIndex: 'playerName', width: 120 },
  { title: '球拍', dataIndex: 'racketBrand', width: 160 },
  { title: '磅数', dataIndex: 'mainTension', width: 100 },
  { title: '总价', dataIndex: 'totalPrice', width: 100 },
  { title: '状态', dataIndex: 'status', width: 100 },
  { title: '创建时间', dataIndex: 'createdAt', width: 180 },
  { title: '操作', key: 'action', width: 280 }
]

const statusColor = (s: number) => ['default', 'processing', 'success'][s] || 'default'
const statusText = (s: number) => ['待穿', '已穿', '已取'][s] || '未知'

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getOrderPage(query)
    tableData.value = res.data.records || res.data
    pagination.total = res.data.total || 0
  } finally { loading.value = false }
}

const handleTableChange = (pag: any) => {
  query.page = pag.current; query.size = pag.pageSize
  pagination.current = pag.current; pagination.pageSize = pag.pageSize
  fetchData()
}
const search = () => { query.page = 1; fetchData() }
const resetSearch = () => { Object.assign(query, { page: 1, shopId: undefined, status: undefined, keyword: '' }); fetchData() }

const handleEdit = (row: StringingOrder) => {
  router.push(`/order/${row.id}/detail?edit=true`)
}

const handleDelete = (id: number) => {
  Modal.confirm({ title: '确认删除', content: '确定要删除该订单吗？', okText: '确定',
    onOk: async () => { await deleteOrder(id); message.success('删除成功'); fetchData() }
  })
}

const handleExport = async () => {
  try {
    const res = await exportOrders(query)
    const blob = new Blob([res as any], { type: 'application/vnd.ms-excel' })
    const link = document.createElement('a')
    link.download = `订单导出_${new Date().toISOString().slice(0,10)}.xlsx`
    link.href = URL.createObjectURL(blob)
    link.click()
    URL.revokeObjectURL(link.href)
  } catch {
    message.error('导出失败')
  }
}

onMounted(async () => {
  try {
    const res = await getShopList()
    shopList.value = res.data || []
  } catch { /* ignore */ }
  fetchData()
})
</script>
