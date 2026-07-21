<template>
  <div>
    <a-card title="权限管理">
      <template #extra>
        <a-button type="primary" @click="showModal()">新增权限</a-button>
      </template>

      <a-table :columns="columns" :data-source="permTree" :loading="loading" row-key="id" :pagination="false" default-expand-all-rows :children-column-name="null">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'action'">
            <a-space>
              <a @click="showModal(record)">编辑</a>
              <a-popconfirm title="确定删除？" @confirm="handleDelete(record.id)">
                <a style="color: #ff4d4f;">删除</a>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="modalVisible" :title="editingRecord ? '编辑权限' : '新增权限'" @ok="handleSubmit" :confirm-loading="submitting">
      <a-form :model="formData" layout="vertical">
        <a-form-item label="权限名称" required>
          <a-input v-model:value="formData.name" placeholder="权限名称" />
        </a-form-item>
        <a-form-item label="权限编码" required>
          <a-input v-model:value="formData.code" placeholder="如 system:user:create" />
        </a-form-item>
        <a-form-item label="父级编码">
          <a-input v-model:value="formData.parentCode" placeholder="留空为顶级" />
        </a-form-item>
        <a-form-item label="排序">
          <a-input-number v-model:value="formData.sortOrder" :min="0" style="width: 100%;" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'antdv-next'
import { getPermissionTree, addPermission, updatePermission, deletePermission } from '@/api/platform/permission'

const loading = ref(false)
const submitting = ref(false)
const permTree = ref<any[]>([])
const modalVisible = ref(false)
const editingRecord = ref<any>(null)

const columns = [
  { title: '权限名称', dataIndex: 'name', key: 'name' },
  { title: '权限编码', dataIndex: 'code', key: 'code' },
  { title: '父级编码', dataIndex: 'parentCode', key: 'parentCode' },
  { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder' },
  { title: '操作', key: 'action', width: 150 },
]

const formData = reactive({ name: '', code: '', parentCode: '', sortOrder: 0 })

async function fetchData() {
  loading.value = true
  try { permTree.value = (await getPermissionTree() as any).data || [] }
  finally { loading.value = false }
}

function showModal(record?: any) {
  editingRecord.value = record || null
  if (record) { Object.assign(formData, { name: record.name, code: record.code, parentCode: record.parentCode || '', sortOrder: record.sortOrder || 0 }) }
  else { Object.assign(formData, { name: '', code: '', parentCode: '', sortOrder: 0 }) }
  modalVisible.value = true
}

async function handleSubmit() {
  submitting.value = true
  try {
    if (editingRecord.value) { await updatePermission(editingRecord.value.id, formData); message.success('修改成功') }
    else { await addPermission(formData); message.success('新增成功') }
    modalVisible.value = false; fetchData()
  } finally { submitting.value = false }
}

async function handleDelete(id: number) { await deletePermission(id); message.success('删除成功'); fetchData() }

onMounted(fetchData)
</script>
