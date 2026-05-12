import axios from 'axios'
import { getToken } from '@/composables/useAuth'
import { clearAuth } from '@/composables/useAuth'
import { getVideoStreamUrl as buildStreamUrl } from '@/config'

const api = axios.create({
    baseURL: '/api',
    timeout: 30000,
    headers: {
        'Content-Type': 'application/json'
    }
})

// 请求拦截器
api.interceptors.request.use(
    (config) => {
        const token = getToken()
        if (token) {
            config.headers.Authorization = `Bearer ${token}`
        }
        return config
    },
    (error) => {
        return Promise.reject(error)
    }
)

// 响应拦截器
api.interceptors.response.use(
    (response) => {
        return response
    },
    (error) => {
        if (error.response?.status === 401) {
            clearAuth()
            if (window.location.pathname !== '/login' && window.location.pathname !== '/register') {
                window.location.href = '/login?redirect=' + encodeURIComponent(window.location.pathname)
            }
        }
        return Promise.reject(error)
    }
)

// ========== 认证相关 ==========
export const login = (username, password) => {
    return api.post('/auth/login', { username, password })
}

export const register = (username, password, nickname) => {
    return api.post('/auth/register', { username, password, nickname })
}

export const getCurrentUser = () => {
    return api.get('/auth/me')
}

export const logoutApi = () => {
    return api.post('/auth/logout')
}

// ========== 视频分析相关 ==========
export const analyzeVideo = (file, frameSkip = 3, detectionLines = null, metersPerPixel = 0.05) => {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('frameSkip', frameSkip)
    formData.append('metersPerPixel', metersPerPixel)
    if (detectionLines) {
        formData.append('detectionLines', detectionLines)
    }
    return api.post('/traffic/analyze', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
    })
}

export const cancelTask = (taskId) => {
    return api.post(`/traffic/cancel/${taskId}`)
}

export const getHistory = (params = {}) => {
    const { page = 1, pageSize = 10, status = null } = params
    return api.get('/traffic/history', {
        params: { page, pageSize, status }
    })
}

// ========== 图片检测 ==========
export const analyzeFrame = (file) => {
    const formData = new FormData()
    formData.append('file', file)
    return api.post('/traffic/analyze/frame', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
    })
}

// ========== 结果查询 ==========
export const getResult = (taskId) => {
    return api.get(`/traffic/result/${taskId}`)
}

export const getDetailedResult = (taskId) => {
    return api.get(`/traffic/result/${taskId}/detail`)
}

export const getVideoStreamUrl = (taskId) => {
    return buildStreamUrl(taskId)
}

export const downloadVideo = (taskId) => {
    return api.get(`/api/video/result/${taskId}`, {
        responseType: 'blob'
    })
}

export default api
