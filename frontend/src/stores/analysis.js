import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getWsProgressUrl } from '@/config'
import { debugLog, debugError } from '@/utils/debug'

export const useAnalysisStore = defineStore('analysis', () => {
    const currentTask = ref(null)
    const progress = ref(0)
    const status = ref('idle')
    const ws = ref(null)
    const metrics = ref({
        frameCount: 0,
        totalCars: 0,
        fps: 0
    })

    const isAnalyzing = computed(() => status.value === 'processing' || status.value === 'uploading')
    const canCancel = computed(() => isAnalyzing.value)

    const setTask = (task) => {
        currentTask.value = task
    }

    const updateProgress = (data) => {
        progress.value = data.progress || progress.value
        if (data.frameCount !== undefined) metrics.value.frameCount = data.frameCount
        if (data.totalCars !== undefined) metrics.value.totalCars = data.totalCars
        if (data.fps !== undefined) metrics.value.fps = data.fps
        if (data.status) {
            status.value = data.status
        }
    }

    const initWebSocket = (taskId, onMessage) => {
        const wsUrl = getWsProgressUrl(taskId)
        ws.value = new WebSocket(wsUrl)

        ws.value.onopen = () => {
            debugLog('[WebSocket] connected')
        }

        ws.value.onmessage = (event) => {
            const data = JSON.parse(event.data)
            updateProgress(data)
            if (onMessage) onMessage(data)
        }

        ws.value.onerror = (error) => {
            debugError('[WebSocket] error:', error)
        }

        ws.value.onclose = () => {
            debugLog('[WebSocket] closed')
            ws.value = null
        }

        return ws.value
    }

    const closeWebSocket = () => {
        if (ws.value) {
            ws.value.close()
            ws.value = null
        }
    }

    const reset = () => {
        currentTask.value = null
        progress.value = 0
        status.value = 'idle'
        metrics.value = { frameCount: 0, totalCars: 0, fps: 0 }
        closeWebSocket()
    }

    return {
        currentTask,
        progress,
        status,
        ws,
        metrics,
        isAnalyzing,
        canCancel,
        setTask,
        updateProgress,
        initWebSocket,
        closeWebSocket,
        reset
    }
})