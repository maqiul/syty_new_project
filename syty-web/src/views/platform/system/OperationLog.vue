<template>
  <div>
    <a-card>
      <template #extra>
        <a-space>
          <a-input v-model:value="filterModule" placeholder="模块筛选" @press-enter="fetchData" style="width: 160px;" allow-clear />
          <a-input v-model:value="filterAction" placeholder="操作筛选" @press-enter="fetchData" style="width: 160px;" allow-clear />
          <a-button @click="fetchData">查询</a-button>
        </a-space>
      </template>

      <a-table :columns="columns" :data-source="dataList" :loading="loading" :pagination="pagination" @change="handleTableChange" row-key="id">
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getOperateLogPage } from '@/api/platform/log'

const loading = ref(false)
const dataList = ref<any[]>([])
const filterModule = ref('')
const filterAction = ref('')
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const columns = [
  { title: '操作人', dataIndex: 'username', key: 'username' },
  { title: '操作', dataIndex: 'operation', key: 'operation' },
  { title: '详情', dataIndex: 'detail', key: 'detail', ellipsis: true },
  { title: '模块', dataIndex: 'module', key: 'module' },
  { title: 'IP', dataIndex: 'ip', key: 'ip' },
  { title: '时间', dataIndex: 'createdAt', key: 'createdAt' },
]

async function fetchData() {
  loading.value = true
  try {
    const res = await getOperateLogPage(pagination.current, pagination.pageSize, filterModule.value, filterAction.value)
    const data = (res as any).data
    dataList.value = data?.records || []
    pagination.total = data?.total || 0
  } finally { loading.value = false }
}

function handleTableChange(pag: any) { pagination.current = pag.current; pagination.pageSize = pag.pageSize; fetchData() }

onMounted(fetchData)
</script>
