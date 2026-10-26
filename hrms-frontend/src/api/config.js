import { request } from '@/utils/request'

// 查询全部系统参数（P5-3，仅管理员）
export const getConfigListApi = () => {
  return request.get('/config/list')
}

// 更新系统参数
export const updateConfigApi = (configKey, configValue) => {
  return request.put('/config/update', { configKey, configValue })
}
