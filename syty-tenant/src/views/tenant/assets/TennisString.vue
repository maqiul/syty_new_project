<template>
  <div>
    <a-card>
      <template #title>
        <div style="display: flex; justify-content: space-between">
          <span>🎾 网球球线管理</span>
          <a-button type="primary" @click="openDialog()"><template #icon><PlusOutlined /></template>新增球线</a-button>
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

    <a-modal v-model:open="dialogVisible" :title="dialogForm.id ? '编辑球线' : '新增球线'" width="500px" @ok="handleSave" :confirmLoading="saving">
      <a-form :model="dialogForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="品牌" required><a-input v-model:value="dialogForm.brand" placeholder="Luxilon / Babolat / Yonex..." /></a-form-item>
        <a-form-item label="型号" required><a-input v-model:value="dialogForm.model" placeholder="ALU Power..." /></a-form-item>
        <a-form-item label="类型"><a-select v-model:value="dialogForm.type" allow-clear>
          <a-select-option value="聚酯">聚酯</a-select-option><a-select-option value="天然肠线">天然肠线</a-select-option>
          <a-select-option value="仿肠线">仿肠线</a-select-option><a-select-option value="凯夫拉">凯夫拉</a-select-option>
        </a-select></a-form-item>
        <a-form-item label="线径"><a-input-number v-model:value="dialogForm.gauge" :min="0" :step="0.01" style="width:100%" placeholder="mm" /></a-form-item>
        <a-form-item label="颜色"><a-input v-model:value="dialogForm.color" placeholder="Natural / Black / Yellow..." /></a-form-item>
        <a-form-item label="备注"><a-textarea v-model:value="dialogForm.remark" :rows="2" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'antdv-next'
import { PlusOutlined } from '@antdv-next/icons'
import { getTennisStringPage, addTennisString, updateTennisString, deleteTennisString } from '@/api'

const loading = ref(false); const saving = ref(false); const dialogVisible = ref(false)
const tableData = ref<any[]>([]); const pagination = reactive({ current: 1, pageSize: 20, total: 0, showSizeChanger: true })
const dialogForm = reactive<any>({ id: null, brand: '', model: '', type: '', gauge: null, color: '', remark: '' })
const columns = [
  { title: '品牌', dataIndex: 'brand' }, { title: '型号', dataIndex: 'model' },
  { title: '类型', dataIndex: 'type' }, { title: '线径', dataIndex: 'gauge' },
  { title: '颜色', dataIndex: 'color' }, { title: '操作', key: 'action', width: 150 }
]
const loadData = async () => { loading.value = true; const r: any = await getTennisStringPage({ page: pagination.current, size: pagination.pageSize }); tableData.value = r.data.records || r.data.list || []; pagination.total = r.data.total || 0; loading.value = false }
const handleTableChange = (p: any) => { pagination.current = p.current; pagination.pageSize = p.pageSize; loadData() }
const openDialog = (r?: any) => { Object.assign(dialogForm, r || { id: null, brand: '', model: '', type: '', gauge: null, color: '', remark: '' }); dialogVisible.value = true }
const handleSave = async () => { saving.value = true; try { if (dialogForm.id) await updateTennisString(dialogForm); else await addTennisString(dialogForm); message.success('ok'); dialogVisible.value = false; loadData() } catch { message.error('fail') } finally { saving.value = false } }
const handleDelete = (id: number) => Modal.confirm({ title: '确认删除？', onOk: async () => { await deleteTennisString(id); loadData(); message.success('已删除') } })
onMounted(loadData)
</script>
