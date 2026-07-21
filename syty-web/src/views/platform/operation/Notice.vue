<template>
  <div class="operation-notice">
    <a-card :bordered="false">
      <template #title>
        <div class="card-header">
          <span>运营通知管理</span>
          <a-button type="primary" @click="handleAdd"><PlusOutlined /> 发布公告</a-button>
        </div>
      </template>

      <a-table :columns="columns" :dataSource="list" :loading="loading" rowKey="id">
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'status'">
            <a-tag :color="record.status === 'PUBLISHED' ? 'green' : 'orange'">
              {{ record.status === 'PUBLISHED' ? '已发布' : '草稿' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-button v-if="record.status === 'DRAFT'" type="link" size="small" @click="handlePublish(record)">发布</a-button>
            <a-button type="link" size="small" danger>删除</a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="visible" title="发布新公告" @ok="handleSave" :confirmLoading="saving">
      <a-form layout="vertical">
        <a-form-item label="标题" required><a-input v-model:value="form.title" /></a-form-item>
        <a-form-item label="内容" required><a-textarea v-model:value="form.content" :rows="4" /></a-form-item>
        <a-form-item label="类型">
          <a-select v-model:value="form.noticeType">
            <a-select-option value="ANNOUNCEMENT">普通公告</a-select-option>
            <a-select-option value="MAINTENANCE">维护通知</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { PlusOutlined } from '@antdv-next/icons'
import { message } from 'antdv-next'
import request from '@/utils/axios'

const list = ref([])
const loading = ref(false)
const visible = ref(false)
const saving = ref(false)
const form = reactive({ title: '', content: '', noticeType: 'ANNOUNCEMENT' })

const columns = [
  { title: '标题', dataIndex: 'title', key: 'title' },
  { title: '类型', dataIndex: 'noticeType', key: 'noticeType', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '时间', dataIndex: 'createdAt', key: 'createdAt', width: 180 },
  { title: '操作', key: 'action', width: 150 },
]

const fetchList = async () => {
  loading.value = true
  try {
    const res = await request.get('/platform/notices')
    list.value = res.data.records
  } finally { loading.value = false }
}

const handleAdd = () => { Object.assign(form, { title: '', content: '', noticeType: 'ANNOUNCEMENT' }); visible.value = true }

const handleSave = async () => {
  if (!form.title || !form.content) return message.error('请填写完整')
  saving.value = true
  try {
    await request.post('/platform/notices', form)
    message.success('发布成功')
    visible.value = false
    fetchList()
  } finally { saving.value = false }
}

const handlePublish = async (record: any) => {
  try {
    await request.put(`/platform/notices/${record.id}/publish`)
    message.success('已发布')
    fetchList()
  } catch {}
}

onMounted(fetchList)
</script>
