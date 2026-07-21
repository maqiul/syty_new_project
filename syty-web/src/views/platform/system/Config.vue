<template>
  <div class="system-config">
    <a-card :bordered="false">
      <template #title>
        <div class="card-header">
          <span>系统全局配置</span>
          <span class="subtitle">配置平台品牌、登录页行为及限制参数</span>
        </div>
      </template>

      <a-form :model="config" layout="vertical" style="max-width: 700px; margin-top: 20px;">
        
        <!-- 板块 1: 基础信息 -->
        <h3 class="section-title">🎨 品牌基础设置</h3>
        <a-row :gutter="24">
          <a-col :span="16">
            <a-form-item label="平台名称">
              <a-input v-model:value="config.platformName" placeholder="例如：舜羽 SaaS 管理平台" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="平台 Logo">
              <a-input v-model:value="config.logoUrl" placeholder="图片 URL" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="24">
          <a-col :span="12">
            <a-form-item label="网站图标 (Favicon)">
              <a-input v-model:value="config.faviconUrl" placeholder="ico/png 地址" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="页脚备案号 (ICP)">
              <a-input v-model:value="config.icpLicense" placeholder="例如：京 ICP 备 12345678 号" />
            </a-form-item>
          </a-col>
        </a-row>

        <!-- 板块 2: 租户策略 -->
        <h3 class="section-title">👥 租户与登录策略</h3>
        <a-row :gutter="24">
          <a-col :span="24">
            <a-form-item label="登录/注册页背景图 URL">
              <a-input v-model:value="config.loginBgUrl" placeholder="https://..." />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="24">
          <a-col :span="8">
            <a-form-item label="允许租户自助注册">
              <a-switch v-model:checked="config.enableSelfRegister" checked-children="开启" un-checked-children="关闭" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="新租户默认试用天数">
              <a-input-number v-model:value="config.defaultTrialDays" :min="0" :step="1" style="width: 100%" addon-after="天" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="官方客服邮箱">
              <a-input v-model:value="config.supportEmail" placeholder="support@syty.com" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-divider />
        <a-form-item>
          <a-button type="primary" @click="handleSave" :loading="saving" size="large">
            <template #icon><SaveOutlined /></template>
            保存全部配置
          </a-button>
          <a-button @click="loadConfig" style="margin-left: 10px">重置</a-button>
        </a-form-item>
      </a-form>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'antdv-next'
import { SaveOutlined } from '@antdv-next/icons'
import request from '@/utils/axios'

interface SysConfig {
  id?: number
  platformName?: string
  logoUrl?: string
  faviconUrl?: string
  loginBgUrl?: string
  enableSelfRegister?: boolean
  defaultTrialDays?: number
  supportEmail?: string
  icpLicense?: string
  configVersion?: number
}

const config = ref<SysConfig>({})
const saving = ref(false)

const loadConfig = async () => {
  try {
    const res = await request.get('/platform/config')
    config.value = res.data || {}
  } catch {}
}

const handleSave = async () => {
  saving.value = true
  try {
    await request.put('/platform/config', config.value)
    message.success('配置已更新，新版本号已生效')
    loadConfig()
  } finally { saving.value = false }
}

onMounted(loadConfig)
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.subtitle { font-size: 12px; color: #999; font-weight: normal; }
.section-title { font-size: 15px; font-weight: 600; margin: 10px 0 15px; padding-left: 8px; border-left: 4px solid #1890ff; color: #333; }
.system-config { padding: 16px; background: #f5f7fa; min-height: 100vh; }
</style>
