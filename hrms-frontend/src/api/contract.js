import { request } from '@/utils/request'

// 合同列表（可按员工过滤）（P1-2）
export const getContractListApi = (params) => {
  return request.get('/contract/list', params)
}

// 新增合同
export const addContractApi = (data) => {
  return request.post('/contract', data)
}

// 修改合同
export const updateContractApi = (data) => {
  return request.put('/contract', data)
}

// 删除合同
export const deleteContractApi = (id) => {
  return request.delete(`/contract/${id}`)
}

// 到期提醒扫描（合同 + 签证，days 默认 60）
// 注意：request.get 的第二个参数就是 query 参数本体，不要包 { params: ... }，
// 否则会发成 ?params[days]=30，改了 CONTRACT_EXPIRE_DAYS 也不会生效。
export const getExpiringApi = (days = 60) => {
  return request.get('/contract/expiring', { days })
}
