import { request } from '@/utils/request'

// 个人资料：账号 + 员工档案 + 部门（P3-2）
export const getSelfProfileApi = () => {
  return request.get('/self/profile')
}

// 我的薪资记录（仅本人）
export const getSelfSalaryApi = () => {
  return request.get('/self/salary')
}

// 我的某月考勤：统计 + 明细
export const getSelfAttendanceApi = (month) => {
  return request.get('/self/attendance', { month })
}

// 今日打卡状态
export const getSelfAttendanceTodayApi = () => {
  return request.get('/self/attendance-today')
}

// 我的本年度假期额度（P4-2，年假）
export const getSelfLeaveQuotaApi = () => {
  return request.get('/self/leave-quota')
}
