<template>
  <div class="analyze-page">
    <!-- 步骤指示器 -->
    <div class="steps-header" v-if="currentStep > 0">
      <div
          v-for="(step, idx) in steps"
          :key="idx"
          :class="['step', { active: currentStep === idx + 1, completed: currentStep > idx + 1 }]"
      >
        <div class="step-number">{{ idx + 1 }}</div>
        <div class="step-label">{{ step }}</div>
        <div class="step-line" v-if="idx < steps.length - 1"></div>
      </div>
    </div>

    <!-- 上传阶段 -->
    <div v-if="currentStep === 1" class="upload-stage">
      <div
          class="drop-zone"
          :class="{ dragging: isDragging, 'has-file': selectedFile }"
          @dragenter.prevent="isDragging = true"
          @dragleave.prevent="isDragging = false"
          @dragover.prevent
          @drop.prevent="handleDrop"
      >
        <input
            type="file"
            ref="fileInput"
            accept="video/*"
            @change="handleFileSelect"
            style="display: none"
        >

        <div class="drop-content" v-if="!selectedFile">
          <div class="upload-illustration">
            <svg viewBox="0 0 200 200" class="upload-svg"  @click="$refs.fileInput.click()">
              <circle cx="100" cy="100" r="80" fill="none" stroke="currentColor" stroke-width="2" opacity="0.2"/>
              <path d="M100 60v80M60 100h80" stroke="currentColor" stroke-width="3" stroke-linecap="round"/>
              <circle cx="100" cy="100" r="80" fill="none" stroke="url(#gradient)" stroke-width="2"
                      stroke-dasharray="10 5" class="rotating-circle"/>
              <defs>
                <linearGradient id="gradient" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stop-color="#6366f1"/>
                  <stop offset="100%" stop-color="#8b5cf6"/>
                </linearGradient>
              </defs>
            </svg>
          </div>
          <h3>拖拽视频文件到此处</h3>
          <p>或 <span class="text-link" @click="$refs.fileInput.click()">点击浏览</span> 选择文件</p>
          <div class="format-hints">
            <span v-for="fmt in allowedFormats" :key="fmt" class="format-tag">{{ fmt }}</span>
          </div>
        </div>

        <div class="file-preview" v-else>
          <div class="file-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/>
              <path d="M14 2v6h6M16 13H8M16 17H8M10 9H8"/>
            </svg>
          </div>
          <div class="file-details">
            <div class="file-name">{{ selectedFile.name }}</div>
            <div class="file-meta">
              <span>{{ formatSize(selectedFile.size) }}</span>
              <span class="dot"></span>
              <span>{{ selectedFile.type || '视频文件' }}</span>
            </div>
          </div>
          <el-button circle class="remove-btn" @click.stop="clearFile">
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
      </div>

      <!-- 配置面板 -->
      <div class="config-panel" v-if="selectedFile">
        <h4 class="panel-title">
          <el-icon><Setting /></el-icon>
          分析配置
        </h4>

        <div class="config-grid">
          <div class="config-item">
            <label>检测线方向</label>
            <el-radio-group v-model="config.direction" size="large">
              <el-radio-button label="vertical">
                <el-icon><Sort /></el-icon>
                南北向
              </el-radio-button>
              <el-radio-button label="horizontal">
                <el-icon><Sort style="transform: rotate(90deg)"/></el-icon>
                东西向
              </el-radio-button>
            </el-radio-group>
          </div>

          <div class="config-item">
            <label>检测线位置 ({{ config.position }}%)</label>
            <el-slider v-model="config.position" :max="100" show-stops />
            <div class="position-preview">
              <div class="preview-box">
                <div class="preview-line" :style="linePreviewStyle"></div>
                <div class="preview-labels">
                  <span>A</span>
                  <span>B</span>
                </div>
              </div>
              <p class="preview-hint">车辆从A侧进入B侧记为"进入"，反之为"离开"</p>
            </div>
          </div>

          <div class="config-item">
            <label>单行道路（仅允许一侧车流）</label>
            <div class="one-way-row">
              <el-switch v-model="config.oneWay" active-text="开启" inactive-text="关闭" />
              <span class="one-way-hint">开启后，与「进入」相反方向的跨越线车辆记为逆行</span>
            </div>
          </div>

          <div class="config-item">
            <label>帧跳过率</label>
            <el-select v-model="config.frameSkip" size="large" style="width: 100%">
              <el-option
                  v-for="opt in frameSkipOptions"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
              >
                <span style="float: left">{{ opt.label }}</span>
                <span style="float: right; color: #8492a6; font-size: 13px">{{ opt.desc }}</span>
              </el-option>
            </el-select>
          </div>

          <div class="config-item">
            <label>像素比例（影响车速计算精度）</label>
            <el-select v-model="config.metersPerPixel" size="large" style="width: 100%">
              <el-option
                  v-for="opt in metersPerPixelOptions"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
              >
                <span style="float: left">{{ opt.label }}</span>
                <span style="float: right; color: #8492a6; font-size: 13px">{{ opt.desc }}</span>
              </el-option>
            </el-select>
          </div>
        </div>

        <div class="config-actions">
          <el-button size="large" @click="clearFile">重新选择</el-button>
          <el-button
              type="primary"
              size="large"
              :loading="uploading"
              @click="startUpload"
              class="start-btn"
          >
            <el-icon><VideoPlay /></el-icon>
            开始分析
          </el-button>
        </div>
      </div>
    </div>

    <!-- 分析中阶段 -->
    <div v-if="currentStep === 2" class="processing-stage">
      <div class="processing-visual">
        <div class="pulse-rings">
          <div class="ring"></div>
          <div class="ring"></div>
          <div class="ring"></div>
        </div>
        <div class="processing-icon">
          <el-icon size="48"><VideoCamera /></el-icon>
        </div>
      </div>

      <h2 class="processing-title">AI 正在分析视频...</h2>
      <p class="processing-subtitle">{{ statusMessage }}</p>

      <div class="progress-section">
        <div class="progress-header">
          <span class="progress-percent">{{ progress }}%</span>
          <span class="progress-detail">{{ progressDetail }}</span>
        </div>
        <div class="progress-bar-bg">
          <div class="progress-bar-fill" :style="{ width: progress + '%' }">
            <div class="progress-shine"></div>
          </div>
        </div>
      </div>

      <div class="realtime-metrics" v-if="metrics.frameCount > 0">
        <div class="metric-card">
          <el-icon><Film /></el-icon>
          <div class="metric-value">{{ metrics.frameCount }}</div>
          <div class="metric-label">已处理帧</div>
        </div>
        <div class="metric-card">
          <el-icon><Truck /></el-icon>
          <div class="metric-value">{{ metrics.totalCars }}</div>
          <div class="metric-label">检测车辆</div>
        </div>
        <div class="metric-card">
          <el-icon><Timer /></el-icon>
          <div class="metric-value">{{ metrics.fps }}</div>
          <div class="metric-label">处理速度</div>
        </div>
      </div>

      <div class="connection-status" :class="{ connected: wsConnected }">
        <span class="status-dot"></span>
        {{ wsConnected ? '实时数据连接正常' : '使用HTTP轮询模式' }}
      </div>

      <el-button
          type="danger"
          plain
          size="large"
          class="cancel-btn"
          @click="confirmCancel"
      >
        <el-icon><CircleClose /></el-icon>
        取消分析
      </el-button>
    </div>

    <!-- 完成阶段 -->
    <div v-if="currentStep === 3" class="complete-stage">
      <div class="success-animation">
        <svg class="checkmark" viewBox="0 0 100 100">
          <circle cx="50" cy="50" r="45" fill="none" stroke="#10b981" stroke-width="3"/>
          <path d="M30 50l15 15 25-30" fill="none" stroke="#10b981" stroke-width="4" stroke-linecap="round" stroke-linejoin="round" class="check-path"/>
        </svg>
      </div>
      <h2>分析完成！</h2>
      <p>视频已成功处理，点击下方按钮查看详细结果</p>
      <el-button type="primary" size="large" @click="viewResult">
        查看分析报告
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { analyzeVideo, cancelTask, getResult } from '@/api'
import { toast, confirmAction } from '@/utils/ui'
import { getWsProgressUrl } from '@/config'
import { formatSize } from '@/utils/format'
import { debugError } from '@/utils/debug'

const router = useRouter()
const steps = ['上传视频', 'AI分析', '查看结果']
const currentStep = ref(1)
const isDragging = ref(false)
const selectedFile = ref(null)
const fileInput = ref(null)
const uploading = ref(false)
const progress = ref(0)
const statusMessage = ref('准备上传...')
const wsConnected = ref(false)
const ws = ref(null)
const pollingTimer = ref(null)
const taskId = ref('')

const allowedFormats = ['MP4', 'AVI', 'MOV', 'MKV']
const frameSkipOptions = [
  { value: 1, label: '每帧处理', desc: '精度最高，速度较慢' },
  { value: 2, label: '跳1帧', desc: '平衡模式' },
  { value: 3, label: '跳2帧', desc: '推荐设置' },
  { value: 5, label: '跳4帧', desc: '快速预览' }
]

const metersPerPixelOptions = [
  { value: 0.02, label: '近景 (0.02 m/px)', desc: '摄像头离道路较近' },
  { value: 0.05, label: '标准 (0.05 m/px)', desc: '推荐设置' },
  { value: 0.08, label: '中远景 (0.08 m/px)', desc: '摄像头离道路较远' },
  { value: 0.12, label: '远景 (0.12 m/px)', desc: '高空/远距离摄像头' }
]

const config = ref({
  direction: 'vertical',
  position: 50,
  frameSkip: 2,
  metersPerPixel: 0.05,
  oneWay: false
})

const metrics = ref({
  frameCount: 0,
  totalCars: 0,
  fps: 0
})

const linePreviewStyle = computed(() => ({
  position: 'absolute',
  background: 'linear-gradient(90deg, #6366f1, #8b5cf6)',
  boxShadow: '0 0 20px rgba(99, 102, 241, 0.5)',
  ...(config.value.direction === 'vertical' ? {
    left: config.value.position + '%',
    top: 0,
    bottom: 0,
    width: '3px',
    transform: 'translateX(-50%)'
  } : {
    top: config.value.position + '%',
    left: 0,
    right: 0,
    height: '3px',
    transform: 'translateY(-50%)'
  })
}))

const progressDetail = computed(() => {
  if (progress.value < 10) return '准备上传'
  if (progress.value < 30) return '上传视频到服务器'
  if (progress.value < 50) return 'AI模型初始化'
  if (progress.value < 80) return '检测与跟踪车辆'
  if (progress.value < 100) return '生成结果视频'
  return '分析完成'
})

const handleDrop = (e) => {
  isDragging.value = false
  const file = e.dataTransfer.files[0]
  if (file && file.type.startsWith('video/')) {
    validateAndSetFile(file)
  } else {
    toast.error('请上传视频文件')
  }
}

const handleFileSelect = (e) => {
  const file = e.target.files[0]
  if (file) validateAndSetFile(file)
}

const validateAndSetFile = (file) => {
  const maxSize = 1024 * 1024 * 1024
  if (file.size > maxSize) {
    toast.error('文件大小不能超过1024MB')
    return
  }
  selectedFile.value = file
}

const clearFile = () => {
  selectedFile.value = null
  if (fileInput.value) fileInput.value.value = ''
}

const startUpload = async () => {
  if (!selectedFile.value) return

  uploading.value = true
  currentStep.value = 2

  try {
    const detectionLines = [{
      name: 'main_line',
      x1: config.value.direction === 'vertical' ? config.value.position / 100 : 0,
      y1: config.value.direction === 'vertical' ? 0 : config.value.position / 100,
      x2: config.value.direction === 'vertical' ? config.value.position / 100 : 1,
      y2: config.value.direction === 'vertical' ? 1 : config.value.position / 100,
      direction: config.value.direction,
      one_way: config.value.oneWay
    }]

    const res = await analyzeVideo(selectedFile.value, config.value.frameSkip, JSON.stringify(detectionLines), config.value.metersPerPixel)

    if (res.data.code === 200) {
      taskId.value = res.data.data.taskId
      progress.value = 0
      statusMessage.value = '开始处理...'
      initWebSocket()

      setTimeout(() => {
        if (!wsConnected.value) startPolling()
      }, 5000)
    } else {
      throw new Error(res.data.message)
    }
  } catch (err) {
    toast.error('上传失败: ' + err.message)
    currentStep.value = 1
    uploading.value = false
  }
}

const initWebSocket = () => {
  const wsUrl = getWsProgressUrl(taskId.value)
  ws.value = new WebSocket(wsUrl)

  ws.value.onopen = () => {
    wsConnected.value = true
    clearInterval(pollingTimer.value)  // ✅ 加这行
    pollingTimer.value = null
  }

  ws.value.onmessage = (e) => {
    const data = JSON.parse(e.data)
    updateProgress(data)
  }

  ws.value.onclose = () => {
    wsConnected.value = false
    if (currentStep.value === 2) startPolling()
  }
}

const startPolling = () => {
  if (pollingTimer.value) return

  pollingTimer.value = setInterval(async () => {
    try {
      const res = await getResult(taskId.value)
      if (res.data.code === 200) {
        updateProgress(res.data.data)
      }
    } catch (e) {
      debugError('轮询失败:', e)
    }
  }, 3000)
}

const updateProgress = (data) => {
  progress.value = data.progress || progress.value
  statusMessage.value = data.message || statusMessage.value

  if (data.frameCount !== undefined) metrics.value.frameCount = data.frameCount
  if (data.totalCars !== undefined) metrics.value.totalCars = data.totalCars

  if (data.progress >= 100) {
    currentStep.value = 3
    cleanup()
  } else if (data.progress === -1 || data.status === 3) {
    toast.error('分析失败: ' + (data.errorMsg || data.message || '未知错误'))
    currentStep.value = 1
    cleanup()
  }
}

const confirmCancel = async () => {
  try {
    await confirmAction(
      '确定要取消当前分析任务吗？取消后需重新上传视频。',
      '取消分析',
      {
        confirmButtonText: '确定取消',
        cancelButtonText: '继续分析',
        type: 'warning'
      }
    )

    await cancelTask(taskId.value)
    toast.success('已取消分析')
    cleanup()
    currentStep.value = 1
    uploading.value = false
    progress.value = 0
  } catch (e) {
    // 用户取消
  }
}

const viewResult = () => {
  router.push(`/result/${taskId.value}`)
}

const cleanup = () => {
  if (ws.value) {
    ws.value.close()
    ws.value = null
  }
  /*if (pollingTimer.value) {
    clearInterval(pollingTimer.value)
    pollingTimer.value = null
  }*/
  clearInterval(pollingTimer.value)
  pollingTimer.value = null
}

onUnmounted(cleanup)
</script>

<style scoped>
.analyze-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px;
}

/* Steps */
.steps-header {
  display: flex;
  justify-content: center;
  gap: 40px;
  margin-bottom: 40px;
  padding: 20px;
  background: var(--bg-card);
  border-radius: 16px;
  border: 1px solid var(--border);
}

.step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  position: relative;
}

.step-number {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--bg-dark);
  border: 2px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  color: var(--text-secondary);
  transition: all 0.3s;
}

.step.active .step-number {
  background: var(--gradient);
  border-color: transparent;
  color: white;
  box-shadow: 0 0 20px rgba(99, 102, 241, 0.4);
}

.step.completed .step-number {
  background: #10b981;
  border-color: #10b981;
  color: white;
}

.step-label {
  font-size: 14px;
  color: var(--text-secondary);
  font-weight: 500;
}

.step.active .step-label {
  color: var(--text-primary);
}

.step-line {
  position: absolute;
  top: 20px;
  left: 60px;
  width: 80px;
  height: 2px;
  background: var(--border);
}

.step.completed .step-line {
  background: #10b981;
}

/* Upload Stage */
.drop-zone {
  border: 2px dashed var(--border);
  border-radius: 24px;
  padding: 60px 40px;
  text-align: center;
  transition: all 0.3s;
  background: var(--bg-card);
}

.drop-zone.dragging {
  border-color: var(--primary);
  background: rgba(99, 102, 241, 0.05);
  transform: scale(1.02);
}

.drop-zone.has-file {
  padding: 40px;
}

.upload-illustration {
  width: 200px;
  height: 200px;
  margin: 0 auto 30px;
  color: var(--primary);
}

.upload-svg {
  width: 100%;
  height: 100%;
}

.rotating-circle {
  animation: rotate 20s linear infinite;
  transform-origin: center;
}

@keyframes rotate {
  to { transform: rotate(360deg); }
}

.drop-content h3 {
  font-size: 24px;
  margin-bottom: 12px;
  color: var(--text-primary);
}

.drop-content p {
  color: var(--text-secondary);
  margin-bottom: 20px;
}

.text-link {
  color: var(--primary);
  cursor: pointer;
  font-weight: 500;
}

.text-link:hover {
  text-decoration: underline;
}

.format-hints {
  display: flex;
  gap: 10px;
  justify-content: center;
}

.format-tag {
  padding: 6px 14px;
  background: rgba(99, 102, 241, 0.1);
  border-radius: 20px;
  font-size: 13px;
  color: var(--primary);
  font-weight: 500;
}

/* File Preview */
.file-preview {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 20px;
  background: var(--bg-dark);
  border-radius: 16px;
  border: 1px solid var(--border);
}

.file-icon {
  width: 60px;
  height: 60px;
  background: var(--gradient);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.file-icon svg {
  width: 30px;
  height: 30px;
}

.file-details {
  flex: 1;
  text-align: left;
}

.file-name {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 6px;
  color: var(--text-primary);
}

.file-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-secondary);
  font-size: 14px;
}

.dot {
  width: 4px;
  height: 4px;
  background: var(--text-secondary);
  border-radius: 50%;
}

.remove-btn {
  background: rgba(239, 68, 68, 0.1) !important;
  color: #ef4444 !important;
  border: none !important;
}

/* Config Panel */
.config-panel {
  margin-top: 30px;
  padding: 30px;
  background: var(--bg-card);
  border-radius: 20px;
  border: 1px solid var(--border);
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  margin-bottom: 30px;
  color: var(--text-primary);
}

.config-grid {
  display: grid;
  gap: 30px;
  margin-bottom: 30px;
}

.config-item label {
  display: block;
  margin-bottom: 12px;
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 500;
}

.one-way-row {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.one-way-hint {
  font-size: 12px;
  color: var(--text-secondary);
  line-height: 1.4;
}

.position-preview {
  margin-top: 20px;
  padding: 20px;
  background: var(--bg-dark);
  border-radius: 12px;
}

.preview-box {
  position: relative;
  width: 100%;
  height: 120px;
  background:
      linear-gradient(45deg, #e2e8f0 25%, transparent 25%),
      linear-gradient(-45deg, #e2e8f0 25%, transparent 25%),
      linear-gradient(45deg, transparent 75%, #e2e8f0 75%),
      linear-gradient(-45deg, transparent 75%, #e2e8f0 75%);
  background-size: 20px 20px;
  background-position: 0 0, 0 10px, 10px -10px, -10px 0px;
  background-color: #f5f7fa;
  border-radius: 8px;
  overflow: hidden;
}

.preview-labels {
  position: absolute;
  inset: 0;
  display: flex;
  justify-content: space-between;
  padding: 10px;
  pointer-events: none;
}

.preview-labels span {
  padding: 4px 12px;
  background: rgba(99, 102, 241, 0.2);
  border-radius: 4px;
  font-size: 12px;
  color: var(--primary);
  font-weight: 600;
}

.preview-hint {
  margin-top: 12px;
  font-size: 13px;
  color: var(--text-secondary);
  text-align: center;
}

.config-actions {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
}

.start-btn {
  background: var(--gradient);
  border: none;
  padding: 0 32px;
}

/* Processing Stage */
.processing-stage {
  text-align: center;
  padding: 60px 20px;
}

.processing-visual {
  position: relative;
  width: 200px;
  height: 200px;
  margin: 0 auto 40px;
}

.pulse-rings {
  position: absolute;
  inset: 0;
}

.ring {
  position: absolute;
  inset: 0;
  border: 2px solid var(--primary);
  border-radius: 50%;
  opacity: 0;
  animation: pulse-ring 2s ease-out infinite;
}

.ring:nth-child(2) {
  animation-delay: 0.5s;
}

.ring:nth-child(3) {
  animation-delay: 1s;
}

@keyframes pulse-ring {
  0% { transform: scale(0.8); opacity: 0.5; }
  100% { transform: scale(1.5); opacity: 0; }
}

.processing-icon {
  position: absolute;
  inset: 40px;
  background: var(--gradient);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  box-shadow: 0 0 40px rgba(99, 102, 241, 0.4);
}

.processing-title {
  font-size: 28px;
  margin-bottom: 12px;
}

.processing-subtitle {
  color: var(--text-secondary);
  margin-bottom: 40px;
}

/* Progress */
.progress-section {
  max-width: 600px;
  margin: 0 auto 40px;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
}

.progress-percent {
  font-size: 24px;
  font-weight: 700;
  color: var(--primary);
}

.progress-detail {
  color: var(--text-secondary);
  font-size: 14px;
}

.progress-bar-bg {
  height: 8px;
  background: var(--bg-dark);
  border-radius: 4px;
  overflow: hidden;
  position: relative;
}

.progress-bar-fill {
  height: 100%;
  background: var(--gradient);
  border-radius: 4px;
  transition: width 0.3s ease;
  position: relative;
}

.progress-shine {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.3), transparent);
  animation: shine 2s infinite;
}

@keyframes shine {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}

/* Metrics */
.realtime-metrics {
  display: flex;
  justify-content: center;
  gap: 30px;
  margin-bottom: 40px;
}

.metric-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 24px 40px;
  min-width: 140px;
}

.metric-card .el-icon {
  font-size: 24px;
  color: var(--primary);
  margin-bottom: 12px;
}

.metric-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.metric-label {
  font-size: 13px;
  color: var(--text-secondary);
}

/* Connection Status */
.connection-status {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: var(--bg-card);
  border-radius: 20px;
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 30px;
}

.connection-status.connected {
  color: #10b981;
}

.status-dot {
  width: 8px;
  height: 8px;
  background: #ef4444;
  border-radius: 50%;
  animation: blink 2s infinite;
}

.connection-status.connected .status-dot {
  background: #10b981;
  animation: none;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

.cancel-btn {
  background: rgba(239, 68, 68, 0.1);
  border-color: #ef4444;
  color: #ef4444;
}

/* Complete Stage */
.complete-stage {
  text-align: center;
  padding: 80px 20px;
}

.success-animation {
  width: 120px;
  height: 120px;
  margin: 0 auto 30px;
}

.checkmark {
  width: 100%;
  height: 100%;
}

.check-path {
  stroke-dasharray: 100;
  stroke-dashoffset: 100;
  animation: draw-check 0.6s ease forwards 0.3s;
}

@keyframes draw-check {
  to { stroke-dashoffset: 0; }
}

.complete-stage h2 {
  font-size: 32px;
  margin-bottom: 16px;
}

.complete-stage p {
  color: var(--text-secondary);
  margin-bottom: 30px;
}
</style>