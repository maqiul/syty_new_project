<template>
  <div>
    <a-card>
      <template #title>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>{{ isEdit ? '编辑订单' : '订单详情' }} - {{ order?.orderNo }}</span>
          <div>
            <a-button v-if="isEdit" @click="switchMode(false)" style="margin-right: 8px">取消编辑</a-button>
            <a-button v-else @click="switchMode(true)" style="margin-right: 8px">编辑</a-button>
            <a-button @click="$router.push('/order')">返回列表</a-button>
          </div>
        </div>
      </template>

      <a-descriptions :column="2" bordered size="small">
        <a-descriptions-item label="订单编号" :span="2">{{ order?.orderNo }}</a-descriptions-item>
        <a-descriptions-item label="店铺">{{ order?.shopName }}</a-descriptions-item>
        <a-descriptions-item label="操作人">{{ order?.operatorName }}</a-descriptions-item>
        <a-descriptions-item label="球员">{{ order?.playerName }}</a-descriptions-item>
        <a-descriptions-item label="球拍">{{ order?.racketName }}</a-descriptions-item>
        <a-descriptions-item label="主线型号">{{ order?.mainStringName }}</a-descriptions-item>
        <a-descriptions-item label="主线磅数">{{ order?.mainTension }} lbs</a-descriptions-item>
        <a-descriptions-item label="横线型号">{{ order?.crossStringName || '-' }}</a-descriptions-item>
        <a-descriptions-item label="横线磅数">{{ order?.crossTension ? `${order.crossTension} lbs` : '-' }}</a-descriptions-item>
        <a-descriptions-item label="总价">¥{{ order?.totalPrice?.toFixed(2) }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="statusColor(order?.status)">{{ statusText(order?.status) }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="备注" :span="2">{{ order?.remark || '-' }}</a-descriptions-item>
        <a-descriptions-item label="创建时间" :span="2">{{ order?.createdAt }}</a-descriptions-item>
        <a-descriptions-item v-if="order?.completedAt" label="完成时间" :span="2">{{ order.completedAt }}</a-descriptions-item>
      </a-descriptions>

      <div style="margin-top: 20px; display: flex; gap: 20px; align-items: flex-start">
        <div style="flex-shrink: 0; text-align: center">
          <div style="font-size: 12px; color: #6B7280; margin-bottom: 8px">订单二维码</div>
          <canvas ref="qrCanvas" width="160" height="160"></canvas>
          <div style="font-size: 11px; color: #9CA3AF; margin-top: 4px">{{ order?.orderNo }}</div>
        </div>
      </div>

      <div v-if="isEdit" style="margin-top: 20px">
        <a-divider>编辑订单</a-divider>
        <a-form :model="editForm" :label-col="{ span: 4 }" :wrapper-col="{ span: 14 }" ref="editFormRef">
          <a-form-item label="主线磅数" :rules="[{ required: true, message: '请输入主线磅数' }]">
            <a-input-number v-model:value="editForm.mainTension" :min="10" :max="80" :step="0.5" style="width:200px" /> lbs
          </a-form-item>
          <a-form-item label="横线磅数">
            <a-input-number v-model:value="editForm.crossTension" :min="10" :max="80" :step="0.5" style="width:200px" /> lbs
          </a-form-item>
          <a-form-item label="总价" :rules="[{ required: true, message: '请输入总价' }]">
            <a-input-number v-model:value="editForm.totalPrice" :min="0" :precision="2" style="width:200px" /> 元
          </a-form-item>
          <a-form-item label="状态">
            <a-select v-model:value="editForm.status" style="width:200px">
              <a-select-option value="PENDING">待处理</a-select-option>
              <a-select-option value="COMPLETED">已完成</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="备注">
            <a-textarea v-model:value="editForm.remark" :rows="3" style="width:400px" />
          </a-form-item>
          <a-form-item :wrapper-col="{ offset: 4 }">
            <a-button type="primary" @click="handleSave" :loading="saving">保存</a-button>
            <a-button style="margin-left: 12px" @click="switchMode(false)">取消</a-button>
          </a-form-item>
        </a-form>
      </div>

      <div style="margin-top: 20px">
        <a-button type="primary" @click="handlePrint" :loading="printing">
          <template #icon><PrinterOutlined /></template>
          推送打印
        </a-button>
      </div>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick, watch } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'antdv-next'
import { PrinterOutlined } from '@antdv-next/icons'
import QRCode from 'qrcode'
import { getOrderDetail, updateOrder, printOrder, type StringingOrder } from '@/api'

const route = useRoute()
const orderId = Number(route.params.id)
const isEdit = ref(false)
const order = ref<StringingOrder>({} as StringingOrder)
const saving = ref(false)
const printing = ref(false)
const editFormRef = ref()
const qrCanvas = ref<HTMLCanvasElement>()

const editForm = reactive({
  id: 0, mainTension: 26, crossTension: 26, totalPrice: 0, status: 0 as number, remark: ''
})

const statusColor = (s?: number) => {
  if (s === undefined || s === null) return 'default'
  const map: Record<number, string> = { 0: 'default', 1: 'processing', 2: 'success' }
  return map[s] || 'default'
}
const statusText = (s?: number) => {
  if (s === undefined || s === null) return '未知'
  const map: Record<number, string> = { 0: '待穿', 1: '已穿', 2: '已取' }
  return map[s] || String(s)
}

const drawQr = async () => {
  await nextTick()
  const canvas = qrCanvas.value
  if (!canvas || !order.value?.orderNo) return
  try {
    await QRCode.toCanvas(canvas, order.value.orderNo, { width: 160, margin: 1 })
  } catch { /* ignore */ }
}

const fetchDetail = async () => {
  try {
    const res = await getOrderDetail(orderId)
    order.value = res.data
    drawQr()
  } catch { message.error('加载订单失败') }
}

const switchMode = (edit: boolean) => {
  isEdit.value = edit
  if (edit && order.value.id) {
    editForm.id = order.value.id
    editForm.mainTension = order.value.mainTension
    editForm.crossTension = order.value.crossTension || 26
    editForm.totalPrice = order.value.totalPrice || 0
    editForm.status = typeof order.value.status === 'number' ? order.value.status : 0
    editForm.remark = order.value.remark || ''
  }
}

const handleSave = async () => {
  try { await editFormRef.value?.validate() } catch { return }
  saving.value = true
  try {
    await updateOrder({ ...editForm, id: orderId } as any)
    message.success('保存成功')
    isEdit.value = false
    await fetchDetail()
  } catch { /* ignore */ } finally { saving.value = false }
}

const handlePrint = async () => {
  printing.value = true
  try {
    await printOrder(orderId)
    message.success('已推送打印任务')
  } catch { message.error('打印推送失败') } finally { printing.value = false }
}

onMounted(() => fetchDetail())
</script>
