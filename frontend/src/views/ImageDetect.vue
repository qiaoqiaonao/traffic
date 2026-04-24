<template>
  <div class="image-detect-page">
    <h1 class="page-title">
      <el-icon><Picture /></el-icon>
      单张图片检测
    </h1>

    <!-- 上传区域 -->
    <div class="upload-section" v-if="!result">
      <div
          class="drop-zone"
          :class="{ dragging: isDragging }"
          @dragenter.prevent="isDragging = true"
          @dragleave.prevent="isDragging = false"
          @dragover.prevent
          @drop.prevent="handleDrop"
      >
        <input
            type="file"
            ref="fileInput"
            accept="image/*"
            @change="handleFileSelect"
            style="display: none"
        >

        <div class="upload-content">
          <div class="upload-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
              <circle cx="8.5" cy="8.5" r="1.5"/>
              <path d="M21 15l-5-5L5 21"/>
            </svg>
          </div>
          <h3>拖拽图片到此处</h3>
          <p>支持 JPG、PNG、BMP 格式，最大 10MB</p>
          <el-button type="primary" @click="$refs.fileInput.click()">
            选择图片
          </el-button>
        </div>
      </div>

      <!-- 快速示例 -->
      <div class="quick-samples">
        <span class="sample-label">快速体验：</span>
        <div class="sample-list">
          <div
              v-for="(sample, idx) in sampleImages"
              :key="idx"
              class="sample-item"
              @click="useSample(sample)"
          >
            <img :src="sample.thumb" :alt="sample.name">
            <span>{{ sample.name }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 检测中 -->
    <div v-if="detecting" class="detecting-state">
      <div class="scanning-effect">
        <div class="scan-line"></div>
        <div class="grid-overlay"></div>
      </div>
      <div class="detecting-info">
        <el-icon class="detecting-icon"><Loading /></el-icon>
        <span>AI 正在分析图片...</span>
        <el-progress :percentage="detectProgress" :show-text="false" class="detect-progress"/>
      </div>
    </div>

    <!-- 检测结果 -->
    <div v-if="result" class="result-section">
      <!-- 结果工具栏 -->
      <div class="result-toolbar">
        <div class="toolbar-left">
          <el-button @click="reset">
            <el-icon><Back /></el-icon>
            重新检测
          </el-button>
        </div>
        <div class="toolbar-center">
          <span class="result-badge success">
            <el-icon><CircleCheck /></el-icon>
            检测到 {{ result.count }} 个目标
          </span>
        </div>
        <div class="toolbar-right">
          <el-button type="primary" @click="downloadResult">
            <el-icon><Download /></el-icon>
            下载结果
          </el-button>
        </div>
      </div>

      <!-- 图片对比展示 -->
      <div class="image-compare">
        <div class="compare-panel">
          <div class="panel-header">
            <span>原始图片</span>
            <span class="panel-meta">{{ originalImage.size }}</span>
          </div>
          <div class="panel-body">
            <img :src="originalImage.url" alt="原始图片" ref="originalImg">
          </div>
        </div>

        <div class="compare-divider">
          <div class="vs-badge">VS</div>
        </div>

        <div class="compare-panel result">
          <div class="panel-header">
            <span>检测结果</span>
            <span class="panel-meta">{{ result.infer_time_ms }}ms</span>
          </div>
          <div class="panel-body">
            <img :src="result.marked_image" alt="检测结果">
          </div>
        </div>
      </div>

      <!-- 统计信息 -->
      <div class="stats-section">
        <div class="stats-grid">
          <div class="stat-card">
            <div class="stat-icon" style="background: rgba(99, 102, 241, 0.2); color: #6366f1;">
              <el-icon><Aim /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ result.count }}</div>
              <div class="stat-label">检测目标</div>
            </div>
          </div>

          <div class="stat-card">
            <div class="stat-icon" style="background: rgba(16, 185, 129, 0.2); color: #10b981;">
              <el-icon><Timer /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ result.infer_time_ms }}<small>ms</small></div>
              <div class="stat-label">推理耗时</div>
            </div>
          </div>

          <div class="stat-card">
            <div class="stat-icon" style="background: rgba(245, 158, 11, 0.2); color: #f59e0b;">
              <el-icon><FullScreen /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ result.image_width }}×{{ result.image_height }}</div>
              <div class="stat-label">图片尺寸</div>
            </div>
          </div>

          <div class="stat-card">
            <div class="stat-icon" style="background: rgba(139, 92, 246, 0.2); color: #8b5cf6;">
              <el-icon><TrendCharts /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ avgConfidence }}<small>%</small></div>
              <div class="stat-label">平均置信度</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 类别分布 -->
      <div class="distribution-section">
        <div class="dist-card">
          <h4>类别分布</h4>
          <div class="class-list">
            <div
                v-for="(count, className) in result.statistics.by_class"
                :key="className"
                class="class-item"
            >
              <div class="class-info">
                <span class="class-dot" :style="{ background: getClassColor(className) }"></span>
                <span class="class-name">{{ className }}</span>
              </div>
              <div class="class-bar">
                <div
                    class="class-fill"
                    :style="{
                    width: (count / result.count * 100) + '%',
                    background: getClassColor(className)
                  }"
                ></div>
              </div>
              <span class="class-count">{{ count }}</span>
            </div>
          </div>
        </div>

        <div class="dist-card">
          <h4>检测详情</h4>
          <el-table :data="result.detections" size="small" border stripe max-height="300">
            <el-table-column type="index" width="50" label="序号" />
            <el-table-column label="类别" width="100">
              <template #default="{ row }">
                <el-tag :type="getTagType(row.class)" size="small">
                  {{ row.class }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="置信度" width="100">
              <template #default="{ row }">
                <span :style="{ color: getScoreColor(row.score) }">
                  {{ (row.score * 100).toFixed(1) }}%
                </span>
              </template>
            </el-table-column>
            <el-table-column label="边界框">
              <template #default="{ row }">
                <code class="bbox-code">[{{ row.bbox.map(v => Math.round(v)).join(', ') }}]</code>
              </template>
            </el-table-column>
            <el-table-column prop="area" label="面积" width="80">
              <template #default="{ row }">
                {{ formatArea(row.area) }}
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElLoading } from 'element-plus'
import { analyzeFrame } from '@/api'
import { toast } from '@/utils/ui'

const fileInput = ref(null)
const isDragging = ref(false)
const detecting = ref(false)
const detectProgress = ref(0)
const result = ref(null)
const originalImage = ref({ url: '', size: '' })
const hoveredDet = ref(null)

// 示例图片（实际项目中替换为真实图片）
const sampleImages = [
  { name: '路口场景', thumb: '/samples/intersection.jpg', url: '/samples/intersection.jpg' },
  { name: '高速公路', thumb: '/samples/highway.jpg', url: '/samples/highway.jpg' },
  { name: '停车场', thumb: '/samples/parking.jpg', url: '/samples/parking.jpg' }
]

const avgConfidence = computed(() => {
  if (!result.value?.detections?.length) return 0
  const avg = result.value.detections.reduce((sum, d) => sum + d.score, 0) / result.value.detections.length
  return (avg * 100).toFixed(1)
})

const handleDrop = (e) => {
  isDragging.value = false
  const file = e.dataTransfer.files[0]
  if (file && file.type.startsWith('image/')) {
    processImage(file)
  } else {
    toast.error('请上传图片文件')
  }
}

const handleFileSelect = (e) => {
  const file = e.target.files[0]
  if (file) processImage(file)
}

const processImage = (file) => {
  const maxSize = 10 * 1024 * 1024
  if (file.size > maxSize) {
    toast.error('图片大小不能超过10MB')
    return
  }

  // 预览原图
  const url = URL.createObjectURL(file)
  originalImage.value = {
    url,
    size: formatSize(file.size),
    file
  }

  startDetect(file)
}

const useSample = async (sample) => {
  // 防止重复点击
  if (detecting.value) {
    toast.warning('正在检测中，请稍候...')
    return
  }

  let loadingInst = null
  try {
    loadingInst = ElLoading.service({
      lock: true,
      text: `正在加载示例：${sample.name}`,
      background: 'rgba(15, 23, 42, 0.65)'
    })

    // 获取示例图片
    const response = await fetch(sample.url)

    if (!response.ok) {
      throw new Error(`图片加载失败: ${response.status}`)
    }

    const blob = await response.blob()
    const file = new File([blob], `${sample.name}.jpg`, { type: blob.type || 'image/jpeg' })

    // 创建本地预览URL
    const previewUrl = URL.createObjectURL(blob)

    // 设置原图预览（在检测前就显示）
    originalImage.value = {
      url: previewUrl,
      size: formatSize(file.size),
      file: file,
      name: sample.name  // 可选：保存示例名称
    }

    loadingInst.close()
    loadingInst = null

    // 开始检测
    await startDetect(file)

  } catch (err) {
    if (loadingInst) loadingInst.close()
    console.error('加载示例失败:', err)
    toast.error(`加载示例图片失败: ${err.message}`)
  }
}

const startDetect = async (file) => {
  detecting.value = true
  detectProgress.value = 0

  // 模拟进度
  const progressTimer = setInterval(() => {
    if (detectProgress.value < 90) {
      detectProgress.value += Math.random() * 15
    }
  }, 200)

  try {
    const res = await analyzeFrame(file)
    clearInterval(progressTimer)

    if (res.data.code === 200) {
      detectProgress.value = 100
      setTimeout(() => {
        result.value = res.data.data
        detecting.value = false
        toast.success(`检测完成，发现 ${result.value.count} 个目标`)
      }, 300)
    } else {
      throw new Error(res.data.message)
    }
  } catch (err) {
    clearInterval(progressTimer)
    detecting.value = false
    toast.error('检测失败: ' + err.message)
  }
}

const getBoxStyle = (det) => {
  const [x1, y1, x2, y2] = det.bbox
  return {
    left: x1 + 'px',
    top: y1 + 'px',
    width: (x2 - x1) + 'px',
    height: (y2 - y1) + 'px'
  }
}

const getTagType = (className) => {
  const map = {
    'car': 'success',
    'truck': 'primary',
    'bus': 'warning',
    'person': 'info',
    'motorcycle': 'danger',
    'bicycle': 'danger'
  }
  return map[className] || 'info'
}

const getClassColor = (className) => {
  const map = {
    'car': '#10b981',
    'truck': '#3b82f6',
    'bus': '#f59e0b',
    'person': '#6366f1',
    'motorcycle': '#ef4444',
    'bicycle': '#8b5cf6'
  }
  return map[className] || '#94a3b8'
}

const getScoreColor = (score) => {
  if (score > 0.9) return '#10b981'
  if (score > 0.7) return '#f59e0b'
  return '#ef4444'
}

const formatSize = (bytes) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
}

const formatArea = (area) => {
  if (area > 10000) return (area / 10000).toFixed(1) + '万'
  return area
}

const downloadResult = () => {
  if (!result.value?.marked_image) return

  const link = document.createElement('a')
  link.href = result.value.marked_image
  link.download = `detect_result_${Date.now()}.jpg`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)

  toast.success('图片已下载')
}

const reset = () => {
  result.value = null
  originalImage.value = { url: '', size: '' }
  detectProgress.value = 0
  if (fileInput.value) fileInput.value.value = ''
}
</script>

<style scoped>
.image-detect-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 30px;
  background: var(--gradient);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.page-title .el-icon {
  font-size: 32px;
  background: var(--gradient);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

/* Upload Section */
.upload-section {
  margin-bottom: 40px;
}

.drop-zone {
  border: 2px dashed var(--border);
  border-radius: 20px;
  padding: 60px;
  text-align: center;
  background: var(--bg-card);
  transition: all 0.3s;
  cursor: pointer;
}

.drop-zone.dragging {
  border-color: var(--primary);
  background: rgba(99, 102, 241, 0.05);
  transform: scale(1.01);
}

.upload-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto 24px;
  color: var(--primary);
  opacity: 0.8;
}

.upload-icon svg {
  width: 100%;
  height: 100%;
}

.upload-content h3 {
  font-size: 24px;
  margin-bottom: 12px;
  color: var(--text-primary);
}

.upload-content p {
  color: var(--text-secondary);
  margin-bottom: 24px;
}

.quick-samples {
  margin-top: 30px;
  padding: 20px;
  background: var(--bg-card);
  border-radius: 12px;
  border: 1px solid var(--border);
}

.sample-label {
  display: block;
  color: var(--text-secondary);
  font-size: 14px;
  margin-bottom: 16px;
}

.sample-list {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.sample-item {
  width: 120px;
  cursor: pointer;
  transition: transform 0.3s;
}

.sample-item:hover {
  transform: translateY(-4px);
}

.sample-item img {
  width: 100%;
  height: 80px;
  object-fit: cover;
  border-radius: 8px;
  margin-bottom: 8px;
}

.sample-item span {
  font-size: 13px;
  color: var(--text-secondary);
}

/* Detecting State */
.detecting-state {
  position: relative;
  height: 400px;
  background: var(--bg-card);
  border-radius: 20px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.scanning-effect {
  position: absolute;
  inset: 0;
}

.scan-line {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: var(--primary);
  box-shadow: 0 0 10px var(--primary);
  animation: scan 2s linear infinite;
}

@keyframes scan {
  0% { top: 0; opacity: 0; }
  10% { opacity: 1; }
  90% { opacity: 1; }
  100% { top: 100%; opacity: 0; }
}

.grid-overlay {
  position: absolute;
  inset: 0;
  background-image:
      linear-gradient(rgba(99, 102, 241, 0.1) 1px, transparent 1px),
      linear-gradient(90deg, rgba(99, 102, 241, 0.1) 1px, transparent 1px);
  background-size: 40px 40px;
}

.detecting-info {
  position: relative;
  z-index: 1;
  text-align: center;
}

.detecting-icon {
  font-size: 48px;
  color: var(--primary);
  margin-bottom: 16px;
  animation: rotate 1s linear infinite;
}

@keyframes rotate {
  to { transform: rotate(360deg); }
}

.detecting-info span {
  display: block;
  color: var(--text-primary);
  font-size: 18px;
  margin-bottom: 20px;
}

.detect-progress {
  width: 200px;
  margin: 0 auto;
}

/* Result Section */
.result-section {
  animation: fadeIn 0.5s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20px); }
}

.result-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  padding: 16px 24px;
  background: var(--bg-card);
  border-radius: 12px;
  border: 1px solid var(--border);
}

.result-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
}

.result-badge.success {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
}

/* Image Compare */
.image-compare {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  gap: 20px;
  margin-bottom: 30px;
}

.compare-panel {
  background: var(--bg-card);
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid var(--border);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: var(--bg-dark);
  border-bottom: 1px solid var(--border);
}

.panel-header span {
  font-weight: 500;
}

.panel-meta {
  color: var(--text-secondary);
  font-size: 13px;
}

.panel-body {
  position: relative;
  padding: 20px;
  min-height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.panel-body img {
  max-width: 100%;
  max-height: 500px;
  border-radius: 8px;
}

.compare-divider {
  display: flex;
  align-items: center;
  justify-content: center;
}

.vs-badge {
  width: 40px;
  height: 40px;
  background: var(--gradient);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 12px;
  color: white;
}


/* Stats Section */
.stats-section {
  margin-bottom: 30px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 24px;
  background: var(--bg-card);
  border-radius: 16px;
  border: 1px solid var(--border);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1;
}

.stat-value small {
  font-size: 14px;
  color: var(--text-secondary);
  margin-left: 4px;
}

.stat-label {
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 4px;
}

/* Distribution Section */
.distribution-section {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 20px;
}

.dist-card {
  background: var(--bg-card);
  border-radius: 16px;
  padding: 24px;
  border: 1px solid var(--border);
}

.dist-card h4 {
  margin-bottom: 20px;
  font-size: 16px;
}

.class-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.class-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.class-info {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100px;
}

.class-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.class-name {
  font-size: 14px;
  text-transform: capitalize;
}

.class-bar {
  flex: 1;
  height: 8px;
  background: var(--bg-dark);
  border-radius: 4px;
  overflow: hidden;
}

.class-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.5s ease;
}

.class-count {
  width: 30px;
  text-align: right;
  font-weight: 600;
}

.bbox-code {
  font-size: 12px;
  color: var(--text-secondary);
  background: var(--bg-dark);
  padding: 4px 8px;
  border-radius: 4px;
}

@media (max-width: 1024px) {
  .image-compare {
    grid-template-columns: 1fr;
  }

  .compare-divider {
    display: none;
  }

  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .distribution-section {
    grid-template-columns: 1fr;
  }
}
</style>