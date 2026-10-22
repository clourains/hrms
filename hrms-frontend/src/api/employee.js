import { request } from '@/utils/request'

// 获取员工列表（分页）
export const getEmployeeListApi = (params) => {
  return request.get('/employee/list', params)
}

// 获取员工详情
export const getEmployeeDetailApi = (id) => {
  return request.get(`/employee/${id}`)
}

// 新增员工
export const addEmployeeApi = (data) => {
  return request.post('/employee', data)
}

// 更新员工信息
export const updateEmployeeApi = (id, data) => {
  return request.put(`/employee/${id}`, data)
}

// 删除员工
export const deleteEmployeeApi = (id) => {
  return request.delete(`/employee/${id}`)
}

// 批量删除员工
export const batchDeleteEmployeeApi = (ids) => {
  return request.post('/employee/batchDelete', { ids })
}

// 导入员工
export const importEmployeeApi = (formData) => {
  return request.upload('/employee/import', formData)
}

// 导出员工
export const exportEmployeeApi = (params) => {
  return request.download('/employee/export', params)
}

// 获取员工下拉选项
export const getEmployeeOptionsApi = (params) => {
  return request.get('/employee/options', params)
}
