<template>
  <div>
    <a-card>
      <template #title>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>角色管理</span>
          <a-button type="primary" @click="openDialog()">
            <template #icon><PlusOutlined /></template>
            新增角色
          </a-button>
        </div>
      </template>

      <div style="margin-bottom: 16px">
        <a-space>
          <span>角色名：</span>
          <a-input v-model:value="query.roleName" placeholder="请输入角色名" allow-clear style="width:200px" />
          <a-button type="primary" @click="fetchData">搜索</a-button>
        </a-space>
      </div>

      <a-table :columns="columns" :dataSource="tableData" :loading="loading" rowKey="id">
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'status'">
            <a-tag :color="record.status === 1 ? 'green' : 'red'">{{ record.status === 1 ? '启用' : '禁用' }}</a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a type="primary" @click="openDialog(record)">编辑</a>
              <a-divider type="vertical" />
              <a-popconfirm title="确定删除吗？" @confirm="handleDelete(record.id)">
                <a style="color: red">删除</a>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="dialogVisible" :title="form.id ? '编辑角色' : '新增角色'" @ok="handleSave" :confirmLoading="saving">
      <a-form :model="form" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="角色编码" name="roleCode" :rules="[{ required: true, message: '请输入角色编码' }]">
          <a-input v-model:value="form.roleCode" :disabled="!!form.id" />
        </a-form-item>
        <a-form-item label="角色名称" name="roleName" :rules="[{ required: true, message: '请输入角色名称' }]">
          <a-input v-model:value="form.roleName" />
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model:value="form.description" />
        </a-form-item>
        <a-form-item label="状态">
          <a-switch v-model:checked="form.status" :checkedValue="1" :unCheckedValue="0" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import request from '@/utils/axios'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const tableData = ref<any[]>([])

const query = reactive({ roleName: '' })
const form = reactive<any>({ id: undefined, roleCode: '', roleName: '', description: '', status: 1 })

const columns = [
  { title: 'ID', dataIndex: 'id', width: 80 },
  { title: '角色编码', dataIndex: 'roleCode', width: 150 },
  { title: '角色名称', dataIndex: 'roleName', width: 150 },
  { title: '备注', dataIndex: 'description' },
  { title: '状态', dataIndex: 'status', width: 80 },
  { title: '操作', key: 'action', width: 150 }
]

const fetchData = async () => {
  loading.value = true
  try {
    const res = await request.get('/role/list')
    tableData.value = res.data || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const openDialog = (record?: any) => {
  if (record) {
    Object.assign(form, record)
  } else {
    Object.assign(form, { id: undefined, roleCode: '', roleName: '', description: '', status: 1 })
  }
  dialogVisible.value = true
}

const handleSave = async () => {
  saving.value = true
  try {
    const url = form.id ? '/v1/system/role/update' : '/v1/system/role/add'
    await request.post(url, form)
    message.success('保存成功')
    dialogVisible.value = false
    fetchData()
  } finally {
    saving.value = false
  }
}

const handleDelete = async (id: number) => {
  try {
    await request.post(`/v1/system/role/delete?id=${id}`)
    message.success('删除成功')
    fetchData()
  } catch (e) {
    console.error(e)
  }
}

onMounted(() => fetchData())
</script>
