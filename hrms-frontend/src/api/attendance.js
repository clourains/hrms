import { request } from '@/utils/request'

// 获取考勤列表
export const getAttendanceListApi = (params) => {
  return request.get('/attendance/list', params)
}

// 获取考勤详情
export const getAttendanceDetailApi = (id) => {
  return request.get(`/attendance/${id}`)
}

// 打卡
export const checkInApi = (data) => {
  return request.post('/attendance/checkIn', data)
}

// 签退
export const checkOutApi = (data) => {
  return request.post('/attendance/checkOut', data)
}

// 提交请假申请（P4-2）：走通用审批流，年假类型审批通过后自动扣减额度
export const submitLeaveApplyApi = (data) => {
  return request.post('/approval/apply', data)
}

// ==================== P4-1 加班申请 ====================

// 提交加班申请：创建加班单并发起审批，通过后计入薪资加班费
export const applyOvertimeApi = (data) => {
  return request.post('/attendance/overtime/apply', data)
}

// 我的加班申请列表
export const getMyOvertimeApi = () => {
  return request.get('/attendance/overtime/my')
}

// 加班申请列表（管理端：admin 全部 / manager 本部门）
export const getOvertimeListApi = () => {
  return request.get('/attendance/overtime/list')
}

// ==================== P4-2 请假额度 ====================

// 我的本年度假期额度（年假）
export const getMyLeaveQuotaApi = () => {
  return request.get('/attendance/leave-quota/my')
}

// 额度列表（管理端：admin 全员 / manager 本部门）
export const getLeaveQuotaListApi = (year) => {
  return request.get('/attendance/leave-quota/list', { year })
}

// 获取考勤统计（后端为 GET /attendance/statistics，参数 empId、month）
export const getAttendanceStatsApi = (params) => {
  return request.get('/attendance/statistics', params)
}

// 获取月度考勤报表
export const getMonthlyReportApi = (params) => {
  return request.get('/attendance/monthlyReport', params)
}

// 导出考勤报表
export const exportAttendanceApi = (params) => {
  return request.download('/attendance/export', params)
}
