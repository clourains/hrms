import { request } from '@/utils/request'

// 登录
export const loginApi = (data) => {
  return request.post('/auth/login', data)
}

// 登出
export const logoutApi = () => {
  return request.post('/auth/logout')
}

// 获取用户信息
export const getUserInfoApi = () => {
  return request.get('/auth/userInfo')
}

// 刷新 token
export const refreshTokenApi = () => {
  return request.post('/auth/refreshToken')
}

// 修改密码
export const changePasswordApi = (data) => {
  return request.put('/auth/password', data)
}

// 获取验证码
export const getCaptchaApi = () => {
  return request.get('/auth/captcha')
}
