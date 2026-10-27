import { request } from '@/utils/request'

// 生效中的公告（首页展示，所有登录用户可用）
export const getActiveNoticesApi = () => request.get('/notice/active')

// 标记公告已读
export const readNoticeApi = (id) => request.post(`/notice/read/${id}`)

// 管理端分页列表
export const getNoticeListApi = (params) => request.get('/notice/list', params)

// 发布公告
export const createNoticeApi = (data) => request.post('/notice', data)

// 编辑/上线下线
export const updateNoticeApi = (data) => request.put('/notice', data)

// 删除
export const deleteNoticeApi = (id) => request.delete(`/notice/${id}`)

// 已读统计
export const getNoticeReadStatsApi = (id) => request.get(`/notice/readStats/${id}`)
