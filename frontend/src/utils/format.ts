const map: Record<string, { label: string; type: string }> = {
  DRAFT: { label: '草稿', type: 'info' },
  PENDING: { label: '审批中', type: 'warning' },
  APPROVED: { label: '已通过', type: 'success' },
  REJECTED: { label: '已驳回', type: 'danger' },
  ACTIVE: { label: '生效中', type: 'success' },
  EXPIRED: { label: '已到期', type: 'info' },
  CANCELLED: { label: '已取消', type: 'info' },
  OUTBOUND: { label: '已出库', type: 'primary' },
  DONE: { label: '已完成', type: 'success' },
  RECEIVED: { label: '已入库', type: 'success' },
  PARTIALLY_RECEIVED: { label: '部分入库', type: 'warning' },
  CONVERTED: { label: '已转化', type: 'success' },
  TODO: { label: '待开始', type: 'info' },
  DOING: { label: '进行中', type: 'warning' },
  NEW: { label: '新线索', type: 'info' },
  FOLLOWING: { label: '跟进中', type: 'warning' },
  QUALIFIED: { label: '已确认', type: 'success' },
  INVALID: { label: '无效', type: 'info' },
  INITIAL: { label: '初步接洽', type: 'info' },
  NEEDS: { label: '需求确认', type: 'primary' },
  SOLUTION: { label: '方案报价', type: 'warning' },
  QUOTE: { label: '商务谈判', type: 'warning' },
  WON: { label: '赢单', type: 'success' },
  LOST: { label: '输单', type: 'danger' },
  OPEN: { label: '进行中', type: 'primary' },
  PLAN: { label: '计划中', type: 'info' },
  SCHEDULED: { label: '已安排', type: 'primary' },
  NORMAL: { label: '正常', type: 'success' },
  LATE: { label: '迟到', type: 'warning' },
  EARLY: { label: '早退', type: 'warning' },
  ABSENT: { label: '缺勤', type: 'danger' },
  ANNUAL: { label: '年假', type: 'primary' },
  SICK: { label: '病假', type: 'warning' },
  PERSONAL: { label: '事假', type: 'info' },
  HIGH: { label: '高', type: 'danger' },
  LOW: { label: '低', type: 'info' },
  CUSTOMER: { label: '客户合同', type: 'primary' },
  SUPPLIER: { label: '供应商合同', type: 'warning' }
}

export function statusMeta(status: string) {
  if (!status) return { label: '-', type: 'info' }
  return map[status] || { label: status, type: 'info' }
}

export const priorityMeta: Record<string, { label: string; type: string }> = {
  HIGH: { label: '高', type: 'danger' },
  NORMAL: { label: '中', type: 'primary' },
  LOW: { label: '低', type: 'info' }
}

export function money(value: any) {
  const num = Number(value)
  if (!num && num !== 0) return '¥0.00'
  return '¥' + num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

export function amount(value: any) {
  const num = Number(value)
  return isNaN(num) ? 0 : num
}

export function dt(value: any) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}

export function d(value: any) {
  if (!value) return '-'
  return String(value).slice(0, 10)
}

export function percent(value: any) {
  if (value === null || value === undefined) return '-'
  return Number(value) + '%'
}
