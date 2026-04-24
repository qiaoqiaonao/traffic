/**
 * 统一轻提示与确认框（位置、动效、文案风格一致）
 */
import { ElMessage, ElMessageBox } from 'element-plus'

const toastBase = {
  showClose: true,
  grouping: true,
  offset: 72,
  duration: 3200
}

export const toast = {
  success: (message) =>
    ElMessage({ ...toastBase, message, type: 'success' }),
  error: (message) =>
    ElMessage({ ...toastBase, message, type: 'error', duration: 5200 }),
  warning: (message) =>
    ElMessage({ ...toastBase, message, type: 'warning' }),
  info: (message) =>
    ElMessage({ ...toastBase, message, type: 'info' })
}

/**
 * 危险操作确认（居中、圆角按钮）
 */
export function confirmAction (message, title = '请确认', options = {}) {
  return ElMessageBox.confirm(message, title, {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
    center: true,
    roundButton: true,
    draggable: true,
    customClass: 'traffic-dialog-premium',
    ...options
  })
}
