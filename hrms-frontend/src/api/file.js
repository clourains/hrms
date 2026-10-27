import { request } from '@/utils/request'

/**
 * 简历等附件上传
 * 文件落在后端本地目录，返回 { path, name, size, type }
 *   path: 相对路径，随候选人一起提交（resumeUrl）
 *   type: image / pdf，前端据此决定预览方式
 */
export const uploadFileApi = (formData) => {
  return request.upload('/file/upload', formData)
}

/**
 * 读取附件二进制（必须带 token，所以不能直接用 <img src="/api/...">）
 * 返回浏览器可直接使用的 blob URL，用完记得 revoke
 */
export const createFileBlobUrlApi = async (path) => {
  const res = await request.download('/file/view', { path })
  const blob = res.data instanceof Blob ? res.data : new Blob([res.data])
  return URL.createObjectURL(blob)
}

/** 下载附件，文件名还原成上传时的原始文件名 */
export const downloadFileApi = async (path, name) => {
  const res = await request.download('/file/download', { path, name })
  const blob = res.data instanceof Blob ? res.data : new Blob([res.data])
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = name || '简历'
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  // 稍后再释放，避免部分浏览器下载尚未开始就失效
  setTimeout(() => URL.revokeObjectURL(url), 5000)
}
