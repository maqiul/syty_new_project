<template>
  <div>
    <a-card :bordered="false">
      <template #title>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>打印模板管理 (V1.4)</span>
          <a-space>
            <a-button type="primary" @click="openEditor()">
              <template #icon><PlusOutlined /></template>
              新建模板
            </a-button>
            <a-button @click="fetchTemplates">
              <template #icon><ReloadOutlined /></template>
              刷新
            </a-button>
          </a-space>
        </div>
      </template>

      <!-- 筛选区 -->
      <div style="margin-bottom: 16px">
        <a-space>
          <a-input-search
            v-model:value="keyword"
            placeholder="搜索模板名称"
            style="width: 280px"
            allow-clear
            @search="fetchTemplates"
            @clear="fetchTemplates"
          >
            <template #prefix>
              <SearchOutlined />
            </template>
          </a-input-search>

          <span>范围：</span>
          <a-select
            v-model:value="filterScope"
            style="width: 140px"
            placeholder="全部范围"
            allow-clear
            @change="fetchTemplates"
          >
            <a-select-option value="PUBLIC">公共</a-select-option>
            <a-select-option value="SHOP">门店</a-select-option>
          </a-select>
        </a-space>
      </div>

      <!-- 模板列表表格 -->
      <a-table
        :columns="columns"
        :dataSource="templateList"
        :loading="loading"
        :pagination="pagination"
        rowKey="id"
        size="small"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <!-- 范围 -->
          <template v-if="column.dataIndex === 'scope'">
            <a-tag :color="record.scope === 'PUBLIC' ? 'blue' : 'orange'">
              {{ scopeLabelMap[record.scope] || record.scope }}
            </a-tag>
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
              <a-popconfirm title="确定删除该模板？" @confirm="handleDelete(record.id)">
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
      :title="editorForm.id ? '编辑模板' : '新建模板'"
      width="900px"
      :confirm-loading="submitting"
      @ok="handleSubmit"
      @cancel="handleCancel"
    >
      <a-form
        ref="formRef"
        :model="editorForm"
        :rules="editorRules"
        :label-col="{ span: 4 }"
        :wrapper-col="{ span: 20 }"
        size="small"
      >
        <a-form-item label="模板名称" name="name">
          <a-input v-model:value="editorForm.name" placeholder="如：羽毛球标签模板" />
        </a-form-item>

        <a-form-item label="范围" name="scope">
          <a-select v-model:value="editorForm.scope" placeholder="请选择范围">
            <a-select-option value="PUBLIC">公共（所有门店可用）</a-select-option>
            <a-select-option value="SHOP">门店（仅指定门店可用）</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="状态" name="status">
          <a-switch
            v-model:checked="statusChecked"
            checked-children="启用"
            un-checked-children="禁用"
          />
        </a-form-item>

        <a-form-item label="模板JSON" name="content">
          <div style="position: relative">
            <a-button
              type="dashed"
              size="small"
              style="position: absolute; right: 8px; top: 8px; z-index: 10"
              @click="showExample"
            >
              <template #icon><CodeOutlined /></template>
              查看示例
            </a-button>
            <a-textarea
              v-model:value="editorForm.content"
              :rows="18"
              placeholder='请输入模板 JSON 内容，例如：
{
  "type": "label",
  "title": "穿线标签",
  "fields": [...]
}'
              style="font-family: 'Consolas', 'Monaco', monospace; font-size: 13px"
            />
          </div>
        </a-form-item>

        <a-form-item label="JSON 校验">
          <a-tag :color="jsonValidateStatus === 'success' ? 'green' : jsonValidateStatus === 'error' ? 'red' : 'default'">
            {{ jsonValidateMsg || '等待输入' }}
          </a-tag>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 示例弹窗 -->
    <a-modal
      v-model:open="exampleVisible"
      title="模板 JSON 示例"
      width="700px"
      :footer="null"
    >
      <a-space direction="vertical" style="width: 100%">
        <a-alert
          message="标准模板 JSON 结构说明"
          type="info"
          show-icon
          style="margin-bottom: 12px"
        />
        <pre style="background: #f5f5f5; padding: 16px; border-radius: 4px; overflow-x: auto; font-size: 13px; line-height: 1.6">{{ exampleJson }}</pre>
        <a-button type="primary" @click="copyExample" style="margin-top: 8px">
          <template #icon><CopyOutlined /></template>
          复制到剪贴板
        </a-button>
      </a-space>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { message, type FormInstance } from 'antdv-next'
import {
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
  CodeOutlined,
  CopyOutlined,
} from '@antdv-next/icons'
import {
  getPrintTemplatePage,
  addPrintTemplate,
  updatePrintTemplate,
  deletePrintTemplate,
  type PrintTemplate,
} from '@/api'

// ============ 类型扩展 ============
interface PrintTemplateV2 extends PrintTemplate {
  scope?: 'PUBLIC' | 'SHOP'
  status?: number
}

// ============ 列表相关 ============
const loading = ref(false)
const templateList = ref<PrintTemplateV2[]>([])
const keyword = ref('')
const filterScope = ref<string | undefined>(undefined)

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
})

const columns = [
  { title: 'ID', dataIndex: 'id', width: 70 },
  { title: '模板名称', dataIndex: 'name', width: 200 },
  { title: '范围', dataIndex: 'scope', width: 100 },
  { title: '状态', dataIndex: 'status', width: 80 },
  { title: '创建时间', dataIndex: 'createdAt', width: 180 },
  { title: '操作', key: 'action', width: 150, fixed: 'right' as const },
]

const scopeLabelMap: Record<string, string> = {
  PUBLIC: '公共',
  SHOP: '门店',
}

// ============ 编辑器相关 ============
const editorVisible = ref(false)
const formRef = ref<FormInstance>()
const submitting = ref(false)

const defaultForm = (): PrintTemplateV2 => ({
  id: undefined,
  name: '',
  scope: 'PUBLIC',
  status: 1,
  content: '',
})

const editorForm = reactive<PrintTemplateV2>(defaultForm())
const statusChecked = ref(true)

const editorRules = {
  name: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  scope: [{ required: true, message: '请选择范围', trigger: 'change' }],
  content: [
    { required: true, message: '请输入模板 JSON 内容', trigger: 'blur' },
    {
      validator: (_rule: any, value: string) => {
        if (!value) return Promise.resolve()
        try {
          JSON.parse(value)
          return Promise.resolve()
        } catch {
          return Promise.reject(new Error('JSON 格式不正确，请检查'))
        }
      },
      trigger: 'blur',
    },
  ],
}

// JSON 校验状态
const jsonValidateStatus = ref<'success' | 'error' | ''>('')
const jsonValidateMsg = ref('')

watch(
  () => editorForm.content,
  (val) => {
    if (!val) {
      jsonValidateStatus.value = ''
      jsonValidateMsg.value = ''
      return
    }
    try {
      JSON.parse(val)
      jsonValidateStatus.value = 'success'
      jsonValidateMsg.value = 'JSON 格式正确'
    } catch (e: any) {
      jsonValidateStatus.value = 'error'
      jsonValidateMsg.value = `JSON 错误: ${e.message}`
    }
  },
)

watch(statusChecked, (val) => {
  editorForm.status = val ? 1 : 0
})

// ============ 示例弹窗 ============
const exampleVisible = ref(false)
const exampleJson = `{
  "type": "label",
  "title": "穿线服务标签",
  "version": "1.0",
  "paper": {
    "width": 80,
    "height": 50
  },
  "sections": [
    {
      "type": "header",
      "content": "穿线标签",
      "fontSize": 16,
      "align": "center",
      "bold": true
    },
    {
      "type": "divider",
      "style": "dashed"
    },
    {
      "type": "fields",
      "items": [
        { "label": "订单号", "value": "{{orderNo}}", "bold": true },
        { "label": "客户", "value": "{{playerName}}" },
        { "label": "球拍", "value": "{{racketName}}" },
        { "label": "主线", "value": "{{mainString}}" },
        { "label": "横线", "value": "{{crossString}}" },
        { "label": "主线磅数", "value": "{{mainTension}} lbs" },
        { "label": "横线磅数", "value": "{{crossTension}} lbs" }
      ]
    },
    {
      "type": "qrcode",
      "value": "{{orderNo}}",
      "size": 60,
      "align": "center"
    }
  ]
}`

const showExample = () => {
  exampleVisible.value = true
}

const copyExample = async () => {
  try {
    await navigator.clipboard.writeText(exampleJson)
    message.success('已复制到剪贴板')
  } catch {
    message.error('复制失败，请手动复制')
  }
}

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
const fetchTemplates = async () => {
  loading.value = true
  try {
    const params: Record<string, any> = {
      current: pagination.current,
      size: pagination.pageSize,
    }
    if (keyword.value) params.keyword = keyword.value
    if (filterScope.value) params.scope = filterScope.value

    const res = await getPrintTemplatePage(params)
    templateList.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch (e: any) {
    message.error(e.message || '获取模板列表失败')
  } finally {
    loading.value = false
  }
}

const handleTableChange = (pag: any) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchTemplates()
}

const openEditor = (record?: PrintTemplateV2) => {
  Object.assign(editorForm, defaultForm())
  statusChecked.value = true

  if (record) {
    Object.assign(editorForm, record)
    statusChecked.value = record.status !== 0
  }

  jsonValidateStatus.value = ''
  jsonValidateMsg.value = ''
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
    // 校验 JSON
    try {
      JSON.parse(editorForm.content || '')
    } catch {
      message.error('JSON 格式不正确，请检查后重试')
      return
    }

    const data = {
      name: editorForm.name,
      scope: editorForm.scope,
      status: editorForm.status,
      content: editorForm.content,
    }

    if (editorForm.id) {
      await updatePrintTemplate({ ...data, id: editorForm.id })
      message.success('更新成功')
    } else {
      await addPrintTemplate(data as PrintTemplate)
      message.success('创建成功')
    }

    editorVisible.value = false
    fetchTemplates()
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
    await deletePrintTemplate(id)
    message.success('删除成功')
    fetchTemplates()
  } catch (e: any) {
    message.error(e.message || '删除失败')
  }
}

// ============ 生命周期 ============
onMounted(() => {
  fetchTemplates()
})
</script>
