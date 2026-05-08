import axios from 'axios'

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
        // 自动附加 token
        const token = localStorage.getItem('token')
        if (token) {
            config.headers.Authorization = `Bearer ${token}`
        }
        console.log(`[API] ${config.method?.toUpperCase()} ${config.url}`)
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
        console.error('[API Error]', error)
        // 401 未授权时跳转登录页
        if (error.response?.status === 401) {
            localStorage.removeItem('token')
            localStorage.removeItem('username')
            localStorage.removeItem('nickname')
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

// 视频分析相关
export const analyzeVideo = (file, frameSkip = 3, detectionLines = null) => {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('frameSkip', frameSkip)
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

// 图片检测
export const analyzeFrame = (file) => {
    const formData = new FormData()
    formData.append('file', file)

    return api.post('/traffic/analyze/frame', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
    })
}

// 获取任务结果
export const getResult = (taskId) => {
    return api.get(`/traffic/result/${taskId}`)
}

// 获取详细结果（包含帧数据）
export const getDetailedResult = (taskId) => {
    return api.get(`/traffic/result/${taskId}/detail`)
}

// 获取视频流 URL（用于 video 标签 src
export const getVideoStreamUrl = (taskId) => {
    // 使用 SpringBoot 的 VideoController 提供的流式接口
    /**
     * ！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！
     * ！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！
     * ！！！！！这里很重要，要是不指定8080端口，系统会默认直接访问调用端口，也就是前端端口3000，不会正确拿到视频流！！！！！！
     */
    return `http://localhost:8080/api/video/result/${taskId}/stream?t=${Date.now()}`

}

// 下载视频
export const downloadVideo = (taskId) => {
    return api.get(`/api/video/result/${taskId}`, {
        responseType: 'blob'
    })
}


export default api