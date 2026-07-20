<template>
  <div>
    <a-card>
      <template #title>
        <div style="display: flex; justify-content: space-between">
          <span>🏸 羽毛球大赛记录</span>
          <a-button type="primary" @click="$router.push('/tournament')">新建登记</a-button>
        </div>
      </template>
      <a-table :columns="columns" :dataSource="tableData" :loading="loading" :pagination="pagination" rowKey="id" @change="handleTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 1 ? 'green' : 'orange'">{{ record.status === 1 ? '已完成' : '待穿' }}</a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a @click="openDetail(record)">详情</a>
              <a v-if="record.status === 0" @click="completeOrder(record.id)">完成</a>
              <a style="color:red" @click="handleDelete(record.id)">删除</a>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="detailVisible" title="比赛详情" width="500px" @cancel="detailVisible = false">
      <a-descriptions v-if="detail" :column="2" bordered size="small">
        <a-descriptions-item label="比赛名称" :span="2">{{ detail.matchName || '-' }}</a-descriptions-item>
        <a-descriptions-item label="比赛日期">{{ detail.matchDate || '-' }}</a-descriptions-item>
        <a-descriptions-item label="球员">{{ detail.playerName || ('#' + detail.playerId) }}</a-descriptions-item>
        <a-descriptions-item label="对手">{{ detail.opponentName || '-' }}</a-descriptions-item>
        <a-descriptions-item label="比分">{{ detail.matchResult || '-' }}</a-descriptions-item>
        <a-descriptions-item label="订单号">{{ detail.orderNo }}</a-descriptions-item>
        <a-descriptions-item label="备注" :span="2">{{ detail.remark || '-' }}</a-descriptions-item>
        <a-descriptions-item label="穿线需求" :span="2" v-if="detail.racketBrand || detail.stringType">
          {{ [detail.racketBrand, detail.racketModel, detail.stringType, detail.mainTension + '/' + detail.crossTension + 'lbs'].filter(Boolean).join(' | ') || '-' }}
        </a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'antdv-next'
import { getBadmintonTournamentPage, completeBadmintonTournamentOrder, deleteBadmintonTournamentOrder } from '@/api'

const loading = ref(false); const tableData = ref<any[]>([]); const detailVisible = ref(false); const detail = ref<any>(null)
const pagination = reactive({ current: 1, pageSize: 20, total: 0, showSizeChanger: true })

const columns = [
  { title: '订单号', dataIndex: 'orderNo', width: 150 },
  { title: '比赛名称', dataIndex: 'matchName', ellipsis: true },
  { title: '比赛日期', dataIndex: 'matchDate', width: 150 },
  { title: '球员', key: 'player', render: ({ record }: any) => record?.playerName || `#${record?.playerId || '--'}` },
  { title: '对手', dataIndex: 'opponentName' },
  { title: '比分', dataIndex: 'matchResult', width: 130 },
  { title: '状态', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 160 }
]

const loadData = async () => {
  loading.value = true
  try {
    const r: any = await getBadmintonTournamentPage({ page: pagination.current, size: pagination.pageSize })
    tableData.value = (r.data as any)?.records || []; pagination.total = (r.data as any)?.total || 0
  } finally { loading.value = false }
}
const handleTableChange = (p: any) => { pagination.current = p.current; pagination.pageSize = p.pageSize; loadData() }
const openDetail = (row: any) => { detail.value = row; detailVisible.value = true }
const completeOrder = async (id: number) => { await completeBadmintonTournamentOrder(id); loadData(); message.success('已完成') }
const handleDelete = (id: number) => { Modal.confirm({ title: '确认删除？', onOk: async () => { await deleteBadmintonTournamentOrder(id); loadData(); message.success('已删除') } }) }
onMounted(loadData)
</script>
