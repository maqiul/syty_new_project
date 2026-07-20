<template>
  <div>
    <a-card>
      <template #title>操作日志</template>

      <div style="margin-bottom: 16px">
        <a-space>
          <span>模块：</span>
          <a-select v-model:value="query.module" placeholder="全部模块" allow-clear style="width:150px">
            <a-select-option v-for="m in modules" :key="m" :value="m">{{ m }}</a-select-option>
          </a-select>
          <span>操作人：</span>
          <a-input v-model:value="query.operatorName" placeholder="操作人" allow-clear style="width:150px" @pressEnter="search" />
          <a-button type="primary" @click="search">搜索</a-button>
          <a-button @click="resetSearch">重置</a-button>
        </a-space>
      </div>

      <a-table :columns="columns" :dataSource="tableData" :loading="loading" :pagination="pagination" rowKey="id" @change="handleTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'detail'">
            <a-tooltip :title="record.detail">
              <span style="max-width: 300px; display: inline-block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap">{{ record.detail }}</span>
            </a-tooltip>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getOperateLogPage, type OperateLog } from '@/api'

const loading = ref(false)
const tableData = ref<OperateLog[]>([])
const modules = ['球员', '球拍', '球线', '店铺', '库存', '订单', '用户', '租户', '打印模板']

const query = reactive({ page: 1, size: 20, module: undefined as string | undefined, operatorName: '' })
const pagination = reactive({ current: 1, pageSize: 20, total: 0, showTotal: (t: number) => `共 ${t} 条` })

const columns = [
  { title: 'ID', dataIndex: 'id', width: 60 },
  { title: '模块', dataIndex: 'module', width: 100 },
  { title: '操作', dataIndex: 'operation', width: 120 },
  { title: '操作人', dataIndex: 'operatorName', width: 120 },
  { title: '详情', key: 'detail', dataIndex: 'detail', width: 350, ellipsis: true },
  { title: '时间', dataIndex: 'createdAt', width: 180 }
]

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getOperateLogPage(query)
    tableData.value = res.data.records || res.data
    pagination.total = res.data.total || 0
  } finally { loading.value = false }
}

const handleTableChange = (pag: any) => {
  query.page = pag.current; query.size = pag.pageSize
  pagination.current = pag.current; pagination.pageSize = pag.pageSize
  fetchData()
}

const search = () => { query.page = 1; fetchData() }
const resetSearch = () => { query.page = 1; query.module = undefined; query.operatorName = ''; fetchData() }

onMounted(() => fetchData())
</script>
