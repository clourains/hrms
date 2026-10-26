import { request } from '@/utils/request'

// 按考核周期查询绩效列表（后端为 GET /performance/listByPeriod，参数 period：yyyy-MM）
export const getPerformanceListApi = (params) => {
  return request.get('/performance/listByPeriod', params)
}

// 获取绩效详情
export const getPerformanceDetailApi = (id) => {
  return request.get(`/performance/${id}`)
}

// 新增绩效记录
export const addPerformanceApi = (data) => {
  return request.post('/performance', data)
}

// 更新绩效（后端为 PUT /performance，id 放在请求体里）
export const updatePerformanceApi = (data) => {
  return request.put('/performance', data)
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

// 绩效评估（后端为 POST /performance/evaluate，总分与等级由后端计算）
export const evaluatePerformanceApi = (data) => {
  return request.post('/performance/evaluate', data)
}

// 获取绩效统计（后端为 GET /performance/statistics，参数 period：yyyy-MM）
export const getPerformanceStatsApi = (params) => {
  return request.get('/performance/statistics', params)
}

// 导出绩效报表
export const exportPerformanceApi = (params) => {
  return request.download('/performance/export', params)
}
