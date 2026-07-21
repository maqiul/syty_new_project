<template>
  <div>
    <a-card>
      <template #extra>
        <a-button type="primary" @click="showModal()">新增菜单</a-button>
      </template>

      <a-table :columns="columns" :data-source="menuTree" :loading="loading" row-key="id" :pagination="false" default-expand-all-rows :children-column-name="null">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'menuType'">
            <a-tag :color="record.menuType === 'DIRECTORY' ? 'blue' : record.menuType === 'MENU' ? 'green' : 'orange'">
              {{ record.menuType === 'DIRECTORY' ? '目录' : record.menuType === 'MENU' ? '菜单' : '按钮' }}
            </a-tag>
          </template>
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

    <a-modal v-model:open="modalVisible" :title="editingRecord ? '编辑菜单' : '新增菜单'" @ok="handleSubmit" :confirm-loading="submitting">
      <a-form :model="formData" layout="vertical">
        <a-form-item label="菜单类型">
          <a-select v-model:value="formData.menuType">
            <a-select-option value="DIRECTORY">目录</a-select-option>
            <a-select-option value="MENU">菜单</a-select-option>
            <a-select-option value="BUTTON">按钮</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="菜单名称" required>
          <a-input v-model:value="formData.menuName" placeholder="菜单名称" />
        </a-form-item>
        <a-form-item label="路径">
          <a-input v-model:value="formData.path" placeholder="如 /system/user" />
        </a-form-item>
        <a-form-item label="图标">
          <a-input v-model:value="formData.icon" placeholder="如 DashboardOutlined" />
        </a-form-item>
        <a-form-item label="权限标识">
          <a-input v-model:value="formData.permission" placeholder="如 system:user:page" />
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
import { getAllMenus, addMenu, updateMenu, deleteMenu } from '@/api/platform/menu'

const loading = ref(false)
const submitting = ref(false)
const menuTree = ref<any[]>([])
const modalVisible = ref(false)
const editingRecord = ref<any>(null)

const columns = [
  { title: '菜单名称', dataIndex: 'menuName', key: 'menuName' },
  { title: '类型', key: 'menuType' },
  { title: '路径', dataIndex: 'path', key: 'path' },
  { title: '权限标识', dataIndex: 'permission', key: 'permission' },
  { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder' },
  { title: '操作', key: 'action', width: 150 },
]

const formData = reactive({ menuType: 'MENU', menuName: '', path: '', icon: '', permission: '', sortOrder: 0, parentId: 0 })

async function fetchData() {
  loading.value = true
  try { menuTree.value = (await getAllMenus() as any).data || [] }
  finally { loading.value = false }
}

function showModal(record?: any) {
  editingRecord.value = record || null
  if (record) { Object.assign(formData, { menuType: record.menuType, menuName: record.menuName, path: record.path, icon: record.icon || '', permission: record.permission || '', sortOrder: record.sortOrder || 0, parentId: record.parentId || 0 }) }
  else { Object.assign(formData, { menuType: 'MENU', menuName: '', path: '', icon: '', permission: '', sortOrder: 0, parentId: 0 }) }
  modalVisible.value = true
}

async function handleSubmit() {
  submitting.value = true
  try {
    if (editingRecord.value) { await updateMenu(editingRecord.value.id, formData); message.success('修改成功') }
    else { await addMenu(formData); message.success('新增成功') }
    modalVisible.value = false; fetchData()
  } finally { submitting.value = false }
}

async function handleDelete(id: number) { await deleteMenu(id); message.success('删除成功'); fetchData() }

onMounted(fetchData)
</script>
