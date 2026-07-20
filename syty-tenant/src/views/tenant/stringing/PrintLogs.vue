<template>
  <div>
    <a-card :bordered="false">
      <template #title>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>打印日志</span>
          <a-button type="primary" @click="fetchLogs">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
        </div>
      </template>

      <!-- 筛选区 -->
      <div style="margin-bottom: 16px">
        <a-space>
          <a-input-search
            v-model:value="keyword"
            placeholder="搜索订单号 / 打印机名称"
            style="width: 280px"
            allow-clear
            @search="onFilterChange"
            @clear="onFilterChange"
          >
            <template #prefix>
              <SearchOutlined />
            </template>
          </a-input-search>

          <span>店铺：</span>
          <a-select
            v-model:value="filterShopId"
            style="width: 200px"
            placeholder="全部店铺"
            allow-clear
            @change="onFilterChange"
          >
            <a-select-option v-for="s in shopList" :key="s.id" :value="s.id">
              {{ s.name }}
            </a-select-option>
          </a-select>

          <span>状态：</span>
          <a-select
            v-model:value="filterStatus"
            style="width: 140px"
            placeholder="全部状态"
            allow-clear
            @change="onFilterChange"
          >
            <a-select-option value="SUCCESS">成功</a-select-option>
            <a-select-option value="FAILED">失败</a-select-option>
            <a-select-option value="PENDING">进行中</a-select-option>
          </a-select>

          <span>时间：</span>
          <a-range-picker
            v-model:value="dateRange"
            style="width: 260px"
            @change="onFilterChange"
          />
        </a-space>
      </div>

      <!-- 打印日志表格 -->
      <a-table
        :columns="columns"
        :dataSource="logList"
        :loading="loading"
        :pagination="pagination"
        rowKey="id"
        @change="handleTableChange"
      >
        <!-- 打印状态 -->
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'status'">
            <a-tag :color="statusColorMap[record.status]">
              {{ statusLabelMap[record.status] || record.status }}
            </a-tag>
          </template>

          <!-- 打印时间 -->
          <template v-else-if="column.dataIndex === 'printTime'">
            {{ formatTime(record.printTime) }}
          </template>

          <!-- 失败原因 -->
          <template v-else-if="column.dataIndex === 'errorMessage'">
            <a-tooltip v-if="record.errorMessage" :title="record.errorMessage">
              <span style="color: #ff4d4f; cursor: help">查看原因</span>
            </a-tooltip>
            <span v-else>-</span>
          </template>
        </template>
      </a-table>

      <!-- 空状态 -->
      <a-empty
        v-if="!loading && logList.length === 0"
        description="暂无打印日志"
      />
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message, type Dayjs } from 'antdv-next'
import { ReloadOutlined, SearchOutlined } from '@antdv-next/icons'
import { getPrintLogPage, getShopList, type PrintLog, type Shop } from '@/api'

/* ==================== 筛选条件 ==================== */
const shopList = ref<Shop[]>([])
const keyword = ref<string>('')
const filterShopId = ref<number | undefined>(undefined)
const filterStatus = ref<string | undefined>(undefined)
const dateRange = ref<[Dayjs, Dayjs] | undefined>(undefined)

/* ==================== 表格数据 ==================== */
const loading = ref(false)
const logList = ref<PrintLog[]>([])

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`
})

/* ==================== 状态映射 ==================== */
const statusColorMap: Record<string, string> = {
  SUCCESS: 'success',
  FAILED: 'error',
  PENDING: 'processing'
}

const statusLabelMap: Record<string, string> = {
  SUCCESS: '成功',
  FAILED: '失败',
  PENDING: '进行中'
}

/* ==================== 表格列定义 ==================== */
const columns = [
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo', width: 180 },
  { title: '打印机名称', dataIndex: 'printerName', key: 'printerName', width: 180 },
  { title: '模板名称', dataIndex: 'templateName', key: 'templateName', width: 160 },
  { title: '打印状态', dataIndex: 'status', key: 'status', width: 110 },
  { title: '打印时间', dataIndex: 'printTime', key: 'printTime', width: 180 },
  { title: '失败原因', dataIndex: 'errorMessage', key: 'errorMessage', ellipsis: true }
]

/* ==================== 加载数据 ==================== */
const fetchLogs = async () => {
  loading.value = true
  try {
    const params: Record<string, any> = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    }
    if (keyword.value) {
      params.keyword = keyword.value
    }
    if (filterShopId.value) {
      params.shopId = filterShopId.value
    }
    if (filterStatus.value) {
      params.status = filterStatus.value
    }
    if (dateRange.value && dateRange.value.length === 2) {
      params.startTime = dateRange.value[0].format('YYYY-MM-DD 00:00:00')
      params.endTime = dateRange.value[1].format('YYYY-MM-DD 23:59:59')
    }

    const res = await getPrintLogPage(params)
    const data = (res as any)?.data
    logList.value = data?.records || []
    pagination.total = data?.total || 0
  } catch (err: any) {
    message.error('获取打印日志失败: ' + (err.message || err))
  } finally {
    loading.value = false
  }
}

const onFilterChange = () => {
  pagination.current = 1
  fetchLogs()
}

const handleTableChange = (pag: any) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchLogs()
}

/* ==================== 工具函数 ==================== */
const formatTime = (time?: string) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN', { hour12: false })
}

/* ==================== 初始化 ==================== */
onMounted(async () => {
  try {
    const res = await getShopList()
    shopList.value = (res as any)?.data || []
  } catch {
    // 店铺列表加载失败不影响日志展示
  }
  fetchLogs()
})
</script>
