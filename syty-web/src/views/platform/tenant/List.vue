<template>
  <div class="platform-tenant-list">
    <a-card :bordered="false">
      <template #title>
        <div class="card-header">
          <span class="card-title">租户列表</span>
          <a-button type="primary" @click="handleAdd">
            <template #icon><PlusOutlined /></template>
            新增租户
          </a-button>
        </div>
      </template>

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
            <a-tag :color="record.status === 1 ? 'green' : 'red'">
              {{ record.status === 1 ? '启用' : '禁用' }}
            </a-tag>
          </template>
          <template v-if="column.dataIndex === 'packageStatus'">
            <a-tag :color="record.packageStatus === 'ACTIVE' ? 'blue' : 'orange'">
              {{ record.packageStatus === 'ACTIVE' ? '正常' : '未知' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a @click="handleEdit(record)">编辑</a>
              <a-divider type="vertical" />
              <a-popconfirm title="确认删除该租户？" @confirm="handleDelete(record.id)">
                <a style="color: #ff4d4f">删除</a>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新增/编辑弹窗 -->
    <a-modal v-model:open="dialogVisible" :title="isEdit ? '编辑租户' : '新增租户'" @ok="handleSave" :confirmLoading="saving">
      <a-form :model="dialogForm" layout="vertical">
        <a-form-item label="租户名称" required><a-input v-model:value="dialogForm.name" /></a-form-item>
        <a-form-item label="租户编码" required><a-input v-model:value="dialogForm.tenantCode" :disabled="isEdit" /></a-form-item>
        <a-form-item label="所属套餐" required>
          <a-select v-model:value="dialogForm.packageId" :disabled="isEdit" placeholder="请选择套餐">
            <a-select-option v-for="p in packageList" :key="p.id" :value="p.id">{{ p.name }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="联系人"><a-input v-model:value="dialogForm.contact" /></a-form-item>
        <a-form-item label="联系电话"><a-input v-model:value="dialogForm.phone" /></a-form-item>
        <a-form-item label="状态"><a-switch v-model:checked="dialogForm.status" /></a-form-item>
      </a-form>
    </a-modal>

  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import {
  getPlatformTenantsList,
  deletePlatformTenant,
  addPlatformTenant,
  updatePlatformTenant,
  type PlatformTenantInfo,
} from '@/api/admin/tenant'
import { getPlatformPackagesList, type PackageInfo } from '@/api/admin/package'

const columns = [
  { title: '租户名称', dataIndex: 'name', key: 'name' },
  { title: '租户编码', dataIndex: 'tenantCode', key: 'tenantCode', width: 120 },
  { title: '套餐到期', dataIndex: 'packageExpiredAt', key: 'packageExpiredAt', width: 160 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '套餐状态', dataIndex: 'packageStatus', key: 'packageStatus', width: 100 },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 160 },
  { title: '操作', key: 'action', width: 140, fixed: 'right' as const },
]

const tableData = ref<PlatformTenantInfo[]>([])
const packageList = ref<PackageInfo[]>([])
const loading = ref(false)
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })
const queryParams = reactive({ keyword: '' })

// 基础弹窗
const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const dialogForm = reactive({
  id: undefined as number | undefined,
  name: '',
  tenantCode: '',
  packageId: undefined as number | undefined,
  contact: '',
  phone: '',
  status: true,
})

const fetchTenants = async () => {
  loading.value = true
  try {
    const res = await getPlatformTenantsList({
      page: pagination.current,
      size: pagination.pageSize,
      keyword: queryParams.keyword || undefined,
    })
    tableData.value = res.data.records
    pagination.total = res.data.total
  } catch (e: any) {
    message.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const loadPackages = async () => {
  try {
    const res = await getPlatformPackagesList()
    packageList.value = res.data || []
  } catch {}
}

onMounted(() => {
  fetchTenants()
  loadPackages()
})

const handleTableChange = (pag: any) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchTenants()
}

const handleSearch = () => { pagination.current = 1; fetchTenants() }
const handleReset = () => { queryParams.keyword = ''; pagination.current = 1; fetchTenants() }

const handleAdd = () => {
  isEdit.value = false
  Object.assign(dialogForm, { id: undefined, name: '', tenantCode: '', packageId: undefined, contact: '', phone: '', status: true })
  dialogVisible.value = true
}

const handleEdit = (record: any) => {
  isEdit.value = true
  Object.assign(dialogForm, {
    id: record.id, name: record.name, tenantCode: record.tenantCode, packageId: record.packageId,
    contact: record.contact, phone: record.phone, status: record.status === 1
  })
  dialogVisible.value = true
}

const handleSave = async () => {
  if (!dialogForm.name || !dialogForm.tenantCode || !dialogForm.packageId) {
    return message.error('请填写必填项')
  }
  saving.value = true
  try {
    const payload = { ...dialogForm, status: dialogForm.status ? 1 : 0 }
    isEdit.value ? await updatePlatformTenant(payload) : await addPlatformTenant(payload)
    message.success('保存成功')
    dialogVisible.value = false
    fetchTenants()
  } catch (e: any) { message.error(e.message || '保存失败') } finally { saving.value = false }
}

const handleDelete = async (id: number) => {
  try { await deletePlatformTenant(id); message.success('删除成功'); fetchTenants() } catch {}
}
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.platform-tenant-list { padding: 16px; background: #f5f7fa; min-height: 100vh; }
</style>
