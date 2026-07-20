<template>
  <div>
    <a-card :bordered="false">
      <!-- 🆕 店铺选择器 -->
      <a-alert message="当前操作店铺" type="info" show-icon style="margin-bottom: 16px">
        <template #description>
          <a-space>
            <span style="font-weight: 500">店铺：</span>
            <a-select 
              v-model:value="currentShopId" 
              style="width: 200px"
              :loading="shopLoading"
              @change="handleShopChange"
              placeholder="请选择要配置的店铺"
            >
              <a-select-option v-for="s in shopList" :key="s.id" :value="s.id">
                {{ s.tenantName }} - {{ s.name }}
              </a-select-option>
            </a-select>
            <span v-if="!shopLoading && shopList.length === 0" style="color: red">
              无可用店铺
            </span>
          </a-space>
        </template>
      </a-alert>

      <a-tabs v-model:activeKey="activeTab" type="card">
        <!-- ==================== Tab 1: 打印机管理 ==================== -->
        <a-tab-pane key="printer" tab="打印机管理">
          <a-table
            :columns="printerColumns"
            :dataSource="printerList"
            :loading="printerLoading"
            :pagination="false"
            rowKey="id"
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'status'">
                <a-badge
                  :status="record.status === 1 ? 'success' : 'error'"
                  :text="record.status === 1 ? '在线' : '离线'"
                />
              </template>
              <template v-if="column.dataIndex === 'isDefault'">
                <a-tag v-if="record.isDefault" color="green">默认</a-tag>
                <span v-else>-</span>
              </template>
              <template v-if="column.dataIndex === 'lastHeartbeatAt'">
                {{ formatTime(record.lastHeartbeatAt) }}
              </template>
              <template v-if="column.key === 'action'">
                <a-space>
                  <a v-if="!record.isDefault" @click="handleSetDefaultPrinter(record)">设为默认</a>
                  <a-divider v-if="!record.isDefault" type="vertical" />
                  <a-popconfirm title="确定删除该打印机？" @confirm="handleDeletePrinter(record.id)">
                    <a type="danger">删除</a>
                  </a-popconfirm>
                </a-space>
              </template>
            </template>
          </a-table>
          <a-empty v-if="!printerLoading && printerList.length === 0" description="暂无已注册的打印机，请先启动本地打印客户端" />
        </a-tab-pane>

        <!-- ==================== Tab 2: 打印规则 ==================== -->
        <a-tab-pane key="rule" tab="打印规则">
          <div style="margin-bottom: 16px">
            <a-button type="primary" @click="openRuleDialog()">
              <template #icon><PlusOutlined /></template>
              新增规则
            </a-button>
          </div>

          <a-table
            :columns="ruleColumns"
            :dataSource="ruleList"
            :loading="ruleLoading"
            :pagination="false"
            rowKey="id"
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'orderType'">
                <a-tag color="blue">{{ getOrderTypeLabel(record.orderType) }}</a-tag>
              </template>
              <template v-if="column.dataIndex === 'enabled'">
                <a-switch
                  v-model:checked="record.enabled"
                  size="small"
                  @change="handleToggleRule(record)"
                />
              </template>
              <template v-if="column.key === 'action'">
                <a-space>
                  <a type="primary" @click="openRuleDialog(record)">编辑</a>
                  <a-divider type="vertical" />
                  <a-popconfirm title="确定删除该规则？" @confirm="handleDeleteRule(record.id)">
                    <a type="danger">删除</a>
                  </a-popconfirm>
                </a-space>
              </template>
            </template>
          </a-table>
          <a-empty v-if="!ruleLoading && ruleList.length === 0" description="暂无打印规则" />
        </a-tab-pane>

        <!-- ==================== Tab 3: 资源管理 ==================== -->
        <a-tab-pane key="resource" tab="资源管理">
          <div style="margin-bottom: 16px">
            <a-space>
              <a-upload
                :before-upload="handleBeforeUpload"
                :custom-request="handleUploadResource"
                :show-upload-list="false"
                accept="image/png,image/jpeg,image/gif,image/webp"
              >
                <a-button type="primary">
                  <template #icon><UploadOutlined /></template>
                  上传图片
                </a-button>
              </a-upload>
              <a-radio-group v-model:value="resourceFilterType" @change="loadResources">
                <a-radio-button value="">全部</a-radio-button>
                <a-radio-button value="QRCODE">收款码</a-radio-button>
                <a-radio-button value="LOGO">Logo</a-radio-button>
              </a-radio-group>
            </a-space>
          </div>

          <a-row :gutter="16">
            <a-col :xs="24" :sm="12" :md="8" :lg="6" v-for="item in filteredResources" :key="item.id">
              <a-card hoverable size="small" style="margin-bottom: 16px">
                <template #cover>
                  <div style="height: 160px; display: flex; align-items: center; justify-content: center; background: #f5f5f5">
                    <img
                      :src="item.url"
                      :alt="item.name"
                      style="max-width: 100%; max-height: 150px; object-fit: contain"
                      @error="handleImageError"
                    />
                  </div>
                </template>
                <a-card-meta>
                  <template #title>
                    <div style="display: flex; align-items: center; gap: 8px">
                      <a-tag :color="item.type === 'QRCODE' ? 'orange' : 'purple'" size="small">
                        {{ item.type === 'QRCODE' ? '收款码' : item.type === 'LOGO' ? 'Logo' : '其他' }}
                      </a-tag>
                      <span style="font-size: 12px">{{ item.name }}</span>
                    </div>
                  </template>
                  <template #description>
                    <a-tooltip :title="item.url">
                      <div style="font-size: 11px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; cursor: pointer" @click="copyUrl(item.url)">
                        点击复制URL
                      </div>
                    </a-tooltip>
                  </template>
                </a-card-meta>
                <template #actions>
                  <a-popconfirm title="确定删除该资源？" @confirm="handleDeleteResource(item.id)">
                    <a type="danger" style="font-size: 12px">删除</a>
                  </a-popconfirm>
                </template>
              </a-card>
            </a-col>
          </a-row>
          <a-empty v-if="!resourceLoading && filteredResources.length === 0" description="暂无资源，请上传图片" />
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <!-- ==================== 打印规则 新增/编辑 弹窗 ==================== -->
    <a-modal
      v-model:open="ruleDialogVisible"
      :title="ruleForm.id ? '编辑打印规则' : '新增打印规则'"
      width="500px"
      @ok="handleSaveRule"
      :confirmLoading="ruleSaving"
    >
      <a-form :model="ruleForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="单据类型" :rules="[{ required: true, message: '请选择单据类型' }]">
          <a-select v-model:value="ruleForm.orderType" placeholder="选择单据类型">
            <a-select-option value="ORDER">普通订单</a-select-option>
            <a-select-option value="REFUND">退款单</a-select-option>
            <a-select-option value="TOURNAMENT">赛事订单</a-select-option>
            <a-select-option value="TENNIS_TOURNAMENT">网球赛事订单</a-select-option>
            <a-select-option value="RECEIPT">收款单</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="打印模板" :rules="[{ required: true, message: '请选择打印模板' }]">
          <a-select v-model:value="ruleForm.templateId" placeholder="选择模板" :loading="templateLoading" show-search :filter-option="filterTemplateOption">
            <a-select-option v-for="t in templateList" :key="t.id" :value="t.id" :label="t.name">
              {{ t.name }}
              <a-tag v-if="t.isDefault" color="green" size="small">默认</a-tag>
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="打印机">
          <a-select v-model:value="ruleForm.printerName" placeholder="选择打印机（可选）" allow-clear>
            <a-select-option v-for="p in printerList" :key="p.id" :value="p.printerName">
              {{ p.printerName }}
              <a-tag v-if="p.isDefault" color="green" size="small">默认</a-tag>
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="启用">
          <a-switch v-model:checked="ruleForm.enabled" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'antdv-next'
import { PlusOutlined, UploadOutlined } from '@antdv-next/icons'
import {
  getPrinterList,
  setDefaultPrinter,
  deletePrinter,
  getPrintRulePage,
  addPrintRule,
  updatePrintRule,
  deletePrintRule,
  getPrintResourceList,
  uploadPrintResource,
  deletePrintResource,
  getPrintTemplateList,
  getShopList, // 🆕 获取店铺列表
  type PrinterRegister,
  type PrintRule,
  type PrintResource,
  type PrintTemplate
} from '@/api/index'

/* ==================== Tab 切换 ==================== */
const activeTab = ref('printer')

/* ==================== 🆕 店铺选择 ==================== */
const shopList = ref<any[]>([])
const shopLoading = ref(false)
const currentShopId = ref<number | undefined>(undefined)
const currentTenantId = ref<number | undefined>(undefined)

/* ==================== Tab 1: 打印机管理 ==================== */
const printerList = ref<PrinterRegister[]>([])
const printerLoading = ref(false)

const printerColumns = [
  { title: '客户端名称', dataIndex: 'clientName', key: 'clientName', width: 160 },
  { title: '打印机名称', dataIndex: 'printerName', key: 'printerName', width: 200 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '默认', dataIndex: 'isDefault', key: 'isDefault', width: 80 },
  { title: '最后心跳', dataIndex: 'lastHeartbeatAt', key: 'lastHeartbeatAt', width: 180 },
  { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true },
  { title: '操作', key: 'action', width: 160, fixed: 'right' as const }
]

const loadPrinters = async () => {
  if (!currentShopId.value || !currentTenantId.value) return
  printerLoading.value = true
  try {
    const res = await getPrinterList({ shopId: currentShopId.value, tenantId: currentTenantId.value })
    printerList.value = (res as any)?.data || []
  } catch (e: any) {
    message.error('加载打印机列表失败: ' + (e.message || e))
  } finally {
    printerLoading.value = false
  }
}

const handleSetDefaultPrinter = async (record: PrinterRegister) => {
  try {
    if (!record.id) return
    await setDefaultPrinter(record.id)
    message.success('已设为默认打印机')
    await loadPrinters()
  } catch (e: any) {
    message.error('设置失败: ' + (e.message || e))
  }
}

const handleDeletePrinter = async (id: number) => {
  try {
    await deletePrinter(id)
    message.success('删除成功')
    await loadPrinters()
  } catch (e: any) {
    message.error('删除失败: ' + (e.message || e))
  }
}

/* ==================== Tab 2: 打印规则 ==================== */
const ruleList = ref<PrintRule[]>([])
const ruleLoading = ref(false)
const ruleDialogVisible = ref(false)
const ruleSaving = ref(false)
const templateList = ref<PrintTemplate[]>([])
const templateLoading = ref(false)

const ruleColumns = [
  { title: '单据类型', dataIndex: 'orderType', key: 'orderType', width: 140 },
  { title: '模板', dataIndex: 'templateName', key: 'templateName', ellipsis: true },
  { title: '打印机', dataIndex: 'printerName', key: 'printerName', width: 160 },
  { title: '状态', dataIndex: 'enabled', key: 'enabled', width: 80 },
  { title: '操作', key: 'action', width: 160, fixed: 'right' as const }
]

const orderTypeMap: Record<string, string> = {
  ORDER: '普通订单',
  REFUND: '退款单',
  TOURNAMENT: '赛事订单',
  TENNIS_TOURNAMENT: '网球赛事订单',
  RECEIPT: '收款单'
}

const getOrderTypeLabel = (type: string) => orderTypeMap[type] || type

const defaultRuleForm = (): Omit<PrintRule, 'id'> & { id?: number } => ({
  orderType: 'ORDER',
  templateId: undefined,
  printerName: undefined,
  enabled: true
})

const ruleForm = reactive(defaultRuleForm())

const loadRules = async () => {
  if (!currentShopId.value || !currentTenantId.value) return
  ruleLoading.value = true
  try {
    const res = await getPrintRulePage({ pageNum: 1, pageSize: 100, shopId: currentShopId.value, tenantId: currentTenantId.value })
    const data = (res as any)?.data
    ruleList.value = Array.isArray(data) ? data : data?.records || []
  } catch (e: any) {
    message.error('加载打印规则失败: ' + (e.message || e))
  } finally {
    ruleLoading.value = false
  }
}

const loadTemplates = async () => {
  if (!currentShopId.value || !currentTenantId.value) return
  templateLoading.value = true
  try {
    const res = await getPrintTemplateList({ shopId: currentShopId.value, tenantId: currentTenantId.value })
    templateList.value = (res as any)?.data || []
  } catch (e: any) {
    console.error('加载模板列表失败', e)
  } finally {
    templateLoading.value = false
  }
}

const openRuleDialog = async (record?: PrintRule) => {
  if (record) {
    Object.assign(ruleForm, { ...record })
  } else {
    Object.assign(ruleForm, defaultRuleForm())
  }
  
  // 🆕 确保打开弹窗时，下拉框的数据源已加载
  if (templateList.value.length === 0) {
    await loadTemplates()
  }
  // 打印机列表同理
  if (printerList.value.length === 0) {
    await loadPrinters()
  }
  
  ruleDialogVisible.value = true
}

const handleSaveRule = async () => {
  if (!ruleForm.orderType) {
    message.warning('请选择单据类型')
    return
  }
  if (!ruleForm.templateId) {
    message.warning('请选择打印模板')
    return
  }
  ruleSaving.value = true
  try {
    if (ruleForm.id) {
      await updatePrintRule(ruleForm as PrintRule)
      message.success('更新成功')
    } else {
      await addPrintRule(ruleForm as PrintRule)
      message.success('新增成功')
    }
    ruleDialogVisible.value = false
    await loadRules()
  } catch (e: any) {
    message.error('保存失败: ' + (e.message || e))
  } finally {
    ruleSaving.value = false
  }
}

const handleToggleRule = async (record: PrintRule) => {
  try {
    await updatePrintRule(record as PrintRule)
    message.success('状态已更新')
  } catch (e: any) {
    message.error('更新失败: ' + (e.message || e))
    // 回滚
    record.enabled = !record.enabled
  }
}

const handleDeleteRule = async (id: number) => {
  try {
    await deletePrintRule(id)
    message.success('删除成功')
    await loadRules()
  } catch (e: any) {
    message.error('删除失败: ' + (e.message || e))
  }
}

const filterTemplateOption = (input: string, option: any) => {
  return (option?.label ?? '').toLowerCase().includes(input.toLowerCase())
}

/* ==================== Tab 3: 资源管理 ==================== */
const resourceList = ref<PrintResource[]>([])
const resourceLoading = ref(false)
const resourceFilterType = ref('')

const filteredResources = computed(() => {
  if (!resourceFilterType.value) return resourceList.value
  return resourceList.value.filter(r => r.type === resourceFilterType.value)
})

const loadResources = async () => {
  if (!currentShopId.value || !currentTenantId.value) return
  resourceLoading.value = true
  try {
    const params: Record<string, any> = { shopId: currentShopId.value, tenantId: currentTenantId.value }
    if (resourceFilterType.value) {
      params.type = resourceFilterType.value
    }
    const res = await getPrintResourceList(params)
    resourceList.value = (res as any)?.data || []
  } catch (e: any) {
    message.error('加载资源列表失败: ' + (e.message || e))
  } finally {
    resourceLoading.value = false
  }
}

const handleBeforeUpload = (file: File) => {
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    message.error('只能上传图片文件!')
    return false
  }
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isLt5M) {
    message.error('图片大小不能超过 5MB!')
    return false
  }
  return true
}

const handleUploadResource = async ({ file }: { file: File }) => {
  const formData = new FormData()
  formData.append('file', file)
  // 根据文件名推测类型
  const fileName = file.name.toLowerCase()
  if (fileName.includes('qr') || fileName.includes('code') || fileName.includes('收款')) {
    formData.append('type', 'QRCODE')
  } else if (fileName.includes('logo') || fileName.includes('icon')) {
    formData.append('type', 'LOGO')
  } else {
    formData.append('type', 'OTHER')
  }
  formData.append('name', file.name)
  try {
    await uploadPrintResource(formData)
    message.success('上传成功')
    await loadResources()
  } catch (e: any) {
    message.error('上传失败: ' + (e.message || e))
  }
}

const handleDeleteResource = async (id: number) => {
  try {
    await deletePrintResource(id)
    message.success('删除成功')
    await loadResources()
  } catch (e: any) {
    message.error('删除失败: ' + (e.message || e))
  }
}

const copyUrl = async (url: string) => {
  try {
    await navigator.clipboard.writeText(url)
    message.success('URL 已复制到剪贴板')
  } catch {
    message.error('复制失败，请手动复制')
  }
}

const handleImageError = (e: Event) => {
  const img = e.target as HTMLImageElement
  img.src = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCIgZmlsbD0iI2Y1ZjVmNSIvPjx0ZXh0IHg9IjUwJSIgeT0iNTAlIiBmb250LWZhbWlseT0ibW9ub3NwYWNlIiBmb250LXNpemU9IjE0IiBmaWxsPSIjOTk5IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBkeT0iLjNlbSI+5p2l6L+HSW1hZ2U8L3RleHQ+PC9zdmc+'
}

/* ==================== 工具函数 ==================== */
const formatTime = (time?: string) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN', { hour12: false })
}

/* ==================== 🆕 店铺切换 ==================== */
const loadShops = async () => {
  shopLoading.value = true
  try {
    const res = await getShopList()
    shopList.value = (res as any)?.data || []
    
    // 🎯 默认选中第一个
    if (shopList.value.length > 0 && !currentShopId.value) {
      currentShopId.value = shopList.value[0].id
      handleShopChange() // 选中后加载数据
    }
  } finally {
    shopLoading.value = false
  }
}

const handleShopChange = () => {
  if (!currentShopId.value) return
  // 同步租户 ID
  const shop = shopList.value.find(s => s.id === currentShopId.value)
  if (shop) {
    currentTenantId.value = shop.tenantId
  }
  // 重载所有数据
  loadPrinters()
  loadRules()
  loadTemplates()
  loadResources()
}

/* ==================== 初始化 ==================== */
onMounted(() => {
  loadShops() // 加载店铺
})
</script>

<style scoped>
/* 资源卡片 hover 时显示操作按钮 */
:deep(.ant-card-actions) {
  border-top: 1px solid #f0f0f0;
}
</style>
