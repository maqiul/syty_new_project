<template>
  <div class="tennis-page">
    <!-- 顶部搜索栏 -->
    <div class="tennis-header">
      <h1 class="tennis-header__title">🎾 网球穿线登记</h1>
      <div class="tennis-header__search">
        <input
          class="input"
          type="text"
          placeholder="输入姓名查询"
          v-model="searchName"
        />
        <button class="btn btn--outline btn--sm" @click="searchHistory" :disabled="submitting">
          查询历史
        </button>
      </div>
    </div>

    <!-- 球拍列表 -->
    <div class="rackets-wrapper">
      <div
        v-for="(racket, index) in rackets"
        :key="racket.id"
        class="racket-card"
      >
        <!-- 卡片头部 -->
        <div class="racket-card__header">
          <span class="racket-card__title">📌 球拍 #{{ index + 1 }}</span>
          <button
            v-if="rackets.length > 1"
            class="racket-card__delete"
            @click="removeRacket(racket.id)"
          >
            ✕
          </button>
        </div>
        <div class="racket-card__divider"></div>

        <!-- 卡片内容 -->
        <div class="racket-card__body">
          <!-- 竖线/横线磅数 -->
          <div class="form-row--2col">
            <div class="form-field" style="margin-bottom: 0">
              <label class="form-field__label">竖线磅数</label>
              <div class="input-group">
                <input
                  type="number"
                  class="input"
                  placeholder="如：52"
                  v-model.number="racket.mainsTension"
                />
                <span class="input-suffix">磅</span>
              </div>
            </div>
            <div class="form-field" style="margin-bottom: 0">
              <label class="form-field__label">横线磅数</label>
              <div class="input-group">
                <input
                  type="number"
                  class="input"
                  placeholder="如：50"
                  v-model.number="racket.crossesTension"
                />
                <span class="input-suffix">磅</span>
              </div>
            </div>
          </div>

          <!-- 打结方式 -->
          <div class="form-field">
            <label class="form-field__label">打结方式</label>
            <div class="radio-group">
              <label class="radio-item">
                <input
                  type="radio"
                  :value="'2'"
                  :name="'knot-' + racket.id"
                  v-model="racket.knot"
                />
                <span class="radio-item__label">2结</span>
              </label>
              <label class="radio-item">
                <input
                  type="radio"
                  :value="'4'"
                  :name="'knot-' + racket.id"
                  v-model="racket.knot"
                />
                <span class="radio-item__label">4结</span>
              </label>
            </div>
          </div>

          <!-- 预拉 -->
          <div class="form-field form-field--toggle">
            <span class="form-field__label">预拉</span>
            <label class="toggle">
              <input type="checkbox" v-model="racket.prePull" />
              <span class="toggle__track"><span class="toggle__thumb"></span></span>
            </label>
          </div>

          <!-- Logo / 避震器备注 -->
          <div class="form-field">
            <label class="form-field__label">Logo / 避震器备注</label>
            <input
              type="text"
              class="input"
              placeholder="如：加装避震器、去除原厂Logo"
              v-model="racket.notes"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- 底部操作栏 -->
    <div class="page-bottom-spacer"></div>
    <div class="bottom-action-bar">
      <button class="btn--add-racket" @click="addRacket">
        <svg width="18" height="18" viewBox="0 0 18 18">
          <line x1="9" y1="3" x2="9" y2="15" stroke="currentColor" stroke-width="2" />
          <line x1="3" y1="9" x2="15" y2="9" stroke="currentColor" stroke-width="2" />
        </svg>
        加拍
      </button>
      <button class="btn--submit" @click="submitOrder" :disabled="submitting">
        {{ submitting ? '提交中...' : '提交订单' }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getOrdersByMobileOrName, submitOrders } from '@/api'

const router = useRouter()

let nextId = 1

/** 创建空球拍对象 */
function createRacket() {
  return reactive({
    id: nextId++,
    mainsTension: null,
    crossesTension: null,
    knot: '2',
    prePull: false,
    notes: '',
  })
}

const searchName = ref('')
const rackets = ref([createRacket()])
const submitting = ref(false)

/** 查询历史记录 */
async function searchHistory() {
  if (!searchName.value.trim()) {
    window.showToast?.('请输入姓名', 'error')
    return
  }

  try {
    const data = await getOrdersByMobileOrName({ oname: searchName.value })
    if (data && Array.isArray(data) && data.length > 0 && rackets.value.length > 0) {
      // 回填第一把球拍
      const first = rackets.value[0]
      const last = data[data.length - 1]
      if (last.mainsTension) first.mainsTension = last.mainsTension
      if (last.crossesTension) first.crossesTension = last.crossesTension
      if (last.knot) first.knot = String(last.knot)
      if (last.prePull !== undefined) first.prePull = !!last.prePull
      if (last.notes) first.notes = last.notes
    }
  } catch (err) {
    console.warn('[Tennis] 未找到历史记录或接口异常:', err.message)
  }
}

/** 添加球拍 */
function addRacket() {
  rackets.value.push(createRacket())
}

/** 删除球拍 */
function removeRacket(id) {
  if (rackets.value.length <= 1) return
  rackets.value = rackets.value.filter((r) => r.id !== id)
}

/** 提交订单 */
async function submitOrder() {
  if (!searchName.value.trim()) {
    window.showToast?.('请输入姓名', 'error')
    return
  }

  // 校验至少有一把球拍填写了磅数
  const validRackets = rackets.value.filter(
    (r) => r.mainsTension || r.crossesTension
  )
  if (validRackets.length === 0) {
    window.showToast?.('请至少填写一把球拍的磅数', 'error')
    return
  }

  submitting.value = true
  try {
    const payload = validRackets.map((r) => ({
      oname: searchName.value,
      mainsTension: r.mainsTension || 0,
      crossesTension: r.crossesTension || 0,
      knot: r.knot || '2',
      prePull: r.prePull ? 1 : 0,
      notes: r.notes || '',
      type: 'tennis',
    }))

    await submitOrders(payload)
    router.push('/thank-you')
  } catch (err) {
    window.showToast?.(err.message || '提交失败，请重试', 'error')
  } finally {
    submitting.value = false
  }
}

/** 页面初始化：从 localStorage 恢复姓名 */
onMounted(() => {
  const savedName = localStorage.getItem('sy_tennis_name')
  if (savedName) {
    searchName.value = savedName
  }
})
</script>

<style scoped>
/* ===== 页面容器 ===== */
.tennis-page {
  max-width: 480px;
  margin: 0 auto;
  background: #f3f4f6;
  min-height: 100vh;
  min-height: 100dvh;
  padding-bottom: calc(80px + env(safe-area-inset-bottom, 0px));
}

/* ===== 顶部搜索栏 ===== */
.tennis-header {
  background: #fff;
  padding: 16px;
  border-bottom: 1px solid #e5e7eb;
}
.tennis-header__title {
  font-size: 20px;
  font-weight: 700;
  color: #111827;
  margin-bottom: 12px;
}
.tennis-header__search {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}
.tennis-header__search .input {
  flex: 1;
}

/* ===== 通用 input ===== */
.input {
  width: 100%;
  height: 44px;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  padding: 0 12px;
  font-size: 14px;
  color: #111827;
  background: #fff;
  outline: none;
  transition: border-color 0.15s;
}
.input:focus {
  border-color: #1a56db;
  box-shadow: 0 0 0 2px rgba(26, 86, 219, 0.12);
}
.input::placeholder {
  color: #9ca3af;
}

/* ===== 输入框组 ===== */
.input-group {
  display: flex;
  align-items: center;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  height: 44px;
  padding: 0 12px;
  background: #fff;
  transition: border-color 0.15s;
}
.input-group:focus-within {
  border-color: #1a56db;
  box-shadow: 0 0 0 2px rgba(26, 86, 219, 0.12);
}
.input-group .input {
  border: none;
  padding: 0;
  height: 100%;
  box-shadow: none;
}
.input-group .input:focus {
  box-shadow: none;
}
.input-suffix {
  font-size: 13px;
  color: #9ca3af;
  margin-left: 4px;
  white-space: nowrap;
}

/* ===== 表单行 ===== */
.form-row--2col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 14px;
}

/* ===== 表单字段 ===== */
.form-field {
  margin-bottom: 14px;
}
.form-field:last-child {
  margin-bottom: 0;
}
.form-field__label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: #4b5563;
  margin-bottom: 8px;
}

/* ===== 单选组 ===== */
.radio-group {
  display: flex;
  gap: 20px;
}
.radio-item {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}
.radio-item input[type='radio'] {
  appearance: none;
  -webkit-appearance: none;
  width: 18px;
  height: 18px;
  border: 2px solid #d1d5db;
  border-radius: 50%;
  cursor: pointer;
  transition: all 0.15s;
  position: relative;
  flex-shrink: 0;
}
.radio-item input[type='radio']:checked {
  border-color: #1a56db;
  background: #fff;
}
.radio-item input[type='radio']:checked::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #1a56db;
}
.radio-item__label {
  font-size: 14px;
  color: #111827;
}

/* ===== 开关 ===== */
.form-field--toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.toggle {
  position: relative;
  cursor: pointer;
}
.toggle input {
  position: absolute;
  opacity: 0;
  width: 0;
  height: 0;
}
.toggle__track {
  display: block;
  width: 44px;
  height: 24px;
  background: #d1d5db;
  border-radius: 12px;
  transition: background 0.2s;
  position: relative;
}
.toggle__thumb {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 20px;
  height: 20px;
  background: #fff;
  border-radius: 50%;
  transition: transform 0.2s;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.15);
}
.toggle input:checked + .toggle__track {
  background: #1a56db;
}
.toggle input:checked + .toggle__track .toggle__thumb {
  transform: translateX(20px);
}

/* ===== 球拍卡片 ===== */
.racket-card {
  background: #ffffff;
  border-radius: 6px;
  margin: 12px 16px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  overflow: hidden;
}
.racket-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px 8px;
}
.racket-card__title {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}
.racket-card__delete {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: #fee2e2;
  color: #ef4444;
  border-radius: 50%;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.15s;
}
.racket-card__delete:active {
  transform: scale(0.9);
  background: #fecaca;
}
.racket-card__divider {
  height: 1px;
  background: #e5e7eb;
  margin: 0 16px;
}
.racket-card__body {
  padding: 14px 16px;
}

/* ===== 按钮 ===== */
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 6px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
}
.btn:active {
  transform: scale(0.98);
}
.btn:disabled {
  background: #e5e7eb !important;
  color: #9ca3af !important;
  cursor: not-allowed;
}
.btn--outline {
  background: #fff;
  color: #1a56db;
  border: 1px solid #1a56db;
  height: 36px;
  font-size: 13px;
  padding: 0 16px;
}
.btn--outline:not(:disabled):active {
  background: #ebf5ff;
}
.btn--sm {
  height: 36px;
  font-size: 13px;
  padding: 0 16px;
}

/* ===== 底部操作栏 ===== */
.page-bottom-spacer {
  height: 16px;
}
.bottom-action-bar {
  position: fixed;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 480px;
  display: flex;
  gap: 12px;
  padding: 12px 16px;
  padding-bottom: calc(12px + env(safe-area-inset-bottom, 0px));
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(8px);
  border-top: 1px solid #e5e7eb;
  z-index: 100;
}
.btn--add-racket {
  flex: 1;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background: #fff;
  color: #4b5563;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}
.btn--add-racket:active {
  background: #f3f4f6;
  transform: scale(0.98);
}
.btn--submit {
  flex: 2;
  height: 48px;
  border: none;
  border-radius: 6px;
  background: #1a56db;
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}
.btn--submit:active {
  background: #1e40af;
  transform: scale(0.98);
}
.btn--submit:disabled {
  background: #e5e7eb !important;
  color: #9ca3af !important;
  cursor: not-allowed;
}
</style>
