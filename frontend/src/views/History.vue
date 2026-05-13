<template>
  <div class="history-page">
    <div class="page-header">
      <h1 class="page-title">
        <el-icon><Clock /></el-icon>
        分析历史
      </h1>
      <div class="header-actions">
        <el-radio-group v-model="filterStatus" @change="loadHistory" size="default">
          <el-radio-button :label="null">全部</el-radio-button>
          <el-radio-button :label="2">已完成</el-radio-button>
          <el-radio-button :label="1">处理中</el-radio-button>
          <el-radio-button :label="3">失败</el-radio-button>
        </el-radio-group>
        <el-button type="primary" @click="loadHistory" :loading="loading">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <!-- 统计概览 -->
    <div class="stats-overview">
      <div class="overview-card">
        <div class="overview-icon" style="background: rgba(99, 102, 241, 0.2); color: #6366f1;">
          <el-icon><VideoCamera /></el-icon>
        </div>
        <div class="overview-info">
          <div class="overview-value">{{ stats.total }}</div>
          <div class="overview-label">总任务数</div>
        </div>
      </div>
      <div class="overview-card">
        <div class="overview-icon" style="background: rgba(16, 185, 129, 0.2); color: #10b981;">
          <el-icon><CircleCheck /></el-icon>
        </div>
        <div class="overview-info">
          <div class="overview-value">{{ stats.completed }}</div>
          <div class="overview-label">已完成</div>
        </div>
      </div>
      <div class="overview-card">
        <div class="overview-icon" style="background: rgba(245, 158, 11, 0.2); color: #f59e0b;">
          <el-icon><Loading /></el-icon>
        </div>
        <div class="overview-info">
          <div class="overview-value">{{ stats.processing }}</div>
          <div class="overview-label">处理中</div>
        </div>
      </div>
      <div class="overview-card">
        <div class="overview-icon" style="background: rgba(239, 68, 68, 0.2); color: #ef4444;">
          <el-icon><CircleClose /></el-icon>
        </div>
        <div class="overview-info">
          <div class="overview-value">{{ stats.failed }}</div>
          <div class="overview-label">失败</div>
        </div>
      </div>
    </div>

    <!-- 任务列表 -->
    <div class="task-table-card">
      <el-table
          :data="tasks"
          v-loading="loading"
          @row-click="handleRowClick"
          highlight-current-row
          stripe
      >
        <el-table-column type="index" width="60" label="#" />

        <el-table-column label="任务ID" width="180">
          <template #default="{ row }">
            <code class="task-id">{{ row.taskId }}</code>
          </template>
        </el-table-column>

        <el-table-column label="文件名" min-width="200">
          <template #default="{ row }">
            <div class="file-cell">
              <el-icon><VideoPlay /></el-icon>
              <span class="filename">{{ row.fileName }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <div class="status-cell">
              <span class="status-dot" :class="getStatusClass(row.status)"></span>
              <el-tag :type="getStatusType(row.status)" size="small" effect="dark">
                {{ getStatusText(row.status) }}
              </el-tag>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="进度" width="150">
          <template #default="{ row }">
            <div class="progress-cell">
              <el-progress
                  :percentage="getProgress(row)"
                  :status="getProgressStatus(row)"
                  :stroke-width="6"
              />
            </div>
          </template>
        </el-table-column>

        <el-table-column label="文件大小" width="120">
          <template #default="{ row }">
            {{ formatSize(row.fileSize) }}
          </template>
        </el-table-column>

        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">
            <div class="time-cell">
              <el-icon><Calendar /></el-icon>
              <span>{{ formatTime(row.createTime, 'short') }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <div class="action-cell">
              <el-button
                  v-if="row.status === 2"
                  type="primary"
                  link
                  @click.stop="viewResult(row)"
              >
                <el-icon><View /></el-icon>
                查看结果
              </el-button>
              <el-button
                  v-if="row.status === 2"
                  type="success"
                  link
                  @click.stop="viewVideo(row)"
              >
                <el-icon><VideoPlay /></el-icon>
                播放视频
              </el-button>
              <el-button
                  v-if="row.status === 1"
                  type="danger"
                  link
                  @click.stop="cancelTaskHandler(row)"
              >
                <el-icon><CircleClose /></el-icon>
                取消
              </el-button>
              <el-button
                  v-if="row.status !== 1"
                  type="danger"
                  link
                  @click.stop="deleteTaskHandler(row)"
              >
                <el-icon><Delete /></el-icon>
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @size-change="handleSizeChange"
            @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- 空状态 -->
    <el-empty
        v-if="!loading && tasks.length === 0"
        description="暂无分析记录"
        class="empty-state"
    >
      <el-button type="primary" @click="$router.push('/analyze')">
        开始首次分析
      </el-button>
    </el-empty>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { getHistory, cancelTask, deleteHistory } from '@/api'
import { toast, confirmAction } from '@/utils/ui'
import { formatSize, formatTime } from '@/utils/format'
import { getStatusClass, getStatusText, getStatusType } from '@/utils/status'
import { debugLog } from '@/utils/debug'
import { getVideoStreamUrl ,getWsProgressUrl } from '@/config'

const router = useRouter()
const loading = ref(false)
const tasks = ref([])
const filterStatus = ref(null)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const stats = computed(() => {
  const s = { total: 0, completed: 0, processing: 0, failed: 0 }
  tasks.value.forEach(t => {
    s.total++
    if (t.status === 2) s.completed++
    else if (t.status === 1) s.processing++
    else if (t.status === 3) s.failed++
  })
  return s
})

const pollingTimer = ref(null)
const wsConnections = ref({})

onMounted(() => {
  loadHistory()
  startPolling()
})

onUnmounted(() => {
  stopPolling()
})

const startPolling = () => {
  if (pollingTimer.value) return
  pollingTimer.value = setInterval(() => {
    if (tasks.value.some(t => t.status === 1)) {
      silentRefresh()
      // 为处理中任务建立WebSocket连接
      connectProcessingTasksWs()
    }
  }, 3000)
}

const stopPolling = () => {
  if (pollingTimer.value) {
    clearInterval(pollingTimer.value)
    pollingTimer.value = null
  }
  // 添加：关闭所有WebSocket连接
  Object.values(wsConnections.value).forEach(ws => {
    if (ws && ws.readyState === WebSocket.OPEN) ws.close()
  })
  wsConnections.value = {}
}

// 新增：为处理中任务建立WebSocket连接
const connectProcessingTasksWs = () => {
  tasks.value.forEach(task => {
    if (task.status === 1 && !wsConnections.value[task.taskId]) {
      const wsUrl = getWsProgressUrl(task.taskId)
      const ws = new WebSocket(wsUrl)
      wsConnections.value[task.taskId] = ws

      ws.onmessage = (e) => {
        try {
          const data = JSON.parse(e.data)
          // 更新对应任务的进度
          const targetTask = tasks.value.find(t => t.taskId === data.taskId)
          if (targetTask && data.progress !== undefined) {
            targetTask.progress = data.progress
            // 如果任务完成或失败，刷新列表
            if (data.progress >= 100 || data.progress < 0) {
              setTimeout(() => loadHistory(), 500)
            }
          }
        } catch (err) {
          // 忽略解析错误
        }
      }

      ws.onclose = () => {
        delete wsConnections.value[task.taskId]
      }

      ws.onerror = () => {
        delete wsConnections.value[task.taskId]
      }
    }
  })
}

const silentRefresh = async () => {
  try {
    const res = await getHistory({
      page: currentPage.value,
      pageSize: pageSize.value,
      status: filterStatus.value
    })
    if (res.data.code === 200) {
      tasks.value = res.data.data.records || []
      total.value = res.data.data.total
      // 如果没有处理中任务了，停止轮询
      if (!tasks.value.some(t => t.status === 1)) {
        stopPolling()
      }
    }
  } catch (e) {
    // 静默失败
  }
}

const handlePageChange = (page) => {
  currentPage.value = page
  loadHistory()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1     // 切换页大小时重置到第1页
  loadHistory()
}

const loadHistory = async () => {
  loading.value = true
  try {
    const res = await getHistory({
      page: currentPage.value,
      pageSize: pageSize.value,
      status: filterStatus.value
    })
    if (res.data.code === 200) {
      tasks.value = res.data.data.records || []
      total.value = res.data.data.total
      // 添加：加载完成后为处理中任务连接WebSocket
      connectProcessingTasksWs()
    }
  } catch (e) {
    toast.error('加载失败: ' + e.message)
  } finally {
    loading.value = false
  }
}

const getProgress = (row) => {
  if (row.status === 2) return 100
  if (row.status === 3) return 100
  return row.progress || 0
}

const getProgressStatus = (row) => {
  if (row.status === 3) return 'exception'
  if (row.status === 2) return 'success'
  return null
}


const handleRowClick = (row) => {
  if (row.status === 2) {
    viewResult(row)
  }
}

const viewResult = (row) => {
  router.push(`/result/${row.taskId}`)
}

const viewVideo = (row) => {
  window.open(getVideoStreamUrl(row.taskId), '_blank')
}

const cancelTaskHandler = async (row) => {
  try {
    await confirmAction('确定要取消该任务吗？', '取消任务', { type: 'warning' })
    await cancelTask(row.taskId)
    toast.success('已取消')
    loadHistory()
  } catch (e) {
    // 取消操作
  }
}

const deleteTaskHandler = async (row) => {
  try {
    await confirmAction(
      `确定要删除「${row.fileName}」的分析记录吗？删除后不可恢复。`,
      '删除记录',
      { confirmButtonText: '确定删除', type: 'warning' }
    )
    await deleteHistory(row.taskId)
    toast.success('已删除')
    loadHistory()
    // 删除后检查是否需要重启轮询
    if (tasks.value.some(t => t.status === 1)) {
      startPolling()
    }
  } catch (e) {
    // 取消
  }
}
</script>

<style scoped>
.history-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
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

.page-title .el-icon {
  font-size: 32px;
}

.header-actions {
  display: flex;
  gap: 16px;
}

/* Stats Overview */
.stats-overview {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 30px;
}

.overview-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 24px;
  background: var(--bg-card);
  border-radius: 16px;
  border: 1px solid var(--border);
}

.overview-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
}

.overview-value {
  font-size: 32px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1;
}

.overview-label {
  font-size: 14px;
  color: var(--text-secondary);
  margin-top: 4px;
}

/* Task Table */
.task-table-card {
  background: var(--bg-card);
  border-radius: 16px;
  border: 1px solid var(--border);
  overflow: hidden;
}

.task-id {
  font-family: 'Monaco', monospace;
  font-size: 13px;
  color: var(--text-secondary);
  background: var(--bg-dark);
  padding: 4px 8px;
  border-radius: 4px;
}

.file-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.file-cell .el-icon {
  color: var(--primary);
  font-size: 18px;
}

.filename {
  color: black;
  font-weight: 500;
}

.status-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-dot.pending { background: #94a3b8; }
.status-dot.processing {
  background: #f59e0b;
  animation: pulse 1.5s infinite;
}
.status-dot.success { background: #10b981; }
.status-dot.error { background: #ef4444; }

.progress-cell {
  width: 100%;
}

.time-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-secondary);
  font-size: 13px;
}

.action-cell {
  display: flex;
  gap: 8px;
}

/* Pagination */
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: 20px 24px;
  border-top: 1px solid var(--border);
}

/* Empty State */
.empty-state {
  padding: 80px 0;
}

/* Table Row Hover */
:deep(.el-table__row) {
  cursor: pointer;
  transition: background 0.3s;
}

:deep(.el-table__row:hover) {
  background: rgba(99, 102, 241, 0.05) !important;
}

@media (max-width: 768px) {
  .stats-overview {
    grid-template-columns: repeat(2, 1fr);
  }

  .page-header {
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
  }
}
</style>