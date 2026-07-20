<template>
  <div>
    <a-card>
      <template #title>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>🎾 店铺网球线库存</span>
          <div>
            <a-button type="primary" @click="openDialog()">
              <template #icon><PlusOutlined /></template>
              添加网球线
            </a-button>
          </div>
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
            @change="fetchData"
          >
            <a-select-option v-for="s in shopList" :key="s.id" :value="s.id">
              {{ s.name }}
            </a-select-option>
          </a-select>
        </a-space>
      </div>

      <a-table :columns="columns" :dataSource="tableData" :loading="loading" rowKey="id"
        :row-class-name="(r: any) => r.stock < 10 ? 'low-stock' : ''">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'action'">
            <a-space>
              <a @click="openDialog(record)">编辑</a>
              <a-divider type="vertical" />
              <a style="color:red" @click="handleDelete(record.id)">删除</a>
            </a-space>
          </template>
          <template v-if="column.dataIndex === 'stock'">
            <span :style="{ color: record.stock < 10 ? '#ff4d4f' : 'inherit', fontWeight: record.stock < 10 ? 'bold' : 'normal' }">
              {{ record.stock }}
            </span>
            <a-tag v-if="record.stock < 10" color="error" style="margin-left: 8px; font-size: 12px">缺</a-tag>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="dialogVisible" :title="dialogForm.id ? '编辑库存' : '添加上架网球线'" width="500px" @ok="handleSave" :confirmLoading="saving">
      <a-form :model="dialogForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="网球线" required>
          <a-select v-model:value="dialogForm.stringId" style="width:100%" placeholder="选择网球线">
            <a-select-option v-for="s in stringList" :key="s.id" :value="s.id">{{ s.brand }} {{ s.model }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="售价" required>
          <a-input-number v-model:value="dialogForm.price" :min="0" :precision="2" style="width:100%" />
        </a-form-item>
        <a-form-item label="库存" required>
          <a-input-number v-model:value="dialogForm.stock" :min="0" style="width:100%" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<style>.low-stock td { background-color: #fff1f0 !important; }</style>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'antdv-next'
import { PlusOutlined } from '@antdv-next/icons'
import { getShopList, getTennisStringList, getTennisShopStrings, addTennisShopString, updateTennisShopString, deleteTennisShopString } from '@/api'
import type { Shop } from '@/api'

const selectedShopId = ref<number>()
const shopList = ref<Shop[]>([])
const loading = ref(false)
const tableData = ref<any[]>([])
const stringList = ref<any[]>([])
const dialogVisible = ref(false)
const saving = ref(false)

const dialogForm = reactive<any>({
  id: null, shopId: 0, stringId: null, price: 0, stock: 0
})

const columns = [
  { title: '品牌/型号', key: 'info', width: 250 },
  { title: '售价', dataIndex: 'price', width: 120 },
  { title: '库存', dataIndex: 'stock', width: 100 },
  { title: '操作', key: 'action', width: 180 }
]

const fetchData = async () => {
  if (!selectedShopId.value) return
  loading.value = true
  try {
    const res = await getTennisShopStrings(selectedShopId.value)
    tableData.value = res.data || []
  } finally { loading.value = false }
}

const openDialog = (row?: any) => {
  if (row) {
    Object.assign(dialogForm, { id: row.id, shopId: selectedShopId.value, stringId: row.stringId, price: row.price, stock: row.stock })
  } else {
    Object.assign(dialogForm, { id: null, shopId: selectedShopId.value || 0, stringId: null, price: 0, stock: 0 })
  }
  dialogVisible.value = true
}

const handleSave = async () => {
  if (!selectedShopId.value) return
  saving.value = true
  try {
    if (dialogForm.id) {
      await updateTennisShopString(dialogForm)
    } else {
      await addTennisShopString(dialogForm)
    }
    message.success(dialogForm.id ? '更新成功' : '添加成功')
    dialogVisible.value = false
    fetchData()
  } finally { saving.value = false }
}

const handleDelete = (id: number) => {
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除该网球线配置吗？',
    onOk: async () => {
      await deleteTennisShopString(id)
      message.success('删除成功')
      fetchData()
    }
  })
}

onMounted(async () => {
  try {
    const res = await getShopList()
    shopList.value = (res.data || []) as Shop[]
  } catch { /* ignore */ }
  try {
    const strRes = await getTennisStringList()
    stringList.value = strRes.data || []
  } catch { /* ignore */ }
})
</script>
