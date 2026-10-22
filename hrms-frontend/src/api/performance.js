import { request } from '@/utils/request'

// 获取绩效列表
export const getPerformanceListApi = (params) => {
  return request.get('/performance/list', params)
}

// 获取绩效详情
export const getPerformanceDetailApi = (id) => {
  return request.get(`/performance/${id}`)
}

// 新增绩效记录
export const addPerformanceApi = (data) => {
  return request.post('/performance', data)
}

// 更新绩效
export const updatePerformanceApi = (id, data) => {
  return request.put(`/performance/${id}`, data)
}

// 删除绩效记录
export const deletePerformanceApi = (id) => {
  return request.delete(`/performance/${id}`)
}

// 获取考核周期列表
export const getReviewCycleListApi = (params) => {
  return request.get('/performance/cycles', params)
}

// 创建考核周期
export const createReviewCycleApi = (data) => {
  return request.post('/performance/cycle', data)
}

// 提交自评
export const submitSelfReviewApi = (id, data) => {
  return request.put(`/performance/selfReview/${id}`, data)
}

// 提交上级评分
export const submitManagerReviewApi = (id, data) => {
  return request.put(`/performance/managerReview/${id}`, data)
}

// 绩效面谈
export const performanceInterviewApi = (id, data) => {
  return request.put(`/performance/interview/${id}`, data)
}

// 获取绩效指标配置
export const getPerformanceIndicatorsApi = () => {
  return request.get('/performance/indicators')
}

// 更新绩效指标
export const updatePerformanceIndicatorsApi = (data) => {
  return request.put('/performance/indicators', data)
}

// 获取绩效统计
export const getPerformanceStatsApi = (params) => {
  return request.get('/performance/stats', params)
}

// 导出绩效报表
export const exportPerformanceApi = (params) => {
  return request.download('/performance/export', params)
}
