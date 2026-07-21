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
        <h1>平台管理</h1>
        <p>三益穿线 · 超管后台</p>
      </div>

      <a-form :model="form" :rules="rules" ref="formRef" layout="vertical">
        <a-form-item name="username">
          <a-input v-model:value="form.username" placeholder="管理员账号" size="large">
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
    </div>
    <div class="login-footer">SYTY Platform v2.0</div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'antdv-next'
import { UserOutlined, LockOutlined } from '@antdv-next/icons'
import { useUserStore } from '@/store/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules: Record<string, any[]> = {
  username: [{ required: true, message: '请输入管理员账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin() {
  try {
    await formRef.value?.validate()
  } catch { return }

  loading.value = true
  try {
    await userStore.login({ username: form.username, password: form.password })
    message.success('登录成功')
    const redirect = (route.query.redirect as string) || '/dashboard'
    router.push(redirect)
  } catch (e: any) {
    message.error(e.message || '登录失败')
  } finally {
    loading.value = false
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
}
.login-card {
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.15);
}
.login-header {
  text-align: center;
  margin-bottom: 32px;
}
.login-logo {
  width: 64px;
  height: 64px;
  margin-bottom: 16px;
}
.login-header h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: #111827;
}
.login-header p {
  margin: 8px 0 0;
  font-size: 13px;
  color: #6B7280;
}
.login-footer {
  margin-top: 24px;
  font-size: 12px;
  color: rgba(255,255,255,0.6);
}
</style>
