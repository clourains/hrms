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

// 补卡申请
export const applyMakeupApi = (data) => {
  return request.post('/attendance/makeup', data)
}

// 请假申请
export const applyLeaveApi = (data) => {
  return request.post('/attendance/leave', data)
}

// 审批考勤
export const approveAttendanceApi = (id, data) => {
  return request.put(`/attendance/approve/${id}`, data)
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
