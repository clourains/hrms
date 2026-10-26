import { request } from '@/utils/request'

// 分页查询审计日志（P1-1，仅管理员）
export const getAuditLogListApi = (params) => {
  return request.get('/auditLog/list', params)
}
