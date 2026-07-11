import axios from 'axios'
import { getToken, removeToken } from '../utils/auth'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 60000
})

request.interceptors.request.use(config => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  response => {
    const data = response.data
    // Skip JSON code check for blob responses (file downloads)
    if (response.config.responseType === 'blob') {
      return data
    }
    if (data.code !== 200) {
      ElMessage.error(data.message || '请求失败')
      return Promise.reject(new Error(data.message))
    }
    return data
  },
  error => {
    if (error.response?.status === 401 || error.response?.status === 403) {
      removeToken()
      window.location.href = '/login'
      return Promise.reject(error)
    }
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default request
