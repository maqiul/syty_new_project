import axios from 'axios'

const request = axios.create({
  baseURL: '',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

request.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const message = error.response?.data?.message || error.message || '请求失败'
    uni.showToast({ title: message, icon: 'none' })
    return Promise.reject(error)
  }
)

export default request
