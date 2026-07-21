<template>
  <div>
    <!-- 统计卡片 -->
    <a-row :gutter="[16, 16]" style="margin-bottom: 16px">
      <a-col :span="6">
        <a-card hoverable>
          <a-statistic
            title="今日营收"
            :value="stats.todayRevenue"
            :precision="2"
            :value-style="{ color: '#1677ff' }"
          >
            <template #prefix>
              <PayCircleOutlined />
            </template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card hoverable>
          <a-statistic
            title="本月营收"
            :value="stats.monthRevenue"
            :precision="2"
            :value-style="{ color: '#722ed1' }"
          >
            <template #prefix>
              <CalendarOutlined />
            </template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card hoverable>
          <a-statistic
            title="挂账总额"
            :value="stats.onAccountTotal"
            :precision="2"
            :value-style="{ color: '#fa8c16' }"
          >
            <template #prefix>
              <FileTextOutlined />
            </template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card hoverable>
          <a-statistic
            title="欠款客户数"
            :value="stats.debtPlayerCount"
            :value-style="{ color: '#cf1322' }"
          >
            <template #prefix>
              <TeamOutlined />
            </template>
          </a-statistic>
        </a-card>
      </a-col>
    </a-row>

    <!-- Tab 切换 -->
    <a-tabs v-model:activeKey="activeTab">
      <!-- Tab 1: 订单列表 -->
      <a-tab-pane key="orders" tab="订单列表">
      <a-card>
      <template #title>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>穿线订单列表</span>
        </div>
      </template>

      <!-- 筛选区 -->
      <div style="margin-bottom: 16px">
        <a-space wrap>
          <span>店铺：</span>
          <a-select
            v-model:value="query.shopId"
            placeholder="全部店铺"
            allow-clear
            style="width: 180px"
          >
            <a-select-option v-for="s in shopList" :key="s.id" :value="s.id">{{ s.name }}</a-select-option>
          </a-select>

          <span>支付状态：</span>
          <a-select
            v-model:value="query.payStatus"
            placeholder="全部"
            allow-clear
            style="width: 120px"
          >
            <a-select-option value="">全部</a-select-option>
            <a-select-option value="UNPAID">未付</a-select-option>
            <a-select-option value="PAID">已付</a-select-option>
            <a-select-option value="ON_ACCOUNT">挂账</a-select-option>
          </a-select>

          <span>单号：</span>
          <a-input
            v-model:value="query.keyword"
            placeholder="订单号 / 客户名"
            allow-clear
            style="width: 180px"
            @pressEnter="search"
          />

          <span>日期：</span>
          <a-range-picker v-model:value="query.dateRange" style="width: 240px" />

          <a-button type="primary" @click="search">搜索</a-button>
          <a-button @click="resetSearch">重置</a-button>
        </a-space>
      </div>

      <!-- 订单表格 -->
      <a-table
        :columns="columns"
        :dataSource="tableData"
        :loading="loading"
        :pagination="pagination"
        rowKey="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <!-- 支付状态 -->
          <template v-if="column.dataIndex === 'payStatus'">
            <a-tag :color="payStatusColor(record.payStatus)">
              {{ payStatusText(record.payStatus) }}
            </a-tag>
          </template>

          <!-- 金额 -->
          <template v-if="column.dataIndex === 'totalPrice'">
            <span style="color: #f5222d; font-weight: 600">
              ¥{{ record.totalPrice?.toFixed(2) }}
            </span>
          </template>

          <!-- 操作列 -->
          <template v-if="column.key === 'action'">
            <a-space>
              <a @click="openPaymentDialog(record)">支付记录</a>
              <a-divider type="vertical" />
              <a @click="viewOrderDetail(record)">查看详情</a>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 支付记录弹窗 -->
    <a-modal
      v-model:open="paymentVisible"
      :title="`订单 ${currentOrder?.orderNo} - 支付记录`"
      width="700px"
      :footer="null"
    >
      <a-table
        :columns="paymentColumns"
        :dataSource="paymentData"
        :loading="paymentLoading"
        rowKey="id"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <!-- 支付方式 -->
          <template v-if="column.dataIndex === 'payMethod'">
            <a-tag :color="payMethodColor(record.payMethod)">
              {{ payMethodText(record.payMethod) }}
            </a-tag>
          </template>

          <!-- 金额 -->
          <template v-if="column.dataIndex === 'amount'">
            <span style="color: #52c41a; font-weight: 600">¥{{ record.amount?.toFixed(2) }}</span>
          </template>

          <!-- 状态 -->
          <template v-if="column.dataIndex === 'status'">
            <a-tag :color="record.status === 1 ? 'success' : 'default'">
              {{ record.status === 1 ? '成功' : '失败' }}
            </a-tag>
          </template>
        </template>
      </a-table>

      <!-- 空状态 -->
      <a-empty v-if="!paymentLoading && paymentData.length === 0" description="暂无支付记录" />
    </a-modal>
      </a-tab-pane>

      <!-- Tab 2: 客户欠款 -->
      <a-tab-pane key="debt" tab="客户欠款">
        <a-card>
          <template #title>
            <div style="display: flex; justify-content: space-between; align-items: center">
              <span>客户欠款查询</span>
              <a-button @click="fetchDebtList"><template #icon><ReloadOutlined /></template>刷新</a-button>
            </div>
          </template>

          <div style="margin-bottom: 16px">
            <a-space>
              <a-input v-model:value="debtKeyword" placeholder="搜索客户姓名/手机号" style="width: 200px" @pressEnter="fetchDebtList" />
              <a-button type="primary" @click="fetchDebtList">搜索</a-button>
            </a-space>
          </div>

          <a-table :columns="debtColumns" :dataSource="debtData" :loading="debtLoading" :pagination="debtPagination" rowKey="playerId" @change="handleDebtTableChange">
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'totalDebt'">
                <span style="color: #ff4d4f; font-weight: bold">¥{{ record.totalDebt?.toFixed(2) }}</span>
              </template>
              <template v-if="column.key === 'action'">
                <a @click="openDebtDetail(record)">查看明细</a>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-tab-pane>
    </a-tabs>

    <!-- 欠款明细弹窗 -->
    <a-modal v-model:open="debtDetailVisible" :title="`欠款明细 - ${currentDebtPlayer?.playerName || ''}`" width="700px" :footer="null">
      <a-table :columns="debtDetailColumns" :dataSource="debtDetailData" :loading="debtDetailLoading" rowKey="orderId" size="small">
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'debtAmount'">
            <span style="color: #ff4d4f; font-weight: bold">¥{{ record.debtAmount?.toFixed(2) }}</span>
          </template>
          <template v-if="column.key === 'action'">
            <a @click="openRepayDialog(record)">还款</a>
          </template>
        </template>
      </a-table>
    </a-modal>

    <!-- 还款弹窗 -->
    <a-modal v-model:open="repayVisible" title="客户还款" width="400px" @ok="handleRepay" :confirmLoading="repaying">
      <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="订单号">{{ repayForm.orderNo }}</a-form-item>
        <a-form-item label="欠款金额">¥{{ repayForm.debtAmount?.toFixed(2) }}</a-form-item>
        <a-form-item label="还款金额" required>
          <a-input-number v-model:value="repayForm.amount" :min="0.01" :max="repayForm.debtAmount" :precision="2" style="width: 100%" />
        </a-form-item>
        <a-form-item label="支付方式">
          <a-select v-model:value="repayForm.payMethod">
            <a-select-option value="CASH">现金</a-select-option>
            <a-select-option value="WECHAT">微信</a-select-option>
            <a-select-option value="ALIPAY">支付宝</a-select-option>
            <a-select-option value="TRANSFER">转账</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'antdv-next'
import { PayCircleOutlined, CalendarOutlined, FileTextOutlined, TeamOutlined, ReloadOutlined } from '@antdv-next/icons'
import { getShopList, getFinanceStats, getStringingOrderPage, getPaymentRecordList, type Shop } from '@/api'
import request from '@/utils/axios'

// ========== 类型定义 ==========

interface FinanceStats {
  todayRevenue: number
  monthRevenue: number
  onAccountTotal: number
  debtPlayerCount: number
}

interface StringingOrder {
  id: number
  orderNo: string
  shopName: string
  playerName: string
  totalPrice: number
  payStatus: 'UNPAID' | 'PAID' | 'ON_ACCOUNT'
  status: number
  createdAt: string
  completedAt?: string
}

interface PaymentRecord {
  id: number
  orderId: number
  payMethod: 'CASH' | 'WECHAT' | 'ALIPAY' | 'TRANSFER' | 'OTHER'
  amount: number
  status: number
  operatorName?: string
  remark?: string
  createdAt: string
}

// ========== 统计卡片 ==========

const stats = reactive<FinanceStats>({
  todayRevenue: 0,
  monthRevenue: 0,
  onAccountTotal: 0,
  debtPlayerCount: 0
})

const activeTab = ref('orders')

/** 获取统计数据 - 真实接口 */
const fetchStats = async () => {
  try {
    const res = await getFinanceStats()
    Object.assign(stats, res.data)
  } catch (err) {
    message.error('获取统计数据失败')
  }
}

// ========== 订单列表 ==========

const router = useRouter()
const loading = ref(false)
const tableData = ref<StringingOrder[]>([])
const shopList = ref<Shop[]>([])

const query = reactive({
  shopId: undefined as number | undefined,
  payStatus: '',
  keyword: '',
  dateRange: [] as any[]
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`
})

const columns = [
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo', width: 160 },
  { title: '店铺', dataIndex: 'shopName', key: 'shopName', width: 120 },
  { title: '客户', dataIndex: 'playerName', key: 'playerName', width: 100 },
  { title: '金额', dataIndex: 'totalPrice', key: 'totalPrice', width: 120 },
  { title: '支付状态', dataIndex: 'payStatus', key: 'payStatus', width: 100 },
  { title: '下单时间', dataIndex: 'createdAt', key: 'createdAt', width: 180 },
  { title: '操作', key: 'action', fixed: 'right' as const, width: 180 }
]

/** 获取订单列表 - 真实接口 */
const fetchOrders = async () => {
  loading.value = true
  try {
    const res = await getStringingOrderPage({
      shopId: query.shopId,
      payStatus: query.payStatus || undefined,
      keyword: query.keyword || undefined,
      startDate: query.dateRange?.[0]?.format('YYYY-MM-DD'),
      endDate: query.dateRange?.[1]?.format('YYYY-MM-DD'),
      current: pagination.current,
      size: pagination.pageSize
    })
    tableData.value = res.data.records || []
    pagination.total = res.data.total
  } catch (err) {
    message.error('获取订单列表失败')
  } finally {
    loading.value = false
  }
}

const search = () => {
  pagination.current = 1
  fetchOrders()
}

const resetSearch = () => {
  query.shopId = undefined
  query.payStatus = ''
  query.keyword = ''
  query.dateRange = []
  pagination.current = 1
  fetchOrders()
}

const handleTableChange = (pag: any) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchOrders()
}

// ========== 支付记录弹窗 ==========

const paymentVisible = ref(false)
const paymentLoading = ref(false)
const paymentData = ref<PaymentRecord[]>([])
const currentOrder = ref<StringingOrder | null>(null)

const paymentColumns = [
  { title: '支付方式', dataIndex: 'payMethod', key: 'payMethod', width: 120 },
  { title: '金额', dataIndex: 'amount', key: 'amount', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '操作人', dataIndex: 'operatorName', key: 'operatorName', width: 100 },
  { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true },
  { title: '支付时间', dataIndex: 'createdAt', key: 'createdAt', width: 180 }
]

const openPaymentDialog = async (record: StringingOrder) => {
  currentOrder.value = record
  paymentVisible.value = true
  await fetchPaymentRecords(record.id)
}

const fetchPaymentRecords = async (orderId: number) => {
  paymentLoading.value = true
  try {
    const res = await getPaymentRecordList({ orderId })
    paymentData.value = res.data || []
  } catch (err) {
    message.error('获取支付记录失败')
  } finally {
    paymentLoading.value = false
  }
}

const viewOrderDetail = (record: StringingOrder) => {
  router.push(`/order/${record.id}/detail`)
}

// ========== 工具函数 ==========

const payStatusColor = (status: string) => {
  const map: Record<string, string> = {
    PAID: 'success',
    UNPAID: 'warning',
    ON_ACCOUNT: 'blue'
  }
  return map[status] || 'default'
}

const payStatusText = (status: string) => {
  const map: Record<string, string> = {
    PAID: '已付',
    UNPAID: '未付',
    ON_ACCOUNT: '挂账'
  }
  return map[status] || status
}

const payMethodColor = (method: string) => {
  const map: Record<string, string> = {
    CASH: 'orange',
    WECHAT: 'green',
    ALIPAY: 'blue',
    TRANSFER: 'purple',
    OTHER: 'default'
  }
  return map[method] || 'default'
}

const payMethodText = (method: string) => {
  const map: Record<string, string> = {
    CASH: '现金',
    WECHAT: '微信',
    ALIPAY: '支付宝',
    TRANSFER: '转账',
    OTHER: '其他'
  }
  return map[method] || method
}

// ========== 初始化 ==========

// ========== 客户欠款 ==========

const debtLoading = ref(false)
const debtData = ref<any[]>([])
const debtKeyword = ref('')
const debtPagination = reactive({ current: 1, pageSize: 10, total: 0 })
const debtColumns = [
  { title: '客户姓名', dataIndex: 'playerName', width: 120 },
  { title: '手机号', dataIndex: 'playerPhone', width: 140 },
  { title: '总欠款', dataIndex: 'totalDebt', width: 120 },
  { title: '挂账笔数', dataIndex: 'debtOrderCount', width: 100 },
  { title: '操作', key: 'action', width: 100 }
]

const debtDetailVisible = ref(false)
const debtDetailLoading = ref(false)
const debtDetailData = ref<any[]>([])
const currentDebtPlayer = ref<any>(null)
const debtDetailColumns = [
  { title: '订单号', dataIndex: 'orderNo', width: 160 },
  { title: '金额', dataIndex: 'totalPrice', width: 100 },
  { title: '已付', dataIndex: 'paidAmount', width: 100 },
  { title: '欠款', dataIndex: 'debtAmount', width: 100 },
  { title: '下单时间', dataIndex: 'createdAt', width: 180 },
  { title: '操作', key: 'action', width: 80 }
]

const repayVisible = ref(false)
const repaying = ref(false)
const repayForm = reactive({ orderId: 0, orderNo: '', debtAmount: 0, amount: 0, payMethod: 'CASH' })

const fetchDebtList = async () => {
  debtLoading.value = true
  try {
    const res: any = await request.get('/api/finance/debt/summary', { params: { keyword: debtKeyword.value, current: debtPagination.current, size: debtPagination.pageSize } })
    const data = res?.data?.data || res?.data || {}
    debtData.value = data?.records || data?.list || []
    debtPagination.total = data?.total || 0
  } catch { debtData.value = [] }
  finally { debtLoading.value = false }
}

const handleDebtTableChange = (p: any) => { debtPagination.current = p.current; debtPagination.pageSize = p.pageSize; fetchDebtList() }

const openDebtDetail = async (record: any) => {
  currentDebtPlayer.value = record
  debtDetailVisible.value = true
  debtDetailLoading.value = true
  try {
    const res: any = await request.get(`/api/finance/debt?playerId=${record.playerId}`)
    debtDetailData.value = res?.data?.data?.debtOrders || res?.data?.debtOrders || []
  } catch { debtDetailData.value = [] }
  finally { debtDetailLoading.value = false }
}

const openRepayDialog = (record: any) => {
  repayForm.orderId = record.orderId
  repayForm.orderNo = record.orderNo
  repayForm.debtAmount = record.debtAmount
  repayForm.amount = record.debtAmount
  repayForm.payMethod = 'CASH'
  repayVisible.value = true
}

const handleRepay = async () => {
  repaying.value = true
  try {
    await request.post('/api/finance/repay', { orderId: repayForm.orderId, amount: repayForm.amount, payMethod: repayForm.payMethod })
    message.success('还款成功')
    repayVisible.value = false
    openDebtDetail(currentDebtPlayer.value)
    fetchStats()
  } catch { message.error('还款失败') }
  finally { repaying.value = false }
}

onMounted(async () => {
  fetchStats()
  try {
    const shopsRes = await getShopList()
    shopList.value = shopsRes.data || []
  } catch (err) {
    message.error('获取店铺列表失败')
  }
  fetchOrders()
})
</script>
