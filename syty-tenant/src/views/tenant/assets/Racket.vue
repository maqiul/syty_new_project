<template>
  <div>
    <a-card>
      <template #title>
        <div style="display: flex; justify-content: space-between">
          <span>🏸 球拍管理</span>
          <a-button type="primary" @click="openDialog()"><template #icon><PlusOutlined /></template>新增球拍</a-button>
        </div>
      </template>
      <a-table :columns="columns" :dataSource="tableData" :loading="loading" :pagination="pagination" rowKey="id" @change="handleTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'action'">
            <a-space><a @click="openDialog(record)">编辑</a><a-divider type="vertical" /><a style="color:red" @click="handleDelete(record.id)">删除</a></a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="dialogVisible" :title="dialogForm.id ? '编辑球拍' : '新增球拍'" width="500px" @ok="handleSave" :confirmLoading="saving">
      <a-form :model="dialogForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="品牌" required><a-input v-model:value="dialogForm.brand" placeholder="Yonex / Victor / Li-Ning..." /></a-form-item>
        <a-form-item label="型号" required><a-input v-model:value="dialogForm.model" placeholder="Astrox 99 / Arcsaber 11..." /></a-form-item>
        <a-form-item label="拍面大小"><a-input-number v-model:value="dialogForm.headSize" :min="80" :max="135" style="width:100%" placeholder="平方英寸" /></a-form-item>
        <a-form-item label="穿线模式"><a-input v-model:value="dialogForm.stringPattern" placeholder="16x19" /></a-form-item>
        <a-form-item label="重量(g)"><a-input-number v-model:value="dialogForm.weight" :min="200" :max="400" style="width:100%" /></a-form-item>
        <a-form-item label="平衡点"><a-input v-model:value="dialogForm.balance" placeholder="320mm" /></a-form-item>
        <a-form-item label="备注"><a-textarea v-model:value="dialogForm.remark" :rows="2" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'antdv-next'
import { PlusOutlined } from '@antdv-next/icons'
import { getRacketPage, addRacket, updateRacket, deleteRacket } from '@/api'

const loading = ref(false); const saving = ref(false); const dialogVisible = ref(false)
const tableData = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0, showSizeChanger: true })
const dialogForm = reactive<any>({ id: null, brand: '', model: '', headSize: 100, stringPattern: '', weight: '', balance: '', remark: '' })
const columns = [
  { title: '品牌', dataIndex: 'brand' }, { title: '型号', dataIndex: 'model' },
  { title: '拍面', dataIndex: 'headSize' }, { title: '穿线模式', dataIndex: 'stringPattern' },
  { title: '重量(g)', dataIndex: 'weight' }, { title: '操作', key: 'action', width: 150 }
]

const loadData = async () => {
  loading.value = true
  const res: any = await getRacketPage({ page: pagination.current, size: pagination.pageSize })
  tableData.value = res.data.records || res.data.list || []; pagination.total = res.data.total || 0; loading.value = false
}
const handleTableChange = (p: any) => { pagination.current = p.current; pagination.pageSize = p.pageSize; loadData() }
const openDialog = (r?: any) => { Object.assign(dialogForm, r || { id: null, brand: '', model: '', headSize: 100, stringPattern: '', weight: '', balance: '', remark: '' }); dialogVisible.value = true }
const handleSave = async () => { saving.value = true; try { if (dialogForm.id) await updateRacket(dialogForm); else await addRacket(dialogForm); message.success('ok'); dialogVisible.value = false; loadData() } catch { message.error('fail') } finally { saving.value = false } }
const handleDelete = (id: number) => { Modal.confirm({ title: '确认删除？', onOk: async () => { await deleteRacket(id); loadData(); message.success('已删除') } }) }
onMounted(loadData)
</script>
