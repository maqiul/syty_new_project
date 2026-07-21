<template>
  <div class="not-found">
    <h1>404</h1>
    <p>页面不存在</p>
    <a-button type="primary" @click="$router.push('/dashboard')">返回工作台</a-button>
  </div>
</template>

<style scoped>
.not-found {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background: #f0f2f5;
}
h1 { font-size: 72px; color: #bbb; margin-bottom: 8px; }
p { font-size: 18px; color: #999; margin-bottom: 24px; }
</style>
