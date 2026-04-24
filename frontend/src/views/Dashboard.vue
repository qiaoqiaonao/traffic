<template>
  <div class="dashboard-page">
    <div class="page-header">
      <h1 class="page-title">
        <el-icon><DataAnalysis /></el-icon>
        数据仪表盘
      </h1>
      <div class="time-selector">
        <el-radio-group v-model="timeRange" @change="updateDashboard" size="default">
          <el-radio-button label="today">今日</el-radio-button>
          <el-radio-button label="week">本周</el-radio-button>
          <el-radio-button label="month">本月</el-radio-button>
        </el-radio-group>
        <el-button @click="refreshData" :loading="loading">
          <el-icon><Refresh /></el-icon>
        </el-button>
      </div>
    </div>

    <!-- 核心指标卡片 -->
    <div class="kpi-section">
      <div
          v-for="kpi in kpis"
          :key="kpi.key"
          class="kpi-card"
          :class="{ 'trend-up': kpi.trend > 0, 'trend-down': kpi.trend < 0 }"
      >
        <div class="kpi-header">
          <span class="kpi-label">{{ kpi.label }}</span>
          <div class="kpi-trend">
            <el-icon v-if="kpi.trend !== 0">
              <component :is="kpi.trend > 0 ? 'ArrowUp' : 'ArrowDown'" />
            </el-icon>
            <span>{{ Math.abs(kpi.trend) }}%</span>
          </div>
        </div>
        <div class="kpi-value">
          <span class="value-number">{{ kpi.value }}</span>
          <span class="value-unit">{{ kpi.unit }}</span>
        </div>
        <div class="kpi-chart">
          <svg viewBox="0 0 100 30" preserveAspectRatio="none">
            <path
                :d="kpi.sparkline"
                fill="none"
                :stroke="kpi.trend >= 0 ? '#10b981' : '#ef4444'"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
            />
            <defs>
              <linearGradient :id="'gradient-' + kpi.key" x1="0%" y1="0%" x2="0%" y2="100%">
                <stop offset="0%" :stop-color="kpi.trend >= 0 ? '#10b981' : '#ef4444'" stop-opacity="0.3"/>
                <stop offset="100%" :stop-color="kpi.trend >= 0 ? '#10b981' : '#ef4444'" stop-opacity="0"/>
              </linearGradient>
            </defs>
            <path
                :d="kpi.sparkline + ' L100,30 L0,30 Z'"
                :fill="'url(#gradient-' + kpi.key + ')'"
                opacity="0.3"
            />
          </svg>
        </div>
      </div>
    </div>

    <!-- 主图表区域 -->
    <div class="charts-main">
      <!-- 流量趋势 -->
      <div class="chart-card large">
        <div class="chart-header">
          <h3>
            <el-icon><TrendCharts /></el-icon>
            时段流量趋势
          </h3>
          <div class="chart-legend">
            <span class="legend-item">
              <span class="dot" style="background: #6366f1;"></span>
              进入
            </span>
            <span class="legend-item">
              <span class="dot" style="background: #8b5cf6;"></span>
              离开
            </span>
          </div>
        </div>
        <div ref="trendChartDom" class="chart-body"></div>
      </div>

      <!-- 实时状态 -->
      <div class="status-panel">
        <h3>
          <el-icon><Monitor /></el-icon>
          实时监控
        </h3>
        <div class="status-list">
          <div
              v-for="task in activeTasks"
              :key="task.taskId"
              class="status-item"
          >
            <div class="status-indicator active"></div>
            <div class="status-info">
              <div class="status-name">{{ task.fileName }}</div>
              <div class="status-progress">
                <el-progress
                    :percentage="task.progress"
                    :stroke-width="4"
                    :show-text="false"
                />
              </div>
            </div>
            <div class="status-percent">{{ task.progress }}%</div>
          </div>
        </div>
        <div v-if="activeTasks.length === 0" class="status-empty">
          暂无进行中的任务
        </div>

        <div class="system-status">
          <h4>系统状态</h4>
          <div class="status-grid">
            <div class="sys-item">
              <span class="sys-label">AI服务</span>
              <span class="sys-value online">在线</span>
            </div>
            <div class="sys-item">
              <span class="sys-label">WebSocket</span>
              <span class="sys-value online">正常</span>
            </div>
            <div class="sys-item">
              <span class="sys-label">数据库</span>
              <span class="sys-value online">正常</span>
            </div>
            <div class="sys-item">
              <span class="sys-label">存储空间</span>
              <span class="sys-value warning">67%</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 次要图表 -->
    <div class="charts-secondary">
      <!-- 车型分布 -->
      <div class="chart-card">
        <div class="chart-header">
          <h3>
            <el-icon><PieChart /></el-icon>
            车型分布
          </h3>
        </div>
        <div ref="pieChartDom" class="chart-body"></div>
      </div>

      <!-- 检测线对比 -->
      <div class="chart-card">
        <div class="chart-header">
          <h3>
            <el-icon><Histogram /></el-icon>
            检测线流量对比
          </h3>
        </div>
        <div ref="barChartDom" class="chart-body"></div>
      </div>

      <!-- 违规统计 -->
      <div class="chart-card">
        <div class="chart-header">
          <h3>
            <el-icon><Warning /></el-icon>
            违规类型统计
          </h3>
        </div>
        <div class="violation-stats">
          <div
              v-for="v in violationStats"
              :key="v.type"
              class="violation-bar"
          >
            <div class="violation-info">
              <span class="violation-name">{{ v.name }}</span>
              <span class="violation-count">{{ v.count }}</span>
            </div>
            <div class="violation-progress">
              <div
                  class="violation-fill"
                  :style="{ width: v.percent + '%', background: v.color }"
              ></div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 最近活动 -->
    <div class="activity-section">
      <h3 class="section-title">
        <el-icon><Clock /></el-icon>
        最近活动
      </h3>
      <div class="activity-list">
        <div
            v-for="(activity, idx) in recentActivities"
            :key="idx"
            class="activity-item"
        >
          <div class="activity-icon" :class="activity.type">
            <el-icon>
              <component :is="activity.icon" />
            </el-icon>
          </div>
          <div class="activity-content">
            <div class="activity-title">{{ activity.title }}</div>
            <div class="activity-time">{{ activity.time }}</div>
          </div>
          <div class="activity-status" :class="activity.status">
            {{ activity.statusText }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getHistory } from '@/api'

const loading = ref(false)
const timeRange = ref('today')

const kpis = ref([
  { key: 'vehicles', label: '累计独立车辆', value: '0', unit: '辆', trend: 0, sparkline: 'M0,15 L100,15' },
  { key: 'tasks', label: '已完成分析', value: '0', unit: '个', trend: 0, sparkline: 'M0,15 L100,15' },
  { key: 'speed', label: '平均车速(估)', value: '—', unit: 'km/h', trend: 0, sparkline: 'M0,15 L100,15' },
  { key: 'violations', label: '违规事件', value: '0', unit: '起', trend: 0, sparkline: 'M0,15 L100,15' }
])

const activeTasks = ref([])
const violationStats = ref([])
const recentActivities = ref([])

const trendIn = ref(Array(24).fill(0))
const trendOut = ref(Array(24).fill(0))
const pieSlices = ref([{ value: 1, name: '暂无', itemStyle: { color: '#334155' } }])
const barCategories = ref(['检测线'])
const barValues = ref([0])

// DOM 容器 ref
const trendChartDom = ref(null)
const pieChartDom = ref(null)
const barChartDom = ref(null)

// ECharts 实例（普通变量，避免和 Vue ref 混用）
let trendChartInst = null
let pieChartInst = null
let barChartInst = null

function fmtTimeAgo (iso) {
  if (!iso) return ''
  const t = new Date(iso).getTime()
  const m = Math.floor((Date.now() - t) / 60000)
  if (m < 1) return '刚刚'
  if (m < 60) return `${m}分钟前`
  if (m < 1440) return `${Math.floor(m / 60)}小时前`
  return `${Math.floor(m / 1440)}天前`
}

async function loadDashboard () {
  loading.value = true
  try {
    console.log('🚀 Dashboard 开始加载...')
    const res = await getHistory({ page: 1, pageSize: 100 })
    console.log('📦 API 返回:', res.data)

    const records = res.data?.data?.records || []
    console.log('📊 总记录数:', records.length)
    console.log('✅ 已完成任务:', records.filter(r => r.status === 2).length)
    console.log('📝 有 resultJson 的:', records.filter(r => r.status === 2 && r.resultJson).length)

    let totalUnique = 0
    let completed = 0
    let violationSum = 0
    let speedSum = 0
    let speedN = 0
    let sumIn = 0
    let sumOut = 0
    const vCount = { wrong_direction: 0, illegal_parking: 0 }
    const pieAgg = { car: 0, bus: 0, van: 0, others: 0 }
    const lineTotals = {}
    const processing = []
    const acts = []

    for (const r of records) {
      if (r.status === 1) {
        processing.push({
          taskId: r.taskId,
          fileName: r.fileName || r.taskId,
          progress: r.progress ?? 0
        })
      }
      if (r.status === 2 && r.resultJson) {
        completed++
        try {
          const pr = JSON.parse(r.resultJson)
          totalUnique += pr.statistics?.unique_vehicles || 0
          violationSum += pr.statistics?.violations?.total || 0
          const sp = pr.statistics?.speed_estimation?.avg_kmh
          if (sp != null && !Number.isNaN(Number(sp))) {
            speedSum += Number(sp)
            speedN++
          }
          const details = pr.statistics?.violations?.details || []
          for (const d of details) {
            if (d.type === 'wrong_direction') vCount.wrong_direction++
            else if (d.type === 'illegal_parking') vCount.illegal_parking++
          }
          const cd = pr.statistics?.class_distribution || {}
          pieAgg.car += cd.car || 0
          pieAgg.bus += cd.bus || 0
          pieAgg.van += cd.van || 0
          pieAgg.others += cd.others || 0
          const tc = pr.statistics?.traffic_counts || {}
          for (const [name, c] of Object.entries(tc)) {
            if (c && typeof c === 'object' && 'in' in c) {
              sumIn += c.in || 0
              sumOut += c.out || 0
              const t = (c.in || 0) + (c.out || 0)
              lineTotals[name] = (lineTotals[name] || 0) + t
            }
          }
        } catch (e) {
          console.warn('解析 resultJson 失败:', r.taskId, e.message)
        }
      }
    }

    activeTasks.value = processing.slice(0, 12)

    kpis.value[0].value = totalUnique.toLocaleString()
    kpis.value[1].value = String(completed)
    kpis.value[2].value = speedN ? (speedSum / speedN).toFixed(1) : '—'
    kpis.value[3].value = String(violationSum)

    const vtot = vCount.wrong_direction + vCount.illegal_parking
    violationStats.value = vtot
        ? [
          {
            type: 'wrong_direction',
            name: '逆行',
            count: vCount.wrong_direction,
            percent: Math.round((vCount.wrong_direction / vtot) * 100),
            color: '#6366f1'
          },
          {
            type: 'illegal_parking',
            name: '违停',
            count: vCount.illegal_parking,
            percent: Math.round((vCount.illegal_parking / vtot) * 100),
            color: '#8b5cf6'
          }
        ]
        : []

    const spread = (total) => Array.from({ length: 24 }, () => Math.max(0, Math.round(total / 24)))
    trendIn.value = spread(sumIn)
    trendOut.value = spread(sumOut)

    pieSlices.value = [
      { value: pieAgg.car || 0, name: '轿车', itemStyle: { color: '#6366f1' } },
      { value: pieAgg.bus || 0, name: '公交', itemStyle: { color: '#06b6d4' } },
      { value: pieAgg.van || 0, name: '厢式', itemStyle: { color: '#8b5cf6' } },
      { value: pieAgg.others || 0, name: '其他', itemStyle: { color: '#64748b' } }
    ].filter((x) => x.value > 0)
    if (!pieSlices.value.length) {
      pieSlices.value = [{ value: 1, name: '暂无', itemStyle: { color: '#334155' } }]
    }

    const lineNames = Object.keys(lineTotals)
    barCategories.value = lineNames.length ? lineNames : ['检测线']
    barValues.value = lineNames.length ? lineNames.map((n) => lineTotals[n]) : [0]

    for (const r of records.slice(0, 8)) {
      const st = r.status
      let title = `${r.fileName || r.taskId}`
      let type = 'info'
      let icon = 'VideoPlay'
      let statusText = '记录'
      if (st === 2) {
        type = 'success'
        icon = 'CircleCheck'
        statusText = '完成'
        title = `分析完成: ${title}`
      } else if (st === 1) {
        type = 'warning'
        icon = 'Loading'
        statusText = '处理中'
        title = `进行中: ${title}`
      } else if (st === 3) {
        type = 'danger'
        icon = 'CircleClose'
        statusText = '失败'
      }
      acts.push({
        type,
        icon,
        title,
        time: fmtTimeAgo(r.updateTime || r.createTime),
        status: type,
        statusText
      })
    }
    recentActivities.value = acts

    console.log('🎯 聚合结果:', { sumIn, sumOut, pieAgg, lineTotals, completed })

    await nextTick()
    setTimeout(() => {
      disposeCharts()
      initCharts()
    }, 100)
  } catch (e) {
    console.error('❌ Dashboard 加载失败:', e)
  } finally {
    loading.value = false
  }
}

const initCharts = () => {
  initTrendChart()
  initPieChart()
  initBarChart()
}

const initTrendChart = () => {
  if (!trendChartDom.value) {
    console.warn('趋势图 DOM 未挂载')
    return
  }
  if (trendChartInst) trendChartInst.dispose()

  trendChartInst = echarts.init(trendChartDom.value)
  const hours = Array.from({ length: 24 }, (_, i) => `${i}:00`)

  trendChartInst.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(30, 41, 59, 0.9)',
      borderColor: 'rgba(99, 102, 241, 0.3)',
      textStyle: { color: '#f1f5f9' }
    },
    legend: {
      data: ['进入', '离开'],
      textStyle: { color: '#94a3b8' },
      bottom: 0
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: hours,
      axisLine: { lineStyle: { color: '#334155' } },
      axisLabel: { color: '#94a3b8' }
    },
    yAxis: {
      type: 'value',
      axisLine: { lineStyle: { color: '#334155' } },
      axisLabel: { color: '#94a3b8' },
      splitLine: { lineStyle: { color: '#1e293b' } }
    },
    series: [
      {
        name: '进入',
        type: 'line',
        data: trendIn.value,
        smooth: true,
        symbol: 'none',
        lineStyle: { width: 3, color: '#6366f1' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(99, 102, 241, 0.3)' },
            { offset: 1, color: 'rgba(99, 102, 241, 0.05)' }
          ])
        }
      },
      {
        name: '离开',
        type: 'line',
        data: trendOut.value,
        smooth: true,
        symbol: 'none',
        lineStyle: { width: 3, color: '#8b5cf6' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(139, 92, 246, 0.3)' },
            { offset: 1, color: 'rgba(139, 92, 246, 0.05)' }
          ])
        }
      }
    ]
  })
  console.log('✅ 趋势图初始化完成')
}

const initPieChart = () => {
  if (!pieChartDom.value) {
    console.warn('饼图 DOM 未挂载')
    return
  }
  if (pieChartInst) pieChartInst.dispose()

  pieChartInst = echarts.init(pieChartDom.value)
  pieChartInst.setOption({
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(30, 41, 59, 0.9)',
      borderColor: 'rgba(99, 102, 241, 0.3)',
      textStyle: { color: '#f1f5f9' }
    },
    legend: {
      orient: 'vertical',
      right: '5%',
      top: 'center',
      textStyle: { color: '#94a3b8' }
    },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['35%', '50%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 8,
        borderColor: '#1e293b',
        borderWidth: 2
      },
      label: { show: false },
      data: pieSlices.value
    }]
  })
  console.log('✅ 饼图初始化完成')
}

const initBarChart = () => {
  if (!barChartDom.value) {
    console.warn('柱状图 DOM 未挂载')
    return
  }
  if (barChartInst) barChartInst.dispose()

  barChartInst = echarts.init(barChartDom.value)
  barChartInst.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(30, 41, 59, 0.9)',
      borderColor: 'rgba(99, 102, 241, 0.3)',
      textStyle: { color: '#f1f5f9' }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: barCategories.value,
      axisLine: { lineStyle: { color: '#334155' } },
      axisLabel: { color: '#94a3b8' }
    },
    yAxis: {
      type: 'value',
      axisLine: { lineStyle: { color: '#334155' } },
      axisLabel: { color: '#94a3b8' },
      splitLine: { lineStyle: { color: '#1e293b' } }
    },
    series: [{
      data: barValues.value,
      type: 'bar',
      barWidth: '50%',
      itemStyle: {
        borderRadius: [6, 6, 0, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#6366f1' },
          { offset: 1, color: '#4f46e5' }
        ])
      }
    }]
  })
  console.log('✅ 柱状图初始化完成')
}

const updateCharts = () => {
  console.log('更新图表:', timeRange.value)
}

const handleResize = () => {
  trendChartInst?.resize()
  pieChartInst?.resize()
  barChartInst?.resize()
}

const disposeCharts = () => {
  trendChartInst?.dispose()
  pieChartInst?.dispose()
  barChartInst?.dispose()
  trendChartInst = null
  pieChartInst = null
  barChartInst = null
}

const refreshData = async () => {
  await loadDashboard()
}

const updateDashboard = () => {
  updateCharts()
}

onMounted(() => {
  loadDashboard()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  disposeCharts()
})

watch(timeRange, () => {
  updateCharts()
})
</script>

<style scoped>
.dashboard-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 28px;
  font-weight: 700;
  background: var(--gradient);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.time-selector {
  display: flex;
  gap: 12px;
}

/* KPI Section */
.kpi-section {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.kpi-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 20px;
  position: relative;
  overflow: hidden;
}

.kpi-card.trend-up {
  border-color: rgba(16, 185, 129, 0.2);
}

.kpi-card.trend-down {
  border-color: rgba(239, 68, 68, 0.2);
}

.kpi-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.kpi-label {
  font-size: 13px;
  color: var(--text-secondary);
}

.kpi-trend {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 600;
  padding: 4px 8px;
  border-radius: 20px;
}

.trend-up .kpi-trend {
  color: #10b981;
  background: rgba(16, 185, 129, 0.1);
}

.trend-down .kpi-trend {
  color: #ef4444;
  background: rgba(239, 68, 68, 0.1);
}

.kpi-value {
  margin-bottom: 16px;
}

.value-number {
  font-size: 32px;
  font-weight: 700;
  color: var(--text-primary);
}

.value-unit {
  font-size: 14px;
  color: var(--text-secondary);
  margin-left: 4px;
}

.kpi-chart {
  height: 50px;
  opacity: 0.6;
}

.kpi-chart svg {
  width: 100%;
  height: 100%;
}

/* Charts Main */
.charts-main {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
  margin-bottom: 24px;
}

.chart-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 20px;
}

.chart-card.large {
  min-height: 400px;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.chart-header h3 {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
}

.chart-header h3 .el-icon {
  color: var(--primary);
}

.chart-legend {
  display: flex;
  gap: 16px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-secondary);
}

.legend-item .dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.chart-body {
  height: 300px;
}

.chart-card.large .chart-body {
  height: 340px;
}

/* Status Panel */
.status-panel {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 20px;
}

.status-panel h3 {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 16px;
}

.status-panel h3 .el-icon {
  color: var(--primary);
}

.status-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 24px;
}

.status-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: var(--bg-dark);
  border-radius: 10px;
}

.status-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #94a3b8;
}

.status-indicator.active {
  background: #10b981;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.status-info {
  flex: 1;
}

.status-name {
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 6px;
}

.status-progress {
  width: 100%;
}

.status-percent {
  font-size: 12px;
  color: var(--text-secondary);
  font-weight: 600;
}

.status-empty {
  text-align: center;
  padding: 40px;
  color: var(--text-secondary);
  font-size: 13px;
}

.system-status {
  border-top: 1px solid var(--border);
  padding-top: 20px;
}

.system-status h4 {
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 12px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.sys-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  background: var(--bg-dark);
  border-radius: 8px;
}

.sys-label {
  font-size: 12px;
  color: var(--text-secondary);
}

.sys-value {
  font-size: 12px;
  font-weight: 600;
}

.sys-value.online {
  color: #10b981;
}

.sys-value.warning {
  color: #f59e0b;
}

/* Charts Secondary */
.charts-secondary {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.violation-stats {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 10px 0;
}

.violation-bar {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.violation-info {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
}

.violation-name {
  color: var(--text-secondary);
}

.violation-count {
  font-weight: 600;
  color: var(--text-primary);
}

.violation-progress {
  height: 6px;
  background: var(--bg-dark);
  border-radius: 3px;
  overflow: hidden;
}

.violation-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.5s ease;
}

/* Activity Section */
.activity-section {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 20px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 16px;
}

.section-title .el-icon {
  color: var(--primary);
}

.activity-list {
  display: flex;
  flex-direction: column;
}

.activity-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 0;
  border-bottom: 1px solid var(--border);
}

.activity-item:last-child {
  border-bottom: none;
}

.activity-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}

.activity-icon.success {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
}

.activity-icon.warning {
  background: rgba(245, 158, 11, 0.1);
  color: #f59e0b;
}

.activity-icon.info {
  background: rgba(99, 102, 241, 0.1);
  color: #6366f1;
}

.activity-content {
  flex: 1;
}

.activity-title {
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 4px;
}

.activity-time {
  font-size: 12px;
  color: var(--text-secondary);
}

.activity-status {
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 600;
}

.activity-status.success {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
}

.activity-status.warning {
  background: rgba(245, 158, 11, 0.1);
  color: #f59e0b;
}

.activity-status.processing {
  background: rgba(99, 102, 241, 0.1);
  color: #6366f1;
}

@media (max-width: 1024px) {
  .kpi-section {
    grid-template-columns: repeat(2, 1fr);
  }

  .charts-main {
    grid-template-columns: 1fr;
  }

  .charts-secondary {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .kpi-section {
    grid-template-columns: 1fr;
  }

  .page-header {
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
  }
}
</style>