import { request } from '@/utils/request'

// 获取薪资列表（后端为 GET /salary/listByMonth，参数 salaryMonth：yyyy-MM）
export const getSalaryListApi = (params) => {
  return request.get('/salary/listByMonth', params)
}

// 获取薪资详情
export const getSalaryDetailApi = (id) => {
  return request.get(`/salary/${id}`)
}

// 新增薪资记录
export const addSalaryApi = (data) => {
  return request.post('/salary', data)
}

// 更新薪资（后端为 PUT /salary，id 放在请求体里）
export const updateSalaryApi = (data) => {
  return request.put('/salary', data)
}

// 删除薪资记录
export const deleteSalaryApi = (id) => {
  return request.delete(`/salary/${id}`)
}

// 批量调薪
export const batchAdjustSalaryApi = (data) => {
  return request.post('/salary/batchAdjust', data)
}

// 获取薪资结构配置
export const getSalaryStructureApi = () => {
  return request.get('/salary/structure')
}

// 更新薪资结构
export const updateSalaryStructureApi = (data) => {
  return request.put('/salary/structure', data)
}

// 获取薪资统计（后端为 GET /salary/statistics，参数 salaryMonth：yyyy-MM）
export const getSalaryStatsApi = (params) => {
  return request.get('/salary/statistics', params)
}

// 一键计算月度薪资（P2-2，管理员）：考勤扣款 + 绩效奖金联动
export const calculateSalaryApi = (salaryMonth) => {
  return request.post('/salary/calculate', { salaryMonth })
}

// 一键计算并推送工资条（P4-4，管理员）：计算完成后站内信通知员工
export const calculateAndNotifyApi = (salaryMonth) => {
  return request.post('/salary/calculate-and-notify', { salaryMonth })
}

// 下载工资条 PDF（P2-3）：管理员任意员工，普通员工仅自己
export const downloadPayslipApi = (params) => {
  return request.download('/salary/payslip', params)
}

// 生成工资条
export const generatePayrollApi = (data) => {
  return request.post('/salary/generate', data)
}

// 导出薪资报表
export const exportSalaryApi = (params) => {
  return request.download('/salary/export', params)
}

// 获取我的薪资
export const getMySalaryApi = (params) => {
  return request.get('/salary/mySalary', params)
}
