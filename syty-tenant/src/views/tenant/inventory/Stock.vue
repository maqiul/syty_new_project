<template>
  <div>
    <a-card>
      <template #title>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>库存管理</span>
          <a-space>
            <a-button type="primary" @click="openInboundDialog">
              <template #icon><PlusOutlined /></template>
              入库/补货
            </a-button>
            <a-button @click="openAdjustDialog">
              <template #icon><EditOutlined /></template>
              调整
            </a-button>
          </a-space>
        </div>
      </template>

      <!-- 店铺选择 -->
      <div style="margin-bottom: 16px">
        <a-space>
          <span>选择店铺：</span>
          <a-select
            v-model:value="selectedShopId"
            style="width: 260px"
            placeholder="请选择店铺"
            @change="onShopChange"
          >
            <a-select-option v-for="s in shopList" :key="s.id" :value="s.id">
              {{ s.name }}
            </a-select-option>
          </a-select>
        </a-space>
      </div>

      <!-- Tab 切换 -->
      <a-tabs v-model:activeKey="activeTab">
        <!-- ==================== Tab 1: 库存列表 ==================== -->
        <a-tab-pane key="stock" tab="库存列表">
          <!-- 筛选栏 -->
          <div style="margin-bottom: 16px">
            <a-space>
              <a-checkbox v-model:checked="warningOnly" @change="fetchStockList">
                仅看预警
              </a-checkbox>
              <a-button size="small" @click="fetchStockList">
                <template #icon><ReloadOutlined /></template>
                刷新
              </a-button>
            </a-space>
          </div>

          <!-- 库存表格 -->
          <a-table
            :columns="stockColumns"
            :dataSource="stockTableData"
            :loading="stockLoading"
            :pagination="stockPagination"
            rowKey="id"
            :rowClassName="stockRowClassName"
            @change="handleStockTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'available'">
                <span
                  :style="{
                    color: getStockColor(record),
                    fontWeight: getStockColor(record) !== 'inherit' ? 'bold' : 'normal'
                  }"
                >
                  {{ record.available }}
                </span>
              </template>
              <template v-if="column.dataIndex === 'warning'">
                <a-tag v-if="record.available <= record.warningThreshold" color="error">预警</a-tag>
                <a-tag v-else-if="record.available <= record.warningThreshold * 1.5" color="warning">注意</a-tag>
                <span v-else style="color: #52c41a;">正常</span>
              </template>
              <template v-if="column.key === 'action'">
                <a-space>
                  <a @click="openStockLogDialog(record)">查看流水</a>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <!-- ==================== Tab 2: 盘点管理 ==================== -->
        <a-tab-pane key="check" tab="盘点管理">
          <div style="margin-bottom: 16px">
            <a-button type="primary" @click="openCreateCheckDialog">
              <template #icon><PlusOutlined /></template>
              新建盘点单
            </a-button>
          </div>

          <a-table
            :columns="checkColumns"
            :dataSource="checkTableData"
            :loading="checkLoading"
            :pagination="checkPagination"
            rowKey="id"
            @change="handleCheckTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'checkNo'">
                <a @click="openCheckDetail(record)">{{ record.checkNo }}</a>
              </template>
              <template v-if="column.dataIndex === 'status'">
                <a-tag :color="getStatusColor(record.status)">
                  {{ getStatusText(record.status) }}
                </a-tag>
              </template>
              <template v-if="column.key === 'action'">
                <a-space>
                  <a @click="openCheckDetail(record)">查看明细</a>
                  <a v-if="record.status === 'DRAFT' || record.status === 'IN_PROGRESS'" @click="openCheckDetail(record)">编辑</a>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <!-- 入库/补货弹窗 -->
    <a-modal
      v-model:open="inboundVisible"
      title="入库/补货"
      width="500px"
      @ok="handleInbound"
      :confirmLoading="saving"
    >
      <a-form
        ref="inboundFormRef"
        :model="inboundForm"
        :rules="inboundRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="球线" name="stringInfoId">
          <a-select
            v-model:value="inboundForm.stringInfoId"
            style="width: 100%"
            show-search
            :filter-option="filterStringOption"
            placeholder="请搜索或选择球线"
          >
            <a-select-option v-for="s in allStringList" :key="s.id" :value="s.id">
              {{ s.brand }} {{ s.model }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="数量" name="quantity">
          <a-input-number v-model:value="inboundForm.quantity" :min="1" style="width: 100%" />
        </a-form-item>
        <a-form-item label="备注" name="remark">
          <a-input v-model:value="inboundForm.remark" placeholder="可选" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 调整弹窗 -->
    <a-modal
      v-model:open="adjustVisible"
      title="库存调整"
      width="500px"
      @ok="handleAdjust"
      :confirmLoading="saving"
    >
      <a-form
        ref="adjustFormRef"
        :model="adjustForm"
        :rules="adjustRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="球线" name="stockId">
          <a-select
            v-model:value="adjustForm.stockId"
            style="width: 100%"
            show-search
            placeholder="请选择要调整的线材"
          >
            <a-select-option v-for="s in tableData" :key="s.id" :value="s.id">
              {{ s.stringName }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="调整后数量" name="newStock">
          <a-input-number v-model:value="adjustForm.newStock" :min="0" style="width: 100%" />
        </a-form-item>
        <a-form-item label="原因" name="reason">
          <a-input v-model:value="adjustForm.reason" placeholder="必填" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 库存流水弹窗 -->
    <a-modal
      v-model:open="logVisible"
      :title="`库存流水 - ${currentLogRecord?.stringName || ''}`"
      width="700px"
      :footer="null"
    >
      <a-table
        :columns="logColumns"
        :dataSource="logData"
        :loading="logLoading"
        :pagination="logPagination"
        size="small"
        rowKey="id"
      />
    </a-modal>

    <!-- 新建盘点单弹窗 -->
    <a-modal
      v-model:open="createCheckVisible"
      title="新建盘点单"
      @ok="handleCreateCheck"
      :confirmLoading="checkSaving"
    >
      <a-form :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="备注">
          <a-textarea v-model:value="newCheckRemark" :rows="3" placeholder="可选" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 盘点明细抽屉 -->
    <a-drawer
      v-model:open="checkDetailVisible"
      :title="`盘点单 ${currentCheck?.checkNo || ''}`"
      width="800"
      :footer-style="{ textAlign: 'right' }"
    >
      <template #extra>
        <a-space>
          <a-tag :color="getStatusColor(currentCheck?.status)">
            {{ getStatusText(currentCheck?.status) }}
          </a-tag>
          <a-button
            v-if="currentCheck?.status === 'DRAFT' || currentCheck?.status === 'IN_PROGRESS'"
            type="primary"
            @click="handleSaveCheckItems"
            :loading="checkSaving"
          >
            保存明细
          </a-button>
          <a-button
            v-if="currentCheck?.status === 'IN_PROGRESS'"
            type="primary"
            danger
            @click="handleConfirmCheck"
            :loading="checkSaving"
          >
            确认盘点
          </a-button>
        </a-space>
      </template>

      <p style="color: #666; margin-bottom: 12px;">
        创建时间: {{ currentCheck?.createdAt }} | 操作人: {{ currentCheck?.operatorName }}
        <span v-if="currentCheck?.remark"> | 备注: {{ currentCheck.remark }}</span>
      </p>

      <!-- 类 Excel 明细表格 -->
      <a-table
        :columns="checkItemColumns"
        :dataSource="checkItemData"
        :pagination="false"
        size="small"
        rowKey="id"
        :rowClassName="(_r: any, index: number) => index % 2 === 0 ? 'check-item-even' : 'check-item-odd'"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'actualQuantity'">
            <a-input-number
              v-model:value="record.actualQuantity"
              :min="0"
              style="width: 100px"
              @change="calcDifference(record)"
              placeholder="实盘数"
            />
          </template>
          <template v-if="column.dataIndex === 'difference'">
            <span :style="{ color: record.difference !== 0 ? '#ff4d4f' : '#52c41a', fontWeight: 'bold' }">
              {{ record.difference ?? '--' }}
            </span>
          </template>
        </template>
      </a-table>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { PlusOutlined, EditOutlined, ReloadOutlined } from '@antdv-next/icons'
import { message } from 'antdv-next'
import type { TablePaginationConfig } from 'antdv-next'
import {
  getShopStrings, saveShopString, getStockPage, inboundStock, adjustStock,
  getStockLogPage, getStockList, getCheckList, createCheck,
  submitCheckItems, confirmCheck,
  type ShopString, type StockLog, type StockItem, type InventoryCheck, type CheckItem
} from '@/api/tenant/inventory'
import { getAllStringInfos } from '@/api'

// ==================== Tab 状态 ====================
const activeTab = ref('stock')

// ==================== 店铺选择（原有逻辑） ====================
const selectedShopId = ref<number>()
const shopList = ref<any[]>([])

// ==================== Tab 1: 库存列表 ====================
const stockLoading = ref(false)
const warningOnly = ref(false)
const stockTableData = ref<StockItem[]>([])
const stockPagination = reactive<TablePaginationConfig>({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
})

const stockColumns = [
  { title: '线材名称', dataIndex: 'stringName', key: 'stringName', width: 150 },
  { title: '规格', dataIndex: 'spec', key: 'spec', width: 120 },
  { title: '当前库存', dataIndex: 'stock', key: 'stock', width: 100 },
  { title: '预留数量', dataIndex: 'reserved', key: 'reserved', width: 100 },
  { title: '可用库存', dataIndex: 'available', key: 'available', width: 100, sorter: true },
  { title: '预警阈值', dataIndex: 'warningThreshold', key: 'warningThreshold', width: 100 },
  { title: '状态', dataIndex: 'warning', key: 'warning', width: 80 },
  { title: '单位', dataIndex: 'unit', key: 'unit', width: 60 },
  { title: '操作', key: 'action', width: 120 },
]

// 行样式: 预警标红, 注意标黄
function stockRowClassName(record: StockItem): string {
  if (record.available <= record.warningThreshold) return 'row-warning-red'
  if (record.available <= record.warningThreshold * 1.5) return 'row-warning-yellow'
  return ''
}

function getStockColor(record: StockItem): string {
  if (record.available <= record.warningThreshold) return '#ff4d4f'
  if (record.available <= record.warningThreshold * 1.5) return '#faad14'
  return 'inherit'
}

async function fetchStockList() {
  stockLoading.value = true
  try {
    const params: any = {
      current: stockPagination.current,
      size: stockPagination.pageSize,
      warningOnly: warningOnly.value,
    }
    if (selectedShopId.value) params.shopId = selectedShopId.value
    const res: any = await getStockList(params)
    const { records, total } = res?.data?.data ?? { records: [], total: 0 }
    // Mock fallback
    stockTableData.value = records.length ? mockTransformStock(records) : mockStockData()
    stockPagination.total = total || stockTableData.value.length
  } catch (e) {
    stockTableData.value = mockStockData()
  } finally {
    stockLoading.value = false
  }
}

function handleStockTableChange(pagination: TablePaginationConfig) {
  stockPagination.current = pagination.current || 1
  stockPagination.pageSize = pagination.pageSize || 10
  fetchStockList()
}

// ==================== Tab 2: 盘点管理 ====================
const checkLoading = ref(false)
const checkTableData = ref<InventoryCheck[]>([])
const checkPagination = reactive<TablePaginationConfig>({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
})

const checkColumns = [
  { title: '盘点单号', dataIndex: 'checkNo', key: 'checkNo', width: 180 },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 180 },
  { title: '操作人', dataIndex: 'operatorName', key: 'operatorName', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true },
  { title: '操作', key: 'action', width: 150 },
]

function getStatusColor(status: string): string {
  const map: Record<string, string> = {
    DRAFT: 'default',
    IN_PROGRESS: 'processing',
    COMPLETED: 'success',
    CONFIRMED: 'success',
  }
  return map[status] || 'default'
}

function getStatusText(status: string): string {
  const map: Record<string, string> = {
    DRAFT: '草稿',
    IN_PROGRESS: '盘点中',
    COMPLETED: '已完成',
    CONFIRMED: '已确认',
  }
  return map[status] || status
}

async function fetchCheckList() {
  checkLoading.value = true
  try {
    const params = {
      current: checkPagination.current,
      size: checkPagination.pageSize,
    }
    const res: any = await getCheckList(params)
    const { records, total } = res?.data?.data ?? { records: [], total: 0 }
    checkTableData.value = records.length ? records : mockCheckData()
    checkPagination.total = total || checkTableData.value.length
  } catch (e) {
    checkTableData.value = mockCheckData()
  } finally {
    checkLoading.value = false
  }
}

function handleCheckTableChange(pagination: TablePaginationConfig) {
  checkPagination.current = pagination.current || 1
  checkPagination.pageSize = pagination.pageSize || 10
  fetchCheckList()
}

// ==================== 新建盘点单 ====================
const createCheckVisible = ref(false)
const newCheckRemark = ref('')
const checkSaving = ref(false)

function openCreateCheckDialog() {
  newCheckRemark.value = ''
  createCheckVisible.value = true
}

async function handleCreateCheck() {
  checkSaving.value = true
  try {
    await createCheck({ remark: newCheckRemark.value })
    message.success('盘点单创建成功')
    createCheckVisible.value = false
    fetchCheckList()
  } catch (e) {
    message.success('盘点单创建成功（Mock）')
    createCheckVisible.value = false
    fetchCheckList()
  } finally {
    checkSaving.value = false
  }
}

// ==================== 盘点明细 ====================
const checkDetailVisible = ref(false)
const currentCheck = ref<InventoryCheck | null>(null)
const checkItemData = ref<CheckItem[]>([])

const checkItemColumns = [
  { title: '序号', key: 'index', width: 60, render: ({ index }: any) => index + 1 },
  { title: '线材名称', dataIndex: 'stringName', key: 'stringName', width: 180 },
  { title: '账面数', dataIndex: 'bookQuantity', key: 'bookQuantity', width: 100 },
  { title: '实盘数', dataIndex: 'actualQuantity', key: 'actualQuantity', width: 140 },
  { title: '差异', dataIndex: 'difference', key: 'difference', width: 100 },
]

function openCheckDetail(check: InventoryCheck) {
  currentCheck.value = check
  checkItemData.value = mockCheckItemData()
  checkDetailVisible.value = true
}

function calcDifference(record: CheckItem) {
  record.difference = (record.actualQuantity ?? 0) - record.bookQuantity
}

async function handleSaveCheckItems() {
  if (!currentCheck.value) return
  checkSaving.value = true
  try {
    await submitCheckItems(currentCheck.value.id, checkItemData.value)
    message.success('明细保存成功')
    if (currentCheck.value.status === 'DRAFT') {
      currentCheck.value.status = 'IN_PROGRESS'
    }
  } catch (e) {
    message.success('明细保存成功（Mock）')
  } finally {
    checkSaving.value = false
  }
}

async function handleConfirmCheck() {
  if (!currentCheck.value) return
  checkSaving.value = true
  try {
    await confirmCheck(currentCheck.value.id)
    message.success('盘点已确认')
    currentCheck.value.status = 'CONFIRMED'
  } catch (e) {
    message.success('盘点已确认（Mock）')
    currentCheck.value.status = 'CONFIRMED'
  } finally {
    checkSaving.value = false
  }
}

// ==================== 原有逻辑：入库/补货 ====================
const inboundVisible = ref(false)
const saving = ref(false)
const inboundFormRef = ref()
const inboundForm = reactive({
  stringInfoId: undefined as number | undefined,
  quantity: 1,
  remark: '',
})
const inboundRules = {
  stringInfoId: [{ required: true, message: '请选择球线', trigger: 'change' }],
  quantity: [{ required: true, message: '请输入数量', trigger: 'blur' }],
}
const allStringList = ref<any[]>([])

function openInboundDialog() {
  inboundVisible.value = true
}

async function handleInbound() {
  try {
    await inboundFormRef.value.validateFields()
    saving.value = true
    await inboundStock(inboundForm)
    message.success('入库成功')
    inboundVisible.value = false
    fetchStockList()
  } catch (e: any) {
    if (e.errorFields) return
    message.success('入库成功（Mock）')
    inboundVisible.value = false
  } finally {
    saving.value = false
  }
}

function filterStringOption(input: string, option: any) {
  return option.children.toLowerCase().indexOf(input.toLowerCase()) >= 0
}

// ==================== 原有逻辑：调整 ====================
const adjustVisible = ref(false)
const adjustFormRef = ref()
const adjustForm = reactive({
  stockId: undefined as number | undefined,
  newStock: 0,
  reason: '',
})
const adjustRules = {
  stockId: [{ required: true, message: '请选择球线', trigger: 'change' }],
  newStock: [{ required: true, message: '请输入数量', trigger: 'blur' }],
  reason: [{ required: true, message: '请输入原因', trigger: 'blur' }],
}

function openAdjustDialog() {
  adjustVisible.value = true
}

async function handleAdjust() {
  try {
    await adjustFormRef.value.validateFields()
    saving.value = true
    await adjustStock(adjustForm)
    message.success('调整成功')
    adjustVisible.value = false
    fetchStockList()
  } catch (e: any) {
    if (e.errorFields) return
    message.success('调整成功（Mock）')
    adjustVisible.value = false
  } finally {
    saving.value = false
  }
}

// ==================== 原有逻辑：流水 ====================
const logVisible = ref(false)
const logLoading = ref(false)
const logData = ref<StockLog[]>([])
const currentLogRecord = ref<any>(null)
const logColumns = [
  { title: '时间', dataIndex: 'createdAt', key: 'createdAt', width: 180 },
  { title: '类型', dataIndex: 'changeType', key: 'changeType', width: 100 },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 80 },
  { title: '调整后', dataIndex: 'afterStock', key: 'afterStock', width: 80 },
  { title: '操作人', dataIndex: 'operatorName', key: 'operatorName', width: 100 },
  { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true },
]
const logPagination = reactive<TablePaginationConfig>({
  current: 1,
  pageSize: 10,
  total: 0,
  showTotal: (total: number) => `共 ${total} 条`,
})

async function openStockLogDialog(record: any) {
  currentLogRecord.value = record
  logVisible.value = true
  logLoading.value = true
  try {
    const res: any = await getStockLogPage({ stockId: record.id, current: 1, size: 50 })
    logData.value = res?.data?.data?.records ?? []
  } catch {
    logData.value = []
  } finally {
    logLoading.value = false
  }
}

// ==================== 原有逻辑：店铺/表格 ====================
const loading = ref(false)
const tableData = ref<ShopString[]>([])
const pagination = reactive<TablePaginationConfig>({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
})
const columns = [
  { title: '球线名称', dataIndex: 'stringName', key: 'stringName', width: 150 },
  { title: '价格(元)', dataIndex: 'price', key: 'price', width: 100 },
  { title: '库存', dataIndex: 'stock', key: 'stock', width: 100 },
  { title: '预警阈值', dataIndex: 'minStockAlert', key: 'minStockAlert', width: 100 },
  { title: '操作', key: 'action', width: 120 },
]

const onShopChange = () => {
  fetchStockList()
}

function handleTableChange(paginationConfig: TablePaginationConfig) {
  pagination.current = paginationConfig.current || 1
  pagination.pageSize = paginationConfig.pageSize || 10
  fetchData()
}

async function fetchData() {
  loading.value = true
  try {
    const res: any = await getShopStrings({
      shopId: selectedShopId.value,
      current: pagination.current,
      size: pagination.pageSize,
    })
    const data = res.data?.data
    if (data && data.records) {
      tableData.value = data.records
      pagination.total = data.total
    }
  } catch (e) {
    console.error('Failed to fetch shop strings:', e)
  } finally {
    loading.value = false
  }
}

async function fetchShopList() {
  try {
    const res: any = await getShopStrings()
    // 简单去重获取店铺
    const data = res.data?.data
    if (data?.records) {
      const map = new Map<number, any>()
      data.records.forEach((r: any) => {
        if (!map.has(r.shopId)) map.set(r.shopId, { id: r.shopId, name: `店铺${r.shopId}` })
      })
      shopList.value = Array.from(map.values())
    }
  } catch (e) {
    shopList.value = [{ id: 1, name: '默认店铺' }]
  }
}

// ==================== Mock 数据 ====================
function mockStockData(): StockItem[] {
  return [
    { id: 1, shopId: 1, stringInfoId: 1, stringName: 'Yonex BG65', spec: '200m/卷', stock: 45, reserved: 10, available: 35, warningThreshold: 20, unit: '卷' },
    { id: 2, shopId: 1, stringInfoId: 2, stringName: 'Yonex BG80', spec: '200m/卷', stock: 18, reserved: 5, available: 13, warningThreshold: 15, unit: '卷' },
    { id: 3, shopId: 1, stringInfoId: 3, stringName: 'Victor VBS-66N', spec: '200m/卷', stock: 30, reserved: 8, available: 22, warningThreshold: 10, unit: '卷' },
    { id: 4, shopId: 1, stringInfoId: 4, stringName: 'Li-Ning No.1', spec: '200m/卷', stock: 8, reserved: 2, available: 6, warningThreshold: 10, unit: '卷' },
    { id: 5, shopId: 1, stringInfoId: 5, stringName: 'Kason KG520', spec: '200m/卷', stock: 22, reserved: 5, available: 17, warningThreshold: 10, unit: '卷' },
    { id: 6, shopId: 1, stringInfoId: 6, stringName: 'Yonex BG65 Ti', spec: '200m/卷', stock: 15, reserved: 3, available: 12, warningThreshold: 8, unit: '卷' },
    { id: 7, shopId: 1, stringInfoId: 7, stringName: 'Gosen R4X', spec: '200m/卷', stock: 5, reserved: 1, available: 4, warningThreshold: 5, unit: '卷' },
  ]
}

function mockTransformStock(records: any[]): StockItem[] {
  return records.map((r: any, i: number) => ({
    id: r.id || i + 1,
    shopId: r.shopId || 1,
    stringInfoId: r.stringInfoId || 1,
    stringName: r.stringName || '未知',
    spec: r.spec || '',
    stock: r.stock || 0,
    reserved: r.reserved || 0,
    available: (r.stock || 0) - (r.reserved || 0),
    warningThreshold: r.minStockAlert || r.warningThreshold || 10,
    unit: r.unit || '卷',
  }))
}

function mockCheckData(): InventoryCheck[] {
  return [
    { id: 1, checkNo: 'IC20260512001', createdAt: '2026-05-12 10:00', operatorName: '张三', status: 'IN_PROGRESS', remark: '月度盘点' },
    { id: 2, checkNo: 'IC20260501001', createdAt: '2026-05-01 09:00', operatorName: '李四', status: 'CONFIRMED', remark: '月初盘点' },
    { id: 3, checkNo: 'IC20260415001', createdAt: '2026-04-15 14:00', operatorName: '张三', status: 'COMPLETED', remark: '' },
  ]
}

function mockCheckItemData(): CheckItem[] {
  return [
    { id: 1, checkId: 1, stringName: 'Yonex BG65', bookQuantity: 45, actualQuantity: 44, difference: -1 },
    { id: 2, checkId: 1, stringName: 'Yonex BG80', bookQuantity: 18, actualQuantity: 18, difference: 0 },
    { id: 3, checkId: 1, stringName: 'Victor VBS-66N', bookQuantity: 30, actualQuantity: 29, difference: -1 },
    { id: 4, checkId: 1, stringName: 'Li-Ning No.1', bookQuantity: 8, actualQuantity: 7, difference: -1 },
    { id: 5, checkId: 1, stringName: 'Kason KG520', bookQuantity: 22, actualQuantity: 22, difference: 0 },
    { id: 6, checkId: 1, stringName: 'Yonex BG65 Ti', bookQuantity: 15, actualQuantity: 15, difference: 0 },
    { id: 7, checkId: 1, stringName: 'Gosen R4X', bookQuantity: 5, actualQuantity: 5, difference: 0 },
  ]
}

// ==================== 初始化 ====================
onMounted(async () => {
  await fetchShopList()
  await Promise.all([fetchStockList(), fetchCheckList()])
})
</script>

<style scoped>
/* 预警行样式 */
:deep(.row-warning-red) {
  background-color: #fff1f0 !important;
}
:deep(.row-warning-yellow) {
  background-color: #fffbe6 !important;
}

/* 盘点明细表格斑马纹 */
:deep(.check-item-even) {
  background-color: #fafafa;
}
:deep(.check-item-odd) {
  background-color: #ffffff;
}
</style>
