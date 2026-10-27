/**
 * 消息快速通道：按消息类型/标题映射到处理页面
 * 后端现有自动消息类型：审批（ApprovalServiceImpl）、薪资（工资条推送）；系统为兜底类型不跳转
 */
export function messageJumpTarget(msg) {
  if (!msg) return null
  const title = msg.title || ''
  if (msg.msgType === '审批') {
    // 通知审批人处理的→待我审批；通知申请人结果的→我的申请
    return title.includes('待处理')
      ? { path: '/approval', query: { tab: 'todo' } }
      : { path: '/approval', query: { tab: 'mine' } }
  }
  if (msg.msgType === '薪资') return { path: '/salary' }
  return null
}

export function messageJumpLabel(msg) {
  if (!msg) return ''
  if (msg.msgType === '审批') return (msg.title || '').includes('待处理') ? '去审批' : '查看进度'
  if (msg.msgType === '薪资') return '查看工资条'
  return ''
}
