<template>
  <div>
    <a-card>
      <template #title>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>球拍管理</span>
          <a-button v-if="isSuperAdmin" type="primary" @click="openDialog()">
            <template #icon><PlusOutlined /></template>
            新增球拍
          </a-button>
        </div>
      </template>

      <a-table :columns="columns" :dataSource="tableData" :loading="loading" :pagination="pagination" rowKey="id" @change="handleTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'action' && isSuperAdmin">
            <a-space>
              <a type="primary" @click="openDialog(record)">编辑</a>
              <a-divider type="vertical" />
              <a type="danger" @click="handleDelete(record.id)">删除</a>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="dialogVisible" :title="dialogForm.id ? '编辑球拍' : '新增球拍'" width="500px" @ok="handleSave" :confirmLoading="saving" @cancel="dialogVisible = false">
      <a-form :model="dialogForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }" ref="formRef">
        <a-form-item label="品牌" name="brand" :rules="[{ required: true, message: '请输入品牌' }]">
          <a-input v-model:value="dialogForm.brand" />
        </a-form-item>
        <a-form-item label="型号" name="model" :rules="[{ required: true, message: '请输入型号' }]">
          <a-input v-model:value="dialogForm.model" />
        </a-form-item>
        <a-form-item label="拍柄尺寸">
          <a-select v-model:value="dialogForm.gripSize" allow-clear>
            <a-select-option value="G1">G1</a-select-option>
            <a-select-option value="G2">G2</a-select-option>
            <a-select-option value="G3">G3</a-select-option>
            <a-select-option value="G4">G4</a-select-option>
            <a-select-option value="G5">G5</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="重量">
          <a-input v-model:value="dialogForm.weight" placeholder="如: 85g" />
        </a-form-item>
        <a-form-item label="平衡点">
          <a-select v-model:value="dialogForm.balance" allow-clear>
            <a-select-option value="头轻">头轻</a-select-option>
            <a-select-option value="平衡">平衡</a-select-option>
            <a-select-option value="头重">头重</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'antdv-next'
import { PlusOutlined } from '@antdv-next/icons'
import { getRacketPage, addRacket, updateRacket, deleteRacket, type Racket } from '@/api'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const isSuperAdmin = userStore.isSuperAdmin

const loading = ref(false)
const tableData = ref<Racket[]>([])
const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref()

const query = reactive({ page: 1, size: 20 })
const pagination = reactive({ current: 1, pageSize: 20, total: 0, showTotal: (t: number) => `共 ${t} 条` })

const dialogForm = reactive<Racket>({ id: undefined, brand: '', model: '', gripSize: '', weight: '', balance: '' })

const columns = [
  { title: 'ID', dataIndex: 'id', width: 60 },
  { title: '品牌', dataIndex: 'brand', width: 150 },
  { title: '型号', dataIndex: 'model', width: 200 },
  { title: '拍柄', dataIndex: 'gripSize', width: 80 },
  { title: '重量', dataIndex: 'weight', width: 100 },
  { title: '平衡点', dataIndex: 'balance', width: 100 },
  ...(isSuperAdmin ? [{ title: '操作', key: 'action', width: 180 }] : [])
]

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getRacketPage(query)
    tableData.value = res.data.records || res.data
    pagination.total = res.data.total || 0
  } finally { loading.value = false }
}

const handleTableChange = (pag: any) => {
  query.page = pag.current; query.size = pag.pageSize
  pagination.current = pag.current; pagination.pageSize = pag.pageSize
  fetchData()
}

const openDialog = (row?: Racket) => {
  if (row) Object.assign(dialogForm, { id: row.id, brand: row.brand, model: row.model, gripSize: row.gripSize || '', weight: row.weight || '', balance: row.balance || '' })
  else Object.assign(dialogForm, { id: undefined, brand: '', model: '', gripSize: '', weight: '', balance: '' })
  dialogVisible.value = true
}

const handleSave = async () => {
  try { await formRef.value?.validate() } catch { return }
  saving.value = true
  try {
    if (dialogForm.id) { await updateRacket(dialogForm); message.success('更新成功') }
    else { await addRacket(dialogForm); message.success('创建成功') }
    dialogVisible.value = false; fetchData()
  } finally { saving.value = false }
}

const handleDelete = (id: number) => {
  Modal.confirm({ title: '确认删除', content: '确定要删除吗？', okText: '确定', cancelText: '取消',
    onOk: async () => { await deleteRacket(id); message.success('删除成功'); fetchData() }
  })
}

onMounted(() => fetchData())
</script>
