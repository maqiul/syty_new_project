<template>
  <view class="register-page">
    <!-- ====== 手机号输入区 ====== -->
    <view class="phone-section">
      <text class="section-label">📱 请输入手机号</text>
      <view class="phone-input-wrap">
        <input
          class="phone-input"
          type="number"
          maxlength="11"
          v-model="phone"
          placeholder="请输入11位手机号"
          placeholder-class="phone-placeholder"
          @blur="onPhoneBlur"
          @confirm="onPhoneConfirm"
          confirm-type="search"
        />
        <view v-if="phone.length === 11" class="phone-status">
          <text v-if="querying" class="status-text querying">查询中...</text>
          <text v-else-if="hasHistory" class="status-text success">✓ 已找到历史记录</text>
          <text v-else-if="queried && !hasHistory" class="status-text new-customer">未找到记录，请填写下方信息</text>
        </view>
      </view>
    </view>

    <!-- ====== 参数确认表单 ====== -->
    <view class="form-section" :class="{ 'form-disabled': !queried }">
      <view class="form-card">
        <view class="form-item">
          <text class="form-label">🏸 球拍型号</text>
          <input
            class="form-input"
            v-model="form.racketModel"
            placeholder="例如：YY AST99"
            placeholder-class="form-placeholder"
          />
        </view>

        <view class="form-item">
          <text class="form-label">🔢 主线磅数</text>
          <view class="pound-control">
            <view class="pound-btn" @click="adjustPound('main', -1)">－</view>
            <input
              class="pound-input"
              type="number"
              v-model.number="form.mainPounds"
              placeholder="0"
              placeholder-class="form-placeholder"
            />
            <view class="pound-btn" @click="adjustPound('main', 1)">＋</view>
          </view>
        </view>

        <view class="form-item">
          <text class="form-label">🔢 横线磅数</text>
          <view class="pound-control">
            <view class="pound-btn" @click="adjustPound('cross', -1)">－</view>
            <input
              class="pound-input"
              type="number"
              v-model.number="form.crossPounds"
              placeholder="0"
              placeholder-class="form-placeholder"
            />
            <view class="pound-btn" @click="adjustPound('cross', 1)">＋</view>
          </view>
        </view>

        <view class="form-item">
          <text class="form-label">🪢 结头方式</text>
          <view class="knot-options">
            <view
              v-for="option in knotOptions"
              :key="option"
              class="knot-option"
              :class="{ active: form.knotType === option }"
              @click="form.knotType = option"
            >
              <text>{{ option }}</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- ====== 次卡弹窗 (Modal) ====== -->
    <view v-if="showPunchCardModal" class="punch-card-modal-mask" @click="closeCardModal">
      <view class="punch-card-modal" @click.stop>
        <view class="modal-header">🎫 检测到您有可用次卡</view>
        <view class="modal-body">
          <view v-for="card in availableCards" :key="card.id" 
                class="card-item"
                :class="{ 'card-selected': selectedCard?.id === card.id }"
                @click="selectCard(card)">
            <view class="card-info">
              <text class="card-type">{{ card.cardType === 'TEN_TIMES' ? '10次卡' : '20次卡' }}</text>
              <text class="card-expire">有效期至 {{ card.expireTime }}</text>
            </view>
            <view class="card-count">
              <text class="count-num">{{ card.remainingCount }}</text>
              <text class="count-unit">次</text>
            </view>
          </view>
        </view>
        <view class="modal-footer">
          <view class="modal-btn cancel" @click="skipPunchCard">暂不使用</view>
          <view class="modal-btn confirm" @click="confirmPunchCard"
                :class="{ 'disabled': !selectedCard }">本次使用</view>
        </view>
      </view>
    </view>

    <!-- ====== 提交按钮 ====== -->
    <view class="submit-section">
      <view
        class="submit-btn"
        :class="{
          'btn-disabled': !canSubmit || submitting || cooling,
          'btn-submitting': submitting,
          'btn-cooling': cooling,
        }"
        @click="handleSubmit"
      >
        <text class="submit-text" v-if="!submitting && !cooling">🎯 确认穿线</text>
        <text class="submit-text" v-else-if="submitting">提交中...</text>
        <text class="submit-text" v-else>冷却中 ({{ coolSeconds }}s)</text>
      </view>
      <!-- 跳转到查询页 -->
      <view class="query-link" @click="goToQuery">
        <text>📋 查询订单进度</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { getHistory, submitOrder, getAvailableCards, type HistoryRecord, type OrderPayload } from '@/api/h5'

// ====== 状态 ======
const phone = ref('')
const queried = ref(false)
const querying = ref(false)
const hasHistory = ref(false)
const submitting = ref(false)
const cooling = ref(false)
const coolSeconds = ref(0)

const form = reactive({
  racketModel: '',
  mainPounds: 0,
  crossPounds: 0,
  knotType: '双结',
})

const knotOptions = ['双结', '单结', 'YONEX结', 'VICTOR结']

// 手机号失焦 / 回车
const onPhoneBlur = () => {
  if (phone.value.length === 11 && !querying.value) {
    queryHistory()
  }
}

const onPhoneConfirm = () => {
  onPhoneBlur()
}

// 查询历史
const queryHistory = async () => {
  querying.value = true
  try {
    const res = await getHistory(phone.value)
    if (res.code === 0 && res.data) {
      fillForm(res.data)
      hasHistory.value = true
    } else {
      resetForm()
      hasHistory.value = false
    }
    // 历史查完后，不管有没有记录，都查一下有没有次卡
    await fetchPunchCards()
  } catch {
    resetForm()
    hasHistory.value = false
  } finally {
    queried.value = true
    querying.value = false
  }
}

// 回填表单
const fillForm = (record: HistoryRecord) => {
  form.racketModel = record.racketModel || ''
  form.mainPounds = record.mainPounds || 0
  form.crossPounds = record.crossPounds || 0
  form.knotType = record.knotType || '双结'
}

const resetForm = () => {
  form.racketModel = ''
  form.mainPounds = 0
  form.crossPounds = 0
  form.knotType = '双结'
}

// 磅数调整
const adjustPound = (type: 'main' | 'cross', delta: number) => {
  const key = type === 'main' ? 'mainPounds' : 'crossPounds'
  const newVal = form[key] + delta
  if (newVal >= 0) {
    form[key] = newVal
  }
}

// --- 次卡逻辑 ---
const showPunchCardModal = ref(false)
const availableCards = ref<any[]>([])
const selectedCard = ref<any>(null)

const fetchPunchCards = async () => {
  try {
    const res = await getAvailableCards(phone.value)
    if (res.code === 0 && res.data && res.data.length > 0) {
      availableCards.value = res.data
      // 如果有次卡，弹出选择框
      showPunchCardModal.value = true
    }
  } catch (e) {
    // 忽略错误
  }
}

const closeCardModal = () => {
  showPunchCardModal.value = false
  selectedCard.value = null
}

const selectCard = (card: any) => {
  selectedCard.value = card
}

const skipPunchCard = () => {
  closeCardModal()
}

const confirmPunchCard = () => {
  if (!selectedCard.value) return
  showPunchCardModal.value = false
}

// --- 修改提交逻辑 ---
const canSubmit = computed(() => {
  return (
    queried.value &&
    phone.value.length === 11 &&
    form.racketModel.trim() !== '' &&
    form.mainPounds > 0 &&
    form.crossPounds > 0 &&
    form.knotType !== ''
  )
})

// 提交订单
const handleSubmit = async () => {
  if (!canSubmit.value || submitting.value || cooling.value) return

  submitting.value = true
  try {
    const payload: any = {
      phone: phone.value,
      racketModel: form.racketModel.trim(),
      mainPounds: form.mainPounds,
      crossPounds: form.crossPounds,
      knotType: form.knotType,
      // 次卡逻辑
      usePunchCard: !!selectedCard.value,
      punchCardId: selectedCard.value?.id || null,
    }

    const res = await submitOrder(payload)

    if (res.code === 0) {
      if (res.warning) {
        uni.showModal({
          title: '⚠️ 提示',
          content: `订单已提交，但${res.warning}，请联系店员确认。`,
          showCancel: false,
          confirmText: '知道了',
        })
      } else {
        uni.showModal({
          title: '✅ 已接单',
          content: selectedCard.value ? '已扣减 1 次卡次数' : '您的穿线订单已提交成功',
          showCancel: false,
          confirmText: '好的',
        })
        // 成功后重置
        resetForm()
        queried.value = false
        hasHistory.value = false
        selectedCard.value = null
        phone.value = ''
      }
    } else {
      uni.showModal({
        title: '提交失败',
        content: res.message || '订单提交失败，请稍后重试',
        showCancel: false,
        confirmText: '知道了',
      })
    }
  } catch {
    // 错误已在 request 拦截器中 toast
  } finally {
    submitting.value = false
    startCoolDown()
  }
}

// 3秒冷却
const startCoolDown = () => {
  cooling.value = true
  coolSeconds.value = 3
  const timer = setInterval(() => {
    coolSeconds.value--
    if (coolSeconds.value <= 0) {
      clearInterval(timer)
      cooling.value = false
    }
  }, 1000)
}

// 跳转到查询页
const goToQuery = () => {
  uni.navigateTo({ url: '/pages/index/index' })
}
</script>

<style lang="scss" scoped>
.register-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding-bottom: 120rpx;
}

/* ====== 手机号输入区 ====== */
.phone-section {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 60rpx 32rpx 48rpx;
}

.section-label {
  display: block;
  font-size: 36rpx;
  color: rgba(255, 255, 255, 0.9);
  margin-bottom: 20rpx;
}

.phone-input-wrap {
  background: #fff;
  border-radius: 16rpx;
  padding: 20rpx 24rpx;
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.1);
}

.phone-input {
  font-size: 48rpx;
  font-weight: 600;
  color: #333;
  height: 80rpx;
  letter-spacing: 4rpx;
}

.phone-placeholder {
  font-size: 32rpx;
  color: #c0c4cc;
}

.phone-status {
  margin-top: 12rpx;
  border-top: 1rpx solid #eee;
  padding-top: 12rpx;
}

.status-text {
  font-size: 26rpx;
  &.querying { color: #909399; }
  &.success { color: #67c23a; }
  &.new-customer { color: #e6a23c; }
}

/* ====== 表单区域 ====== */
.form-section {
  padding: 32rpx;
  &.form-disabled {
    opacity: 0.5;
    pointer-events: none;
  }
}

.form-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 32rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.06);
}

.form-item {
  padding: 24rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
  &:last-child { border-bottom: none; }
}

.form-label {
  display: block;
  font-size: 32rpx;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16rpx;
}

.form-input {
  font-size: 34rpx;
  color: #333;
  height: 72rpx;
  background: #f5f7fa;
  border-radius: 8rpx;
  padding: 0 20rpx;
}

.form-placeholder {
  font-size: 28rpx;
  color: #c0c4cc;
}

/* 磅数控制 */
.pound-control {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.pound-btn {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  font-size: 40rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  &:active { opacity: 0.7; }
}

.pound-input {
  flex: 1;
  font-size: 40rpx;
  font-weight: 700;
  color: #667eea;
  text-align: center;
  height: 72rpx;
  background: #f5f7fa;
  border-radius: 8rpx;
}

/* 结头选项 */
.knot-options {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.knot-option {
  padding: 16rpx 32rpx;
  background: #f5f7fa;
  border-radius: 12rpx;
  font-size: 28rpx;
  color: #606266;
  border: 2rpx solid transparent;
  transition: all 0.2s;
  &:active { opacity: 0.7; }
  &.active {
    background: #ecf5ff;
    color: #667eea;
    border-color: #667eea;
    font-weight: 600;
  }
}

/* ====== 提交按钮 ====== */
.submit-section {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 20rpx 32rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background: #fff;
  box-shadow: 0 -4rpx 16rpx rgba(0, 0, 0, 0.08);
  z-index: 10;
}

.submit-btn {
  height: 96rpx;
  border-radius: 48rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 24rpx rgba(102, 126, 234, 0.4);
  transition: all 0.2s;
  &:active:not(.btn-disabled) {
    transform: scale(0.98);
    opacity: 0.9;
  }
  &.btn-disabled {
    background: #c0c4cc;
    box-shadow: none;
  }
  &.btn-submitting,
  &.btn-cooling {
    background: #909399;
    box-shadow: none;
  }
}

.submit-text {
  font-size: 36rpx;
  font-weight: 700;
  color: #fff;
  letter-spacing: 2rpx;
}

.query-link {
  text-align: center;
  margin-top: 20rpx;
  padding: 12rpx;
  font-size: 28rpx;
  color: #909399;
  &:active { color: #667eea; }
}
</style>
