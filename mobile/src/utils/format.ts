const STATUS_MAP: Record<string, { label: string; cls: string }> = {
  DRAFT: { label: '草稿', cls: 'gray' },
  PENDING: { label: '审批中', cls: 'warn' },
  APPROVED: { label: '已通过', cls: 'ok' },
  REJECTED: { label: '已驳回', cls: 'danger' },
  ACTIVE: { label: '生效中', cls: 'ok' },
  EXPIRED: { label: '已到期', cls: 'gray' },
  OUTBOUND: { label: '已出库', cls: 'ok' },
  DONE: { label: '已完成', cls: 'ok' },
  CANCELLED: { label: '已取消', cls: 'gray' },
  RECEIVED: { label: '已入库', cls: 'ok' },
  PARTIALLY_RECEIVED: { label: '部分入库', cls: 'warn' },
  CONVERTED: { label: '已转化', cls: 'ok' },
  TODO: { label: '待办', cls: 'warn' },
  DOING: { label: '进行中', cls: 'warn' },
  NEW: { label: '新线索', cls: 'warn' },
  FOLLOWING: { label: '跟进中', cls: 'warn' },
  QUALIFIED: { label: '已确认', cls: 'ok' },
  INVALID: { label: '无效', cls: 'gray' },
  INITIAL: { label: '初步接洽', cls: 'gray' },
  NEEDS: { label: '需求确认', cls: 'warn' },
  SOLUTION: { label: '方案报价', cls: 'warn' },
  QUOTE: { label: '商务谈判', cls: 'warn' },
  WON: { label: '赢单', cls: 'ok' },
  LOST: { label: '输单', cls: 'danger' },
  OPEN: { label: '进行中', cls: 'warn' },
  PLAN: { label: '计划中', cls: 'gray' },
  SCHEDULED: { label: '已安排', cls: 'ok' },
  NORMAL: { label: '正常', cls: 'ok' },
  LATE: { label: '迟到', cls: 'warn' },
  EARLY: { label: '早退', cls: 'warn' },
  ABSENT: { label: '缺勤', cls: 'danger' },
  HIGH: { label: '高', cls: 'danger' },
  LOW: { label: '低', cls: 'gray' },
  CUSTOMER: { label: '客户', cls: '' },
  SUPPLIER: { label: '供应商', cls: 'warn' }
}

export function statusMeta(status?: string) {
  if (!status) return { label: '-', cls: 'gray' }
  return STATUS_MAP[status] || { label: status, cls: 'gray' }
}

export function money(v: any) {
  const n = Number(v || 0)
  return '¥' + n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

export function moneyShort(v: any) {
  const n = Number(v || 0)
  if (n >= 100000000) return '¥' + (n / 100000000).toFixed(2) + '亿'
  if (n >= 10000) return '¥' + (n / 10000).toFixed(2) + '万'
  return '¥' + n.toFixed(0)
}

export function dt(v?: string) {
  if (!v) return '-'
  return String(v).replace('T', ' ').slice(0, 16)
}

export function d(v?: string) {
  if (!v) return '-'
  return String(v).slice(0, 10)
}

export function priorityMeta(p?: string) {
  if (p === 'HIGH') return { label: '高优先级', cls: 'danger' }
  if (p === 'LOW') return { label: '低优先级', cls: 'gray' }
  return { label: '普通', cls: '' }
}
