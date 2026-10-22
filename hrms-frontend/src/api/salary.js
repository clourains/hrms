import { request } from '@/utils/request'

// 获取薪资列表
export const getSalaryListApi = (params) => {
  return request.get('/salary/list', params)
}

// 获取薪资详情
export const getSalaryDetailApi = (id) => {
  return request.get(`/salary/${id}`)
}

// 新增薪资记录
export const addSalaryApi = (data) => {
  return request.post('/salary', data)
}

// 更新薪资
export const updateSalaryApi = (id, data) => {
  return request.put(`/salary/${id}`, data)
}

// 删除薪资记录
export const deleteSalaryApi = (id) => {
  return request.delete(`/salary/${id}`)
}

// 批量调薪
export const batchAdjustSalaryApi = (data) => {
  return request.post('/salary/batchAdjust', data)
}

// 获取薪资结构配置
export const getSalaryStructureApi = () => {
  return request.get('/salary/structure')
}

// 更新薪资结构
export const updateSalaryStructureApi = (data) => {
  return request.put('/salary/structure', data)
}

// 获取薪资统计
export const getSalaryStatsApi = (params) => {
  return request.get('/salary/stats', params)
}

// 生成工资条
export const generatePayrollApi = (data) => {
  return request.post('/salary/generate', data)
}

// 导出薪资报表
export const exportSalaryApi = (params) => {
  return request.download('/salary/export', params)
}

// 获取我的薪资
export const getMySalaryApi = (params) => {
  return request.get('/salary/mySalary', params)
}
