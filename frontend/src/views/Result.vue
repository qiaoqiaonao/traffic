<template>
  <div class="result-page" v-if="task">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <el-button @click="$router.back()" class="back-btn">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <div class="header-title">
          <h1>分析结果详情</h1>
          <div class="task-meta">
            <code class="task-id">{{ task.taskId }}</code>
            <span class="separator">·</span>
            <span>{{ formatTime(task.createTime) }}</span>
          </div>
        </div>
      </div>
      <div class="header-actions">
        <el-button @click="exportData" :loading="exporting">
          <el-icon><Download /></el-icon>
          导出数据
        </el-button>
        <el-button type="primary" @click="scrollToVideo">
          <el-icon><VideoPlay /></el-icon>
          查看视频
        </el-button>
      </div>
    </div>

    <!-- 状态卡片 -->
    <div class="status-banner" :class="getStatusClass(task.status)">
      <div class="status-icon">
        <el-icon v-if="task.status === 2"><CircleCheck /></el-icon>
        <el-icon v-else-if="task.status === 1"><Loading /></el-icon>
        <el-icon v-else><CircleClose /></el-icon>
      </div>
      <div class="status-info">
        <h3>{{ getStatusText(task.status) }}</h3>
        <p v-if="task.errorMsg">{{ task.errorMsg }}</p>
        <p v-else-if="task.status === 2">分析成功完成，共处理 {{ parsedResult?.video_info?.total_frames }} 帧</p>
      </div>
    </div>

    <!-- 视频播放器区域 -->
    <div id="video-section" class="video-container" v-if="task.status === 2">
      <div class="section-header">
        <h3 class="section-title">
          <el-icon><VideoPlay /></el-icon>
          检测结果视频
        </h3>
        <div class="video-actions">
          <el-button size="small" @click="retryLoadVideo">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
          <el-button size="small" type="primary" @click="forceDownload">
            <el-icon><Download /></el-icon>
            下载
          </el-button>
        </div>
      </div>

      <!-- 加载中 -->
      <div v-if="videoLoading" class="loading-overlay">
        <el-icon class="is-loading" size="32"><Loading /></el-icon>
        <p>加载视频中...</p>
      </div>

      <!-- 错误 -->
      <div v-else-if="videoError" class="error-box">
        <el-icon size="48"><CircleClose /></el-icon>
        <p>{{ videoErrorMsg }}</p>
        <div class="error-actions">
          <el-button @click="retryLoadVideo">重试</el-button>
          <el-button type="primary" @click="forceDownload">下载观看</el-button>
        </div>
      </div>

      <!-- 视频播放器 -->
      <video
          v-show="!videoLoading && !videoError"
          ref="videoPlayer"
          controls
          preload="metadata"
          style="width: 100%; border-radius: 8px; max-height: 600px; background: #000;"
          @loadeddata="onVideoReady"
          @error="onVideoError"
          @waiting="videoLoading = true"
          @playing="videoLoading = false"
      ></video>

      <!-- 视频信息 -->
      <div class="video-info-bar" v-if="videoInfo">
        <span>分辨率: {{ parsedResult?.video_info?.width }}×{{ parsedResult?.video_info?.height }}</span>
        <el-divider direction="vertical" />
        <span>帧率: {{ parsedResult?.video_info?.fps }} FPS</span>
        <el-divider direction="vertical" />
        <span>时长: {{ formatDuration(parsedResult?.video_info?.duration_sec) }}</span>
      </div>
    </div>

    <!-- 核心指标 -->
    <div class="metrics-grid" v-if="parsedResult">
      <div class="metric-card large">
        <div class="metric-bg">
          <svg viewBox="0 0 200 200">
            <circle cx="100" cy="100" r="80" fill="none" stroke="rgba(99, 102, 241, 0.1)" stroke-width="8"/>
            <circle
                cx="100"
                cy="100"
                r="80"
                fill="none"
                stroke="url(#metricGradient)"
                stroke-width="8"
                stroke-linecap="round"
                stroke-dasharray="502"
                :stroke-dashoffset="502 - (502 * (parsedResult.statistics?.unique_vehicles || 0) / 1000)"
                transform="rotate(-90 100 100)"
            />
            <defs>
              <linearGradient id="metricGradient" x1="0%" y1="0%" x2="100%" y2="0%">
                <stop offset="0%" stop-color="#6366f1"/>
                <stop offset="100%" stop-color="#8b5cf6"/>
              </linearGradient>
            </defs>
          </svg>
        </div>
        <div class="metric-content">
          <div class="metric-value">{{ parsedResult.statistics?.unique_vehicles || 0 }}</div>
          <div class="metric-label">独立车辆数</div>
          <div class="metric-trend">
            <el-icon><TrendCharts /></el-icon>
            唯一ID跟踪
          </div>
        </div>
      </div>

      <div class="metric-card">
        <div class="metric-icon" style="background: rgba(16, 185, 129, 0.2); color: #10b981;">
          <el-icon><Aim /></el-icon>
        </div>
        <div class="metric-content">
          <div class="metric-value">{{ parsedResult.statistics?.total_detections?.toLocaleString() }}</div>
          <div class="metric-label">总检测次数</div>
        </div>
      </div>

      <div class="metric-card">
        <div class="metric-icon" style="background: rgba(245, 158, 11, 0.2); color: #f59e0b;">
          <el-icon><Timer /></el-icon>
        </div>
        <div class="metric-content">
          <div class="metric-value">{{ parsedResult.statistics?.avg_cars_per_frame ?? '—' }}</div>
          <div class="metric-label">平均每帧车辆</div>
        </div>
      </div>

      <div class="metric-card">
        <div class="metric-icon" style="background: rgba(6, 182, 212, 0.2); color: #06b6d4;">
          <el-icon><Odometer /></el-icon>
        </div>
        <div class="metric-content">
          <div class="metric-value">{{ avgSpeedKm }}</div>
          <div class="metric-label">平均车速（估算, km/h）</div>
        </div>
      </div>

      <div class="metric-card">
        <div class="metric-icon" style="background: rgba(239, 68, 68, 0.2); color: #ef4444;">
          <el-icon><Warning /></el-icon>
        </div>
        <div class="metric-content">
          <div class="metric-value">{{ parsedResult.statistics?.violations?.total || 0 }}</div>
          <div class="metric-label">违规事件</div>
        </div>
      </div>
    </div>

    <!-- 视频信息 -->
    <div class="info-section" v-if="parsedResult?.video_info">
      <h3 class="section-title">
        <el-icon><InfoFilled /></el-icon>
        视频信息
      </h3>
      <div class="info-grid">
        <div class="info-item">
          <span class="info-label">分辨率</span>
          <span class="info-value">{{ parsedResult.video_info.width }} × {{ parsedResult.video_info.height }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">帧率</span>
          <span class="info-value">{{ parsedResult.video_info.fps }} FPS</span>
        </div>
        <div class="info-item">
          <span class="info-label">总帧数</span>
          <span class="info-value">{{ parsedResult.video_info.total_frames?.toLocaleString() }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">时长</span>
          <span class="info-value">{{ formatDuration(parsedResult.video_info.duration_sec) }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">处理帧跳过</span>
          <span class="info-value">{{ task.frameSkip }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">实际处理帧</span>
          <span class="info-value">{{ parsedResult.statistics?.processed_frames?.toLocaleString() }}</span>
        </div>
      </div>
    </div>

    <!-- 流量统计 -->
    <div class="traffic-section" v-if="trafficData.length > 0">
      <h3 class="section-title">
        <el-icon><DataLine /></el-icon>
        流量统计
      </h3>
      <div class="traffic-cards">
        <div v-for="item in trafficData" :key="item.name" class="traffic-card">
          <div class="traffic-header">
            <span class="traffic-name">{{ item.name }}</span>
            <el-tag size="small" :type="item.in > item.out ? 'success' : 'warning'">
              净{{ item.in > item.out ? '流入' : '流出' }}
            </el-tag>
          </div>
          <div class="traffic-flow">
            <div class="flow-item in">
              <div class="flow-icon">
                <el-icon><ArrowDown /></el-icon>
              </div>
              <div class="flow-info">
                <span class="flow-value">{{ item.in }}</span>
                <span class="flow-label">进入</span>
              </div>
            </div>
            <div class="flow-divider"></div>
            <div class="flow-item out">
              <div class="flow-icon">
                <el-icon><ArrowUp /></el-icon>
              </div>
              <div class="flow-info">
                <span class="flow-value">{{ item.out }}</span>
                <span class="flow-label">离开</span>
              </div>
            </div>
            <div class="flow-total">
              <div class="flow-value">{{ item.total }}</div>
              <div class="flow-label">总计</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 趋势图表 -->
    <div class="chart-section" v-if="frameData.length > 0">
      <div class="chart-header">
        <h3 class="section-title">
          <el-icon><TrendCharts /></el-icon>
          车辆数变化趋势
        </h3>
        <el-radio-group v-model="chartType" size="small">
          <el-radio-button label="line">折线图</el-radio-button>
          <el-radio-button label="bar">柱状图</el-radio-button>
        </el-radio-group>
      </div>
      <div ref="trendChartRef" class="chart-container"></div>
    </div>

    <!-- 违规记录 -->
    <div class="violation-section" v-if="violations.length > 0">
      <h3 class="section-title">
        <el-icon><WarningFilled /></el-icon>
        违规记录
        <el-tag type="danger" size="small" class="violation-count">{{ violations.length }}</el-tag>
      </h3>
      <div class="violation-list">
        <div
            v-for="(v, idx) in violations.slice(0, 10)"
            :key="idx"
            class="violation-item"
            :class="v.type"
        >
          <div class="violation-icon">
            <el-icon v-if="v.type === 'wrong_direction'"><Sort /></el-icon>
            <el-icon v-else><Timer /></el-icon>
          </div>
          <div class="violation-info">
            <div class="violation-title">
              {{ v.type === 'wrong_direction' ? '逆行检测' : '违停检测' }}
              <span class="violation-time">{{ v.timestamp?.toFixed(2) }}s</span>
            </div>
            <div class="violation-detail">
              帧号: {{ v.frame }} | 车辆ID: {{ v.track_id }} |
              位置: {{ v.location ? '[' + v.location.map(x => Math.round(x)).join(', ') + ']' : '—' }}
            </div>
          </div>
          <el-tag :type="v.type === 'wrong_direction' ? 'danger' : 'warning'" size="small">
            {{ v.type === 'wrong_direction' ? '逆行' : '违停' }}
          </el-tag>
        </div>
      </div>
      <el-button v-if="violations.length > 10" link type="primary" class="view-more">
        查看全部 {{ violations.length }} 条记录
      </el-button>
    </div>

    <!-- 原始数据 -->
    <div class="raw-data-section">
      <h3 class="section-title" @click="showRawData = !showRawData">
        <el-icon><Document /></el-icon>
        原始数据
        <el-icon class="expand-icon" :class="{ expanded: showRawData }"><ArrowDown /></el-icon>
      </h3>
      <el-collapse-transition>
        <pre v-show="showRawData" class="raw-data">{{ JSON.stringify(parsedResult, null, 2) }}</pre>
      </el-collapse-transition>
    </div>
  </div>

  <!-- 加载状态 -->
  <div v-else-if="loading" class="loading-state">
    <el-skeleton :rows="10" animated />
  </div>

  <!-- 错误状态 -->
  <div v-else class="error-state">
    <el-empty description="未找到任务信息">
      <template #extra>
        <p style="color: #999; margin-bottom: 16px;">任务ID: {{ route.params.taskId }}</p>
        <el-button type="primary" @click="$router.push('/history')">
          返回历史记录
        </el-button>
        <el-button @click="loadData">重试</el-button>
      </template>
    </el-empty>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElLoading } from 'element-plus'
import { toast } from '@/utils/ui'
import * as echarts from 'echarts'
import {getResult, getDetailedResult, downloadVideo, getVideoStreamUrl} from '@/api'

const route = useRoute()
const router = useRouter()

const taskId = computed(() => route.params.taskId)

const loading = ref(true)
const task = ref(null)
const parsedResult = ref(null)
const detailedResult = ref(null)
const exporting = ref(false)
const showRawData = ref(false)
const chartType = ref('line')

// ✅ 修复：视频相关状态
const videoPlayer = ref(null)
const videoLoading = ref(true)
const videoError = ref(false)
const videoErrorMsg = ref('')
const videoInfo = ref(null)
const videoRetryCount = ref(0)

// ✅ 修复：ECharts 实例变量（使用不同的名字，避免和 ref 冲突）
let trendChartInstance = null
const trendChartRef = ref(null)  // 用于模板 ref

// 计算属性保持不变...
const trafficData = computed(() => {
  const counts = parsedResult.value?.statistics?.traffic_counts
  if (!counts) return []
  return Object.entries(counts).map(([name, data]) => ({
    name,
    in: data.in || 0,
    out: data.out || 0,
    total: (data.in || 0) + (data.out || 0)
  }))
})

const violations = computed(() => {
  return parsedResult.value?.statistics?.violations?.details || []
})

const avgSpeedKm = computed(() => {
  const v = parsedResult.value?.statistics?.speed_estimation?.avg_kmh
  return v != null && !Number.isNaN(v) ? Number(v).toFixed(1) : '—'
})

const frameData = computed(() => {
  if (!detailedResult.value?.frame_results?.length) return []
  const frames = detailedResult.value.frame_results
  const step = Math.max(1, Math.floor(frames.length / 100))
  return frames.filter((_, i) => i % step === 0).map(f => ({
    frame: f.frame,
    timestamp: f.timestamp,
    count: f.count,
    cars: f.tracks?.length || 0
  }))
})

// 视频流地址
const videoStreamUrl = computed(() => {
  if (!taskId.value) return ''
  return getVideoStreamUrl(taskId.value)  // 调用函数
})

onMounted(() => {
  if (!taskId.value) {
    toast.error('URL中缺少任务ID')
    loading.value = false
    return
  }
  loadData()
})

// ✅ 监听 chartType 变化，切换图表类型
watch(chartType, () => {
  initTrendChart()
})

watch(() => task.value?.status, (status) => {
  if (status === 2) {
    nextTick(() => initVideo())
  }
})

// ✅ 修复：加载任务数据，添加详细调试
const loadData = async () => {
  loading.value = true
  task.value = null

  try {
    console.log('=== 开始加载任务 ===')
    console.log('任务ID:', taskId.value)

    // 获取基本信息
    console.log('请求 URL:', `/traffic/result/${taskId.value}`)
    const res = await getResult(taskId.value)
    console.log('API返回:', res.data)

    if (res.data.code !== 200) {
      throw new Error(res.data.message || '获取任务失败')
    }

    task.value = res.data.data

    if (!task.value) {
      throw new Error('任务数据为空')
    }

    console.log('任务状态:', task.value.status)
    console.log('任务结果JSON:', task.value.resultJson)

    // 解析结果JSON
    if (task.value.resultJson) {
      try {
        parsedResult.value = JSON.parse(task.value.resultJson)
        console.log('解析结果成功:', parsedResult.value)
      } catch (e) {
        console.error('解析结果JSON失败:', e)
        parsedResult.value = null
      }
    }

    // 获取详细结果（仅当任务完成时）
    if (task.value.status === 2) {
      try {
        console.log('任务已完成，获取详细结果...')
        const detailRes = await getDetailedResult(taskId.value)
        console.log('详细结果返回:', detailRes.data)

        if (detailRes.data.code === 200) {
          detailedResult.value = detailRes.data.data
          // 等待 DOM 更新后初始化图表
          await nextTick()
          setTimeout(() => {
            initTrendChart()
          }, 100)
        }
      } catch (e) {
        console.warn('获取详细结果失败:', e)
      }

      // 初始化视频
      nextTick(() => {
        setTimeout(() => {
          initVideo()
        }, 500)
      })
    }
  } catch (err) {
    console.error('加载失败详情:', err)
    console.error('错误配置:', err.config)
    toast.error('加载失败: ' + (err.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

// ✅ 修复：初始化视频，添加详细调试
const initVideo = () => {
  console.log('=== 初始化视频 ===')
  console.log('videoPlayer ref:', videoPlayer.value)
  console.log('videoStreamUrl:', videoStreamUrl.value)

  if (!videoPlayer.value) {
    console.error('videoPlayer ref 为 null')
    return
  }

  if (!taskId.value) {
    console.error('taskId 为空')
    return
  }

  videoLoading.value = true
  videoError.value = false

  // 设置视频源
  const url = videoStreamUrl.value
  console.log('设置视频 src:', url)

  videoPlayer.value.src = url

  // 添加事件监听（调试用）
  videoPlayer.value.onloadeddata = () => {
    console.log('视频数据已加载')
    onVideoReady()
  }

  videoPlayer.value.onerror = (e) => {
    console.error('视频加载错误事件:', e)
    onVideoError(e)
  }

  // 尝试加载
  try {
    videoPlayer.value.load()
    console.log('video.load() 已调用')
  } catch (err) {
    console.error('video.load() 失败:', err)
    videoLoading.value = false
    videoError.value = true
    videoErrorMsg.value = '视频初始化失败: ' + err.message
  }
}

const onVideoReady = () => {
  console.log('视频就绪')
  videoLoading.value = false
  videoError.value = false
  videoInfo.value = {
    duration: videoPlayer.value?.duration || 0
  }
}

const onVideoError = (e) => {
  videoLoading.value = false
  videoError.value = true

  const video = e.target
  console.error('视频错误详情:', {
    code: video.error?.code,
    message: video.error?.message,
    networkState: video.networkState,
    readyState: video.readyState,
    src: video.src
  })

  if (video.error) {
    switch(video.error.code) {
      case 1: videoErrorMsg.value = '视频加载中断（用户取消）'; break
      case 2: videoErrorMsg.value = '网络错误，无法加载视频（请检查后端服务）'; break
      case 3: videoErrorMsg.value = '视频解码失败（格式不支持或文件损坏）'; break
      case 4: videoErrorMsg.value = '视频格式不支持或文件不存在'; break
      default: videoErrorMsg.value = '未知错误 (代码: ' + video.error.code + ')'
    }
  } else {
    videoErrorMsg.value = '视频加载失败，请检查网络连接'
  }
}

const retryLoadVideo = () => {
  console.log('重试加载视频...')
  videoRetryCount.value++
  initVideo()
}


const forceDownload = async () => {
  try {
    // 显示加载提示
    const loading = ElLoading.service({
      lock: true,
      text: '正在下载视频...',
      background: 'rgba(0, 0, 0, 0.7)'
    })

    // 调用下载API
    const response = await downloadVideo(taskId.value)

    loading.close()

    // 创建 Blob URL 并触发下载
    const blob = new Blob([response.data], { type: 'video/mp4' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `${taskId.value}_result.mp4`  // 设置下载文件名
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)

    // 清理 Blob URL
    URL.revokeObjectURL(url)

    toast.success('下载成功')
  } catch (error) {
    console.error('下载失败:', error)
    toast.error('下载失败: ' + (error.message || '网络错误'))
  }
}

const scrollToVideo = () => {
  const el = document.getElementById('video-section')
  if (el) {
    el.scrollIntoView({ behavior: 'smooth' })
    if (videoPlayer.value && videoPlayer.value.paused) {
      videoPlayer.value.play().catch(e => console.log('自动播放失败:', e))
    }
  }
}

// ✅ 修复：初始化 ECharts，使用 trendChartRef
const initTrendChart = () => {
  if (!frameData.value.length) {
    console.log('没有帧数据，跳过图表初始化')
    return
  }

  console.log('初始化图表...')

  // 如果已存在实例，先销毁
  if (trendChartInstance) {
    try {
      trendChartInstance.dispose()
      trendChartInstance = null
    } catch (e) {
      console.warn('销毁旧图表失败:', e)
    }
  }

  // 使用 nextTick 确保 DOM 已更新
  nextTick(() => {
    const chartDom = trendChartRef.value  // ✅ 使用 ref 获取 DOM
    console.log('图表 DOM 元素:', chartDom)

    if (!chartDom) {
      console.error('找不到图表容器 DOM')
      return
    }

    try {
      trendChartInstance = echarts.init(chartDom)

      const option = {
        tooltip: {
          trigger: 'axis',
          backgroundColor: 'rgba(30, 41, 59, 0.9)',
          borderColor: 'rgba(99, 102, 241, 0.3)',
          textStyle: { color: '#f1f5f9' },
          formatter: (params) => {
            const p = params[0]
            return `时间: ${p.name}s<br/>车辆数: ${p.value}`
          }
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: frameData.value.map(f => f.timestamp.toFixed(1)),
          name: '时间 (s)',
          axisLine: { lineStyle: { color: '#475569' } },
          axisLabel: { color: '#94a3b8' }
        },
        yAxis: {
          type: 'value',
          name: '车辆数',
          minInterval: 1,
          axisLine: { lineStyle: { color: '#475569' } },
          axisLabel: { color: '#94a3b8' },
          splitLine: { lineStyle: { color: '#1e293b' } }
        },
        series: [{
          name: '车辆数',
          type: chartType.value,
          data: frameData.value.map(f => f.count),
          smooth: true,
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#6366f1' },
              { offset: 1, color: '#8b5cf6' }
            ])
          },
          lineStyle: {
            width: 3,
            color: '#6366f1'
          },
          areaStyle: chartType.value === 'line' ? {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(99, 102, 241, 0.3)' },
              { offset: 1, color: 'rgba(99, 102, 241, 0.05)' }
            ])
          } : undefined
        }]
      }

      trendChartInstance.setOption(option)
      console.log('图表初始化完成')
    } catch (e) {
      console.error('图表初始化失败:', e)
    }
  })
}

// 导出数据
const exportData = async () => {
  if (!parsedResult.value) return

  exporting.value = true
  try {
    const data = {
      task_info: {
        task_id: task.value.taskId,
        file_name: task.value.fileName,
        create_time: task.value.createTime,
        complete_time: task.value.updateTime
      },
      analysis_result: parsedResult.value,
      frame_data: detailedResult.value?.frame_results || []
    }

    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `traffic_analysis_${taskId.value}_${Date.now()}.json`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)

    toast.success('导出成功')
  } catch (e) {
    toast.error('导出失败: ' + e.message)
  } finally {
    exporting.value = false
  }
}

// 辅助函数保持不变
const getStatusClass = (status) => {
  const map = { 0: 'pending', 1: 'processing', 2: 'success', 3: 'error' }
  return map[status] || 'pending'
}

const getStatusText = (status) => {
  const map = { 0: '等待处理', 1: '正在分析', 2: '分析完成', 3: '分析失败' }
  return map[status] || '未知状态'
}

const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

const formatDuration = (sec) => {
  if (!sec) return '-'
  const m = Math.floor(sec / 60)
  const s = Math.floor(sec % 60)
  return m > 0 ? `${m}分${s}秒` : `${s}秒`
}

const formatSize = (bytes) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
}

// ✅ 修复：清理函数
onUnmounted(() => {
  console.log('组件卸载，清理资源')
  if (trendChartInstance) {
    try {
      trendChartInstance.dispose()
      trendChartInstance = null
    } catch (e) {
      console.warn('销毁图表失败:', e)
    }
  }
  if (videoPlayer.value) {
    videoPlayer.value.pause()
    videoPlayer.value.src = ''
    videoPlayer.value.load()
  }
})
</script>

<style scoped>
.result-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

/* 页面头部 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.back-btn {
  padding: 10px 16px;
}

.header-title h1 {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 8px;
}

.task-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--text-secondary);
  font-size: 13px;
}

.task-id {
  background: var(--bg-dark);
  padding: 4px 10px;
  border-radius: 6px;
  font-family: monospace;
}

.separator {
  opacity: 0.5;
}

.header-actions {
  display: flex;
  gap: 12px;
}

/* 状态横幅 */
.status-banner {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px;
  border-radius: 12px;
  margin-bottom: 24px;
}

.status-banner.success {
  background: rgba(16, 185, 129, 0.1);
  border: 1px solid rgba(16, 185, 129, 0.2);
}

.status-banner.error {
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.2);
}

.status-banner.processing {
  background: rgba(245, 158, 11, 0.1);
  border: 1px solid rgba(245, 158, 11, 0.2);
}

.status-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.status-banner.success .status-icon {
  background: rgba(16, 185, 129, 0.2);
  color: #10b981;
}

.status-banner.error .status-icon {
  background: rgba(239, 68, 68, 0.2);
  color: #ef4444;
}

.status-banner.processing .status-icon {
  background: rgba(245, 158, 11, 0.2);
  color: #f59e0b;
  animation: rotate 1s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 视频容器 */
.video-container {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 24px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 600;
  margin: 0;
}

.video-actions {
  display: flex;
  gap: 8px;
}

.loading-overlay {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px;
  color: var(--text-secondary);
  gap: 16px;
}

.error-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: #ef4444;
  gap: 12px;
}

.error-actions {
  display: flex;
  gap: 12px;
  margin-top: 8px;
}

.video-info-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--border);
  font-size: 13px;
  color: var(--text-secondary);
}

/* 指标网格 */
.metrics-grid {
  display: grid;
  grid-template-columns: 2fr repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.metric-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  position: relative;
  overflow: hidden;
}

.metric-card.large {
  min-height: 140px;
}

.metric-bg {
  position: absolute;
  right: -20px;
  top: 50%;
  transform: translateY(-50%);
  width: 150px;
  height: 150px;
  opacity: 0.3;
}

.metric-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.metric-content {
  position: relative;
  z-index: 1;
}

.metric-value {
  font-size: 32px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1;
  margin-bottom: 4px;
}

.metric-card.large .metric-value {
  font-size: 40px;
  background: var(--gradient);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.metric-label {
  font-size: 13px;
  color: var(--text-secondary);
}

.metric-trend {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  font-size: 12px;
  color: var(--primary);
}

/* 信息区域 */
.info-section {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 24px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.info-label {
  font-size: 12px;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.info-value {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

/* 流量统计 */
.traffic-section {
  margin-bottom: 24px;
}

.traffic-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 20px;
}

.traffic-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 20px;
}

.traffic-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.traffic-name {
  font-weight: 600;
  color: var(--text-primary);
}

.traffic-flow {
  display: flex;
  align-items: center;
  gap: 16px;
}

.flow-item {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.flow-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.flow-item.in .flow-icon {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
}

.flow-item.out .flow-icon {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.flow-info {
  display: flex;
  flex-direction: column;
}

.flow-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
}

.flow-label {
  font-size: 12px;
  color: var(--text-secondary);
}

.flow-divider {
  width: 1px;
  height: 50px;
  background: var(--border);
}

.flow-total {
  text-align: center;
  padding-left: 16px;
  border-left: 1px solid var(--border);
  min-width: 80px;
}

.flow-total .flow-value {
  font-size: 28px;
  background: var(--gradient);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

/* 图表区域 */
.chart-section {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 24px;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.chart-container {
  height: 350px;
}

/* 违规记录 */
.violation-section {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 24px;
}

.violation-count {
  margin-left: 8px;
}

.violation-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.violation-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  background: var(--bg-dark);
  border-radius: 12px;
  border-left: 4px solid;
}

.violation-item.wrong_direction {
  border-color: #ef4444;
}

.violation-item.illegal_parking {
  border-color: #f59e0b;
}

.violation-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.violation-item.illegal_parking .violation-icon {
  background: rgba(245, 158, 11, 0.1);
  color: #f59e0b;
}

.violation-info {
  flex: 1;
}

.violation-title {
  display: flex;
  align-items: center;
  gap: 12px;
  font-weight: 600;
  margin-bottom: 4px;
}

.violation-time {
  font-size: 12px;
  color: var(--text-secondary);
  font-weight: normal;
}

.violation-detail {
  font-size: 13px;
  color: var(--text-secondary);
}

.view-more {
  margin-top: 16px;
  width: 100%;
}

/* 原始数据 */
.raw-data-section {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 20px 24px;
}

.raw-data-section .section-title {
  cursor: pointer;
  margin-bottom: 0;
  user-select: none;
}

.expand-icon {
  margin-left: auto;
  transition: transform 0.3s;
}

.expand-icon.expanded {
  transform: rotate(180deg);
}

.raw-data {
  margin-top: 16px;
  padding: 20px;
  background: var(--bg-dark);
  border-radius: 12px;
  font-family: 'Monaco', 'Menlo', monospace;
  font-size: 12px;
  line-height: 1.6;
  color: var(--text-secondary);
  overflow-x: auto;
  max-height: 400px;
  overflow-y: auto;
}

/* 加载和错误状态 */
.loading-state,
.error-state {
  padding: 80px 20px;
  max-width: 600px;
  margin: 0 auto;
}

/* 响应式 */
@media (max-width: 1024px) {
  .metrics-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .metric-card.large {
    grid-column: span 2;
  }

  .info-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
  }

  .metrics-grid {
    grid-template-columns: 1fr;
  }

  .metric-card.large {
    grid-column: span 1;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }

  .traffic-flow {
    flex-wrap: wrap;
  }
}
</style>