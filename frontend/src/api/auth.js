import axios from 'axios'

// 根据环境选择 API 基础 URL
const getBaseURL = () => {
  // 如果有配置的生产环境 API URL，则使用它
  if (process.env.VUE_APP_API_BASE_URL) {
    return process.env.VUE_APP_API_BASE_URL + '/api'
  }
  // 否则使用代理（开发环境和生产环境都可以）
  return '/api'
}

const api = axios.create({
  baseURL: getBaseURL(),
  timeout: 10000
})

// Request interceptor to add auth token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Response interceptor to handle errors
api.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error.response?.status === 401) {
      // Token expired or invalid - clear all auth data
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('user')
      localStorage.removeItem('tokenExpiresAt')
      // 跳转到登录页
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    // 提取错误信息，优先使用 error 字段（具体错误），其次使用 message
    const errorData = error.response?.data
    let errorMessage = '操作失败'
    if (errorData) {
      errorMessage = errorData.error || errorData.message || '操作失败'
    }
    const enhancedError = new Error(errorMessage)
    enhancedError.response = error.response
    enhancedError.data = errorData
    return Promise.reject(enhancedError)
  }
)

export const authApi = {
  register(data) {
    return api.post('/auth/register', data)
  },

  login(data) {
    return api.post('/auth/login', data)
  },

  sendCode(data) {
    return api.post('/auth/send-code', data)
  },

  loginWithCode(data) {
    return api.post('/auth/login-with-code', data)
  },

  logout() {
    return api.post('/auth/logout')
  }
}

export default api
