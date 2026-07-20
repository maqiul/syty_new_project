<template>
  <div class="thank-you-page">
    <div class="thank-you-icon">✅</div>
    <h1 class="thank-you-title">提交成功！</h1>
    <p class="thank-you-desc">
      您的穿线订单已成功提交<br />
      请耐心等待，我们会尽快处理
    </p>
    <div class="thank-you-actions">
      <button class="btn btn--primary btn--lg btn--block" @click="goHome">
        返回首页
      </button>
      <button class="btn btn--outline btn--lg btn--block mt-base" @click="goBadminton">
        🏸 继续登记羽毛球
      </button>
      <button class="btn btn--outline btn--lg btn--block mt-base" @click="goTennis">
        🎾 继续登记网球
      </button>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

function goHome() {
  router.push('/badminton')
}

function goBadminton() {
  router.push('/badminton')
}

function goTennis() {
  router.push('/tennis')
}
</script>

<style scoped>
.thank-you-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 80vh;
  text-align: center;
  padding: var(--space-2xl);
}

.thank-you-icon {
  font-size: 64px;
  margin-bottom: var(--space-xl);
  animation: bounceIn 0.5s ease;
}

.thank-you-title {
  font-size: var(--text-3xl);
  font-weight: var(--font-bold);
  color: var(--color-success);
  margin-bottom: var(--space-base);
}

.thank-you-desc {
  font-size: var(--text-base);
  color: var(--text-secondary);
  margin-bottom: var(--space-2xl);
  line-height: var(--leading-relaxed);
}

.thank-you-actions {
  width: 100%;
  max-width: 320px;
}

@keyframes bounceIn {
  0% { opacity: 0; transform: scale(0.3); }
  50% { transform: scale(1.05); }
  70% { transform: scale(0.95); }
  100% { opacity: 1; transform: scale(1); }
}
</style>
