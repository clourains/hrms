import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import router from '@/router'

// 创建 axios 实例
const service = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  }
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    const token = userStore.token
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response) => {
    // 文件下载（blob）不走业务码校验，直接放行
    if (response.config.responseType === 'blob') {
      return response
    }
    const res = response.data
    // 根据后端约定的状态码处理
    if (res.code !== 200 && res.code !== 0) {
      // 业务错误
      ElMessage.error(res.message || '请求失败')

      // token 过期或未授权
      // P045：403 是权限不足，保留登录态只提示，只有 401 才登出
      if (res.code === 401) {
        const userStore = useUserStore()
        userStore.clearUser()
        router.push('/login')
      }

      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  (error) => {
    console.error('响应错误:', error)

    if (error.response) {
      const status = error.response.status
      const userStore = useUserStore()

      switch (status) {
        case 401:
          ElMessage.error('登录已过期，请重新登录')
          userStore.clearUser()
          router.push('/login')
          break
        case 403:
          ElMessage.error('没有权限访问')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          ElMessage.error('服务器内部错误')
          break
        default:
          ElMessage.error(error.response.data?.message || '网络错误')
      }
    } else if (error.request) {
      ElMessage.error('网络连接失败，请检查网络')
    } else {
      ElMessage.error('请求配置错误')
    }

    return Promise.reject(error)
  }
)

// 封装请求方法
export const request = {
  get(url, params = {}, config = {}) {
    return service.get(url, { params, ...config })
  },
  post(url, data = {}, config = {}) {
    return service.post(url, data, config)
  },
  put(url, data = {}, config = {}) {
    return service.put(url, data, config)
  },
  delete(url, params = {}, config = {}) {
    return service.delete(url, { params, ...config })
  },
  // 文件上传
  upload(url, formData, config = {}) {
    return service.post(url, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      ...config
    })
  },
  // 文件下载
  download(url, params = {}, config = {}) {
    return service.get(url, {
      params,
      responseType: 'blob',
      ...config
    })
  }
}

export default service
