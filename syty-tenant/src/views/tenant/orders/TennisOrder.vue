<template>
  <div>
    <a-card>
      <template #title>
        <div style="display: flex; justify-content: space-between">
          <span>🎾 网球穿线订单</span>
          <a-button type="primary" @click="openDialog()"><template #icon><PlusOutlined /></template>新建订单</a-button>
        </div>
      </template>
      <a-table :columns="columns" :dataSource="tableData" :loading="loading" :pagination="pagination" rowKey="id" @change="handleTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'"><a-tag :color="record.status === 1 ? 'green' : 'blue'">{{ record.status === 1 ? '已完成' : '待穿' }}</a-tag></template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a @click="completeOrder(record.id)" v-if="record.status === 0">完成</a>
              <a style="color:red" @click="handleDelete(record.id)">删除</a>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="dialogVisible" title="新建网球订单" width="600px" @ok="handleSave" :confirmLoading="saving">
      <a-form :model="dialogForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="球员"><a-select v-model:value="dialogForm.playerId" show-search placeholder="选择球员" :options="playerOptions" style="width:100%" /></a-form-item>
        <a-form-item label="球拍"><a-select v-model:value="dialogForm.racketId" show-search placeholder="选择球拍" :options="racketOptions" style="width:100%" /></a-form-item>
        <a-form-item label="球线"><a-select v-model:value="dialogForm.stringId" show-search placeholder="选择球线" :options="stringOptions" style="width:100%" /></a-form-item>
        <a-form-item label="磅数"><a-input-number v-model:value="dialogForm.tension" :min="30" :max="65" style="width:100%" /></a-form-item>
        <a-form-item label="线材价格"><a-input-number v-model:value="dialogForm.stringPrice" :min="0" :precision="2" style="width:100%" /></a-form-item>
        <a-form-item label="手工费"><a-input-number v-model:value="dialogForm.laborPrice" :min="0" :precision="2" style="width:100%" /></a-form-item>
        <a-form-item label="备注"><a-textarea v-model:value="dialogForm.remark" :rows="2" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'antdv-next'
import { PlusOutlined } from '@antdv-next/icons'
import { getTennisOrderPage, createTennisOrder, completeTennisOrder, deleteTennisOrder, getTennisPlayerList, getTennisRacketList, getTennisStringList } from '@/api'

const loading = ref(false); const saving = ref(false); const dialogVisible = ref(false)
const tableData = ref<any[]>([]); const pagination = reactive({ current: 1, pageSize: 20, total: 0, showSizeChanger: true })
const playerOptions = ref<any[]>([]); const racketOptions = ref<any[]>([]); const stringOptions = ref<any[]>([])
const dialogForm = reactive<any>({ playerId: null, racketId: null, stringId: null, tension: 52, stringPrice: 0, laborPrice: 0, remark: '' })

const columns = [
  { title: '订单号', dataIndex: 'orderNo' }, { title: '状态', key: 'status' },
  { title: '磅数', dataIndex: 'tension' }, { title: '总价', dataIndex: 'totalPrice' },
  { title: '创建时间', dataIndex: 'createdAt' }, { title: '操作', key: 'action', width: 150 }
]

const loadData = async () => { loading.value = true; const r: any = await getTennisOrderPage({ page: pagination.current, size: pagination.pageSize }); tableData.value = r.data.records || r.data.list || []; pagination.total = r.data.total || 0; loading.value = false }
const handleTableChange = (p: any) => { pagination.current = p.current; pagination.pageSize = p.pageSize; loadData() }

const openDialog = async () => {
  dialogForm.playerId = null; dialogForm.racketId = null; dialogForm.stringId = null
  const [pl, rl, sl] = await Promise.all([getTennisPlayerList(), getTennisRacketList(), getTennisStringList()])
  playerOptions.value = (pl.data || []).map((p: any) => ({ value: p.id, label: p.name }))
  racketOptions.value = (rl.data || []).map((r: any) => ({ value: r.id, label: r.brand + ' ' + r.model }))
  stringOptions.value = (sl.data || []).map((s: any) => ({ value: s.id, label: s.brand + ' ' + s.model }))
  dialogVisible.value = true
}

const handleSave = async () => { saving.value = true; try { await createTennisOrder(dialogForm); message.success('ok'); dialogVisible.value = false; loadData() } catch { message.error('fail') } finally { saving.value = false } }
const completeOrder = async (id: number) => { await completeTennisOrder(id); loadData(); message.success('已完成') }
const handleDelete = (id: number) => Modal.confirm({ title: '确认删除？', onOk: async () => { await deleteTennisOrder(id); loadData(); message.success('已删除') } })

onMounted(loadData)
</script>
