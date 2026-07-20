<template>
  <div style="max-width: 800px; margin: 0 auto">
    <a-card title="🏸 大赛登记">
      <a-form :model="form" :label-col="{ span: 4 }" :wrapper-col="{ span: 16 }" ref="formRef">
        <a-form-item label="店铺" name="shopId" :rules="[{ required: true, message: '请选择店铺' }]">
          <a-select v-model:value="form.shopId" placeholder="选择店铺">
            <a-select-option v-for="s in shopList" :key="s.id" :value="s.id">{{ s.name }}</a-select-option>
          </a-select>
        </a-form-item>

        <a-divider>比赛信息</a-divider>

        <a-form-item label="比赛名称">
          <a-input v-model:value="form.matchName" placeholder="如 2026春季公开赛" />
        </a-form-item>

        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="比赛日期" :label-col="{ span: 8 }">
              <a-date-picker v-model:value="form.matchDateDayjs" value-format="YYYY-MM-DD HH:mm:ss" show-time style="width:100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="选手姓名" :label-col="{ span: 8 }" :rules="[{ required: true, message: '请输入' }]">
              <a-input v-model:value="form.playerName" placeholder="选手姓名" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="对手" :label-col="{ span: 8 }">
              <a-input v-model:value="form.opponentName" placeholder="对手姓名" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="比分" :label-col="{ span: 8 }">
              <a-input v-model:value="form.matchResult" placeholder="如 21:15,18:21,21:19" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item label="备注">
          <a-textarea v-model:value="form.remark" :rows="2" placeholder="额外信息" />
        </a-form-item>

        <a-form-item :wrapper-col="{ offset: 4, span: 16 }">
          <a-button type="primary" size="large" :loading="submitting" @click="handleSubmit">
            <CheckOutlined /> 登记并生成二维码
          </a-button>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 登记成功，展示二维码 -->
    <a-card v-if="createdOrder" title="✅ 登记成功" style="margin-top: 20px; text-align: center">
      <p style="color: #6B7280; margin-bottom: 16px">选手 {{ createdOrder.playerName }} · {{ createdOrder.matchName || '未命名比赛' }}</p>
      <canvas ref="qrCanvas" width="200" height="200" style="border: 1px solid #e8ecf1; border-radius: 8px" />
      <p style="margin-top: 12px; font-size: 13px; color: #374151; font-weight: 500">{{ createdOrder.orderNo }}</p>
      <p style="font-size: 12px; color: #9CA3AF">扫描二维码登记穿线信息</p>
      <a-space style="margin-top: 16px">
        <a-button @click="resetAfterCreate">继续登记</a-button>
        <a-button type="primary" @click="router.push('/tournament/list')">查看记录</a-button>
      </a-space>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'antdv-next'
import { CheckOutlined } from '@antdv-next/icons'
import QRCode from 'qrcode'
import { getShopList, createBadmintonTournamentOrder } from '@/api'

const router = useRouter()
const shopList = ref<any[]>([])
const submitting = ref(false)
const formRef = ref()
const createdOrder = ref<any>(null)
const qrCanvas = ref<HTMLCanvasElement>()

const form = reactive({
  shopId: undefined as number | undefined,
  playerName: '',
  matchName: '',
  opponentName: '',
  matchResult: '',
  matchDateDayjs: null as any,
  remark: ''
})

const handleSubmit = async () => {
  try { await formRef.value?.validate() } catch { return }
  if (!form.shopId) { message.warning('请选择店铺'); return }
  submitting.value = true
  try {
    const res: any = await createBadmintonTournamentOrder({
      shopId: form.shopId,
      playerName: form.playerName,
      matchName: form.matchName,
      matchDate: form.matchDateDayjs,
      opponentName: form.opponentName,
      matchResult: form.matchResult,
      remark: form.remark,
      status: 0
    })
    createdOrder.value = res.data || res
    await nextTick()
    if (qrCanvas.value && createdOrder.value?.orderNo) {
      await QRCode.toCanvas(qrCanvas.value, createdOrder.value.orderNo, { width: 200, margin: 1 })
    }
    message.success('登记成功')
  } catch { /* ignore */ } finally { submitting.value = false }
}

const resetAfterCreate = () => {
  createdOrder.value = null
  form.playerName = ''; form.matchName = ''; form.opponentName = ''; form.matchResult = ''
  form.matchDateDayjs = null; form.remark = ''
}

;(async () => {
  try { const r = await getShopList(); shopList.value = (r.data as any) || [] } catch { /* */ }
})()
</script>
