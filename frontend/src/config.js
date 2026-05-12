/**
 * Centralized backend URLs. Set VITE_BACKEND_URL / VITE_WS_URL env vars for deployment.
 */
export const BACKEND_BASE_URL = import.meta.env.VITE_BACKEND_URL || 'http://localhost:8080'
export const WS_BASE_URL = import.meta.env.VITE_WS_URL || 'ws://localhost:8080'

export function getVideoStreamUrl(taskId) {
  return `${BACKEND_BASE_URL}/api/video/result/${taskId}/stream?t=${Date.now()}`
}

export function getWsProgressUrl(taskId) {
  return `${WS_BASE_URL}/ws/progress?taskId=${taskId}`
}

// Python AI service URL (for HLS streaming, direct access)
export const PYTHON_BASE_URL = import.meta.env.VITE_PYTHON_URL || 'http://localhost:8000'

export function getHlsStreamUrl(taskId) {
  return `${PYTHON_BASE_URL}/api/analyze/hls/${taskId}/stream.m3u8`
}
