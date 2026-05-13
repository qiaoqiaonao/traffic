/**
 * 统一轻提示与确认框
 * toast: 完全自定义的 Premium Toast（毛玻璃、动画、进度条）
 * confirmAction: Element Plus MessageBox 确认框
 */
import { ElMessageBox } from 'element-plus'
import { toast as premiumToast } from '@/components/toast'

/**
 * Toast 提示（Premium 自定义样式）
 * - success: 绿色渐变 + 对勾图标
 * - error: 红色渐变 + 错误图标，持续时间更长
 * - warning: 黄色渐变 + 警告图标
 * - info: 蓝色渐变 + 信息图标
 */
export const toast = premiumToast

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