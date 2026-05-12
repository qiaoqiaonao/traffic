/**
 * Task status helpers — single source of truth for all views.
 */
const STATUS_CLASS_MAP = { 0: 'pending', 1: 'processing', 2: 'success', 3: 'error' }
const STATUS_TEXT_MAP = { 0: '等待处理', 1: '正在分析', 2: '分析完成', 3: '分析失败' }
const STATUS_TYPE_MAP = { 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' }

export function getStatusClass(status) {
  return STATUS_CLASS_MAP[status] || 'pending'
}

export function getStatusText(status) {
  return STATUS_TEXT_MAP[status] || '未知状态'
}

export function getStatusType(status) {
  return STATUS_TYPE_MAP[status] || 'info'
}
