import { createApp } from 'vue'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import './styles/global.css'
import App from './App.vue'
import router, { setUserStore as setRouterStore, addDynamicRoutes } from './router'
import { createPinia } from 'pinia'
import { registerDirectives } from './directives'
import { useUserStore } from './store/user'
import { setUserStore as setAxiosStore } from './utils/axios'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)

// 初始化 store 并注入到 router 和 axios
const userStore = useUserStore()
setRouterStore(userStore)
setAxiosStore(userStore)

app.use(router)
app.use(Antd)

// 注册自定义指令（v-hasPermi / v-hasRole）
registerDirectives(app)

// 页面刷新时：如果有 token，先恢复权限，再动态生成路由
if (userStore.token) {
  // 静默刷新权限 + 动态路由（不阻塞页面渲染）
  userStore.fetchPermissions()
    .then(() => {
      addDynamicRoutes()
    })
    .catch(() => {
      // 权限刷新失败，token 可能已过期，会被 axios 拦截器处理
    })
}

app.mount('#app')
