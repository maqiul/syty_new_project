<template>
  <div>
    <a-card>
      <template #title>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>用户管理</span>
          <a-button type="primary" @click="openDialog()">
            <template #icon><PlusOutlined /></template>
            新增用户
          </a-button>
        </div>
      </template>

      <div style="margin-bottom: 16px">
        <a-space>
          <span v-if="userStore.isSuperAdmin">
            租户：
            <a-select v-model:value="query.tenantId" placeholder="全部租户" allow-clear style="width:180px">
              <a-select-option v-for="t in tenantList" :key="t.id" :value="t.id">{{ t.name }}</a-select-option>
            </a-select>
          </span>
          <span>关键字：</span>
          <a-input v-model:value="query.keyword" placeholder="用户名/姓名/手机号" allow-clear style="width:200px" @pressEnter="search" />
          <a-button type="primary" @click="search">搜索</a-button>
          <a-button @click="resetSearch">重置</a-button>
        </a-space>
      </div>

      <a-table :columns="columns" :dataSource="tableData" :loading="loading" :pagination="pagination" rowKey="id" @change="handleTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'role'">
            <a-tag :color="roleTag(record.role)">{{ roleText(record.role) }}</a-tag>
          </template>
          <template v-if="column.dataIndex === 'status'">
            <a-tag :color="record.status === 1 ? 'green' : 'red'">{{ record.status === 1 ? '启用' : '禁用' }}</a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a type="primary" @click="openDialog(record)">编辑</a>
              <a-divider type="vertical" />
              <a @click="handleReset(record.username)">重置密码</a>
              <a-divider type="vertical" />
              <a type="danger" @click="handleDelete(record.id)">删除</a>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="dialogVisible" :title="dialogForm.id ? '编辑用户' : '新增用户'" width="500px" @ok="handleSave" :confirmLoading="saving" @cancel="dialogVisible = false">
      <a-form :model="dialogForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }" ref="formRef">
        <a-form-item label="用户名" name="username" :rules="[{ required: true, message: '请输入用户名' }]">
          <a-input v-model:value="dialogForm.username" />
        </a-form-item>
        <a-form-item label="密码" :name="dialogForm.id ? undefined : 'password'" :rules="dialogForm.id ? [] : [{ required: true, message: '请输入密码' }]">
          <a-input-password v-model:value="dialogForm.password" :placeholder="dialogForm.id ? '留空不修改' : '请输入密码'" />
        </a-form-item>
        <a-form-item label="姓名" name="realName" :rules="[{ required: true, message: '请输入姓名' }]">
          <a-input v-model:value="dialogForm.realName" />
        </a-form-item>
        <a-form-item label="手机号">
          <a-input v-model:value="dialogForm.phone" />
        </a-form-item>
        <a-form-item label="角色" name="role" :rules="[{ required: true, message: '请选择角色' }]">
          <a-select v-model:value="dialogForm.role" style="width:100%">
            <a-select-option value="TENANT_ADMIN">租户管理员</a-select-option>
            <a-select-option value="STAFF">店员</a-select-option>
            <a-select-option v-if="userStore.isSuperAdmin" value="SUPER_ADMIN">超级管理员</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="所属租户" name="tenantId" :rules="[{ required: true, message: '请选择租户' }]">
          <a-select v-model:value="dialogForm.tenantId" style="width:100%">
            <a-select-option v-for="t in tenantList" :key="t.id" :value="t.id">{{ t.name }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="状态">
          <a-switch v-model:checked="dialogForm.status" :checkedValue="1" :unCheckedValue="0" checkedChildren="启用" unCheckedChildren="禁用" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { getUserPage, addUser, updateUser, deleteUser, getTenantList, resetPassword, type SysUser, type Tenant } from '@/api'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const loading = ref(false)
const tableData = ref<SysUser[]>([])
const tenantList = ref<Tenant[]>([])
const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref()

const query = reactive({ page: 1, size: 20, keyword: '', tenantId: undefined as number | undefined })
const total = ref(0)
const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showTotal: (t: number) => `共 ${t} 条`
})

const dialogForm = reactive<SysUser>({
  id: undefined, username: '', password: '', realName: '', phone: '',
  role: 'STAFF', tenantId: undefined, status: 1
})

const columns = [
  { title: 'ID', dataIndex: 'id', width: 60 },
  { title: '用户名', dataIndex: 'username', width: 120 },
  { title: '姓名', dataIndex: 'realName', width: 120 },
  { title: '手机号', dataIndex: 'phone', width: 150 },
  { title: '角色', dataIndex: 'role', width: 150 },
  { title: '状态', dataIndex: 'status', width: 80 },
  { title: '操作', key: 'action', width: 180 }
]

const roleTag = (role: string) => {
  const map: Record<string, string> = { SUPER_ADMIN: 'red', TENANT_ADMIN: 'blue', STAFF: 'green' }
  return map[role] || 'default'
}
const roleText = (role: string) => {
  const map: Record<string, string> = { SUPER_ADMIN: '超级管理员', TENANT_ADMIN: '租户管理员', STAFF: '店员' }
  return map[role] || role
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getUserPage(query)
    tableData.value = res.data.records || res.data
    total.value = res.data.total || 0
    pagination.total = total.value
  } finally {
    loading.value = false
  }
}

const handleTableChange = (pag: any) => {
  query.page = pag.current
  query.size = pag.pageSize
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchData()
}

const search = () => { query.page = 1; fetchData() }
const resetSearch = () => { query.page = 1; query.keyword = ''; query.tenantId = undefined; fetchData() }

const handleReset = async (username: string) => {
  Modal.confirm({
    title: '确认重置密码',
    content: `是否重置用户「${username}」的密码？\n重置后的密码将为：${username}@123!@#`,
    okText: '确认重置',
    cancelText: '取消',
    onOk: async () => {
      try {
        const res = await resetPassword({ username })
        message.success(`密码已重置，新密码：${res.data.newPassword}`)
      } catch { }
    }
  })
}

const openDialog = (row?: SysUser) => {
  if (row) {
    Object.assign(dialogForm, {
      id: row.id, username: row.username, password: '',
      realName: row.realName, phone: row.phone || '',
      role: row.role, tenantId: row.tenantId, status: row.status
    })
  } else {
    Object.assign(dialogForm, { id: undefined, username: '', password: '', realName: '', phone: '', role: 'STAFF', tenantId: undefined, status: 1 })
  }
  dialogVisible.value = true
}

const handleSave = async () => {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    if (dialogForm.id) {
      await updateUser(dialogForm)
      message.success('更新成功')
    } else {
      await addUser(dialogForm)
      message.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } finally {
    saving.value = false
  }
}

const handleDelete = (id: number) => {
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除该用户吗？',
    okText: '确定',
    cancelText: '取消',
    onOk: async () => {
      await deleteUser(id)
      message.success('删除成功')
      fetchData()
    }
  })
}

onMounted(async () => {
  if (userStore.isSuperAdmin) {
    const res = await getTenantList()
    tenantList.value = res.data || []
  }
  fetchData()
})
</script>
