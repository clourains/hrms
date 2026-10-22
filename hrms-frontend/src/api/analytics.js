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
  return request.get('/analytics/attendance', { params })
}

// ==================== 薪资分析 ====================

// 获取薪资分析
export const getSalaryAnalysisApi = (params) => {
  return request.get('/analytics/salary', { params })
}

// ==================== 绩效分析 ====================

// 获取绩效分析
export const getPerformanceAnalysisApi = (params) => {
  return request.get('/analytics/performance', { params })
}

// ==================== 人才预测 ====================

// 获取人才预测（离职率、高风险员工、晋升候选人、招聘需求、未来人员）
export const getTalentPredictionApi = () => {
  return request.get('/analytics/talent-prediction')
}

// ==================== AI洞察 ====================

// 获取AI智能洞察
export const getAiInsightsApi = () => {
  return request.get('/analytics/ai-insights')
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
