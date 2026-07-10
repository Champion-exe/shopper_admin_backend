import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.request.use(config => {
  const userStr = localStorage.getItem('user') || localStorage.getItem('adminUser')
  if (userStr) {
    try {
      const data = JSON.parse(userStr)
      const token = data.token || ''
      if (token) {
        config.headers.Authorization = token
      }
    } catch (e) {}
  }
  return config
})

request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code === 401) {
      localStorage.removeItem('user')
      localStorage.removeItem('adminUser')
      const isAdmin = response.config.url?.includes('/admin/')
      window.location.href = isAdmin ? '/admin/login' : '/auth/login'
      return Promise.reject(new Error(res.msg))
    }
    if (res.code === 501) {
      ElMessage.warning(res.msg || '功能开发中')
    }
    return res
  },
  error => {
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default request
