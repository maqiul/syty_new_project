<template>
  <div>
    <a-card :bordered="false">
      <template #title>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>打印策略配置</span>
          <a-space>
            <a-button type="primary" @click="openEditor()">
              <template #icon><PlusOutlined /></template>
              新建策略
            </a-button>
            <a-button @click="fetchPolicies">
              <template #icon><ReloadOutlined /></template>
              刷新
            </a-button>
          </a-space>
        </div>
      </template>

      <!-- 筛选区 -->
      <div style="margin-bottom: 16px">
        <a-space>
          <span>场景：</span>
          <a-select
            v-model:value="filterScene"
            style="width: 140px"
            placeholder="全部场景"
            allow-clear
            @change="fetchPolicies"
          >
            <a-select-option value="CASUAL">散客</a-select-option>
            <a-select-option value="TOURNAMENT">赛事</a-select-option>
          </a-select>

          <span>球类：</span>
          <a-select
            v-model:value="filterSport"
            style="width: 140px"
            placeholder="全部球类"
            allow-clear
            @change="fetchPolicies"
          >
            <a-select-option value="BADMINTON">羽毛球</a-select-option>
            <a-select-option value="TENNIS">网球</a-select-option>
            <a-select-option value="GENERAL">通用</a-select-option>
          </a-select>

          <span>单据类型：</span>
          <a-select
            v-model:value="filterDocType"
            style="width: 140px"
            placeholder="全部类型"
            allow-clear
            @change="fetchPolicies"
          >
            <a-select-option value="LABEL">标签</a-select-option>
            <a-select-option value="RECEIPT">回执</a-select-option>
          </a-select>
        </a-space>
      </div>

      <!-- 策略列表表格 -->
      <a-table
        :columns="columns"
        :dataSource="policyList"
        :loading="loading"
        :pagination="pagination"
        rowKey="id"
        size="small"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <!-- 场景 -->
          <template v-if="column.dataIndex === 'scene'">
            <a-tag :color="record.scene === 'TOURNAMENT' ? 'purple' : 'blue'">
              {{ sceneLabelMap[record.scene] || record.scene }}
            </a-tag>
          </template>

          <!-- 球类 -->
          <template v-else-if="column.dataIndex === 'sport'">
            <a-tag :color="sportColorMap[record.sport] || 'default'">
              {{ sportLabelMap[record.sport] || record.sport }}
            </a-tag>
          </template>

          <!-- 单据类型 -->
          <template v-else-if="column.dataIndex === 'docType'">
            <a-tag :color="record.docType === 'LABEL' ? 'cyan' : 'geekblue'">
              {{ docTypeLabelMap[record.docType] || record.docType }}
            </a-tag>
          </template>

          <!-- 目标角色 -->
          <template v-else-if="column.dataIndex === 'targetRole'">
            <a-tag color="volcano">
              {{ roleLabelMap[record.targetRole] || record.targetRole }}
            </a-tag>
          </template>

          <!-- 关联模板 -->
          <template v-else-if="column.dataIndex === 'templateName'">
            <a-tag v-if="record.templateName" color="green">{{ record.templateName }}</a-tag>
            <span v-else style="color: #999">未关联</span>
          </template>

          <!-- 状态 -->
          <template v-else-if="column.dataIndex === 'status'">
            <a-badge
              :status="record.status === 1 ? 'success' : 'default'"
              :text="record.status === 1 ? '启用' : '禁用'"
            />
          </template>

          <!-- 创建时间 -->
          <template v-else-if="column.dataIndex === 'createdAt'">
            {{ formatTime(record.createdAt) }}
          </template>

          <!-- 操作列 -->
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a @click="openEditor(record)">编辑</a>
              <a-divider type="vertical" />
              <a-popconfirm title="确定删除该策略？" @confirm="handleDelete(record.id)">
                <a style="color: #ff4d4f">删除</a>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新增/编辑弹窗 -->
    <a-modal
      v-model:open="editorVisible"
      :title="editorForm.id ? '编辑策略' : '新建策略'"
      width="650px"
      :confirm-loading="submitting"
      @ok="handleSubmit"
      @cancel="handleCancel"
    >
      <a-form
        ref="formRef"
        :model="editorForm"
        :rules="editorRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
        size="small"
      >
        <a-divider orientation="left" style="margin: 12px 0 16px">匹配条件</a-divider>

        <a-form-item label="场景" name="scene">
          <a-select v-model:value="editorForm.scene" placeholder="请选择场景">
            <a-select-option value="CASUAL">散客</a-select-option>
            <a-select-option value="TOURNAMENT">赛事</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="球类" name="sport">
          <a-select v-model:value="editorForm.sport" placeholder="请选择球类">
            <a-select-option value="BADMINTON">羽毛球</a-select-option>
            <a-select-option value="TENNIS">网球</a-select-option>
            <a-select-option value="GENERAL">通用</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="单据类型" name="docType">
          <a-select v-model:value="editorForm.docType" placeholder="请选择单据类型">
            <a-select-option value="LABEL">标签</a-select-option>
            <a-select-option value="RECEIPT">回执</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="目标角色" name="targetRole">
          <a-select v-model:value="editorForm.targetRole" placeholder="请选择目标角色">
            <a-select-option value="FRONT_DESK">前台</a-select-option>
            <a-select-option value="STRINGING_ROOM">穿线房</a-select-option>
            <a-select-option value="TOURNAMENT">大赛</a-select-option>
          </a-select>
        </a-form-item>

        <a-divider orientation="left" style="margin: 12px 0 16px">输出配置</a-divider>

        <a-form-item label="关联模板" name="templateId">
          <a-select
            v-model:value="editorForm.templateId"
            placeholder="请选择打印模板"
            :loading="templateLoading"
            allow-clear
            show-search
            :filter-option="filterTemplateOption"
          >
            <a-select-option
              v-for="t in templateList"
              :key="t.id"
              :value="t.id"
              :label="t.name"
            >
              {{ t.name }}
              <a-tag v-if="t.scope === 'PUBLIC'" size="small" style="margin-left: 4px">公共</a-tag>
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="优先级" name="priority">
          <a-input-number
            v-model:value="editorForm.priority"
            :min="0"
            :max="999"
            style="width: 100%"
            placeholder="数值越大优先级越高，默认 0"
          />
        </a-form-item>

        <a-form-item label="状态" name="status">
          <a-switch
            v-model:checked="statusChecked"
            checked-children="启用"
            un-checked-children="禁用"
          />
        </a-form-item>

        <a-form-item label="备注" name="remark">
          <a-textarea
            v-model:value="editorForm.remark"
            :rows="2"
            placeholder="可选，策略说明"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch, onMounted } from 'vue'
import { message, type FormInstance } from 'antdv-next'
import { PlusOutlined, ReloadOutlined } from '@antdv-next/icons'
import {
  getPrintPolicyPage,
  addPrintPolicy,
  updatePrintPolicy,
  deletePrintPolicy,
  getPrintTemplateList,
  type PrintTemplate,
} from '@/api'

// ============ 类型定义 ============
interface PrintPolicy {
  id?: number
  scene: string        // CASUAL / TOURNAMENT
  sport: string        // BADMINTON / TENNIS / GENERAL
  docType: string      // LABEL / RECEIPT
  targetRole: string   // FRONT_DESK / STRINGING_ROOM / TOURNAMENT
  templateId?: number
  templateName?: string
  priority?: number
  status?: number
  remark?: string
  createdAt?: string
}

// ============ 列表相关 ============
const loading = ref(false)
const policyList = ref<PrintPolicy[]>([])

const filterScene = ref<string | undefined>(undefined)
const filterSport = ref<string | undefined>(undefined)
const filterDocType = ref<string | undefined>(undefined)

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
})

const columns = [
  { title: 'ID', dataIndex: 'id', width: 60 },
  { title: '场景', dataIndex: 'scene', width: 90 },
  { title: '球类', dataIndex: 'sport', width: 90 },
  { title: '单据类型', dataIndex: 'docType', width: 90 },
  { title: '目标角色', dataIndex: 'targetRole', width: 100 },
  { title: '关联模板', dataIndex: 'templateName', width: 160 },
  { title: '优先级', dataIndex: 'priority', width: 80 },
  { title: '状态', dataIndex: 'status', width: 80 },
  { title: '创建时间', dataIndex: 'createdAt', width: 170 },
  { title: '操作', key: 'action', width: 140, fixed: 'right' as const },
]

const sceneLabelMap: Record<string, string> = {
  CASUAL: '散客',
  TOURNAMENT: '赛事',
}

const sportLabelMap: Record<string, string> = {
  BADMINTON: '羽毛球',
  TENNIS: '网球',
  GENERAL: '通用',
}

const sportColorMap: Record<string, string> = {
  BADMINTON: 'green',
  TENNIS: 'orange',
  GENERAL: 'blue',
}

const docTypeLabelMap: Record<string, string> = {
  LABEL: '标签',
  RECEIPT: '回执',
}

const roleLabelMap: Record<string, string> = {
  FRONT_DESK: '前台',
  STRINGING_ROOM: '穿线房',
  TOURNAMENT: '大赛',
}

// ============ 模板列表（用于关联下拉框） ============
const templateList = ref<PrintTemplate[]>([])
const templateLoading = ref(false)

const fetchTemplateList = async () => {
  templateLoading.value = true
  try {
    const res = await getPrintTemplateList()
    templateList.value = res.data || []
  } catch {
    templateList.value = []
  } finally {
    templateLoading.value = false
  }
}

const filterTemplateOption = (input: string, option: any) => {
  return (option?.label ?? '').toLowerCase().includes(input.toLowerCase())
}

// ============ 编辑器相关 ============
const editorVisible = ref(false)
const formRef = ref<FormInstance>()
const submitting = ref(false)

const defaultForm = (): PrintPolicy => ({
  id: undefined,
  scene: 'CASUAL',
  sport: 'GENERAL',
  docType: 'LABEL',
  targetRole: 'FRONT_DESK',
  templateId: undefined,
  priority: 0,
  status: 1,
  remark: '',
})

const editorForm = reactive<PrintPolicy>(defaultForm())
const statusChecked = ref(true)

const editorRules = {
  scene: [{ required: true, message: '请选择场景', trigger: 'change' }],
  sport: [{ required: true, message: '请选择球类', trigger: 'change' }],
  docType: [{ required: true, message: '请选择单据类型', trigger: 'change' }],
  targetRole: [{ required: true, message: '请选择目标角色', trigger: 'change' }],
  templateId: [{ required: true, message: '请选择关联模板', trigger: 'change' }],
}

watch(statusChecked, (val) => {
  editorForm.status = val ? 1 : 0
})

// ============ 工具函数 ============
const formatTime = (time?: string) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

// ============ CRUD 操作 ============
const fetchPolicies = async () => {
  loading.value = true
  try {
    const params: Record<string, any> = {
      current: pagination.current,
      size: pagination.pageSize,
    }
    if (filterScene.value) params.scene = filterScene.value
    if (filterSport.value) params.sport = filterSport.value
    if (filterDocType.value) params.docType = filterDocType.value

    const res = await getPrintPolicyPage(params)
    policyList.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch (e: any) {
    message.error(e.message || '获取策略列表失败')
  } finally {
    loading.value = false
  }
}

const handleTableChange = (pag: any) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchPolicies()
}

const openEditor = (record?: PrintPolicy) => {
  Object.assign(editorForm, defaultForm())
  statusChecked.value = true

  if (record) {
    Object.assign(editorForm, record)
    statusChecked.value = record.status !== 0
  }

  editorVisible.value = true
}

const handleSubmit = async () => {
  try {
    await formRef.value?.validateFields()
  } catch {
    return
  }

  submitting.value = true
  try {
    const data = {
      scene: editorForm.scene,
      sport: editorForm.sport,
      docType: editorForm.docType,
      targetRole: editorForm.targetRole,
      templateId: editorForm.templateId,
      priority: editorForm.priority ?? 0,
      status: editorForm.status,
      remark: editorForm.remark,
    }

    if (editorForm.id) {
      await updatePrintPolicy({ ...data, id: editorForm.id })
      message.success('更新成功')
    } else {
      await addPrintPolicy(data)
      message.success('创建成功')
    }

    editorVisible.value = false
    fetchPolicies()
  } catch (e: any) {
    message.error(e.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

const handleCancel = () => {
  editorVisible.value = false
  Object.assign(editorForm, defaultForm())
}

const handleDelete = async (id: number) => {
  try {
    await deletePrintPolicy(id)
    message.success('删除成功')
    fetchPolicies()
  } catch (e: any) {
    message.error(e.message || '删除失败')
  }
}

// ============ 生命周期 ============
onMounted(() => {
  fetchPolicies()
  fetchTemplateList()
})
</script>
