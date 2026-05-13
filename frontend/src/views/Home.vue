<template>
  <div class="home">
    <!-- Hero Section -->
    <section class="hero">
      <div class="hero-bg">
        <div class="gradient-orb orb-1"></div>
        <div class="gradient-orb orb-2"></div>
        <div class="grid-pattern"></div>
      </div>

      <div class="hero-content">
        <div class="badge">
          <span class="pulse"></span>
          AI 驱动 · 实时分析
        </div>
        <h1 class="hero-title">
          智能交通流量
          <span class="gradient-text">分析系统</span>
        </h1>
        <p class="hero-desc">
          基于 RT-DETR 深度学习模型与 DeepSORT 多目标跟踪算法，
          提供毫秒级车辆检测、精准流量统计与违规行为识别
        </p>

        <div class="hero-actions">
          <el-button type="primary" size="large" class="btn-primary" @click="$router.push('/analyze')">
            <el-icon><VideoPlay /></el-icon>
            开始视频分析
          </el-button>
          <el-button size="large" class="btn-secondary" @click="$router.push('/image-detect')">
            <el-icon><Picture /></el-icon>
            图片快速检测
          </el-button>
        </div>

        <!-- 实时统计卡片 -->
        <div class="stats-bar">
          <div v-for="stat in heroStats" :key="stat.label" class="stat-item">
            <div class="stat-number">{{ stat.value }}</div>
            <div class="stat-label">{{ stat.label }}</div>
          </div>
        </div>
      </div>
    </section>

    <!-- 功能特性 -->
    <section class="features">
      <h2 class="section-title">核心能力</h2>
      <div class="features-grid">
        <div
            v-for="(feature, idx) in features"
            :key="idx"
            class="feature-card"
            :style="{ '--delay': idx * 0.1 + 's' }"
        >
          <div class="feature-icon">
            <el-icon :size="32">
              <component :is="feature.icon" />
            </el-icon>
          </div>
          <h3>{{ feature.title }}</h3>
          <p>{{ feature.desc }}</p>
          <div class="feature-tags">
            <span v-for="tag in feature.tags" :key="tag">{{ tag }}</span>
          </div>
        </div>
      </div>
    </section>

    <!-- 最近分析 -->
    <section class="recent" v-if="recentTasks.length > 0">
      <div class="section-header">
        <h2 class="section-title">最近分析</h2>
        <router-link to="/history" class="view-all">
          查看全部 <el-icon><ArrowRight /></el-icon>
        </router-link>
      </div>

      <div class="task-list">
        <div
            v-for="task in recentTasks"
            :key="task.taskId"
            class="task-card"
            @click="viewResult(task)"
        >
          <div class="task-status" :class="getStatusClass(task.status)">
            <el-icon v-if="task.status === 2"><Check /></el-icon>
            <el-icon v-else-if="task.status === 1"><Loading /></el-icon>
            <el-icon v-else><Close /></el-icon>
          </div>
          <div class="task-info">
            <div class="task-name">{{ task.fileName }}</div>
            <div class="task-meta">
              <span>{{ formatTime(task.createTime, 'relative') }}</span>
              <span class="dot"></span>
              <span>{{ formatSize(task.fileSize) }}</span>
            </div>
          </div>
          <div class="task-progress" v-if="task.status === 1">
            <el-progress
                :percentage="task.progress"
                :stroke-width="4"
                :show-text="false"
            />
          </div>
          <el-icon class="task-arrow"><ArrowRight /></el-icon>
        </div>
      </div>
    </section>

    <!-- 技术架构 -->
    <section class="tech-stack">
      <h2 class="section-title">技术架构</h2>
      <div class="tech-flow">
        <div class="tech-node">
          <div class="node-icon" style="background: #3b82f6;">
            <el-icon><Upload /></el-icon>
          </div>
          <span>视频上传</span>
        </div>
        <div class="flow-arrow"></div>
        <div class="tech-node">
          <div class="node-icon" style="background: #8b5cf6;">
            <el-icon><Cpu /></el-icon>
          </div>
          <span>RT-DETR 检测</span>
        </div>
        <div class="flow-arrow"></div>
        <div class="tech-node">
          <div class="node-icon" style="background: #06b6d4;">
            <el-icon><Guide /></el-icon>
          </div>
          <span>DeepSORT 跟踪</span>
        </div>
        <div class="flow-arrow"></div>
        <div class="tech-node">
          <div class="node-icon" style="background: #10b981;">
            <el-icon><DataAnalysis /></el-icon>
          </div>
          <span>流量统计</span>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  VideoPlay,
  Picture,
  Check,
  Loading,
  Close,
  ArrowRight,
  Upload,
  Cpu,
  Guide,
  DataAnalysis,
  VideoCamera,
  TrendCharts,
  Warning,
  Timer
} from '@element-plus/icons-vue'
import { getHistory } from '@/api'
import { formatSize, formatTime } from '@/utils/format'
import { getStatusClass } from '@/utils/status'
import { debugError } from '@/utils/debug'

const router = useRouter()
const recentTasks = ref([])

const heroStats = ref([
  { value: '50+', label: '支持车型' },
  { value: '<30ms', label: '单帧推理' },
  { value: '97.2%', label: '检测精度' },
  { value: '24/7', label: '全天候运行' }
])

const features = [
  {
    icon: 'VideoCamera',
    title: '多目标实时检测',
    desc: '支持同时检测数百个目标，实时识别轿车、卡车、公交车、行人等多种类型',
    tags: ['RT-DETR', '实时检测', '多类别']
  },
  {
    icon: 'Guide',
    title: '智能轨迹跟踪',
    desc: 'DeepSORT算法实现稳定的ID关联，准确统计车辆进出流量与行驶轨迹',
    tags: ['DeepSORT', '轨迹分析', '流量统计']
  },
  {
    icon: 'Warning',
    title: '违规行为识别',
    desc: '自动检测逆行、违停、超速等交通违规行为，生成告警记录与证据截图',
    tags: ['违规检测', '自动告警', '证据留存']
  },
  {
    icon: 'TrendCharts',
    title: '可视化报表',
    desc: '生成时段流量趋势图、热力分布图、车型占比饼图等多维度数据可视化',
    tags: ['ECharts', '数据可视化', '报表导出']
  },
  {
    icon: 'Timer',
    title: '低延迟处理',
    desc: 'GPU加速推理，支持视频流实时处理，延迟低于100ms，满足实时性要求',
    tags: ['ONNX Runtime', 'GPU加速', '流式处理']
  },
  {
    icon: 'DataAnalysis',
    title: '历史数据分析',
    desc: '完整保存分析历史，支持任意时段回溯查询，对比不同时段交通流量变化',
    tags: ['历史查询', '数据对比', '趋势分析']
  }
]

onMounted(async () => {
  try {
    const res = await getHistory(5)
    if (res.data.code === 200) {
      recentTasks.value = res.data.data || []
    }
  } catch (e) {
    debugError('加载历史失败:', e)
  }
})

const viewResult = (task) => {
  if (task.status === 2) {
    router.push(`/result/${task.taskId}`)
  }
}

</script>

<style scoped>
.home {
  max-width: 1400px;
  margin: 0 auto;
}

/* Hero Section */
.hero {
  position: relative;
  padding: 80px 0 60px;
  text-align: center;
  overflow: hidden;
}

.hero-bg {
  position: absolute;
  inset: 0;
  overflow: hidden;
}

.gradient-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.4;
  animation: float 20s infinite ease-in-out;
}

.orb-1 {
  width: 500px;
  height: 500px;
  background: var(--primary);
  top: -200px;
  left: -100px;
  animation-delay: 0s;
}

.orb-2 {
  width: 400px;
  height: 400px;
  background: var(--secondary);
  bottom: -150px;
  right: -100px;
  animation-delay: -10s;
}

.grid-pattern {
  position: absolute;
  inset: 0;
  background-image:
      linear-gradient(rgba(99, 102, 241, 0.03) 1px, transparent 1px),
      linear-gradient(90deg, rgba(99, 102, 241, 0.03) 1px, transparent 1px);
  background-size: 50px 50px;
}

@keyframes float {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(30px, -30px) scale(1.1); }
  66% { transform: translate(-20px, 20px) scale(0.9); }
}

.hero-content {
  position: relative;
  z-index: 1;
}

.badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: rgba(99, 102, 241, 0.1);
  border: 1px solid rgba(99, 102, 241, 0.2);
  border-radius: 20px;
  font-size: 14px;
  color: var(--primary);
  margin-bottom: 24px;
}

.pulse {
  width: 8px;
  height: 8px;
  background: #10b981;
  border-radius: 50%;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(1.2); }
}

.hero-title {
  font-size: 56px;
  font-weight: 800;
  line-height: 1.2;
  margin-bottom: 24px;
  letter-spacing: -0.02em;
}

.gradient-text {
  background: var(--gradient);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.hero-desc {
  font-size: 18px;
  color: var(--text-secondary);
  max-width: 600px;
  margin: 0 auto 40px;
  line-height: 1.8;
}

.hero-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
  margin-bottom: 60px;
}

.btn-primary {
  background: var(--gradient);
  border: none;
  padding: 16px 32px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 12px;
  height: auto;
}

.btn-secondary {
  background: transparent;
  border: 1px solid var(--border);
  color: var(--text-primary);
  padding: 16px 32px;
  font-size: 16px;
  border-radius: 12px;
  height: auto;
}

.btn-secondary:hover {
  border-color: var(--primary);
  background: rgba(99, 102, 241, 0.1);
}

.stats-bar {
  display: flex;
  justify-content: center;
  gap: 60px;
  padding: 30px;
  background: rgba(245, 247, 250, 0.9);
  border: 1px solid var(--border);
  border-radius: 16px;
  backdrop-filter: blur(10px);
  max-width: 800px;
  margin: 0 auto;
}

.stat-number {
  font-size: 32px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.stat-label {
  font-size: 14px;
  color: var(--text-secondary);
}

/* Features */
.features {
  padding: 80px 0;
}

.section-title {
  font-size: 32px;
  font-weight: 700;
  text-align: center;
  margin-bottom: 48px;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

.feature-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 32px;
  transition: all 0.3s ease;
  animation: fadeUp 0.6s ease backwards;
  animation-delay: var(--delay);
}

@keyframes fadeUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
}

.feature-card:hover {
  transform: translateY(-5px);
  border-color: rgba(99, 102, 241, 0.3);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3);
}

.feature-icon {
  width: 60px;
  height: 60px;
  background: var(--gradient);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  margin-bottom: 20px;
}

.feature-card h3 {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 12px;
}

.feature-card p {
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 1.7;
  margin-bottom: 16px;
}

.feature-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.feature-tags span {
  padding: 4px 10px;
  background: rgba(99, 102, 241, 0.1);
  border-radius: 20px;
  font-size: 12px;
  color: var(--primary);
}

/* Recent Tasks */
.recent {
  padding: 60px 0;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.view-all {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--primary);
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  transition: gap 0.3s;
}

.view-all:hover {
  gap: 8px;
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.task-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s;
}

.task-card:hover {
  border-color: var(--primary);
  background: rgba(99, 102, 241, 0.05);
}

.task-status {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.task-status.success {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
}

.task-status.processing {
  background: rgba(245, 158, 11, 0.1);
  color: #f59e0b;
  animation: spin 1s linear infinite;
}

.task-status.error {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.task-status.pending {
  background: rgba(148, 163, 184, 0.1);
  color: #94a3b8;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.task-info {
  flex: 1;
}

.task-name {
  font-weight: 500;
  margin-bottom: 4px;
  color: var(--text-primary);
}

.task-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--text-secondary);
}

.dot {
  width: 4px;
  height: 4px;
  background: var(--text-secondary);
  border-radius: 50%;
}

.task-progress {
  width: 100px;
}

.task-arrow {
  color: var(--text-secondary);
  transition: transform 0.3s;
}

.task-card:hover .task-arrow {
  transform: translateX(4px);
  color: var(--primary);
}

/* Tech Stack */
.tech-stack {
  padding: 60px 0 80px;
}

.tech-flow {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
  flex-wrap: wrap;
}

.tech-node {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.node-icon {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 28px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
}

.tech-node span {
  font-size: 14px;
  color: var(--text-secondary);
  font-weight: 500;
}

.flow-arrow {
  width: 40px;
  height: 2px;
  background: linear-gradient(90deg, var(--primary), var(--secondary));
  position: relative;
}

.flow-arrow::after {
  content: '';
  position: absolute;
  right: 0;
  top: -4px;
  border: 5px solid transparent;
  border-left: 8px solid var(--secondary);
}

@media (max-width: 768px) {
  .features-grid {
    grid-template-columns: 1fr;
  }

  .hero-title {
    font-size: 36px;
  }

  .stats-bar {
    flex-wrap: wrap;
    gap: 30px;
  }
}
</style>