import { request } from '@/utils/request'

// 获取员工列表（全量）
export const getEmployeeListApi = (params) => {
  return request.get('/employee/list', params)
}

// 分页查询员工列表（后端为 POST /employee/page，body 为 EmployeeQueryDTO）
export const getEmployeePageApi = (data) => {
  return request.post('/employee/page', data)
}

// 获取员工详情
export const getEmployeeDetailApi = (id) => {
  return request.get(`/employee/${id}`)
}

// 新增员工
export const addEmployeeApi = (data) => {
  return request.post('/employee', data)
}

// 更新员工信息（后端为 PUT /employee，id 放在请求体里）
export const updateEmployeeApi = (data) => {
  return request.put('/employee', data)
}

// 删除员工
export const deleteEmployeeApi = (id) => {
  return request.delete(`/employee/${id}`)
}

// 批量删除员工（后端为 DELETE /employee/batch，body 为 id 数组）
export const batchDeleteEmployeeApi = (ids) => {
  return request.delete('/employee/batch', {}, { data: ids })
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
