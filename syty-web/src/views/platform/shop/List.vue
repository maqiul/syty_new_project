<template>
  <div class="platform-shop-list">
    <a-card :bordered="false">
      <template #title>
        <div class="card-header">
          <span class="card-title">门店管理</span>
          <a-button type="primary" @click="handleAdd">
            <template #icon><PlusOutlined /></template>
            开通门店
          </a-button>
        </div>
      </template>

      <!-- 搜索栏 -->
      <div class="search-bar">
        <a-input
          v-model:value="queryParams.keyword"
          placeholder="搜索门店名称/编码"
          allow-clear
          style="width: 250px"
          @pressEnter="handleSearch"
        />
        <a-button type="primary" @click="handleSearch">搜索</a-button>
      </div>

      <a-table
        :columns="columns"
        :dataSource="tableData"
        :loading="loading"
        :pagination="pagination"
        rowKey="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'status'">
            <a-tag :color="getStatusColor(record.status)">
              {{ getStatusText(record.status) }}
            </a-tag>
          </template>
          <template v-if="column.dataIndex === 'expiredAt'">
            {{ record.expiredAt ? dayjs(record.expiredAt).format('YYYY-MM-DD') : '-' }}
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleRenew(record)">续费</a-button>
              <a-divider type="vertical" />
              <a-popconfirm title="确定停用该门店吗？" @confirm="handleSuspend(record.id)">
                <a-button type="link" danger size="small">停用</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 开通门店弹窗 -->
    <a-modal v-model:open="addVisible" title="开通新门店" @ok="handleSave" :confirmLoading="saving">
      <a-form :model="addForm" layout="vertical">
        <a-form-item label="所属租户" required>
          <a-select
            v-model:value="addForm.tenantCode"
            placeholder="请选择租户"
            allow-clear
          >
            <a-select-option v-for="item in tenantOptions" :key="item.id" :value="item.tenantCode">
              {{ item.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="门店名称" required>
          <a-input v-model:value="addForm.shopName" placeholder="例如：朝阳旗舰店" />
        </a-form-item>
        <a-form-item label="选择套餐" required>
          <a-select
            v-model:value="addForm.packageId"
            placeholder="请选择套餐（留空自动生成门店编码）"
            allow-clear
          >
            <a-select-option v-for="item in packageOptions" :key="item.id" :value="item.id">
              {{ item.name }}（{{ item.durationDays }}天）
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 续费弹窗 -->
    <a-modal v-model:open="renewVisible" title="门店续费 / 调整套餐" @ok="handleRenewSave" :confirmLoading="renewSaving">
      <a-form layout="vertical">
        <a-alert :message="'当前门店到期时间：' + (renewForm.currentExpiredAt ? dayjs(renewForm.currentExpiredAt).format('YYYY-MM-DD') : '未设置')" type="info" show-icon style="margin-bottom: 16px" />
        
        <a-form-item label="选择套餐（续费时可升级/降级）">
          <a-select v-model:value="renewForm.packageId" placeholder="请选择套餐" @change="handlePackageChange">
            <a-select-option v-for="item in packageOptions" :key="item.id" :value="item.id">
              {{ item.name }}（{{ item.durationDays }}天）
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="续费时长 (天)" required>
          <a-input-number v-model:value="renewForm.durationDays" :min="1" style="width: 100%" />
          <div style="font-size: 12px; color: #999; margin-top: 4px;">
            * 切换套餐将自动更新默认续费天数
          </div>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import request from '@/utils/axios'

// 定义列
const columns = [
  { title: '门店名称', dataIndex: 'shopName', key: 'shopName' },
  { title: '门店编码', dataIndex: 'shopCode', key: 'shopCode', width: 180 },
  { title: '所属租户', dataIndex: 'tenantCode', key: 'tenantCode', width: 150 },
  { title: '到期时间', dataIndex: 'expiredAt', key: 'expiredAt', width: 150 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 120 },
  { title: '操作', key: 'action', width: 180, fixed: 'right' as const },
]

const tableData = ref([])
const loading = ref(false)
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })
const queryParams = reactive({ keyword: '' })

// 下拉选项数据
const tenantOptions = ref<any[]>([])
const packageOptions = ref<any[]>([])

// 开通门店状态
const addVisible = ref(false)
const saving = ref(false)
const addForm = reactive({ tenantCode: '', shopName: '', packageId: undefined as string | undefined })

// 续费状态
const renewVisible = ref(false)
const renewSaving = ref(false)
const renewForm = reactive({ id: 0, packageId: undefined as string | undefined, durationDays: 30, currentExpiredAt: '' })

const fetchList = async () => {
  loading.value = true
  try {
    const res = await request.get('/platform/shops/list', {
      params: { page: pagination.current, size: pagination.pageSize, ...queryParams }
    })
    tableData.value = res.data.records
    pagination.total = res.data.total
  } catch (e: any) {
    message.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

// 获取租户列表（第一页全量）
const fetchTenantOptions = async () => {
  try {
    const res = await request.get('/platform/tenants/list', {
      params: { page: 1, size: 100 }
    })
    tenantOptions.value = res.data.records || []
  } catch (e: any) {
    message.error('租户列表加载失败：' + (e.message || ''))
  }
}

// 获取套餐列表
const fetchPackageOptions = async () => {
  try {
    const res = await request.get('/platform/packages/list')
    packageOptions.value = res.data || []
  } catch (e: any) {
    message.error('套餐列表加载失败：' + (e.message || ''))
  }
}

onMounted(() => {
  fetchList()
  fetchTenantOptions()
  fetchPackageOptions()
})

const handleSearch = () => { pagination.current = 1; fetchList() }
const handleTableChange = (pag: any) => {
  pagination.current = pag.current; pagination.pageSize = pag.pageSize; fetchList()
}

const handleAdd = () => {
  Object.assign(addForm, { tenantCode: '', shopName: '', packageId: undefined })
  addVisible.value = true
}

const handleSave = async () => {
  if (!addForm.tenantCode || !addForm.shopName || !addForm.packageId) {
    return message.error('请填写完整信息')
  }
  saving.value = true
  try {
    await request.post('/platform/shops', {
      tenantCode: addForm.tenantCode,
      shopName: addForm.shopName,
      packageId: addForm.packageId
    })
    message.success('开通成功')
    addVisible.value = false
    fetchList()
  } catch (e: any) { message.error(e.message || '开通失败') } finally { saving.value = false }
}

const handleRenew = (record: any) => {
  Object.assign(renewForm, { id: record.id, packageId: record.packageId, durationDays: 365, currentExpiredAt: record.expiredAt })
  renewVisible.value = true
}

const handleRenewSave = async () => {
  renewSaving.value = true
  try {
    await request.put(`/platform/shops/${renewForm.id}/renew`, {
      packageId: renewForm.packageId,
      days: renewForm.durationDays
    })
    message.success('续费成功')
    renewVisible.value = false
    fetchList()
  } catch (e: any) { message.error(e.message || '续费失败') } finally { renewSaving.value = false }
}

const handlePackageChange = (pkgId: string) => {
  const pkg = packageOptions.value.find((p: any) => p.id === pkgId)
  if (pkg && pkg.durationDays) {
    renewForm.durationDays = pkg.durationDays
  }
}

const handleSuspend = async (id: number) => {
  try {
    await request.delete(`/platform/shops/${id}`)
    message.success('已停用')
    fetchList()
  } catch {}
}

const getStatusColor = (status: string) => status === 'ACTIVE' ? 'green' : 'red'
const getStatusText = (status: string) => status === 'ACTIVE' ? '营业中' : '已停用'
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.search-bar { margin-bottom: 16px; }
.platform-shop-list { padding: 16px; background: #f5f7fa; min-height: 100vh; }
</style>
