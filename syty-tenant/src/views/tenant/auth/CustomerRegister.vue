<template>
  <div class="register-page">
    <div class="container">
      <div class="form-card">
        <!-- 头部 -->
        <div class="card-header">
          <div class="header-content">
            <div class="header-icon">🏸</div>
            <div class="header-text">
              <h1>羽毛球拍穿线登记</h1>
              <p class="sub-text">请填写准确的穿线信息，我们将为您提供专业服务</p>
            </div>
          </div>
        </div>

        <!-- 表单 -->
        <div class="card-body">
          <a-form :model="formData" layout="vertical" @finish="submitForm">
            <!-- 联系信息 -->
            <div class="section">
              <div class="section-header">
                <span class="section-icon">👤</span>
                <h2>联系信息</h2>
              </div>

              <a-form-item label="手机号码" name="phone" :rules="[{ required: true, message: '请输入手机号码' }, { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号' }]">
                <a-input v-model:value="formData.phone" placeholder="请输入手机号码" size="large">
                  <template #prefix><a-icon type="mobile" /></template>
                </a-input>
              </a-form-item>

              <a-form-item label="姓名" name="name" :rules="[{ required: true, message: '请输入姓名' }]">
                <a-input v-model:value="formData.name" placeholder="请输入您的姓名" size="large" />
              </a-form-item>
            </div>

            <!-- 拍子信息 -->
            <div class="section">
              <div class="section-header">
                <span class="section-icon">🏸</span>
                <h2>拍子信息</h2>
              </div>

              <a-form-item label="拍子品牌及型号" name="racketModel" :rules="[{ required: true, message: '请输入拍子型号' }]">
                <a-input v-model:value="formData.racketModel" placeholder="例如：YONEX ASTROX 88D" size="large" />
              </a-form-item>

              <a-form-item label="穿线型号" name="stringModel" :rules="[{ required: true, message: '请选择穿线型号' }]">
                <a-select v-model:value="formData.stringModel" placeholder="请选择穿线型号" size="large" allow-clear>
                  <a-select-option v-for="s in stringOptions" :key="s.id" :value="s.id">
                    {{ s.brand }} {{ s.model }}
                    <span v-if="s.price" style="color: #999">({{ s.price }}元)</span>
                  </a-select-option>
                </a-select>
              </a-form-item>

              <a-form-item label="磅数" name="tension" :rules="[{ required: true, message: '请输入磅数' }]">
                <a-input-number v-model:value="formData.tension" :min="18" :max="35" size="large" style="width: 100%" placeholder="请输入磅数（18-35）" />
              </a-form-item>
            </div>

            <!-- 其他信息 -->
            <div class="section">
              <div class="section-header">
                <span class="section-icon">📝</span>
                <h2>其他信息</h2>
              </div>

              <a-form-item label="特殊要求" name="remark">
                <a-textarea v-model:value="formData.remark" :rows="3" placeholder="如：双线双结、特殊穿线法等" size="large" />
              </a-form-item>

              <a-form-item label="取拍时间" name="pickupTime">
                <a-date-picker v-model:value="formData.pickupTime" size="large" style="width: 100%" placeholder="请选择取拍时间" />
              </a-form-item>
            </div>

            <!-- 提交按钮 -->
            <div class="submit-section">
              <a-button type="primary" html-type="submit" size="large" :loading="loading" block>
                <template #icon><a-icon type="check-circle" /></template>
                提交订单
              </a-button>
            </div>
          </a-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'antdv-next'
import axios from 'axios'

// 表单数据
const formData = reactive({
  phone: '',
  name: '',
  racketModel: '',
  stringModel: null as number | null,
  tension: null as number | null,
  remark: '',
  pickupTime: null as any,
  shopId: 1,
  shopName: ''
})

const stringOptions = ref<any[]>([])
const loading = ref(false)

// 获取球线列表
const fetchStrings = async () => {
  try {
    const res = await axios.get('/api/string-info/list')
    stringOptions.value = res.data.data || []
  } catch (e) {
    console.error('获取球线列表失败', e)
  }
}

// 提交表单
const submitForm = async () => {
  if (loading.value) return
  loading.value = true

  try {
    const payload = {
      playerName: formData.name,
      playerPhone: formData.phone,
      racketBrand: formData.racketModel.split(' ')[0] || '',
      racketModel: formData.racketModel,
      mainStringId: formData.stringModel,
      mainTension: formData.tension,
      remark: formData.remark,
      shopId: formData.shopId,
      shopName: formData.shopName,
      source: 'customer_register'
    }

    const res = await axios.post('/api/order/customer', payload)
    
    if (res.data.code === 200) {
      message.success('订单提交成功！我们将尽快为您穿线')
      // 重置表单
      Object.assign(formData, {
        phone: '',
        name: '',
        racketModel: '',
        stringModel: null,
        tension: null,
        remark: '',
        pickupTime: null,
        shopId: 1,
        shopName: ''
      })
    } else {
      message.error(res.data.msg || '提交失败，请重试')
    }
  } catch (e: any) {
    message.error('网络异常，请检查网络后重试')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchStrings()
  // 从URL参数获取shopId
  const params = new URLSearchParams(window.location.search)
  const sid = params.get('shopId')
  const sname = params.get('shopName')
  if (sid) formData.shopId = parseInt(sid)
  if (sname) formData.shopName = decodeURIComponent(sname)
})
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.container {
  max-width: 600px;
  margin: 0 auto;
}

.form-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.card-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 24px;
  color: #fff;
}

.header-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-icon {
  font-size: 40px;
}

.header-text h1 {
  font-size: 22px;
  margin: 0;
  font-weight: 600;
}

.sub-text {
  font-size: 14px;
  opacity: 0.9;
  margin: 4px 0 0;
}

.card-body {
  padding: 24px;
}

.section {
  margin-bottom: 24px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid #eee;
}

.section-icon {
  font-size: 18px;
}

.section-header h2 {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin: 0;
}

.submit-section {
  margin-top: 32px;
}

@media (max-width: 768px) {
  .container {
    padding: 0;
  }
  
  .form-card {
    border-radius: 0;
    margin: -20px;
  }
  
  .card-body {
    padding: 16px;
  }
}
</style>
