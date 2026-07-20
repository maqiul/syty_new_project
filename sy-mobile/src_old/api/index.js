import axios from 'axios'

/**
 * API 基础配置
 * 开发环境通过 Vite proxy 转发到后端
 * 生产环境直接使用 /api 前缀（需 Nginx 等配置代理）
 */
const api = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 请求拦截器
api.interceptors.request.use(
  (config) => {
    console.log(`[API] ${config.method?.toUpperCase()} ${config.url}`, config.data || '')
    return config
  },
  (error) => Promise.reject(error),
)

// 响应拦截器
api.interceptors.response.use(
  (response) => {
    const { data } = response
    if (data.code === 200) {
      return data.data
    }
    return Promise.reject(new Error(data.msg || '请求失败'))
  },
  (error) => {
    const message = error.response?.data?.msg || error.message || '网络异常'
    console.error('[API Error]', message)
    return Promise.reject(new Error(message))
  },
)

/**
 * 获取用户最近一次配置（历史记录）
 * @param {string} phone - 手机号
 * @returns {Promise<Object>} 历史订单数据
 */
export function getLastConfig(phone) {
  return api.get('/order/last-config', {
    params: { phone },
  })
}

/**
 * 提交订单数组
 * @param {Array<Object>} orders - 订单数组
 * @returns {Promise<Object>} 提交结果
 */
export function submitOrders(orders) {
  return api.post('/order/customer', orders)
}

/**
 * 获取指定门店的可用线材列表
 * @param {string|number} shopId - 门店 ID
 * @returns {Promise<Array>} 线材数组，包含 { id, brand, model, price, stock, sortOrder }
 */
export function getShopStrings(shopId) {
  return api.get('/strings', {
    params: { shopId },
  })
}

/**
 * 通过手机号或姓名查询订单列表（备用接口）
 * @param {Object} params - { mobile, oname }
 * @returns {Promise<Object>} 订单列表
 */
export function getOrdersByMobileOrName(params) {
  return api.post('/order/getOrderListByMobileOrName', params)
}

export default api
