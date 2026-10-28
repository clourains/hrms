import { request } from '@/utils/request'

// ========== 入职登记（入离管理之「入」）==========
export const getOnboardPageApi = (data) => request.post('/employee-onboard/page', data)
export const getOnboardStatisticsApi = () => request.get('/employee-onboard/statistics')
export const addOnboardApi = (data) => request.post('/employee-onboard', data)
export const updateOnboardApi = (data) => request.put('/employee-onboard', data)
// 关联已有员工档案
export const linkOnboardApi = (id, employeeId) => request.put(`/employee-onboard/${id}/link/${employeeId}`, {})
// 按登记信息新建员工档案 + 试用期记录
export const createProfileApi = (id) => request.post(`/employee-onboard/${id}/create-profile`, {})
export const deleteOnboardApi = (id) => request.delete(`/employee-onboard/${id}`)
