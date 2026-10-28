import { request } from '@/utils/request'

// ========== Excel 批量导入 ==========
// module: customer / supplier / product / order / invoice / offline-deal
export const downloadImportTemplateApi = (module) => request.download(`/import/template/${module}`)

export const importExcelApi = (module, file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.upload(`/import/upload/${module}`, formData)
}

// 最近导入记录（审计用，不含具体业务字段）；source 可选 EXCEL / API
export const getImportLogsApi = (module, limit = 10, source) => request.get('/import/logs', { module, limit, source })

// ========== 开放接口密钥 ==========
export const getOpenApiCredentialApi = () => request.get('/open-api/credential')
export const rotateOpenApiKeyApi = () => request.post('/open-api/credential/rotate')
