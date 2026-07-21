<template>
  <div>
    <a-card>
      <template #title>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>💰 提成规则配置</span>
          <a-button type="primary" @click="openDialog()"><template #icon><PlusOutlined /></template>新增规则</a-button>
        </div>
      </template>

      <a-table :columns="columns" :dataSource="tableData" :loading="loading" :pagination="pagination" rowKey="id" @change="handleTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'ruleType'">
            <a-tag :color="ruleTypeColor[record.ruleType]">{{ ruleTypeText[record.ruleType] }}</a-tag>
          </template>
          <template v-if="column.dataIndex === 'enabled'">
            <a-tag :color="record.enabled === 1 ? 'green' : 'default'">{{ record.enabled === 1 ? '启用' : '禁用' }}</a-tag>
          </template>
          <template v-if="column.dataIndex === 'amount'">
            <template v-if="record.ruleType === 'FIXED'">{{ record.fixedAmount }} 元/支</template>
            <template v-else-if="record.ruleType === 'PERCENT'">{{ record.percentRate }}% ({{ record.percentBase }})</template>
            <template v-else>阶梯规则</template>
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

    <a-modal v-model:open="dialogVisible" :title="dialogForm.id ? '编辑提成规则' : '新增提成规则'" width="600px" @ok="handleSave" :confirmLoading="saving">
      <a-form :model="dialogForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="规则类型" required>
          <a-select v-model:value="dialogForm.ruleType">
            <a-select-option value="FIXED">固定金额</a-select-option>
            <a-select-option value="PERCENT">百分比</a-select-option>
            <a-select-option value="TIERED">阶梯</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item v-if="dialogForm.ruleType === 'FIXED'" label="固定金额(元/支)">
          <a-input-number v-model:value="dialogForm.fixedAmount" :min="0" :step="0.5" style="width:100%" />
        </a-form-item>
        <a-form-item v-if="dialogForm.ruleType === 'PERCENT'" label="提成比例(%)">
          <a-input-number v-model:value="dialogForm.percentRate" :min="0" :max="100" :step="1" style="width:100%" />
        </a-form-item>
        <a-form-item v-if="dialogForm.ruleType === 'PERCENT'" label="提成基数">
          <a-select v-model:value="dialogForm.percentBase">
            <a-select-option value="LABOR">手工费</a-select-option>
            <a-select-option value="TOTAL">总价</a-select-option>
            <a-select-option value="STRING">线材费</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="生效日期">
          <a-date-picker v-model:value="dialogForm.effectiveFrom" style="width:100%" />
        </a-form-item>
        <a-form-item label="截止日期">
          <a-date-picker v-model:value="dialogForm.effectiveTo" style="width:100%" placeholder="留空=永久" />
        </a-form-item>
        <a-form-item label="状态">
          <a-switch v-model:checked="dialogForm.enabledBool" checked-children="启用" un-checked-children="禁用" />
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model:value="dialogForm.remark" :rows="2" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'antdv-next'
import { PlusOutlined } from '@antdv-next/icons'
import request from '@/utils/axios'

const loading = ref(false); const saving = ref(false); const dialogVisible = ref(false)
const tableData = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0, showSizeChanger: true })

const ruleTypeText: Record<string, string> = { FIXED: '固定金额', PERCENT: '百分比', TIERED: '阶梯' }
const ruleTypeColor: Record<string, string> = { FIXED: 'blue', PERCENT: 'green', TIERED: 'purple' }

const dialogForm = reactive<any>({
  id: null, ruleType: 'FIXED', fixedAmount: 15, percentRate: 30, percentBase: 'LABOR',
  effectiveFrom: null, effectiveTo: null, enabled: 1, enabledBool: true, remark: ''
})

const columns = [
  { title: '规则类型', dataIndex: 'ruleType', width: 120 },
  { title: '提成金额', dataIndex: 'amount', width: 160 },
  { title: '生效日期', dataIndex: 'effectiveFrom', width: 120 },
  { title: '截止日期', dataIndex: 'effectiveTo', width: 120 },
  { title: '状态', dataIndex: 'enabled', width: 80 },
  { title: '备注', dataIndex: 'remark', ellipsis: true },
  { title: '操作', key: 'action', width: 150 }
]

const loadData = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/api/commission/rule/list')
    tableData.value = res?.data?.data || res?.data || []
  } catch { tableData.value = [] }
  finally { loading.value = false }
}

const handleTableChange = (p: any) => { pagination.current = p.current; pagination.pageSize = p.pageSize; loadData() }

const openDialog = (record?: any) => {
  if (record) {
    Object.assign(dialogForm, record)
    dialogForm.enabledBool = record.enabled === 1
  } else {
    Object.assign(dialogForm, { id: null, ruleType: 'FIXED', fixedAmount: 15, percentRate: 30, percentBase: 'LABOR', effectiveFrom: null, effectiveTo: null, enabled: 1, enabledBool: true, remark: '' })
  }
  dialogVisible.value = true
}

const handleSave = async () => {
  saving.value = true
  dialogForm.enabled = dialogForm.enabledBool ? 1 : 0
  try {
    if (dialogForm.id) await request.put('/api/commission/rule', dialogForm)
    else await request.post('/api/commission/rule', dialogForm)
    message.success('保存成功'); dialogVisible.value = false; loadData()
  } catch { message.error('保存失败') }
  finally { saving.value = false }
}

const handleDelete = (id: number) => {
  Modal.confirm({ title: '确认删除？', onOk: async () => { await request.delete(`/api/commission/rule/${id}`); loadData(); message.success('已删除') } })
}

onMounted(loadData)
</script>
