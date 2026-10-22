import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { loginApi, getUserInfoApi, logoutApi } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  // state
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || '{}'))

  // getters
  const isLoggedIn = computed(() => !!token.value)
  const username = computed(() => userInfo.value.username || '')
  const avatar = computed(() => userInfo.value.avatar || '')
  const role = computed(() => userInfo.value.role || '')

  // actions
  const login = async (loginForm) => {
    try {
      const res = await loginApi(loginForm)
      if (res.code === 200) {
        token.value = res.data.token
        localStorage.setItem('token', res.data.token)
        // 获取用户信息
        await fetchUserInfo()
        return res
      }
      return res
    } catch (error) {
      throw error
    }
  }

  const fetchUserInfo = async () => {
    try {
      const res = await getUserInfoApi()
      if (res.code === 200) {
        userInfo.value = res.data
        localStorage.setItem('userInfo', JSON.stringify(res.data))
      }
      return res
    } catch (error) {
      throw error
    }
  }

  const logout = async () => {
    try {
      await logoutApi()
    } catch (error) {
      console.error('登出接口调用失败', error)
    } finally {
      token.value = ''
      userInfo.value = {}
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
    }
  }

  const setToken = (newToken) => {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  const clearUser = () => {
    token.value = ''
    userInfo.value = {}
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    username,
    avatar,
    role,
    login,
    fetchUserInfo,
    logout,
    setToken,
    clearUser
  }
})
