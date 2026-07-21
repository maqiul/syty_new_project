<template>
  <div class="platform-package-list">
    <a-card :bordered="false">
      <template #title>
        <div class="card-header">
          <span class="card-title">租户套餐管理</span>
          <a-button type="primary" @click="handleAdd">
            <template #icon><PlusOutlined /></template>
            新增套餐
          </a-button>
        </div>
      </template>

      <a-table :columns="columns" :dataSource="list" :loading="loading" rowKey="id">
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'price'">¥ {{ record.price }}</template>
          <template v-if="column.dataIndex === 'status'">
            <a-tag :color="record.status === 1 ? 'green' : 'default'">{{ record.status === 1 ? '启用' : '停用' }}</a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a @click="handleEdit(record)">编辑</a>
              <a-divider type="vertical" />
              <a-popconfirm title="确认删除？" @confirm="handleDelete(record.id)">
                <a style="color: #ff4d4f">删除</a>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新增/编辑弹窗 -->
    <a-modal v-model:open="dialogVisible" :title="isEdit ? '编辑套餐' : '新增套餐'" @ok="handleSave" :confirmLoading="saving">
      <a-form :model="dialogForm" layout="vertical">
        <a-form-item label="套餐名称" required><a-input v-model:value="dialogForm.name" /></a-form-item>
        <a-form-item label="月费价格" required><a-input-number v-model:value="dialogForm.price" :min="0" style="width: 100%" /></a-form-item>
        <a-form-item label="有效期(天)" required><a-input-number v-model:value="dialogForm.durationDays" :min="1" style="width: 100%" /></a-form-item>
        <a-form-item label="最大用户数"><a-input-number v-model:value="dialogForm.maxUsers" :min="0" :step="10" style="width: 100%" /></a-form-item>
        <a-form-item label="状态"><a-switch v-model:checked="dialogForm.status" /></a-form-item>
        <a-form-item label="特性说明">
          <a-textarea v-model:value="dialogForm.featuresText" :rows="3" placeholder='JSON 数组，如 ["10GB存储", "专属客服"]' />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { PlusOutlined } from '@antdv-next/icons'
import { message } from 'antdv-next'
import { getPlatformPackagesList, addPlatformPackage, updatePlatformPackage, deletePlatformPackage } from '@/api/admin/package'

const columns = [
  { title: '套餐名称', dataIndex: 'name', key: 'name' },
  { title: '月费', dataIndex: 'price', key: 'price', width: 120 },
  { title: '有效期', dataIndex: 'durationDays', key: 'durationDays', width: 120, customRender: ({ text }) => `${text} 天` },
  { title: '最大用户', dataIndex: 'maxUsers', key: 'maxUsers', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 150, fixed: 'right' as const },
]

const list = ref<any[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)

const dialogForm = reactive({
  id: undefined as number | undefined,
  name: '',
  price: 0,
  durationDays: 365,
  maxUsers: 50,
  status: true,
  featuresText: '[]',
})

const fetchList = async () => {
  loading.value = true
  try {
    const res = await getPlatformPackagesList()
    list.value = res.data || []
  } catch (e: any) { message.error(e.message || '加载失败') } finally { loading.value = false }
}

const handleAdd = () => {
  isEdit.value = false
  Object.assign(dialogForm, { id: undefined, name: '', price: 0, durationDays: 365, maxUsers: 50, status: true, featuresText: '[]' })
  dialogVisible.value = true
}

const handleEdit = (record: any) => {
  isEdit.value = true
  Object.assign(dialogForm, { ...record, status: record.status === 1, featuresText: record.features || '[]' })
  dialogVisible.value = true
}

const handleSave = async () => {
  if (!dialogForm.name || !dialogForm.price) return message.error('请填写必填项')
  saving.value = true
  try {
    // 🔴 关键：features 必须是字符串！禁止 JSON.parse！
    const payload = {
      id: dialogForm.id,
      name: dialogForm.name,
      price: dialogForm.price,
      durationDays: dialogForm.durationDays,
      maxUsers: dialogForm.maxUsers,
      status: dialogForm.status ? 1 : 0,
      features: typeof dialogForm.featuresText === 'string' ? dialogForm.featuresText : '[]'
    }

    console.log('提交的 features 类型:', typeof payload.features, payload.features)
    isEdit.value ? await updatePlatformPackage(payload) : await addPlatformPackage(payload)
    message.success('保存成功')
    dialogVisible.value = false
    fetchList()
  } catch (e: any) { message.error(e.message || '保存失败') } finally { saving.value = false }
}

const handleDelete = async (id: number) => {
  try { await deletePlatformPackage(id); message.success('删除成功'); fetchList() } catch {}
}

onMounted(fetchList)
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.platform-package-list { padding: 16px; background: #f5f7fa; min-height: 100vh; }
</style>
