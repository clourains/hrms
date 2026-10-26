import { request } from '@/utils/request'

// 收件箱（当前登录人，可按类型/关键字过滤）（P2-1）
export const getMessageInboxApi = (params) => {
  return request.get('/message/inbox', params)
}

// 未读数（顶部铃铛角标）
export const getUnreadCountApi = () => {
  return request.get('/message/unread-count')
}

// 标记单条已读
export const markMessageReadApi = (id) => {
  return request.put(`/message/read/${id}`)
}

// 全部已读
export const readAllMessagesApi = () => {
  return request.put('/message/read-all')
}

// 删除消息
export const deleteMessageApi = (id) => {
  return request.delete(`/message/${id}`)
}

// 可选收件人列表（管理员）
export const getMessageUsersApi = () => {
  return request.get('/message/users')
}

// 发送站内信（管理员），receivers: ['all'] 或用户名数组
export const sendMessageApi = (data) => {
  return request.post('/message/send', data)
}
