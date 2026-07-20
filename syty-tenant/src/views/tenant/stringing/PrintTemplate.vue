<template>
  <div>
    <a-card>
      <template #title>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>打印模板管理</span>
          <a-button type="primary" @click="openEditor()">
            <template #icon><PlusOutlined /></template>
            新建模板
          </a-button>
        </div>
      </template>

      <a-table :columns="columns" :dataSource="tableData" :loading="loading" :pagination="pagination" rowKey="id" @change="handleTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'name'">
            {{ record.name }}
            <a-tag v-if="record.isDefault" color="green" style="margin-left: 8px">默认</a-tag>
          </template>
          <template v-if="column.dataIndex === 'showQrcode'">
            {{ record.showQrcode ? '显示' : '隐藏' }}
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a type="primary" @click="openEditor(record)">编辑</a>
              <a-divider type="vertical" />
              <a v-if="!record.isDefault" @click="setDefault(record)">设为默认</a>
              <a-divider v-if="!record.isDefault" type="vertical" />
              <a type="danger" @click="handleDelete(record.id)">删除</a>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 编辑器对话框 -->
    <a-modal v-model:open="editorVisible" :title="editorForm.id ? '编辑模板' : '新建模板'" width="900px" :footer="null" @cancel="editorVisible = false">
      <!-- 🆕 编辑模式切换 -->
      <a-tabs v-model:activeKey="editMode" type="card" style="margin-bottom: 16px">
        <a-tab-pane key="visual" tab="可视化编辑">
          <a-button type="link" size="small" @click="showExampleModal = true">📋 查看模板示例 (50x30mm)</a-button>
        </a-tab-pane>
        <a-tab-pane key="json" tab="JSON 编辑">
          <a-space>
            <a-button type="primary" size="small" @click="parseJsonToForm">⬇️ JSON → 表单</a-button>
            <a-button size="small" @click="exportFormToJson">⬆️ 表单 → JSON</a-button>
            <a-button type="link" size="small" @click="showExampleModal = true">📋 查看模板示例</a-button>
          </a-space>
        </a-tab-pane>
      </a-tabs>

      <!-- 可视化编辑模式 -->
      <a-row v-show="editMode === 'visual'" :gutter="20">
        <a-col :span="14">
          <a-form :model="editorForm" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }" size="small">
            <a-form-item label="模板名称">
              <a-input v-model:value="editorForm.name" placeholder="如：小票标签" />
            </a-form-item>
            <a-row :gutter="10">
              <a-col :span="12">
                <a-form-item label="纸张宽(mm)">
                  <a-input-number v-model:value="editorForm.paperWidth" :min="40" :max="300" style="width:100%" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="纸张高(mm)">
                  <a-input-number v-model:value="editorForm.paperHeight" :min="20" :max="300" style="width:100%" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-form-item label="默认字号">
              <a-input-number v-model:value="editorForm.fontSize" :min="8" :max="30" style="width:100%" />
            </a-form-item>
            <a-form-item label="显示二维码">
              <a-switch v-model:checked="editorForm.showQrcode" />
            </a-form-item>
            <a-form-item label="设为默认">
              <a-switch v-model:checked="editorForm.isDefault" />
            </a-form-item>

            <a-divider />
            <p><strong>可用字段</strong>（点击添加）：</p>
            <a-row :gutter="[8, 8]">
              <a-col :span="8" v-for="f in availableFields" :key="f.key">
                <a-tag color="blue" style="cursor: pointer; width: 100%" @click="addField(f)">
                  {{ f.label }}
                </a-tag>
              </a-col>
            </a-row>

            <a-divider />
            <p><strong>已选字段</strong>（拖拽排序 / 点击移除）：</p>
            <draggable v-model="editorForm.fields" item-key="key" :group="{ name: 'fields', pull: false, put: false }" handle=".drag-handle" style="min-height: 100px">
              <template #item="{ element, index }">
                <div style="display: flex; align-items: center; gap: 8px; padding: 6px; margin-bottom: 4px; background: #fafafa; border: 1px solid #d9d9d9; border-radius: 4px; cursor: move">
                  <span class="drag-handle">☰</span>
                  <span style="flex: 1">{{ element.label }}</span>
                  <a-input v-model:value="element.label" size="small" style="width: 100px" placeholder="显示名" />
                  <a-popover title="样式配置" trigger="click">
                    <template #content>
                      <a-form size="small" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
                        <a-form-item label="字号">
                          <a-input-number v-model:value="element.fontSize" :min="8" :max="30" style="width: 80px" />
                        </a-form-item>
                        <a-form-item label="加粗">
                          <a-switch v-model:checked="element.bold" />
                        </a-form-item>
                        <a-form-item label="对齐">
                          <a-select v-model:value="element.align" style="width: 80px">
                            <a-select-option value="left">左</a-select-option>
                            <a-select-option value="center">中</a-select-option>
                            <a-select-option value="right">右</a-select-option>
                          </a-select>
                        </a-form-item>
                      </a-form>
                    </template>
                    <a-button size="small">样式</a-button>
                  </a-popover>
                  <a-button size="small" type="danger" @click="removeField(index)">×</a-button>
                </div>
              </template>
            </draggable>
          </a-form>
        </a-col>

        <a-col :span="10">
          <p><strong>预览</strong></p>
          <div :style="{ border: '1px solid #d9d9d9', width: previewWidth + 'mm', minHeight: previewHeight + 'mm', padding: '4px', background: '#fff', fontFamily: 'monospace', fontSize: editorForm.fontSize + 'px', overflow: 'auto', margin: '0 auto' }">
            <div v-if="editorForm.showQrcode" style="text-align: center; margin-bottom: 4px">
              <div style="font-size: 10px; color: #999">[二维码区域]</div>
              <div style="width: 40px; height: 40px; background: #eee; margin: 0 auto"></div>
            </div>
            <div v-for="f in editorForm.fields" :key="f.key" :style="{ fontSize: (f.fontSize || editorForm.fontSize) + 'px', fontWeight: (f.bold ? 'bold' : 'normal') as any, textAlign: (f.align || 'left') as any, marginBottom: '2px' }">
              {{ f.label }}：{{ f.key === 'orderNo' ? 'SY202501010001' : `[${f.label}]` }}
            </div>
          </div>
        </a-col>
      </a-row>

      <!-- 🆕 JSON 编辑模式 -->
      <a-row v-show="editMode === 'json'" :gutter="20">
        <a-col :span="14">
          <a-form :model="editorForm" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }" size="small">
            <a-form-item label="模板名称">
              <a-input v-model:value="editorForm.name" placeholder="如：小票标签" />
            </a-form-item>
            <a-row :gutter="10">
              <a-col :span="12">
                <a-form-item label="纸张宽(mm)">
                  <a-input-number v-model:value="editorForm.paperWidth" :min="40" :max="300" style="width:100%" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="纸张高(mm)">
                  <a-input-number v-model:value="editorForm.paperHeight" :min="20" :max="300" style="width:100%" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-form-item label="默认字号">
              <a-input-number v-model:value="editorForm.fontSize" :min="8" :max="30" style="width:100%" />
            </a-form-item>
            <a-form-item label="显示二维码">
              <a-switch v-model:checked="editorForm.showQrcode" />
            </a-form-item>
            <a-form-item label="设为默认">
              <a-switch v-model:checked="editorForm.isDefault" />
            </a-form-item>
          </a-form>
          <a-divider>模板 JSON 配置</a-divider>
          <a-textarea
            v-model:value="jsonEditorContent"
            :rows="18"
            placeholder='在此粘贴或编辑 JSON 模板配置...'
            style="font-family: 'Cascadia Code', 'Fira Code', 'Consolas', monospace; font-size: 12px; line-height: 1.5"
            :allow-clear="false"
          />
          <a-alert v-if="jsonError" type="error" :message="jsonError" style="margin-top: 8px" closable />
        </a-col>
        <a-col :span="10">
          <p><strong>JSON 说明</strong></p>
          <a-descriptions :column="1" size="small" bordered>
            <a-descriptions-item label="name">模板名称</a-descriptions-item>
            <a-descriptions-item label="paperWidth">纸张宽度 (mm)</a-descriptions-item>
            <a-descriptions-item label="paperHeight">纸张高度 (mm)</a-descriptions-item>
            <a-descriptions-item label="fontSize">默认字号</a-descriptions-item>
            <a-descriptions-item label="showQrcode">是否显示二维码</a-descriptions-item>
            <a-descriptions-item label="fields">字段数组，每项包含 key、label、fontSize、bold、align</a-descriptions-item>
          </a-descriptions>
          <p style="margin-top: 12px"><strong>可用字段 Key：</strong></p>
          <a-space wrap>
            <a-tag v-for="f in availableFields" :key="f.key">{{ f.key }} ({{ f.label }})</a-tag>
          </a-space>
        </a-col>
      </a-row>

      <div style="text-align: right; margin-top: 16px">
        <a-button @click="editorVisible = false" style="margin-right: 8px">取消</a-button>
        <a-button type="primary" @click="handleSaveTemplate" :loading="saving">保存模板</a-button>
      </div>
    </a-modal>

    <!-- 🆕 模板示例弹窗 -->
    <a-modal v-model:open="showExampleModal" title="📋 模板示例 (50x30mm 标签格式)" width="700px" :footer="null">
      <a-alert type="info" message="可直接复制以下 JSON，粘贴到「JSON 编辑」模式中使用" style="margin-bottom: 12px" />
      <a-textarea
        :value="exampleJson"
        :rows="20"
        readonly
        style="font-family: 'Cascadia Code', 'Fira Code', 'Consolas', monospace; font-size: 12px; line-height: 1.5"
      />
      <div style="text-align: right; margin-top: 12px">
        <a-button type="primary" @click="copyExample">📋 复制 JSON</a-button>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { message, Modal } from 'antdv-next'
import { PlusOutlined } from '@antdv-next/icons'
import draggable from 'vuedraggable'
import { getPrintTemplatePage, getPrintTemplate, addPrintTemplate, updatePrintTemplate, deletePrintTemplate, type PrintTemplate } from '@/api'

interface TemplateField {
  key: string
  label: string
  fontSize?: number
  bold?: boolean
  align?: string
}

interface AvailableField {
  key: string
  label: string
}

interface EditorForm {
  id: number | undefined
  name: string
  paperWidth: number
  paperHeight: number
  fontSize: number
  showQrcode: boolean
  isDefault: boolean
  fields: TemplateField[]
}

const loading = ref(false)
const tableData = ref<PrintTemplate[]>([])
const query = reactive({ page: 1, size: 20 })
const pagination = reactive({ current: 1, pageSize: 20, total: 0, showTotal: (t: number) => `共 ${t} 条` })

const editorVisible = ref(false)
const saving = ref(false)
const editorForm = reactive<EditorForm>({
  id: undefined, name: '', paperWidth: 80, paperHeight: 200,
  fontSize: 14, showQrcode: true, isDefault: false,
  fields: []
})

// 🆕 编辑模式：'visual' | 'json'
const editMode = ref<'visual' | 'json'>('visual')
const jsonEditorContent = ref('')
const jsonError = ref('')
const showExampleModal = ref(false)

// 🆕 50x30mm 标签标准 JSON 示例
const exampleJson = `{
  "name": "穿线标签-50x30mm",
  "paperWidth": 50,
  "paperHeight": 30,
  "fontSize": 9,
  "showQrcode": true,
  "isDefault": false,
  "fields": [
    { "key": "orderNo", "label": "订单号", "fontSize": 10, "bold": true, "align": "left" },
    { "key": "shopName", "label": "店铺", "fontSize": 9, "bold": false, "align": "left" },
    { "key": "playerName", "label": "球员", "fontSize": 9, "bold": true, "align": "left" },
    { "key": "playerPhone", "label": "电话", "fontSize": 8, "bold": false, "align": "left" },
    { "key": "racketModel", "label": "球拍", "fontSize": 9, "bold": false, "align": "left" },
    { "key": "mainString", "label": "主线", "fontSize": 8, "bold": false, "align": "left" },
    { "key": "mainTension", "label": "主线磅", "fontSize": 8, "bold": true, "align": "left" },
    { "key": "crossString", "label": "横线", "fontSize": 8, "bold": false, "align": "left" },
    { "key": "crossTension", "label": "横线磅", "fontSize": 8, "bold": true, "align": "left" },
    { "key": "remark", "label": "备注", "fontSize": 8, "bold": false, "align": "left" },
    { "key": "createdAt", "label": "下单时间", "fontSize": 7, "bold": false, "align": "left" }
  ]
}`

const availableFields: AvailableField[] = [
  { key: 'orderNo', label: '订单号' },
  { key: 'shopName', label: '店铺名' },
  { key: 'playerName', label: '球员姓名' },
  { key: 'playerPhone', label: '手机号' },
  { key: 'racketModel', label: '球拍型号' },
  { key: 'mainString', label: '主线型号' },
  { key: 'mainTension', label: '主线磅数' },
  { key: 'crossString', label: '横线型号' },
  { key: 'crossTension', label: '横线磅数' },
  { key: 'totalPrice', label: '总价' },
  { key: 'isPaid', label: '付费状态' },
  { key: 'remark', label: '备注' },
  { key: 'createdAt', label: '下单时间' },
  { key: 'pickupDate', label: '取拍日期' },
]

const previewWidth = computed(() => Math.min(editorForm.paperWidth, 200))
const previewHeight = computed(() => Math.min(editorForm.paperHeight, 300))

const columns = [
  { title: '模板名称', dataIndex: 'name', width: 200 },
  { title: '纸张', key: 'paper', width: 160 },
  { title: '二维码', dataIndex: 'showQrcode', width: 100 },
  { title: '字段数', key: 'fieldCount', width: 100 },
  { title: '创建时间', dataIndex: 'createdAt', width: 180 },
  { title: '操作', key: 'action', width: 250 }
]

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getPrintTemplatePage(query)
    tableData.value = res.data.records || res.data
    pagination.total = res.data.total || 0
  } finally { loading.value = false }
}

const handleTableChange = (pag: any) => {
  query.page = pag.current; query.size = pag.pageSize
  pagination.current = pag.current; pagination.pageSize = pag.pageSize
  fetchData()
}

// 🆕 打开编辑器
const openEditor = async (row?: PrintTemplate) => {
  editMode.value = 'visual'
  jsonError.value = ''
  jsonEditorContent.value = ''
  if (row) {
    const res = await getPrintTemplate(row.id!)
    const t = res.data
    editorForm.id = t.id; editorForm.name = t.name
    editorForm.paperWidth = (t as any).paperWidth || 80
    editorForm.paperHeight = (t as any).paperHeight || 200
    editorForm.fontSize = (t as any).fontSize || 14
    editorForm.showQrcode = (t as any).showQrcode ?? true
    editorForm.isDefault = t.isDefault || false
    editorForm.fields = (t as any).fields?.length ? (t as any).fields.map((f: any) => ({ ...f })) : []
  } else {
    editorForm.id = undefined; editorForm.name = ''
    editorForm.paperWidth = 50; editorForm.paperHeight = 30
    editorForm.fontSize = 9; editorForm.showQrcode = true
    editorForm.isDefault = false; editorForm.fields = []
  }
  editorVisible.value = true
}

const addField = (f: AvailableField) => {
  editorForm.fields.push({
    key: f.key, label: f.label, fontSize: editorForm.fontSize, bold: false, align: 'left'
  })
}

const removeField = (index: number) => {
  editorForm.fields.splice(index, 1)
}

// 保存模板
const handleSaveTemplate = async () => {
  // 🆕 JSON 模式下，先解析 JSON 到表单
  if (editMode.value === 'json') {
    try {
      const parsed = JSON.parse(jsonEditorContent.value)
      editorForm.name = parsed.name || editorForm.name
      editorForm.paperWidth = parsed.paperWidth || editorForm.paperWidth
      editorForm.paperHeight = parsed.paperHeight || editorForm.paperHeight
      editorForm.fontSize = parsed.fontSize || editorForm.fontSize
      editorForm.showQrcode = parsed.showQrcode ?? editorForm.showQrcode
      editorForm.isDefault = parsed.isDefault ?? editorForm.isDefault
      editorForm.fields = (parsed.fields || []).map((f: any) => ({
        key: f.key, label: f.label,
        fontSize: f.fontSize || editorForm.fontSize,
        bold: f.bold ?? false,
        align: f.align || 'left'
      }))
    } catch (e: any) {
      message.error(`JSON 格式错误：${e.message}`)
      return
    }
  }

  if (!editorForm.name) { message.warning('请输入模板名称'); return }
  if (editorForm.fields.length === 0) { message.warning('至少添加一个字段'); return }
  saving.value = true
  try {
    const data = { ...editorForm }
    if (editorForm.id) {
      await updatePrintTemplate(data as any)
    } else {
      await addPrintTemplate(data as any)
    }
    message.success(editorForm.id ? '更新成功' : '创建成功')
    editorVisible.value = false
    fetchData()
  } catch { /* ignore */ } finally { saving.value = false }
}

const setDefault = async (row: PrintTemplate) => {
  await updatePrintTemplate({ ...row, isDefault: true } as any)
  message.success('已设为默认模板')
  fetchData()
}

const handleDelete = (id: number) => {
  Modal.confirm({ title: '确认删除', content: '确定要删除该模板吗？', okText: '确定',
    onOk: async () => { await deletePrintTemplate(id); message.success('删除成功'); fetchData() }
  })
}

// 🆕 JSON 编辑：JSON → 表单
const parseJsonToForm = () => {
  jsonError.value = ''
  try {
    const parsed = JSON.parse(jsonEditorContent.value)
    editorForm.name = parsed.name || ''
    editorForm.paperWidth = parsed.paperWidth || 80
    editorForm.paperHeight = parsed.paperHeight || 200
    editorForm.fontSize = parsed.fontSize || 14
    editorForm.showQrcode = parsed.showQrcode ?? true
    editorForm.isDefault = parsed.isDefault ?? false
    editorForm.fields = (parsed.fields || []).map((f: any) => ({
      key: f.key, label: f.label,
      fontSize: f.fontSize || editorForm.fontSize,
      bold: f.bold ?? false,
      align: f.align || 'left'
    }))
    message.success('JSON 已解析到表单！')
  } catch (e: any) {
    jsonError.value = `JSON 解析失败：${e.message}`
  }
}

// 🆕 JSON 编辑：表单 → JSON
const exportFormToJson = () => {
  jsonEditorContent.value = JSON.stringify({
    name: editorForm.name,
    paperWidth: editorForm.paperWidth,
    paperHeight: editorForm.paperHeight,
    fontSize: editorForm.fontSize,
    showQrcode: editorForm.showQrcode,
    isDefault: editorForm.isDefault,
    fields: editorForm.fields
  }, null, 2)
  message.success('表单已导出为 JSON！')
}

// 🆕 复制示例 JSON
const copyExample = async () => {
  try {
    await navigator.clipboard.writeText(exampleJson)
    message.success('JSON 已复制到剪贴板！')
  } catch {
    // fallback for non-secure contexts
    const ta = document.createElement('textarea')
    ta.value = exampleJson
    document.body.appendChild(ta)
    ta.select()
    document.execCommand('copy')
    document.body.removeChild(ta)
    message.success('JSON 已复制到剪贴板！')
  }
}

// 🆕 监听编辑模式切换：切换到 JSON 模式时自动同步表单内容
watch(editMode, (newMode) => {
  if (newMode === 'json') {
    exportFormToJson()
    jsonError.value = ''
  }
})

onMounted(() => fetchData())
</script>

<style scoped>
.drag-handle {
  cursor: grab;
  color: #999;
}
.drag-handle:active {
  cursor: grabbing;
}
</style>
