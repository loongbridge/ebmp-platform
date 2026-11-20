import axios from 'axios'
import { clearToken, getApiBase, getToken } from '../utils/native'

const http = axios.create({ timeout: 15000 })

http.interceptors.request.use((config) => {
  const base = getApiBase()
  config.baseURL = base ? `${base}/api` : '/api'
  const token = getToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use(
  (res) => {
    const body: any = res.data
    if (body && typeof body.code !== 'undefined') {
      if (body.code === 0) return body.data
      if (body.code === 401) {
        clearToken()
        location.hash = '#/login'
      }
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body
  },
  (err) => {
    const status = err.response?.status
    if (status === 401) {
      clearToken()
      location.hash = '#/login'
      return Promise.reject(new Error('登录已过期，请重新登录'))
    }
    const msg =
      err.response?.data?.message ||
      (err.code === 'ECONNABORTED'
        ? '请求超时，请检查网络或服务器地址'
        : err.response
          ? err.message
          : '无法连接服务器，请检查网络或服务器地址')
    return Promise.reject(new Error(msg || '网络异常，请检查服务器地址'))
  }
)

export default http
