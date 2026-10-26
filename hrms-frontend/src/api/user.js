import { request } from '@/utils/request'

// 分页查询用户列表（P5-1，仅管理员）
export const getUserListApi = (params) => {
  return request.get('/user/list', params)
}

// 新建用户
export const createUserApi = (data) => {
  return request.post('/user', data)
}

// 更新用户（昵称/角色/绑定员工/状态）
export const updateUserApi = (data) => {
  return request.put('/user', data)
}

// 删除用户
export const deleteUserApi = (id) => {
  return request.delete(`/user/${id}`)
}

// 重置密码（不传 password 则重置为默认 123456）
export const resetUserPasswordApi = (id, password) => {
  return request.post(`/user/resetPassword/${id}`, password ? { password } : {})
}
