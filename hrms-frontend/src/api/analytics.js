import { request } from '@/utils/request'

// ==================== 仪表盘 ====================

// 获取仪表盘统计数据
export const getDashboardStatsApi = () => {
  return request.get('/analytics/dashboard')
}

// ==================== 人员结构分析 ====================

// 获取人员结构分析（性别、年龄、学历、部门、职位、工龄）
export const getEmployeeStructureApi = () => {
  return request.get('/analytics/employee-structure')
}

// ==================== 考勤分析 ====================

// 获取考勤分析
export const getAttendanceAnalysisApi = (params) => {
  return request.get('/analytics/attendance', params)
}

// ==================== 薪资分析 ====================

// 获取薪资分析
export const getSalaryAnalysisApi = (params) => {
  return request.get('/analytics/salary', params)
}

// ==================== 绩效分析 ====================

// 获取绩效分析
export const getPerformanceAnalysisApi = (params) => {
  return request.get('/analytics/performance', params)
}

// ==================== 人才预测 ====================

// 获取人才预测（离职率、高风险员工、晋升候选人、招聘需求、未来人员）
export const getTalentPredictionApi = () => {
  return request.get('/analytics/talent-prediction')
}

// ==================== AI洞察 ====================

// 获取智能洞察
// generate=false：规则引擎计算（本地、免费、毫秒级）
// generate=true：调用大模型生成（用户主动触发，结果后端缓存 10 分钟）
export const getAiInsightsApi = (generate = false) => {
  return request.get('/analytics/ai-insights', { generate })
}

// 入职离职趋势（真实统计，months=3/6/12）
// 注意：request.get 的第二个参数就是 query 参数本体，不要再包一层 { params: ... }，
// 否则会发成 ?params[months]=6，后端收不到而永远吃默认值 12。
export const getTurnoverTrendApi = (months = 12) => {
  return request.get('/analytics/turnover-trend', { months })
}

// ==================== 招聘分析 ====================

// 获取招聘分析
export const getRecruitmentAnalysisApi = () => {
  return request.get('/analytics/recruitment')
}

// ==================== 培训分析 ====================

// 获取培训分析
export const getTrainingAnalysisApi = () => {
  return request.get('/analytics/training')
}

// 首页最新动态：真实的入职、离职、审批记录，按发生时间倒序
export const getRecentActivitiesApi = (limit = 6) => {
  return request.get('/analytics/recent-activities', { limit })
}
