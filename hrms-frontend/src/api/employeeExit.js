import { request } from '@/utils/request'

// ========== 入离管理：离职 / 辞退 ==========
// kind: 1 离职、2 辞退
export const getExitPageApi = (data) => request.post('/employee-exit/page', data)
export const getExitStatisticsApi = (kind) => request.get('/employee-exit/statistics', { kind })
export const addExitApi = (data) => request.post('/employee-exit', data)
export const updateExitApi = (data) => request.put('/employee-exit', data)
export const approveExitApi = (id, data = {}) => request.put(`/employee-exit/${id}/approve`, data)
export const rejectExitApi = (id, data = {}) => request.put(`/employee-exit/${id}/reject`, data)
// 生效：员工置离职 + 写离职日期 + 停用登录账号
export const effectExitApi = (id) => request.put(`/employee-exit/${id}/effect`, {})
export const cancelExitApi = (id) => request.put(`/employee-exit/${id}/cancel`, {})
export const deleteExitApi = (id) => request.delete(`/employee-exit/${id}`)
