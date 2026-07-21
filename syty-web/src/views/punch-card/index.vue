<template>
  <div class="punch-card-container">
    <div class="header-actions">
      <a-button type="primary" @click="showIssueModal">🎫 新增售卡</a-button>
    </div>

    <a-table
      :columns="columns"
      :data-source="cards"
      :loading="loading"
      :pagination="pagination"
      row-key="id"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-tag :color="statusColorMap[record.status]">{{ statusTextMap[record.status] }}</a-tag>
        </template>
        <template v-if="column.key === 'remainingCount'">
          <span :style="{ color: record.remainingCount === 0 ? 'red' : 'green', fontWeight: 'bold' }">
            {{ record.remainingCount }} / {{ record.totalCount }}
          </span>
        </template>
      </template>
    </a-table>

    <a-modal
      v-model:visible="issueVisible"
      title="新增售卡"
      @ok="handleIssue"
      :confirmLoading="issuing"
    >
      <a-form layout="vertical">
        <a-form-item label="会员手机号" required>
          <a-input v-model:value="issueForm.phone" placeholder="请输入手机号" />
        </a-form-item>
        <a-form-item label="卡种" required>
          <a-select v-model:value="issueForm.cardType">
            <a-select-option value="TEN_TIMES">10 次卡</a-select-option>
            <a-select-option value="TWENTY_TIMES">20 次卡</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="有效期（天）">
          <a-input-number v-model:value="issueForm.validDays" :min="1" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'antdv-next'
import { issueCard, getCardList } from '@/api/punchCard'

const cards = ref<any[]>([])
const loading = ref(false)
const issueVisible = ref(false)
const issuing = ref(false)

const pagination = { current: 1, pageSize: 10, showSizeChanger: true }

const columns = [
  { title: '卡号', dataIndex: 'id', key: 'id' },
  { title: '手机号', dataIndex: 'phone', key: 'phone' },
  { title: '卡种', dataIndex: 'cardType', key: 'cardType' },
  { title: '剩余/总次数', key: 'remainingCount' },
  { title: '过期时间', dataIndex: 'expireTime', key: 'expireTime' },
  { title: '状态', key: 'status' },
]

const statusColorMap: Record<number, string> = { 1: 'green', 2: 'gray', 3: 'orange', 4: 'red' }
const statusTextMap: Record<number, string> = { 1: '正常', 2: '已用完', 3: '已过期', 4: '已退款' }

const issueForm = reactive({ phone: '', cardType: 'TEN_TIMES', validDays: 90 })

const fetchCards = async () => {
  loading.value = true
  try {
    const res = await getCardList()
    if (res.code === 0) cards.value = res.data || []
  } finally {
    loading.value = false
  }
}

const showIssueModal = () => {
  issueVisible.value = true
  issueForm.phone = ''
  issueForm.cardType = 'TEN_TIMES'
  issueForm.validDays = 90
}

const handleIssue = async () => {
  if (!issueForm.phone) {
    message.error('请输入手机号')
    return
  }
  issuing.value = true
  try {
    const res = await issueCard(issueForm)
    if (res.code === 0) {
      message.success('发卡成功')
      issueVisible.value = false
      fetchCards()
    } else {
      message.error(res.message || '发卡失败')
    }
  } finally {
    issuing.value = false
  }
}

onMounted(fetchCards)
</script>

<style scoped>
.punch-card-container { padding: 20px; background: #fff; min-height: 100vh; }
.header-actions { margin-bottom: 16px; }
</style>
