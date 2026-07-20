<template>
  <Teleport to="body">
    <Transition name="toast">
      <div v-if="visible" :class="['toast', `toast--${type}`]">
        <span v-if="type === 'loading'" class="toast-loading">
          <span class="toast-spinner"></span>
          {{ message }}
        </span>
        <span v-else>{{ message }}</span>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const visible = ref(false)
const message = ref('')
const type = ref('success') // success | error | loading
let timer = null

/**
 * 显示 Toast
 * @param {string} msg - 提示内容
 * @param {string} t - 类型
 * @param {number} duration - 持续时间(ms)，loading 类型不自动消失
 */
function showToast(msg, t = 'success', duration = 2000) {
  clearTimeout(timer)
  message.value = msg
  type.value = t
  visible.value = true

  if (t !== 'loading') {
    timer = setTimeout(() => {
      visible.value = false
    }, duration)
  }
}

/**
 * 隐藏 Toast
 */
function hideToast() {
  clearTimeout(timer)
  visible.value = false
}

// 暴露给外部使用
defineExpose({ showToast, hideToast })

// 全局挂载
onMounted(() => {
  window.showToast = showToast
  window.hideToast = hideToast
})
</script>

<style scoped>
.toast {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: rgba(0, 0, 0, 0.75);
  color: var(--text-white);
  padding: var(--space-base) var(--space-xl);
  border-radius: var(--radius-lg);
  font-size: var(--text-base);
  z-index: 9999;
  pointer-events: none;
  text-align: center;
  max-width: 80vw;
  min-width: 120px;
}

.toast--success {
  background: rgba(16, 185, 129, 0.92);
}

.toast--error {
  background: rgba(239, 68, 68, 0.92);
}

.toast-loading {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}

.toast-spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  flex-shrink: 0;
}

.toast-enter-active {
  animation: toastIn 0.25s ease forwards;
}

.toast-leave-active {
  animation: toastOut 0.2s ease forwards;
}

@keyframes toastIn {
  from {
    opacity: 0;
    transform: translate(-50%, -50%) scale(0.85);
  }
  to {
    opacity: 1;
    transform: translate(-50%, -50%) scale(1);
  }
}

@keyframes toastOut {
  from {
    opacity: 1;
    transform: translate(-50%, -50%) scale(1);
  }
  to {
    opacity: 0;
    transform: translate(-50%, -50%) scale(0.85);
  }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
