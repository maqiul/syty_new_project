import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/badminton',
  },
  {
    path: '/badminton',
    name: 'Badminton',
    component: () => import('@/views/Badminton.vue'),
    meta: { title: '羽毛球穿线登记' },
  },
  {
    path: '/tennis',
    name: 'Tennis',
    component: () => import('@/views/Tennis.vue'),
    meta: { title: '网球穿线登记' },
  },
  {
    path: '/thank-you',
    name: 'ThankYou',
    component: () => import('@/views/ThankYou.vue'),
    meta: { title: '提交成功' },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

// 全局前置守卫：设置页面标题
router.beforeEach((to) => {
  if (to.meta.title) {
    document.title = to.meta.title
  }
})

export default router
