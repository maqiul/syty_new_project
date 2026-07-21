<template>
  <div class="page-container">
    <a-card title="菜单管理" :bordered="false">
      <template #extra>
        <a-button type="primary" @click="handleAdd">
          <PlusOutlined /> 新增菜单
        </a-button>
      </template>

      <a-table
        :columns="columns"
        :dataSource="treeData"
        :loading="loading"
        :pagination="false"
        rowKey="id"
        defaultExpandAllRows
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'icon'">
            <component :is="iconMap[record.icon]" v-if="record.icon && iconMap[record.icon]" />
            <span v-else>{{ record.icon || '-' }}</span>
          </template>
          <template v-if="column.key === 'type'">
            <a-tag :color="record.type === 0 ? 'blue' : 'green'">
              {{ record.type === 0 ? '目录' : '菜单' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button size="small" @click="handleEdit(record)">编辑</a-button>
              <a-popconfirm title="确定删除？" @confirm="handleDelete(record.id)">
                <a-button size="small" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新增/编辑弹窗 -->
    <a-modal
      :title="editForm.id ? '编辑菜单' : '新增菜单'"
      :visible="modalVisible"
      :confirmLoading="saving"
      @ok="handleSave"
      @cancel="modalVisible = false"
    >
      <a-form :model="editForm" layout="vertical">
        <a-form-item label="父级菜单">
          <a-tree-select
            v-model:value="editForm.parentId"
            :treeData="parentTree"
            placeholder="选择父级（留空为根目录）"
            allowClear
            treeDefaultExpandAll
            :fieldNames="{ label: 'name', value: 'id', children: 'children' }"
          />
        </a-form-item>
        <a-form-item label="名称" required>
          <a-input v-model:value="editForm.name" placeholder="菜单名称" />
        </a-form-item>
        <a-form-item label="类型">
          <a-radio-group v-model:value="editForm.type">
            <a-radio :value="0">目录</a-radio>
            <a-radio :value="1">菜单</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="路由路径" v-if="editForm.type === 1">
          <a-input v-model:value="editForm.path" placeholder="/user" />
        </a-form-item>
        <a-form-item label="组件名" v-if="editForm.type === 1">
          <a-input v-model:value="editForm.component" placeholder="User" />
        </a-form-item>
        <a-form-item label="图标">
          <a-input v-model:value="editForm.icon" placeholder="DashboardOutlined" />
        </a-form-item>
        <a-form-item label="权限码">
          <a-input v-model:value="editForm.permissionCode" placeholder="user:page" />
        </a-form-item>
        <a-form-item label="排序">
          <a-input-number v-model:value="editForm.sortOrder" :min="0" style="width: 100%" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { message } from 'antdv-next'
import { PlusOutlined } from '@antdv-next/icons'
import request from '@/utils/axios'
import {
  DashboardOutlined, TeamOutlined, AuditOutlined, PrinterOutlined,
  SettingOutlined, AppstoreOutlined, ShoppingOutlined, UnorderedListOutlined,
  TrophyOutlined, MenuOutlined, LockOutlined, SafetyOutlined
} from '@antdv-next/icons'

const iconMap: Record<string, any> = {
  DashboardOutlined, TeamOutlined, AuditOutlined, PrinterOutlined,
  SettingOutlined, AppstoreOutlined, ShoppingOutlined, UnorderedListOutlined,
  TrophyOutlined, MenuOutlined, LockOutlined, SafetyOutlined,
}

interface MenuItem { id?: number; parentId: number; name: string; path?: string; component?: string; icon?: string; permissionCode?: string; sortOrder: number; type: number; children?: MenuItem[] }

const loading = ref(false)
const saving = ref(false)
const modalVisible = ref(false)
const allMenus = ref<MenuItem[]>([])
const editForm = ref<MenuItem>({ parentId: 0, name: '', path: '', component: '', icon: '', permissionCode: '', sortOrder: 0, type: 1 })

const columns = [
  { title: '名称', dataIndex: 'name', key: 'name' },
  { title: '图标', key: 'icon', width: 60 },
  { title: '路径', dataIndex: 'path', key: 'path' },
  { title: '类型', key: 'type', width: 80 },
  { title: '权限码', dataIndex: 'permissionCode', key: 'permissionCode' },
  { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder', width: 60 },
  { title: '操作', key: 'action', width: 150 },
]

const treeData = computed(() => allMenus.value)

const parentTree = computed(() => {
  const add = (items: MenuItem[]): any[] => items.map(m => ({
    id: m.id, name: m.name, type: m.type,
    children: m.children ? add(m.children) : undefined,
    selectable: m.type === 0
  }))
  return [{ id: 0, name: '根目录', type: 0, children: add(allMenus.value) }]
})

async function loadMenus() {
  loading.value = true
  try {
    const res = await request.get('/menu/all')
    allMenus.value = res.data || []
  } finally {
    loading.value = false
  }
}

function handleAdd() {
  editForm.value = { parentId: 0, name: '', path: '', component: '', icon: '', permissionCode: '', sortOrder: 0, type: 1 }
  modalVisible.value = true
}

function handleEdit(record: MenuItem) {
  editForm.value = {
    id: record.id,
    parentId: record.parentId || 0,
    name: record.name,
    path: record.path || '',
    component: record.component || '',
    icon: record.icon || '',
    permissionCode: record.permissionCode || '',
    sortOrder: record.sortOrder || 0,
    type: record.type
  }
  modalVisible.value = true
}

async function handleSave() {
  if (!editForm.value.name) { message.warning('请输入名称'); return }
  saving.value = true
  try {
    if (editForm.value.id) {
      await request.put(`/menu/${editForm.value.id}`, editForm.value)
    } else {
      await request.post('/menu', editForm.value)
    }
    message.success('保存成功')
    modalVisible.value = false
    loadMenus()
  } finally {
    saving.value = false
  }
}

async function handleDelete(id: number) {
  await request.delete(`/menu/${id}`)
  message.success('删除成功')
  loadMenus()
}

onMounted(loadMenus)
</script>
