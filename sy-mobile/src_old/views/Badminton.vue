<template>
  <div class="badminton-page">
    <!-- 顶部信息区 -->
    <div class="top-info-bar">
      <!-- 手机号 -->
      <div class="form-group">
        <label class="form-group__label">手机号</label>
        <div class="form-row">
          <input
            class="input"
            :class="{ 'input--error': phoneError }"
            type="tel"
            placeholder="请输入手机号"
            maxlength="11"
            v-model="phone"
            @input="phoneError = ''"
          />
          <button class="btn btn--primary btn--sm" @click="fetchHistory" :disabled="submitting">
            获取记录
          </button>
        </div>
        <p v-if="phoneError" class="input-error-msg">{{ phoneError }}</p>
      </div>

      <!-- 姓名 (全局唯一) -->
      <div class="form-group">
        <label class="form-group__label">姓名</label>
        <input
          class="input input--lg"
          :class="{ 'input--filled': customerName }"
          type="text"
          placeholder="请输入姓名"
          v-model="customerName"
        />
      </div>

      <!-- 店铺标识 (隐藏但显示给用户看，增强信任感) -->
      <div class="form-group">
        <label class="form-group__label">当前门店</label>
        <div class="shop-tag">{{ shopName || '加载中...' }}</div>
      </div>
    </div>

    <!-- 球拍列表 -->
    <div class="rackets-wrapper">
      <div
        v-for="(racket, index) in rackets"
        :key="racket.id"
        class="racket-section"
      >
        <div class="racket-section__header">
          <span class="racket-section__title">🏸 球拍 #{{ index + 1 }}</span>
          <button
            v-if="rackets.length > 1"
            class="racket-section__delete"
            @click="removeRacket(racket.id)"
          >
            ✕
          </button>
        </div>

        <!-- 拍型 -->
        <div class="form-card">
          <label class="form-card__label" :for="'bm-racket-' + racket.id">拍型</label>
          <input
            class="input input--lg"
            :id="'bm-racket-' + racket.id"
            type="text"
            placeholder="如：天斧100ZZ"
            v-model="racket.model"
          />
        </div>

        <!-- 线材 (按品牌分组动态加载) -->
        <div class="form-card">
          <label class="form-card__label" :for="'bm-string-' + racket.id">线材</label>
          <select 
            class="input input--lg" 
            :id="'bm-string-' + racket.id" 
            v-model="racket.stringId"
            :disabled="stringsLoading"
          >
            <option value="" disabled>{{ stringsLoading ? '线材加载中...' : '请选择线材' }}</option>
            <optgroup 
              v-for="(items, brand) in groupedStrings" 
              :key="brand" 
              :label="brand"
            >
              <option
                v-for="s in items"
                :key="s.id"
                :value="s.id"
                :disabled="s.stock === 0"
              >
                {{ s.model }}{{ s.price ? ` ¥${s.price}` : '' }}{{ s.stock === 0 ? ' (缺货)' : '' }}{{ s.stock > 0 && s.stock < 5 ? ' (紧)' : '' }}
              </option>
            </optgroup>
          </select>
        </div>

        <!-- 磅数 -->
        <div class="form-card">
          <label class="form-card__label" :for="'bm-tension-' + racket.id">磅数</label>
          <div class="input-group">
            <input
              class="input"
              :id="'bm-tension-' + racket.id"
              type="number"
              placeholder="输入磅数"
              v-model.number="racket.tension"
            />
            <span class="input-suffix">磅</span>
          </div>
        </div>

        <!-- 特殊要求 -->
        <div class="form-card">
          <label class="form-card__label" :for="'bm-notes-' + racket.id">特殊要求</label>
          <input
            class="input input--lg"
            :id="'bm-notes-' + racket.id"
            type="text"
            placeholder="如：加手胶、穿线方式"
            v-model="racket.notes"
          />
          <p class="form-card__hint">选填</p>
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
      <button class="btn--submit" @click="submitOrder" :disabled="submitting || stringsLoading">
        {{ submitting ? '提交中...' : '提交订单' }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getLastConfig, submitOrders, getShopStrings } from '@/api'

const route = useRoute()
const router = useRouter()

let nextId = 1

/** 模拟店铺映射表 (后续由接口配置或本地注入) */
const shopMap = {
  '1': '总店 (示例)',
  '2': '分店 A (示例)',
}

// 全局状态
const phone = ref('')
const phoneError = ref('')
const customerName = ref('')
const shopId = ref('') // 从 URL 获取
const shopName = ref('') // 显示用

// 线材状态
const strings = ref([])
const stringsLoading = ref(false)

const rackets = ref([createRacket()])
const submitting = ref(false)

/** 创建空球拍对象 */
function createRacket() {
  return reactive({
    id: nextId++,
    model: '',
    stringId: '', // 改为存 ID
    tension: null,
    notes: '',
  })
}

/** 计算属性：按品牌分组，组内按 sortOrder 升序 */
const groupedStrings = computed(() => {
  const groups = {}
  strings.value.forEach(s => {
    const brand = s.brand || '其他'
    if (!groups[brand]) groups[brand] = []
    groups[brand].push(s)
  })
  // 每个品牌组内按 sortOrder 升序排列
  Object.values(groups).forEach((items) => {
    items.sort((a, b) => (a.sortOrder ?? 999) - (b.sortOrder ?? 999))
  })
  return groups
})

/** 手机号校验 */
function validatePhone() {
  if (!phone.value) {
    phoneError.value = '请输入手机号'
    return false
  }
  if (!/^1[3-9]\d{9}$/.test(phone.value)) {
    phoneError.value = '请输入 11 位有效手机号'
    return false
  }
  phoneError.value = ''
  return true
}

/** 获取历史记录 */
async function fetchHistory() {
  if (!validatePhone()) return

  try {
    const data = await getLastConfig(phone.value)
    if (data) {
      if (data.name) customerName.value = data.name
      
      // 回填第一把球拍
      if (rackets.value.length > 0) {
        const first = rackets.value[0]
        if (data.model) first.model = data.model
        // 历史记录可能只存了型号名，这里尝试匹配 ID
        if (data.string) {
          const match = strings.value.find(s => s.model === data.string)
          if (match) first.stringId = match.id
          else first.stringId = '' // 没匹配上则清空，让用户重选
        }
        if (data.tension) first.tension = data.tension
        if (data.notes) first.notes = data.notes
      }
    }
  } catch (err) {
    console.warn('[Badminton] 未找到历史记录或接口异常:', err.message)
  }
}

/** 获取门店线材列表 */
async function fetchStrings(id) {
  if (!id) return
  stringsLoading.value = true
  try {
    // 调用真实接口
    const list = await getShopStrings(id)
    // 按 sortOrder 升序排列；若后端未返回该字段，回退到 999 排在末尾
    strings.value = (list || []).sort((a, b) => {
      return (a.sortOrder ?? 999) - (b.sortOrder ?? 999)
    })
  } catch (err) {
    window.showToast?.('加载线材失败，请刷新重试', 'error')
    console.error('[Badminton] 线材加载失败:', err)
  } finally {
    stringsLoading.value = false
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
  if (!validatePhone()) return
  if (!shopId.value) {
    window.showToast?.('无效的门店入口', 'error')
    return
  }

  const validRackets = rackets.value.filter((r) => r.model && r.model.trim())
  if (validRackets.length === 0) {
    window.showToast?.('请至少填写一把球拍的拍型', 'error')
    return
  }

  submitting.value = true
  try {
    const payload = validRackets.map((r) => ({
      phone: phone.value,
      name: customerName.value || '',
      shopId: Number(shopId.value), 
      model: r.model,
      stringId: r.stringId || null, // 传递 ID 给后端
      tension: r.tension || 0,
      notes: r.notes || '',
      type: 'badminton',
    }))

    await submitOrders(payload)
    router.push('/thank-you')
  } catch (err) {
    window.showToast?.(err.message || '提交失败，请重试', 'error')
  } finally {
    submitting.value = false
  }
}

/** 页面初始化 */
onMounted(() => {
  const savedPhone = localStorage.getItem('sy_phone')
  if (savedPhone) {
    phone.value = savedPhone
  }
  
  // 从 URL 获取店铺
  const idFromQuery = route.query.shopId || route.params.shopId
  if (idFromQuery) {
    shopId.value = String(idFromQuery)
    shopName.value = shopMap[shopId.value] || `门店 ${shopId.value}`
    
    // 拿到店铺后立即拉取线材
    fetchStrings(shopId.value)
  } else {
    shopName.value = '未识别门店'
  }
})
</script>

<style scoped>
/* 样式保持不变，确保工具感 */
.badminton-page {
  max-width: 480px;
  margin: 0 auto;
  background: #f3f4f6;
  min-height: 100vh;
  min-height: 100dvh;
  padding-bottom: calc(80px + env(safe-area-inset-bottom, 0px));
}
.top-info-bar {
  background: #fff;
  padding: 16px 16px 8px;
  border-bottom: 1px solid #e5e7eb;
}
.form-group { margin-bottom: 12px; }
.form-group__label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: #4b5563;
  margin-bottom: 8px;
}
.form-row { display: flex; gap: 10px; align-items: flex-start; }
.form-row .input { flex: 1; }
.shop-tag {
  background: #f0f4ff;
  border: 1px solid #bfdbfe;
  color: #1e3a8a;
  font-size: 14px;
  font-weight: 600;
  height: 44px;
  line-height: 44px;
  padding: 0 12px;
  border-radius: 4px;
}
.rackets-wrapper { padding-top: 8px; }
.racket-section { position: relative; }
.racket-section__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px 4px;
}
.racket-section__title { font-size: 14px; font-weight: 600; color: #111827; }
.racket-section__delete {
  width: 28px; height: 28px;
  display: flex; align-items: center; justify-content: center;
  border: none; background: #fee2e2; color: #ef4444;
  border-radius: 50%; font-size: 14px; cursor: pointer; transition: all 0.15s;
}
.racket-section__delete:active { transform: scale(0.9); background: #fecaca; }
.input {
  width: 100%; height: 44px;
  border: 1px solid #d1d5db; border-radius: 4px; padding: 0 12px;
  font-size: 14px; color: #111827; background: #fff; outline: none;
  transition: border-color 0.15s;
}
.input:focus { border-color: #1a56db; box-shadow: 0 0 0 2px rgba(26, 86, 219, 0.12); }
.input::placeholder { color: #9ca3af; }
.input--filled { background-color: #f0fdf4; border-color: #bbf7d0; }
.input--error { border-color: #d97706; background-color: #fffbeb; }
.input--lg { font-size: 14px; }
.form-card {
  background: #ffffff; border-radius: 6px; padding: 14px 16px;
  margin: 0 16px 12px; box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}
.form-card__label {
  display: block; font-size: 13px; font-weight: 500; color: #4b5563; margin-bottom: 8px;
}
.form-card__hint { font-size: 12px; color: #9ca3af; margin-top: 6px; }
.input-group {
  display: flex; align-items: center;
  border: 1px solid #d1d5db; border-radius: 4px; height: 44px; padding: 0 12px; background: #fff;
  transition: border-color 0.15s;
}
.input-group:focus-within { border-color: #1a56db; box-shadow: 0 0 0 2px rgba(26, 86, 219, 0.12); }
.input-group .input { border: none; padding: 0; height: 100%; box-shadow: none; }
.input-group .input:focus { box-shadow: none; }
.input-suffix { font-size: 13px; color: #9ca3af; margin-left: 4px; white-space: nowrap; }
.input-error-msg { font-size: 12px; color: #d97706; margin-top: 4px; }
.btn {
  display: inline-flex; align-items: center; justify-content: center;
  border: none; border-radius: 6px; font-weight: 600; cursor: pointer; transition: all 0.15s;
  white-space: nowrap;
}
.btn:active { transform: scale(0.98); }
.btn:disabled { background: #e5e7eb !important; color: #9ca3af !important; cursor: not-allowed; }
.btn--primary { background: #1a56db; color: #fff; }
.btn--primary:not(:disabled):active { background: #1e40af; }
.btn--sm { height: 36px; font-size: 13px; padding: 0 16px; }
.page-bottom-spacer { height: 16px; }
.bottom-action-bar {
  position: fixed; bottom: 0; left: 50%; transform: translateX(-50%);
  width: 100%; max-width: 480px; display: flex; gap: 12px; padding: 12px 16px;
  padding-bottom: calc(12px + env(safe-area-inset-bottom, 0px));
  background: rgba(255, 255, 255, 0.95); backdrop-filter: blur(8px);
  border-top: 1px solid #e5e7eb; z-index: 100;
}
.btn--add-racket {
  flex: 1; height: 48px; display: flex; align-items: center; justify-content: center; gap: 6px;
  border: 1px solid #d1d5db; border-radius: 6px; background: #fff; color: #4b5563;
  font-size: 14px; font-weight: 600; cursor: pointer; transition: all 0.15s;
}
.btn--add-racket:active { background: #f3f4f6; transform: scale(0.98); }
.btn--submit {
  flex: 2; height: 48px; border: none; border-radius: 6px;
  background: #1a56db; color: #fff; font-size: 15px; font-weight: 600;
  cursor: pointer; transition: all 0.15s;
}
.btn--submit:active { background: #1e40af; transform: scale(0.98); }
.btn--submit:disabled { background: #e5e7eb !important; color: #9ca3af !important; cursor: not-allowed; }
</style>
