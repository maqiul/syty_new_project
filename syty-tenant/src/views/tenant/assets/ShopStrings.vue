<template>
  <div>
    <a-card>
      <template #title>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>店铺球线库存</span>
          <div>
            <a-button type="primary" @click="openDialog()">
              <template #icon><PlusOutlined /></template>
              添加球线
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
            @change="onShopChange"
          >
            <a-select-option v-for="s in shopList" :key="s.id" :value="s.id">
              {{ s.name }}
            </a-select-option>
          </a-select>
        </a-space>
      </div>

      <a-table
        :columns="columns"
        :dataSource="tableData"
        :loading="loading"
        rowKey="id"
        :rowClassName="(record: any) => record.stock < 10 ? 'low-stock' : ''"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'action'">
            <a-space>
              <a type="primary" @click="openDialog(record)">编辑</a>
              <a-divider type="vertical" />
              <a type="danger" @click="handleDelete(record.id)">删除</a>
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

    <a-modal
      v-model:open="dialogVisible"
      :title="dialogForm.id ? '编辑库存' : '添加上架球线'"
      width="500px"
      @ok="handleSave"
      :confirmLoading="saving"
      @cancel="dialogVisible = false"
    >
      <a-form :model="dialogForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }" ref="formRef">
        <a-form-item label="球线" name="stringInfoId" :rules="[{ required: true, message: '请选择球线' }]">
          <a-select v-model:value="dialogForm.stringInfoId" style="width:100%" placeholder="请选择球线" @change="onStringChange">
            <a-select-option v-for="s in stringList" :key="s.id" :value="s.id">{{ s.brand }} {{ s.model }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="售价" name="price" :rules="[{ required: true, message: '请输入售价' }]">
          <a-input-number v-model:value="dialogForm.price" :min="0" :precision="2" style="width:100%" />
        </a-form-item>
        <a-form-item label="库存" name="stock" :rules="[{ required: true, message: '请输入库存' }]">
          <a-input-number v-model:value="dialogForm.stock" :min="0" style="width:100%" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { message, Modal } from 'antdv-next'
import { PlusOutlined } from '@antdv-next/icons'
import {
  getShopList, getStringInfoList, getShopStrings, saveShopString,
  updateShopString, deleteShopString,
  type ShopString, type StringInfo, type Shop
} from '@/api'

const route = useRoute()
const selectedShopId = ref<number>()
const shopList = ref<Shop[]>([])
const loading = ref(false)
const tableData = ref<ShopString[]>([])
const stringList = ref<StringInfo[]>([])
const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref()

const dialogForm = reactive<ShopString>({
  id: undefined, shopId: 0, stringInfoId: undefined as any,
  stringName: '', price: 0, stock: 0
})

const columns = [
  { title: '球线名称', dataIndex: 'stringName', width: 250 },
  { title: '售价', dataIndex: 'price', width: 120 },
  { title: '库存', dataIndex: 'stock', width: 100 },
  { title: '操作', key: 'action', width: 180 }
]

const fetchData = async () => {
  if (!selectedShopId.value) return
  loading.value = true
  try {
    const res = await getShopStrings(selectedShopId.value)
    tableData.value = res.data || []
  } finally { loading.value = false }
}

const onShopChange = () => {
  fetchData()
}

const openDialog = (row?: ShopString) => {
  const shopId = selectedShopId.value || 0
  if (row) {
    Object.assign(dialogForm, {
      id: row.id, shopId, stringInfoId: row.stringInfoId,
      stringName: row.stringName || '', price: row.price, stock: row.stock
    })
  } else {
    Object.assign(dialogForm, {
      id: undefined, shopId, stringInfoId: undefined,
      stringName: '', price: 0, stock: 0
    })
  }
  dialogVisible.value = true
}

const onStringChange = (val: number) => {
  const s = stringList.value.find(x => x.id === val)
  if (s) dialogForm.stringName = `${s.brand} ${s.model}`
}

const handleSave = async () => {
  try { await formRef.value?.validate() } catch { return }
  if (!selectedShopId.value) return
  saving.value = true
  try {
    if (dialogForm.id) {
      await updateShopString(selectedShopId.value, dialogForm)
    } else {
      await saveShopString(selectedShopId.value, dialogForm)
    }
    message.success(dialogForm.id ? '更新成功' : '添加成功')
    dialogVisible.value = false
    fetchData()
  } finally { saving.value = false }
}

const handleDelete = (id: number) => {
  if (!selectedShopId.value) return
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除该球线配置吗？',
    okText: '确定',
    onOk: async () => {
      await deleteShopString(selectedShopId.value!, id)
      message.success('删除成功')
      fetchData()
    }
  })
}

onMounted(async () => {
  // 加载店铺列表
  try {
    const res = await getShopList()
    shopList.value = (res.data || []) as Shop[]
  } catch { /* ignore */ }

  // 加载球线列表
  try {
    const strRes = await getStringInfoList()
    stringList.value = strRes.data || []
  } catch { /* ignore */ }

  // 从 query 参数预选店铺
  const qShopId = route.query.shopId
  if (qShopId) {
    selectedShopId.value = Number(qShopId)
    if (selectedShopId.value) fetchData()
  }
})
</script>

<style>
.low-stock td {
  background-color: #fff1f0 !important;
}
</style>
