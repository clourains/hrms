import { request } from '@/utils/request'

// 获取部门列表（树形结构）
export const getDepartmentTreeApi = () => {
  return request.get('/department/tree')
}

// 获取部门列表（扁平化）
export const getDepartmentListApi = (params) => {
  return request.get('/department/list', params)
}

// 获取部门详情
export const getDepartmentDetailApi = (id) => {
  return request.get(`/department/${id}`)
}

// 新增部门
export const addDepartmentApi = (data) => {
  return request.post('/department', data)
}

// 更新部门（后端为 PUT /department，id 放在请求体里）
export const updateDepartmentApi = (data) => {
  return request.put('/department', data)
}

// 删除部门
export const deleteDepartmentApi = (id) => {
  return request.delete(`/department/${id}`)
}

// 获取部门下拉选项
export const getDepartmentOptionsApi = () => {
  return request.get('/department/options')
}

// 获取部门员工数量统计
export const getDepartmentStatsApi = () => {
  return request.get('/department/stats')
}
