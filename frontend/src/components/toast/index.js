/**
 * Premium Toast 控制器
 * API 与旧版 ui.js 中的 toast 完全兼容
 * 使用方法：import { toast } from '@/components/toast'
 */

import { createApp, h } from 'vue'
import ToastContainer from './ToastContainer.vue'

let containerInstance = null
let containerEl = null

/**
 * 懒加载初始化 Toast 容器
 */
function ensureContainer() {
    if (containerInstance) return containerInstance

    containerEl = document.createElement('div')
    containerEl.id = 'premium-toast-root'
    document.body.appendChild(containerEl)

    const app = createApp(ToastContainer)
    containerInstance = app.mount(containerEl)

    return containerInstance
}

/**
 * 显示 Toast
 * @param {Object} options
 * @param {string} options.message - 提示内容
 * @param {string} options.type - success | error | warning | info
 * @param {number} options.duration - 持续时间(ms)，0=不自动关闭
 */
function show(options) {
    const container = ensureContainer()
    return container.add(options)
}

/**
 * 关闭指定 Toast
 * @param {string} id
 */
function close(id) {
    if (containerInstance) {
        containerInstance.remove(id)
    }
}

/**
 * 关闭所有 Toast
 */
function closeAll() {
    if (containerInstance) {
        containerInstance.clear()
    }
}

/**
 * 销毁容器（通常在应用卸载时调用）
 */
function destroy() {
    if (containerInstance) {
        containerInstance.clear()
        const app = containerInstance.$?.appContext?.app
        if (app) {
            app.unmount()
        }
        containerInstance = null
    }
    if (containerEl && containerEl.parentNode) {
        containerEl.parentNode.removeChild(containerEl)
        containerEl = null
    }
}

// ===== 对外 API（与旧版完全兼容）=====
export const toast = {
    /** 成功提示（默认 3.2s） */
    success: (message, duration = 2200) =>
        show({ message, type: 'success', duration }),

    /** 错误提示（默认 5.2s） */
    error: (message, duration = 5200) =>
        show({ message, type: 'error', duration }),

    /** 警告提示（默认 3.2s） */
    warning: (message, duration = 2200) =>
        show({ message, type: 'warning', duration }),

    /** 信息提示（默认 3.2s） */
    info: (message, duration = 2200) =>
        show({ message, type: 'info', duration }),

    /** 关闭指定 toast */
    close,

    /** 关闭所有 toast */
    closeAll,

    /** 销毁容器 */
    destroy
}

// 默认导出兼容
export default toast