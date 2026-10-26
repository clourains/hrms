import { request } from '@/utils/request'

// 发起审批（P3-1）：approvers 为逐级审批人用户名数组
export const applyApprovalApi = (data) => {
  return request.post('/approval/apply', data)
}

// 通过当前节点
export const approveApprovalApi = (id, comment) => {
  return request.post(`/approval/approve/${id}`, { comment })
}

// 驳回
export const rejectApprovalApi = (id, comment) => {
  return request.post(`/approval/reject/${id}`, { comment })
}

// 申请人撤销
export const cancelApprovalApi = (id) => {
  return request.post(`/approval/cancel/${id}`)
}

// 我发起的审批
export const getMyApplyApi = () => {
  return request.get('/approval/my-apply')
}

// 待我审批
export const getApprovalTodoApi = () => {
  return request.get('/approval/todo')
}

// 我已处理的审批
export const getApprovalDoneApi = () => {
  return request.get('/approval/done')
}

// 审批详情
export const getApprovalDetailApi = (id) => {
  return request.get(`/approval/detail/${id}`)
}

// 审批人候选列表
export const getApproverOptionsApi = () => {
  return request.get('/approval/approvers')
}
