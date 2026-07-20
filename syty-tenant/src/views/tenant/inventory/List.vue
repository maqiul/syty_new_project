<template>
  <div class="inventory-list">
    <a-card title="线材库存" :bordered="false">
      <template #extra>
        <a-button type="primary" @click="showAddModal">
          <template #icon><PlusOutlined /></template>
          入库补货
        </a-button>
      </template>

      <a-table
        :columns="columns"
        :data-source="data"
        :loading="loading"
        :pagination="pagination"
        rowKey="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'nameAndSpec'">
            <div>{{ record.name }}</div>
            <div class="text-gray">{{ record.spec }}</div>
          </template>

          <template v-if="column.key === 'stock'">
            <span :class="{ 'text-red': record.stock <= record.threshold }">
              {{ record.stock }}
            </span>
            <span class="text-gray">{{ record.unit }}</span>
          </template>

          <template v-if="column.key === 'status'">
            <a-tag :color="record.stock <= record.threshold ? 'red' : 'green'">
              {{ record.stock <= record.threshold ? '库存不足' : '库存充足' }}
            </a-tag>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 入库弹窗 -->
    <a-modal v-model:open="addVisible" title="线材入库/补货" @ok="handleAdd" :confirmLoading="adding">
      <a-form layout="vertical">
        <a-form-item label="线材 ID (String ID)" required>
          <a-input-number v-model:value="addForm.stringId" :min="1" style="width: 100%" placeholder="请输入线材库 ID" />
        </a-form-item>
        <a-form-item label="入库数量" required>
          <a-input-number v-model:value="addForm.count" :min="1" style="width: 100%" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { PlusOutlined } from '@antdv-next/icons'
import { getInventoryList } from '@/api/tenant/inventory'
import request from '@/utils/axios'

// 定义变量
const data = ref([])
const loading = ref(false)
const pagination = ref({ current: 1, pageSize: 10, total: 0 })

const columns = [
  { title: '线材/规格', key: 'nameAndSpec', width: 200 },
  { title: '当前库存', key: 'stock', width: 150 },
  { title: '价格', dataIndex: 'price', key: 'price', width: 100 },
  { title: '状态', key: 'status', width: 120 }
]

// 入库相关
const addVisible = ref(false)
const adding = ref(false)
const addForm = reactive({ stringId: 1, count: 1 })

const showAddModal = () => { addVisible.value = true }

const handleAdd = async () => {
  adding.value = true
  try {
    await request.post('/tenant/inventory', addForm)
    addVisible.value = false
    fetchData() // 刷新列表
  } catch (e) {
    console.error('Add failed', e)
  } finally {
    adding.value = false
  }
}

onMounted(fetchData)
</script>

<style scoped>
.text-gray { color: #999; font-size: 12px; }
.text-red { color: #ff4d4f; font-weight: bold; }
.inventory-list { padding: 24px; background: #f0f2f5; min-height: 100vh; }
</style>
