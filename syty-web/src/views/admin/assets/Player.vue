<template>
  <div>
    <a-card>
      <template #title>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>球员管理</span>
          <a-button v-if="isSuperAdmin" type="primary" @click="openDialog()">
            <template #icon><PlusOutlined /></template>
            新增球员
          </a-button>
        </div>
      </template>

      <a-table :columns="columns" :dataSource="tableData" :loading="loading" :pagination="pagination" rowKey="id" @change="handleTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'action' && isSuperAdmin">
            <a-space>
              <a type="primary" @click="openDialog(record)">编辑</a>
              <a-divider type="vertical" />
              <a type="danger" @click="handleDelete(record.id)">删除</a>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="dialogVisible" :title="dialogForm.id ? '编辑球员' : '新增球员'" width="500px" @ok="handleSave" :confirmLoading="saving" @cancel="dialogVisible = false">
      <a-form :model="dialogForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }" ref="formRef">
        <a-form-item label="姓名" name="name" :rules="[{ required: true, message: '请输入姓名' }]">
          <a-input v-model:value="dialogForm.name" />
        </a-form-item>
        <a-form-item label="手机号">
          <a-input v-model:value="dialogForm.phone" />
        </a-form-item>
        <a-form-item label="性别">
          <a-select v-model:value="dialogForm.gender" allow-clear>
            <a-select-option value="男">男</a-select-option>
            <a-select-option value="女">女</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="水平等级">
          <a-input v-model:value="dialogForm.skillLevel" placeholder="国家一级 / 省队 / 业余高级" />
        </a-form-item>
        <a-form-item label="近期战绩">
          <a-textarea v-model:value="dialogForm.progressNotes" :rows="3" placeholder="近期比赛成绩、进步记录" />
        </a-form-item>
        <a-form-item label="照片">
          <a-upload v-model:file-list="fileList" list-type="picture-card" :max-count="1" :custom-request="customUpload" :before-upload="beforeUpload">
            <div v-if="fileList.length < 1"><plus-outlined /><div style="margin-top:8px">上传</div></div>
          </a-upload>
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model:value="dialogForm.remark" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message, Modal, Upload } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { getPlayerPage, addPlayer, updatePlayer, deletePlayer, uploadFile, type Player } from '@/api'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const isSuperAdmin = userStore.isSuperAdmin

const loading = ref(false)
const tableData = ref<Player[]>([])
const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref()
const fileList = ref<any[]>([])

const query = reactive({ page: 1, size: 20 })
const pagination = reactive({ current: 1, pageSize: 20, total: 0, showTotal: (t: number) => `共 ${t} 条` })

const dialogForm = reactive<any>({ id: undefined, name: '', phone: '', gender: '', level: '', skillLevel: '', progressNotes: '', photoUrl: '', remark: '' })

const columns = [
  { title: 'ID', dataIndex: 'id', width: 60 },
  { title: '姓名', dataIndex: 'name', width: 120 },
  { title: '手机号', dataIndex: 'phone', width: 130 },
  { title: '水平', dataIndex: 'skillLevel', width: 100 },
  { title: '备注', dataIndex: 'remark' },
  ...(isSuperAdmin ? [{ title: '操作', key: 'action', width: 180 }] : [])
]

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getPlayerPage(query)
    tableData.value = res.data.records || res.data
    pagination.total = res.data.total || 0
  } finally { loading.value = false }
}

const handleTableChange = (pag: any) => {
  query.page = pag.current; query.size = pag.pageSize
  pagination.current = pag.current; pagination.pageSize = pag.pageSize
  fetchData()
}

const openDialog = (row?: any) => {
  if (row) Object.assign(dialogForm, { id: row.id, name: row.name, phone: row.phone || '', gender: row.gender || '', level: row.level || '', skillLevel: row.skillLevel || '', progressNotes: row.progressNotes || '', photoUrl: row.photoUrl || '', remark: row.remark || '' })
  else Object.assign(dialogForm, { id: undefined, name: '', phone: '', gender: '', level: '', skillLevel: '', progressNotes: '', photoUrl: '', remark: '' })
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
  try {
    const res: any = await uploadFile(fd)
    if (res.data.code === 200) { dialogForm.photoUrl = res.data.data; options.onSuccess(res.data) }
    else options.onError(new Error('fail'))
  } catch (e) { options.onError(e) }
}

const handleSave = async () => {
  try { await formRef.value?.validate() } catch { return }
  saving.value = true
  try {
    if (dialogForm.id) { await updatePlayer(dialogForm); message.success('更新成功') }
    else { await addPlayer(dialogForm); message.success('创建成功') }
    dialogVisible.value = false; fetchData()
  } finally { saving.value = false }
}

const handleDelete = (id: number) => {
  Modal.confirm({ title: '确认删除', content: '确定要删除吗？', okText: '确定', cancelText: '取消',
    onOk: async () => { await deletePlayer(id); message.success('删除成功'); fetchData() }
  })
}

onMounted(() => fetchData())
</script>
