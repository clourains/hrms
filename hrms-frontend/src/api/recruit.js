import { request } from '@/utils/request'

// ========== 招聘需求 ==========
export const getRequisitionPageApi = (data) => request.post('/recruit/requisition/page', data)
// 在招需求下拉（候选人登记时选岗位）
export const getRequisitionListApi = () => request.get('/recruit/requisition/list')
export const getRecruitStatisticsApi = () => request.get('/recruit/requisition/statistics')
export const addRequisitionApi = (data) => request.post('/recruit/requisition', data)
export const updateRequisitionApi = (data) => request.put('/recruit/requisition', data)
export const deleteRequisitionApi = (id) => request.delete(`/recruit/requisition/${id}`)
// status: 1招聘中 2已暂停 3已完成 4已关闭
export const changeRequisitionStatusApi = (id, status) => request.put(`/recruit/requisition/${id}/status/${status}`)

// ========== 候选人 ==========
export const getCandidatePageApi = (data) => request.post('/recruit/candidate/page', data)
export const getCandidateFunnelApi = () => request.get('/recruit/candidate/funnel')
export const addCandidateApi = (data) => request.post('/recruit/candidate', data)
export const updateCandidateApi = (data) => request.put('/recruit/candidate', data)
export const deleteCandidateApi = (id) => request.delete(`/recruit/candidate/${id}`)
// 手工调整阶段，淘汰时必须带 reason
export const changeCandidateStageApi = (id, data) => request.put(`/recruit/candidate/${id}/stage`, data)
// 录用入职：生成员工档案 + 试用期记录
export const hireCandidateApi = (id, data) => request.post(`/recruit/candidate/${id}/hire`, data)

// ========== 面试安排 ==========
export const getInterviewPageApi = (data) => request.post('/recruit/interview/page', data)
export const getInterviewsByCandidateApi = (candidateId) => request.get(`/recruit/interview/candidate/${candidateId}`)
export const addInterviewApi = (data) => request.post('/recruit/interview', data)
export const updateInterviewApi = (data) => request.put('/recruit/interview', data)
export const deleteInterviewApi = (id) => request.delete(`/recruit/interview/${id}`)
// result: 1通过 2未通过 3爽约
export const submitInterviewResultApi = (id, data) => request.put(`/recruit/interview/${id}/result`, data)

// ========== 试用期转正 ==========
export const getProbationPageApi = (data) => request.post('/recruit/probation/page', data)
// params 可选，用于传 expireDays 指定到期提醒天数
export const getProbationStatisticsApi = (params = {}) => request.get('/recruit/probation/statistics', params)
export const addProbationApi = (data) => request.post('/recruit/probation', data)
export const updateProbationApi = (data) => request.put('/recruit/probation', data)
export const deleteProbationApi = (id) => request.delete(`/recruit/probation/${id}`)
export const confirmProbationApi = (id, data) => request.put(`/recruit/probation/${id}/confirm`, data)
export const failProbationApi = (id, data) => request.put(`/recruit/probation/${id}/fail`, data)
export const extendProbationApi = (id, data) => request.put(`/recruit/probation/${id}/extend`, data)
