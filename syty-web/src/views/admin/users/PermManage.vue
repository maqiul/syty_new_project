<template>
  <div class="page-container">
    <a-card title="权限管理" :bordered="false">
      <template #extra>
        <a-button type="primary" @click="handleAdd">
          <PlusOutlined /> 新增权限
        </a-button>
      </template>

      <a-table
        :columns="columns"
        :dataSource="permTree"
        :loading="loading"
        :pagination="false"
        rowKey="id"
        :defaultExpandAllRows="true"
        :expandable="{ childrenColumnName: 'children' }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'type'">
            <a-tag :color="typeColor(record.type)">{{ typeLabel(record.type) }}</a-tag>
          </template>
          <template v-if="column.key === 'menuId'">
            {{ getMenuName(record.menuId) }}
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button size="small" @click="handleEdit(record)">编辑</a-button>
              <a-popconfirm title="确定删除？子权限将一并删除" @confirm="handleDelete(record.id)">
                <a-button size="small" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新增/编辑弹窗 -->
    <a-modal
      :title="editForm.id ? '编辑权限' : '新增权限'"
      :visible="modalVisible"
      :confirmLoading="saving"
      @ok="handleSave"
      @cancel="modalVisible = false"
      width="520px"
    >
      <a-form :model="editForm" layout="vertical">
        <a-form-item label="父权限">
          <a-tree-select
            v-model:value="editForm.parentCode"
            :treeData="parentOptions"
            placeholder="留空则为顶级权限"
            allowClear
            treeDefaultExpandAll
            :fieldNames="{ label: 'title', value: 'value', children: 'children' }"
          />
        </a-form-item>
        <a-form-item label="权限编码" required>
          <a-input v-model:value="editForm.code" placeholder="user:create" />
        </a-form-item>
        <a-form-item label="权限名称" required>
          <a-input v-model:value="editForm.name" placeholder="用户新增" />
        </a-form-item>
        <a-form-item label="所属菜单">
          <a-tree-select
            v-model:value="editForm.menuId"
            :treeData="menuTree"
            placeholder="选择菜单"
            allowClear
            treeDefaultExpandAll
            :fieldNames="{ label: 'name', value: 'id', children: 'children' }"
          />
        </a-form-item>
        <a-form-item label="类型">
          <a-radio-group v-model:value="editForm.type">
            <a-radio value="menu">菜单</a-radio>
            <a-radio value="button">按钮</a-radio>
            <a-radio value="api">接口</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="排序">
          <a-input-number v-model:value="editForm.sortOrder" :min="0" style="width:100%" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { message } from 'antdv-next'
import { PlusOutlined } from '@antdv-next/icons'
import request from '@/utils/axios'

interface Permission {
  id?: number
  code: string
  name: string
  parentCode?: string
  menuId?: number
  type: string
  sortOrder: number
  children?: Permission[]
}

interface MenuItem { id: number; name: string; path: string; children?: MenuItem[] }

const loading = ref(false)
const permTree = ref<Permission[]>([])
const modalVisible = ref(false)
const saving = ref(false)
const menuTree = ref<MenuItem[]>([])

const defaultForm = (): Permission => ({ code: '', name: '', parentCode: undefined, menuId: undefined, type: 'api', sortOrder: 0 })
const editForm = ref<Permission>(defaultForm())

const columns = [
  { title: '权限名称', dataIndex: 'name', width: 200 },
  { title: '权限编码', dataIndex: 'code', width: 240 },
  { title: '类型', key: 'type', width: 80 },
  { title: '关联菜单', key: 'menuId', width: 150 },
  { title: '排序', dataIndex: 'sortOrder', width: 60 },
  { title: '操作', key: 'action', width: 140 }
]

const typeColor = (t: string) => ({ menu: 'green', button: 'blue', api: 'orange' }[t] || 'default')
const typeLabel = (t: string) => ({ menu: '菜单', button: '按钮', api: '接口' }[t] || t)

/** 将菜单树拍平为 id→name 映射 */
function flattenMenu(items: MenuItem[]): Map<number, string> {
  const map = new Map<number, string>()
  const walk = (list: MenuItem[]) => {
    for (const m of list) {
      map.set(m.id, m.name)
      if (m.children) walk(m.children)
    }
  }
  walk(items)
  return map
}

let menuNameMap = new Map<number, string>()
const getMenuName = (id?: number) => (id ? menuNameMap.get(id) || `#${id}` : '-')

/** 将权限树转为 parentCode 下拉选项 */
function permTreeToOptions(tree: Permission[]): any[] {
  return tree.map(p => ({
    title: `${p.name} (${p.code})`,
    value: p.code,
    children: p.children ? permTreeToOptions(p.children) : undefined
  }))
}

const parentOptions = computed(() => permTreeToOptions(permTree.value))

const fetchData = async () => {
  loading.value = true
  try {
    const res = await request.get('/permission/tree')
    permTree.value = res.data || []
  } finally { loading.value = false }
}

const fetchMenus = async () => {
  try {
    const res = await request.get('/menu/all')
    menuTree.value = res.data || []
    menuNameMap = flattenMenu(menuTree.value)
  } catch { /* ignore */ }
}

const handleAdd = () => {
  editForm.value = defaultForm()
  modalVisible.value = true
}

const handleEdit = (record: Permission) => {
  editForm.value = {
    id: record.id,
    code: record.code,
    name: record.name,
    parentCode: record.parentCode || undefined,
    menuId: record.menuId,
    type: record.type,
    sortOrder: record.sortOrder
  }
  modalVisible.value = true
}

const handleSave = async () => {
  if (!editForm.value.code || !editForm.value.name) {
    message.warning('请填写权限编码和名称')
    return
  }
  saving.value = true
  try {
    if (editForm.value.id) {
      await request.put(`/permission/${editForm.value.id}`, editForm.value)
    } else {
      await request.post('/permission', editForm.value)
    }
    message.success(editForm.value.id ? '更新成功' : '新增成功')
    modalVisible.value = false
    fetchData()
  } finally { saving.value = false }
}

const handleDelete = async (id: number) => {
  await request.delete(`/permission/${id}`)
  message.success('删除成功')
  fetchData()
}

onMounted(() => {
  fetchData()
  fetchMenus()
})
</script>
