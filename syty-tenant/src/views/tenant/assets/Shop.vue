<template>
  <div>
    <a-card>
      <template #title>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>店铺管理</span>
          <a-button type="primary" @click="openDialog()">
            <template #icon><PlusOutlined /></template>
            新增店铺
          </a-button>
        </div>
      </template>

      <div style="margin-bottom: 16px">
        <a-space>
          <span>关键字：</span>
          <a-input v-model:value="query.keyword" placeholder="店铺名称/地址/电话" allow-clear style="width:240px" @pressEnter="search" />
          <a-button type="primary" @click="search">搜索</a-button>
          <a-button @click="resetSearch">重置</a-button>
        </a-space>
      </div>

      <a-table :columns="columns" :dataSource="tableData" :loading="loading" :pagination="pagination" rowKey="id" @change="handleTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'action'">
            <a-space>
              <a type="primary" @click="openDialog(record)">编辑</a>
              <a-divider type="vertical" />
              <a @click="$router.push(`/shop-string?shopId=${record.id}`)">球线</a>
              <a-divider type="vertical" />
              <a @click="openBindDialog(record)">绑定用户</a>
              <a-divider type="vertical" />
              <a @click="openQrDialog(record)">二维码</a>
              <a-divider type="vertical" />
              <a type="danger" @click="handleDelete(record.id)">删除</a>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新增/编辑 -->
    <a-modal v-model:open="dialogVisible" :title="dialogForm.id ? '编辑店铺' : '新增店铺'" width="520px" @ok="handleSave" :confirmLoading="saving" @cancel="dialogVisible = false">
      <a-form :model="dialogForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }" ref="formRef">
        <!-- 超管可见：租户选择器 -->
        <a-form-item v-if="isSuperAdmin && !dialogForm.id" label="所属租户" name="tenantId" :rules="[{ required: true, message: '请选择租户' }]">
          <a-select v-model:value="dialogForm.tenantId" placeholder="选择租户" style="width:100%">
            <a-select-option v-for="t in tenantList" :key="t.id" :value="t.id">{{ t.name }}（{{ t.code }}）</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="店铺名称" name="name" :rules="[{ required: true, message: '请输入店铺名称' }]">
          <a-input v-model:value="dialogForm.name" />
        </a-form-item>
        <a-form-item label="地址">
          <a-input v-model:value="dialogForm.address" />
        </a-form-item>
        <a-form-item label="联系电话">
          <a-input v-model:value="dialogForm.phone" />
        </a-form-item>
        <a-form-item label="联系人">
          <a-input v-model:value="dialogForm.contactPerson" />
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model:value="dialogForm.remark" :rows="2" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 绑定用户 -->
    <a-modal v-model:open="bindVisible" title="绑定用户" width="500px" @ok="handleBindSave" :confirmLoading="bindSaving">
      <p style="margin-bottom: 16px">店铺：<strong>{{ currentShop?.name }}</strong></p>
      <a-select v-model:value="selectedUserId" placeholder="选择要绑定的用户" style="width:100%">
        <a-select-option v-for="u in availableUsers" :key="u.id" :value="u.id">{{ u.realName || u.username }}（{{ u.role }}）</a-select-option>
      </a-select>
      <a-divider />
      <span>已绑定用户：</span>
      <a-tag v-for="u in boundUsers" :key="u.id" closable @close="handleUnbind(u.id)" style="margin: 4px">{{ u.realName || u.username }}</a-tag>
      <p v-if="boundUsers.length === 0" style="color: #999">暂无</p>
    </a-modal>

    <!-- 二维码 -->
    <a-modal v-model:open="qrVisible" title="客户自助登记二维码" width="380px" :footer="null">
      <div style="text-align:center">
        <canvas ref="qrCanvas" style="width:256px;height:256px" />
        <p style="margin-top:12px;color:#666">{{ currentShop?.name }}</p>
        <p style="color:#999;font-size:12px">扫码填写穿线登记表</p>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { message, Modal } from 'antdv-next'
import { PlusOutlined } from '@antdv-next/icons'
import QRCode from 'qrcode'
import { getShopPage, addShop, updateShop, deleteShop, getUserList, getShopUsers, bindUserToShop, unbindUserFromShop, getTenantList, type Shop, type SysUser, type Tenant } from '@/api'

const loading = ref(false)
const tableData = ref<Shop[]>([])
const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref()
const query = reactive({ page: 1, size: 20, keyword: '' })
const pagination = reactive<any>({ current: 1, pageSize: 20, total: 0, showTotal: (t: number) => `共 ${t} 条` })
const tenantList = ref<Tenant[]>([])

// 判断当前用户是否为超管
import { useUserStore } from '@/store/user'
const userStore = useUserStore()
const isSuperAdmin = computed(() => userStore.isSuperAdmin)

const dialogForm = reactive<Shop & { remark?: string }>({ id: undefined, name: '', address: '', phone: '', contactPerson: '', tenantId: undefined, remark: '' })

const columns = [
  { title: 'ID', dataIndex: 'id', width: 50 },
  { title: '所属租户', dataIndex: 'tenantName', width: 120 },
  { title: '店铺名称', dataIndex: 'name', width: 180 },
  { title: '地址', dataIndex: 'address', width: 200, ellipsis: true },
  { title: '联系电话', dataIndex: 'phone', width: 130 },
  { title: '操作', key: 'action', width: 'auto' }
]

// 绑定用户 state
const bindVisible = ref(false)
const bindSaving = ref(false)
const currentShop = ref<Shop>({} as Shop)
const availableUsers = ref<SysUser[]>([])
const boundUsers = ref<SysUser[]>([])
const selectedUserId = ref<number>()

// 二维码 state
const qrVisible = ref(false)
const qrCanvas = ref<HTMLCanvasElement>()
const baseUrl = `${window.location.protocol}//${window.location.host}`

const fetchData = async () => {
  loading.value = true
  try {
    const res: any = await getShopPage(query)
    tableData.value = res.data.records || res.data || []
    pagination.total = res.data.total || 0
  } finally { loading.value = false }
}

const handleTableChange = (pag: any) => {
  query.page = pag.current; query.size = pag.pageSize
  pagination.current = pag.current; pagination.pageSize = pag.pageSize
  fetchData()
}
const search = () => { query.page = 1; fetchData() }
const resetSearch = () => { query.page = 1; query.keyword = ''; fetchData() }

const openDialog = (row?: Shop) => {
  if (row) {
    Object.assign(dialogForm, {
      id: row.id, name: row.name, address: row.address || '',
      phone: row.phone || '', contactPerson: row.contactPerson || '',
      tenantId: row.tenantId, remark: (row as any).remark || ''
    })
  } else {
    Object.assign(dialogForm, {
      id: undefined, name: '', address: '', phone: '',
      contactPerson: '', tenantId: undefined, remark: ''
    })
  }
  // 非超管编辑时隐藏租户选择（租户不可变）
  dialogVisible.value = true
}

const handleSave = async () => {
  try { await formRef.value?.validate() } catch { return }
  saving.value = true
  try {
    if (dialogForm.id) {
      await updateShop(dialogForm as Shop)
    } else {
      await addShop(dialogForm as Shop)
    }
    message.success(dialogForm.id ? '更新成功' : '创建成功')
    dialogVisible.value = false; fetchData()
  } finally { saving.value = false }
}

const handleDelete = (id: number) => {
  Modal.confirm({
    title: '确认删除', content: '确定要删除该店铺吗？', okText: '确定',
    onOk: async () => { await deleteShop(id); message.success('删除成功'); fetchData() }
  })
}

// 绑定用户
const openBindDialog = async (shop: Shop) => {
  currentShop.value = shop
  bindVisible.value = true
  selectedUserId.value = undefined
  try {
    const [userRes, boundRes]: any[] = await Promise.all([getUserList(), getShopUsers(shop.id!)])
    const boundIds = ((boundRes as any).data || []).map((b: any) => typeof b === 'object' ? b.id : b)
    availableUsers.value = ((userRes as any).data || []).filter((u: SysUser) => !boundIds.includes(u.id))
    boundUsers.value = (userRes as any).data.filter((u: SysUser) => boundIds.includes(u.id))
  } catch { /* ignore */ }
}

const handleBindSave = async () => {
  if (!selectedUserId.value) { message.warning('请选择用户'); return }
  bindSaving.value = true
  try {
    await bindUserToShop(selectedUserId.value, currentShop.value.id!)
    message.success('绑定成功')
    if (currentShop.value.id) {
      const res: any = await getShopUsers(currentShop.value.id)
      const [userRes]: any[] = await Promise.all([getUserList()])
      const boundIds = ((res as any).data || []).map((b: any) => typeof b === 'object' ? b.id : b)
      availableUsers.value = ((userRes as any).data || []).filter((u: SysUser) => !boundIds.includes(u.id))
      boundUsers.value = (userRes as any).data.filter((u: SysUser) => boundIds.includes(u.id))
    }
    selectedUserId.value = undefined
  } finally { bindSaving.value = false }
}

const handleUnbind = async (userId?: number) => {
  if (!userId || !currentShop.value.id) return
  await unbindUserFromShop(userId, currentShop.value.id)
  message.success('解绑成功')
  if (currentShop.value.id) {
    const res: any = await getShopUsers(currentShop.value.id)
    const [userRes]: any[] = await Promise.all([getUserList()])
    const boundIds = ((res as any).data || []).map((b: any) => typeof b === 'object' ? b.id : b)
    availableUsers.value = ((userRes as any).data || []).filter((u: SysUser) => !boundIds.includes(u.id))
    boundUsers.value = (userRes as any).data.filter((u: SysUser) => boundIds.includes(u.id))
  }
}

// 二维码
const openQrDialog = async (shop: Shop) => {
  currentShop.value = shop
  qrVisible.value = true
  await nextTick()
  if (qrCanvas.value) {
    await QRCode.toCanvas(qrCanvas.value, `${baseUrl}/register?shopId=${shop.id}`, { width: 256 })
  }
}

onMounted(async () => {
  fetchData()
  if (isSuperAdmin.value) {
    try { const res: any = await getTenantList(); tenantList.value = res.data || [] } catch { /* ignore */ }
  }
})
</script>
