import { request } from '@/utils/request'

// 分页查询登录日志（P5-2，仅管理员）
export const getLoginLogListApi = (params) => {
  return request.get('/loginLog/list', { params })
}
