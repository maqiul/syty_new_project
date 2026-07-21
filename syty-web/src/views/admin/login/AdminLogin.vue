<template>
  <div class="admin-login-container">
    <div class="login-card">
      <div class="login-header">
        <h1 class="login-title">系统管理后台</h1>
        <p class="login-subtitle">Admin Portal</p>
      </div>
      
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical" @finish="handleLogin" @finishFailed="handleFailed">
        <a-form-item name="username">
          <a-input v-model:value="form.username" placeholder="请输入管理员账号" size="large" allow-clear>
            <template #prefix><UserOutlined /></template>
          </a-input>
        </a-form-item>

        <a-form-item name="password">
          <a-input-password v-model:value="form.password" placeholder="请输入管理员密码" size="large" @keyup.enter="handleLogin">
            <template #prefix><LockOutlined /></template>
          </a-input-password>
        </a-form-item>

        <a-form-item>
          <a-button type="primary" size="large" block :loading="loading" html-type="submit">
            登 录
          </a-button>
        </a-form-item>
      </a-form>

      <div class="login-footer">
        <a-button type="link" @click="$router.push('/login')">切换到门店登录</a-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { message, type FormInstance } from 'antdv-next'
import { UserOutlined, LockOutlined } from '@antdv-next/icons'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
})

const rules = {
  username: [{ required: true, message: '请输入管理员账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const handleLogin = async () => {
  loading.value = true
  try {
    await userStore.adminLogin(form)
    message.success('登录成功')
    router.push('/dashboard')
  } catch (err) {
    message.error('登录失败，请检查账号密码')
  } finally {
    loading.value = false
  }
}

const handleFailed = (errorInfo: any) => {
  console.log('Failed:', errorInfo)
}
</script>

<style scoped>
.admin-login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0a1628 0%, #1a365d 100%);
}

.login-card {
  width: 420px;
  padding: 40px;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.login-title {
  font-size: 28px;
  font-weight: 700;
  color: #1a365d;
  margin: 0 0 8px;
}

.login-subtitle {
  font-size: 14px;
  color: #888;
  margin: 0;
  letter-spacing: 2px;
}

.login-footer {
  text-align: center;
  margin-top: 16px;
}
</style>
