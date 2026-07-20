<template>
  <div>
    <a-card>
      <template #title>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>租户管理</span>
          <a-button type="primary" @click="openDialog()">
            <template #icon><PlusOutlined /></template>
            新增租户
          </a-button>
        </div>
      </template>

      <a-table :columns="columns" :dataSource="tableData" :loading="loading" :pagination="pagination" rowKey="id" @change="handleTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'status'">
            <a-tag :color="record.status === 1 ? 'green' : 'red'">{{ record.status === 1 ? '启用' : '禁用' }}</a-tag>
          </template>
          <template v-if="column.key === 'badminton'">
            <a-tag :color="record.enableBadmintonTournament === 1 ? 'green' : 'default'">{{ record.enableBadmintonTournament === 1 ? '开' : '关' }}</a-tag>
          </template>
          <template v-if="column.key === 'tennis'">
            <a-tag :color="record.enableTennisTournament === 1 ? 'green' : 'default'">{{ record.enableTennisTournament === 1 ? '开' : '关' }}</a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a type="primary" @click="openDialog(record)">编辑</a>
              <a-divider type="vertical" />
              <a type="danger" @click="handleDelete(record.id)">删除</a>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="dialogVisible" :title="dialogForm.id ? '编辑租户' : '新增租户'" width="500px" @ok="handleSave" :confirmLoading="saving" @cancel="resetForm">
      <a-form :model="dialogForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }" ref="formRef">
        <a-form-item label="租户名称" name="name" :rules="[{ required: true, message: '请输入租户名称' }]">
          <a-input v-model:value="dialogForm.name" />
        </a-form-item>
        <a-form-item label="租户编码" name="code" :rules="[{ required: true, message: '请输入编码' }]">
          <a-input v-model:value="dialogForm.code" placeholder="唯一标识，如: tianhe" />
        </a-form-item>
        <a-form-item label="联系人">
          <a-input v-model:value="dialogForm.contactPerson" />
        </a-form-item>
        <a-form-item label="联系电话">
          <a-input v-model:value="dialogForm.phone" />
        </a-form-item>
        <a-form-item label="状态">
          <a-switch v-model:checked="dialogForm.statusBool" checked-children="启用" un-checked-children="禁用" />
        </a-form-item>
        <a-form-item label="到期时间">
          <a-date-picker v-model:value="dialogForm.expiredAtDayjs" show-time value-format="YYYY-MM-DD HH:mm:ss" placeholder="选填" style="width:100%" />
        </a-form-item>
        <a-divider orientation="left" plain>功能开关</a-divider>
        <a-form-item label="羽毛球大赛">
          <a-switch v-model:checked="dialogForm.badmintonBool" checked-children="启用" un-checked-children="禁用" />
        </a-form-item>
        <a-form-item label="网球大赛">
          <a-switch v-model:checked="dialogForm.tennisBool" checked-children="启用" un-checked-children="禁用" />
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model:value="dialogForm.remark" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import utc from 'dayjs/plugin/utc'
dayjs.extend(utc)
import { getTenantPage, addTenant, updateTenant, deleteTenant, type Tenant } from '@/api'

const columns = [
  { dataIndex: 'id', title: 'ID', width: 60 },
  { dataIndex: 'name', title: '租户名称', ellipsis: true },
  { dataIndex: 'code', title: '编码' },
  { dataIndex: 'contactPerson', title: '联系人' },
  { dataIndex: 'phone', title: '电话' },
  { dataIndex: 'status', title: '状态', width: 70 },
  { key: 'badminton', title: '羽大赛', width: 70 },
  { key: 'tennis', title: '网大赛', width: 70 },
  { key: 'action', title: '操作', width: 150 }
]

const loading = ref(false)
const tableData = ref<Tenant[]>([])
const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref()

const query = reactive({ page: 1, size: 20 })
const total = ref(0)
const pagination = reactive({ current: 1, pageSize: 20, total: 0, showTotal: (t: number) => `共 ${t} 条` })

interface FormData {
  id?: number
  name: string
  code: string
  contactPerson: string
  phone: string
  statusBool: boolean
  expiredAtDayjs: any
  badmintonBool: boolean
  tennisBool: boolean
  remark: string
}

const dialogForm = reactive<FormData>({
  id: undefined, name: '', code: '', contactPerson: '', phone: '',
  statusBool: true, expiredAtDayjs: undefined, badmintonBool: true, tennisBool: false, remark: ''
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getTenantPage({ page: query.page, size: query.size })
    tableData.value = (res.data as any).records || []
    const pg = res.data as any
    total.value = pg.total || 0
    pagination.current = pg.current || 1
    pagination.pageSize = pg.size || 20
    pagination.total = total.value
  } finally { loading.value = false }
}

const handleTableChange = (pag: any) => {
  query.page = pag.current; query.size = pag.pageSize
  fetchData()
}

const openDialog = (row?: Tenant) => {
  if (row) {
    const expiredStr = (row as any).expiredAt
    dialogForm.id = row.id
    dialogForm.name = row.name || ''
    dialogForm.code = row.code || ''
    dialogForm.contactPerson = (row as any).contactPerson || ''
    dialogForm.phone = (row as any).phone || ''
    dialogForm.statusBool = row.status === 1
    dialogForm.expiredAtDayjs = expiredStr ? dayjs(expiredStr) : undefined
    dialogForm.badmintonBool = row.enableBadmintonTournament === 1
    dialogForm.tennisBool = row.enableTennisTournament === 1
    dialogForm.remark = (row as any).remark || ''
  } else {
    Object.assign(dialogForm, { id: undefined, name: '', code: '', contactPerson: '', phone: '', statusBool: true, expiredAtDayjs: undefined, badmintonBool: true, tennisBool: false, remark: '' })
  }
  dialogVisible.value = true
}

const resetForm = () => { dialogVisible.value = false }

const handleSave = async () => {
  try { await formRef.value?.validate() } catch { return }
  saving.value = true
  try {
    const payload = {
      id: dialogForm.id,
      name: dialogForm.name,
      code: dialogForm.code,
      contactPerson: dialogForm.contactPerson,
      phone: dialogForm.phone,
      status: dialogForm.statusBool ? 1 : 0,
      enableBadmintonTournament: dialogForm.badmintonBool ? 1 : 0,
      enableTennisTournament: dialogForm.tennisBool ? 1 : 0,
      expiredAt: dialogForm.expiredAtDayjs ? dayjs(dialogForm.expiredAtDayjs).format('YYYY-MM-DD HH:mm:ss') : null,
      remark: dialogForm.remark
    }
    if (dialogForm.id) {
      await updateTenant(payload as any)
      message.success('更新成功')
    } else {
      await addTenant(payload as any)
      message.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } finally { saving.value = false }
}

const handleDelete = (id: number) => {
  Modal.confirm({
    title: '确认删除', content: '确定要删除该租户吗？',
    okText: '确定', cancelText: '取消',
    onOk: async () => { await deleteTenant(id); message.success('删除成功'); fetchData() }
  })
}

onMounted(() => fetchData())
</script>
