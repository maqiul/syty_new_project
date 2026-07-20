<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <svg class="login-logo" viewBox="0 0 48 48" fill="none">
          <ellipse cx="24" cy="16" rx="13" ry="5" stroke="#4F46E5" stroke-width="2.5"/>
          <line x1="24" y1="21" x2="24" y2="36" stroke="#4F46E5" stroke-width="2.5" stroke-linecap="round"/>
          <path d="M16 26 C20 22, 28 22, 32 26" stroke="#4F46E5" stroke-width="2.5" stroke-linecap="round"/>
          <path d="M11 21 C18 16, 30 16, 37 21" stroke="#A5B4FC" stroke-width="1.5" stroke-linecap="round"/>
          <path d="M13 30 C19 26, 29 26, 35 30" stroke="#A5B4FC" stroke-width="1.5" stroke-linecap="round"/>
        </svg>
        <h1>三益穿线</h1>
        <p>专业羽毛球 · 网球穿线管理系统</p>
      </div>

      <!-- 角色切换 Tabs -->
      <a-segmented v-model:value="loginMode" :options="loginModeOptions" block class="login-mode-tabs" />

      <a-form :model="form" :rules="rules" ref="formRef" layout="vertical">
        <!-- 员工模式：门店编码（必填） -->
        <a-form-item v-show="loginMode === 'employee'" name="tenantCode">
          <a-input v-model:value="form.tenantCode" placeholder="门店编码" size="large">
            <template #prefix><ApartmentOutlined /></template>
          </a-input>
        </a-form-item>

        <a-form-item name="username">
          <a-input v-model:value="form.username" placeholder="用户名" size="large">
            <template #prefix><UserOutlined /></template>
          </a-input>
        </a-form-item>
        <a-form-item name="password">
          <a-input-password v-model:value="form.password" placeholder="密码" size="large">
            <template #prefix><LockOutlined /></template>
          </a-input-password>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" :loading="loading" block size="large" @click="handleLogin">
            {{ loading ? '验证中...' : '登 录' }}
          </a-button>
        </a-form-item>
      </a-form>

      <div class="login-actions">
        <a @click="showResetModal">忘记密码 / 重置密码</a>
      </div>
    </div>
    <div class="login-footer">SYTY v2.0</div>

    <!-- 重置密码弹窗 -->
    <a-modal v-model:open="resetVisible" title="重置密码" @ok="handleReset" ok-text="重置" cancel-text="取消">
      <p style="margin-bottom: 8px; color: #666;">请输入需要重置密码的账号用户名：</p>
      <a-input v-model:value="resetUsername" placeholder="例如：user111" size="large" />
      <p style="margin-top: 12px; color: #999; font-size: 12px;">重置后的密码将为：<code style="color: #4F46E5;">用户名@123!@#</code></p>
    </a-modal>

    <!-- V2.0 门店选择弹窗 -->
    <a-modal
      v-model:open="shopModalVisible"
      title="选择门店"
      :closable="false"
      :mask-closable="false"
      @ok="confirmShopSelection"
      ok-text="确认"
      cancel-text="取消"
      :cancel-button-props="{ style: { display: 'none' } }"
    >
      <p style="margin-bottom: 12px; color: #666;">当前账号关联了多个门店，请选择您要进入的门店：</p>
      <a-radio-group v-model:value="selectedShop" style="width: 100%;">
        <a-space direction="vertical" style="width: 100%;">
          <a-radio
            v-for="shop in shopList"
            :key="shop.shopCode"
            :value="shop"
            class="shop-radio-item"
          >
            <span class="shop-name">{{ shop.shopName }}</span>
            <span class="shop-code">{{ shop.shopCode }}</span>
          </a-radio>
        </a-space>
      </a-radio-group>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message, type FormInstance } from 'antdv-next'
import { UserOutlined, LockOutlined, ApartmentOutlined } from '@antdv-next/icons'
import { tenantLogin, adminLogin, resetPassword } from '@/api'
import { useUserStore, type AvailableShop } from '@/store/user'

// ============ 登录模式 ============
type LoginMode = 'employee' | 'admin'

const loginMode = ref<LoginMode>('employee')
const loginModeOptions = [
  { label: '我是员工', value: 'employee' },
  { label: '我是管理员', value: 'admin' },
]

// ============ 表单 ============
const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const loading = ref(false)
const resetVisible = ref(false)
const resetUsername = ref('')

// ============ V2.0 门店选择弹窗 ============
const shopModalVisible = ref(false)
const shopList = ref<AvailableShop[]>([])
const selectedShop = ref<AvailableShop | null>(null)

const form = reactive({
  tenantCode: '',
  username: '',
  password: ''
})

// 动态规则：仅员工模式下门店编码必填
const rules = computed(() => {
  const base = {
    username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
    password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  }
  if (loginMode.value === 'employee') {
    return {
      ...base,
      tenantCode: [{ required: true, message: '请输入门店编码', trigger: 'blur' }],
    }
  }
  return base
})

/** 确认选择门店 */
function confirmShopSelection() {
  if (!selectedShop.value) {
    message.warning('请选择一个门店')
    return
  }
  userStore.selectShop(selectedShop.value)
  shopModalVisible.value = false
  // 跳转到目标页
  const redirect = (route.query.redirect as string) || '/dashboard'
  router.push(redirect)
}

/**
 * 登录处理：
 * - 员工模式：调用 tenantLogin，Header 携带 X-Tenant-Code
 * - 管理员模式：调用 adminLogin，不携带租户 Header
 * - 成功后调用 fetchShops()，根据门店数量分支处理
 */
async function handleLogin() {
  try {
    await formRef.value?.validateFields()
  } catch {
    return
  }

  loading.value = true
  try {
    let res: any

    if (loginMode.value === 'employee') {
      // 员工登录：必须传 tenantCode
      res = await tenantLogin(
        { username: form.username, password: form.password },
        form.tenantCode
      )
    } else {
      // 管理员登录：不传 tenantCode
      res = await adminLogin({ username: form.username, password: form.password })
    }

    // 后端返回格式：{ code: 200, data: { token, tokenName, userId, username } }
    const { token, username } = res.data

    // store.loginSuccess 内部会存储 token 并调用 fetchPermissions 拉取用户信息和菜单
    await userStore.loginSuccess(token)

    // ---- V2.0 门店管控逻辑 ----
    try {
      const shops = await userStore.fetchShops()

      if (shops.length === 0) {
        // 情况 A：0 个门店
        message.error('当前无可用门店，请联系管理员')
        userStore.logout()
        return
      }

      if (shops.length === 1) {
        // 情况 B：1 个门店，自动选中
        userStore.selectShop(shops[0])
        message.success(`欢迎回来，${username}！`)
      } else {
        // 情况 C：>1 个门店，弹窗选择
        message.success(`欢迎回来，${username}！请选择门店`)
        shopList.value = shops
        selectedShop.value = shops[0] // 默认选中第一个
        shopModalVisible.value = true
        return // 不跳转，等用户选择
      }
    } catch (shopErr: any) {
      // fetchShops 抛出的错误（如 0 个门店）
      message.error(shopErr.message || '获取门店列表失败')
      userStore.logout()
      return
    }

    // 跳转到目标页（或默认首页）
    let redirect = (route.query.redirect as string) || '/dashboard'

    // 超管或无租户用户 → 强制跳平台管理看板，避免跳到门店首页报 403
    if (userStore.isSuperAdmin || userStore.tenantId === null) {
      redirect = '/dashboard'
    }

    router.push(redirect)
  } catch (err: any) {
    console.error('Login failed:', err)
    message.error(err.response?.data?.msg || err.message || '登录失败，请重试')
  } finally {
    loading.value = false
  }
}

function showResetModal() {
  resetVisible.value = true
  resetUsername.value = form.username
}

async function handleReset() {
  if (!resetUsername.value) {
    message.warning('请输入用户名')
    return
  }
  try {
    await resetPassword(resetUsername.value)
    message.success('密码已重置为: 用户名@123!@#')
    resetVisible.value = false
  } catch {
    message.error('重置失败，请稍后重试')
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}
.login-card {
  width: 100%;
  max-width: 400px;
  background: #fff;
  border-radius: 16px;
  padding: 40px 32px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}
.login-header {
  text-align: center;
  margin-bottom: 24px;
}
.login-logo {
  width: 48px;
  height: 48px;
  margin-bottom: 12px;
}
.login-header h1 {
  margin: 0 0 4px;
  font-size: 24px;
  color: #1a1a2e;
}
.login-header p {
  margin: 0;
  font-size: 14px;
  color: #999;
}
.login-mode-tabs {
  margin-bottom: 24px;
}
.login-actions {
  text-align: center;
  margin-top: 8px;
}
.login-footer {
  margin-top: 20px;
  color: rgba(255, 255, 255, 0.6);
  font-size: 12px;
}
:deep(.ant-segmented) {
  background: #f0f0f0;
  border-radius: 8px;
}
:deep(.ant-segmented-item) {
  border-radius: 6px;
}
:deep(.ant-segmented-item-selected) {
  background: #fff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  font-weight: 500;
}
:deep(.ant-input-affix-wrapper),
:deep(.ant-segmented-item-label) {
  border-radius: 8px;
}
.shop-radio-item {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  border-radius: 8px;
  transition: background-color 0.2s;
}
.shop-radio-item:hover {
  background-color: #f5f5f5;
}
.shop-name {
  font-weight: 500;
  color: #333;
}
.shop-code {
  margin-left: 8px;
  font-size: 12px;
  color: #999;
}
</style>
