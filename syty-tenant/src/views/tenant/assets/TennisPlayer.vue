<template>
  <div>
    <a-card>
      <template #title>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>🎾 网球球员管理</span>
          <a-button type="primary" @click="openDialog()"><template #icon><PlusOutlined /></template>新增球员</a-button>
        </div>
      </template>

      <a-table :columns="columns" :dataSource="tableData" :loading="loading" :pagination="pagination" rowKey="id" @change="handleTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'photoUrl'">
            <img v-if="record.photoUrl" :src="`/api/file/view/${record.photoUrl.split('/').pop()}`" style="width:40px;height:40px;border-radius:50%;object-fit:cover" />
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a @click="openDialog(record)">编辑</a>
              <a-divider type="vertical" />
              <a style="color:red" @click="handleDelete(record.id)">删除</a>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="dialogVisible" :title="dialogForm.id ? '编辑球员' : '新增球员'" width="500px" @ok="handleSave" :confirmLoading="saving">
      <a-form :model="dialogForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="姓名" required><a-input v-model:value="dialogForm.name" /></a-form-item>
        <a-form-item label="手机号"><a-input v-model:value="dialogForm.phone" /></a-form-item>
        <a-form-item label="水平等级"><a-input v-model:value="dialogForm.skillLevel" placeholder="UTR / NTRP" /></a-form-item>
        <a-form-item label="近期战绩"><a-textarea v-model:value="dialogForm.progressNotes" :rows="3" placeholder="近期比赛/进步记录" /></a-form-item>
        <a-form-item label="照片">
          <a-upload v-model:file-list="fileList" list-type="picture-card" :max-count="1" :custom-request="customUpload" :before-upload="beforeUpload">
            <div v-if="fileList.length < 1"><plus-outlined /><div style="margin-top:8px">上传</div></div>
          </a-upload>
        </a-form-item>
        <a-form-item label="备注"><a-textarea v-model:value="dialogForm.remark" :rows="2" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message, Modal, Upload } from 'antdv-next'
import { PlusOutlined } from '@antdv-next/icons'
import { getTennisPlayerPage, addTennisPlayer, updateTennisPlayer, deleteTennisPlayer, uploadFile } from '@/api'

const loading = ref(false); const saving = ref(false); const dialogVisible = ref(false)
const tableData = ref<any[]>([]); const fileList = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0, showSizeChanger: true })

const dialogForm = reactive<any>({ id: null, name: '', phone: '', skillLevel: '', progressNotes: '', photoUrl: '', remark: '' })

const columns = [
  { title: '姓名', dataIndex: 'name', key: 'name' },
  { title: '手机号', dataIndex: 'phone', key: 'phone' },
  { title: '水平', dataIndex: 'skillLevel', key: 'skillLevel' },
  { title: '照片', key: 'photoUrl' },
  { title: '操作', key: 'action', width: 150 }
]

const loadData = async () => {
  loading.value = true
  const res: any = await getTennisPlayerPage({ page: pagination.current, size: pagination.pageSize })
  tableData.value = res.data.records || res.data.list || []
  pagination.total = res.data.total || 0
  loading.value = false
}

const handleTableChange = (pag: any) => { pagination.current = pag.current; pagination.pageSize = pag.pageSize; loadData() }

const openDialog = (record?: any) => {
  if (record) Object.assign(dialogForm, record)
  else Object.keys(dialogForm).forEach(k => (dialogForm as any)[k] = k === 'id' ? null : '')
  fileList.value = dialogForm.photoUrl ? [{ url: `/api/file/view/${dialogForm.photoUrl.split('/').pop()}`, status: 'done' }] : []
  dialogVisible.value = true
}

const beforeUpload = (file: File) => {
  const isImg = file.type.startsWith('image/')
  if (!isImg) message.error('只能上传图片!')
  return (isImg && file.size / 1024 / 1024 < 2) || Upload.LIST_IGNORE
}

const customUpload = async (options: any) => {
  const fd = new FormData(); fd.append('file', options.file)
  try { const r: any = await uploadFile(fd); if (r.data.code === 200) { dialogForm.photoUrl = r.data.data; options.onSuccess(r.data) } else options.onError(new Error('fail')) }
  catch (e) { options.onError(e) }
}

const handleSave = async () => {
  saving.value = true
  try {
    if (dialogForm.id) await updateTennisPlayer(dialogForm)
    else await addTennisPlayer(dialogForm)
    message.success('保存成功'); dialogVisible.value = false; loadData()
  } catch { message.error('保存失败') }
  finally { saving.value = false }
}

const handleDelete = (id: number) => {
  Modal.confirm({ title: '确认删除？', onOk: async () => { await deleteTennisPlayer(id); loadData(); message.success('已删除') } })
}

onMounted(loadData)
</script>
