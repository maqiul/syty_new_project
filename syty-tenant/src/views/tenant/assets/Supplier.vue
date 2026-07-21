<template>
  <div>
    <a-card>
      <template #title>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>📦 供应商管理</span>
          <a-button type="primary" @click="openDialog()"><template #icon><PlusOutlined /></template>新增供应商</a-button>
        </div>
      </template>

      <a-table :columns="columns" :dataSource="tableData" :loading="loading" :pagination="pagination" rowKey="id" @change="handleTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'action'">
            <a-space>
              <a @click="openDialog(record)">编辑</a>
              <a-divider type="vertical" />
              <a style="color:red" @click="handleDelete(record.id)">删除</a>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="dialogVisible" :title="dialogForm.id ? '编辑供应商' : '新增供应商'" width="560px" @ok="handleSave" :confirmLoading="saving">
      <a-form :model="dialogForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="名称" required><a-input v-model:value="dialogForm.name" placeholder="供应商名称" /></a-form-item>
        <a-form-item label="联系人"><a-input v-model:value="dialogForm.contactPerson" placeholder="联系人姓名" /></a-form-item>
        <a-form-item label="电话"><a-input v-model:value="dialogForm.phone" placeholder="联系电话" /></a-form-item>
        <a-form-item label="邮箱"><a-input v-model:value="dialogForm.email" placeholder="电子邮箱" /></a-form-item>
        <a-form-item label="地址"><a-input v-model:value="dialogForm.address" placeholder="供应商地址" /></a-form-item>
        <a-form-item label="运动类型"><a-select v-model:value="dialogForm.sportType" allow-clear placeholder="选择运动类型">
          <a-select-option value="BADMINTON">羽毛球</a-select-option>
          <a-select-option value="TENNIS">网球</a-select-option>
          <a-select-option value="DUAL">双修</a-select-option>
        </a-select></a-form-item>
        <a-form-item label="备注"><a-textarea v-model:value="dialogForm.remark" :rows="2" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'antdv-next'
import { PlusOutlined } from '@antdv-next/icons'
import { getSupplierPage, addSupplier, updateSupplier, deleteSupplier } from '@/api'

const loading = ref(false); const saving = ref(false); const dialogVisible = ref(false)
const tableData = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0, showSizeChanger: true })
const dialogForm = reactive<any>({ id: null, name: '', contactPerson: '', phone: '', email: '', address: '', sportType: '', remark: '' })

const columns = [
  { title: '名称', dataIndex: 'name', key: 'name' },
  { title: '联系人', dataIndex: 'contactPerson', key: 'contactPerson' },
  { title: '电话', dataIndex: 'phone', key: 'phone' },
  { title: '运动类型', dataIndex: 'sportType', key: 'sportType' },
  { title: '操作', key: 'action', width: 150 }
]

const loadData = async () => {
  loading.value = true
  const res: any = await getSupplierPage({ page: pagination.current, size: pagination.pageSize })
  tableData.value = res.data.records || res.data.list || []
  pagination.total = res.data.total || 0
  loading.value = false
}

const handleTableChange = (pag: any) => { pagination.current = pag.current; pagination.pageSize = pag.pageSize; loadData() }

const openDialog = (record?: any) => {
  if (record) Object.assign(dialogForm, record)
  else Object.keys(dialogForm).forEach(k => (dialogForm as any)[k] = k === 'id' ? null : '')
  dialogVisible.value = true
}

const handleSave = async () => {
  saving.value = true
  try {
    if (dialogForm.id) await updateSupplier(dialogForm)
    else await addSupplier(dialogForm)
    message.success('保存成功'); dialogVisible.value = false; loadData()
  } catch { message.error('保存失败') }
  finally { saving.value = false }
}

const handleDelete = (id: number) => {
  Modal.confirm({ title: '确认删除？', onOk: async () => { await deleteSupplier(id); loadData(); message.success('已删除') } })
}

onMounted(loadData)
</script>
