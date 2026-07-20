<template>
  <div>
    <a-card :title="isEdit ? '编辑订单' : '新建订单'">
      <a-form :model="form" :label-col="{ span: 4 }" :wrapper-col="{ span: 14 }" ref="formRef">
        <a-form-item label="店铺" name="shopId" :rules="[{ required: true, message: '请选择店铺' }]">
          <a-select v-model:value="form.shopId" style="width:100%" placeholder="请选择店铺" @change="onShopChange">
            <a-select-option v-for="s in shopList" :key="s.id" :value="s.id">{{ s.name }}</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="球员" name="playerId" :rules="[{ required: true, message: '请选择球员' }]">
          <a-select v-model:value="form.playerId" style="width:100%" placeholder="请选择球员" show-search :filter-option="playerFilter">
            <a-select-option v-for="p in playerList" :key="p.id" :value="p.id">{{ p.name }} {{ p.phone ? `(${p.phone})` : '' }}</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="球拍" name="racketId" :rules="[{ required: true, message: '请选择球拍' }]">
          <a-select v-model:value="form.racketId" style="width:100%" placeholder="请选择球拍">
            <a-select-option v-for="r in racketList" :key="r.id" :value="r.id">{{ r.brand }} {{ r.model }}</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="主线" name="mainStringId" :rules="[{ required: true, message: '请选择主线' }]">
          <a-select v-model:value="form.mainStringId" style="width:100%" placeholder="请选择球线" @change="onMainStringChange">
            <a-select-option v-for="s in shopStringList" :key="s.id" :value="s.stringInfoId">{{ s.stringName }} - ¥{{ s.price }}</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="主线磅数" name="mainTension" :rules="[{ required: true, message: '请输入主线磅数' }]">
          <a-input-number v-model:value="form.mainTension" :min="10" :max="80" :step="0.5" style="width:100%" /> lbs
        </a-form-item>

        <a-form-item label="横线">
          <a-row :gutter="10">
            <a-col :span="14">
              <a-select v-model:value="form.crossStringId" style="width:100%" placeholder="选填" allow-clear>
                <a-select-option v-for="s in shopStringList" :key="s.id" :value="s.stringInfoId">{{ s.stringName }} - ¥{{ s.price }}</a-select-option>
              </a-select>
            </a-col>
            <a-col :span="10">
              <a-input-number v-model:value="form.crossTension" :min="10" :max="80" :step="0.5" style="width:100%" placeholder="磅数" /> lbs
            </a-col>
          </a-row>
        </a-form-item>

        <a-form-item label="指派穿线师" name="stringerId">
          <a-select v-model:value="form.stringerId" style="width:100%" placeholder="请选择穿线师（可选）" allow-clear>
            <a-select-option v-for="s in stringerList" :key="s.id" :value="s.id">{{ s.name }} {{ s.phone ? `(${s.phone})` : '' }}</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="总价" name="totalPrice" :rules="[{ required: true, message: '请输入总价' }]">
          <a-input-number v-model:value="form.totalPrice" :min="0" :precision="2" style="width:100%" /> 元
        </a-form-item>

        <a-form-item label="备注">
          <a-textarea v-model:value="form.remark" :rows="3" />
        </a-form-item>

        <a-form-item :wrapper-col="{ offset: 4 }">
          <a-button type="primary" @click="handleSubmit" :loading="submitting">{{ isEdit ? '保存' : '创建订单' }}</a-button>
          <a-button style="margin-left: 12px" @click="$router.back()">取消</a-button>
        </a-form-item>
      </a-form>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'antdv-next'
import { getShopList, getPlayerList, getRacketList, getShopStrings, getStringers, createOrder, updateOrder, getOrderDetail, type Shop, type Player, type Racket, type ShopString, type StringingOrder, type Stringer } from '@/api'

const route = useRoute()
const router = useRouter()
const isEdit = ref(false)
const orderId = ref<number>()
const formRef = ref()
const submitting = ref(false)

const shopList = ref<Shop[]>([])
const playerList = ref<Player[]>([])
const racketList = ref<Racket[]>([])
const shopStringList = ref<ShopString[]>([])
const stringerList = ref<Stringer[]>([])

const form = reactive({
  shopId: undefined as number | undefined,
  playerId: undefined as number | undefined,
  racketId: undefined as number | undefined,
  mainStringId: undefined as number | undefined,
  mainTension: 26,
  crossStringId: undefined as number | undefined,
  crossTension: 26,
  totalPrice: 0,
  remark: '',
  stringerId: undefined as number | undefined
})

const fetchLists = async () => {
  try {
    const [sRes, pRes, rRes, strRes] = await Promise.all([
      getShopList(), getPlayerList(), getRacketList(), getStringers()
    ])
    shopList.value = sRes.data || []
    playerList.value = pRes.data || []
    racketList.value = rRes.data || []
    stringerList.value = strRes.data || []
  } catch { /* ignore */ }
}

const playerFilter = (input: string, option: any) => {
  return option.children?.toLowerCase().includes(input.toLowerCase())
}

const onShopChange = async (shopId: number) => {
  form.shopId = shopId
  shopStringList.value = []
  try {
    const res = await getShopStrings(shopId)
    shopStringList.value = res.data || []
  } catch { /* ignore */ }
}

const onMainStringChange = (val: number) => {
  const s = shopStringList.value.find(x => x.stringInfoId === val)
  if (s) {
    form.totalPrice = form.totalPrice || s.price
  }
}

const handleSubmit = async () => {
  try { await formRef.value?.validate() } catch { return }
  submitting.value = true
  try {
    if (isEdit.value && orderId.value) {
      await updateOrder({ ...form, id: orderId.value } as any)
      message.success('更新成功')
    } else {
      await createOrder(form as any)
      message.success('创建成功')
    }
    router.push('/order')
  } catch { /* ignore */ } finally { submitting.value = false }
}

onMounted(async () => {
  await fetchLists()

  // 从查询参数获取 shopId（扫码）
  const shopIdParam = route.query.shopId
  if (shopIdParam) {
    const sid = Number(shopIdParam)
    if (sid) { form.shopId = sid; await onShopChange(sid) }
  }

  // 编辑模式
  const editId = route.query.id
  if (editId) {
    orderId.value = Number(editId)
    isEdit.value = true
    try {
      const res = await getOrderDetail(orderId.value)
      const d = res.data
      form.shopId = d.shopId; form.playerId = d.playerId
      form.racketId = d.racketId; form.mainStringId = d.mainStringId
      form.mainTension = d.mainTension; form.crossStringId = d.crossStringId
      form.crossTension = d.crossTension || 0
      form.totalPrice = d.totalPrice || 0; form.remark = d.remark || ''
      form.stringerId = d.stringerId || undefined
      if (d.shopId) await onShopChange(d.shopId)
    } catch { message.error('加载订单失败') }
  }
})
</script>
